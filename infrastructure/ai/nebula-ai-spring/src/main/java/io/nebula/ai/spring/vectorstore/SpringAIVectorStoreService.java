package io.nebula.ai.spring.vectorstore;

import io.nebula.ai.core.embedding.EmbeddingService;
import io.nebula.ai.core.exception.VectorStoreException;
import io.nebula.ai.core.model.Document;
import io.nebula.ai.core.model.SearchRequest;
import io.nebula.ai.core.model.SearchResult;
import io.nebula.ai.core.vectorstore.VectorStoreService;
import io.nebula.ai.spring.config.VectorStoreProperties;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 基于Spring AI的向量存储服务实现
 * 
 * 包含批处理和重试机制，提供开箱即用的可靠性保证
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Service
@ConditionalOnMissingBean(name = "vectorStoreService")
public class SpringAIVectorStoreService implements VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(SpringAIVectorStoreService.class);

    private final VectorStore vectorStore;
    private final EmbeddingService embeddingService;
    private final VectorStoreProperties properties;
    private final String collectionName;

    @Autowired
    public SpringAIVectorStoreService(
            VectorStore vectorStore, 
            EmbeddingService embeddingService,
            VectorStoreProperties properties) {
        this.vectorStore = vectorStore;
        this.embeddingService = embeddingService;
        this.properties = properties;
        this.collectionName = "nebula-documents"; // 默认集合名称
        
        log.info("初始化 SpringAIVectorStoreService - 批处理: {}, 批大小: {}, 重试: {}, 最大重试: {}",
                properties.isBatchingEnabled(), properties.getBatchSize(),
                properties.isRetryEnabled(), properties.getMaxRetryAttempts());
    }

    @Override
    public boolean add(Document document) {
        try {
            log.debug("添加文档到向量存储: {}", document.getId());

            org.springframework.ai.document.Document springDocument = convertToSpringDocument(document);
            
            // 如果文档没有向量，则进行向量化
            if (!document.hasVector()) {
                List<Double> vector = embeddingService.embed(document.getContent()).getFirstVector();
                springDocument = new org.springframework.ai.document.Document(
                    springDocument.getText(),
                    springDocument.getMetadata()
                );
                // Spring AI Document 的ID在构造函数中设置
                // Spring AI Document 不直接支持设置向量，向量化由VectorStore内部处理
            }

            vectorStore.add(List.of(springDocument));
            return true;

        } catch (Exception e) {
            log.error("添加文档失败: {}", e.getMessage(), e);
            throw new VectorStoreException("添加文档失败: " + e.getMessage(), e);
        }
    }

    @Override
    public int addAll(List<Document> documents) {
        try {
            log.debug("批量添加文档到向量存储: {} 条", documents.size());

            List<org.springframework.ai.document.Document> springDocuments = documents.stream()
                    .map(this::convertToSpringDocument)
                    .collect(Collectors.toList());

            // 检查是否启用批处理
            if (!properties.isBatchingEnabled() || documents.size() <= properties.getBatchSize()) {
                // 不启用批处理或文档数量小于批大小，直接添加
                addWithRetry(springDocuments);
                return documents.size();
            }

            // 分批处理
            int totalProcessed = 0;
            int batchSize = properties.getBatchSize();
            
            for (int i = 0; i < springDocuments.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, springDocuments.size());
                List<org.springframework.ai.document.Document> batch = springDocuments.subList(i, endIndex);
                
                log.debug("处理批次 {}/{}: {} 个文档", 
                        (i / batchSize) + 1, 
                        (springDocuments.size() + batchSize - 1) / batchSize,
                        batch.size());
                
                // 带重试的添加操作
                addWithRetry(batch);
                totalProcessed += batch.size();
                
                // 批次间延迟
                if (endIndex < springDocuments.size() && properties.getBatchDelayMs() > 0) {
                    Thread.sleep(properties.getBatchDelayMs());
                }
            }
            
            log.info("成功批量添加 {} 个文档到向量存储", totalProcessed);
            return totalProcessed;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("批量添加文档被中断: {}", e.getMessage(), e);
            throw new VectorStoreException("批量添加文档被中断: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("批量添加文档失败: {}", e.getMessage(), e);
            throw new VectorStoreException("批量添加文档失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 带重试的添加文档（框架级可靠性保证）
     * 
     * @param documents Spring AI 文档列表
     * @throws VectorStoreException 所有重试失败后抛出
     */
    private void addWithRetry(List<org.springframework.ai.document.Document> documents) throws VectorStoreException {
        if (!properties.isRetryEnabled()) {
            // 不启用重试，直接添加
            vectorStore.add(documents);
            return;
        }
        
        Exception lastException = null;
        int maxAttempts = properties.getMaxRetryAttempts();
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                vectorStore.add(documents);
                
                if (attempt > 1) {
                    log.info("✅ 重试成功 (第 {} 次尝试)", attempt);
                }
                return; // 成功，直接返回
                
            } catch (Exception e) {
                lastException = e;
                
                if (attempt < maxAttempts) {
                    // 指数退避策略
                    long delayMs = properties.getRetryDelayMs() * (long) Math.pow(2, attempt - 1);
                    log.warn("⚠️ 添加文档失败 (尝试 {}/{}): {}，{}ms 后重试...", 
                            attempt, maxAttempts, e.getMessage(), delayMs);
                    
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new VectorStoreException("重试被中断", ie);
                    }
                } else {
                    log.error("❌ 所有重试均失败 ({} 次尝试)", maxAttempts);
                }
            }
        }
        
        // 所有重试都失败，抛出最后一个异常
        throw new VectorStoreException("批量添加文档失败（已重试 " + maxAttempts + " 次）: " + lastException.getMessage(), lastException);
    }

    @Override
    public CompletableFuture<Boolean> addAsync(Document document) {
        return CompletableFuture.supplyAsync(() -> add(document));
    }

    @Override
    public CompletableFuture<Integer> addAllAsync(List<Document> documents) {
        return CompletableFuture.supplyAsync(() -> addAll(documents));
    }

    @Override
    public Document get(String id) {
        try {
            log.debug("根据ID获取文档: {}", id);
            
            // Spring AI VectorStore 没有直接的get方法
            // 这里通过搜索ID来实现（需要将ID作为元数据存储）
            org.springframework.ai.vectorstore.SearchRequest springRequest =
                org.springframework.ai.vectorstore.SearchRequest.builder()
                    .query("dummy")
                    .topK(1)
                    .filterExpression("id == '" + id + "'")
                    .build();
            
            List<org.springframework.ai.document.Document> results = vectorStore.similaritySearch(springRequest);
            
            if (results.isEmpty()) {
                return null;
            }
            
            return convertToNebulaDocument(results.get(0));

        } catch (Exception e) {
            log.error("获取文档失败: {}", e.getMessage(), e);
            throw new VectorStoreException("获取文档失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Document> getAll(List<String> ids) {
        return ids.stream()
                .map(this::get)
                .filter(doc -> doc != null)
                .collect(Collectors.toList());
    }

    @Override
    public boolean delete(String id) {
        try {
            log.debug("删除文档: {}", id);

            // Spring AI VectorStore 支持按ID删除
            vectorStore.delete(List.of(id));
            return true;

        } catch (Exception e) {
            log.error("删除文档失败: {}", e.getMessage(), e);
            throw new VectorStoreException("删除文档失败: " + e.getMessage(), e);
        }
    }

    @Override
    public int deleteAll(List<String> ids) {
        try {
            log.debug("批量删除文档: {} 条", ids.size());

            vectorStore.delete(ids);
            return ids.size();

        } catch (Exception e) {
            log.error("批量删除文档失败: {}", e.getMessage(), e);
            throw new VectorStoreException("批量删除文档失败: " + e.getMessage(), e);
        }
    }

    @Override
    public int deleteByFilter(Map<String, Object> filter) {
        // Spring AI VectorStore 目前不支持按过滤条件批量删除
        // 需要先搜索再删除
        try {
            log.debug("按过滤条件删除文档: {}", filter);

            SearchResult searchResult = search(SearchRequest.builder()
                    .query("*")
                    .filter(filter)
                    .topK(10000) // 设置一个大的值来获取所有匹配的文档
                    .build());

            List<String> idsToDelete = searchResult.getDocumentIds();
            return deleteAll(idsToDelete);

        } catch (Exception e) {
            log.error("按过滤条件删除文档失败: {}", e.getMessage(), e);
            throw new VectorStoreException("按过滤条件删除文档失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Document document) {
        try {
            log.debug("更新文档: {}", document.getId());

            // Spring AI VectorStore 没有直接的更新方法
            // 通过删除后重新添加来实现
            delete(document.getId());
            return add(document);

        } catch (Exception e) {
            log.error("更新文档失败: {}", e.getMessage(), e);
            throw new VectorStoreException("更新文档失败: " + e.getMessage(), e);
        }
    }

    @Override
    public SearchResult search(String query, int topK) {
        return search(SearchRequest.query(query, topK));
    }

    @Override
    public SearchResult search(String query, int topK, double similarityThreshold) {
        return search(SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build());
    }

    @Override
    public SearchResult search(String query, int topK, Map<String, Object> filter) {
        return search(SearchRequest.builder()
                .query(query)
                .topK(topK)
                .filter(filter)
                .build());
    }

    @Override
    public SearchResult search(SearchRequest request) {
        try {
            log.debug("执行相似性搜索: query={}, topK={}", request.getQuery(), request.getTopK());

            org.springframework.ai.vectorstore.SearchRequest.Builder springRequestBuilder =
                org.springframework.ai.vectorstore.SearchRequest.builder()
                    .query(request.getQuery())
                    .topK(request.getTopK())
                    .similarityThreshold(request.getSimilarityThreshold());

            // 添加过滤条件
            if (request.hasFilter()) {
                String filterExpression = buildFilterExpression(request.getFilter());
                springRequestBuilder.filterExpression(filterExpression);
            }

            org.springframework.ai.vectorstore.SearchRequest springRequest = springRequestBuilder.build();
            List<org.springframework.ai.document.Document> results = vectorStore.similaritySearch(springRequest);

            return convertToNebulaSearchResult(results, request.getQuery());

        } catch (Exception e) {
            log.error("相似性搜索失败: {}", e.getMessage(), e);
            throw new VectorStoreException("相似性搜索失败: " + e.getMessage(), e);
        }
    }

    @Override
    public CompletableFuture<SearchResult> searchAsync(String query, int topK) {
        return CompletableFuture.supplyAsync(() -> search(query, topK));
    }

    @Override
    public CompletableFuture<SearchResult> searchAsync(SearchRequest request) {
        return CompletableFuture.supplyAsync(() -> search(request));
    }

    @Override
    public SearchResult searchByVector(List<Double> vector, int topK) {
        try {
            log.debug("执行向量搜索: dimension={}, topK={}", vector.size(), topK);

            // Spring AI VectorStore 主要支持文本搜索
            // 向量搜索需要使用更低级的API或者先将向量转换为查询
            throw new VectorStoreException("Spring AI VectorStore 暂不支持直接向量搜索");

        } catch (Exception e) {
            log.error("向量搜索失败: {}", e.getMessage(), e);
            throw new VectorStoreException("向量搜索失败: " + e.getMessage(), e);
        }
    }

    @Override
    public SearchResult searchByVector(List<Double> vector, int topK, Map<String, Object> filter) {
        return searchByVector(vector, topK);
    }

    @Override
    public boolean exists(String id) {
        return get(id) != null;
    }

    @Override
    public long count() {
        // Spring AI VectorStore 没有直接的count方法
        // 这里返回一个占位值
        return -1; // 表示不支持
    }

    @Override
    public long count(Map<String, Object> filter) {
        // Spring AI VectorStore 没有直接的count方法
        return -1; // 表示不支持
    }

    @Override
    public boolean clear() {
        try {
            log.warn("清空向量存储 - 这是一个危险操作");
            // Spring AI VectorStore 没有直接的清空方法
            // 需要根据具体实现来处理
            throw new VectorStoreException("Spring AI VectorStore 暂不支持清空操作");

        } catch (Exception e) {
            log.error("清空向量存储失败: {}", e.getMessage(), e);
            throw new VectorStoreException("清空向量存储失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            // 尝试执行一个简单的搜索来检查服务可用性
            search("test", 1);
            return true;
        } catch (Exception e) {
            log.warn("向量存储服务不可用: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getCollectionName() {
        return collectionName;
    }

    @Override
    public boolean createCollection(int dimension) {
        // Spring AI VectorStore 通常在首次使用时自动创建
        log.info("Spring AI VectorStore 将在首次使用时自动创建集合");
        return true;
    }

    @Override
    public boolean deleteCollection() {
        log.warn("Spring AI VectorStore 暂不支持删除集合操作");
        return false;
    }

    @Override
    public boolean collectionExists() {
        return isAvailable(); // 简化实现
    }

    /**
     * 将Nebula Document转换为Spring AI Document
     */
    private org.springframework.ai.document.Document convertToSpringDocument(Document document) {
        // 创建可变的 HashMap 以便添加额外的元数据
        Map<String, Object> metadata = document.getMetadata() != null ? 
                new java.util.HashMap<>(document.getMetadata()) : new java.util.HashMap<>();
        
        // 将文档ID添加到元数据中，以便后续查询
        metadata.put("nebula_id", document.getId());
        metadata.put("nebula_created_at", document.getCreatedAt().toString());
        metadata.put("nebula_updated_at", document.getUpdatedAt().toString());

        return new org.springframework.ai.document.Document(
                document.getId(),
                document.getContent(),
                metadata
        );
    }

    /**
     * 将Spring AI Document转换为Nebula Document
     */
    private Document convertToNebulaDocument(org.springframework.ai.document.Document springDocument) {
        return Document.builder()
                .id(springDocument.getId())
                .content(springDocument.getText())
                .metadata(springDocument.getMetadata())
                .build();
    }

    /**
     * 将Spring AI搜索结果转换为Nebula搜索结果
     */
    private SearchResult convertToNebulaSearchResult(
            List<org.springframework.ai.document.Document> springResults, String query) {
        
        List<SearchResult.DocumentResult> nebulaResults = springResults.stream()
                .map(doc -> SearchResult.DocumentResult.fromDocument(
                        convertToNebulaDocument(doc), 
                        1.0 // Spring AI 结果默认没有分数，这里使用1.0
                ))
                .collect(Collectors.toList());

        return SearchResult.builder()
                .documents(nebulaResults)
                .query(query)
                .totalFound(nebulaResults.size())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 构建过滤表达式
     */
    private String buildFilterExpression(Map<String, Object> filter) {
        return filter.entrySet().stream()
                .map(entry -> entry.getKey() + " == '" + entry.getValue() + "'")
                .collect(Collectors.joining(" AND "));
    }
}

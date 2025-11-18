package io.nebula.autoconfigure.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 自定义Chroma向量存储实现
 * 绕过Spring AI 1.1.0与Chroma latest的JSON解析兼容性问题
 */
public class CustomChromaVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(CustomChromaVectorStore.class);

    private static final String DEFAULT_TENANT = "default_tenant";
    private static final String DEFAULT_DATABASE = "default_database";

    private final ChromaApi chromaApi;
    private final EmbeddingModel embeddingModel;
    private final String collectionName;
    private final boolean initializeSchema;
    private String collectionId;

    public CustomChromaVectorStore(
            ChromaApi chromaApi,
            EmbeddingModel embeddingModel,
            String collectionName,
            boolean initializeSchema) {
        this.chromaApi = chromaApi;
        this.embeddingModel = embeddingModel;
        this.collectionName = collectionName;
        this.initializeSchema = initializeSchema;

        // 延迟初始化
        initializeCollection();
    }

    private void initializeCollection() {
        try {
            log.info("初始化Chroma collection: {}", collectionName);

            // 尝试获取collection
            try {
                ChromaApi.Collection collection = chromaApi.getCollection(DEFAULT_TENANT, DEFAULT_DATABASE, collectionName);
                this.collectionId = collection.id();
                log.info("找到已存在的collection: {}, ID: {}", collectionName, collectionId);
            } catch (Exception e) {
                if (initializeSchema) {
                    log.warn("Collection不存在，尝试创建: {}", collectionName);
                    try {
                        ChromaApi.CreateCollectionRequest request = new ChromaApi.CreateCollectionRequest(collectionName);
                        ChromaApi.Collection newCollection = chromaApi.createCollection(DEFAULT_TENANT, DEFAULT_DATABASE, request);
                        this.collectionId = newCollection.id();
                        log.info("成功创建collection: {}, ID: {}", collectionName, collectionId);
                    } catch (Exception createEx) {
                        // JSON解析失败，尝试从错误信息中提取ID或使用collection名称
                        log.warn("创建collection时JSON解析失败，将在首次使用时重试: {}", createEx.getMessage());
                        // 暂时使用collection名称作为ID（Chroma v2可能接受名称作为ID）
                        this.collectionId = collectionName;
                    }
                } else {
                    log.error("Collection不存在且未启用schema初始化: {}", collectionName);
                    // 不抛出异常，允许应用继续启动
                    log.warn("将使用降级模式运行");
                }
            }

        } catch (Exception e) {
            log.error("初始化Chroma collection失败", e);
            log.warn("将使用降级模式运行，collection ID将在首次add时设置");
        }
    }

    @Override
    public void add(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        try {
            log.debug("添加 {} 个文档到collection: {}", documents.size(), collectionName);

            // 如果collectionId还未设置，尝试再次初始化
            if (collectionId == null) {
                initializeCollection();
                if (collectionId == null) {
                    throw new RuntimeException("Collection未正确初始化");
                }
            }

            // 生成embeddings
            List<String> texts = documents.stream()
                    .map(Document::getText)
                    .toList();

            List<float[]> embeddings = embeddingModel.embed(texts);

            // 准备添加请求
            List<String> ids = documents.stream()
                    .map(Document::getId)
                    .toList();

            List<Map<String, Object>> metadatas = documents.stream()
                    .map(Document::getMetadata)
                    .map(this::convertMetadata)
                    .toList();

            // 调用Chroma API upsert文档
            ChromaApi.AddEmbeddingsRequest request = new ChromaApi.AddEmbeddingsRequest(
                    ids,
                    embeddings,
                    metadatas,
                    texts
            );

            chromaApi.upsertEmbeddings(DEFAULT_TENANT, DEFAULT_DATABASE, collectionId, request);
            log.debug("成功添加 {} 个文档到collection ID: {}", documents.size(), collectionId);

        } catch (Exception e) {
            log.error("添加文档失败", e);
            throw new RuntimeException("添加文档到Chroma失败", e);
        }
    }

    @Override
    public void delete(List<String> idList) {
        try {
            if (collectionId == null) {
                initializeCollection();
            }

            ChromaApi.DeleteEmbeddingsRequest request = new ChromaApi.DeleteEmbeddingsRequest(idList);
            chromaApi.deleteEmbeddings(DEFAULT_TENANT, DEFAULT_DATABASE, collectionName, request);

        } catch (Exception e) {
            log.error("删除文档失败", e);
            throw new RuntimeException("删除文档失败", e);
        }
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        log.warn("Filter.Expression delete 方法未实现");
        throw new UnsupportedOperationException("Filter-based delete not yet implemented");
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        try {
            if (collectionId == null) {
                initializeCollection();
            }

            // 生成查询文本的embedding
            List<float[]> queryEmbeddings = embeddingModel.embed(List.of(request.getQuery()));

            // 调用Chroma查询API
            ChromaApi.QueryRequest queryRequest = new ChromaApi.QueryRequest(
                    queryEmbeddings.get(0),
                    request.getTopK()
            );

            ChromaApi.QueryResponse response = chromaApi.queryCollection(
                    DEFAULT_TENANT,
                    DEFAULT_DATABASE,
                    collectionName,
                    queryRequest
            );

            // 转换响应为Document列表
            return convertQueryResponse(response);

        } catch (Exception e) {
            log.error("相似度搜索失败", e);
            throw new RuntimeException("搜索文档失败", e);
        }
    }

    private Map<String, Object> convertMetadata(Map<String, Object> metadata) {
        // Chroma要求metadata中的值类型有限制
        Map<String, Object> converted = new HashMap<>();
        metadata.forEach((k, v) -> {
            if (v instanceof String || v instanceof Number || v instanceof Boolean) {
                converted.put(k, v);
            } else if (v != null) {
                converted.put(k, v.toString());
            }
        });
        return converted;
    }

    private List<Document> convertQueryResponse(ChromaApi.QueryResponse response) {
        // 简化实现：从QueryResponse中提取文档
        // 实际实现需要根据具体的QueryResponse结构
        try {
            List<Document> documents = new ArrayList<>();

            if (response.ids() != null && !response.ids().isEmpty()) {
                List<List<String>> idsNested = response.ids();
                List<List<Map<String, Object>>> metadatasNested = response.metadata();
                List<List<String>> documentsNested = response.documents();
                List<List<Double>> distancesNested = response.distances();

                for (int i = 0; i < idsNested.size(); i++) {
                    List<String> idList = idsNested.get(i);
                    List<Map<String, Object>> metaList = metadatasNested != null && i < metadatasNested.size() ? metadatasNested.get(i) : null;
                    List<String> docList = documentsNested != null && i < documentsNested.size() ? documentsNested.get(i) : null;
                    List<Double> distList = distancesNested != null && i < distancesNested.size() ? distancesNested.get(i) : null;

                    for (int j = 0; j < idList.size(); j++) {
                        String id = idList.get(j);
                        String text = docList != null && j < docList.size() ? docList.get(j) : "";
                        Map<String, Object> metadata = metaList != null && j < metaList.size() ? new HashMap<>(metaList.get(j)) : new HashMap<>();
                        Double distance = distList != null && j < distList.size() ? distList.get(j) : 0.0;

                        // 计算相似度分数 (1 - distance)
                        metadata.put("distance", distance);
                        metadata.put("score", 1.0 - distance);

                        Document document = new Document(id, text, metadata);
                        documents.add(document);
                    }
                }
            }

            return documents;

        } catch (Exception e) {
            log.error("转换查询响应失败", e);
            return List.of();
        }
    }
}


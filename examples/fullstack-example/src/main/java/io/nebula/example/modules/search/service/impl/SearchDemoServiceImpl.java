package io.nebula.example.modules.search.service.impl;

import io.nebula.example.modules.data.entity.dos.Product;
import io.nebula.example.modules.data.mapper.ProductMapper;
import io.nebula.example.modules.search.entity.dos.ProductSearchDocument;
import io.nebula.example.modules.search.entity.dto.*;
import io.nebula.example.modules.search.service.SearchDemoService;
import io.nebula.search.core.SearchService;
import io.nebula.search.core.model.*;
import io.nebula.search.core.query.SearchQuery;
import io.nebula.search.core.query.builder.*;
import io.nebula.search.core.suggestion.TermSuggester;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 搜索演示服务实现
 *
 * @author nebula
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(SearchService.class)
public class SearchDemoServiceImpl implements SearchDemoService {

    private static final String PRODUCT_INDEX = "products";

    private final SearchService searchService;
    private final ProductMapper productMapper;

    @Override
    public CreateIndexDto.Response createProductIndex(CreateIndexDto.Request request) {
        try {
            // 使用请求中的索引名称,如果未提供则使用默认值
            String indexName = (request.getIndexName() != null && !request.getIndexName().trim().isEmpty()) 
                                ? request.getIndexName().trim() : PRODUCT_INDEX;
            
            log.info("创建产品索引: indexName={}, shards={}, replicas={}", 
                     indexName, request.getShards(), request.getReplicas());
            
            // 构建索引映射
            Map<String, Object> properties = new HashMap<>();
            properties.put("id", Map.of("type", "long"));
            properties.put("name", Map.of("type", "text", "analyzer", "standard"));
            properties.put("description", Map.of("type", "text", "analyzer", "standard"));
            properties.put("category", Map.of("type", "keyword"));
            properties.put("price", Map.of("type", "double"));
            properties.put("stockQuantity", Map.of("type", "integer"));
            properties.put("status", Map.of("type", "keyword"));
            properties.put("brand", Map.of("type", "keyword"));
            properties.put("tags", Map.of("type", "keyword"));
            properties.put("rating", Map.of("type", "double"));
            properties.put("salesCount", Map.of("type", "integer"));
            properties.put("createTime", Map.of("type", "date", "format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"));
            properties.put("updateTime", Map.of("type", "date", "format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"));

            IndexMapping mapping = new IndexMapping(Map.of("properties", properties));
            
            // 创建索引
            IndexResult result = searchService.createIndex(indexName, mapping);
            
            CreateIndexDto.Response response = new CreateIndexDto.Response();
            response.setSuccess(result.isSuccess());
            response.setIndexName(result.getIndexName());
            return response;
            
        } catch (Exception e) {
            log.error("创建产品索引失败", e);
            CreateIndexDto.Response response = new CreateIndexDto.Response();
            response.setSuccess(false);
            response.setErrorMessage(e.getMessage());
            return response;
        }
    }

    @Override
    public DeleteIndexDto.Response deleteProductIndex(DeleteIndexDto.Request request) {
        try {
            // 使用请求中的索引名称,如果未提供则使用默认值
            String indexName = (request.getIndexName() != null && !request.getIndexName().trim().isEmpty()) 
                                ? request.getIndexName().trim() : PRODUCT_INDEX;
            
            log.info("删除产品索引: indexName={}", indexName);
            
            IndexResult result = searchService.deleteIndex(indexName);
            DeleteIndexDto.Response response = new DeleteIndexDto.Response();
            response.setSuccess(result.isSuccess());
            response.setIndexName(indexName);
            return response;
        } catch (Exception e) {
            log.error("删除产品索引失败", e);
            DeleteIndexDto.Response response = new DeleteIndexDto.Response();
            response.setSuccess(false);
            response.setErrorMessage(e.getMessage());
            return response;
        }
    }

    @Override
    public IndexExistsDto.Response productIndexExists(IndexExistsDto.Request request) {
        try {
            // 使用请求中的索引名称,如果未提供则使用默认值
            String indexName = (request.getIndexName() != null && !request.getIndexName().trim().isEmpty()) 
                                ? request.getIndexName().trim() : PRODUCT_INDEX;
            
            log.info("检查产品索引是否存在: indexName={}", indexName);
            
            boolean exists = searchService.indexExists(indexName);
            IndexExistsDto.Response response = new IndexExistsDto.Response();
            response.setExists(exists);
            response.setIndexName(indexName);
            return response;
        } catch (Exception e) {
            log.error("检查产品索引是否存在失败", e);
            IndexExistsDto.Response response = new IndexExistsDto.Response();
            response.setExists(false);
            response.setErrorMessage(e.getMessage());
            return response;
        }
    }

    @Override
    public IndexProductDto.Response indexProduct(IndexProductDto.Request request) {
        try {
            // 从数据库获取产品信息
            Product product = productMapper.selectById(request.getProductId());
            if (product == null) {
                IndexProductDto.Response response = new IndexProductDto.Response();
                response.setSuccess(false);
                response.setErrorMessage("产品不存在: " + request.getProductId());
                return response;
            }

            // 转换为搜索文档
            ProductSearchDocument searchDoc = convertToSearchDocument(product);

            // 索引文档
            DocumentResult result = searchService.indexDocument(
                PRODUCT_INDEX,
                product.getId().toString(),
                searchDoc
            );

            IndexProductDto.Response response = new IndexProductDto.Response();
            response.setSuccess(result.isSuccess());
            response.setIndex(result.getIndex());
            response.setId(result.getId());
            response.setErrorMessage(result.getErrorMessage());
            return response;

        } catch (Exception e) {
            log.error("索引产品失败: {}", request.getProductId(), e);
            IndexProductDto.Response response = new IndexProductDto.Response();
            response.setSuccess(false);
            response.setErrorMessage("索引产品失败: " + e.getMessage());
            return response;
        }
    }

    @Override
    public BulkIndexProductsDto.Response bulkIndexProducts(BulkIndexProductsDto.Request request) {
        try {
            // 批量查询产品
            List<Product> products = productMapper.selectBatchIds(request.getProductIds());
            if (products.isEmpty()) {
                BulkIndexProductsDto.Response response = new BulkIndexProductsDto.Response();
                response.setTotalCount(0);
                response.setSuccessCount(0);
                response.setFailureCount(0);
                response.setErrors(Collections.emptyList());
                return response;
            }

            // 转换为文档Map
            Map<String, ProductSearchDocument> documents = products.stream()
                .collect(Collectors.toMap(
                    p -> p.getId().toString(),
                    this::convertToSearchDocument
                ));

            // 批量索引
            BulkResult result = searchService.bulkIndexDocuments(PRODUCT_INDEX, documents);

            BulkIndexProductsDto.Response response = new BulkIndexProductsDto.Response();
            response.setSuccess(result.getFailureCount() == 0);
            response.setTotalCount(result.getTotalCount());
            response.setSuccessCount(result.getSuccessCount());
            response.setFailureCount(result.getFailureCount());
            response.setErrors(result.getErrors());
            return response;

        } catch (Exception e) {
            log.error("批量索引产品失败", e);
            BulkIndexProductsDto.Response response = new BulkIndexProductsDto.Response();
            response.setSuccess(false);
            response.setTotalCount(request.getProductIds().size());
            response.setSuccessCount(0);
            response.setFailureCount(request.getProductIds().size());
            response.setErrors(List.of("批量索引失败: " + e.getMessage()));
            return response;
        }
    }

    @Override
    public SearchProductsDto.Response searchProducts(SearchProductsDto.Request request) {
        try {
            // 构建查询条件
            QueryBuilder query = buildQuery(request);

            // 构建搜索查询
            SearchQuery.Builder queryBuilder = SearchQuery.builder()
                .index(PRODUCT_INDEX)
                .query(query)
                .from((request.getPage() - 1) * request.getSize())
                .size(request.getSize());

            // 添加排序
            if (request.getSortFields() != null && !request.getSortFields().isEmpty()) {
                List<Map<String, Object>> sortList = request.getSortFields().stream()
                    .map(field -> {
                        Map<String, Object> sortMap = new HashMap<>();
                        sortMap.put(field, Map.of("order", request.getSortOrder()));
                        return sortMap;
                    })
                    .collect(Collectors.toList());
                queryBuilder.sort(sortList);
            }

            // 添加高亮
            if (Boolean.TRUE.equals(request.getHighlight())) {
                queryBuilder.highlight(Map.of(
                    "fields", Map.of(
                        "name", Map.of(),
                        "description", Map.of()
                    )
                ));
            }

            SearchQuery searchQuery = queryBuilder.build();

            // 执行搜索
            SearchResult<ProductSearchDocument> searchResult = searchService.search(searchQuery, ProductSearchDocument.class);

            // 转换结果
            SearchProductsDto.Response response = new SearchProductsDto.Response();
            response.setSuccess(true);
            response.setTotalHits(searchResult.getTotalHits());
            response.setMaxScore(searchResult.getMaxScore());
            response.setTook(searchResult.getTook());
            response.setTimedOut(searchResult.isTimedOut());
            response.setAggregations(searchResult.getAggregations());

            // 转换产品列表
            List<SearchProductsDto.ProductSearchResult> productResults = searchResult.getDocuments().stream()
                .map(doc -> {
                    SearchProductsDto.ProductSearchResult productResult = new SearchProductsDto.ProductSearchResult();
                    ProductSearchDocument source = doc.getSource();
                    
                    BeanUtils.copyProperties(source, productResult);
                    productResult.setId(doc.getId());
                    productResult.setScore(doc.getScore());
                    productResult.setHighlight(doc.getHighlight());
                    
                    return productResult;
                })
                .collect(Collectors.toList());

            response.setProducts(productResults);
            return response;

        } catch (Exception e) {
            log.error("搜索产品失败", e);
            SearchProductsDto.Response response = new SearchProductsDto.Response();
            response.setSuccess(false);
            response.setErrorMessage("搜索产品失败: " + e.getMessage());
            return response;
        }
    }

    @Override
    public DeleteProductIndexDto.Response deleteProductIndex(DeleteProductIndexDto.Request request) {
        try {
            DocumentResult result = searchService.deleteDocument(
                PRODUCT_INDEX,
                request.getProductId().toString()
            );

            DeleteProductIndexDto.Response response = new DeleteProductIndexDto.Response();
            response.setSuccess(result.isSuccess());
            response.setIndex(result.getIndex());
            response.setId(result.getId());
            response.setErrorMessage(result.getErrorMessage());
            return response;

        } catch (Exception e) {
            log.error("删除产品索引文档失败: {}", request.getProductId(), e);
            DeleteProductIndexDto.Response response = new DeleteProductIndexDto.Response();
            response.setSuccess(false);
            response.setErrorMessage("删除产品索引文档失败: " + e.getMessage());
            return response;
        }
    }

    @Override
    public SuggestProductsDto.Response suggestProducts(SuggestProductsDto.Request request) {
        try {
            // 构建建议查询（使用 TermSuggester）
            io.nebula.search.core.query.SuggestQuery query = io.nebula.search.core.query.SuggestQuery.builder()
                .index(PRODUCT_INDEX)
                .addSuggester(
                    new TermSuggester("product-suggest", request.getText(), "name")
                        .suggestMode("popular")
                        .maxEdits(2)
                        .prefixLength(1)
                )
                .build();
            
            // 执行建议查询
            io.nebula.search.core.model.SuggestResult result = searchService.suggest(query);

            SuggestProductsDto.Response response = new SuggestProductsDto.Response();
            response.setSuccess(result.isSuccess());
            
            // 转换建议列表
            List<SuggestProductsDto.Suggestion> suggestions = new ArrayList<>();
            if (result.getSuggestions() != null) {
                result.getSuggestions().forEach((key, texts) -> {
                    texts.stream()
                        .limit(request.getSize())
                        .forEach(text -> {
                            SuggestProductsDto.Suggestion suggestion = new SuggestProductsDto.Suggestion();
                            suggestion.setText(text);
                            suggestion.setScore(1.0); // 简化评分
                            suggestions.add(suggestion);
                        });
                });
            }
            
            response.setSuggestions(suggestions);
            return response;
            
        } catch (Exception e) {
            log.error("获取搜索建议失败", e);
            SuggestProductsDto.Response response = new SuggestProductsDto.Response();
            response.setSuccess(false);
            response.setErrorMessage("获取搜索建议失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 构建查询条件（使用 QueryBuilder）
     */
    private QueryBuilder buildQuery(SearchProductsDto.Request dto) {
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();

        // 关键词搜索（name字段）
        if (dto.getKeyword() != null && !dto.getKeyword().isEmpty()) {
            MatchQueryBuilder matchQuery = new MatchQueryBuilder("name", dto.getKeyword())
                .operator("AND")
                .minimumShouldMatch("75%");
            boolQuery.must(matchQuery);
        }

        // 分类筛选
        if (dto.getCategory() != null) {
            boolQuery.filter(new TermQueryBuilder("category.keyword", dto.getCategory()));
        }

        // 状态筛选
        if (dto.getStatus() != null) {
            boolQuery.filter(new TermQueryBuilder("status", dto.getStatus()));
        }

        // 品牌筛选
        if (dto.getBrand() != null) {
            boolQuery.filter(new TermQueryBuilder("brand.keyword", dto.getBrand()));
        }

        // 标签筛选
        if (dto.getTags() != null && !dto.getTags().isEmpty()) {
            // 简化：使用第一个标签
            boolQuery.filter(new TermQueryBuilder("tags", dto.getTags().get(0)));
        }

        // 价格范围
        if (dto.getMinPrice() != null || dto.getMaxPrice() != null) {
            RangeQueryBuilder priceRange = new RangeQueryBuilder("price");
            if (dto.getMinPrice() != null) {
                priceRange.gte(dto.getMinPrice());
            }
            if (dto.getMaxPrice() != null) {
                priceRange.lte(dto.getMaxPrice());
            }
            boolQuery.filter(priceRange);
        }

        // 评分范围
        if (dto.getMinRating() != null || dto.getMaxRating() != null) {
            RangeQueryBuilder ratingRange = new RangeQueryBuilder("rating");
            if (dto.getMinRating() != null) {
                ratingRange.gte(dto.getMinRating());
            }
            if (dto.getMaxRating() != null) {
                ratingRange.lte(dto.getMaxRating());
            }
            boolQuery.filter(ratingRange);
        }

        // 销量范围
        if (dto.getMinSalesCount() != null || dto.getMaxSalesCount() != null) {
            RangeQueryBuilder salesRange = new RangeQueryBuilder("salesCount");
            if (dto.getMinSalesCount() != null) {
                salesRange.gte(dto.getMinSalesCount());
            }
            if (dto.getMaxSalesCount() != null) {
                salesRange.lte(dto.getMaxSalesCount());
            }
            boolQuery.filter(salesRange);
        }

        // 如果没有任何条件,返回match_all查询
        if (!boolQuery.hasClauses()) {
            return new MatchAllQueryBuilder();
        }

        return boolQuery;
    }

    /**
     * 将Product转换为ProductSearchDocument
     */
    private ProductSearchDocument convertToSearchDocument(Product product) {
        ProductSearchDocument doc = new ProductSearchDocument();
        doc.setId(product.getId());
        doc.setName(product.getName());
        doc.setDescription(product.getDescription());
        doc.setCategory(product.getCategory());
        doc.setPrice(product.getPrice());
        doc.setStockQuantity(product.getStockQuantity());
        doc.setStatus(product.getStatus());
        // 将LocalDateTime转换为字符串格式
        if (product.getCreateTime() != null) {
            doc.setCreateTime(product.getCreateTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (product.getUpdateTime() != null) {
            doc.setUpdateTime(product.getUpdateTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        return doc;
    }
}

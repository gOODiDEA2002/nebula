# Nebula Search Elasticsearch - 使用示例

> Elasticsearch全文搜索完整使用指南，以票务系统为例

## 目录

- [快速开始](#快速开始)
- [索引管理](#索引管理)
- [文档操作](#文档操作)
- [基础搜索](#基础搜索)
- [高级搜索](#高级搜索)
- [聚合分析](#聚合分析)
- [搜索建议](#搜索建议)
- [票务系统完整示例](#票务系统完整示例)
- [最佳实践](#最佳实践)

---

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-search-elasticsearch</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 基础配置

```yaml
nebula:
  search:
    elasticsearch:
      enabled: true
      uris:
        - http://localhost:9200  # Elasticsearch地址
      username: elastic
      password: changeme
      connection-timeout: 5s
      read-timeout: 30s
      index-prefix: "ticket-"    # 索引前缀
```

---

## 索引管理

### 1. 创建索引

```java
/**
 * 索引管理服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IndexManagementService {
    
    private final SearchService searchService;
    
    /**
     * 创建演出索引
     */
    public void createShowtimeIndex() {
        String indexName = "showtimes";
        
        // 定义索引映射
        IndexMapping mapping = IndexMapping.builder()
                .field("showtimeId", FieldType.LONG)
                .field("title", FieldType.TEXT, Map.of("analyzer", "ik_max_word"))
                .field("venue", FieldType.TEXT, Map.of("analyzer", "ik_max_word"))
                .field("performer", FieldType.TEXT, Map.of("analyzer", "ik_max_word"))
                .field("showTime", FieldType.DATE)
                .field("price", FieldType.DOUBLE)
                .field("category", FieldType.KEYWORD)
                .field("status", FieldType.KEYWORD)
                .field("tags", FieldType.KEYWORD)
                .field("description", FieldType.TEXT, Map.of("analyzer", "ik_max_word"))
                .field("location", FieldType.GEO_POINT)
                .build();
        
        // 创建索引
        searchService.createIndex(indexName, mapping);
        
        log.info("演出索引创建成功：{}", indexName);
    }
    
    /**
     * 删除索引
     */
    public void deleteIndex(String indexName) {
        searchService.deleteIndex(indexName);
        
        log.info("索引删除成功：{}", indexName);
    }
    
    /**
     * 检查索引是否存在
     */
    public boolean indexExists(String indexName) {
        return searchService.indexExists(indexName);
    }
}
```

### 2. 初始化票务系统所有索引

```java
/**
 * 初始化票务系统搜索索引
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketingIndexInitializer {
    
    private final SearchService searchService;
    
    /**
     * 初始化所有索引
     */
    @PostConstruct
    public void initializeIndexes() {
        createShowtimeIndex();
        createOrderIndex();
        createUserIndex();
        
        log.info("票务系统搜索索引初始化完成");
    }
    
    private void createShowtimeIndex() {
        String indexName = "showtimes";
        
        if (searchService.indexExists(indexName)) {
            log.info("演出索引已存在：{}", indexName);
            return;
        }
        
        IndexMapping mapping = IndexMapping.builder()
                .field("showtimeId", FieldType.LONG)
                .field("title", FieldType.TEXT, Map.of(
                        "analyzer", "ik_max_word",
                        "search_analyzer", "ik_smart"
                ))
                .field("venue", FieldType.TEXT, Map.of("analyzer", "ik_max_word"))
                .field("performer", FieldType.TEXT, Map.of("analyzer", "ik_max_word"))
                .field("showTime", FieldType.DATE)
                .field("price", FieldType.DOUBLE)
                .field("category", FieldType.KEYWORD)
                .field("status", FieldType.KEYWORD)
                .field("tags", FieldType.KEYWORD)
                .field("location", FieldType.GEO_POINT)
                .build();
        
        searchService.createIndex(indexName, mapping);
        
        log.info("演出索引创建成功：{}", indexName);
    }
    
    private void createOrderIndex() {
        String indexName = "orders";
        
        if (searchService.indexExists(indexName)) {
            log.info("订单索引已存在：{}", indexName);
            return;
        }
        
        IndexMapping mapping = IndexMapping.builder()
                .field("orderNo", FieldType.KEYWORD)
                .field("userId", FieldType.LONG)
                .field("showtimeId", FieldType.LONG)
                .field("status", FieldType.KEYWORD)
                .field("totalAmount", FieldType.DOUBLE)
                .field("createTime", FieldType.DATE)
                .field("payTime", FieldType.DATE)
                .build();
        
        searchService.createIndex(indexName, mapping);
        
        log.info("订单索引创建成功：{}", indexName);
    }
    
    private void createUserIndex() {
        String indexName = "users";
        
        if (searchService.indexExists(indexName)) {
            log.info("用户索引已存在：{}", indexName);
            return;
        }
        
        IndexMapping mapping = IndexMapping.builder()
                .field("userId", FieldType.LONG)
                .field("username", FieldType.KEYWORD)
                .field("email", FieldType.KEYWORD)
                .field("phone", FieldType.KEYWORD)
                .field("registerTime", FieldType.DATE)
                .build();
        
        searchService.createIndex(indexName, mapping);
        
        log.info("用户索引创建成功：{}", indexName);
    }
}
```

---

## 文档操作

### 1. 索引文档

```java
/**
 * 文档操作服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentOperationService {
    
    private final SearchService searchService;
    
    /**
     * 索引演出文档
     */
    public void indexShowtime(Showtime showtime) {
        String indexName = "showtimes";
        String documentId = showtime.getId().toString();
        
        // 构建文档
        ShowtimeDocument document = convertToDocument(showtime);
        
        // 索引文档
        searchService.indexDocument(indexName, documentId, document);
        
        log.info("演出文档已索引：showtimeId={}", showtime.getId());
    }
    
    /**
     * 批量索引演出文档
     */
    public void batchIndexShowtimes(List<Showtime> showtimes) {
        String indexName = "showtimes";
        
        Map<String, ShowtimeDocument> documents = showtimes.stream()
                .collect(Collectors.toMap(
                        showtime -> showtime.getId().toString(),
                        this::convertToDocument
                ));
        
        searchService.bulkIndexDocuments(indexName, documents);
        
        log.info("批量索引演出文档完成：共{}条", showtimes.size());
    }
    
    /**
     * 更新演出文档
     */
    public void updateShowtime(Long showtimeId, Map<String, Object> updates) {
        String indexName = "showtimes";
        String documentId = showtimeId.toString();
        
        searchService.updateDocument(indexName, documentId, updates);
        
        log.info("演出文档已更新：showtimeId={}", showtimeId);
    }
    
    /**
     * 删除演出文档
     */
    public void deleteShowtime(Long showtimeId) {
        String indexName = "showtimes";
        String documentId = showtimeId.toString();
        
        searchService.deleteDocument(indexName, documentId);
        
        log.info("演出文档已删除：showtimeId={}", showtimeId);
    }
    
    private ShowtimeDocument convertToDocument(Showtime showtime) {
        ShowtimeDocument document = new ShowtimeDocument();
        document.setShowtimeId(showtime.getId());
        document.setTitle(showtime.getTitle());
        document.setVenue(showtime.getVenue());
        document.setPerformer(showtime.getPerformer());
        document.setShowTime(showtime.getShowTime());
        document.setPrice(showtime.getPrice());
        document.setCategory(showtime.getCategory());
        document.setStatus(showtime.getStatus());
        document.setTags(Arrays.asList(showtime.getTags().split(",")));
        document.setDescription(showtime.getDescription());
        
        // 设置地理位置
        if (showtime.getLatitude() != null && showtime.getLongitude() != null) {
            document.setLocation(new GeoPoint(
                    showtime.getLatitude(), showtime.getLongitude()));
        }
        
        return document;
    }
}

/**
 * 演出文档
 */
@Data
public class ShowtimeDocument {
    private Long showtimeId;
    private String title;
    private String venue;
    private String performer;
    private LocalDateTime showTime;
    private BigDecimal price;
    private String category;
    private String status;
    private List<String> tags;
    private String description;
    private GeoPoint location;
}
```

---

## 基础搜索

### 1. 全文搜索

```java
/**
 * 演出搜索服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShowtimeSearchService {
    
    private final SearchService searchService;
    
    /**
     * 搜索演出（全文搜索）
     */
    public SearchResult<ShowtimeDocument> searchShowtimes(String keyword, int page, int size) {
        String indexName = "showtimes";
        
        // 构建搜索查询
        SearchQuery query = SearchQuery.builder()
                // 多字段匹配
                .should(MatchQueryBuilder.match("title", keyword).boost(3.0))    // 标题权重最高
                .should(MatchQueryBuilder.match("performer", keyword).boost(2.0)) // 表演者次之
                .should(MatchQueryBuilder.match("venue", keyword).boost(1.5))     // 场馆
                .should(MatchQueryBuilder.match("description", keyword))          // 描述
                // 只搜索可售状态
                .filter(TermQueryBuilder.term("status", "UPCOMING"))
                // 分页
                .from((page - 1) * size)
                .size(size)
                // 排序
                .sort("_score", SortOrder.DESC)  // 相关度排序
                .build();
        
        SearchResult<ShowtimeDocument> result = searchService.search(
                indexName, query, ShowtimeDocument.class);
        
        log.info("搜索演出：keyword={}, page={}, size={}, 结果数={}", 
                keyword, page, size, result.getHits().size());
        
        return result;
    }
    
    /**
     * 按分类搜索
     */
    public SearchResult<ShowtimeDocument> searchByCategory(String category, int page, int size) {
        String indexName = "showtimes";
        
        SearchQuery query = SearchQuery.builder()
                .must(TermQueryBuilder.term("category", category))
                .filter(TermQueryBuilder.term("status", "UPCOMING"))
                .from((page - 1) * size)
                .size(size)
                .sort("showTime", SortOrder.ASC)  // 按演出时间升序
                .build();
        
        return searchService.search(indexName, query, ShowtimeDocument.class);
    }
    
    /**
     * 按价格范围搜索
     */
    public SearchResult<ShowtimeDocument> searchByPriceRange(
            BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        
        String indexName = "showtimes";
        
        SearchQuery query = SearchQuery.builder()
                .must(RangeQueryBuilder.range("price")
                        .gte(minPrice)
                        .lte(maxPrice))
                .filter(TermQueryBuilder.term("status", "UPCOMING"))
                .from((page - 1) * size)
                .size(size)
                .sort("price", SortOrder.ASC)
                .build();
        
        return searchService.search(indexName, query, ShowtimeDocument.class);
    }
}
```

---

## 高级搜索

### 1. 多条件组合搜索

```java
/**
 * 高级搜索服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdvancedSearchService {
    
    private final SearchService searchService;
    
    /**
     * 高级演出搜索（多条件）
     */
    public SearchResult<ShowtimeDocument> advancedSearch(ShowtimeSearchRequest request) {
        String indexName = "showtimes";
        
        SearchQuery.Builder queryBuilder = SearchQuery.builder();
        
        // 1. 关键词搜索
        if (StringUtils.hasText(request.getKeyword())) {
            queryBuilder
                    .should(MatchQueryBuilder.match("title", request.getKeyword()).boost(3.0))
                    .should(MatchQueryBuilder.match("performer", request.getKeyword()).boost(2.0))
                    .should(MatchQueryBuilder.match("venue", request.getKeyword()).boost(1.5));
        }
        
        // 2. 分类筛选
        if (StringUtils.hasText(request.getCategory())) {
            queryBuilder.filter(TermQueryBuilder.term("category", request.getCategory()));
        }
        
        // 3. 标签筛选
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            queryBuilder.filter(TermsQueryBuilder.terms("tags", request.getTags()));
        }
        
        // 4. 价格范围
        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            RangeQueryBuilder priceRange = RangeQueryBuilder.range("price");
            if (request.getMinPrice() != null) {
                priceRange.gte(request.getMinPrice());
            }
            if (request.getMaxPrice() != null) {
                priceRange.lte(request.getMaxPrice());
            }
            queryBuilder.filter(priceRange);
        }
        
        // 5. 演出时间范围
        if (request.getStartTime() != null || request.getEndTime() != null) {
            RangeQueryBuilder timeRange = RangeQueryBuilder.range("showTime");
            if (request.getStartTime() != null) {
                timeRange.gte(request.getStartTime());
            }
            if (request.getEndTime() != null) {
                timeRange.lte(request.getEndTime());
            }
            queryBuilder.filter(timeRange);
        }
        
        // 6. 状态筛选
        queryBuilder.filter(TermQueryBuilder.term("status", "UPCOMING"));
        
        // 7. 分页
        queryBuilder
                .from((request.getPage() - 1) * request.getSize())
                .size(request.getSize());
        
        // 8. 排序
        if (StringUtils.hasText(request.getSortField())) {
            queryBuilder.sort(request.getSortField(), 
                    "asc".equalsIgnoreCase(request.getSortOrder()) 
                            ? SortOrder.ASC : SortOrder.DESC);
        } else {
            queryBuilder.sort("_score", SortOrder.DESC);  // 默认按相关度排序
        }
        
        SearchQuery query = queryBuilder.build();
        
        return searchService.search(indexName, query, ShowtimeDocument.class);
    }
}

/**
 * 演出搜索请求
 */
@Data
public class ShowtimeSearchRequest {
    private String keyword;
    private String category;
    private List<String> tags;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String sortField;
    private String sortOrder;
    private int page = 1;
    private int size = 10;
}
```

### 2. 地理位置搜索

```java
/**
 * 地理位置搜索服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeoSearchService {
    
    private final SearchService searchService;
    
    /**
     * 搜索附近的演出
     */
    public SearchResult<ShowtimeDocument> searchNearbyShowtimes(
            double latitude, double longitude, double radiusKm, int page, int size) {
        
        String indexName = "showtimes";
        
        SearchQuery query = SearchQuery.builder()
                // 地理位置距离过滤
                .filter(GeoDistanceQueryBuilder.geoDistance("location")
                        .point(latitude, longitude)
                        .distance(radiusKm, "km"))
                .filter(TermQueryBuilder.term("status", "UPCOMING"))
                .from((page - 1) * size)
                .size(size)
                // 按距离排序
                .sort(GeoDistanceSortBuilder.geoDistance("location")
                        .point(latitude, longitude)
                        .order(SortOrder.ASC))
                .build();
        
        SearchResult<ShowtimeDocument> result = searchService.search(
                indexName, query, ShowtimeDocument.class);
        
        log.info("搜索附近演出：lat={}, lon={}, radius={}km, 结果数={}", 
                latitude, longitude, radiusKm, result.getHits().size());
        
        return result;
    }
}
```

---

## 聚合分析

### 1. 分类统计

```java
/**
 * 聚合分析服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AggregationService {
    
    private final SearchService searchService;
    
    /**
     * 按分类聚合统计
     */
    public Map<String, Long> aggregateByCategory() {
        String indexName = "showtimes";
        
        SearchQuery query = SearchQuery.builder()
                .filter(TermQueryBuilder.term("status", "UPCOMING"))
                .size(0)  // 不返回文档，只返回聚合结果
                .aggregation(TermsAggregationBuilder.terms("category_agg", "category")
                        .size(10))  // 返回前10个分类
                .build();
        
        SearchResult<ShowtimeDocument> result = searchService.search(
                indexName, query, ShowtimeDocument.class);
        
        // 解析聚合结果
        Map<String, Long> categoryCount = new HashMap<>();
        Aggregation categoryAgg = result.getAggregations().get("category_agg");
        
        for (Bucket bucket : categoryAgg.getBuckets()) {
            categoryCount.put(bucket.getKey(), bucket.getDocCount());
        }
        
        log.info("分类统计：{}", categoryCount);
        
        return categoryCount;
    }
    
    /**
     * 价格区间统计
     */
    public Map<String, Long> aggregateByPriceRange() {
        String indexName = "showtimes";
        
        SearchQuery query = SearchQuery.builder()
                .filter(TermQueryBuilder.term("status", "UPCOMING"))
                .size(0)
                .aggregation(RangeAggregationBuilder.range("price_range_agg", "price")
                        .addRange("0-100", 0.0, 100.0)
                        .addRange("100-300", 100.0, 300.0)
                        .addRange("300-500", 300.0, 500.0)
                        .addRange("500+", 500.0, null))
                .build();
        
        SearchResult<ShowtimeDocument> result = searchService.search(
                indexName, query, ShowtimeDocument.class);
        
        // 解析聚合结果
        Map<String, Long> priceRangeCount = new HashMap<>();
        Aggregation priceRangeAgg = result.getAggregations().get("price_range_agg");
        
        for (Bucket bucket : priceRangeAgg.getBuckets()) {
            priceRangeCount.put(bucket.getKey(), bucket.getDocCount());
        }
        
        log.info("价格区间统计：{}", priceRangeCount);
        
        return priceRangeCount;
    }
    
    /**
     * 按月统计演出数量
     */
    public Map<String, Long> aggregateByMonth() {
        String indexName = "showtimes";
        
        SearchQuery query = SearchQuery.builder()
                .filter(TermQueryBuilder.term("status", "UPCOMING"))
                .size(0)
                .aggregation(DateHistogramAggregationBuilder
                        .dateHistogram("monthly_agg", "showTime")
                        .interval("month")
                        .format("yyyy-MM"))
                .build();
        
        SearchResult<ShowtimeDocument> result = searchService.search(
                indexName, query, ShowtimeDocument.class);
        
        // 解析聚合结果
        Map<String, Long> monthlyCount = new HashMap<>();
        Aggregation monthlyAgg = result.getAggregations().get("monthly_agg");
        
        for (Bucket bucket : monthlyAgg.getBuckets()) {
            monthlyCount.put(bucket.getKey(), bucket.getDocCount());
        }
        
        log.info("按月统计：{}", monthlyCount);
        
        return monthlyCount;
    }
}
```

---

## 搜索建议

### 1. 自动补全

```java
/**
 * 搜索建议服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SuggestionService {
    
    private final SearchService searchService;
    
    /**
     * 搜索建议（自动补全）
     */
    public List<String> suggest(String prefix) {
        String indexName = "showtimes";
        
        // 使用completion suggester
        SuggestQuery query = SuggestQuery.builder()
                .suggester("title_suggest", SuggesterBuilder.completion()
                        .field("title.completion")
                        .prefix(prefix)
                        .size(10))
                .build();
        
        SuggestResult result = searchService.suggest(indexName, query);
        
        List<String> suggestions = result.getSuggestions("title_suggest");
        
        log.info("搜索建议：prefix={}, 建议数={}", prefix, suggestions.size());
        
        return suggestions;
    }
    
    /**
     * 拼写纠错
     */
    public List<String> spellCheck(String text) {
        String indexName = "showtimes";
        
        // 使用phrase suggester
        SuggestQuery query = SuggestQuery.builder()
                .suggester("spell_check", SuggesterBuilder.phrase()
                        .field("title")
                        .text(text)
                        .size(5))
                .build();
        
        SuggestResult result = searchService.suggest(indexName, query);
        
        List<String> corrections = result.getSuggestions("spell_check");
        
        log.info("拼写纠错：text={}, 纠正数={}", text, corrections.size());
        
        return corrections;
    }
}
```

---

## 票务系统完整示例

### 完整的搜索服务

```java
/**
 * 票务搜索服务（完整示例）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketingSearchService {
    
    private final SearchService searchService;
    private final ShowtimeMapper showtimeMapper;
    
    /**
     * 1. 同步演出数据到Elasticsearch
     */
    @Scheduled(cron = "0 0 */6 * * ?")  // 每6小时同步一次
    public void syncShowtimesToES() {
        log.info("开始同步演出数据到Elasticsearch");
        
        // 1. 从数据库查询所有演出
        List<Showtime> showtimes = showtimeMapper.selectAll();
        
        // 2. 转换为ES文档
        Map<String, ShowtimeDocument> documents = showtimes.stream()
                .collect(Collectors.toMap(
                        showtime -> showtime.getId().toString(),
                        this::convertToDocument
                ));
        
        // 3. 批量索引
        searchService.bulkIndexDocuments("showtimes", documents);
        
        log.info("演出数据同步完成：共{}条", showtimes.size());
    }
    
    /**
     * 2. 综合搜索（支持多种搜索模式）
     */
    public ShowtimeSearchResponse comprehensiveSearch(ShowtimeSearchRequest request) {
        // 2.1 全文搜索
        SearchResult<ShowtimeDocument> searchResult = advancedSearch(request);
        
        // 2.2 聚合分析
        Map<String, Long> categoryCount = aggregateByCategory();
        Map<String, Long> priceRangeCount = aggregateByPriceRange();
        
        // 2.3 搜索建议
        List<String> suggestions = suggest(request.getKeyword());
        
        // 2.4 组装响应
        ShowtimeSearchResponse response = new ShowtimeSearchResponse();
        response.setShowtimes(searchResult.getHits());
        response.setTotal(searchResult.getTotal());
        response.setCategoryCount(categoryCount);
        response.setPriceRangeCount(priceRangeCount);
        response.setSuggestions(suggestions);
        response.setPage(request.getPage());
        response.setSize(request.getSize());
        
        return response;
    }
    
    /**
     * 3. 热门演出推荐
     */
    public List<ShowtimeDocument> getHotShowtimes(int limit) {
        String indexName = "showtimes";
        
        SearchQuery query = SearchQuery.builder()
                .filter(TermQueryBuilder.term("status", "UPCOMING"))
                .size(limit)
                // 按热度排序（假设有hot_score字段）
                .sort("hot_score", SortOrder.DESC)
                .build();
        
        SearchResult<ShowtimeDocument> result = searchService.search(
                indexName, query, ShowtimeDocument.class);
        
        return result.getHits();
    }
    
    /**
     * 4. 个性化推荐（基于用户历史）
     */
    public List<ShowtimeDocument> getPersonalizedRecommendations(Long userId, int limit) {
        // 1. 获取用户历史偏好
        UserPreference preference = getUserPreference(userId);
        
        // 2. 构建推荐查询
        SearchQuery query = SearchQuery.builder()
                // 偏好的分类
                .should(TermsQueryBuilder.terms("category", preference.getFavoriteCategories())
                        .boost(2.0))
                // 偏好的标签
                .should(TermsQueryBuilder.terms("tags", preference.getFavoriteTags())
                        .boost(1.5))
                // 偏好的价格范围
                .should(RangeQueryBuilder.range("price")
                        .gte(preference.getMinPrice())
                        .lte(preference.getMaxPrice()))
                .filter(TermQueryBuilder.term("status", "UPCOMING"))
                .size(limit)
                .sort("_score", SortOrder.DESC)
                .build();
        
        SearchResult<ShowtimeDocument> result = searchService.search(
                "showtimes", query, ShowtimeDocument.class);
        
        log.info("个性化推荐：userId={}, 推荐数={}", userId, result.getHits().size());
        
        return result.getHits();
    }
    
    /**
     * 5. 相似演出推荐
     */
    public List<ShowtimeDocument> getSimilarShowtimes(Long showtimeId, int limit) {
        // 1. 获取当前演出
        ShowtimeDocument current = getCurrentShowtime(showtimeId);
        
        // 2. 构建相似查询
        SearchQuery query = SearchQuery.builder()
                // 排除当前演出
                .mustNot(TermQueryBuilder.term("showtimeId", showtimeId))
                // 相同分类
                .should(TermQueryBuilder.term("category", current.getCategory())
                        .boost(3.0))
                // 相同标签
                .should(TermsQueryBuilder.terms("tags", current.getTags())
                        .boost(2.0))
                // 相似价格
                .should(RangeQueryBuilder.range("price")
                        .gte(current.getPrice().multiply(new BigDecimal("0.7")))
                        .lte(current.getPrice().multiply(new BigDecimal("1.3"))))
                .filter(TermQueryBuilder.term("status", "UPCOMING"))
                .size(limit)
                .sort("_score", SortOrder.DESC)
                .build();
        
        SearchResult<ShowtimeDocument> result = searchService.search(
                "showtimes", query, ShowtimeDocument.class);
        
        log.info("相似演出推荐：showtimeId={}, 推荐数={}", showtimeId, result.getHits().size());
        
        return result.getHits();
    }
    
    // 辅助方法
    
    private ShowtimeDocument convertToDocument(Showtime showtime) {
        ShowtimeDocument document = new ShowtimeDocument();
        document.setShowtimeId(showtime.getId());
        document.setTitle(showtime.getTitle());
        document.setVenue(showtime.getVenue());
        document.setPerformer(showtime.getPerformer());
        document.setShowTime(showtime.getShowTime());
        document.setPrice(showtime.getPrice());
        document.setCategory(showtime.getCategory());
        document.setStatus(showtime.getStatus());
        document.setTags(Arrays.asList(showtime.getTags().split(",")));
        document.setDescription(showtime.getDescription());
        
        if (showtime.getLatitude() != null && showtime.getLongitude() != null) {
            document.setLocation(new GeoPoint(
                    showtime.getLatitude(), showtime.getLongitude()));
        }
        
        return document;
    }
    
    private SearchResult<ShowtimeDocument> advancedSearch(ShowtimeSearchRequest request) {
        // 实现高级搜索逻辑（参考前面的示例）
        return null;
    }
    
    private Map<String, Long> aggregateByCategory() {
        // 实现分类聚合（参考前面的示例）
        return null;
    }
    
    private Map<String, Long> aggregateByPriceRange() {
        // 实现价格区间聚合（参考前面的示例）
        return null;
    }
    
    private List<String> suggest(String keyword) {
        // 实现搜索建议（参考前面的示例）
        return null;
    }
    
    private UserPreference getUserPreference(Long userId) {
        // 获取用户偏好（从缓存或数据库）
        return new UserPreference();
    }
    
    private ShowtimeDocument getCurrentShowtime(Long showtimeId) {
        String indexName = "showtimes";
        String documentId = showtimeId.toString();
        
        return searchService.getDocument(indexName, documentId, ShowtimeDocument.class);
    }
}

/**
 * 演出搜索响应
 */
@Data
public class ShowtimeSearchResponse {
    private List<ShowtimeDocument> showtimes;
    private Long total;
    private Map<String, Long> categoryCount;
    private Map<String, Long> priceRangeCount;
    private List<String> suggestions;
    private int page;
    private int size;
}

/**
 * 用户偏好
 */
@Data
public class UserPreference {
    private List<String> favoriteCategories;
    private List<String> favoriteTags;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
```

---

## 最佳实践

### 1. 索引设计

- **字段类型选择**：
  - `KEYWORD`：精确匹配（ID、状态、分类）
  - `TEXT`：全文搜索（标题、描述）
  - `DATE`：日期时间
  - `DOUBLE`：价格、评分
  - `GEO_POINT`：地理位置

- **中文分词**：使用`ik_max_word`分词器

### 2. 查询优化

- **使用filter代替query**：filter不计算相关度，可以缓存
- **合理设置boost**：调整字段权重
- **避免深度分页**：使用`search_after`或scroll

### 3. 数据同步

- **增量同步**：监听数据库变更，实时同步
- **全量同步**：定时全量同步，确保数据一致性
- **双写策略**：更新数据库的同时更新ES

### 4. 性能优化

- **分片和副本**：合理设置分片数和副本数
- **批量操作**：使用bulk API批量索引
- **缓存查询结果**：热门查询结果缓存到Redis

### 5. 监控和维护

- **索引大小监控**：定期检查索引大小
- **慢查询监控**：监控慢查询并优化
- **定期清理**：删除过期数据

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [CONFIG.md](./CONFIG.md) - 配置指南
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 发展路线图

---

**最后更新**: 2025-11-20  
**文档版本**: v2.0

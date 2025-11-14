# nebula-search-elasticsearch 模块单元测试清单

## 模块说明

Elasticsearch搜索服务实现模块，提供文档管理、搜索、聚合、建议等完整的搜索功能。

## 核心功能

1. 文档操作（索引、获取、更新、删除）
2. 搜索操作（全文搜索、高级搜索）
3. 批量操作
4. 聚合分析
5. 自动建议
6. 索引管理
7. 滚动查询

## 测试类清单

### 1. DocumentOperationsTest

**测试类路径**: `io.nebula.search.elasticsearch.ElasticsearchSearchService`  
**测试目的**: 验证文档的基本CRUD操作

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testIndexDocument() | indexDocument(String, String, Object) | 测试索引文档 | ElasticsearchClient |
| testGetDocument() | getDocument(String, String, Class) | 测试获取文档 | ElasticsearchClient |
| testUpdateDocument() | updateDocument(String, String, Object) | 测试更新文档 | ElasticsearchClient |
| testDeleteDocument() | deleteDocument(String, String) | 测试删除文档 | ElasticsearchClient |
| testDocumentExists() | existsDocument(String, String) | 测试文档是否存在 | ElasticsearchClient |

**测试数据准备**:
- Mock ElasticsearchClient
- 准备测试文档对象

**验证要点**:
- 文档正确索引
- 文档正确获取
- 更新操作生效
- 删除操作生效

**Mock示例**:
```java
@Mock
private ElasticsearchClient esClient;

@InjectMocks
private ElasticsearchSearchService searchService;

@Test
void testIndexDocument() throws IOException {
    Map<String, Object> document = Map.of(
        "title", "测试文档",
        "content", "这是测试内容"
    );
    
    IndexResponse mockResponse = mock(IndexResponse.class);
    when(mockResponse.result()).thenReturn(Result.Created);
    
    when(esClient.index(any(IndexRequest.class)))
        .thenReturn(mockResponse);
    
    DocumentResult result = searchService.indexDocument("docs", "1", document);
    
    assertThat(result.isSuccess()).isTrue();
    verify(esClient).index(any(IndexRequest.class));
}
```

---

### 2. SearchOperationsTest

**测试类路径**: `io.nebula.search.elasticsearch.ElasticsearchSearchService`  
**测试目的**: 验证搜索功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testBasicSearch() | search(SearchQuery, Class) | 测试基础搜索 | ElasticsearchClient |
| testSearchWithSort() | search() | 测试带排序的搜索 | ElasticsearchClient |
| testSearchWithHighlight() | search() | 测试带高亮的搜索 | ElasticsearchClient |
| testSearchWithPagination() | search() | 测试分页搜索 | ElasticsearchClient |
| testSearchNoResults() | search() | 测试无结果搜索 | ElasticsearchClient |

**测试数据准备**:
- Mock ElasticsearchClient
- 准备SearchQuery对象
- 准备模拟搜索结果

**验证要点**:
- 查询条件正确构建
- 搜索结果正确解析
- 分页参数正确
- 高亮字段正确

**Mock示例**:
```java
@Test
void testBasicSearch() throws IOException {
    SearchQuery query = SearchQuery.builder()
        .index("docs")
        .query(Map.of("query", "测试"))
        .from(0)
        .size(10)
        .build();
    
    // Mock搜索响应
    SearchResponse<Map> mockResponse = mock(SearchResponse.class);
    HitsMetadata<Map> hitsMetadata = mock(HitsMetadata.class);
    
    when(hitsMetadata.total()).thenReturn(TotalHits.of(1L, TotalHitsRelation.Eq));
    when(mockResponse.hits()).thenReturn(hitsMetadata);
    
    when(esClient.search(any(SearchRequest.class), eq(Map.class)))
        .thenReturn(mockResponse);
    
    SearchResult<Map> result = searchService.search(query, Map.class);
    
    assertThat(result.getTotalHits()).isEqualTo(1);
    verify(esClient).search(any(SearchRequest.class), eq(Map.class));
}
```

---

### 3. BulkOperationsTest

**测试类路径**: `io.nebula.search.elasticsearch.ElasticsearchSearchService`  
**测试目的**: 验证批量操作功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testBulkIndexDocuments() | bulkIndexDocuments(String, Map) | 测试批量索引 | ElasticsearchClient |
| testBulkUpdateDocuments() | bulkUpdateDocuments(String, Map) | 测试批量更新 | ElasticsearchClient |
| testBulkDeleteDocuments() | bulkDeleteDocuments(String, List) | 测试批量删除 | ElasticsearchClient |
| testBulkWithPartialFailure() | - | 测试部分失败的批量操作 | ElasticsearchClient |

**测试数据准备**:
- Mock ElasticsearchClient
- 准备批量操作的文档集合

**验证要点**:
- 批量操作正确执行
- 失败的操作正确记录
- 成功/失败统计正确

**Mock示例**:
```java
@Test
void testBulkIndexDocuments() throws IOException {
    Map<String, Map<String, Object>> documents = Map.of(
        "1", Map.of("title", "文档1"),
        "2", Map.of("title", "文档2")
    );
    
    BulkResponse mockResponse = mock(BulkResponse.class);
    when(mockResponse.errors()).thenReturn(false);
    
    when(esClient.bulk(any(BulkRequest.class)))
        .thenReturn(mockResponse);
    
    BulkResult result = searchService.bulkIndexDocuments("docs", documents);
    
    assertThat(result.hasErrors()).isFalse();
    verify(esClient).bulk(any(BulkRequest.class));
}
```

---

### 4. AggregationTest

**测试类路径**: `io.nebula.search.elasticsearch.ElasticsearchSearchService`  
**测试目的**: 验证聚合分析功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testTermsAggregation() | aggregate(AggregationQuery) | 测试分类聚合 | ElasticsearchClient |
| testMetricsAggregation() | aggregate() | 测试指标聚合 | ElasticsearchClient |
| testDateHistogramAggregation() | aggregate() | 测试日期直方图聚合 | ElasticsearchClient |

**测试数据准备**:
- Mock ElasticsearchClient
- 准备AggregationQuery对象

**验证要点**:
- 聚合查询正确构建
- 聚合结果正确解析

---

### 5. SuggestTest

**测试类路径**: `io.nebula.search.elasticsearch.ElasticsearchSearchService`  
**测试目的**: 验证自动建议功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testCompletionSuggest() | suggest(SuggestQuery) | 测试自动完成建议 | ElasticsearchClient |
| testTermSuggest() | suggest() | 测试词项建议 | ElasticsearchClient |
| testPhraseSuggest() | suggest() | 测试短语建议 | ElasticsearchClient |

**测试数据准备**:
- Mock ElasticsearchClient
- 准备SuggestQuery对象

**验证要点**:
- 建议查询正确构建
- 建议结果正确返回

---

### 6. IndexManagementTest

**测试类路径**: `io.nebula.search.elasticsearch.ElasticsearchSearchService`  
**测试目的**: 验证索引管理功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testCreateIndex() | createIndex(String, IndexMapping) | 测试创建索引 | ElasticsearchClient |
| testDeleteIndex() | deleteIndex(String) | 测试删除索引 | ElasticsearchClient |
| testExistsIndex() | existsIndex(String) | 测试检查索引是否存在 | ElasticsearchClient |
| testGetIndexInfo() | getIndexInfo(String) | 测试获取索引信息 | ElasticsearchClient |
| testUpdateIndexMapping() | - | 测试更新索引映射 | ElasticsearchClient |

**测试数据准备**:
- Mock ElasticsearchClient
- 准备IndexMapping对象

**验证要点**:
- 索引正确创建
- 映射定义正确
- 索引正确删除

**Mock示例**:
```java
@Test
void testCreateIndex() throws IOException {
    IndexMapping mapping = IndexMapping.builder()
        .mappings(Map.of(
            "properties", Map.of(
                "title", Map.of("type", "text"),
                "created_at", Map.of("type", "date")
            )
        ))
        .build();
    
    CreateIndexResponse mockResponse = mock(CreateIndexResponse.class);
    when(mockResponse.acknowledged()).thenReturn(true);
    
    when(esClient.indices().create(any(CreateIndexRequest.class)))
        .thenReturn(mockResponse);
    
    boolean success = searchService.createIndex("docs", mapping);
    
    assertThat(success).isTrue();
    verify(esClient.indices()).create(any(CreateIndexRequest.class));
}
```

---

### 7. ScrollQueryTest

**测试类路径**: `io.nebula.search.elasticsearch.ElasticsearchSearchService`  
**测试目的**: 验证滚动查询功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testScrollSearch() | search(SearchQuery) | 测试初始滚动查询 | ElasticsearchClient |
| testScrollNext() | scroll(String, String) | 测试获取下一批数据 | ElasticsearchClient |
| testClearScroll() | clearScroll(String) | 测试清理滚动上下文 | ElasticsearchClient |

**测试数据准备**:
- Mock ElasticsearchClient
- 准备滚动查询参数

**验证要点**:
- ScrollId正确返回
- 分批获取数据正确
- 滚动上下文正确清理

---

## Mock策略

### 需要Mock的对象

| Mock对象 | 使用场景 | Mock行为 |
|---------|-----------|---------|
| ElasticsearchClient | 所有ES操作 | Mock index(), search(), bulk() |
| IndexResponse | 索引响应 | Mock result() |
| SearchResponse | 搜索响应 | Mock hits() |
| BulkResponse | 批量响应 | Mock errors() |

### 不需要真实Elasticsearch
**所有测试都应该Mock ElasticsearchClient，不需要启动真实的Elasticsearch服务器**。

---

## 测试依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>co.elastic.clients</groupId>
        <artifactId>elasticsearch-java</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 测试执行

```bash
mvn test -pl nebula/infrastructure/search/nebula-search-elasticsearch
```

---

## 验收标准

- 所有测试方法通过
- 核心功能测试覆盖率 >= 90%
- 文档CRUD操作测试通过
- 搜索和聚合测试通过
- 批量操作和索引管理测试通过


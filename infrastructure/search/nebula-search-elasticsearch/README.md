# Nebula Search Elasticsearch

Nebula 框架的 Elasticsearch 搜索服务实现模块。

## 功能特性

- **完整的搜索功能**：支持全文搜索、精确匹配、模糊查询等
- **文档管理**：支持文档的索引、获取、更新、删除操作
- **批量操作**：支持批量索引、批量更新等高效操作
- **滚动查询**：支持大量数据的分页和滚动查询
- **聚合分析**：支持多种聚合统计分析
- **自动建议**：支持搜索建议和自动完成功能
- **索引管理**：支持索引的创建、删除、映射管理
- **高可用配置**：支持集群配置、SSL 加密、认证等
- **Spring Boot 集成**：提供自动配置和 Starter 支持

## 依赖要求

- Java 21+
- Spring Boot 3.x
- Elasticsearch 8.x+
- Nebula Search Core

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-search-elasticsearch</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置 Elasticsearch

```yaml
nebula:
  search:
    elasticsearch:
      enabled: true
      uris:
        - http://localhost:9200
      username: elastic
      password: changeme
      connection-timeout: 10s
      read-timeout: 30s
      index-prefix: myapp
      default-shards: 1
      default-replicas: 1
```

### 3. 使用搜索服务

```java
@Service
public class ProductSearchService {
    
    @Autowired
    private SearchService searchService;
    
    public void indexProduct(Product product) {
        SearchDocument<Product> document = SearchDocument.<Product>builder()
            .id(product.getId().toString())
            .source(product)
            .build();
            
        searchService.indexDocument("products", product.getId().toString(), document);
    }
    
    public SearchResult<Product> searchProducts(String query) {
        SearchQuery searchQuery = SearchQuery.builder()
            .index("products")
            .query(Map.of("query", query))
            .from(0)
            .size(10)
            .build();
            
        return searchService.search(searchQuery, Product.class);
    }
}
```

## 配置说明

### 基础配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `nebula.search.elasticsearch.enabled` | `true` | 是否启用 Elasticsearch |
| `nebula.search.elasticsearch.uris` | `["http://localhost:9200"]` | Elasticsearch 节点地址 |
| `nebula.search.elasticsearch.username` | - | 用户名 |
| `nebula.search.elasticsearch.password` | - | 密码 |

### 连接配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `nebula.search.elasticsearch.connection-timeout` | `10s` | 连接超时时间 |
| `nebula.search.elasticsearch.read-timeout` | `30s` | 读取超时时间 |
| `nebula.search.elasticsearch.max-connections` | `100` | 最大连接数 |
| `nebula.search.elasticsearch.max-connections-per-route` | `10` | 每个路由的最大连接数 |

### 索引配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `nebula.search.elasticsearch.index-prefix` | `nebula` | 索引名前缀 |
| `nebula.search.elasticsearch.default-shards` | `1` | 默认分片数 |
| `nebula.search.elasticsearch.default-replicas` | `1` | 默认副本数 |

### 操作配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `nebula.search.elasticsearch.bulk-size` | `1000` | 批量操作大小 |
| `nebula.search.elasticsearch.bulk-timeout` | `30s` | 批量操作超时 |
| `nebula.search.elasticsearch.search-timeout` | `30s` | 搜索超时时间 |
| `nebula.search.elasticsearch.scroll-timeout` | `1m` | 滚动查询超时 |
| `nebula.search.elasticsearch.scroll-size` | `1000` | 滚动查询大小 |

### SSL 配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `nebula.search.elasticsearch.ssl-enabled` | `false` | 是否启用 SSL |
| `nebula.search.elasticsearch.ssl-verification-enabled` | `true` | 是否验证 SSL 证书 |
| `nebula.search.elasticsearch.ssl-certificate-path` | - | SSL 证书路径 |
| `nebula.search.elasticsearch.ssl-key-path` | - | SSL 密钥路径 |
| `nebula.search.elasticsearch.ssl-ca-path` | - | SSL CA 证书路径 |

## 使用示例

### 文档操作

```java
// 索引文档
Map<String, Object> docData = Map.of("title", "测试文档", "content", "这是一个测试文档");
DocumentResult result = searchService.indexDocument("docs", "1", docData);

// 获取文档
Map<String, Object> document = searchService.getDocument("docs", "1", Map.class);

// 更新文档
Map<String, Object> updateData = Map.of("title", "更新后的标题");
DocumentResult result = searchService.updateDocument("docs", "1", updateData);

// 删除文档
DocumentResult result = searchService.deleteDocument("docs", "1");
```

### 搜索操作

```java
// 基础搜索
SearchQuery query = SearchQuery.builder()
    .index("docs")
    .query(Map.of("query", "测试"))
    .from(0)
    .size(10)
    .build();
SearchResult<Map> result = searchService.search(query, Map.class);

// 高级搜索（带排序和高亮）
SearchQuery query = SearchQuery.builder()
    .index("docs")
    .query(Map.of("query", "测试"))
    .from(0)
    .size(10)
    .sort(List.of(
        Map.of("_score", Map.of("order", "desc")),
        Map.of("created_at", Map.of("order", "desc"))
    ))
    .highlight(Map.of(
        "fields", Map.of("title", Map.of(), "content", Map.of())
    ))
    .source(List.of("title", "content"))
    .build();
SearchResult<Map> result = searchService.search(query, Map.class);
```

### 批量操作

```java
// 批量索引
Map<String, Map<String, Object>> documents = Map.of(
    "1", Map.of("title", "文档1"),
    "2", Map.of("title", "文档2")
);
BulkResult result = searchService.bulkIndexDocuments("docs", documents);
```

### 聚合分析

```java
// 分类聚合
AggregationQuery query = AggregationQuery.builder()
    .index("products")
    .name("category_stats")
    .field("category")
    .size(10)
    .build();
AggregationResult result = searchService.aggregate(query);
```

### 自动建议

```java
// 自动完成建议
SuggestQuery query = SuggestQuery.builder()
    .index("products")
    .name("product_suggest")
    .field("suggest")
    .text("手机")
    .type(SuggestQuery.SuggestType.COMPLETION)
    .size(5)
    .build();
SuggestResult result = searchService.suggest(query);
```

### 索引管理

```java
// 创建索引
IndexMapping mapping = IndexMapping.builder()
    .mappings(Map.of(
        "properties", Map.of(
            "title", Map.of("type", "text"),
            "content", Map.of("type", "text"),
            "created_at", Map.of("type", "date")
        )
    ))
    .build();
boolean success = searchService.createIndex("docs", mapping);

// 检查索引是否存在
boolean exists = searchService.existsIndex("docs");

// 获取索引信息
IndexInfo info = searchService.getIndexInfo("docs");

// 删除索引
boolean success = searchService.deleteIndex("docs");
```

### 滚动查询

```java
// 开始滚动查询
SearchQuery query = SearchQuery.builder()
    .index("docs")
    .query("*")
    .size(100)
    .build();
SearchResult result = searchService.search(query);

// 继续滚动
if (result.getScrollId() != null) {
    ScrollResult scrollResult = searchService.scroll(result.getScrollId(), "1m");
    // 处理结果...
    
    // 清理滚动上下文
    searchService.clearScroll(result.getScrollId());
}
```

## 集群配置

### 多节点配置

```yaml
nebula:
  search:
    elasticsearch:
      uris:
        - http://es-node1:9200
        - http://es-node2:9200
        - http://es-node3:9200
      max-connections: 300
      max-connections-per-route: 100
```

### SSL 安全配置

```yaml
nebula:
  search:
    elasticsearch:
      ssl-enabled: true
      ssl-verification-enabled: true
      ssl-ca-path: /path/to/ca.crt
      ssl-certificate-path: /path/to/client.crt
      ssl-key-path: /path/to/client.key
```

### 认证配置

```yaml
nebula:
  search:
    elasticsearch:
      username: elastic
      password: ${ELASTICSEARCH_PASSWORD}
```

## 性能优化

### 批量操作优化

```yaml
nebula:
  search:
    elasticsearch:
      bulk-size: 5000        # 增加批量大小
      bulk-timeout: 60s      # 增加超时时间
```

### 连接池优化

```yaml
nebula:
  search:
    elasticsearch:
      max-connections: 500
      max-connections-per-route: 50
      connection-timeout: 5s
      read-timeout: 120s
```

### 搜索优化

```yaml
nebula:
  search:
    elasticsearch:
      search-timeout: 10s    # 减少搜索超时
      scroll-size: 5000      # 增加滚动大小
```

## 监控和日志

### 启用调试日志

```yaml
logging:
  level:
    io.nebula.search.elasticsearch: DEBUG
    org.elasticsearch.client: DEBUG
```

### 性能监控

该模块会自动记录关键操作的性能指标，包括：

- 搜索响应时间
- 索引操作耗时
- 批量操作统计
- 连接池状态

## 故障排除

### 常见问题

1. **连接失败**
   - 检查 Elasticsearch 服务是否启动
   - 验证网络连接和防火墙设置
   - 确认认证信息正确

2. **索引操作失败**
   - 检查索引名称是否符合规范
   - 验证文档结构是否符合映射定义
   - 确认磁盘空间充足

3. **搜索性能问题**
   - 优化查询语句
   - 检查索引分片设置
   - 考虑使用缓存

4. **SSL 连接问题**
   - 验证证书路径和权限
   - 检查证书有效性
   - 确认 SSL 配置正确

### 调试技巧

1. 启用详细日志记录
2. 使用 Elasticsearch 的监控 API
3. 检查集群健康状态
4. 分析慢查询日志

## 注意事项

1. **索引命名**：索引名称会自动添加配置的前缀
2. **类型映射**：建议在创建索引时明确指定字段映射
3. **分片设置**：根据数据量和查询模式合理设置分片数
4. **版本兼容**：确保客户端版本与服务端版本兼容
5. **资源管理**：及时清理不需要的滚动查询上下文

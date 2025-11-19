# Nebula Search Core 模块

## 模块简介

`nebula-search-core` 是 Nebula 框架的搜索服务核心抽象层。它提供了一套独立于具体搜索引擎（如 Elasticsearch、OpenSearch、Solr）的统一 API，用于索引管理、文档操作、查询构建和聚合分析。

## 核心组件

### 1. SearchService - 搜索服务接口

定义了核心的搜索和索引操作。

```java
public interface SearchService {
    // 索引操作
    boolean createIndex(String indexName, IndexMapping mapping);
    boolean deleteIndex(String indexName);
    boolean indexExists(String indexName);
    
    // 文档操作
    <T> DocumentResult indexDocument(String indexName, String id, T document);
    <T> T getDocument(String indexName, String id, Class<T> clazz);
    boolean deleteDocument(String indexName, String id);
    
    // 搜索操作
    <T> SearchResult<T> search(SearchQuery query, Class<T> clazz);
    
    // 聚合操作
    AggregationResult aggregate(AggregationQuery query);
    
    // 建议操作
    SuggestResult suggest(SuggestQuery query);
}
```

### 2. 查询构建器 (Query Builders)

提供了一套类型安全的查询构建 API。

- **SearchQuery**: 主查询对象，包含过滤、排序、分页、高亮等。
- **QueryBuilder**: 具体查询条件的构建接口（如 Match, Term, Range, Bool）。

### 3. 模型对象 (Models)

- **IndexMapping**: 定义索引的字段结构和类型。
- **SearchResult**: 封装搜索结果，包含命中列表、总数、高亮片段等。
- **AggregationResult**: 封装聚合分析结果。

## 扩展性

本模块仅包含接口和抽象类。具体的实现（如 `nebula-search-elasticsearch`）需要依赖本模块并实现 `SearchService` 接口。

## 依赖说明

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-search-core</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

## 版本要求

- Java 21+


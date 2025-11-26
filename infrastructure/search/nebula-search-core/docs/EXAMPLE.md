# nebula-search-core 模块示例

## 模块简介

`nebula-search-core` 模块定义了 Nebula 框架的搜索服务核心抽象。它提供了一套统一的 API，用于索引管理、文档操作、复杂查询构建、聚合分析和自动补全，旨在屏蔽底层搜索引擎（主要是 Elasticsearch）的复杂性。

核心组件包括：
- **SearchService**: 统一的搜索服务接口。
- **SearchQuery**: 搜索查询构建器。
- **AggregationQuery**: 聚合查询构建器。
- **SuggestQuery**: 建议查询构建器。
- **IndexMapping**: 索引结构定义。

## 核心功能示例

### 1. 创建索引与映射

定义索引结构并创建。

**`io.nebula.example.search.service.ProductSearchService`**:

```java
package io.nebula.example.search.service;

import io.nebula.search.core.SearchService;
import io.nebula.search.core.model.IndexMapping;
import io.nebula.search.core.model.IndexResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final SearchService searchService;
    private final String INDEX_NAME = "products";

    public void initIndex() {
        if (!searchService.indexExists(INDEX_NAME)) {
            IndexMapping mapping = new IndexMapping();
            Map<String, Object> properties = new HashMap<>();
            
            // 定义字段类型
            properties.put("id", Map.of("type", "keyword"));
            properties.put("name", Map.of("type", "text", "analyzer", "ik_max_word")); // 假设支持 IK 分词
            properties.put("price", Map.of("type", "double"));
            properties.put("category", Map.of("type", "keyword"));
            properties.put("tags", Map.of("type", "keyword"));
            properties.put("createTime", Map.of("type", "date"));
            
            mapping.setProperties(properties);
            
            IndexResult result = searchService.createIndex(INDEX_NAME, mapping);
            log.info("索引创建结果: {}", result.isSuccess());
        }
    }
}
```

### 2. 索引文档

添加或更新文档。

```java
public void indexProduct(Product product) {
    // indexDocument(indexName, id, document)
    searchService.indexDocument(INDEX_NAME, product.getId().toString(), product);
}
```

### 3. 构建复杂查询

使用 `SearchQuery` 构建查询条件。

```java
import io.nebula.search.core.query.SearchQuery;
import io.nebula.search.core.query.builder.BoolQueryBuilder;
import io.nebula.search.core.query.builder.MatchQueryBuilder;
import io.nebula.search.core.query.builder.RangeQueryBuilder;
import io.nebula.search.core.query.builder.TermQueryBuilder;
import io.nebula.search.core.model.SearchResult;

public SearchResult<Product> searchProducts(String keyword, Double minPrice, String category) {
    
    BoolQueryBuilder boolQuery = new BoolQueryBuilder();
    
    // 关键词匹配
    if (keyword != null && !keyword.isEmpty()) {
        boolQuery.must(new MatchQueryBuilder("name", keyword));
    }
    
    // 价格范围
    if (minPrice != null) {
        boolQuery.filter(new RangeQueryBuilder("price").gte(minPrice));
    }
    
    // 分类筛选
    if (category != null) {
        boolQuery.filter(new TermQueryBuilder("category", category));
    }
    
    SearchQuery query = SearchQuery.builder()
            .indexName(INDEX_NAME)
            .query(boolQuery)
            .page(1)
            .size(20)
            .sort("createTime", "desc") // 排序
            .highlight("name") // 高亮字段
            .build();
            
    return searchService.search(query, Product.class);
}
```

### 4. 聚合分析

统计各分类下的商品数量。

```java
import io.nebula.search.core.query.AggregationQuery;
import io.nebula.search.core.aggregation.TermsAggregation;
import io.nebula.search.core.model.AggregationResult;

public Map<String, Long> getCategoryStats() {
    AggregationQuery aggQuery = AggregationQuery.builder()
            .indexName(INDEX_NAME)
            .aggregation(new TermsAggregation("category_count", "category"))
            .size(0) // 不返回文档，只返回聚合结果
            .build();
            
    AggregationResult result = searchService.aggregate(aggQuery);
    // 处理 result 获取桶数据 (具体结构取决于实现)
    return new HashMap<>();
}
```

## 总结

`nebula-search-core` 提供了丰富的搜索和分析能力抽象，使得开发者可以用 Java 对象的方式构建复杂的搜索引擎查询，而无需直接编写原生 DSL。


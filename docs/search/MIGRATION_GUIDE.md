# Nebula Elasticsearch API 迁移指南

从旧的 Map-based API 迁移到新的强类型 API

## 概述

本指南帮助您从旧的 `Map<String, Object>` API 迁移到新的强类型 QueryBuilder/Aggregation/Suggester API。

## 为什么要迁移？

旧 API 的问题：
- ❌ 缺乏类型安全，容易出错
- ❌ IDE 无法提供代码补全
- ❌ 难以重构和维护
- ❌ 缺少编译时检查
- ❌ 文档和 API 脱节

新 API 的优势：
- ✅ 类型安全，编译时检查
- ✅ IDE 友好，自动补全
- ✅ 易于重构和维护
- ✅ 自我文档化
- ✅ 更好的错误提示

## 迁移步骤

### 1. 查询 API 迁移

#### 1.1 Match Query

**旧 API:**
```java
SearchQuery query = new SearchQuery();
query.setIndices(new String[]{"my-index"});
Map<String, Object> queryMap = new HashMap<>();
queryMap.put("match", Map.of("content", Map.of("query", "搜索关键词")));
query.setQuery(queryMap);
query.setFrom(0);
query.setSize(10);

SearchResult<MyDoc> result = searchService.search(query, MyDoc.class);
```

**新 API:**
```java
QueryBuilder queryBuilder = new MatchQueryBuilder("content", "搜索关键词");

SearchQuery query = SearchQuery.builder()
    .index("my-index")
    .query(queryBuilder)
    .from(0)
    .size(10)
    .build();

SearchResult<MyDoc> result = searchService.search(query, MyDoc.class);
```

#### 1.2 Term Query

**旧 API:**
```java
Map<String, Object> queryMap = new HashMap<>();
queryMap.put("term", Map.of("status", "published"));
query.setQuery(queryMap);
```

**新 API:**
```java
QueryBuilder queryBuilder = new TermQueryBuilder("status", "published");

SearchQuery query = SearchQuery.builder()
    .index("my-index")
    .query(queryBuilder)
    .build();
```

#### 1.3 Range Query

**旧 API:**
```java
Map<String, Object> queryMap = new HashMap<>();
Map<String, Object> rangeQuery = new HashMap<>();
rangeQuery.put("gte", 100);
rangeQuery.put("lte", 500);
queryMap.put("range", Map.of("price", rangeQuery));
query.setQuery(queryMap);
```

**新 API:**
```java
QueryBuilder queryBuilder = new RangeQueryBuilder("price")
    .gte(100)
    .lte(500);

SearchQuery query = SearchQuery.builder()
    .index("my-index")
    .query(queryBuilder)
    .build();
```

#### 1.4 Bool Query

**旧 API:**
```java
Map<String, Object> boolQuery = new HashMap<>();
Map<String, Object> must = new ArrayList<>();
must.add(Map.of("match", Map.of("title", "Elasticsearch")));
must.add(Map.of("term", Map.of("status", "published")));

Map<String, Object> should = new ArrayList<>();
should.add(Map.of("match", Map.of("tags", "搜索")));

Map<String, Object> mustNot = new ArrayList<>();
mustNot.add(Map.of("term", Map.of("deleted", true)));

boolQuery.put("must", must);
boolQuery.put("should", should);
boolQuery.put("must_not", mustNot);
boolQuery.put("minimum_should_match", 1);

Map<String, Object> queryMap = new HashMap<>();
queryMap.put("bool", boolQuery);
query.setQuery(queryMap);
```

**新 API:**
```java
BoolQueryBuilder boolQuery = new BoolQueryBuilder();

// must 子句
boolQuery.must(new MatchQueryBuilder("title", "Elasticsearch"));
boolQuery.must(new TermQueryBuilder("status", "published"));

// should 子句
boolQuery.should(new MatchQueryBuilder("tags", "搜索"));

// must_not 子句
boolQuery.mustNot(new TermQueryBuilder("deleted", true));

// 最小匹配数
boolQuery.minimumShouldMatch(1);

SearchQuery query = SearchQuery.builder()
    .index("my-index")
    .query(boolQuery)
    .build();
```

#### 1.5 Match All Query

**旧 API:**
```java
Map<String, Object> queryMap = new HashMap<>();
queryMap.put("match_all", new HashMap<>());
query.setQuery(queryMap);
```

**新 API:**
```java
QueryBuilder queryBuilder = new MatchAllQueryBuilder();

SearchQuery query = SearchQuery.builder()
    .index("my-index")
    .query(queryBuilder)
    .build();
```

### 2. 聚合 API 迁移

#### 2.1 Terms Aggregation

**旧 API:**
```java
Map<String, Object> aggregations = new HashMap<>();
Map<String, Object> termsAgg = new HashMap<>();
termsAgg.put("field", "category");
termsAgg.put("size", 10);
aggregations.put("category-stats", Map.of("terms", termsAgg));

AggregationQuery query = new AggregationQuery();
query.setIndices(new String[]{"products"});
query.setAggregations(aggregations);

AggregationResult result = searchService.aggregate(query);
```

**新 API:**
```java
Aggregation termsAgg = new TermsAggregation("category-stats", "category")
    .size(10);

AggregationQuery query = AggregationQuery.builder()
    .index("products")
    .addAggregation(termsAgg)
    .build();

AggregationResult result = searchService.aggregate(query);
```

#### 2.2 Metric Aggregations

**旧 API:**
```java
Map<String, Object> aggregations = new HashMap<>();
aggregations.put("avg-price", Map.of("avg", Map.of("field", "price")));
aggregations.put("total-sales", Map.of("sum", Map.of("field", "sales")));

AggregationQuery query = new AggregationQuery();
query.setIndices(new String[]{"products"});
query.setAggregations(aggregations);
```

**新 API:**
```java
Aggregation avgAgg = MetricAggregation.avg("avg-price", "price");
Aggregation sumAgg = MetricAggregation.sum("total-sales", "sales");

AggregationQuery query = AggregationQuery.builder()
    .index("products")
    .addAggregation(avgAgg)
    .addAggregation(sumAgg)
    .build();
```

#### 2.3 Date Histogram Aggregation

**旧 API:**
```java
Map<String, Object> aggregations = new HashMap<>();
Map<String, Object> dateHistogram = new HashMap<>();
dateHistogram.put("field", "order_time");
dateHistogram.put("calendar_interval", "day");
dateHistogram.put("format", "yyyy-MM-dd");
aggregations.put("daily-stats", Map.of("date_histogram", dateHistogram));

AggregationQuery query = new AggregationQuery();
query.setIndices(new String[]{"orders"});
query.setAggregations(aggregations);
```

**新 API:**
```java
Aggregation dateHistoAgg = new DateHistogramAggregation("daily-stats", "order_time")
    .calendarInterval("1d")  // 注意：使用 "1d" 而不是 "day"
    .format("yyyy-MM-dd");

AggregationQuery query = AggregationQuery.builder()
    .index("orders")
    .addAggregation(dateHistoAgg)
    .build();
```

#### 2.4 嵌套聚合

**旧 API:**
```java
Map<String, Object> subAggs = new HashMap<>();
subAggs.put("avg-price", Map.of("avg", Map.of("field", "price")));

Map<String, Object> termsAgg = new HashMap<>();
termsAgg.put("field", "category");
termsAgg.put("size", 10);

Map<String, Object> categoryAgg = new HashMap<>();
categoryAgg.put("terms", termsAgg);
categoryAgg.put("aggs", subAggs);

Map<String, Object> aggregations = new HashMap<>();
aggregations.put("categories", categoryAgg);

AggregationQuery query = new AggregationQuery();
query.setIndices(new String[]{"products"});
query.setAggregations(aggregations);
```

**新 API:**
```java
// 创建主聚合
TermsAggregation categoryAgg = new TermsAggregation("categories", "category")
    .size(10);

// 添加子聚合
categoryAgg.addSubAggregation(MetricAggregation.avg("avg-price", "price"));

AggregationQuery query = AggregationQuery.builder()
    .index("products")
    .addAggregation(categoryAgg)
    .build();
```

### 3. 建议 API 迁移

#### 3.1 Term Suggester

**旧 API:**
```java
Map<String, Object> suggesters = new HashMap<>();
Map<String, Object> termSuggester = new HashMap<>();
termSuggester.put("text", "some test mssage");
termSuggester.put("term", Map.of(
    "field", "message",
    "size", 5
));
suggesters.put("my-term-suggester", termSuggester);

SuggestQuery query = new SuggestQuery();
query.setIndices(new String[]{"my-index"});
query.setSuggest(suggesters);

SuggestResult result = searchService.suggest(query);
```

**新 API:**
```java
TermSuggester termSuggester = new TermSuggester("my-term-suggester", "message", "some test mssage");
termSuggester.size(5);

SuggestQuery query = SuggestQuery.builder()
    .index("my-index")
    .addSuggester(termSuggester)
    .build();

SuggestResult result = searchService.suggest(query);
```

#### 3.2 Phrase Suggester

**旧 API:**
```java
Map<String, Object> suggesters = new HashMap<>();
Map<String, Object> phraseSuggester = new HashMap<>();
phraseSuggester.put("text", "noble prize");
phraseSuggester.put("phrase", Map.of(
    "field", "title",
    "size", 5,
    "confidence", 0.5
));
suggesters.put("my-phrase-suggester", phraseSuggester);

SuggestQuery query = new SuggestQuery();
query.setIndices(new String[]{"articles"});
query.setSuggest(suggesters);
```

**新 API:**
```java
PhraseSuggester phraseSuggester = new PhraseSuggester("my-phrase-suggester", "title", "noble prize");
phraseSuggester.size(5);
phraseSuggester.confidence(0.5f);

SuggestQuery query = SuggestQuery.builder()
    .index("articles")
    .addSuggester(phraseSuggester)
    .build();
```

#### 3.3 Completion Suggester

**旧 API:**
```java
Map<String, Object> suggesters = new HashMap<>();
Map<String, Object> completionSuggester = new HashMap<>();
completionSuggester.put("prefix", "sear");
completionSuggester.put("completion", Map.of(
    "field", "suggest",
    "size", 10,
    "skip_duplicates", true
));
suggesters.put("my-completion", completionSuggester);

SuggestQuery query = new SuggestQuery();
query.setIndices(new String[]{"products"});
query.setSuggest(suggesters);
```

**新 API:**
```java
CompletionSuggester completionSuggester = new CompletionSuggester("my-completion", "suggest", "sear");
completionSuggester.size(10);
completionSuggester.skipDuplicates(true);

SuggestQuery query = SuggestQuery.builder()
    .index("products")
    .addSuggester(completionSuggester)
    .build();
```

## 迁移检查清单

### 准备阶段
- [ ] 了解新 API 的结构和用法
- [ ] 确认项目依赖版本（nebula-search-core >= 1.0.0）
- [ ] 备份现有代码

### 迁移阶段
- [ ] 识别所有使用旧 API 的代码位置
- [ ] 按模块逐步迁移（查询 → 聚合 → 建议）
- [ ] 更新单元测试
- [ ] 进行集成测试

### 验证阶段
- [ ] 运行所有测试确保通过
- [ ] 进行性能测试对比
- [ ] 检查日志确保没有警告或错误
- [ ] 进行代码审查

### 清理阶段
- [ ] 删除旧的 Map 构建代码
- [ ] 更新文档和注释
- [ ] 提交代码并打标签

## 常见迁移问题

### 问题 1: 编译错误 - 找不到 QueryBuilder 类

**原因**: 没有导入正确的包

**解决方法**:
```java
import io.nebula.search.core.query.builder.*;
```

### 问题 2: DateHistogramAggregation calendarInterval 参数错误

**原因**: 旧 API 使用 `"day"`, 新 API 使用 `"1d"`

**解决方法**:
```java
// 旧 API
"calendar_interval": "day"

// 新 API
.calendarInterval("1d")

// 对应关系:
// "minute" -> "1m"
// "hour" -> "1h"
// "day" -> "1d"
// "week" -> "1w"
// "month" -> "1M"
// "quarter" -> "1q"
// "year" -> "1y"
```

### 问题 3: Suggester 构造函数参数顺序

**原因**: 新 API 的参数顺序为 (name, field, text)

**解决方法**:
```java
// 正确的顺序
new TermSuggester("suggester-name", "field-name", "search-text")

// 而不是
new TermSuggester("suggester-name", "search-text", "field-name")  // 错误
```

### 问题 4: 类名冲突

**原因**: Nebula 和 Elasticsearch Java Client 有同名类

**解决方法**: 使用完全限定名或重命名导入
```java
// 方法 1: 使用完全限定名
io.nebula.search.core.aggregation.Aggregation nebulaAgg = ...
co.elastic.clients.elasticsearch._types.aggregations.Aggregation esAgg = ...

// 方法 2: 重命名导入
import io.nebula.search.core.aggregation.Aggregation as NebulaAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation as ESAggregation;
```

### 问题 5: FieldValue 类型转换

**原因**: Elasticsearch 的某些方法需要 FieldValue 类型

**解决方法**:
```java
// 在测试中 mock 时
when(mockBucket.key()).thenReturn(FieldValue.of("value"));

// 而不是
when(mockBucket.key()).thenReturn("value");  // 错误
```

## 迁移示例

### 完整迁移示例：商品搜索

**迁移前:**
```java
public SearchResult<Product> searchProducts(String keyword, Integer minPrice, Integer maxPrice, int page, int size) {
    SearchQuery query = new SearchQuery();
    query.setIndices(new String[]{"products"});
    
    // 构建查询
    Map<String, Object> boolQuery = new HashMap<>();
    List<Map<String, Object>> must = new ArrayList<>();
    
    if (keyword != null && !keyword.isEmpty()) {
        must.add(Map.of("match", Map.of("title", keyword)));
    }
    
    List<Map<String, Object>> filter = new ArrayList<>();
    if (minPrice != null || maxPrice != null) {
        Map<String, Object> priceRange = new HashMap<>();
        if (minPrice != null) priceRange.put("gte", minPrice);
        if (maxPrice != null) priceRange.put("lte", maxPrice);
        filter.add(Map.of("range", Map.of("price", priceRange)));
    }
    
    boolQuery.put("must", must);
    boolQuery.put("filter", filter);
    
    Map<String, Object> queryMap = new HashMap<>();
    queryMap.put("bool", boolQuery);
    query.setQuery(queryMap);
    query.setFrom(page * size);
    query.setSize(size);
    
    return searchService.search(query, Product.class);
}
```

**迁移后:**
```java
public SearchResult<Product> searchProducts(String keyword, Integer minPrice, Integer maxPrice, int page, int size) {
    BoolQueryBuilder boolQuery = new BoolQueryBuilder();
    
    // 关键词查询
    if (keyword != null && !keyword.isEmpty()) {
        boolQuery.must(new MatchQueryBuilder("title", keyword));
    }
    
    // 价格过滤
    if (minPrice != null || maxPrice != null) {
        RangeQueryBuilder priceRange = new RangeQueryBuilder("price");
        if (minPrice != null) priceRange.gte(minPrice);
        if (maxPrice != null) priceRange.lte(maxPrice);
        boolQuery.filter(priceRange);
    }
    
    SearchQuery query = SearchQuery.builder()
        .index("products")
        .query(boolQuery)
        .from(page * size)
        .size(size)
        .build();
    
    return searchService.search(query, Product.class);
}
```

**对比分析:**
- 代码行数减少约 30%
- 类型安全，编译时检查
- 更易读、更易维护
- IDE 可以提供代码补全

## 性能对比

新旧 API 在性能上没有显著差异，因为：

1. **运行时性能相同**: 两者最终都转换为 Elasticsearch 的原生查询
2. **编译时优势**: 新 API 在编译时进行更多检查，减少运行时错误
3. **开发效率**: 新 API 提高了开发效率和代码质量

## 获取帮助

- 查看 [API 使用指南](./ELASTICSEARCH_API_GUIDE.md)
- 参考测试用例：`nebula-search-elasticsearch/src/test/`
- 提交 Issue: [GitHub Issues](https://github.com/your-org/nebula/issues)

## 总结

迁移到新的强类型 API 可以：
- ✅ 提高代码质量和可维护性
- ✅ 减少运行时错误
- ✅ 提升开发效率
- ✅ 更好的 IDE 支持

建议逐步迁移，先从简单的查询开始，再逐步迁移复杂的聚合和建议功能。

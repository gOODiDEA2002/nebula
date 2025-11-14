# Nebula Elasticsearch 强类型 API 使用指南

## 概述

Nebula Elasticsearch 模块提供了基于强类型的查询、聚合和建议 API，相比传统的 `Map<String, Object>` 方式，具有以下优势：

- **类型安全**：编译时检查，减少运行时错误
- **IDE 友好**：自动补全、重构支持
- **可维护性**：代码更易读、易维护
- **文档化**：通过类型系统自我文档化

## 1. 查询 API (QueryBuilder)

### 1.1 Match Query（全文匹配查询）

用于全文检索，会对查询文本进行分词。

```java
// 基本用法
QueryBuilder query = new MatchQueryBuilder("content", "Elasticsearch 搜索");

// 带参数
QueryBuilder query = new MatchQueryBuilder("content", "Elasticsearch 搜索")
    .operator("AND")              // 所有词都必须匹配
    .minimumShouldMatch("75%")    // 至少75%的词匹配
    .fuzziness("AUTO")            // 自动模糊匹配
    .boost(2.0f);                 // 提升权重

// 执行搜索
SearchQuery searchQuery = SearchQuery.builder()
    .index("my-index")
    .query(query)
    .from(0)
    .size(10)
    .build();

SearchResult<MyDocument> result = searchService.search(searchQuery, MyDocument.class);
```

### 1.2 Term Query（精确匹配查询）

用于精确值匹配，不进行分词。

```java
// 精确匹配状态
QueryBuilder query = new TermQueryBuilder("status", "published");

// 带权重
QueryBuilder query = new TermQueryBuilder("status", "published")
    .boost(1.5f);
```

### 1.3 Range Query（范围查询）

用于数值、日期范围查询。

```java
// 数值范围
QueryBuilder query = new RangeQueryBuilder("price")
    .gte(100)    // 大于等于 100
    .lte(500);   // 小于等于 500

// 日期范围
QueryBuilder query = new RangeQueryBuilder("create_time")
    .gte("2024-01-01")
    .lt("2024-12-31")
    .format("yyyy-MM-dd")
    .timeZone("Asia/Shanghai");
```

### 1.4 Bool Query（布尔组合查询）

组合多个查询条件。

```java
// 复杂布尔查询
BoolQueryBuilder boolQuery = new BoolQueryBuilder();

// must: 必须匹配（AND）
boolQuery.must(new MatchQueryBuilder("title", "Elasticsearch"));
boolQuery.must(new TermQueryBuilder("status", "published"));

// should: 应该匹配（OR），增加评分
boolQuery.should(new MatchQueryBuilder("tags", "搜索引擎"));
boolQuery.should(new MatchQueryBuilder("tags", "大数据"));

// must_not: 必须不匹配
boolQuery.mustNot(new TermQueryBuilder("deleted", true));

// filter: 必须匹配但不影响评分（性能更好）
boolQuery.filter(new RangeQueryBuilder("price").gte(0));

// 设置最小匹配数
boolQuery.minimumShouldMatch(1);  // should 子句至少匹配1个
```

### 1.5 Match All Query（匹配所有文档）

用于获取所有文档或作为基础查询。

```java
QueryBuilder query = new MatchAllQueryBuilder();

// 通常配合过滤器使用
BoolQueryBuilder boolQuery = new BoolQueryBuilder();
boolQuery.must(new MatchAllQueryBuilder());
boolQuery.filter(new TermQueryBuilder("category", "electronics"));
```

## 2. 聚合 API (Aggregation)

### 2.1 Terms Aggregation（分组统计）

按字段值分组统计文档数量。

```java
// 基本用法：按分类统计
Aggregation termsAgg = new TermsAggregation("category-stats", "category")
    .size(10);  // 返回前10个分类

AggregationQuery query = AggregationQuery.builder()
    .index("products")
    .addAggregation(termsAgg)
    .build();

AggregationResult result = searchService.aggregate(query);

// 解析结果
Map<String, Object> categoryStats = (Map<String, Object>) result.getAggregations().get("category-stats");
List<Map<String, Object>> buckets = (List<Map<String, Object>>) categoryStats.get("buckets");
for (Map<String, Object> bucket : buckets) {
    String key = (String) bucket.get("key");
    Long count = (Long) bucket.get("doc_count");
    System.out.println(key + ": " + count);
}
```

### 2.2 Metric Aggregations（指标聚合）

计算字段的统计指标。

```java
// 平均值
Aggregation avgAgg = MetricAggregation.avg("avg-price", "price");

// 求和
Aggregation sumAgg = MetricAggregation.sum("total-sales", "sales_amount");

// 最小值
Aggregation minAgg = MetricAggregation.min("min-price", "price");

// 最大值
Aggregation maxAgg = MetricAggregation.max("max-price", "price");

// 统计信息（count, min, max, avg, sum）
Aggregation statsAgg = MetricAggregation.stats("price-stats", "price");

// 基数统计（去重计数）
Aggregation cardinalityAgg = MetricAggregation.cardinality("unique-users", "user_id");

// 组合使用
AggregationQuery query = AggregationQuery.builder()
    .index("orders")
    .addAggregation(avgAgg)
    .addAggregation(sumAgg)
    .addAggregation(statsAgg)
    .build();
```

### 2.3 Histogram Aggregation（直方图聚合）

按数值区间分组。

```java
// 价格区间统计（每100一个区间）
Aggregation histoAgg = new HistogramAggregation("price-histogram", "price", 100.0)
    .minDocCount(0);  // 包含文档数为0的区间

AggregationQuery query = AggregationQuery.builder()
    .index("products")
    .addAggregation(histoAgg)
    .build();
```

### 2.4 Date Histogram Aggregation（日期直方图聚合）

按时间区间分组。

```java
// 按天统计订单
Aggregation dateHistoAgg = new DateHistogramAggregation("daily-orders", "order_time")
    .calendarInterval("1d")           // 每天一个区间
    .format("yyyy-MM-dd")             // 日期格式
    .timeZone("Asia/Shanghai")        // 时区
    .minDocCount(0);                  // 包含空区间

// 其他时间间隔选项：
// - "1m": 每分钟
// - "1h": 每小时
// - "1d": 每天
// - "1w": 每周
// - "1M": 每月
// - "1q": 每季度
// - "1y": 每年

AggregationQuery query = AggregationQuery.builder()
    .index("orders")
    .addAggregation(dateHistoAgg)
    .build();
```

### 2.5 嵌套聚合（Nested Aggregations）

在聚合内部再进行子聚合。

```java
// 按分类统计，每个分类内计算平均价格
TermsAggregation categoryAgg = new TermsAggregation("categories", "category")
    .size(10);

// 添加子聚合
categoryAgg.addSubAggregation(MetricAggregation.avg("avg-price", "price"));
categoryAgg.addSubAggregation(MetricAggregation.sum("total-sales", "sales"));

AggregationQuery query = AggregationQuery.builder()
    .index("products")
    .addAggregation(categoryAgg)
    .build();

AggregationResult result = searchService.aggregate(query);

// 解析嵌套结果
Map<String, Object> categories = (Map<String, Object>) result.getAggregations().get("categories");
List<Map<String, Object>> buckets = (List<Map<String, Object>>) categories.get("buckets");
for (Map<String, Object> bucket : buckets) {
    String category = (String) bucket.get("key");
    Map<String, Object> subAggs = (Map<String, Object>) bucket.get("aggregations");
    Map<String, Object> avgPrice = (Map<String, Object>) subAggs.get("avg-price");
    Double avgValue = (Double) avgPrice.get("value");
    System.out.println(category + " 平均价格: " + avgValue);
}
```

### 2.6 带过滤条件的聚合

对数据进行过滤后再聚合。

```java
// 只统计价格大于100的商品
Aggregation avgAgg = MetricAggregation.avg("avg-price", "price");

AggregationQuery query = AggregationQuery.builder()
    .index("products")
    .query(new RangeQueryBuilder("price").gte(100))  // 过滤条件
    .addAggregation(avgAgg)
    .build();
```

## 3. 建议 API (Suggester)

### 3.1 Term Suggester（词项建议器）

用于拼写纠错，提供相似词建议。

```java
// 基本用法
TermSuggester termSuggester = new TermSuggester("my-term-suggester", "message", "some test mssage");
termSuggester.size(5);                // 返回前5个建议
termSuggester.minDocFreq(1);          // 最小文档频率
termSuggester.maxEdits(2);            // 最大编辑距离
termSuggester.prefixLength(1);        // 前缀长度
termSuggester.minWordLength(4);       // 最小词长度
termSuggester.suggestMode("popular"); // 建议模式：missing, popular, always

SuggestQuery query = SuggestQuery.builder()
    .index("my-index")
    .addSuggester(termSuggester)
    .build();

SuggestResult result = searchService.suggest(query);
```

### 3.2 Phrase Suggester（短语建议器）

用于短语级别的建议，考虑词与词之间的关系。

```java
// 短语建议
PhraseSuggester phraseSuggester = new PhraseSuggester("my-phrase-suggester", "title", "noble prize");
phraseSuggester.size(5);
phraseSuggester.confidence(0.5f);              // 置信度阈值
phraseSuggester.highlight("<em>", "</em>");    // 高亮标签

SuggestQuery query = SuggestQuery.builder()
    .index("articles")
    .addSuggester(phraseSuggester)
    .build();

SuggestResult result = searchService.suggest(query);
```

### 3.3 Completion Suggester（自动补全建议器）

用于实时自动补全，性能最好。

```java
// 自动补全
CompletionSuggester completionSuggester = new CompletionSuggester("my-completion", "suggest", "sear");
completionSuggester.size(10);
completionSuggester.skipDuplicates(true);  // 跳过重复结果

SuggestQuery query = SuggestQuery.builder()
    .index("products")
    .addSuggester(completionSuggester)
    .build();

SuggestResult result = searchService.suggest(query);
```

## 4. 完整示例

### 4.1 商品搜索示例

```java
// 1. 构建复杂查询
BoolQueryBuilder boolQuery = new BoolQueryBuilder();

// 标题或描述包含关键词
boolQuery.should(new MatchQueryBuilder("title", "智能手机").boost(2.0f));
boolQuery.should(new MatchQueryBuilder("description", "智能手机"));
boolQuery.minimumShouldMatch(1);

// 必须是已发布状态
boolQuery.filter(new TermQueryBuilder("status", "published"));

// 价格范围
boolQuery.filter(new RangeQueryBuilder("price").gte(1000).lte(5000));

// 排除缺货商品
boolQuery.mustNot(new TermQueryBuilder("stock", 0));

// 2. 执行搜索
SearchQuery searchQuery = SearchQuery.builder()
    .index("products")
    .query(boolQuery)
    .from(0)
    .size(20)
    .build();

SearchResult<Product> result = searchService.search(searchQuery, Product.class);

// 3. 处理结果
for (SearchResult.Document<Product> doc : result.getDocuments()) {
    Product product = doc.getSource();
    Double score = doc.getScore();
    System.out.println("商品：" + product.getTitle() + " 评分：" + score);
}
```

### 4.2 订单统计示例

```java
// 1. 按天统计订单，每天内按状态分组
DateHistogramAggregation dailyAgg = new DateHistogramAggregation("daily-stats", "order_time")
    .calendarInterval("1d")
    .format("yyyy-MM-dd");

// 添加子聚合：按订单状态分组
TermsAggregation statusAgg = new TermsAggregation("by-status", "status");
statusAgg.addSubAggregation(MetricAggregation.sum("total-amount", "amount"));

dailyAgg.addSubAggregation(statusAgg);

// 2. 只统计最近7天的数据
RangeQueryBuilder timeFilter = new RangeQueryBuilder("order_time")
    .gte("now-7d/d")
    .lt("now/d");

// 3. 执行聚合
AggregationQuery query = AggregationQuery.builder()
    .index("orders")
    .query(timeFilter)
    .addAggregation(dailyAgg)
    .build();

AggregationResult result = searchService.aggregate(query);

// 4. 解析结果
Map<String, Object> dailyStats = (Map<String, Object>) result.getAggregations().get("daily-stats");
List<Map<String, Object>> buckets = (List<Map<String, Object>>) dailyStats.get("buckets");

for (Map<String, Object> dayBucket : buckets) {
    String date = (String) dayBucket.get("key_as_string");
    System.out.println("日期：" + date);
    
    Map<String, Object> statusStats = (Map<String, Object>) dayBucket.get("by-status");
    List<Map<String, Object>> statusBuckets = (List<Map<String, Object>>) statusStats.get("buckets");
    
    for (Map<String, Object> statusBucket : statusBuckets) {
        String status = (String) statusBucket.get("key");
        Long count = (Long) statusBucket.get("doc_count");
        Map<String, Object> totalAmount = (Map<String, Object>) statusBucket.get("total-amount");
        Double amount = (Double) totalAmount.get("value");
        System.out.println("  状态：" + status + " 订单数：" + count + " 总金额：" + amount);
    }
}
```

## 5. 最佳实践

### 5.1 查询优化

1. **使用过滤器代替查询**：不需要评分时使用 `filter` 而不是 `must`
2. **合理使用 boost**：为重要字段提升权重
3. **避免深度分页**：使用 scroll API 代替 from/size
4. **使用索引别名**：便于索引重建和切换

### 5.2 聚合优化

1. **限制聚合数量**：使用 `size` 限制返回的桶数量
2. **使用 `filter` 预过滤**：减少聚合的数据量
3. **避免过深嵌套**：嵌套层级不要超过3层
4. **合理使用 `minDocCount`**：过滤掉文档数过少的桶

### 5.3 性能建议

1. **批量操作**：使用 bulk API 进行批量索引/更新
2. **合理设置分片数**：根据数据量和节点数设置
3. **使用 routing**：相关文档路由到同一分片
4. **监控查询性能**：使用 profile API 分析慢查询

## 6. 常见问题

### Q1: 如何实现分词搜索？
A: 使用 `MatchQueryBuilder`，它会自动对查询文本进行分词。

### Q2: 如何实现精确匹配？
A: 使用 `TermQueryBuilder`，它不会进行分词，直接匹配原始值。

### Q3: 如何实现多字段搜索？
A: 使用 `BoolQueryBuilder` 组合多个 `MatchQueryBuilder`。

### Q4: 如何实现搜索高亮？
A: 在 `SearchQuery` 中设置 `highlight` 参数（待实现）。

### Q5: 如何处理大量数据？
A: 使用 `scrollSearch()` API 进行游标查询。

## 7. 参考资料

- [Elasticsearch 官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Elasticsearch Java Client 文档](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/index.html)
- [Nebula Search 核心 API](../nebula-search-core/README.md)

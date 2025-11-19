# nebula-search-core 模块单元测试清单

## 模块说明

搜索核心抽象层，定义了SearchService接口以及查询构建器、索引映射等核心模型。

## 核心功能

1. 查询构建器（SearchQuery, BoolQuery, MatchQuery等）
2. 索引映射定义（IndexMapping）
3. 搜索结果模型（SearchResult）
4. 聚合查询模型（AggregationQuery）

## 测试类清单

### 1. QueryBuilderTest

**测试类路径**: `io.nebula.search.core.query.builder` 包下的类  
**测试目的**: 验证查询条件的构建逻辑

| 测试方法 | 被测试方法 | 测试目的 |
|---------|-----------|---------|
| testBoolQuery() | BoolQueryBuilder | 验证布尔查询构建 |
| testMatchQuery() | MatchQueryBuilder | 验证匹配查询构建 |
| testRangeQuery() | RangeQueryBuilder | 验证范围查询构建 |
| testTermQuery() | TermQueryBuilder | 验证精确查询构建 |

### 2. SearchQueryTest

**测试类路径**: `io.nebula.search.core.query.SearchQuery`  
**测试目的**: 验证主查询对象的构建

| 测试方法 | 被测试方法 | 测试目的 |
|---------|-----------|---------|
| testBuilder() | builder() | 验证分页、排序、高亮参数设置 |
| testDefaultValues() | - | 验证默认参数值 |

### 3. IndexMappingTest

**测试类路径**: `io.nebula.search.core.model.IndexMapping`  
**测试目的**: 验证索引映射的定义

| 测试方法 | 被测试方法 | 测试目的 |
|---------|-----------|---------|
| testMappingConstruction() | - | 验证字段类型和属性设置 |

## 测试执行

```bash
mvn test -pl nebula/infrastructure/search/nebula-search-core
```

## 验收标准

- 所有查询构建器测试通过
- 模型类构建测试通过


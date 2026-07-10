# nebula-data-mongodb 模块单元测试清单

## 模块说明

基于Spring Data MongoDB的数据访问模块，提供增强的Repository、动态查询、聚合分析和地理位置查询功能。

## 核心功能

1. 增强的MongoRepository
2. 动态查询构建
3. 聚合查询封装
4. 地理位置查询
5. 批量操作

## 测试类清单

### 1. MongoRepositoryExtensionTest

**测试类路径**: `io.nebula.data.mongodb.repository.MongoRepository` 实现类  
**测试目的**: 验证Repository扩展功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testFindByField() | findByField(String, Object) | 测试动态字段查询 | MongoTemplate |
| testUpdateField() | updateField(Query, String, Object) | 测试单字段更新 | MongoTemplate |
| testAddToArray() | addToArray(Query, String, Object) | 测试数组追加 | MongoTemplate |

### 2. DynamicQueryTest

**测试类路径**: `io.nebula.data.mongodb.core.query.QueryBuilder`  
**测试目的**: 验证动态查询构建

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testBuildCriteria() | - | 测试Criteria构建 | - |
| testBuildSort() | - | 测试Sort构建 | - |
| testBuildPage() | - | 测试Pageable构建 | - |

### 3. GeoSpatialTest

**测试类路径**: 地理位置功能测试  
**测试目的**: 验证地理位置查询

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testFindNear() | findByLocationNear() | 测试附近查询 | MongoTemplate |
| testFindWithin() | findByLocationWithin() | 测试区域查询 | MongoTemplate |

## Mock策略

### 需要Mock的对象

| Mock对象 | 使用场景 | Mock行为 |
|---------|---------|---------|
| MongoTemplate | 所有数据库操作 | Mock find(), save(), update(), aggregate() |
| MongoConverter | 数据转换 | Mock write(), read() |

**注意**: 单元测试不应连接真实的MongoDB数据库，应使用Mockito模拟MongoTemplate的行为，或者使用Embedded MongoDB进行集成测试（如果环境允许）。

## 测试执行

```bash
mvn test -pl nebula/infrastructure/data/nebula-data-mongodb
```

## 验收标准

- 所有扩展方法测试通过
- 动态查询构建逻辑正确
- Mock测试覆盖主要业务场景


# Nebula Data MongoDB

`nebula-data-mongodb` 基于 Spring Data MongoDB 提供一个可按实体类型创建的轻量仓储实现。
模块随根 Maven Reactor 构建，但不会自动替业务实体创建仓储 Bean。

## 引入依赖

```xml
<dependency>
    <groupId>com.nebula-projects</groupId>
    <artifactId>nebula-data-mongodb</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

连接配置沿用 Spring Boot：

```yaml
spring:
  mongodb:
    uri: mongodb://localhost:27017/nebula
```

## 创建仓储

```java
@Configuration(proxyBeanMethods = false)
class UserMongoConfiguration {

    @Bean
    MongoRepository<UserDocument, String> userMongoRepository(MongoOperations operations) {
        return new io.nebula.data.mongodb.template.MongoTemplate<>(operations, UserDocument.class);
    }
}
```

模块提供以下能力：

- 常用 CRUD、分页、排序和条件查询。
- 字段、范围、正则、全文和地理位置查询。
- 批量更新、数组更新和 upsert。
- Spring Data `AggregationOperation`、BSON、JSON 字符串或字符串键 Map 组成的聚合管道。
- 普通、复合、文本和 `2dsphere` 地理索引管理。

`saveAll` 使用逐项 `save` 保持「新增或更新」语义。该方法会产生 N 次数据库写入，
适用于同一批数据中同时存在新增和更新的情况。仅批量新增时使用 `insertAll`，该方法通过
一次批量请求写入，遇到重复主键时抛出异常。
全文查询前必须先创建文本索引，地理查询前必须为对应字段创建地理索引。

## 验证

```bash
mvn test -pl infrastructure/data/nebula-data-mongodb -am
```

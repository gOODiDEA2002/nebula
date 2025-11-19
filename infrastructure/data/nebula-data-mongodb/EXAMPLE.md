# nebula-data-mongodb 模块示例

## 模块简介

`nebula-data-mongodb` 模块基于 Spring Data MongoDB 提供了增强的数据访问能力。它引入了 `MongoRepository` 接口，扩展了标准 Repository 的功能，增加了丰富的动态查询、聚合操作和地理位置查询支持。

## 核心功能示例

### 1. 定义实体类

使用 `@Document` 注解定义 MongoDB 文档实体。

**`io.nebula.example.mongodb.model.Product`**:

```java
package io.nebula.example.mongodb.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.List;

@Data
@Document(collection = "products")
public class Product {

    @Id
    private String id;

    @TextIndexed // 支持全文搜索
    private String name;

    @Indexed
    private String category;

    private BigDecimal price;
    
    private List<String> tags;
    
    private boolean active;
}
```

### 2. 定义 Repository

继承 `MongoRepository<T, ID>` 接口。

**`io.nebula.example.mongodb.repository.ProductRepository`**:

```java
package io.nebula.example.mongodb.repository;

import io.nebula.data.mongodb.repository.MongoRepository;
import io.nebula.example.mongodb.model.Product;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    // 继承的方法包括:
    // - findByField
    // - findByText
    // - findByRange
    // - aggregate
    // - updateField
    // ... 以及所有 Spring Data 标准方法
}
```

### 3. 使用 Repository

**`io.nebula.example.mongodb.service.ProductService`**:

```java
package io.nebula.example.mongodb.service;

import io.nebula.example.mongodb.model.Product;
import io.nebula.example.mongodb.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public void demoOperations() {
        // 1. 简单字段查询
        List<Product> electronics = productRepository.findByField("category", "Electronics");
        
        // 2. 全文搜索 (需要定义 @TextIndexed)
        List<Product> searchResults = productRepository.findByText("smartphone");
        
        // 3. 范围查询
        List<Product> expensiveItems = productRepository.findByRange("price", new BigDecimal("1000"), new BigDecimal("9999"));
        
        // 4. 复杂 Criteria 查询
        Criteria criteria = Criteria.where("active").is(true)
                .and("tags").in("sale", "new")
                .and("price").lt(new BigDecimal("500"));
        List<Product> filtered = productRepository.findByCriteria(criteria);
        
        // 5. 局部字段更新 (原子操作)
        Query updateQuery = new Query(Criteria.where("category").is("Old_Electronics"));
        long count = productRepository.updateField(updateQuery, "active", false);
        log.info("下架了 {} 个商品", count);
        
        // 6. 数组操作
        Query tagQuery = new Query(Criteria.where("id").is("prod-1"));
        productRepository.addToArray(tagQuery, "tags", "featured");
    }
}
```

### 4. 聚合查询示例

使用 `aggregate` 方法执行原生聚合管道。

```java
public void aggregationDemo() {
    // 统计每个分类的平均价格
    // 对应 MongoDB Pipeline: [ {$group: {_id: "$category", avgPrice: {$avg: "$price"}}} ]
    
    // 注意：这里仅为示例，实际使用需构建 Bson 或 Document 对象
    // productRepository.aggregate(pipeline, ResultDto.class);
}
```

## 总结

`nebula-data-mongodb` 在 Spring Data MongoDB 的基础上提供了更多“开箱即用”的快捷方法（如 `updateField`, `findByText`, `addToArray`），减少了手动构建 `MongoTemplate` 操作的样板代码，极大提升了开发效率。


# Nebula Starter All - 使用示例

> 全功能Starter的完整使用示例，展示如何在单体应用中使用Nebula框架的所有能力。

## 示例概览

本文档包含以下示例：

- [示例1：单体Web应用](#示例1单体web应用)
- [示例2：完整CRUD应用](#示例2完整crud应用)
- [示例3：微服务功能使用](#示例3微服务功能使用)
- [示例4：AI功能集成](#示例4ai功能集成)
- [示例5：全栈搜索](#示例5全栈搜索)
- [示例6：文件存储](#示例6文件存储)
- [示例7：任务调度](#示例7任务调度)
- [票务系统完整应用](#票务系统完整应用)

## 前提条件

### 环境要求

- **Java**：21+
- **Maven**：3.8+
- **Spring Boot**：3.2+
- **MySQL**：8.0+
- **Redis**：7.0+
- **Elasticsearch**：8.0+（可选）
- **MinIO**：RELEASE.2024（可选）
- **RabbitMQ**：3.12+（可选）

### 依赖配置

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-all</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

---

## 示例1：单体Web应用

### 场景说明

创建一个包含Web、数据库、缓存的完整单体应用。

### 实现步骤

#### 步骤1：创建主类

```java
package com.example.monolith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 单体应用主类
 */
@SpringBootApplication
public class MonolithApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MonolithApplication.class, args);
    }
}
```

#### 步骤2：配置文件

`application.yml`:

```yaml
spring:
  application:
    name: monolith-app
  
  # 数据源配置
  datasource:
    url: jdbc:mysql://localhost:3306/monolith_db
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  # Redis配置
  data:
    redis:
      host: localhost
      port: 6379
  
  # 缓存配置
  cache:
    type: redis

# MyBatis-Plus配置
mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  type-aliases-package: com.example.monolith.entity
```

#### 步骤3：创建实体

```java
package com.example.monolith.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产品实体
 */
@Data
@TableName("t_product")
public class Product {
    
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String category;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
```

#### 步骤4：创建Mapper

```java
package com.example.monolith.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.monolith.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/**
 * 产品Mapper
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
```

#### 步骤5：创建Service

```java
package com.example.monolith.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.monolith.entity.Product;
import com.example.monolith.mapper.ProductMapper;
import io.nebula.core.exception.BusinessException;
import io.nebula.core.util.IdGenerator;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

/**
 * 产品服务
 */
@Service
@Slf4j
public class ProductService extends ServiceImpl<ProductMapper, Product> {
    
    /**
     * 创建产品
     */
    public Product create(Product product) {
        product.setId(IdGenerator.snowflakeIdString());
        save(product);
        
        log.info("创建产品: {}", product.getName());
        return product;
    }
    
    /**
     * 查询产品（带缓存）
     */
    @Cacheable(value = "products", key = "#id")
    public Product getById(String id) {
        Product product = super.getById(id);
        if (product == null) {
            throw new BusinessException("产品不存在");
        }
        return product;
    }
    
    /**
     * 更新产品（清除缓存）
     */
    @CacheEvict(value = "products", key = "#product.id")
    public Product update(Product product) {
        updateById(product);
        
        log.info("更新产品: {}", product.getId());
        return product;
    }
    
    /**
     * 删除产品（清除缓存）
     */
    @CacheEvict(value = "products", key = "#id")
    public void delete(String id) {
        removeById(id);
        
        log.info("删除产品: {}", id);
    }
}
```

#### 步骤6：创建Controller

```java
package com.example.monolith.controller;

import io.nebula.web.controller.BaseController;
import io.nebula.core.model.Result;
import io.nebula.core.model.PageResult;
import com.example.monolith.entity.Product;
import com.example.monolith.service.ProductService;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 产品Controller
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController extends BaseController {
    
    private final ProductService productService;
    
    /**
     * 创建产品
     */
    @PostMapping
    public Result<Product> create(@Valid @RequestBody Product product) {
        Product created = productService.create(product);
        return success(created);
    }
    
    /**
     * 查询产品
     */
    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable String id) {
        Product product = productService.getById(id);
        return success(product);
    }
    
    /**
     * 分页查询
     */
    @GetMapping
    public PageResult<Product>> page(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize) {
        
        Page<Product> page = productService.page(new Page<>(pageNum, pageSize));
        
        PageResult<Product> pageResult = PageResult.of(
            page.getRecords(),
            page.getTotal(),
            pageNum,
            pageSize
        );
        
        return success(pageResult);
    }
    
    /**
     * 更新产品
     */
    @PutMapping("/{id}")
    public Result<Product> update(
        @PathVariable String id,
        @Valid @RequestBody Product product) {
        
        product.setId(id);
        Product updated = productService.update(product);
        return success(updated);
    }
    
    /**
     * 删除产品
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        productService.delete(id);
        return success();
    }
}
```

---

## 示例2：完整CRUD应用

### 场景说明

展示包含验证、异常处理、分页的完整CRUD应用。

### 实现代码

**DTO定义**:

```java
package com.example.monolith.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * 创建产品请求
 */
@Data
public class CreateProductRequest {
    
    @NotBlank(message = "产品名称不能为空")
    @Size(min = 2, max = 100, message = "产品名称长度为2-100个字符")
    private String name;
    
    @Size(max = 500, message = "描述不能超过500个字符")
    private String description;
    
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;
    
    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;
    
    @NotBlank(message = "分类不能为空")
    private String category;
}

/**
 * 更新产品请求
 */
@Data
public class UpdateProductRequest {
    
    @NotBlank(message = "产品名称不能为空")
    private String name;
    
    private String description;
    
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;
    
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;
    
    private String category;
}

/**
 * 产品查询请求
 */
@Data
public class ProductQueryRequest {
    
    private String keyword;
    private String category;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    
    @Min(1)
    private Integer pageNum = 1;
    
    @Min(1)
    @Max(100)
    private Integer pageSize = 10;
}
```

**Service增强**:

```java
@Service
@Slf4j
public class ProductService extends ServiceImpl<ProductMapper, Product> {
    
    /**
     * 高级查询
     */
    public PageResult<Product> query(ProductQueryRequest request) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        
        // 关键词搜索
        if (StringUtils.isNotBlank(request.getKeyword())) {
            wrapper.and(w -> w
                .like(Product::getName, request.getKeyword())
                .or()
                .like(Product::getDescription, request.getKeyword())
            );
        }
        
        // 分类过滤
        if (StringUtils.isNotBlank(request.getCategory())) {
            wrapper.eq(Product::getCategory, request.getCategory());
        }
        
        // 价格范围
        if (request.getMinPrice() != null) {
            wrapper.ge(Product::getPrice, request.getMinPrice());
        }
        if (request.getMaxPrice() != null) {
            wrapper.le(Product::getPrice, request.getMaxPrice());
        }
        
        // 分页查询
        Page<Product> page = page(
            new Page<>(request.getPageNum(), request.getPageSize()),
            wrapper
        );
        
        return PageResult.of(
            page.getRecords(),
            page.getTotal(),
            request.getPageNum(),
            request.getPageSize()
        );
    }
    
    /**
     * 批量操作
     */
    @Transactional
    public void batchDelete(List<String> ids) {
        if (ids.isEmpty()) {
            return;
        }
        
        removeByIds(ids);
        log.info("批量删除产品: {} 个", ids.size());
    }
    
    /**
     * 库存扣减
     */
    @Transactional
    public void deductStock(String id, int quantity) {
        Product product = getById(id);
        
        if (product.getStock() < quantity) {
            throw new BusinessException("库存不足");
        }
        
        product.setStock(product.getStock() - quantity);
        updateById(product);
        
        log.info("扣减库存: productId={}, quantity={}", id, quantity);
    }
}
```

---

## 示例3：微服务功能使用

### 场景说明

在单体应用中使用RPC、消息队列等微服务功能。

### 实现代码

**RPC服务定义**:

```java
package com.example.monolith.rpc;

import io.nebula.rpc.annotation.RpcService;
import io.nebula.core.model.Result;
import com.example.monolith.entity.Product;

/**
 * 产品RPC服务
 */
@RpcService(name = "product-service")
public interface ProductRpcService {
    
    Result<Product> getById(String id);
    
    Result<Boolean> checkStock(String id, Integer quantity);
}
```

**RPC服务实现**:

```java
package com.example.monolith.rpc;

import io.nebula.rpc.annotation.RpcServiceImpl;
import com.example.monolith.service.ProductService;
import lombok.RequiredArgsConstructor;

/**
 * 产品RPC服务实现
 */
@RpcServiceImpl
@RequiredArgsConstructor
public class ProductRpcServiceImpl implements ProductRpcService {
    
    private final ProductService productService;
    
    @Override
    public Result<Product> getById(String id) {
        Product product = productService.getById(id);
        return Result.success(product);
    }
    
    @Override
    public Result<Boolean> checkStock(String id, Integer quantity) {
        Product product = productService.getById(id);
        boolean sufficient = product.getStock() >= quantity;
        return Result.success(sufficient);
    }
}
```

**消息发送**:

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final MessageProducer messageProducer;
    
    public Order createOrder(CreateOrderRequest request) {
        // 创建订单
        Order order = new Order();
        // ...
        
        // 发送订单创建事件
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(order.getId());
        event.setProductId(request.getProductId());
        event.setQuantity(request.getQuantity());
        
        messageProducer.send("order.created", event);
        
        return order;
    }
}
```

**消息监听**:

```java
@Component
@RequiredArgsConstructor
public class OrderEventListener {
    
    private final ProductService productService;
    
    @MessageListener(topic = "order.created")
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 扣减库存
        productService.deductStock(
            event.getProductId(),
            event.getQuantity()
        );
    }
}
```

---

## 示例4：AI功能集成

### 实现代码

```java
@Service
@RequiredArgsConstructor
public class ProductAIService {
    
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    
    /**
     * 生成产品描述
     */
    public String generateDescription(Product product) {
        String prompt = String.format("""
            请为以下产品生成一段吸引人的描述：
            
            产品名称：%s
            分类：%s
            价格：¥%s
            
            要求：
            1. 描述要简洁有力
            2. 突出产品特点
            3. 字数在100-200字
            """,
            product.getName(),
            product.getCategory(),
            product.getPrice()
        );
        
        return chatClient.call(prompt);
    }
    
    /**
     * 索引产品信息
     */
    public void indexProduct(Product product) {
        String content = String.format(
            "%s %s %s",
            product.getName(),
            product.getCategory(),
            product.getDescription()
        );
        
        Document doc = new Document(content, Map.of(
            "productId", product.getId(),
            "category", product.getCategory()
        ));
        
        vectorStore.add(List.of(doc));
    }
    
    /**
     * 智能搜索
     */
    public List<Product> search(String query) {
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.query(query).withTopK(10)
        );
        
        return docs.stream()
            .map(doc -> doc.getMetadata().get("productId").toString())
            .map(productService::getById)
            .toList();
    }
}
```

---

## 示例5：全栈搜索

### 实现代码

```java
@Service
@RequiredArgsConstructor
public class ProductSearchService {
    
    private final ElasticsearchOperations elasticsearchOperations;
    
    /**
     * 索引产品到Elasticsearch
     */
    public void index(Product product) {
        ProductDocument doc = new ProductDocument();
        doc.setId(product.getId());
        doc.setName(product.getName());
        doc.setDescription(product.getDescription());
        doc.setCategory(product.getCategory());
        doc.setPrice(product.getPrice());
        
        elasticsearchOperations.save(doc);
    }
    
    /**
     * 全文搜索
     */
    public List<Product> search(String keyword) {
        NativeSearchQuery query = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.multiMatchQuery(keyword, "name", "description"))
            .build();
        
        SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);
        
        return hits.getSearchHits().stream()
            .map(hit -> productService.getById(hit.getContent().getId()))
            .toList();
    }
}

@Document(indexName = "products")
@Data
class ProductDocument {
    @Id
    private String id;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String name;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String description;
    
    @Field(type = FieldType.Keyword)
    private String category;
    
    @Field(type = FieldType.Double)
    private BigDecimal price;
}
```

---

## 示例6：文件存储

### 实现代码

```java
@Service
@RequiredArgsConstructor
public class FileStorageService {
    
    private final MinioClient minioClient;
    
    /**
     * 上传文件
     */
    public String upload(MultipartFile file) throws Exception {
        String filename = IdGenerator.shortId() + "_" + file.getOriginalFilename();
        String bucketName = "products";
        
        // 创建bucket
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
        
        // 上传文件
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .object(filename)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build()
        );
        
        return filename;
    }
    
    /**
     * 下载文件
     */
    public InputStream download(String filename) throws Exception {
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket("products")
                .object(filename)
                .build()
        );
    }
}
```

---

## 示例7：任务调度

### 实现代码

```java
@Component
@RequiredArgsConstructor
public class ProductJob {
    
    private final ProductService productService;
    
    /**
     * 每天凌晨1点更新产品统计
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void updateStatistics() {
        log.info("开始更新产品统计");
        
        // 统计逻辑
        List<Product> products = productService.list();
        
        // 更新统计数据
        
        log.info("产品统计更新完成");
    }
    
    /**
     * 每小时检查库存预警
     */
    @Scheduled(cron = "0 0 * * * *")
    public void checkStockWarning() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(Product::getStock, 10);  // 库存<=10
        
        List<Product> lowStockProducts = productService.list(wrapper);
        
        if (!lowStockProducts.isEmpty()) {
            log.warn("低库存产品: {} 个", lowStockProducts.size());
            // 发送预警通知
        }
    }
}
```

---

## 票务系统完整应用

### 项目结构

```
ticket-monolith/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/ticketsystem/
│   │   │       ├── entity/          # 实体类
│   │   │       │   ├── Movie.java
│   │   │       │   ├── Cinema.java
│   │   │       │   ├── Showtime.java
│   │   │       │   ├── Order.java
│   │   │       │   └── User.java
│   │   │       ├── mapper/          # MyBatis Mapper
│   │   │       ├── service/         # 业务服务
│   │   │       ├── controller/      # REST API
│   │   │       ├── rpc/            # RPC接口
│   │   │       ├── listener/        # 消息监听
│   │   │       ├── job/            # 定时任务
│   │   │       └── ai/             # AI服务
│   │   └── resources/
│   │       ├── application.yml
│   │       └── mapper/             # MyBatis XML
│   └── test/
└── pom.xml
```

### 核心服务实现

**电影服务**:

```java
@Service
@RequiredArgsConstructor
public class MovieService extends ServiceImpl<MovieMapper, Movie> {
    
    private final ElasticsearchOperations elasticsearchOperations;
    private final VectorStore vectorStore;
    
    /**
     * 创建电影并索引
     */
    @Transactional
    public Movie create(Movie movie) {
        save(movie);
        
        // 索引到Elasticsearch
        indexToEs(movie);
        
        // 索引到向量数据库
        indexToVector(movie);
        
        return movie;
    }
    
    /**
     * 智能搜索
     */
    public List<Movie> intelligentSearch(String query) {
        // 1. 全文搜索
        List<Movie> esResults = searchFromEs(query);
        
        // 2. 向量搜索
        List<Movie> vectorResults = searchFromVector(query);
        
        // 3. 合并结果
        return mergeResults(esResults, vectorResults);
    }
}
```

**订单服务**:

```java
@Service
@RequiredArgsConstructor
public class OrderService extends ServiceImpl<OrderMapper, Order> {
    
    private final LockService lockService;
    private final MessageProducer messageProducer;
    
    /**
     * 创建订单
     */
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        String lockKey = "seat:" + request.getShowtimeId() + ":" + 
                        String.join(",", request.getSeatIds());
        
        // 获取分布式锁
        boolean locked = lockService.tryLock(lockKey, 3000, 10000);
        if (!locked) {
            throw new BusinessException("座位被锁定");
        }
        
        try {
            // 创建订单
            Order order = new Order();
            order.setId(IdGenerator.snowflakeIdString());
            order.setUserId(request.getUserId());
            order.setShowtimeId(request.getShowtimeId());
            order.setSeatIds(request.getSeatIds());
            order.setStatus(OrderStatus.PENDING);
            save(order);
            
            // 发送事件
            OrderCreatedEvent event = new OrderCreatedEvent();
            event.setOrderId(order.getId());
            messageProducer.send("order.created", event);
            
            return order;
        } finally {
            lockService.unlock(lockKey);
        }
    }
}
```

**AI客服**:

```java
@Service
@RequiredArgsConstructor
public class AICustomerService {
    
    private final ChatClient chatClient;
    private final RAGService ragService;
    private final ConversationService conversationService;
    
    /**
     * 处理用户咨询
     */
    public String handle(String sessionId, String message) {
        // 1. 检索相关信息
        String context = ragService.query(message);
        
        // 2. 多轮对话
        return conversationService.chat(sessionId, message, context);
    }
}
```

### 完整配置

`application.yml`:

```yaml
spring:
  application:
    name: ticket-monolith
  
  # 数据库
  datasource:
    url: jdbc:mysql://localhost:3306/ticket_db
    username: root
    password: password
  
  # Redis
  data:
    redis:
      host: localhost
      port: 6379
  
  # Elasticsearch
  elasticsearch:
    uris: http://localhost:9200
  
  # AI
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
    vectorstore:
      chroma:
        client:
          host: localhost
          port: 8000

# MinIO
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin

# RabbitMQ
rabbitmq:
  host: localhost
  port: 5672
```

---

## 最佳实践

1. **模块化设计**：按功能划分包结构
2. **统一异常处理**：使用全局异常处理器
3. **缓存策略**：合理使用多级缓存
4. **事务管理**：注意事务边界
5. **性能优化**：启用所需功能，禁用不用的

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [CONFIG.md](./CONFIG.md) - 配置参考
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 未来规划

---

> 如有问题或建议，欢迎提Issue。


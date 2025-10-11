# Nebula 框架使用指南

## 概述

Nebula 是基于 Spring Boot 3.x 和 Java 21 的现代化企业级Java后端框架，采用分层DDD架构设计，提供完整的基础设施支持和开箱即用的企业级功能。

## 🏗️ 架构概览

### 模块分层结构
```
nebula/
├── core/                    # 核心组件
│   └── nebula-foundation/   # 基础组件
├── infrastructure/          # 基础设施层
│   ├── data/                # 数据访问
│   ├── messaging/           # 消息传递
│   ├── rpc/                 # 远程调用
│   ├── discovery/           # 服务发现
│   ├── storage/             # 对象存储
│   ├── search/              # 搜索引擎
│   ├── integration/         # 第三方集成
│   └── ai/                  # 人工智能
├── application/             # 应用层
│   ├── nebula-web/          # Web应用框架
│   └── nebula-task/         # 任务调度框架
├── autoconfigure/           # 自动配置
│   └── nebula-autoconfigure/ # 统一自动配置模块
├── starter/                 # 启动器
│   └── nebula-starter/      # 便捷启动器（依赖 autoconfigure）
└── nebula-example/          # 示例应用
```

## 🔧 自动配置架构

### 统一自动配置模块

所有基础设施模块的自动配置类都集中管理在 `nebula-autoconfigure` 模块中，带来以下优势：

#### 核心优势

1. **集中式配置管理**
   - 所有自动配置类集中在一个模块中
   - 更清晰的依赖关系和初始化顺序
   - 避免了模块间的循环依赖问题

2. **更好的开发体验**
   - 应用只需引入 `nebula-autoconfigure` 依赖
   - 零配置启动，按需自动加载功能模块
   - 明确的配置顺序和依赖关系

3. **架构解耦**
   - 基础模块专注于核心功能实现
   - 配置逻辑分离到独立模块
   - 易于扩展和维护

#### 自动配置初始化顺序

```mermaid
flowchart TD
    A[NacosDiscoveryAutoConfiguration<br/>服务发现] --> B[HttpRpcAutoConfiguration<br/>HTTP RPC]
    A --> C[GrpcRpcAutoConfiguration<br/>gRPC]
    B --> D[RpcDiscoveryAutoConfiguration<br/>RPC+Discovery集成]
    C --> D
    D --> E[应用层服务]
    E --> F[DataPersistenceAutoConfiguration<br/>数据持久化]
    E --> G[CacheAutoConfiguration<br/>缓存]
    E --> H[RabbitMQAutoConfiguration<br/>消息队列]
    E --> I[ElasticsearchAutoConfiguration<br/>搜索引擎]
    E --> J[StorageAutoConfiguration<br/>对象存储]
    E --> K[AIAutoConfiguration<br/>AI服务]
```

#### 快速开始

**1. 添加自动配置依赖**

```xml
<!-- 统一自动配置模块 -->
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-autoconfigure</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>

<!-- 按需添加功能模块 -->
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-discovery-nacos</artifactId>
</dependency>
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-rpc-http</artifactId>
</dependency>
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-data-persistence</artifactId>
</dependency>
<!-- 其他模块... -->
```

**2. 配置应用**

```yaml
spring:
  application:
    name: my-nebula-app

nebula:
  # 服务发现配置
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
      namespace: dev
  
  # RPC 配置
  rpc:
    http:
      enabled: true
    discovery:
      enabled: true
  
  # 数据访问配置
  data:
    persistence:
      enabled: true
    cache:
      enabled: true
```

**3. 启动应用**

所有配置的功能模块将自动初始化并可用，无需手动配置。


## 📦 核心模块详解

<!-- ### 1. 数据访问层 (Data Access Layer)

#### nebula-data-access
**核心抽象层，提供统一的数据访问接口**
```java
// 统一Repository接口
@Repository
public class UserRepository extends AbstractRepository<User, Long> {
    
    @Override
    protected Class<User> getEntityClass() {
        return User.class;
    }
}

// 查询构建器
QueryBuilder query = DefaultQueryBuilder.create()
    .eq("status", "ACTIVE")
    .like("name", "张%")
    .gt("createTime", lastWeek)
    .build();
``` -->

#### nebula-data-persistence (MyBatis-Plus集成)
**关系型数据库持久化支持**
```yaml
nebula:
  data:
    persistence:
      # 读写分离配置
      read-write-separation:
        enabled: true
        master:
          url: jdbc:mysql://master:3306/nebula
          username: root
          password: password
        slave:
          url: jdbc:mysql://slave:3306/nebula
          username: reader
          password: password
      # 分库分表配置  
      sharding:
        enabled: true
        tables:
          user:
            actual-data-nodes: ds_${0..1}.user_${0..3}
            table-strategy:
              inline:
                sharding-column: id
                algorithm-expression: user_${id % 4}
```

<!-- #### nebula-data-mongodb (NoSQL支持)
**MongoDB集成支持**
```java
@Service
public class DocumentService {
    
    @Autowired
    private MongoRepository mongoRepository;
    
    public void saveDocument(Document doc) {
        mongoRepository.save(doc);
    }
    
    public List<Document> findByCategory(String category) {
        return mongoRepository.findByCategory(category);
    }
}
``` -->

#### nebula-data-cache (缓存支持)
**多级缓存管理**
```yaml
nebula:
  data:
    cache:
      # 多级缓存配置
      multi-level:
        enabled: true
        local:
          type: caffeine
          max-size: 10000
          expire-after-write: 5m
        remote:
          type: redis
          expire-after-write: 1h
          key-prefix: "nebula:"
```

### 2. 消息传递层 (Messaging Layer)

#### nebula-messaging-core & nebula-messaging-rabbitmq
**消息队列抽象和RabbitMQ实现**
```java
@Service
public class NotificationService {
    
    @Autowired
    private MessageManager messageManager;
    
    public void sendNotification(String userId, String message) {
        Message<String> msg = Message.<String>builder()
            .topic("user-notifications")
            .payload(message)
            .build();
            
        messageManager.getProducer().send("user-notifications", msg);
    }
    
    @MessageHandler("user-notifications")
    public void handleNotification(Message<String> message) {
        // 处理通知消息
        log.info("处理通知: {}", message.getPayload());
    }
}
```

### 3. 服务发现与RPC层

#### nebula-discovery-core & nebula-discovery-nacos
**服务注册发现**
```yaml
nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
      namespace: nebula-dev
      group: DEFAULT_GROUP
```

#### nebula-rpc-core & nebula-rpc-http  
**远程调用支持**
```java
@RpcClient("user-service")
public interface UserRpcClient {
    
    @RpcCall("/api/users/{id}")
    User getUserById(@PathParam("id") Long id);
    
    @RpcCall(value = "/api/users", method = "POST")
    User createUser(@RequestBody CreateUserRequest request);
}
```

### 4. 对象存储层 (Storage Layer)

#### nebula-storage-core, nebula-storage-minio, nebula-storage-aliyun-oss
**统一对象存储接口**
```java
@Service
public class FileService {
    
    @Autowired
    private StorageService storageService;
    
    public String uploadFile(MultipartFile file) {
        ObjectMetadata metadata = ObjectMetadata.builder()
            .contentType(file.getContentType())
            .contentLength(file.getSize())
            .build();
            
        StorageResult result = storageService.upload(
            "documents/" + file.getOriginalFilename(),
            file.getInputStream(),
            metadata
        );
        
        return result.getUrl();
    }
}
```

**配置示例:**
```yaml
nebula:
  storage:
    # MinIO配置
    minio:
      enabled: true
      endpoint: http://localhost:9000
      access-key: minioadmin
      secret-key: minioadmin
      default-bucket: nebula-files
    
    # 阿里云OSS配置  
    aliyun:
      oss:
        enabled: false
        endpoint: https://oss-cn-hangzhou.aliyuncs.com
        access-key-id: ${ALIYUN_ACCESS_KEY}
        access-key-secret: ${ALIYUN_SECRET_KEY}
        default-bucket: nebula-oss
```

### 5. 搜索引擎层 (Search Layer)

#### nebula-search-core & nebula-search-elasticsearch
**全文搜索支持**
```java
@Service
public class ProductSearchService {
    
    @Autowired
    private SearchService searchService;
    
    public void indexProduct(Product product) {
        SearchDocument document = SearchDocument.builder()
            .id(product.getId().toString())
            .content(product.getName() + " " + product.getDescription())
            .metadata(Map.of(
                "category", product.getCategory(),
                "price", product.getPrice(),
                "brand", product.getBrand()
            ))
            .build();
            
        searchService.index("products", document);
    }
    
    public SearchResult searchProducts(String query, String category) {
        SearchQuery searchQuery = SearchQuery.builder()
            .query(query)
            .filter("category", category)
            .size(20)
            .build();
            
        return searchService.search("products", searchQuery);
    }
}
```

### 6. 第三方集成层 (Integration Layer)

#### nebula-integration-payment
**支付集成抽象**
```java
@Service
public class OrderService {
    
    @Autowired
    private PaymentService paymentService;
    
    public PaymentResponse createPayment(Order order) {
        PaymentRequest request = PaymentRequest.builder()
            .orderNo(order.getOrderNo())
            .amount(order.getTotalAmount())
            .currency("CNY")
            .subject(order.getTitle())
            .buyerInfo(BuyerInfo.builder()
                .buyerId(order.getUserId().toString())
                .buyerName(order.getUserName())
                .build())
            .build();
            
        return paymentService.createPayment(request);
    }
}
```

**配置示例:**
```yaml
nebula:
  payment:
    # Mock支付（开发测试）
    mock:
      enabled: true
      auto-success-delay: 60
    
    # 支付宝配置
    alipay:
      enabled: false
      app-id: ${ALIPAY_APP_ID}
      private-key: ${ALIPAY_PRIVATE_KEY}
      public-key: ${ALIPAY_PUBLIC_KEY}
    
    # 微信支付配置
    wechat-pay:
      enabled: false
      app-id: ${WECHAT_APP_ID}
      mch-id: ${WECHAT_MCH_ID}
      mch-key: ${WECHAT_MCH_KEY}
```

### 7. 人工智能层 (AI Layer)

#### nebula-ai-core & nebula-ai-spring
**AI能力集成 (基于Spring AI)**
```java
@Service
public class AIService {
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private EmbeddingService embeddingService;
    
    @Autowired
    private VectorStoreService vectorStoreService;
    
    // 智能聊天
    public String chat(String message) {
        ChatResponse response = chatService.chat(message);
        return response.getContent();
    }
    
    // 文档智能问答 (RAG)
    public String intelligentQA(String question) {
        // 1. 搜索相关文档
        SearchResult searchResult = vectorStoreService.search(question, 3);
        
        // 2. 构建上下文
        String context = searchResult.getContents()
            .stream()
            .collect(Collectors.joining("\n"));
        
        // 3. 生成回答
        List<ChatMessage> messages = List.of(
            ChatMessage.system("基于以下上下文回答问题:\n" + context),
            ChatMessage.user(question)
        );
        
        return chatService.chat(messages).getContent();
    }
}
```

**配置示例:**
```yaml
nebula:
  ai:
    enabled: true
    # 聊天配置
    chat:
      default-provider: openai
      providers:
        openai:
          api-key: ${OPENAI_API_KEY}
          model: gpt-3.5-turbo
          options:
            temperature: 0.7
            max-tokens: 1000
    
    # 嵌入配置
    embedding:
      default-provider: openai
      providers:
        openai:
          api-key: ${OPENAI_API_KEY}
          model: text-embedding-ada-002
    
    # 向量存储配置        
    vector-store:
      default-provider: chroma
      providers:
        chroma:
          host: localhost
          port: 8000
          collection-name: nebula-docs
```

## 🚀 快速开始

Nebula 提供两种使用方式，根据需求选择：

### 方式一：使用 nebula-starter（推荐）

适合需要完整功能、快速开始的应用。

#### 1. 添加依赖
```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### 方式二：使用 nebula-autoconfigure

适合需要精确控制依赖的应用。

#### 1. 添加依赖
```xml
<!-- 统一自动配置 -->
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-autoconfigure</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>

<!-- 按需添加功能模块 -->
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-discovery-nacos</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>

<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-rpc-http</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>

<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-data-persistence</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
<!-- 其他模块... -->
```

### 2. 应用配置（两种方式通用）
```yaml
spring:
  application:
    name: my-nebula-app
  
nebula:
  # 启用需要的模块
  data:
    persistence:
      enabled: true
    cache:
      enabled: true
  messaging:
    rabbitmq:
      enabled: true
  storage:
    minio:
      enabled: true
  search:
    elasticsearch:
      enabled: true
  ai:
    enabled: true
```

### 3. 启动类
```java
@SpringBootApplication
public class MyNebulaApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyNebulaApplication.class, args);
    }
}
```

**说明**：
- v2.0.1+ 不再需要 `@EnableNebula` 注解
- 所有功能通过自动配置自动启用
- 使用配置文件中的 `enabled` 属性控制功能开关

## 🔧 开发指南

### 自定义Repository
```java
public interface UserRepository extends BaseRepository<User, Long> {
    
    @Query("SELECT * FROM users WHERE status = #{status}")
    List<User> findByStatus(String status);
    
    @Cacheable(value = "users", key = "#email")
    User findByEmail(String email);
}
```

### 消息处理器
```java
@Component
public class OrderEventHandler {
    
    @MessageHandler("order.created")
    public void handleOrderCreated(Message<Order> message) {
        Order order = message.getPayload();
        // 处理订单创建事件
    }
    
    @MessageHandler("order.cancelled") 
    public void handleOrderCancelled(Message<String> message) {
        String orderId = message.getPayload();
        // 处理订单取消事件
    }
}
```

### Web控制器
```java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    @GetMapping
    public Page<Product> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return productService.searchProducts(keyword, page, size);
    }
    
    @PostMapping
    public Product createProduct(@Valid @RequestBody CreateProductRequest request) {
        return productService.createProduct(request);
    }
}
```

## 🐳 部署指南

### Docker Compose 示例
```yaml
version: '3.8'
services:
  app:
    image: my-nebula-app:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - MYSQL_HOST=mysql
      - REDIS_HOST=redis
      - RABBITMQ_HOST=rabbitmq
      - MINIO_ENDPOINT=http://minio:9000
      - ELASTICSEARCH_URIS=http://elasticsearch:9200
      - OPENAI_API_KEY=${OPENAI_API_KEY}
    depends_on:
      - mysql
      - redis
      - rabbitmq
      - minio
      - elasticsearch
      - chroma

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: nebula
      MYSQL_ROOT_PASSWORD: password
    volumes:
      - mysql_data:/var/lib/mysql

  redis:
    image: redis:7-alpine
    volumes:
      - redis_data:/data

  rabbitmq:
    image: rabbitmq:3-management-alpine
    environment:
      RABBITMQ_DEFAULT_USER: nebula
      RABBITMQ_DEFAULT_PASS: password
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq

  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    volumes:
      - minio_data:/data

  elasticsearch:
    image: elasticsearch:8.11.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    volumes:
      - es_data:/usr/share/elasticsearch/data

  chroma:
    image: chromadb/chroma:latest
    ports:
      - "8000:8000"
    volumes:
      - chroma_data:/chroma/chroma

volumes:
  mysql_data:
  redis_data:
  rabbitmq_data:
  minio_data:
  es_data:
  chroma_data:
```

## 🧪 测试指南

### 单元测试
```java
@SpringBootTest
@TestConfiguration
class ProductServiceTest {
    
    @Autowired
    private ProductService productService;
    
    @MockBean
    private ProductRepository productRepository;
    
    @Test
    void shouldCreateProduct() {
        CreateProductRequest request = CreateProductRequest.builder()
            .name("测试商品")
            .price(BigDecimal.valueOf(99.99))
            .build();
            
        Product result = productService.createProduct(request);
        
        assertThat(result.getName()).isEqualTo("测试商品");
        assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(99.99));
    }
}
```

### 集成测试
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:test.properties")
class ProductIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldCreateAndRetrieveProduct() {
        // 创建商品
        CreateProductRequest request = new CreateProductRequest("测试商品", BigDecimal.valueOf(99.99));
        ResponseEntity<Product> createResponse = restTemplate.postForEntity(
            "/api/products", request, Product.class);
            
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        // 查询商品
        Long productId = createResponse.getBody().getId();
        ResponseEntity<Product> getResponse = restTemplate.getForEntity(
            "/api/products/" + productId, Product.class);
            
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getName()).isEqualTo("测试商品");
    }
}
```

## 📊 监控与运维

### 应用指标
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
        
nebula:
  monitoring:
    enabled: true
    metrics:
      # 自动收集各模块指标
      data-access: true
      messaging: true
      storage: true
      search: true
      ai: true
```

### 健康检查
```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Autowired
    private PaymentService paymentService;
    
    @Override
    public Health health() {
        if (paymentService.isAvailable()) {
            return Health.up()
                .withDetail("payment", "服务正常")
                .build();
        } else {
            return Health.down()
                .withDetail("payment", "服务不可用")
                .build();
        }
    }
}
```

## 🎯 最佳实践

### 1. 配置管理
- 使用环境变量管理敏感配置
- 不同环境使用不同的配置文件
- 合理使用配置优先级

### 2. 异常处理
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        return ResponseEntity.badRequest()
            .body(ErrorResponse.builder()
                .code(e.getErrorCode())
                .message(e.getMessage())
                .build());
    }
}
```

### 3. 日志规范
```java
@Slf4j
@Service
public class UserService {
    
    public User createUser(CreateUserRequest request) {
        log.info("开始创建用户: email={}", request.getEmail());
        
        try {
            User user = userRepository.save(convertToEntity(request));
            log.info("用户创建成功: id={}, email={}", user.getId(), user.getEmail());
            return user;
        } catch (Exception e) {
            log.error("用户创建失败: email={}, error={}", request.getEmail(), e.getMessage(), e);
            throw e;
        }
    }
}
```

### 4. 缓存策略
```java
@Service
public class UserService {
    
    @Cacheable(value = "users", key = "#id")
    public User getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    @CacheEvict(value = "users", key = "#user.id")
    public User updateUser(User user) {
        return userRepository.save(user);
    }
}
```

## 🔗 相关链接

- **GitHub 仓库**: [nebula-framework](https://github.com/your-org/nebula-framework)
- **官方文档**: [https://nebula.docs.your-domain.com](https://nebula.docs.your-domain.com)
- **API文档**: [https://nebula.api.your-domain.com](https://nebula.api.your-domain.com)
- **问题反馈**: [GitHub Issues](https://github.com/your-org/nebula-framework/issues)

## 📄 许可证

Nebula 框架基于 MIT 许可证开源。详见 [LICENSE](../LICENSE) 文件。

---

**Nebula框架 - 现代化的企业级Java后端框架** 🚀

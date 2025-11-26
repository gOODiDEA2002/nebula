# Nebula Starter All - 测试指南

> 全功能Starter的完整测试指南，包括所有模块的测试方法。

## 测试概览

- [测试环境准备](#测试环境准备)
- [单元测试](#单元测试)
- [集成测试](#集成测试)
- [完整功能测试](#完整功能测试)

---

## 测试环境准备

### Maven依赖

```xml
<dependencies>
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-all</artifactId>
        <version>2.0.1-SNAPSHOT</version>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mysql</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>redis</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 测试配置

`application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  
  data:
    redis:
      host: localhost
      port: 6379
```

---

## 单元测试

### Service测试

```java
@SpringBootTest
class ProductServiceTest {
    
    @Autowired
    private ProductService productService;
    
    @Test
    void testCreateProduct() {
        Product product = new Product();
        product.setName("测试产品");
        product.setPrice(new BigDecimal("99.99"));
        
        Product created = productService.create(product);
        
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("测试产品");
    }
}
```

---

## 集成测试

### 使用Testcontainers

```java
@SpringBootTest
@Testcontainers
class IntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }
    
    @Autowired
    private ProductService productService;
    
    @Test
    void testCompleteFlow() {
        // 测试完整流程
        Product product = productService.create(new Product());
        assertThat(product).isNotNull();
    }
}
```

---

## 完整功能测试

### 票务系统集成测试

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class TicketSystemIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testCompleteTicketPurchaseFlow() {
        // 1. 查询电影
        // 2. 选择场次
        // 3. 选座
        // 4. 创建订单
        // 5. 支付
        // 验证完整流程
    }
}
```

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [EXAMPLE.md](./EXAMPLE.md) - 使用示例
- [CONFIG.md](./CONFIG.md) - 配置参考
- [ROADMAP.md](./ROADMAP.md) - 未来规划

---

> 如有问题或建议，欢迎提Issue。


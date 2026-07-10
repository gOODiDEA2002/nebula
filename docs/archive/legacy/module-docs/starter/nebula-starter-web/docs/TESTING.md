# Nebula Starter Web - 测试指南

> Web应用专用Starter的完整测试指南，包括单元测试、集成测试、API测试和性能测试。

## 测试概览

本文档包含以下测试内容：

- [测试环境准备](#测试环境准备)
- [单元测试](#单元测试)
- [集成测试](#集成测试)
- [API测试](#api测试)
- [Controller测试](#controller测试)
- [Service测试](#service测试)
- [Repository测试](#repository测试)
- [安全测试](#安全测试)
- [性能测试](#性能测试)
- [票务系统测试示例](#票务系统测试示例)

---

## 测试环境准备

### Maven依赖

在 `pom.xml` 中添加测试依赖：

```xml
<dependencies>
    <!-- Nebula Web Starter -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-web</artifactId>
        <version>2.0.1-SNAPSHOT</version>
    </dependency>
    
    <!-- Spring Boot Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- RestAssured (API测试) -->
    <dependency>
        <groupId>io.rest-assured</groupId>
        <artifactId>rest-assured</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Testcontainers (容器化测试) -->
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
    
    <!-- H2 Database (测试数据库) -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 测试配置文件

创建 `src/test/resources/application-test.yml`:

```yaml
spring:
  application:
    name: test-app
  
  # 使用H2内存数据库
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  
  # Redis（使用嵌入式或Testcontainers）
  data:
    redis:
      host: localhost
      port: 6379
  
  # JPA配置
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

# 日志配置
logging:
  level:
    root: INFO
    io.nebula: DEBUG
    com.example: DEBUG
```

---

## 单元测试

### 测试Controller层

```java
package com.example.web.controller;

import com.example.web.service.ProductService;
import com.example.web.entity.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ProductController测试
 */
@WebMvcTest(ProductController.class)
@DisplayName("商品Controller测试")
class ProductControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ProductService productService;
    
    @Test
    @DisplayName("查询商品")
    void testGetById() throws Exception {
        // 准备数据
        Product product = new Product();
        product.setId("1");
        product.setName("测试商品");
        product.setPrice(new BigDecimal("99.99"));
        
        // Mock服务层
        when(productService.getById("1")).thenReturn(product);
        
        // 执行测试
        mockMvc.perform(get("/api/products/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.id").value("1"))
            .andExpect(jsonPath("$.data.name").value("测试商品"))
            .andExpect(jsonPath("$.data.price").value(99.99));
        
        // 验证调用
        verify(productService, times(1)).getById("1");
    }
    
    @Test
    @DisplayName("创建商品")
    void testCreate() throws Exception {
        // 准备数据
        Product product = new Product();
        product.setId("1");
        product.setName("新商品");
        
        when(productService.create(any(Product.class))).thenReturn(product);
        
        // 执行测试
        String json = "{\"name\":\"新商品\",\"price\":99.99}";
        
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.id").value("1"));
        
        verify(productService, times(1)).create(any(Product.class));
    }
    
    @Test
    @DisplayName("参数验证失败")
    void testValidationFailure() throws Exception {
        String json = "{\"name\":\"\",\"price\":-1}";  // 无效数据
        
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
```

### 测试Service层

```java
package com.example.web.service;

import com.example.web.entity.Product;
import com.example.web.repository.ProductRepository;
import io.nebula.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ProductService测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("商品Service测试")
class ProductServiceTest {
    
    @Mock
    private ProductRepository productRepository;
    
    @InjectMocks
    private ProductService productService;
    
    @Test
    @DisplayName("创建商品成功")
    void testCreateSuccess() {
        // 准备数据
        Product product = new Product();
        product.setName("测试商品");
        product.setPrice(new BigDecimal("99.99"));
        
        when(productRepository.save(any(Product.class))).thenReturn(product);
        
        // 执行测试
        Product result = productService.create(product);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getCreateTime()).isNotNull();
        
        verify(productRepository, times(1)).save(any(Product.class));
    }
    
    @Test
    @DisplayName("查询不存在的商品")
    void testGetByIdNotFound() {
        when(productRepository.findById("999")).thenReturn(null);
        
        assertThatThrownBy(() -> productService.getById("999"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("商品不存在");
    }
    
    @Test
    @DisplayName("更新商品")
    void testUpdate() {
        // 准备数据
        Product existing = new Product();
        existing.setId("1");
        existing.setName("旧名称");
        
        Product updated = new Product();
        updated.setName("新名称");
        
        when(productRepository.findById("1")).thenReturn(existing);
        when(productRepository.save(any(Product.class))).thenReturn(updated);
        
        // 执行测试
        Product result = productService.update("1", updated);
        
        // 验证结果
        assertThat(result.getName()).isEqualTo("新名称");
        assertThat(result.getUpdateTime()).isNotNull();
    }
}
```

---

## 集成测试

### Spring Boot集成测试

```java
package com.example.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

/**
 * 集成测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class IntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testHealthEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }
    
    @Test
    void testApiEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/hello", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

### 使用Testcontainers

```java
package com.example.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 使用Testcontainers的集成测试
 */
@SpringBootTest
@Testcontainers
class ContainerIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // MySQL配置
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        
        // Redis配置
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }
    
    @Autowired
    private ProductService productService;
    
    @Test
    void testWithContainers() {
        // 测试逻辑
        Product product = new Product();
        product.setName("测试商品");
        
        Product saved = productService.create(product);
        assertThat(saved.getId()).isNotNull();
    }
}
```

---

## API测试

### 使用RestAssured

```java
package com.example.web.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * API测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("API测试")
class ApiTest {
    
    @LocalServerPort
    private int port;
    
    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api";
    }
    
    @Test
    @DisplayName("测试获取商品列表")
    void testGetProducts() {
        given()
            .when()
            .get("/products")
            .then()
            .statusCode(200)
            .body("code", equalTo("0"))
            .body("data", notNullValue());
    }
    
    @Test
    @DisplayName("测试创建商品")
    void testCreateProduct() {
        String json = """
            {
                "name": "测试商品",
                "price": 99.99,
                "stock": 100
            }
            """;
        
        given()
            .contentType(ContentType.JSON)
            .body(json)
            .when()
            .post("/products")
            .then()
            .statusCode(200)
            .body("code", equalTo("0"))
            .body("data.id", notNullValue())
            .body("data.name", equalTo("测试商品"));
    }
    
    @Test
    @DisplayName("测试JWT认证")
    void testJwtAuth() {
        // 1. 登录获取Token
        String loginJson = """
            {
                "username": "admin",
                "password": "123456"
            }
            """;
        
        String token = given()
            .contentType(ContentType.JSON)
            .body(loginJson)
            .when()
            .post("/auth/login")
            .then()
            .statusCode(200)
            .extract()
            .path("data.token");
        
        // 2. 使用Token访问受保护的API
        given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/auth/me")
            .then()
            .statusCode(200)
            .body("code", equalTo("0"))
            .body("data.username", equalTo("admin"));
    }
}
```

---

## Controller测试

### 完整的Controller测试

```java
@WebMvcTest(MovieController.class)
@DisplayName("电影Controller测试")
class MovieControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private MovieService movieService;
    
    @Test
    @DisplayName("分页查询电影")
    void testPage() throws Exception {
        // 准备数据
        List<Movie> movies = Arrays.asList(
            new Movie("1", "电影1"),
            new Movie("2", "电影2")
        );
        PageResult<Movie> page = PageResult.of(movies, 2, 1, 10);
        
        when(movieService.page(1, 10, null)).thenReturn(page);
        
        // 执行测试
        mockMvc.perform(get("/api/movies")
                .param("pageNum", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.records").isArray())
            .andExpect(jsonPath("$.data.records", hasSize(2)))
            .andExpect(jsonPath("$.data.total").value(2));
    }
    
    @Test
    @DisplayName("搜索电影")
    void testSearch() throws Exception {
        List<Movie> movies = Arrays.asList(new Movie("1", "阿凡达"));
        PageResult<Movie> page = PageResult.of(movies, 1, 1, 10);
        
        when(movieService.page(1, 10, "阿凡达")).thenReturn(page);
        
        mockMvc.perform(get("/api/movies")
                .param("keyword", "阿凡达"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.records[0].name").value("阿凡达"));
    }
}
```

---

## Repository测试

### MyBatis Mapper测试

```java
@SpringBootTest
@DisplayName("ProductMapper测试")
class ProductMapperTest {
    
    @Autowired
    private ProductMapper productMapper;
    
    @Test
    @Transactional
    @DisplayName("插入商品")
    void testInsert() {
        Product product = new Product();
        product.setName("测试商品");
        product.setPrice(new BigDecimal("99.99"));
        
        int rows = productMapper.insert(product);
        
        assertThat(rows).isEqualTo(1);
        assertThat(product.getId()).isNotNull();
    }
    
    @Test
    @DisplayName("查询商品")
    void testSelectById() {
        Product product = productMapper.selectById("1");
        
        assertThat(product).isNotNull();
        assertThat(product.getName()).isNotBlank();
    }
    
    @Test
    @DisplayName("分页查询")
    void testSelectPage() {
        IPage<Product> page = new Page<>(1, 10);
        IPage<Product> result = productMapper.selectPage(page, null);
        
        assertThat(result.getRecords()).isNotEmpty();
        assertThat(result.getTotal()).isGreaterThan(0);
    }
}
```

---

## 安全测试

### JWT认证测试

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("JWT认证测试")
class JwtAuthTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    @DisplayName("未认证访问受保护的API")
    void testUnauthorized() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/protected", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    
    @Test
    @DisplayName("使用Token访问受保护的API")
    void testWithToken() {
        // 1. 登录获取Token
        LoginRequest loginRequest = new LoginRequest("admin", "123456");
        ResponseEntity<Result> loginResponse = restTemplate.postForEntity(
            "/api/auth/login",
            loginRequest,
            Result.class
        );
        
        String token = (String) ((Map) loginResponse.getBody().getData()).get("token");
        
        // 2. 使用Token访问API
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/protected",
            HttpMethod.GET,
            entity,
            String.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

---

## 性能测试

### 使用JMeter

创建JMeter测试计划 `performance-test.jmx`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan>
      <stringProp name="TestPlan.comments">性能测试计划</stringProp>
      <boolProp name="TestPlan.functional_mode">false</boolProp>
      <stringProp name="TestPlan.user_defined_variables"/>
    </TestPlan>
    <hashTree>
      <ThreadGroup>
        <stringProp name="ThreadGroup.num_threads">100</stringProp>
        <stringProp name="ThreadGroup.ramp_time">10</stringProp>
        <stringProp name="ThreadGroup.duration">60</stringProp>
      </ThreadGroup>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

运行性能测试：

```bash
jmeter -n -t performance-test.jmx -l results.jtl -e -o report/
```

### 使用Gatling

```scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class PerformanceTest extends Simulation {
  
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
  
  val scn = scenario("Product API Test")
    .exec(http("Get Products")
      .get("/api/products")
      .check(status.is(200)))
    .pause(1)
  
  setUp(
    scn.inject(rampUsers(100) during (10 seconds))
  ).protocols(httpProtocol)
}
```

---

## 票务系统测试示例

### 场景1：购票流程测试

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("购票流程测试")
class TicketPurchaseFlowTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    @DisplayName("完整购票流程")
    void testCompletePurchaseFlow() {
        // 1. 登录
        String token = login("user@example.com", "password");
        
        // 2. 查询电影
        Movie movie = getMovie("阿凡达2");
        assertThat(movie).isNotNull();
        
        // 3. 查询场次
        List<Showtime> showtimes = getShowtimes(movie.getId(), LocalDate.now());
        assertThat(showtimes).isNotEmpty();
        
        // 4. 查询座位
        Showtime showtime = showtimes.get(0);
        List<Seat> seats = getSeats(showtime.getId());
        assertThat(seats).isNotEmpty();
        
        // 5. 锁定座位
        List<String> seatIds = Arrays.asList(seats.get(0).getId());
        String lockToken = lockSeats(showtime.getId(), seatIds, token);
        assertThat(lockToken).isNotNull();
        
        // 6. 创建订单
        Order order = createOrder(showtime.getId(), seatIds, lockToken, token);
        assertThat(order).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        
        // 7. 支付订单
        PaymentResult payment = payOrder(order.getId(), token);
        assertThat(payment.isSuccess()).isTrue();
        
        // 8. 验证订单状态
        Order paidOrder = getOrder(order.getId(), token);
        assertThat(paidOrder.getStatus()).isEqualTo(OrderStatus.PAID);
    }
    
    private String login(String username, String password) {
        // 实现登录逻辑
        return "mock-token";
    }
    
    // ... 其他辅助方法
}
```

### 场景2：并发购票测试

```java
@SpringBootTest
@DisplayName("并发购票测试")
class ConcurrentPurchaseTest {
    
    @Autowired
    private TicketService ticketService;
    
    @Test
    @DisplayName("多用户同时购买相同座位")
    void testConcurrentPurchaseSameSeat() throws InterruptedException {
        String showtimeId = "1";
        String seatId = "A01";
        
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    ticketService.lockSeats(showtimeId, Arrays.asList(seatId));
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // 只有一个成功
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(9);
    }
}
```

---

## 测试覆盖率

### 配置JaCoCo

在 `pom.xml` 中配置：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.11</version>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
                <execution>
                    <id>check</id>
                    <goals>
                        <goal>check</goal>
                    </goals>
                    <configuration>
                        <rules>
                            <rule>
                                <element>PACKAGE</element>
                                <limits>
                                    <limit>
                                        <counter>LINE</counter>
                                        <value>COVEREDRATIO</value>
                                        <minimum>0.80</minimum>
                                    </limit>
                                </limits>
                            </rule>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

运行测试并生成报告：

```bash
mvn clean test
mvn jacoco:report

# 查看报告
open target/site/jacoco/index.html
```

---

## 最佳实践

### 实践1：使用@DisplayName

```java
@Test
@DisplayName("当商品不存在时抛出异常")
void testProductNotFound() {
    // 测试代码
}
```

### 实践2：使用AssertJ进行断言

```java
assertThat(product.getName()).isEqualTo("商品名称");
assertThat(products).hasSize(3);
assertThat(result).isNotNull();
```

### 实践3：使用@Transactional回滚测试数据

```java
@Test
@Transactional
void testCreate() {
    // 测试后自动回滚
}
```

### 实践4：使用@TestMethodOrder控制测试顺序

```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderedTest {
    
    @Test
    @Order(1)
    void test1() {}
    
    @Test
    @Order(2)
    void test2() {}
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


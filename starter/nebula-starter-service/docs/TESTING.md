# Nebula Starter Service - 测试指南

> 微服务应用专用Starter的完整测试指南，包括单元测试、集成测试、RPC测试、消息队列测试等。

## 测试概览

- [测试环境准备](#测试环境准备)
- [单元测试](#单元测试)
- [RPC测试](#rpc测试)
- [服务发现测试](#服务发现测试)
- [消息队列测试](#消息队列测试)
- [分布式锁测试](#分布式锁测试)
- [集成测试](#集成测试)
- [契约测试](#契约测试)
- [票务系统测试示例](#票务系统测试示例)

---

## 测试环境准备

### Maven依赖

```xml
<dependencies>
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-service</artifactId>
        <version>2.0.1-SNAPSHOT</version>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Testcontainers -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>rabbitmq</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- WireMock（RPC测试） -->
    <dependency>
        <groupId>com.github.tomakehurst</groupId>
        <artifactId>wiremock-jre8</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 测试配置

`src/test/resources/application-test.yml`:

```yaml
spring:
  application:
    name: test-service

nebula:
  discovery:
    nacos:
      enabled: false
  rpc:
    http:
      enabled: true
  messaging:
    rabbitmq:
      enabled: true
```

---

## 单元测试

### 测试Service层

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("订单Service测试")
class OrderServiceTest {
    
    @Mock
    private UserRpcService userRpcService;
    
    @Mock
    private MessageProducer messageProducer;
    
    @InjectMocks
    private OrderService orderService;
    
    @Test
    @DisplayName("创建订单成功")
    void testCreateOrderSuccess() {
        // Mock RPC调用
        when(userRpcService.getById("123"))
            .thenReturn(Result.success(new User("123", "张三")));
        
        // 执行测试
        Order order = orderService.createOrder(request);
        
        // 验证结果
        assertThat(order).isNotNull();
        assertThat(order.getUserId()).isEqualTo("123");
        
        // 验证RPC调用
        verify(userRpcService, times(1)).getById("123");
        
        // 验证消息发送
        verify(messageProducer, times(1)).send(eq("order.created"), any());
    }
}
```

---

## RPC测试

### 使用WireMock测试RPC调用

```java
@SpringBootTest
@DisplayName("RPC调用测试")
class RpcClientTest {
    
    @Autowired
    private OrderService orderService;
    
    private WireMockServer wireMockServer;
    
    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }
    
    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }
    
    @Test
    @DisplayName("RPC调用成功")
    void testRpcCallSuccess() {
        // Mock RPC响应
        stubFor(post(urlEqualTo("/rpc/user/getById"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"code\":\"0\",\"data\":{\"id\":\"123\",\"name\":\"张三\"}}")));
        
        // 执行测试
        Result<User> result = userRpcService.getById("123");
        
        // 验证结果
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getId()).isEqualTo("123");
        
        // 验证调用
        verify(postRequestedFor(urlEqualTo("/rpc/user/getById")));
    }
}
```

---

## 服务发现测试

```java
@SpringBootTest
@Testcontainers
@DisplayName("服务发现测试")
class ServiceDiscoveryTest {
    
    @Container
    static GenericContainer<?> nacos = new GenericContainer<>("nacos/nacos-server:v2.2.3")
        .withExposedPorts(8848)
        .withEnv("MODE", "standalone");
    
    @DynamicPropertySource
    static void nacosProperties(DynamicPropertyRegistry registry) {
        registry.add("nebula.discovery.nacos.server-addr", 
            () -> nacos.getHost() + ":" + nacos.getFirstMappedPort());
    }
    
    @Test
    @DisplayName("服务注册成功")
    void testServiceRegistration() {
        // 验证服务已注册
        // 实际测试逻辑
    }
}
```

---

## 消息队列测试

### 使用Testcontainers测试RabbitMQ

```java
@SpringBootTest
@Testcontainers
@DisplayName("消息队列测试")
class MessageQueueTest {
    
    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.12-alpine");
    
    @DynamicPropertySource
    static void rabbitmqProperties(DynamicPropertyRegistry registry) {
        registry.add("nebula.messaging.rabbitmq.host", rabbitmq::getHost);
        registry.add("nebula.messaging.rabbitmq.port", rabbitmq::getAmqpPort);
    }
    
    @Autowired
    private MessageProducer messageProducer;
    
    @Test
    @DisplayName("发送消息成功")
    void testSendMessage() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId("123");
        
        messageProducer.send("order.created", event);
        
        // 等待消息处理
        await().atMost(5, TimeUnit.SECONDS)
            .until(() -> messageReceived);
    }
}

@Component
class TestMessageListener {
    
    private boolean received = false;
    
    @MessageHandler(topic = "order.created")
    public void handle(OrderCreatedEvent event) {
        received = true;
    }
    
    public boolean isReceived() {
        return received;
    }
}
```

---

## 分布式锁测试

```java
@SpringBootTest
@DisplayName("分布式锁测试")
class DistributedLockTest {
    
    @Autowired
    private LockService lockService;
    
    @Test
    @DisplayName("并发获取锁")
    void testConcurrentLock() throws InterruptedException {
        String lockKey = "test:lock";
        int threadCount = 10;
        AtomicInteger successCount = new AtomicInteger(0);
        
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    boolean locked = lockService.tryLock(lockKey, 3000, 5000);
                    if (locked) {
                        successCount.incrementAndGet();
                        Thread.sleep(100);
                        lockService.unlock(lockKey);
                    }
                } catch (Exception e) {
                    // ignore
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // 所有线程都应该成功获取锁（因为会排队）
        assertThat(successCount.get()).isEqualTo(threadCount);
    }
}
```

---

## 集成测试

### 完整微服务集成测试

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@DisplayName("微服务集成测试")
class MicroserviceIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);
    
    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.12-alpine");
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // MySQL
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        
        // Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        
        // RabbitMQ
        registry.add("nebula.messaging.rabbitmq.host", rabbitmq::getHost);
        registry.add("nebula.messaging.rabbitmq.port", rabbitmq::getAmqpPort);
    }
    
    @Autowired
    private OrderService orderService;
    
    @Test
    @DisplayName("完整订单流程测试")
    void testCompleteOrderFlow() {
        // 创建订单
        Order order = orderService.createOrder(request);
        assertThat(order).isNotNull();
        
        // 支付订单
        orderService.payOrder(order.getId());
        
        // 验证订单状态
        Order paidOrder = orderService.getById(order.getId());
        assertThat(paidOrder.getStatus()).isEqualTo(OrderStatus.PAID);
    }
}
```

---

## 契约测试

### 使用Spring Cloud Contract

**定义契约** `contracts/user-service.groovy`:

```groovy
import org.springframework.cloud.contract.spec.Contract

Contract.make {
    request {
        method 'POST'
        url '/rpc/user/getById'
        body([
            userId: '123'
        ])
        headers {
            contentType('application/json')
        }
    }
    response {
        status 200
        body([
            code: '0',
            data: [
                id: '123',
                username: '张三'
            ]
        ])
        headers {
            contentType('application/json')
        }
    }
}
```

**测试契约**:

```java
@SpringBootTest
@AutoConfigureStubRunner(
    ids = "com.example:user-service:+:stubs:8089",
    stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class ContractTest {
    
    @RpcClient(name = "user-service")
    private UserRpcService userRpcService;
    
    @Test
    void testContract() {
        Result<User> result = userRpcService.getById("123");
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getUsername()).isEqualTo("张三");
    }
}
```

---

## 票务系统测试示例

### 场景：跨服务订单创建测试

```java
@SpringBootTest
@Testcontainers
@DisplayName("票务系统-订单创建集成测试")
class TicketOrderIntegrationTest {
    
    @Autowired
    private OrderService orderService;
    
    private WireMockServer userServiceMock;
    private WireMockServer showtimeServiceMock;
    
    @BeforeEach
    void setUp() {
        // 启动用户服务Mock
        userServiceMock = new WireMockServer(8081);
        userServiceMock.start();
        WireMock.configureFor("localhost", 8081);
        
        // Mock用户服务响应
        stubFor(post(urlEqualTo("/rpc/user/getById"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"code\":\"0\",\"data\":{\"id\":\"1\",\"balance\":1000}}")));
        
        // 启动场次服务Mock
        showtimeServiceMock = new WireMockServer(8082);
        showtimeServiceMock.start();
        WireMock.configureFor("localhost", 8082);
        
        // Mock场次服务响应
        stubFor(post(urlEqualTo("/rpc/showtime/lockSeats"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"code\":\"0\",\"data\":true}")));
    }
    
    @AfterEach
    void tearDown() {
        userServiceMock.stop();
        showtimeServiceMock.stop();
    }
    
    @Test
    @DisplayName("创建订单-成功")
    void testCreateOrderSuccess() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId("1");
        request.setShowtimeId("1");
        request.setSeatIds(Arrays.asList("A01", "A02"));
        
        Order order = orderService.createOrder(request);
        
        assertThat(order).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }
    
    @Test
    @DisplayName("创建订单-余额不足")
    void testCreateOrderInsufficientBalance() {
        // Mock余额不足
        stubFor(post(urlEqualTo("/rpc/user/checkBalance"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"code\":\"0\",\"data\":false}")));
        
        assertThatThrownBy(() -> orderService.createOrder(request))
            .isInstanceOf(BusinessException.class)
            .hasMessage("余额不足");
    }
}
```

---

## 测试覆盖率

### 配置JaCoCo

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <configuration>
        <excludes>
            <exclude>**/dto/**</exclude>
            <exclude>**/entity/**</exclude>
            <exclude>**/config/**</exclude>
        </excludes>
    </configuration>
</plugin>
```

运行测试：

```bash
mvn clean test jacoco:report
open target/site/jacoco/index.html
```

---

## 最佳实践

1. **使用Testcontainers**：确保测试环境一致性
2. **Mock外部依赖**：使用WireMock模拟RPC调用
3. **契约测试**：确保服务间接口兼容性
4. **并发测试**：测试分布式锁等并发场景
5. **集成测试**：验证完整业务流程

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [EXAMPLE.md](./EXAMPLE.md) - 使用示例
- [CONFIG.md](./CONFIG.md) - 配置参考
- [ROADMAP.md](./ROADMAP.md) - 未来规划

---

> 如有问题或建议，欢迎提Issue。


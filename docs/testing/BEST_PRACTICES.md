# Nebula Framework 测试最佳实践

> **版本**: 2.0.1-SNAPSHOT  
> **最后更新**: 2025-01-13

## 📝 命名规范

### 测试类命名

```java
// 模式: [ClassUnderTest]Test
public class UserServiceTest { }
public class OrderRepositoryTest { }
```

### 测试方法命名

```java
// 推荐: testMethodName_Should[ExpectedBehavior]_When[StateUnderTest]
@Test
void testCreateUser_ShouldReturnSuccess_WhenValidInput() { }

@Test
void testDeleteOrder_ShouldThrowException_WhenOrderNotFound() { }
```

### Display Name

```java
@Test
@DisplayName("Should return success when valid input")
void testCreateUser() { }
```

## 🏗️ 测试结构

### Given-When-Then 模式

```java
@Test
void testPayment() {
    // Given - 准备测试环境和数据
    PaymentRequest request = PaymentRequest.builder()
        .amount(BigDecimal.valueOf(100))
        .currency("CNY")
        .build();
    
    // When - 执行被测试的操作
    PaymentResponse response = paymentService.process(request);
    
    // Then - 验证结果
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.getTransactionId()).isNotNull();
}
```

## 🎭 Mock 使用

### 推荐方式

```java
@ExtendWith(MockitoExtension.class)
class ServiceTest {
    
    @Mock
    private ExternalService externalService;
    
    @InjectMocks
    private ServiceImpl service;
    
    @Test
    void testMethod() {
        // 配置 Mock 行为
        when(externalService.call(any()))
            .thenReturn(mockResult);
        
        // 执行测试
        Result result = service.method();
        
        // 验证 Mock 调用
        verify(externalService, times(1)).call(any());
    }
}
```

### Mock 最佳实践

✅ **推荐**:
```java
// 1. 使用 @Mock 注解
@Mock
private UserRepository repository;

// 2. 具体化 Mock 行为
when(repository.findById(1L))
    .thenReturn(Optional.of(user));

// 3. 验证重要调用
verify(repository).save(user);
```

❌ **避免**:
```java
// 1. 不要 Mock 值对象
// ❌ when(user.getName()).thenReturn("test");

// 2. 不要过度 Mock
// ❌ Mock 每个依赖

// 3. 不要忘记验证
// ❌ 缺少 verify() 调用
```

## ✅ 断言

### 使用 AssertJ

```java
// 推荐: AssertJ 流式断言
assertThat(result)
    .isNotNull()
    .extracting(User::getName, User::getEmail)
    .containsExactly("John", "john@example.com");

// 集合断言
assertThat(users)
    .hasSize(3)
    .extracting(User::getName)
    .containsExactlyInAnyOrder("Alice", "Bob", "Charlie");

// 异常断言
assertThatThrownBy(() -> service.method(null))
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("cannot be null");
```

### 避免的断言方式

```java
// ❌ 避免: 使用 assertTrue/assertFalse
assertTrue(result.isSuccess());  // 不推荐

// ✅ 推荐: 使用语义化断言
assertThat(result.isSuccess()).isTrue();  // 推荐
```

## 🧪 测试数据

### Builder 模式

```java
// 创建测试数据工厂
public class TestDataFactory {
    
    public static User createUser() {
        return User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .createdAt(LocalDateTime.now())
            .build();
    }
    
    public static User createUserWithId(Long id) {
        return createUser().toBuilder()
            .id(id)
            .build();
    }
}

// 使用
@Test
void testMethod() {
    User user = TestDataFactory.createUser();
    // 测试代码
}
```

### 参数化测试

```java
@ParameterizedTest
@ValueSource(strings = {"", "  ", "\t", "\n"})
void testValidation_ShouldFail_WhenBlankInput(String input) {
    assertThatThrownBy(() -> service.validate(input))
        .isInstanceOf(ValidationException.class);
}

@ParameterizedTest
@CsvSource({
    "1, 2, 3",
    "10, 20, 30",
    "100, 200, 300"
})
void testAdd(int a, int b, int expected) {
    assertThat(calculator.add(a, b)).isEqualTo(expected);
}
```

## 🔧 Spring Boot 测试

### 单元测试

```java
// 不加载Spring容器
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository repository;
    
    @InjectMocks
    private UserServiceImpl service;
}
```

### 集成测试

```java
// 加载完整Spring容器
@SpringBootTest
class UserServiceIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @MockBean  // Mock Spring Bean
    private ExternalService externalService;
}
```

### Web 层测试

```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Test
    void testGetUser() throws Exception {
        when(userService.findById(1L))
            .thenReturn(user);
        
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("John"));
    }
}
```

## 🐳 TestContainers

### 数据库测试

```java
@Testcontainers
@SpringBootTest
class DatabaseIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

### Redis 测试

```java
@Container
static GenericContainer<?> redis = 
    new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);
```

## ⏱️ 异步测试

### 使用 Awaitility

```java
@Test
void testAsyncOperation() {
    // 触发异步操作
    service.asyncMethod();
    
    // 等待异步操作完成
    await()
        .atMost(5, SECONDS)
        .untilAsserted(() -> {
            assertThat(service.getResult()).isNotNull();
        });
}
```

## 📋 测试清理

### BeforeEach / AfterEach

```java
class ServiceTest {
    
    private Service service;
    
    @BeforeEach
    void setUp() {
        service = new ServiceImpl();
    }
    
    @AfterEach
    void tearDown() {
        // 清理资源
        service.cleanup();
    }
}
```

## 🚫 反模式

### 1. 测试过于复杂

❌ **避免**:
```java
@Test
void complexTest() {
    // 100 行测试代码
    // 多个when-then组合
    // 难以理解测试意图
}
```

✅ **推荐**:
```java
@Test
void simpleTest1() {
    // 单一关注点
}

@Test
void simpleTest2() {
    // 另一个关注点
}
```

### 2. 测试依赖顺序

❌ **避免**:
```java
@Test
@Order(1)
void createUser() { }

@Test
@Order(2)
void updateUser() {  // 依赖 test1
}
```

✅ **推荐**:
```java
@Test
void createUser() {
    // 独立测试
}

@Test
void updateUser() {
    // 准备自己的数据
    User user = createTestUser();
    // 独立测试
}
```

### 3. 忽略测试失败

❌ **避免**:
```java
@Test
@Disabled("暂时跳过")
void testMethod() { }
```

✅ **推荐**:
```java
// 修复测试，不要跳过
@Test
void testMethod() {
    // 正确的测试实现
}
```

## 📚 推荐资源

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [TestContainers](https://www.testcontainers.org/)

---

**持续改进测试质量** - Nebula Framework


# [模块名称] 单元测试文档

> **模块**: [模块artifactId]  
> **版本**: 2.0.1-SNAPSHOT  
> **最后更新**: YYYY-MM-DD

## 📋 测试概述

### 测试目标

本文档描述 `[模块名称]` 模块的单元测试策略、测试用例设计和执行指南。

### 测试范围

- ✅ 核心功能测试
- ✅ 边界条件测试
- ✅ 异常情况测试
- ✅ 性能基准测试(如适用)
- ✅ 集成测试(如适用)

### 测试覆盖率目标

- **行覆盖率**: ≥ 80%
- **分支覆盖率**: ≥ 70%
- **核心业务逻辑**: ≥ 90%

## 🏗️ 测试环境准备

### 必需依赖

```xml
<dependencies>
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Spring Boot Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- 其他测试依赖 -->
</dependencies>
```

### 测试配置

```yaml
# src/test/resources/application-test.yml
spring:
  profiles:
    active: test

nebula:
  [模块配置前缀]:
    enabled: true
    # 测试环境配置
```

### 外部服务 Mock

如果模块依赖外部服务,使用以下方式 Mock:

**选项1: TestContainers** (推荐用于数据库、消息队列等)
```java
@Testcontainers
class IntegrationTest {
    @Container
    static GenericContainer<?> container = new GenericContainer<>("service:tag")
        .withExposedPorts(PORT);
}
```

**选项2: WireMock** (推荐用于 HTTP 服务)
```java
@SpringBootTest
@AutoConfigureWireMock(port = 0)
class HttpServiceTest {
    // 测试代码
}
```

**选项3: Mockito** (推荐用于接口Mock)
```java
@Mock
private ExternalService externalService;
```

## 🧪 测试用例设计

### 1. [功能模块1] 测试

#### 1.1 正常场景测试

**测试类**: `[ClassName]Test.java`

**测试方法**: `testMethodName_Should[ExpectedBehavior]_When[StateUnderTest]()`

**测试用例**:

| ID | 用例名称 | 输入条件 | 预期输出 | 优先级 |
|----|----------|----------|----------|--------|
| TC001 | [用例描述] | [输入] | [输出] | P0 |
| TC002 | [用例描述] | [输入] | [输出] | P0 |
| TC003 | [用例描述] | [输入] | [输出] | P1 |

**示例代码**:
```java
@Test
@DisplayName("Should [ExpectedBehavior] When [StateUnderTest]")
void testMethodName_ShouldReturnSuccess_WhenValidInput() {
    // Given
    String input = "valid input";
    
    // When
    Result result = service.method(input);
    
    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
}
```

#### 1.2 边界条件测试

**测试用例**:

| ID | 用例名称 | 边界条件 | 预期行为 | 优先级 |
|----|----------|----------|----------|--------|
| BC001 | 空值输入 | null | 抛出异常或返回错误 | P0 |
| BC002 | 空字符串 | "" | 抛出异常或返回错误 | P0 |
| BC003 | 超长输入 | 超过最大长度 | 抛出异常或返回错误 | P1 |
| BC004 | 最小值 | 最小有效值 | 正常处理 | P1 |
| BC005 | 最大值 | 最大有效值 | 正常处理 | P1 |

**示例代码**:
```java
@Test
@DisplayName("Should throw exception when input is null")
void testMethod_ShouldThrowException_WhenInputIsNull() {
    // Given
    String input = null;
    
    // When & Then
    assertThatThrownBy(() -> service.method(input))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("input cannot be null");
}
```

#### 1.3 异常场景测试

**测试用例**:

| ID | 用例名称 | 异常条件 | 预期行为 | 优先级 |
|----|----------|----------|----------|--------|
| EX001 | [异常场景] | [条件] | [行为] | P0 |
| EX002 | [异常场景] | [条件] | [行为] | P1 |

**示例代码**:
```java
@Test
@DisplayName("Should handle exception gracefully when external service fails")
void testMethod_ShouldHandleException_WhenExternalServiceFails() {
    // Given
    when(externalService.call()).thenThrow(new RuntimeException("Service unavailable"));
    
    // When
    Result result = service.method();
    
    // Then
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getErrorMessage()).contains("Service unavailable");
}
```

### 2. [功能模块2] 测试

[重复上述结构]

## 🎯 关键测试场景

### 场景1: [场景名称]

**背景**: [场景描述]

**测试目标**: [测试什么]

**前置条件**:
1. [条件1]
2. [条件2]

**测试步骤**:
1. [步骤1]
2. [步骤2]
3. [步骤3]

**验收标准**:
- ✅ [标准1]
- ✅ [标准2]
- ✅ [标准3]

**完整示例**:
```java
@Test
@DisplayName("场景: [场景名称]")
void testScenario_[ScenarioName]() {
    // 1. 准备测试数据
    // Given
    
    // 2. 执行测试操作
    // When
    
    // 3. 验证结果
    // Then
}
```

### 场景2: [场景名称]

[重复上述结构]

## 🔧 Mock 依赖配置

### Mock 外部服务

```java
@ExtendWith(MockitoExtension.class)
class ServiceTest {
    
    @Mock
    private ExternalService externalService;
    
    @InjectMocks
    private YourService yourService;
    
    @BeforeEach
    void setUp() {
        // 配置 Mock 行为
        when(externalService.method(any()))
            .thenReturn(mockResponse);
    }
}
```

### Mock Spring Bean

```java
@SpringBootTest
class IntegrationTest {
    
    @MockBean
    private ExternalService externalService;
    
    @Autowired
    private YourService yourService;
    
    @Test
    void testWithMockedBean() {
        // 配置 Mock 行为
        when(externalService.method(any()))
            .thenReturn(mockResponse);
            
        // 执行测试
    }
}
```

## 🚀 测试执行

### 执行所有测试

```bash
# Maven
mvn test

# 仅执行本模块测试
mvn test -pl [模块路径]

# 执行特定测试类
mvn test -Dtest=[TestClassName]

# 执行特定测试方法
mvn test -Dtest=[TestClassName]#[testMethod]
```

### 测试覆盖率报告

```bash
# 生成覆盖率报告
mvn clean test jacoco:report

# 查看报告
open target/site/jacoco/index.html
```

### CI/CD 集成

```yaml
# GitHub Actions 示例
name: Test
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 21
        uses: actions/setup-java@v2
        with:
          java-version: '21'
      - name: Run tests
        run: mvn test -pl [模块路径]
      - name: Upload coverage
        uses: codecov/codecov-action@v2
```

## 📊 测试验收标准

### 必须满足的标准

- ✅ 所有P0级别测试用例通过率 100%
- ✅ 所有P1级别测试用例通过率 ≥ 95%
- ✅ 代码覆盖率达到目标(行覆盖≥80%, 分支覆盖≥70%)
- ✅ 无已知的P0/P1级别缺陷
- ✅ 性能测试通过(如适用)

### 测试报告

测试完成后应生成以下报告:
1. **单元测试报告**: target/surefire-reports/
2. **覆盖率报告**: target/site/jacoco/
3. **测试摘要**: 包含通过率、覆盖率、执行时间

## 🐛 已知问题与限制

### 当前限制

1. **限制1**: [描述限制和影响范围]
2. **限制2**: [描述限制和影响范围]

### 待完善的测试

- [ ] [待添加的测试场景1]
- [ ] [待添加的测试场景2]
- [ ] [待添加的性能测试]

## 📚 参考资源

- [JUnit 5 用户指南](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito 文档](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [AssertJ 文档](https://assertj.github.io/doc/)

## 🤝 贡献测试用例

欢迎贡献更多测试用例！请遵循:
1. 使用 Given-When-Then 模式
2. 测试方法命名清晰
3. 添加 `@DisplayName` 注解
4. 补充必要的注释
5. 确保测试独立且可重复

---

**测试是质量的保障** - 让我们一起构建可靠的 Nebula 框架！


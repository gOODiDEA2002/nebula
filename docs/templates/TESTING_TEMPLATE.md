# [模块名称] - 测试指南

> 本文档提供 [模块名称] 的完整测试策略、测试用例和测试实践。

## 测试概览

### 测试层次

```
┌─────────────────────────┐
│     端到端测试 (E2E)     │  验证完整业务流程
├─────────────────────────┤
│     集成测试 (IT)        │  验证模块间集成
├─────────────────────────┤
│     单元测试 (UT)        │  验证单个组件
└─────────────────────────┘
```

### 测试策略

- **单元测试**：覆盖率目标 ≥ 80%
- **集成测试**：覆盖核心业务流程
- **性能测试**：验证性能指标
- **安全测试**：验证安全机制

### 测试工具

- **JUnit 5**：测试框架
- **Mockito**：Mock框架
- **AssertJ**：断言库
- **Spring Boot Test**：Spring集成测试
- **Testcontainers**：容器测试
- **JMH**：性能基准测试

## 测试环境准备

### 依赖配置

在 `pom.xml` 中添加测试依赖：

```xml
<dependencies>
    <!-- Spring Boot Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Nebula 模块 -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>[artifact-id]</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Testcontainers（可选）-->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 测试配置

**src/test/resources/application-test.yml**：

```yaml
spring:
  profiles:
    active: test

nebula:
  [module]:
    enabled: true
    property1: test-value
    property2: 100
    
# 测试数据库配置
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    
# 日志配置
logging:
  level:
    io.nebula.[module]: DEBUG
```

### 测试基类

创建测试基类简化测试配置：

```java
package com.example.test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * 测试基类
 */
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
public abstract class BaseTest {
    
    // 通用测试设置
}
```

---

## 单元测试

### 测试用例1：基础功能测试

**测试目标**：验证模块的基础功能是否正常工作。

```java
package io.nebula.[module];

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;

/**
 * 基础功能测试
 */
@SpringBootTest
@DisplayName("[模块名称] 基础功能测试")
class BasicFunctionTest {
    
    @Autowired
    private ModuleComponent component;
    
    @Test
    @DisplayName("测试基础功能")
    void testBasicFunction() {
        // 准备测试数据
        TestData data = new TestData("test");
        
        // 执行测试
        Result result = component.execute(data);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("expected");
    }
    
    @Test
    @DisplayName("测试参数验证")
    void testParameterValidation() {
        // 测试空参数
        assertThatThrownBy(() -> component.execute(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("参数不能为空");
    }
    
    @Test
    @DisplayName("测试异常处理")
    void testExceptionHandling() {
        // 准备异常场景数据
        TestData invalidData = new TestData("");
        
        // 验证异常处理
        assertThatThrownBy(() -> component.execute(invalidData))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("数据无效");
    }
}
```

### 测试用例2：配置加载测试

**测试目标**：验证配置是否正确加载。

```java
package io.nebula.[module].config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;

/**
 * 配置加载测试
 */
@SpringBootTest
@DisplayName("配置加载测试")
class ConfigurationTest {
    
    @Autowired
    private ModuleProperties properties;
    
    @Test
    @DisplayName("测试配置加载")
    void testConfigurationLoading() {
        assertThat(properties).isNotNull();
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getProperty1()).isEqualTo("test-value");
        assertThat(properties.getProperty2()).isEqualTo(100);
    }
    
    @Test
    @DisplayName("测试默认配置")
    void testDefaultConfiguration() {
        assertThat(properties.getProperty2()).isEqualTo(100);
    }
}
```

### 测试用例3：组件依赖测试

**测试目标**：验证组件依赖注入是否正常。

```java
package io.nebula.[module].component;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.*;

/**
 * 组件依赖测试
 */
@SpringBootTest
@DisplayName("组件依赖测试")
class ComponentDependencyTest {
    
    @Autowired
    private ApplicationContext context;
    
    @Test
    @DisplayName("测试Bean是否存在")
    void testBeanExists() {
        assertThat(context.containsBean("moduleComponent")).isTrue();
        assertThat(context.getBean(ModuleComponent.class)).isNotNull();
    }
    
    @Test
    @DisplayName("测试Bean单例")
    void testBeanSingleton() {
        ModuleComponent bean1 = context.getBean(ModuleComponent.class);
        ModuleComponent bean2 = context.getBean(ModuleComponent.class);
        assertThat(bean1).isSameAs(bean2);
    }
}
```

### 测试用例4：Mock测试

**测试目标**：使用Mock隔离依赖进行单元测试。

```java
package io.nebula.[module].service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Mock测试示例
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Mock测试")
class MockTest {
    
    @Mock
    private DependencyComponent dependency;
    
    @InjectMocks
    private ServiceComponent service;
    
    @Test
    @DisplayName("测试使用Mock依赖")
    void testWithMock() {
        // 设置Mock行为
        when(dependency.getData()).thenReturn("mocked-data");
        
        // 执行测试
        String result = service.processData();
        
        // 验证结果
        assertThat(result).isEqualTo("processed-mocked-data");
        
        // 验证Mock调用
        verify(dependency, times(1)).getData();
    }
}
```

---

## 集成测试

### 测试用例1：模块集成测试

**测试目标**：验证模块与其他模块的集成。

```java
package io.nebula.[module].integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

/**
 * 模块集成测试
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("模块集成测试")
class ModuleIntegrationTest {
    
    @Autowired
    private ModuleComponent moduleComponent;
    
    @Autowired
    private OtherModuleComponent otherComponent;
    
    @Test
    @DisplayName("测试模块间协作")
    void testModuleCollaboration() {
        // 准备数据
        TestData data = new TestData("test");
        
        // 使用第一个模块处理
        Result result1 = moduleComponent.process(data);
        assertThat(result1.isSuccess()).isTrue();
        
        // 使用第二个模块处理
        Result result2 = otherComponent.process(result1.getData());
        assertThat(result2.isSuccess()).isTrue();
    }
}
```

### 测试用例2：数据库集成测试

**测试目标**：验证与数据库的集成。

```java
package io.nebula.[module].integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

/**
 * 数据库集成测试
 */
@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
@DisplayName("数据库集成测试")
class DatabaseIntegrationTest {
    
    @Autowired
    private ModuleRepository repository;
    
    @Test
    @DisplayName("测试数据库CRUD操作")
    void testCrudOperations() {
        // 创建
        TestEntity entity = new TestEntity("test");
        repository.save(entity);
        assertThat(entity.getId()).isNotNull();
        
        // 查询
        TestEntity found = repository.findById(entity.getId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("test");
        
        // 更新
        found.setName("updated");
        repository.save(found);
        
        TestEntity updated = repository.findById(entity.getId()).orElse(null);
        assertThat(updated.getName()).isEqualTo("updated");
        
        // 删除
        repository.deleteById(entity.getId());
        assertThat(repository.findById(entity.getId())).isEmpty();
    }
}
```

### 测试用例3：缓存集成测试

**测试目标**：验证缓存功能是否正常。

```java
package io.nebula.[module].integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.*;

/**
 * 缓存集成测试
 */
@SpringBootTest
@DisplayName("缓存集成测试")
class CacheIntegrationTest {
    
    @Autowired
    private CacheableService service;
    
    @Autowired
    private CacheManager cacheManager;
    
    @Test
    @DisplayName("测试缓存生效")
    void testCacheWorks() {
        // 第一次调用，应该查询数据库
        String result1 = service.getCachedData(1L);
        assertThat(result1).isNotNull();
        
        // 第二次调用，应该从缓存获取
        String result2 = service.getCachedData(1L);
        assertThat(result2).isEqualTo(result1);
        
        // 验证缓存存在
        assertThat(cacheManager.getCache("test-cache")).isNotNull();
        assertThat(cacheManager.getCache("test-cache").get(1L)).isNotNull();
    }
}
```

### 测试用例4：消息队列集成测试

**测试目标**：验证消息队列功能。

```java
package io.nebula.[module].integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * 消息队列集成测试
 */
@SpringBootTest
@DisplayName("消息队列集成测试")
class MessageQueueIntegrationTest {
    
    @Autowired
    private MessageProducer producer;
    
    @Autowired
    private MessageConsumerListener consumer;
    
    @Test
    @DisplayName("测试消息发送和接收")
    void testMessageSendAndReceive() {
        // 发送消息
        TestMessage message = new TestMessage("test");
        producer.send("test.topic", message);
        
        // 等待消息被消费（最多5秒）
        await()
            .atMost(5, TimeUnit.SECONDS)
            .until(() -> consumer.getReceivedMessages().size() > 0);
        
        // 验证消息接收
        assertThat(consumer.getReceivedMessages()).hasSize(1);
        assertThat(consumer.getReceivedMessages().get(0).getContent())
            .isEqualTo("test");
    }
}
```

---

## 性能测试

### 测试用例1：吞吐量测试

**测试目标**：测试模块的处理吞吐量。

```java
package io.nebula.[module].performance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * 吞吐量测试
 */
@SpringBootTest
@DisplayName("吞吐量测试")
class ThroughputTest {
    
    @Autowired
    private ModuleComponent component;
    
    @Test
    @DisplayName("测试并发吞吐量")
    void testThroughput() throws InterruptedException {
        int threadCount = 100;
        int requestsPerThread = 100;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        // 并发执行
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        try {
                            component.execute(new TestData("test"));
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待完成
        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 计算吞吐量
        int totalRequests = threadCount * requestsPerThread;
        double throughput = (double) totalRequests / duration * 1000;
        
        System.out.printf("总请求数: %d%n", totalRequests);
        System.out.printf("成功数: %d%n", successCount.get());
        System.out.printf("失败数: %d%n", failureCount.get());
        System.out.printf("总耗时: %d ms%n", duration);
        System.out.printf("吞吐量: %.2f ops/s%n", throughput);
        
        // 验证结果
        assertThat(successCount.get()).isGreaterThan(totalRequests * 0.95); // 95%成功率
    }
}
```

### 测试用例2：延迟测试

**测试目标**：测试模块的响应延迟。

```java
package io.nebula.[module].performance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 延迟测试
 */
@SpringBootTest
@DisplayName("延迟测试")
class LatencyTest {
    
    @Autowired
    private ModuleComponent component;
    
    @Test
    @DisplayName("测试响应延迟")
    void testLatency() {
        int iterations = 1000;
        List<Long> latencies = new ArrayList<>();
        
        // 预热
        for (int i = 0; i < 100; i++) {
            component.execute(new TestData("warmup"));
        }
        
        // 测试
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            component.execute(new TestData("test"));
            long endTime = System.nanoTime();
            
            latencies.add((endTime - startTime) / 1_000_000); // 转换为毫秒
        }
        
        // 统计
        Collections.sort(latencies);
        long avgLatency = latencies.stream().mapToLong(Long::longValue).sum() / iterations;
        long p50Latency = latencies.get(iterations / 2);
        long p95Latency = latencies.get((int) (iterations * 0.95));
        long p99Latency = latencies.get((int) (iterations * 0.99));
        long maxLatency = latencies.get(iterations - 1);
        
        System.out.printf("平均延迟: %d ms%n", avgLatency);
        System.out.printf("P50延迟: %d ms%n", p50Latency);
        System.out.printf("P95延迟: %d ms%n", p95Latency);
        System.out.printf("P99延迟: %d ms%n", p99Latency);
        System.out.printf("最大延迟: %d ms%n", maxLatency);
        
        // 验证性能指标
        assertThat(avgLatency).isLessThan(100); // 平均延迟 < 100ms
        assertThat(p99Latency).isLessThan(500); // P99延迟 < 500ms
    }
}
```

### 测试用例3：JMH基准测试

**测试目标**：使用JMH进行精确的性能基准测试。

```java
package io.nebula.[module].benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * JMH基准测试
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class ModuleBenchmark {
    
    private ModuleComponent component;
    private TestData testData;
    
    @Setup
    public void setup() {
        component = new ModuleComponent();
        testData = new TestData("test");
    }
    
    @Benchmark
    public void benchmarkExecute() {
        component.execute(testData);
    }
    
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(ModuleBenchmark.class.getSimpleName())
                .build();
        
        new Runner(opt).run();
    }
}
```

---

## 测试覆盖率

### 运行覆盖率报告

使用 JaCoCo 生成测试覆盖率报告：

```bash
mvn clean test jacoco:report
```

### 查看覆盖率报告

报告位置：`target/site/jacoco/index.html`

### 覆盖率目标

| 类型 | 目标覆盖率 |
|------|-----------|
| 行覆盖率 | ≥ 80% |
| 分支覆盖率 | ≥ 70% |
| 方法覆盖率 | ≥ 80% |

### 当前覆盖率

**模块整体覆盖率**：XX%

**各组件覆盖率**：

| 组件 | 行覆盖率 | 分支覆盖率 |
|------|---------|-----------|
| Component1 | XX% | XX% |
| Component2 | XX% | XX% |
| Service1 | XX% | XX% |

---

## 测试最佳实践

### 实践1：使用有意义的测试名称

**推荐**：

```java
@Test
@DisplayName("当用户ID为空时应该抛出IllegalArgumentException异常")
void shouldThrowIllegalArgumentExceptionWhenUserIdIsNull() {
    // 测试代码
}
```

**不推荐**：

```java
@Test
void test1() {
    // 测试代码
}
```

### 实践2：遵循AAA模式

**AAA模式**：Arrange（准备）、Act（执行）、Assert（断言）

```java
@Test
void testExample() {
    // Arrange - 准备测试数据
    TestData data = new TestData("test");
    
    // Act - 执行测试方法
    Result result = component.execute(data);
    
    // Assert - 验证结果
    assertThat(result.isSuccess()).isTrue();
}
```

### 实践3：每个测试只验证一个行为

**推荐**：

```java
@Test
void shouldReturnSuccessWhenDataIsValid() {
    // 只测试成功场景
}

@Test
void shouldThrowExceptionWhenDataIsInvalid() {
    // 只测试异常场景
}
```

### 实践4：使用AssertJ提高可读性

**推荐**：

```java
assertThat(result)
    .isNotNull()
    .extracting("status", "message")
    .containsExactly("SUCCESS", "操作成功");
```

**不推荐**：

```java
assertTrue(result != null);
assertEquals("SUCCESS", result.getStatus());
assertEquals("操作成功", result.getMessage());
```

### 实践5：使用Testcontainers测试真实依赖

```java
@Testcontainers
@SpringBootTest
class TestcontainersTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
    
    @Test
    void testWithRealDatabase() {
        // 测试代码
    }
}
```

---

## 常见测试问题

### 问题1：测试不稳定（Flaky Test）

**原因**：

- 依赖外部资源
- 多线程竞争
- 时间相关逻辑

**解决方案**：

```java
// 使用Awaitility处理异步
await()
    .atMost(5, TimeUnit.SECONDS)
    .until(() -> condition());
    
// 使用Mock隔离外部依赖
@Mock
private ExternalService externalService;
```

### 问题2：测试运行缓慢

**原因**：

- 启动整个Spring上下文
- 访问真实数据库

**解决方案**：

```java
// 使用@WebMvcTest只加载Web层
@WebMvcTest(Controller.class)
class ControllerTest {
    // 测试代码
}

// 使用@DataJpaTest只加载数据层
@DataJpaTest
class RepositoryTest {
    // 测试代码
}
```

### 问题3：Mock不生效

**原因**：

- 未正确配置Mock
- Mock对象未注入

**解决方案**：

```java
@ExtendWith(MockitoExtension.class)
class ServiceTest {
    
    @Mock
    private Dependency dependency;
    
    @InjectMocks
    private Service service;
    
    @Test
    void test() {
        when(dependency.method()).thenReturn("mocked");
        // 测试代码
    }
}
```

---

## 持续集成

### Maven配置

**pom.xml**：

```xml
<build>
    <plugins>
        <!-- Surefire Plugin -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <includes>
                    <include>**/*Test.java</include>
                    <include>**/*Tests.java</include>
                </includes>
            </configuration>
        </plugin>
        
        <!-- JaCoCo Plugin -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                        <goal>report</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### CI流程

**GitHub Actions示例**：

```yaml
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
      - name: Test
        run: mvn test
      - name: Coverage Report
        run: mvn jacoco:report
      - name: Upload Coverage
        uses: codecov/codecov-action@v2
```

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [EXAMPLE.md](./EXAMPLE.md) - 使用示例
- [CONFIG.md](./CONFIG.md) - 配置参考
- [ROADMAP.md](./ROADMAP.md) - 未来规划

---

> 测试是代码质量的保障，请保持良好的测试覆盖率。


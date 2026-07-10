# Nebula Gateway Core - 测试指南

> 本文档提供 Nebula Gateway Core 的测试策略和测试用例。

## 测试概览

### 测试层次

```
+-------------------------+
|    集成测试 (IT)         |  验证过滤器链完整工作
+-------------------------+
|    单元测试 (UT)         |  验证单个过滤器功能
+-------------------------+
```

### 测试策略

- **单元测试**：覆盖率目标 >= 80%
- **集成测试**：覆盖核心业务流程

### 测试工具

- **JUnit 5**：测试框架
- **Mockito**：Mock框架
- **AssertJ**：断言库
- **Spring Boot Test**：Spring集成测试
- **WebTestClient**：响应式Web测试客户端

## 测试环境准备

### 依赖配置

在 `pom.xml` 中添加测试依赖：

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 测试配置

**src/test/resources/application-test.yml**：

```yaml
nebula:
  gateway:
    enabled: true
    jwt:
      enabled: true
      secret: test-jwt-secret-key-at-least-32-characters
      whitelist:
        - /api/public/**
    logging:
      enabled: true
      slow-request-threshold: 1000
```

---

## 单元测试

### 测试用例1：JWT过滤器测试

```java
package io.nebula.gateway.filter;

import io.nebula.gateway.config.GatewayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JWT认证过滤器测试")
class JwtAuthGatewayFilterFactoryTest {
    
    private JwtAuthGatewayFilterFactory filterFactory;
    private GatewayProperties properties;
    
    @BeforeEach
    void setUp() {
        properties = new GatewayProperties();
        properties.getJwt().setEnabled(true);
        properties.getJwt().setSecret("test-jwt-secret-key-at-least-32-characters");
        properties.getJwt().getWhitelist().add("/api/public/**");
        
        filterFactory = new JwtAuthGatewayFilterFactory(properties);
    }
    
    @Test
    @DisplayName("白名单路径应该放行")
    void shouldAllowWhitelistedPath() {
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/public/test")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        
        // 白名单路径不需要Token也能通过
        // 测试过滤器逻辑...
    }
    
    @Test
    @DisplayName("缺少Token应该返回401")
    void shouldReturn401WhenTokenMissing() {
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/v1/users/info")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        
        // 验证返回401
        // 测试过滤器逻辑...
    }
    
    @Test
    @DisplayName("无效Token应该返回401")
    void shouldReturn401WhenTokenInvalid() {
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/v1/users/info")
            .header("Authorization", "Bearer invalid-token")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        
        // 验证返回401
        // 测试过滤器逻辑...
    }
}
```

### 测试用例2：日志过滤器测试

```java
package io.nebula.gateway.filter;

import io.nebula.gateway.config.GatewayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.*;

@DisplayName("日志过滤器测试")
class LoggingGlobalFilterTest {
    
    private LoggingGlobalFilter filter;
    private GatewayProperties properties;
    
    @BeforeEach
    void setUp() {
        properties = new GatewayProperties();
        properties.getLogging().setEnabled(true);
        properties.getLogging().setSlowRequestThreshold(3000);
        
        filter = new LoggingGlobalFilter(properties);
    }
    
    @Test
    @DisplayName("应该添加RequestId请求头")
    void shouldAddRequestIdHeader() {
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/test")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        
        // 验证请求头中包含X-Request-Id
        // 测试过滤器逻辑...
    }
    
    @Test
    @DisplayName("过滤器顺序应该是最高优先级")
    void shouldHaveHighestPrecedence() {
        assertThat(filter.getOrder()).isEqualTo(Integer.MIN_VALUE);
    }
}
```

### 测试用例3：gRPC路由器测试

```java
package io.nebula.gateway.grpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("gRPC路由器测试")
class AbstractGrpcServiceRouterTest {
    
    private TestGrpcServiceRouter router;
    
    @BeforeEach
    void setUp() {
        router = new TestGrpcServiceRouter(new ObjectMapper());
        router.init();
    }
    
    @Test
    @DisplayName("精确路径匹配")
    void shouldMatchExactPath() {
        AbstractGrpcServiceRouter.RouteInfo route = router.route("/api/v1/users/login", "POST");
        
        assertThat(route).isNotNull();
        assertThat(route.getServiceName()).isEqualTo("user");
        assertThat(route.getMethodName()).isEqualTo("login");
    }
    
    @Test
    @DisplayName("路径变量匹配")
    void shouldMatchPathWithVariable() {
        AbstractGrpcServiceRouter.RouteInfo route = router.route("/api/v1/orders/123", "GET");
        
        assertThat(route).isNotNull();
        assertThat(route.getServiceName()).isEqualTo("order");
        assertThat(route.getMethodName()).isEqualTo("getOrderDetail");
    }
    
    @Test
    @DisplayName("未匹配路径返回null")
    void shouldReturnNullForUnmatchedPath() {
        AbstractGrpcServiceRouter.RouteInfo route = router.route("/api/v1/unknown", "GET");
        
        assertThat(route).isNull();
    }
    
    /**
     * 测试用路由器
     */
    static class TestGrpcServiceRouter extends AbstractGrpcServiceRouter {
        
        public TestGrpcServiceRouter(ObjectMapper objectMapper) {
            super(objectMapper);
        }
        
        @Override
        protected void registerRoutes() {
            registerRoute("POST", "/api/v1/users/login", "user", "login",
                (body, exchange) -> "login result");
            registerRoute("GET", "/api/v1/orders/{id}", "order", "getOrderDetail",
                (body, exchange) -> "order detail");
        }
    }
}
```

---

## 集成测试

### 测试用例1：完整请求流程测试

```java
package io.nebula.gateway.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("网关集成测试")
class GatewayIntegrationTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Test
    @DisplayName("白名单路径应该不需要认证")
    void whitelistPathShouldNotRequireAuth() {
        webTestClient.get()
            .uri("/api/public/health")
            .exchange()
            .expectStatus().isOk();
    }
    
    @Test
    @DisplayName("受保护路径缺少Token应该返回401")
    void protectedPathWithoutTokenShouldReturn401() {
        webTestClient.get()
            .uri("/api/v1/users/info")
            .exchange()
            .expectStatus().isUnauthorized()
            .expectBody()
            .jsonPath("$.code").isEqualTo("UNAUTHORIZED");
    }
}
```

---

## 测试覆盖率

### 运行覆盖率报告

```bash
mvn clean test jacoco:report
```

### 覆盖率目标

| 类型 | 目标覆盖率 |
|------|-----------|
| 行覆盖率 | >= 80% |
| 分支覆盖率 | >= 70% |
| 方法覆盖率 | >= 80% |

---

## 测试最佳实践

### 实践1：使用有意义的测试名称

```java
@Test
@DisplayName("当JWT Token过期时应该返回401未授权错误")
void shouldReturn401WhenJwtTokenExpired() {
    // 测试代码
}
```

### 实践2：遵循AAA模式

```java
@Test
void testExample() {
    // Arrange - 准备测试数据
    MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
    
    // Act - 执行测试方法
    boolean result = filter.isWhitelisted(request.getPath().value());
    
    // Assert - 验证结果
    assertThat(result).isTrue();
}
```

### 实践3：使用WebTestClient测试响应式端点

```java
@Test
void testReactiveEndpoint() {
    webTestClient.get()
        .uri("/api/test")
        .exchange()
        .expectStatus().isOk()
        .expectBody(String.class)
        .isEqualTo("expected");
}
```

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [EXAMPLE.md](./EXAMPLE.md) - 使用示例
- [CONFIG.md](./CONFIG.md) - 配置参考
- [ROADMAP.md](./ROADMAP.md) - 未来规划

---

> 测试是代码质量的保障，请保持良好的测试覆盖率。


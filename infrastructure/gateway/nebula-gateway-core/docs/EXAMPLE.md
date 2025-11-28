# Nebula Gateway Core - 使用示例

> 本文档提供 Nebula Gateway Core 的完整使用示例，所有代码均可运行。

## 示例概览

本文档包含以下示例：

- [示例1：基础网关搭建](#示例1基础网关搭建) - 最简单的网关配置
- [示例2：JWT认证配置](#示例2jwt认证配置) - JWT认证过滤器使用
- [示例3：gRPC桥接配置](#示例3grpc桥接配置) - HTTP到gRPC的协议转换
- [示例4：完整网关项目](#示例4完整网关项目) - 完整的生产级网关

## 前提条件

### 环境要求

- **Java**：21+
- **Spring Boot**：3.5+
- **Spring Cloud**：2025.0+
- **Maven**：3.8+

### 依赖配置

在 `pom.xml` 中添加：

```xml
<dependencies>
    <!-- Nebula Gateway Starter（推荐）-->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-gateway</artifactId>
        <version>2.0.1-SNAPSHOT</version>
    </dependency>
    
    <!-- 或者单独引入核心模块 -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-gateway-core</artifactId>
        <version>2.0.1-SNAPSHOT</version>
    </dependency>
</dependencies>
```

---

## 示例1：基础网关搭建

### 场景说明

搭建一个基础的API网关，包含路由转发、请求日志功能。

### 实现步骤

#### 步骤1：创建Spring Boot应用

```java
package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

#### 步骤2：配置路由

**application.yml**：

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/v1/users/**
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/v1/orders/**

nebula:
  gateway:
    enabled: true
    logging:
      enabled: true
      slow-request-threshold: 3000
```

#### 步骤3：运行和测试

启动应用后，日志输出示例：

```
[abc123ef] >>> GET /api/v1/users/info from 192.168.1.100
[abc123ef] <<< GET /api/v1/users/info - 200 (45ms)
```

---

## 示例2：JWT认证配置

### 场景说明

配置JWT认证，保护需要登录才能访问的接口。

### 实现步骤

#### 步骤1：配置JWT

**application.yml**：

```yaml
nebula:
  gateway:
    enabled: true
    jwt:
      enabled: true
      secret: ${JWT_SECRET:your-jwt-secret-key-at-least-32-characters}
      whitelist:
        - /api/v1/users/login
        - /api/v1/users/register
        - /api/v1/public/**
      claim-headers:
        - phone:X-User-Phone
        - role:X-User-Role
```

#### 步骤2：配置路由使用JWT过滤器

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/v1/users/**
          filters:
            - JwtAuth  # 启用JWT认证
```

#### 步骤3：测试JWT认证

**未带Token访问受保护接口**：

```bash
curl http://localhost:8080/api/v1/users/info
```

响应：

```json
{
  "success": false,
  "code": "UNAUTHORIZED",
  "message": "缺少认证Token"
}
```

**带Token访问**：

```bash
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." http://localhost:8080/api/v1/users/info
```

响应（成功）：下游服务返回的用户信息

---

## 示例3：gRPC桥接配置

### 场景说明

将HTTP请求转换为gRPC调用，实现协议桥接。

### 实现步骤

#### 步骤1：创建gRPC路由器

继承 `AbstractGrpcServiceRouter` 实现路由注册：

```java
package com.example.gateway.grpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.gateway.grpc.AbstractGrpcServiceRouter;
import com.example.user.api.UserRpcClient;
import com.example.user.api.dto.*;
import com.example.order.api.OrderRpcClient;
import com.example.order.api.dto.*;
import org.springframework.stereotype.Component;

@Component
public class MyGrpcServiceRouter extends AbstractGrpcServiceRouter {
    
    private final UserRpcClient userRpcClient;
    private final OrderRpcClient orderRpcClient;
    
    public MyGrpcServiceRouter(ObjectMapper objectMapper,
                               UserRpcClient userRpcClient,
                               OrderRpcClient orderRpcClient) {
        super(objectMapper);
        this.userRpcClient = userRpcClient;
        this.orderRpcClient = orderRpcClient;
    }
    
    @Override
    protected void registerRoutes() {
        // 用户服务路由
        registerRoute("POST", "/api/v1/users/login", "user", "login",
            (body, exchange) -> userRpcClient.login(parseBody(body, UserLoginDto.Request.class)));
        
        registerRoute("GET", "/api/v1/users/info", "user", "getUserInfo",
            (body, exchange) -> {
                Long userId = getUserIdFromHeader(exchange);
                UserInfoDto.Request request = new UserInfoDto.Request();
                request.setUserId(userId);
                return userRpcClient.getUserInfo(request);
            });
        
        // 订单服务路由
        registerRoute("POST", "/api/v1/orders", "order", "createOrder",
            (body, exchange) -> orderRpcClient.createOrder(parseBody(body, OrderCreateDto.Request.class)));
        
        registerRoute("GET", "/api/v1/orders/{id}", "order", "getOrderDetail",
            (body, exchange) -> {
                Long orderId = extractPathVariableLong(
                    exchange.getRequest().getPath().value(), 
                    "/api/v1/orders/(\\d+)"
                );
                return orderRpcClient.getOrderDetail(orderId);
            });
    }
}
```

#### 步骤2：配置gRPC客户端

```java
package com.example.gateway.config;

import io.nebula.rpc.grpc.client.GrpcClientProxyFactory;
import com.example.user.api.UserRpcClient;
import com.example.order.api.OrderRpcClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {
    
    @Bean
    public UserRpcClient userRpcClient(GrpcClientProxyFactory proxyFactory) {
        return proxyFactory.createClient(UserRpcClient.class, "user-service", 9090);
    }
    
    @Bean
    public OrderRpcClient orderRpcClient(GrpcClientProxyFactory proxyFactory) {
        return proxyFactory.createClient(OrderRpcClient.class, "order-service", 9090);
    }
}
```

#### 步骤3：配置路由使用gRPC过滤器

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: grpc-route
          uri: no://op  # 占位符，实际由gRPC过滤器处理
          predicates:
            - Path=/api/v1/**
          filters:
            - JwtAuth
            - Grpc  # 启用gRPC桥接
```

---

## 示例4：完整网关项目

### 项目结构

```
my-gateway/
+-- src/main/java/com/example/gateway/
|   +-- GatewayApplication.java
|   +-- config/
|   |   +-- GrpcClientConfig.java
|   +-- grpc/
|       +-- MyGrpcServiceRouter.java
+-- src/main/resources/
|   +-- application.yml
|   +-- application-dev.yml
|   +-- application-prod.yml
+-- pom.xml
```

### 完整配置

**application.yml**：

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      routes:
        # gRPC路由（优先）
        - id: grpc-route
          uri: no://op
          predicates:
            - Path=/api/v1/**
          filters:
            - JwtAuth
            - Grpc
        
        # 健康检查路由
        - id: actuator
          uri: forward:/actuator
          predicates:
            - Path=/actuator/**

nebula:
  gateway:
    enabled: true
    jwt:
      enabled: true
      secret: ${JWT_SECRET:default-secret-key-for-development-only}
      whitelist:
        - /api/v1/users/login
        - /api/v1/users/register
        - /api/v1/public/**
        - /actuator/**
      claim-headers:
        - phone:X-User-Phone
    logging:
      enabled: true
      slow-request-threshold: 3000
    rate-limit:
      enabled: true
      strategy: ip

# gRPC客户端配置
grpc:
  client:
    user-service:
      host: ${USER_SERVICE_HOST:localhost}
      port: ${USER_SERVICE_GRPC_PORT:9090}
    order-service:
      host: ${ORDER_SERVICE_HOST:localhost}
      port: ${ORDER_SERVICE_GRPC_PORT:9090}
```

### 运行测试

```bash
# 启动网关
mvn spring-boot:run

# 测试登录（白名单，不需要Token）
curl -X POST http://localhost:8080/api/v1/users/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000","password":"123456"}'

# 测试获取用户信息（需要Token）
curl http://localhost:8080/api/v1/users/info \
  -H "Authorization: Bearer <your-jwt-token>"

# 测试创建订单
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{"showtimeId":1,"seatIds":[1,2]}'
```

---

## 最佳实践

### 实践1：使用环境变量配置敏感信息

**推荐**：

```yaml
nebula:
  gateway:
    jwt:
      secret: ${JWT_SECRET}  # 从环境变量读取
```

**不推荐**：

```yaml
nebula:
  gateway:
    jwt:
      secret: hard-coded-secret  # 硬编码密钥
```

### 实践2：合理配置白名单

**推荐**：

```yaml
nebula:
  gateway:
    jwt:
      whitelist:
        - /api/v1/users/login      # 具体路径
        - /api/v1/users/register
        - /api/v1/public/**        # 公开接口统一前缀
```

**不推荐**：

```yaml
nebula:
  gateway:
    jwt:
      whitelist:
        - /api/**  # 太宽泛，失去认证意义
```

### 实践3：gRPC路由器分模块组织

对于大型项目，建议按服务分模块组织路由注册：

```java
@Component
public class MyGrpcServiceRouter extends AbstractGrpcServiceRouter {
    
    @Override
    protected void registerRoutes() {
        registerUserRoutes();
        registerOrderRoutes();
        registerPaymentRoutes();
    }
    
    private void registerUserRoutes() {
        // 用户服务路由
    }
    
    private void registerOrderRoutes() {
        // 订单服务路由
    }
    
    private void registerPaymentRoutes() {
        // 支付服务路由
    }
}
```

---

## 常见错误

### 错误1：JWT密钥长度不足

**错误现象**：

```
IllegalArgumentException: JWT secret must be at least 32 characters
```

**解决方案**：

确保JWT密钥长度至少32字符：

```yaml
nebula:
  gateway:
    jwt:
      secret: this-is-a-long-secret-key-at-least-32-characters
```

### 错误2：gRPC路由器未注册

**错误现象**：

```
IllegalStateException: 未配置GrpcServiceRouter
```

**解决方案**：

创建继承 `AbstractGrpcServiceRouter` 的类并添加 `@Component` 注解。

### 错误3：白名单路径不匹配

**错误现象**：

白名单接口仍然需要认证。

**解决方案**：

检查路径模式是否正确，使用Ant风格：

```yaml
whitelist:
  - /api/v1/public/**   # 正确：匹配/api/v1/public/下所有路径
  - /api/v1/public/*    # 错误：只匹配一级子路径
```

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [CONFIG.md](./CONFIG.md) - 配置参考
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 未来规划

---

> 如有问题或建议，欢迎提Issue或PR。


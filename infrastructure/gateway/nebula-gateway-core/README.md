# Nebula Gateway Core

API网关核心模块，提供构建API网关的基础设施组件。

## 概述

nebula-gateway-core 是 Nebula 框架的网关核心模块，基于 Spring Cloud Gateway 构建，提供以下核心能力：

- **HTTP 反向代理** - 将请求转发到后端服务的 Controller
- **请求日志** - 全局请求追踪、耗时统计、慢请求标记
- **限流支持** - 多种限流策略（IP、路径）
- **CORS 处理** - 跨域请求配置

## 设计理念

### 微服务三原则

基于微服务架构最佳实践，Gateway 职责已经简化为纯 HTTP 反向代理：

```
微服务三原则：
1. 前端接口通过 Controller 暴露（HTTP 代理）
2. 服务间接口通过 RpcClient 暴露（纯 RPC，无 HTTP 路径）
3. 服务间直接调用（不经过 Gateway）
```

### 架构图

```
+------------------+     +------------------+     +------------------+
|   HTTP Client    | --> |   API Gateway    | --> | Backend Service  |
|  (Browser/App)   |     | (HTTP反向代理)    |     |  (Controller)    |
+------------------+     +------------------+     +------------------+
                              |
                         +----+----+
                         |        |
                      认证    限流/日志
```

## 模块结构

```
nebula-gateway-core
  +-- config/
  |   +-- GatewayProperties.java         # 网关配置属性
  |   +-- GatewayRoutesAutoConfiguration # 路由自动配置
  |   +-- GatewayRedisAutoConfiguration  # Redis限流自动配置
  |   +-- RateLimitKeyResolverConfig     # 限流Key解析器
  +-- filter/
      +-- LoggingGlobalFilter            # 日志过滤器
```

## 组件说明

### 1. LoggingGlobalFilter

全局日志过滤器，为每个请求生成唯一的RequestId，记录请求开始/结束日志。

特性：
- RequestId生成与追踪
- 请求耗时统计
- 慢请求标记

### 2. GatewayRoutesAutoConfiguration

根据配置自动生成 Spring Cloud Gateway 路由：

- HTTP 代理路由配置
- 自定义路由定义
- 默认过滤器配置
- CORS 配置

### 3. RateLimitKeyResolverConfig

限流 Key 解析器配置：

- IP 限流策略
- 路径限流策略
- 可扩展的自定义策略

## 配置说明

完整配置示例：

```yaml
nebula:
  gateway:
    enabled: true
    
    # 日志配置
    logging:
      enabled: true
      request-id-header: X-Request-Id
      log-request-body: false
      log-response-body: false
      slow-request-threshold: 3000
    
    # 限流配置
    rate-limit:
      enabled: true
      strategy: ip  # ip, path
      replenish-rate: 100
      burst-capacity: 200
      redis:
        enabled: true
        host: localhost
        port: 6379
    
    # HTTP 代理配置
    http:
      enabled: true
      use-discovery: true  # 使用服务发现
      services:
        user-service:
          api-paths:
            - /api/v1/users/**
        order-service:
          api-paths:
            - /api/v1/orders/**
    
    # CORS配置
    cors:
      enabled: true
      allowed-origins:
        - "*"
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
        - OPTIONS
```

## 认证说明

JWT 认证已移至应用层实现，框架不再内置认证功能。

各应用（如 ticket-gateway）应自行：
1. 定义认证配置属性
2. 实现 GlobalFilter 进行 Token 验证
3. 配置白名单路径

示例：

```java
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 验证 Token
        // 将用户信息注入请求头
        return chain.filter(exchange);
    }
}
```

## 依赖

- Spring Cloud Gateway
- Jackson (JSON处理)
- Spring Boot WebFlux
- Spring Data Redis (可选，用于限流)

## 版本要求

- Java 21+
- Spring Boot 3.5+
- Spring Cloud 2025.0+

## 相关模块

- [nebula-starter-gateway](../../starter/nebula-starter-gateway) - Gateway启动器

# Nebula Starter Gateway - 使用示例

本文档展示 `nebula-starter-gateway` 的常见用法。

## 示例 1：基础网关路由

```yaml
server:
  port: 8080

spring:
  application:
    name: ticket-gateway

nebula:
  gateway:
    enabled: true
    http:
      enabled: true
      use-discovery: true
      services:
        ticket-user:
          api-paths:
            - /api/v1/users/**
        ticket-order:
          api-paths:
            - /api/v1/orders/**
```

## 示例 2：自定义认证过滤器

```java
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtProperties jwtProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (isWhitelisted(exchange.getRequest().getPath().value())) {
            return chain.filter(exchange);
        }
        String token = extractToken(exchange.getRequest());
        if (token == null || !validateToken(token)) {
            return unauthorized(exchange);
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
```

## 示例 3：票务场景（模块示例）

票务系统通常将用户、订单、票务等服务通过网关聚合：

```yaml
nebula:
  gateway:
    http:
      use-discovery: true
      services:
        ticket-user:
          api-paths:
            - /api/v1/users/**
        ticket-order:
          api-paths:
            - /api/v1/orders/**
        ticket-cinema:
          api-paths:
            - /api/v1/cinemas/**
```

网关负责 HTTP 聚合、限流与请求日志，服务间 RPC 调用不经过网关。

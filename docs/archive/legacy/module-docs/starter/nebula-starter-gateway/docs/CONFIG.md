# Nebula Starter Gateway - 配置参考

本文档描述 `nebula-starter-gateway` 的核心配置项。

## 配置前缀

- `nebula.gateway`

## 核心配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `nebula.gateway.enabled` | boolean | true | 是否启用网关 |
| `nebula.gateway.logging.enabled` | boolean | true | 是否启用请求日志 |
| `nebula.gateway.logging.slow-request-threshold` | long | 3000 | 慢请求阈值（毫秒） |
| `nebula.gateway.rate-limit.enabled` | boolean | true | 是否启用限流 |
| `nebula.gateway.rate-limit.strategy` | string | `ip` | 限流策略（ip/path） |
| `nebula.gateway.rate-limit.replenish-rate` | int | 100 | 每秒补充令牌数 |
| `nebula.gateway.rate-limit.burst-capacity` | int | 200 | 令牌桶容量 |
| `nebula.gateway.http.enabled` | boolean | true | 是否启用 HTTP 反向代理 |
| `nebula.gateway.http.use-discovery` | boolean | true | 是否使用服务发现 |
| `nebula.gateway.http.services.*.api-paths` | list | - | 服务映射路径 |
| `nebula.gateway.cors.enabled` | boolean | true | 是否启用 CORS |
| `nebula.gateway.cors.allowed-origins` | list | `*` | 允许的跨域来源 |

## 配置示例

```yaml
nebula:
  gateway:
    enabled: true
    logging:
      enabled: true
      slow-request-threshold: 3000
    rate-limit:
      enabled: true
      strategy: ip
      replenish-rate: 100
      burst-capacity: 200
    http:
      enabled: true
      use-discovery: true
      services:
        user-service:
          api-paths:
            - /api/v1/users/**
    cors:
      enabled: true
      allowed-origins:
        - "*"
```

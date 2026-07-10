# Nebula 配置说明

本文面向 Nebula `2.1.0-SNAPSHOT`。配置字段的最终定义以代码中的
`@ConfigurationProperties` 类为准；本页维护配置入口、安全要求和常用样例，不重复抄录
每个 Java 字段。

## 配置来源与优先级

配置优先级从高到低为：

1. 命令行参数
2. 环境变量和 JVM 系统属性
3. `application.yml` 或 `application.properties`
4. Starter 的 `META-INF/nebula-defaults.properties`
5. 配置属性类中的默认值

Starter 默认值不会覆盖应用显式配置。

## 主要配置前缀

| 能力 | 配置前缀 | 属性定义 |
| --- | --- | --- |
| Security | `nebula.security` | `SecurityProperties` |
| Web | `nebula.web` | `WebProperties` |
| Persistence | `nebula.data.persistence` | `DataSourceManager`、`MybatisPlusProperties` |
| Cache | `nebula.data.cache` | `CacheProperties` |
| Read/Write | `nebula.data.read-write-separation` | `ReadWriteDataSourceManager` |
| Sharding | `nebula.data.sharding` | `ShardingConfig` |
| Neo4j | `nebula.data.neo4j` | `Neo4jProperties` |
| RabbitMQ | `nebula.messaging.rabbitmq` | `RabbitMQProperties` |
| RocketMQ | `nebula.messaging.rocketmq` | `RocketMQProperties` |
| Redis Messaging | `nebula.messaging.redis` | `RedisMessagingProperties` |
| HTTP RPC | `nebula.rpc.http` | `HttpRpcProperties` |
| gRPC | `nebula.rpc.grpc` | `GrpcRpcProperties` |
| Async RPC | `nebula.rpc.async` | `AsyncRpcProperties` |
| RPC Discovery | `nebula.rpc.discovery` | `RpcDiscoveryProperties` |
| Nacos | `nebula.discovery.nacos` | `NacosProperties` |
| Redis Lock | `nebula.lock` | `RedisLockProperties` |
| Gateway | `nebula.gateway` | `GatewayProperties` |
| MinIO | `nebula.storage.minio` | `MinIOProperties` |
| Aliyun OSS | `nebula.storage.aliyun.oss` | `AliyunOSSProperties` |
| Elasticsearch | `nebula.search.elasticsearch` | `ElasticsearchProperties` |
| AI | `nebula.ai` | `AIProperties` |
| MCP | `nebula.ai.mcp` | `McpProperties` |
| Vector Store | `nebula.ai.vector-store` | `VectorStoreProperties` |
| RAG | `nebula.ai.rag` | `RagProperties` |
| Crawler | `nebula.crawler` | `CrawlerProperties` 及各实现属性类 |
| WebSocket | `nebula.websocket` | `WebSocketProperties`、`NettyWebSocketProperties` |
| Task | `nebula.task` | `TaskProperties` |
| Payment | `nebula.payment` | `PaymentProperties` |
| Notification | `nebula.notification` | `NotificationProperties` |

源码位置可用以下命令直接查找：

```bash
rg -n '@ConfigurationProperties' application autoconfigure core infrastructure integration
```

## 安全配置

JWT 密钥没有默认值，启用 JWT 时必须显式提供至少 32 个字符的密钥：

```yaml
nebula:
  security:
    enabled: true
    jwt:
      enabled: true
      secret: ${JWT_SECRET}
      expiration: 24h
      refresh-expiration: 7d
      header-name: Authorization
      token-prefix: "Bearer "
```

登录服务生成 access token 和 refresh token，鉴权过滤器只接受 access token。旧的
`io.nebula.web.auth.JwtUtils` 已废弃，新代码统一注入 `JwtService`。

Web 鉴权开启后，Security JWT 过滤器会沿用同一套 `JwtService`：

```yaml
nebula:
  web:
    auth:
      enabled: true
```

## Web 配置

```yaml
nebula:
  web:
    trusted-proxies:
      - 10.0.0.0/8
    cors:
      enabled: true
      allowed-origins:
        - https://console.example.com
    api-doc:
      enabled: true
      title: Example API
      version: 2.1.0
    performance:
      enabled: true
    health:
      enabled: true
```

`trusted-proxies` 为空时不会信任 `X-Forwarded-For` 或 `X-Real-IP`。CORS 允许来源默认
为空，生产环境不得使用不受控的通配来源。

## RPC 配置

```yaml
nebula:
  rpc:
    http:
      enabled: true
      client:
        enabled: true
        connect-timeout: 5000
        read-timeout: 10000
      server:
        enabled: true
        port: 8080
        context-path: /rpc
    grpc:
      enabled: true
      client:
        enabled: true
        target: localhost:9090
        request-timeout: 5000
        auth-token: ${GRPC_AUTH_TOKEN:}
      server:
        enabled: true
        auth-token: ${GRPC_AUTH_TOKEN:}

spring:
  grpc:
    server:
      port: 9090
```

gRPC 服务端设置 `auth-token` 后，客户端必须配置相同令牌。令牌不得写入日志或组件摘要。

## 外部服务开关

直接引入模块时，以下能力默认关闭，需要显式开启：

```yaml
nebula:
  data:
    persistence:
      enabled: true
    cache:
      enabled: true
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
  messaging:
    rabbitmq:
      enabled: true
  lock:
    enabled: true
  search:
    elasticsearch:
      enabled: true
      uris:
        - http://localhost:9200
  storage:
    minio:
      enabled: true
```

使用 Starter 时，上述部分开关可能已经通过最低优先级默认值开启，具体列表见
[Starter 选择指南](Nebula%20Starter%20选择指南.md)。

## TLS 防误配

Elasticsearch 和 HTTP 爬虫允许在开发环境跳过证书校验，但条件严格限制为：

- 至少存在一个活动 Profile。
- 所有活动 Profile 都属于 `dev`、`test`、`local`。
- 未配置 Profile、存在 `prod`，或出现未知 Profile 时保持证书校验。

生产环境不得配置：

```yaml
nebula:
  search:
    elasticsearch:
      ssl-verification-enabled: false
  crawler:
    http:
      trust-all-certs: true
```

## 时间和大小

`Duration` 字段使用 Spring Boot 标准格式，例如 `500ms`、`5s`、`10m`、`24h`。
仍声明为 `int` 或 `long` 的超时字段使用毫秒数，例如 `5000`。列表和 Map 使用 YAML
原生结构，避免用逗号字符串代替结构化值。

## 敏感信息

以下值只通过环境变量、密钥服务或部署平台 Secret 提供：

- JWT 和 gRPC 令牌
- 数据库、Redis、Nacos、RabbitMQ、Elasticsearch 密码
- MinIO、OSS Access Key 与 Secret Key
- AI API Key
- 支付和通知供应商密钥

示例配置只允许使用无权限的本地占位值，不得提交真实凭据。

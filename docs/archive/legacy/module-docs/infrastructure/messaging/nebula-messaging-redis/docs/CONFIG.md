# Nebula Messaging Redis - 配置参考

本文档说明 `nebula-messaging-redis` 的配置项，适用于 Redis Pub/Sub 与 Redis Stream 两种模式。

## 配置前缀

- `nebula.messaging.redis`

## 基础配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `nebula.messaging.redis.enabled` | boolean | true | 是否启用 Redis 消息模块 |
| `nebula.messaging.redis.channel-prefix` | string | `nebula:` | 频道前缀 |
| `nebula.messaging.redis.serializer` | string | `json` | 序列化方式（`json` / `jdk`） |

## Pub/Sub 配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `nebula.messaging.redis.pubsub.listener-thread-pool-size` | int | 4 | 监听线程池大小 |
| `nebula.messaging.redis.pubsub.pattern-subscription-enabled` | boolean | true | 是否启用模式订阅 |

## Stream 配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `nebula.messaging.redis.stream.enabled` | boolean | false | 是否启用 Stream |
| `nebula.messaging.redis.stream.consumer-group` | string | `nebula-consumer-group` | 消费者组 |
| `nebula.messaging.redis.stream.consumer-name-prefix` | string | `consumer-` | 消费者名称前缀 |
| `nebula.messaging.redis.stream.batch-size` | int | 10 | 每次拉取消息数量 |
| `nebula.messaging.redis.stream.poll-timeout` | long | 1000 | 拉取超时（毫秒） |
| `nebula.messaging.redis.stream.max-len` | long | 0 | 消息保留长度（0 表示不限制） |

## 配置示例

### 仅启用 Pub/Sub

```yaml
nebula:
  messaging:
    redis:
      enabled: true
      channel-prefix: "nebula:"
      serializer: json
      pubsub:
        listener-thread-pool-size: 4
        pattern-subscription-enabled: true
      stream:
        enabled: false
```

### 启用 Stream（可靠消息）

```yaml
nebula:
  messaging:
    redis:
      enabled: true
      channel-prefix: "nebula:"
      serializer: json
      stream:
        enabled: true
        consumer-group: "order-group"
        consumer-name-prefix: "order-consumer-"
        batch-size: 20
        poll-timeout: 1000
        max-len: 10000
```

## 说明

Redis 连接信息复用 Spring Boot 的 Redis 配置（如 `spring.data.redis.*`），确保 Redis 服务可访问。

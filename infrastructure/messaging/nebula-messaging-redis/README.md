# Nebula Messaging Redis

Nebula 框架的 Redis 消息模块，提供基于 Redis Pub/Sub 和 Redis Stream 的消息发送与订阅功能。

## 特性

- **双模式支持**: 同时支持 Redis Pub/Sub（实时）和 Redis Stream（可靠）
- **注解驱动**: 使用 `@RedisMessageHandler` 注解简化消息处理器定义
- **JSON 序列化**: 默认使用 Jackson 进行消息序列化
- **自动配置**: 开箱即用，零配置启动
- **统一 API**: 通过 `RedisMessageManager` 统一管理消息发送与订阅

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-messaging-redis</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 2. 配置

```yaml
nebula:
  messaging:
    redis:
      enabled: true
      channel-prefix: "nebula:"
      pubsub:
        listener-thread-pool-size: 4
      stream:
        enabled: true
        consumer-group: "my-consumer-group"
        batch-size: 10
        poll-timeout: 1000
```

### 3. 使用

#### 发送消息

```java
@Service
public class NotificationService {
    
    @Autowired
    private RedisMessageManager messageManager;
    
    // 发送 Pub/Sub 消息（实时，不持久化）
    public void sendNotification(Notification notification) {
        messageManager.publish("user:notification", notification);
    }
    
    // 发送 Stream 消息（可靠，持久化）
    public void sendOrder(Order order) {
        messageManager.send("order:events", order);
    }
}
```

#### 接收消息（注解方式）

```java
@Component
public class NotificationHandler {
    
    // 订阅 Pub/Sub 频道
    @RedisMessageHandler(channel = "user:notification")
    public void handleNotification(Message<Notification> message) {
        Notification notification = message.getPayload();
        // 处理通知
    }
    
    // 模式订阅（支持通配符）
    @RedisMessageHandler(pattern = "user:*")
    public void handleUserEvents(Message<UserEvent> message) {
        // 处理用户相关事件
    }
    
    // 异步处理
    @RedisMessageHandler(channel = "async:events", async = true)
    public void handleAsyncEvent(Message<Event> message) {
        // 异步处理事件
    }
}
```

#### 接收消息（编程方式）

```java
@Service
public class OrderService {
    
    @Autowired
    private RedisMessageManager messageManager;
    
    @PostConstruct
    public void init() {
        // 订阅 Pub/Sub
        messageManager.subscribe("order:created", message -> {
            Order order = (Order) message.getPayload();
            // 处理订单
        });
        
        // 订阅 Stream
        messageManager.subscribeStream("order:events", message -> {
            OrderEvent event = (OrderEvent) message.getPayload();
            // 处理订单事件
        });
    }
}
```

## Pub/Sub vs Stream

| 特性 | Pub/Sub | Stream |
|------|---------|--------|
| 消息持久化 | 否 | 是 |
| 消费者组 | 否 | 是 |
| 消息确认 | 否 | 是 |
| 消息重试 | 否 | 是 |
| 历史消息 | 否 | 是 |
| 适用场景 | 实时通知、广播 | 可靠消息、任务队列 |

## 配置参考

```yaml
nebula:
  messaging:
    redis:
      # 是否启用 Redis 消息
      enabled: true
      
      # 频道前缀
      channel-prefix: "nebula:"
      
      # 序列化方式: json, jdk
      serializer: json
      
      # Pub/Sub 配置
      pubsub:
        # 监听线程池大小
        listener-thread-pool-size: 4
        # 是否启用模式订阅
        pattern-subscription-enabled: true
      
      # Stream 配置
      stream:
        # 是否启用 Stream
        enabled: false
        # 消费者组名称
        consumer-group: "nebula-consumer-group"
        # 消费者名称前缀
        consumer-name-prefix: "consumer-"
        # 每次拉取的消息数量
        batch-size: 10
        # 拉取超时时间（毫秒）
        poll-timeout: 1000
        # 消息保留数量（0表示不限制）
        max-len: 0
```

## 模块结构

```
nebula-messaging-redis/
├── src/main/java/io/nebula/messaging/redis/
│   ├── annotation/
│   │   ├── RedisMessageHandler.java          # 消息处理器注解
│   │   └── RedisMessageHandlerProcessor.java # 注解处理器
│   ├── config/
│   │   ├── RedisMessagingProperties.java     # 配置属性
│   │   └── RedisMessagingAutoConfiguration.java # 自动配置类
│   ├── consumer/
│   │   └── RedisMessageConsumer.java         # Pub/Sub 消费者
│   ├── producer/
│   │   └── RedisMessageProducer.java         # Pub/Sub 生产者
│   ├── stream/
│   │   ├── RedisStreamConsumer.java          # Stream 消费者
│   │   └── RedisStreamProducer.java          # Stream 生产者
│   ├── support/
│   │   └── RedisMessageSerializer.java       # 消息序列化器
│   └── RedisMessageManager.java              # 统一消息管理器
```

> **注意**: 自动配置由 `nebula-autoconfigure` 模块统一管理，注册在其 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件中。

## 注意事项

1. **Pub/Sub 消息不持久化**: 如果没有订阅者在线，消息将丢失
2. **Stream 需要 Redis 5.0+**: 请确保 Redis 版本支持 Stream 功能
3. **消费者组需预先创建**: Stream 模式下会自动创建消费者组
4. **消息确认**: Stream 模式下默认自动确认，可通过配置关闭

## 许可证

Apache License 2.0


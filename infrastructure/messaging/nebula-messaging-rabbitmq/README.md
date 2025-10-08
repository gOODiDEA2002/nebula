# Nebula Messaging RabbitMQ 模块

## 📋 模块简介

`nebula-messaging-rabbitmq` 是 Nebula 框架的消息传递模块，提供了基于 RabbitMQ 的统一消息抽象和强大的消息传递能力。该模块基于 Spring AMQP 构建，支持多种消息传递模式和高级特性。

## ✨ 功能特性

### 🎯 核心功能
- **消息生产**: 支持同步/异步发送、批量发送、延迟消息、顺序消息
- **消息消费**: 支持推模式和拉模式消费，自动/手动确认
- **消息路由**: 支持主题路由、标签过滤、内容路由等多种路由策略
- **注解驱动**: 使用 @MessageHandler 注解自动注册消息处理器
- **交换机管理**: 支持 Topic、Direct、Fanout、Headers 等多种交换机类型

### 🚀 增强特性
- **自动配置**: Spring Boot 自动配置，零配置启动
- **连接管理**: 自动重连、心跳检测、连接池管理
- **消息序列化**: 支持 JSON、Java、Protobuf 等多种序列化方式
- **性能监控**: 提供生产者和消费者的统计信息
- **异常处理**: 完善的异常处理和重试机制

## 🚀 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-messaging-rabbitmq</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

### 基础配置

在 `application.yml` 中配置 RabbitMQ：

```yaml
nebula:
  messaging:
    rabbitmq:
      enabled: true
      host: localhost
      port: 5672
      username: guest
      password: guest
      virtual-host: /
      
      # 连接配置
      connection-timeout: 60000
      heartbeat: 60
      automatic-recovery: true
      network-recovery-interval: 5000
      
      # 生产者配置
      producer:
        publisher-confirms: true
        confirm-timeout: 5000
        publisher-returns: true
      
      # 消费者配置
      consumer:
        prefetch-count: 1
        auto-ack: false
        retry-count: 3
        retry-interval: 1000
      
      # Exchange配置
      exchange:
        default-type: topic
        durable: true
        auto-delete: false
```

## 📚 基础消息传递功能

### 1. 消息生产者

#### 简单发送

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final MessageManager messageManager;
    
    public void createOrder(Order order) {
        // 业务逻辑...
        
        // 发送订单创建通知
        Message<OrderEvent> message = Message.<OrderEvent>builder()
            .topic("order.created")
            .payload(new OrderEvent(order))
            .build();
            
        SendResult result = messageManager.getProducer().send(message);
        
        if (result.isSuccess()) {
            log.info("订单通知发送成功: messageId={}", result.getMessageId());
        }
    }
}
```

#### 异步发送

```java
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final MessageManager messageManager;
    
    public void sendNotificationAsync(String userId, String content) {
        messageManager.getProducer()
            .sendAsync("user.notification", new NotificationEvent(userId, content))
            .thenAccept(result -> {
                if (result.isSuccess()) {
                    log.info("通知发送成功: {}", result.getMessageId());
                } else {
                    log.error("通知发送失败: {}", result.getErrorMessage());
                }
            });
    }
}
```

#### 批量发送

```java
@Service
@RequiredArgsConstructor
public class BatchNotificationService {
    
    private final MessageManager messageManager;
    
    public void sendBatchNotifications(List<NotificationEvent> events) {
        List<Message<NotificationEvent>> messages = events.stream()
            .map(event -> Message.<NotificationEvent>builder()
                .topic("batch.notification")
                .payload(event)
                .build())
            .collect(Collectors.toList());
            
        BatchSendResult result = messageManager.getProducer().sendBatch(messages);
        
        log.info("批量发送完成: 总数={}, 成功={}, 失败={}", 
            result.getTotalCount(), result.getSuccessCount(), result.getFailedCount());
    }
}
```

### 2. 消息消费者

#### 注解方式（推荐）

```java
@Component
@Slf4j
public class OrderNotificationHandler {
    
    @MessageHandler("order.created")
    public void handleOrderCreated(Message<OrderEvent> message) {
        OrderEvent event = message.getPayload();
        log.info("收到订单创建通知: orderId={}, userId={}", 
            event.getOrderId(), event.getUserId());
        
        // 处理订单创建事件
        // ...
    }
    
    @MessageHandler(topic = "order.updated", queue = "order-update-queue", concurrency = 3)
    public void handleOrderUpdated(Message<OrderEvent> message) {
        OrderEvent event = message.getPayload();
        log.info("收到订单更新通知: orderId={}, status={}", 
            event.getOrderId(), event.getStatus());
        
        // 处理订单更新事件
        // ...
    }
    
    @MessageHandler(topic = "order.cancelled", maxRetries = 5)
    public void handleOrderCancelled(Message<OrderEvent> message) {
        OrderEvent event = message.getPayload();
        log.info("收到订单取消通知: orderId={}", event.getOrderId());
        
        // 处理订单取消事件
        // ...
    }
}
```

#### 编程方式

```java
@Service
@RequiredArgsConstructor
public class ManualConsumerService {
    
    private final MessageManager messageManager;
    
    @PostConstruct
    public void init() {
        // 订阅主题
        messageManager.getConsumer().subscribe("manual.topic", message -> {
            log.info("收到消息: {}", message.getPayload());
            // 处理消息
        });
    }
}
```

### 3. 拉模式消费

```java
@Service
@RequiredArgsConstructor
public class PullConsumerService {
    
    private final MessageManager messageManager;
    
    public void pullMessages() {
        MessageConsumer<?> consumer = messageManager.getConsumer();
        
        // 拉取单个消息
        Message<?> message = consumer.pullOne("pull.topic", Duration.ofSeconds(5));
        if (message != null) {
            log.info("拉取到消息: {}", message.getPayload());
        }
        
        // 批量拉取
        List<Message<Object>> messages = consumer.pull("pull.topic", 10, Duration.ofSeconds(5));
        log.info("批量拉取到 {} 条消息", messages.size());
    }
}
```

## 🔧 高级特性

### 消息路由

```java
@Configuration
public class MessageRoutingConfig {
    
    @Bean
    public MessageRouter customMessageRouter() {
        DefaultMessageRouter router = new DefaultMessageRouter();
        
        // 添加路由规则
        router.addRoute("order.*", "order-queue");
        router.addRoute("user.*", "user-queue");
        router.addRoute("payment.*", "payment-queue");
        
        // 添加条件路由
        router.addRoute(
            message -> message.getHeaders() != null && 
                      "VIP".equals(message.getHeaders().get("userLevel")),
            "vip-queue"
        );
        
        // 设置默认路由
        router.setDefaultRoute("default-queue");
        
        return router;
    }
}
```

### 延迟消息

```java
@Service
@RequiredArgsConstructor
public class DelayedMessageService {
    
    private final MessageManager messageManager;
    
    public void sendDelayedMessage(OrderEvent event) {
        // 发送 5 分钟后的延迟消息
        messageManager.getProducer().sendDelayMessage(
            "order.reminder",
            event,
            Duration.ofMinutes(5)
        );
    }
}
```

### 顺序消息

```java
@Service
@RequiredArgsConstructor
public class OrderedMessageService {
    
    private final MessageManager messageManager;
    
    public void sendOrderedMessage(String orderId, OrderEvent event) {
        // 使用订单ID作为分片键，保证同一订单的消息有序
        messageManager.getProducer().sendOrderedMessage(
            "order.events",
            event,
            orderId  // 分片键
        );
    }
}
```

### 消息序列化

```java
@Configuration
public class MessageSerializerConfig {
    
    @Bean
    public MessageSerializer customMessageSerializer() {
        // 使用自定义的 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        
        return new JsonMessageSerializer(objectMapper);
    }
}
```

### Exchange 管理

```java
@Service
@RequiredArgsConstructor
public class ExchangeManagementService {
    
    private final RabbitMQExchangeManager exchangeManager;
    
    public void setupExchanges() {
        // 声明 Topic Exchange
        exchangeManager.declareTopicExchange("order-exchange");
        
        // 声明 Direct Exchange
        exchangeManager.declareDirectExchange("direct-exchange");
        
        // 声明 Fanout Exchange
        exchangeManager.declareFanoutExchange("fanout-exchange");
        
        // 声明队列并绑定
        exchangeManager.declareQueue("order-queue", true, false, false, null);
        exchangeManager.bindQueue("order-queue", "order-exchange", "order.#", null);
    }
}
```

## 📊 性能监控

### 生产者统计

```java
@Service
@RequiredArgsConstructor
public class ProducerMonitorService {
    
    private final MessageManager messageManager;
    
    public ProducerStats getProducerStats() {
        ProducerStats stats = messageManager.getProducer().getStats();
        
        log.info("生产者统计: 发送总数={}, 成功={}, 失败={}, 成功率={}%", 
            stats.getSentCount(),
            stats.getSuccessCount(),
            stats.getFailedCount(),
            stats.getSuccessRate() * 100
        );
        
        return stats;
    }
}
```

### 消费者统计

```java
@Service
@RequiredArgsConstructor
public class ConsumerMonitorService {
    
    private final MessageManager messageManager;
    
    public ConsumerStats getConsumerStats() {
        ConsumerStats stats = messageManager.getConsumer().getStats();
        
        log.info("消费者统计: 消费总数={}, 成功={}, 失败={}, 成功率={}%", 
            stats.getConsumedCount(),
            stats.getSuccessCount(),
            stats.getFailedCount(),
            stats.getSuccessRate() * 100
        );
        
        return stats;
    }
}
```

## 🔍 故障排查

### 常见问题

1. **连接失败**
   - 检查 RabbitMQ 服务是否启动
   - 验证连接配置（host、port、username、password）
   - 确认防火墙和网络配置

2. **消息发送失败**
   - 检查 Exchange 是否存在
   - 验证路由键配置
   - 确认消息序列化是否成功

3. **消息消费失败**
   - 检查队列是否声明
   - 验证绑定关系是否正确
   - 确认消息处理逻辑是否有异常

### 开启调试日志

```yaml
logging:
  level:
    io.nebula.messaging: DEBUG
    com.rabbitmq: DEBUG
    org.springframework.amqp: DEBUG
```

## 📖 完整示例

详细的功能演示请参考：
- [Nebula Messaging RabbitMQ 功能测试指南](../../../nebula-example/docs/nebula-messaging-rabbitmq-test.md)
- [完整示例项目](../../../nebula-example)

## 🎯 最佳实践

### 1. 消息设计

```java
/**
 * 消息事件应该包含足够的信息，避免消费者需要回查
 */
@Data
public class OrderEvent {
    private Long orderId;
    private Long userId;
    private String orderNo;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createTime;
    
    // 包含业务所需的所有关键信息
}
```

### 2. 异常处理

```java
@Component
@Slf4j
public class RobustMessageHandler {
    
    @MessageHandler(topic = "order.created", maxRetries = 5)
    public void handleOrderCreated(Message<OrderEvent> message) {
        try {
            // 业务处理
            processOrder(message.getPayload());
            
        } catch (BusinessException e) {
            // 业务异常，不重试
            log.error("业务处理失败，放弃重试: {}", e.getMessage());
            throw new RuntimeException("业务异常，不重试", e);
            
        } catch (Exception e) {
            // 系统异常，允许重试
            log.error("系统异常，将重试: {}", e.getMessage());
            throw e;
        }
    }
}
```

### 3. 幂等性保证

```java
@Service
@RequiredArgsConstructor
public class IdempotentMessageHandler {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @MessageHandler("order.payment")
    public void handlePayment(Message<PaymentEvent> message) {
        String messageId = message.getId();
        
        // 检查消息是否已处理
        String key = "processed:message:" + messageId;
        Boolean isProcessed = redisTemplate.opsForValue().setIfAbsent(
            key, "1", Duration.ofDays(1)
        );
        
        if (Boolean.FALSE.equals(isProcessed)) {
            log.info("消息已处理过，跳过: messageId={}", messageId);
            return;
        }
        
        try {
            // 处理支付事件
            processPayment(message.getPayload());
            
        } catch (Exception e) {
            // 处理失败，删除标记，允许重试
            redisTemplate.delete(key);
            throw e;
        }
    }
}
```

### 4. 消息优先级

```java
@Service
@RequiredArgsConstructor
public class PriorityMessageService {
    
    private final MessageManager messageManager;
    
    public void sendVipOrder(OrderEvent event) {
        Message<OrderEvent> message = Message.<OrderEvent>builder()
            .topic("order.created")
            .payload(event)
            .priority(9)  // 高优先级
            .build();
            
        messageManager.getProducer().send(message);
    }
    
    public void sendNormalOrder(OrderEvent event) {
        Message<OrderEvent> message = Message.<OrderEvent>builder()
            .topic("order.created")
            .payload(event)
            .priority(5)  // 普通优先级
            .build();
            
        messageManager.getProducer().send(message);
    }
}
```

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request 来帮助改进这个模块。

## 📄 许可证

本项目基于 Apache 2.0 许可证开源。


# Nebula Messaging RabbitMQ 模块

##  模块简介

`nebula-messaging-rabbitmq` 是 Nebula 框架的消息传递模块，提供了基于 RabbitMQ 的统一消息抽象和强大的消息传递能力该模块基于 Spring AMQP 构建，支持多种消息传递模式和高级特性

##  功能特性

###  核心功能
- **消息生产**: 支持同步/异步发送批量发送延迟消息顺序消息
- **消息消费**: 支持推模式和拉模式消费，自动/手动确认
- **消息路由**: 支持主题路由标签过滤内容路由等多种路由策略
- **注解驱动**: 使用 @MessageHandler 注解自动注册消息处理器
- **交换机管理**: 支持 TopicDirectFanoutHeaders 等多种交换机类型

###  增强特性
- **自动配置**: Spring Boot 自动配置，零配置启动
- **连接管理**: 自动重连心跳检测连接池管理
- **消息序列化**: 支持 JSONJavaProtobuf 等多种序列化方式
- **性能监控**: 提供生产者和消费者的统计信息
- **异常处理**: 完善的异常处理和重试机制

##  快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-messaging-rabbitmq</artifactId>
    <version>2.0.1-SNAPSHOT</version>
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

##  基础消息传递功能

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

##  高级特性

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

### 延迟消息（增强版）

Nebula框架提供了完整的延时消息支持，基于RabbitMQ的TTL+DLX机制实现。

#### 快速使用

```java
@Service
@RequiredArgsConstructor
public class OrderTimeoutService {
    
    private final DelayMessageProducer delayMessageProducer;
    
    /**
     * 发送订单超时消息
     * 30分钟后如果订单未支付，自动取消
     */
    public void sendOrderTimeoutMessage(Long orderId) {
        OrderTimeoutEvent event = new OrderTimeoutEvent(orderId);
        
        // 方式1：简单发送
        delayMessageProducer.send(
            "order.timeout", 
            event, 
            Duration.ofMinutes(30)
        );
        
        // 方式2：使用DelayMessage对象（支持更多配置）
        DelayMessage<OrderTimeoutEvent> message = DelayMessage.<OrderTimeoutEvent>builder()
                .topic("order.events")
                .queue("order.timeout.queue")
                .payload(event)
                .delay(Duration.ofMinutes(30))
                .maxRetries(3)
                .retryInterval(Duration.ofSeconds(5))
                .priority(8)
                .build();
        
        DelayMessageResult result = delayMessageProducer.send(message);
        
        if (result.isSuccess()) {
            log.info("延时消息发送成功: messageId={}, 将在{}后处理", 
                result.getMessageId(), Duration.ofMinutes(30));
        }
    }
}
```

#### 消费延时消息

```java
@Component
@Slf4j
public class OrderTimeoutHandler {
    
    /**
     * 方式1：使用@DelayMessageListener注解
     */
    @DelayMessageListener(
        queue = "order.timeout.queue",
        topic = "order.events",
        maxRetries = 3
    )
    public void handleOrderTimeout(OrderTimeoutEvent event, DelayMessageContext context) {
        log.info("处理订单超时: orderId={}, 延时误差={}ms, 总延时={}ms",
                event.getOrderId(),
                context.getDelayError(),
                context.getTotalDelay());
        
        // 检查订单状态，如果未支付则取消
        orderService.cancelUnpaidOrder(event.getOrderId());
    }
    
    /**
     * 方式2：编程式订阅
     */
    @PostConstruct
    public void init() throws IOException {
        delayMessageConsumer.subscribe(
            "order.timeout.queue",
            OrderTimeoutEvent.class,
            (event, context) -> {
                log.info("收到延时消息: orderId={}", event.getOrderId());
                orderService.cancelUnpaidOrder(event.getOrderId());
            }
        );
    }
}
```

#### 配置延时消息

在 `application.yml` 中配置：

```yaml
nebula:
  messaging:
    rabbitmq:
      # 基础配置...
      
      # 延时消息配置
      delay-message:
        enabled: true                      # 是否启用延时消息
        default-max-retries: 3             # 默认最大重试次数
        default-retry-interval: 1000       # 默认重试间隔(毫秒)
        max-delay-millis: 604800000        # 最大延时时间(7天)
        min-delay-millis: 1000             # 最小延时时间(1秒)
        auto-create-resources: true        # 自动创建交换机和队列
        enable-dead-letter-queue: true     # 启用死信队列
        dead-letter-exchange: nebula.dlx.exchange
        dead-letter-queue: nebula.dlx.queue
```

#### 工作原理

延时消息基于RabbitMQ的TTL（Time To Live）+ DLX（Dead Letter Exchange）机制：

1. 消息首先发送到延时队列（设置了TTL）
2. 消息在延时队列中等待，TTL到期后变为死信
3. 死信自动转发到目标交换机（DLX）
4. 消息最终路由到目标队列被消费

```
[生产者] --> [延时交换机] --> [延时队列(TTL)] --> [DLX目标交换机] --> [目标队列] --> [消费者]
```

#### 使用场景

1. **订单超时取消**：用户下单30分钟未支付自动取消
2. **优惠券过期提醒**：优惠券过期前3天发送提醒
3. **会员到期通知**：会员到期前7天发送续费提醒
4. **定时任务**：延时发送营销短信
5. **重试机制**：失败后延时重试

#### 高级特性

批量发送延时消息：

```java
@Service
@RequiredArgsConstructor
public class BatchDelayService {
    
    private final DelayMessageProducer delayMessageProducer;
    
    public void sendBatchReminders(List<Order> orders) {
        List<DelayMessage<OrderReminderEvent>> messages = orders.stream()
            .map(order -> DelayMessage.<OrderReminderEvent>builder()
                .topic("order.reminder")
                .queue("order.reminder.queue")
                .payload(new OrderReminderEvent(order.getId()))
                .delay(Duration.ofHours(1))
                .build())
            .collect(Collectors.toList());
        
        BatchDelayMessageResult result = delayMessageProducer.sendBatch(messages);
        
        log.info("批量发送完成: 总数={}, 成功={}, 失败={}, 成功率={}%",
                result.getTotalCount(),
                result.getSuccessCount(),
                result.getFailedCount(),
                result.getSuccessRate() * 100);
    }
}
```

#### 注意事项

1. **延时精度**：基于TTL机制，延时精度约为毫秒级，但受RabbitMQ调度影响可能有数秒误差
2. **延时范围**：建议延时时间在1秒到7天之间
3. **消息持久化**：延时消息默认持久化，确保RabbitMQ重启后不丢失
4. **重试机制**：支持自动重试，超过最大重试次数后进入死信队列
5. **监控告警**：建议监控死信队列，及时处理失败消息

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

##  性能监控

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

##  故障排查

### 常见问题

1. **连接失败**
   - 检查 RabbitMQ 服务是否启动
   - 验证连接配置（hostportusernamepassword）
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

##  完整示例

详细的功能演示请参考：
- [Nebula Messaging RabbitMQ 功能测试指南](../../../nebula-example/docs/nebula-messaging-rabbitmq-test.md)
- [完整示例项目](../../../nebula-example)

##  最佳实践

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

##  贡献指南

欢迎提交 Issue 和 Pull Request 来帮助改进这个模块

##  许可证

本项目基于 Apache 2.0 许可证开源


## 🧪 测试

本模块提供完整的单元测试文档和示例，详见 [TESTING.md](./TESTING.md)


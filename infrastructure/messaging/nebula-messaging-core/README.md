# Nebula Messaging Core

> Nebula 框架的消息传递核心抽象层，提供统一的消息处理接口，支持多种消息中间件实现

## 模块概述

`nebula-messaging-core` 是 Nebula 框架消息传递功能的核心抽象层，定义了统一的消息处理接口和规范该模块不依赖于特定的消息中间件实现，为上层应用提供了一致的消息处理API

## 核心特性

- 统一的消息抽象模型
- 生产者消费者模式
- 注解驱动的消息处理
- 消息路由和分发
- 消息序列化支持
- 异常处理机制
- 扩展性强，易于集成不同的消息中间件

## 核心组件

### 1. Message 消息对象

统一的消息数据模型，包含完整的消息元数据：

```java
@Data
@Builder
public class Message<T> {
    private String id;              // 消息ID
    private String topic;           // 消息主题
    private String queue;           // 消息队列
    private String tag;             // 消息标签
    private T payload;              // 消息载荷
    private Map<String, String> headers;  // 消息头
    private LocalDateTime createTime;      // 创建时间
    private LocalDateTime sendTime;        // 发送时间
    private int priority;           // 优先级 (0-9)
    private int retryCount;         // 重试次数
    private int maxRetryCount;      // 最大重试次数
    private boolean requireAck;     // 是否需要确认
    private boolean persistent;     // 是否持久化
    
    // 工具方法
    public boolean isExpired() { ... }
    public boolean canRetry() { ... }
    public void incrementRetryCount() { ... }
    
    // 静态工厂方法
    public static <T> Message<T> of(String topic, T payload) { ... }
}
```

### 2. MessageProducer 生产者接口

发送消息的抽象接口：

```java
public interface MessageProducer {
    
    /**
     * 发送消息
     */
    <T> void send(String topic, Message<T> message);
    
    /**
     * 发送消息到指定队列
     */
    <T> void send(String topic, String queue, Message<T> message);
    
    /**
     * 异步发送消息
     */
    <T> CompletableFuture<Void> sendAsync(String topic, Message<T> message);
    
    /**
     * 批量发送消息
     */
    <T> void sendBatch(String topic, List<Message<T>> messages);
    
    /**
     * 延迟发送消息
     */
    <T> void sendDelayed(String topic, Message<T> message, Duration delay);
}
```

### 3. MessageConsumer 消费者接口

接收和处理消息的抽象接口：

```java
public interface MessageConsumer {
    
    /**
     * 订阅主题
     */
    void subscribe(String topic);
    
    /**
     * 取消订阅
     */
    void unsubscribe(String topic);
    
    /**
     * 启动消费
     */
    void start();
    
    /**
     * 停止消费
     */
    void stop();
}
```

### 4. MessageHandler 消息处理器

处理具体消息的接口：

```java
public interface MessageHandler<T> {
    
    /**
     * 处理消息
     * 
     * @param message 消息对象
     * @throws Exception 处理异常
     */
    void handle(Message<T> message) throws Exception;
    
    /**
     * 获取处理的消息类型
     * 
     * @return 消息类型
     */
    Class<T> getMessageType();
}
```

### 5. MessageManager 消息管理器

统一管理生产者和消费者：

```java
public interface MessageManager {
    
    // 获取生产者和消费者
    MessageProducer getProducer();
    MessageConsumer getConsumer();
    
    // 生命周期管理
    void start() throws Exception;
    void stop() throws Exception;
    boolean isRunning();
    
    // 处理器管理
    void registerHandler(String topic, MessageHandler<?> handler);
    void unregisterHandler(String topic);
    
    // 主题管理
    void createTopic(String topic);
    void deleteTopic(String topic);
    boolean topicExists(String topic);
}
```

### 6. @MessageHandler 注解

基于注解的消息处理器声明：

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MessageHandler {
    
    /**
     * 消息主题
     */
    String value();
    
    /**
     * 消息队列（可选）
     */
    String queue() default "";
    
    /**
     * 消息标签（可选，用于过滤）
     */
    String tag() default "";
    
    /**
     * 消费者组
     */
    String consumerGroup() default "";
    
    /**
     * 并发消费线程数
     */
    int concurrency() default 1;
}
```

## 使用示例

### 1. 发送消息

```java
@Service
public class OrderService {
    
    @Autowired
    private MessageManager messageManager;
    
    public void createOrder(Order order) {
        // 保存订单
        orderRepository.save(order);
        
        // 发送订单创建消息
        Message<OrderCreatedEvent> message = Message.<OrderCreatedEvent>builder()
            .topic("order-events")
            .queue("order.created")
            .payload(new OrderCreatedEvent(order.getId(), order.getUserId()))
            .priority(8)
            .persistent(true)
            .build();
        
        messageManager.getProducer().send("order-events", message);
    }
    
    public void cancelOrder(Long orderId) {
        // 取消订单
        orderRepository.updateStatus(orderId, OrderStatus.CANCELLED);
        
        // 发送订单取消消息
        Message<String> message = Message.of("order-events", "order.cancelled", orderId.toString());
        messageManager.getProducer().send("order-events", message);
    }
}
```

### 2. 消费消息（方式一：实现接口）

```java
@Component
public class OrderCreatedHandler implements MessageHandler<OrderCreatedEvent> {
    
    @Autowired
    private InventoryService inventoryService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Override
    public void handle(Message<OrderCreatedEvent> message) throws Exception {
        OrderCreatedEvent event = message.getPayload();
        
        // 扣减库存
        inventoryService.decreaseStock(event.getOrderId());
        
        // 发送通知
        notificationService.sendOrderNotification(event.getUserId(), event.getOrderId());
        
        log.info("订单创建事件处理完成: orderId={}", event.getOrderId());
    }
    
    @Override
    public Class<OrderCreatedEvent> getMessageType() {
        return OrderCreatedEvent.class;
    }
}
```

### 3. 消费消息（方式二：使用注解）

```java
@Component
public class OrderEventListener {
    
    @MessageHandler("order.created")
    public void handleOrderCreated(Message<OrderCreatedEvent> message) {
        OrderCreatedEvent event = message.getPayload();
        log.info("接收到订单创建事件: orderId={}", event.getOrderId());
        
        // 处理订单创建逻辑
        processOrderCreated(event);
    }
    
    @MessageHandler(value = "order.cancelled", queue = "order.cancelled.queue")
    public void handleOrderCancelled(Message<String> message) {
        String orderId = message.getPayload();
        log.info("接收到订单取消事件: orderId={}", orderId);
        
        // 处理订单取消逻辑
        processOrderCancelled(orderId);
    }
    
    @MessageHandler(value = "order.paid", consumerGroup = "payment-group", concurrency = 3)
    public void handleOrderPaid(Message<OrderPaidEvent> message) {
        OrderPaidEvent event = message.getPayload();
        log.info("接收到订单支付事件: orderId={}", event.getOrderId());
        
        // 处理订单支付逻辑（支持并发消费）
        processOrderPaid(event);
    }
}
```

### 4. 异步发送消息

```java
@Service
public class NotificationService {
    
    @Autowired
    private MessageProducer messageProducer;
    
    public void sendBatchNotifications(List<User> users, String content) {
        List<CompletableFuture<Void>> futures = users.stream()
            .map(user -> {
                Message<NotificationData> message = Message.<NotificationData>builder()
                    .topic("notifications")
                    .payload(new NotificationData(user.getId(), content))
                    .priority(5)
                    .build();
                
                return messageProducer.sendAsync("notifications", message);
            })
            .collect(Collectors.toList());
        
        // 等待所有消息发送完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> log.info("所有通知消息发送完成"))
            .exceptionally(ex -> {
                log.error("发送通知消息失败", ex);
                return null;
            });
    }
}
```

### 5. 延迟消息

```java
@Service
public class ReminderService {
    
    @Autowired
    private MessageProducer messageProducer;
    
    public void scheduleReminder(Long userId, String content, Duration delay) {
        Message<ReminderData> message = Message.<ReminderData>builder()
            .topic("reminders")
            .payload(new ReminderData(userId, content))
            .delayTime(LocalDateTime.now().plus(delay))
            .build();
        
        // 延迟发送消息
        messageProducer.sendDelayed("reminders", message, delay);
        
        log.info("已安排提醒消息，将在 {} 后发送", delay);
    }
}
```

## 消息路由

### MessageRouter 消息路由器

```java
public interface MessageRouter {
    
    /**
     * 路由消息到对应的处理器
     */
    void route(Message<?> message);
    
    /**
     * 注册路由规则
     */
    void registerRoute(String topic, MessageHandler<?> handler);
    
    /**
     * 取消路由规则
     */
    void unregisterRoute(String topic);
}
```

### 默认路由实现

```java
@Component
public class DefaultMessageRouter implements MessageRouter {
    
    private final Map<String, List<MessageHandler<?>>> routes = new ConcurrentHashMap<>();
    
    @Override
    public void route(Message<?> message) {
        String topic = message.getTopic();
        List<MessageHandler<?>> handlers = routes.get(topic);
        
        if (handlers == null || handlers.isEmpty()) {
            log.warn("未找到主题 {} 的消息处理器", topic);
            return;
        }
        
        for (MessageHandler handler : handlers) {
            try {
                handler.handle(message);
            } catch (Exception e) {
                log.error("处理消息失败: topic={}, messageId={}", topic, message.getId(), e);
                
                // 重试逻辑
                if (message.canRetry()) {
                    message.incrementRetryCount();
                    retryMessage(message);
                }
            }
        }
    }
    
    @Override
    public void registerRoute(String topic, MessageHandler<?> handler) {
        routes.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(handler);
    }
    
    @Override
    public void unregisterRoute(String topic) {
        routes.remove(topic);
    }
}
```

## 消息序列化

### MessageSerializer 接口

```java
public interface MessageSerializer {
    
    /**
     * 序列化消息
     */
    byte[] serialize(Message<?> message);
    
    /**
     * 反序列化消息
     */
    <T> Message<T> deserialize(byte[] data, Class<T> payloadType);
}
```

### JSON 序列化实现

```java
@Component
public class JsonMessageSerializer implements MessageSerializer {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public byte[] serialize(Message<?> message) {
        try {
            return objectMapper.writeValueAsBytes(message);
        } catch (JsonProcessingException e) {
            throw new MessageSerializationException("消息序列化失败", e);
        }
    }
    
    @Override
    public <T> Message<T> deserialize(byte[] data, Class<T> payloadType) {
        try {
            JavaType type = objectMapper.getTypeFactory()
                .constructParametricType(Message.class, payloadType);
            return objectMapper.readValue(data, type);
        } catch (IOException e) {
            throw new MessageSerializationException("消息反序列化失败", e);
        }
    }
}
```

## 异常处理

### 异常层次结构

```
MessagingException (基础异常)
 MessageSendException (发送异常)
 MessageReceiveException (接收异常)
 MessageConnectionException (连接异常)
 MessageSerializationException (序列化异常)
```

### 异常处理示例

```java
@Service
public class RobustMessageService {
    
    @Autowired
    private MessageProducer messageProducer;
    
    public void sendMessageSafely(String topic, Object payload) {
        Message<Object> message = Message.of(topic, payload);
        
        try {
            messageProducer.send(topic, message);
        } catch (MessageSendException e) {
            log.error("发送消息失败: topic={}, error={}", topic, e.getMessage());
            
            // 保存到数据库，稍后重试
            saveFailedMessage(message);
        } catch (MessageConnectionException e) {
            log.error("消息服务连接失败: {}", e.getMessage());
            
            // 触发告警
            alertService.sendAlert("消息服务连接失败");
        }
    }
}
```

## 实现指南

### 实现自定义消息中间件支持

要为 Nebula 添加新的消息中间件支持（如 KafkaRocketMQ 等），需要实现以下接口：

1. 实现 `MessageProducer` 接口
2. 实现 `MessageConsumer` 接口
3. 实现 `MessageManager` 接口
4. 提供自动配置类

示例：

```java
// 1. 实现生产者
public class KafkaMessageProducer implements MessageProducer {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public <T> void send(String topic, Message<T> message) {
        kafkaTemplate.send(topic, message.getPayload());
    }
    
    // 实现其他方法...
}

// 2. 实现消费者
public class KafkaMessageConsumer implements MessageConsumer {
    
    private final ConsumerFactory<String, Object> consumerFactory;
    
    @Override
    public void subscribe(String topic) {
        // 创建 Kafka 消费者并订阅主题
    }
    
    // 实现其他方法...
}

// 3. 实现管理器
public class KafkaMessageManager implements MessageManager {
    
    private final KafkaMessageProducer producer;
    private final KafkaMessageConsumer consumer;
    
    @Override
    public MessageProducer getProducer() {
        return producer;
    }
    
    @Override
    public MessageConsumer getConsumer() {
        return consumer;
    }
    
    // 实现其他方法...
}

// 4. 自动配置
@Configuration
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaMessagingAutoConfiguration {
    
    @Bean
    public KafkaMessageProducer kafkaMessageProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        return new KafkaMessageProducer(kafkaTemplate);
    }
    
    @Bean
    public KafkaMessageConsumer kafkaMessageConsumer(ConsumerFactory<String, Object> consumerFactory) {
        return new KafkaMessageConsumer(consumerFactory);
    }
    
    @Bean
    public MessageManager messageManager(
            KafkaMessageProducer producer, 
            KafkaMessageConsumer consumer) {
        return new KafkaMessageManager(producer, consumer);
    }
}
```

## 最佳实践

### 1. 消息设计

- 消息载荷应该小而精，避免传输大对象
- 使用消息版本号，支持消息格式演进
- 重要消息设置持久化和确认机制
- 合理设置消息优先级和过期时间

### 2. 消费者设计

- 消费者应该幂等，能够处理重复消息
- 避免在消费者中执行长时间操作
- 合理设置并发消费线程数
- 实现优雅的错误处理和重试机制

### 3. 性能优化

- 使用批量发送减少网络开销
- 异步发送提高吞吐量
- 合理配置消息序列化方式
- 监控消息堆积情况

### 4. 可靠性

- 重要业务使用事务消息
- 实现消息确认机制
- 设置合理的重试策略
- 建立消息失败处理流程

## 依赖说明

```xml
<dependencies>
    <!-- Nebula Foundation -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-foundation</artifactId>
    </dependency>
    
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    
    <!-- JSON 处理 -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```

## 相关模块

- [nebula-messaging-rabbitmq](../nebula-messaging-rabbitmq/README.md) - RabbitMQ 实现
- [nebula-foundation](../../../core/nebula-foundation/README.md) - 基础工具

## 版本要求

- Java 21+
- Spring Boot 3.x
- Maven 3.6+

## 许可证

Apache License 2.0

---

**Nebula Messaging Core** - 构建可靠消息传递的基础


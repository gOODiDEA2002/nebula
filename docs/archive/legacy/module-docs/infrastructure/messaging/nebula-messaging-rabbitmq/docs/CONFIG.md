# Nebula Messaging RabbitMQ 配置指南

> RabbitMQ消息队列配置说明

## 概述

`nebula-messaging-rabbitmq` 提供 RabbitMQ 的集成支持,用于异步消息处理、系统解耦、流量削峰等场景。

## 基本配置

### Maven依赖

```xml
<dependency>
    <groupId>com.andy.nebula</groupId>
    <artifactId>nebula-messaging-rabbitmq</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### 单机配置

```yaml
nebula:
  messaging:
    rabbitmq:
      enabled: true
      host: localhost
      port: 5672
      username: ${RABBITMQ_USERNAME}
      password: ${RABBITMQ_PASSWORD}
      virtual-host: /ticket
```

### 集群配置

```yaml
nebula:
  messaging:
    rabbitmq:
      enabled: true
      addresses: rabbitmq1:5672,rabbitmq2:5672,rabbitmq3:5672
      username: ${RABBITMQ_USERNAME}
      password: ${RABBITMQ_PASSWORD}
      virtual-host: /ticket
```

## 生产者配置

```yaml
nebula:
  messaging:
    rabbitmq:
      producer:
        # 发送确认
        publisher-confirms: true
        # 返回确认
        publisher-returns: true
        # 强制回调
        mandatory: true
        # 连接超时
        connection-timeout: 15s
        # 重试次数
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 1s
          multiplier: 2
```

## 消费者配置

```yaml
nebula:
  messaging:
    rabbitmq:
      consumer:
        # 并发消费者数量
        concurrency: 5
        # 最大并发消费者数量
        max-concurrency: 10
        # 预取数量
        prefetch-count: 10
        # 自动确认
        acknowledge-mode: manual
        # 重试配置
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 1s
```

## 票务系统场景

### 订单创建消息

```yaml
nebula:
  messaging:
    rabbitmq:
      exchanges:
        - name: ticket.order
          type: topic
          durable: true
          auto-delete: false
      queues:
        # 订单创建队列
        - name: ticket.order.created
          durable: true
          exclusive: false
          auto-delete: false
          arguments:
            x-message-ttl: 300000  # 消息5分钟过期
            x-max-length: 100000   # 队列最大长度
        # 订单支付队列
        - name: ticket.order.paid
          durable: true
      bindings:
        - exchange: ticket.order
          queue: ticket.order.created
          routing-key: order.created
        - exchange: ticket.order
          queue: ticket.order.paid
          routing-key: order.paid
```

### 延迟消息(订单超时取消)

```yaml
nebula:
  messaging:
    rabbitmq:
      delay-message:
        enabled: true
        plugin-enabled: true  # 需要安装rabbitmq-delayed-message-exchange插件
      exchanges:
        - name: ticket.order.delay
          type: x-delayed-message
          arguments:
            x-delayed-type: topic
      queues:
        - name: ticket.order.timeout
          durable: true
      bindings:
        - exchange: ticket.order.delay
          queue: ticket.order.timeout
          routing-key: order.timeout
```

### 使用示例

```java
/**
 * 订单服务发送消息
 */
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final MessageProducer messageProducer;
    
    public OrderVO createOrder(CreateOrderDTO dto) {
        // 1. 创建订单
        Order order = new Order();
        // ... 设置订单信息
        orderRepository.save(order);
        
        // 2. 发送订单创建消息
        OrderCreatedEvent event = new OrderCreatedEvent(order);
        messageProducer.send("ticket.order", "order.created", event);
        
        // 3. 发送延迟消息(15分钟后检查订单状态)
        OrderTimeoutEvent timeoutEvent = new OrderTimeoutEvent(order.getOrderNo());
        messageProducer.sendDelayed(
            "ticket.order.delay",
            "order.timeout",
            timeoutEvent,
            Duration.ofMinutes(15)
        );
        
        return toVO(order);
    }
}

/**
 * 消息消费者
 */
@Component
public class OrderEventListener {
    
    /**
     * 处理订单创建事件
     */
    @MessageHandler(
        topic = "order.created",
        queue = "ticket.order.created"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("订单创建: orderNo={}", event.getOrderNo());
        
        // 1. 发送短信通知
        smsService.sendOrderCreated(event.getUserId(), event.getOrderNo());
        
        // 2. 记录操作日志
        operationLogService.log(event.getUserId(), "order.created", event.getOrderNo());
    }
    
    /**
     * 处理订单超时事件
     */
    @MessageHandler(
        topic = "order.timeout",
        queue = "ticket.order.timeout"
    )
    public void handleOrderTimeout(OrderTimeoutEvent event) {
        log.info("检查订单超时: orderNo={}", event.getOrderNo());
        
        Order order = orderRepository.findByOrderNo(event.getOrderNo())
            .orElse(null);
        
        if (order != null && "PENDING".equals(order.getStatus())) {
            // 订单未支付,自动取消
            orderService.cancelOrder(order.getOrderNo());
        }
    }
}
```

## 死信队列配置

```yaml
nebula:
  messaging:
    rabbitmq:
      exchanges:
        # 业务交换机
        - name: ticket.order
          type: topic
        # 死信交换机
        - name: ticket.order.dlx
          type: topic
      queues:
        # 业务队列(配置死信)
        - name: ticket.order.created
          durable: true
          arguments:
            x-dead-letter-exchange: ticket.order.dlx
            x-dead-letter-routing-key: order.created.failed
            x-message-ttl: 300000
        # 死信队列
        - name: ticket.order.created.dlq
          durable: true
      bindings:
        - exchange: ticket.order
          queue: ticket.order.created
          routing-key: order.created
        - exchange: ticket.order.dlx
          queue: ticket.order.created.dlq
          routing-key: order.created.failed
```

## 性能优化

### 批量发送

```java
@Service
public class NotificationService {
    
    @Autowired
    private MessageProducer messageProducer;
    
    public void batchSendNotifications(List<Notification> notifications) {
        List<Message> messages = notifications.stream()
            .map(n -> Message.builder()
                .topic("notification")
                .routingKey("notification.send")
                .payload(n)
                .build())
            .collect(Collectors.toList());
        
        // 批量发送
        messageProducer.sendBatch(messages);
    }
}
```

### 消费者性能配置

```yaml
nebula:
  messaging:
    rabbitmq:
      consumer:
        # 高并发场景
        concurrency: 20
        max-concurrency: 50
        prefetch-count: 50  # 提高预取数量
        # 手动确认,避免消息丢失
        acknowledge-mode: manual
```

## 环境配置

### 开发环境

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
      consumer:
        concurrency: 1
```

### 生产环境

```yaml
nebula:
  messaging:
    rabbitmq:
      enabled: true
      addresses: ${RABBITMQ_ADDRESSES}
      username: ${RABBITMQ_USERNAME}
      password: ${RABBITMQ_PASSWORD}
      virtual-host: /ticket-prod
      producer:
        publisher-confirms: true
        publisher-returns: true
        retry:
          enabled: true
          max-attempts: 3
      consumer:
        concurrency: 10
        max-concurrency: 50
        prefetch-count: 20
        acknowledge-mode: manual
```

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0


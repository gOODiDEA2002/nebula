# Nebula Messaging Redis - 使用示例

本文档提供 `nebula-messaging-redis` 的常用使用示例。

## 示例 1：Pub/Sub 发送与订阅

### 发送消息

```java
@Service
public class NotificationService {

    private final RedisMessageManager messageManager;

    public NotificationService(RedisMessageManager messageManager) {
        this.messageManager = messageManager;
    }

    public void sendNotification(Notification notification) {
        messageManager.publish("user:notification", notification);
    }
}
```

### 订阅消息（注解方式）

```java
@Component
public class NotificationHandler {

    @RedisMessageHandler(channel = "user:notification")
    public void handleNotification(Message<Notification> message) {
        Notification notification = message.getPayload();
        // 处理通知
    }
}
```

## 示例 2：Stream 可靠消息

### 发送消息

```java
@Service
public class OrderEventService {

    private final RedisMessageManager messageManager;

    public OrderEventService(RedisMessageManager messageManager) {
        this.messageManager = messageManager;
    }

    public void sendOrderCreated(OrderCreatedEvent event) {
        messageManager.send("order:events", event);
    }
}
```

### 消费消息

```java
@Component
public class OrderEventHandler {

    @RedisMessageHandler(stream = "order:events")
    public void handleOrderEvent(Message<OrderCreatedEvent> message) {
        OrderCreatedEvent event = message.getPayload();
        // 处理订单创建
    }
}
```

## 示例 3：票务场景（模块示例）

### 场景说明
订单支付成功后发送 Redis Stream 消息，异步生成电子票并发送通知。

```java
@Service
public class TicketOrderService {

    private final RedisMessageManager messageManager;

    public TicketOrderService(RedisMessageManager messageManager) {
        this.messageManager = messageManager;
    }

    public void handlePaymentSuccess(OrderPaidEvent event) {
        messageManager.send("ticket:paid", event);
    }
}

@Component
public class TicketPaidHandler {

    @RedisMessageHandler(stream = "ticket:paid")
    public void onTicketPaid(Message<OrderPaidEvent> message) {
        // 生成电子票 -> 存储 -> 通知
    }
}
```

## 最佳实践

1. Pub/Sub 用于实时通知，Stream 用于可靠消息。
2. 对重要 Stream 主题设置合理的 `max-len` 以控制内存。
3. 同一业务场景建议统一使用 Stream，避免重复消费。

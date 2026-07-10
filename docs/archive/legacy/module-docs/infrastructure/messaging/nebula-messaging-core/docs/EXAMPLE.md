# nebula-messaging-core 模块示例

## 模块简介

`nebula-messaging-core` 模块定义了 Nebula 框架的消息驱动核心抽象。它提供了一套统一的 API，用于发送和接收消息，屏蔽了底层具体消息中间件（如 RabbitMQ, Kafka, RocketMQ）的差异。

核心组件包括：
- **MessageProducer**: 统一的消息发送接口。
- **@MessageHandler**: 声明式的消息接收注解。
- **Message**: 统一的消息模型。

## 核心功能示例

### 1. 定义消息模型

消息通常承载一个业务对象（Payload）。建议定义 POJO 类作为消息载荷。

**`io.nebula.example.messaging.event.OrderCreatedEvent`**:

```java
package io.nebula.example.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单创建事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {
    private String orderId;
    private Long userId;
    private BigDecimal amount;
    private LocalDateTime createTime;
}
```

### 2. 发送消息 (Producer)

在业务服务中注入 `MessageProducer` 接口，即可发送消息。

**`io.nebula.example.messaging.service.OrderService`**:

```java
package io.nebula.example.messaging.service;

import io.nebula.example.messaging.event.OrderCreatedEvent;
import io.nebula.messaging.core.message.Message;
import io.nebula.messaging.core.producer.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    // 注入泛型 MessageProducer，或者使用 MessageProducer<Object>
    private final MessageProducer<Object> messageProducer;

    public void createOrder(Long userId, BigDecimal amount) {
        String orderId = UUID.randomUUID().toString();
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, userId, amount, LocalDateTime.now());

        // 1. 发送普通消息
        // topic: "order.events"
        messageProducer.send("order.events", event);
        log.info("订单创建事件已发送: {}", orderId);

        // 2. 发送带 Key 的消息 (用于顺序消费或分区)
        // shardKey: orderId
        messageProducer.sendOrderedMessage("order.events.partitioned", event, orderId);
        
        // 3. 发送延时消息 (例如 15 分钟后检查支付状态)
        messageProducer.sendDelayMessage("order.check.payment", event, Duration.ofMinutes(15));
        
        // 4. 异步发送
        CompletableFuture<MessageProducer.SendResult> future = messageProducer.sendAsync("order.events.async", event);
        future.thenAccept(result -> {
            if (result.isSuccess()) {
                log.info("异步发送成功, ID: {}", result.getMessageId());
            } else {
                log.error("异步发送失败", result.getException());
            }
        });
    }
}
```

### 3. 接收消息 (Consumer)

使用 `@MessageHandler` 注解标注在 Spring Bean 的方法上，即可监听并处理消息。

**`io.nebula.example.messaging.listener.OrderEventListener`**:

```java
package io.nebula.example.messaging.listener;

import io.nebula.example.messaging.event.OrderCreatedEvent;
import io.nebula.messaging.core.annotation.MessageHandler;
import io.nebula.messaging.core.message.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventListener {

    /**
     * 监听 "order.events" 主题
     * 方法参数可以是 Message<T> 或直接是 Payload T
     */
    @MessageHandler(topic = "order.events", consumerGroup = "order-group")
    public void handleOrderCreated(Message<OrderCreatedEvent> message) {
        OrderCreatedEvent event = message.getPayload();
        log.info("收到订单创建事件: orderId={}, userId={}", event.getOrderId(), event.getUserId());
        
        // 处理业务逻辑...
    }

    /**
     * 监听顺序消息队列
     * concurrency = 1 保证单线程消费
     */
    @MessageHandler(topic = "order.events.partitioned", concurrency = 1)
    public void handleOrderedEvent(OrderCreatedEvent event) {
        log.info("处理顺序事件: {}", event.getOrderId());
    }
    
    /**
     * 监听延时消息
     */
    @MessageHandler(topic = "order.check.payment")
    public void checkPayment(OrderCreatedEvent event) {
        log.info("检查订单支付状态 (延时触发): {}", event.getOrderId());
    }
}
```

## 进阶用法

### 1. 事务消息

`MessageProducer` 支持发送事务消息，确保本地事务执行成功后才发送消息。

```java
messageProducer.sendTransactionMessage("order.tx", event, (msg) -> {
    // 执行本地事务
    try {
        // saveOrder(event);
        return MessageProducer.TransactionResult.COMMIT;
    } catch (Exception e) {
        return MessageProducer.TransactionResult.ROLLBACK;
    }
});
```

### 2. 批量发送

```java
List<OrderCreatedEvent> events = new ArrayList<>();
// ... 添加事件
messageProducer.sendBatch("order.events.batch", events);
```

## 总结

`nebula-messaging-core` 提供了标准化的消息编程模型。具体的行为（如消息如何传输、延时如何实现）取决于引入的具体实现模块（如 `nebula-messaging-rabbitmq`）。


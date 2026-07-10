# Nebula Messaging RabbitMQ - 使用示例

> RabbitMQ消息队列完整使用指南，以票务系统为例

## 目录

- [快速开始](#快速开始)
- [基础消息发送](#基础消息发送)
- [消息消费](#消息消费)
- [延迟消息](#延迟消息)
- [死信队列](#死信队列)
- [消息重试](#消息重试)
- [事务消息](#事务消息)
- [批量发送](#批量发送)
- [消息确认](#消息确认)
- [票务系统完整示例](#票务系统完整示例)
- [最佳实践](#最佳实践)

---

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-messaging-rabbitmq</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 基础配置

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /

nebula:
  messaging:
    rabbitmq:
      enabled: true
      
      # 生产者配置
      producer:
        publisher-confirms: true    # 开启发送确认
        publisher-returns: true     # 开启消息回退
        confirm-timeout: 5000       # 确认超时时间(ms)
      
      # 消费者配置
      consumer:
        auto-ack: false            # 手动ACK
        prefetch-count: 10         # 预取数量
        retry-count: 3             # 消费失败重试次数
        retry-interval: 2000       # 重试间隔(ms)
      
      # 延时消息配置
      delay-message:
        enabled: true
        auto-create-resources: true
        max-delay-millis: 604800000  # 最大延时7天
```

### 启用消息功能

```java
@SpringBootApplication
@EnableMessaging  // 启用消息功能
public class TicketingApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketingApplication.class, args);
    }
}
```

---

## 基础消息发送

### 1. 发送简单消息

```java
/**
 * 基础消息发送示例
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationService {
    
    private final MessageProducer messageProducer;
    
    /**
     * 发送订单创建消息
     */
    public void sendOrderCreatedMessage(Order order) {
        // 创建消息
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderNo(order.getOrderNo());
        event.setUserId(order.getUserId());
        event.setShowtimeId(order.getShowtimeId());
        event.setTotalAmount(order.getTotalAmount());
        event.setCreateTime(order.getCreateTime());
        
        // 发送消息到 order.created 主题
        messageProducer.send("order.created", event);
        
        log.info("订单创建消息已发送：orderNo={}", order.getOrderNo());
    }
    
    /**
     * 发送订单支付成功消息
     */
    public void sendOrderPaidMessage(String orderNo, LocalDateTime payTime) {
        OrderPaidEvent event = new OrderPaidEvent();
        event.setOrderNo(orderNo);
        event.setPayTime(payTime);
        
        messageProducer.send("order.paid", event);
        
        log.info("订单支付消息已发送：orderNo={}", orderNo);
    }
}

/**
 * 订单创建事件
 */
@Data
public class OrderCreatedEvent implements Serializable {
    private String orderNo;
    private Long userId;
    private Long showtimeId;
    private BigDecimal totalAmount;
    private LocalDateTime createTime;
}

/**
 * 订单支付事件
 */
@Data
public class OrderPaidEvent implements Serializable {
    private String orderNo;
    private LocalDateTime payTime;
}
```

### 2. 发送消息到指定队列

```java
/**
 * 发送消息到指定队列
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketGenerationService {
    
    private final MessageProducer messageProducer;
    
    /**
     * 发送生成电子票任务
     */
    public void sendTicketGenerationTask(String orderNo, List<String> seatNos) {
        TicketGenerationTask task = new TicketGenerationTask();
        task.setOrderNo(orderNo);
        task.setSeatNos(seatNos);
        task.setTaskTime(LocalDateTime.now());
        
        // 直接发送到队列（不通过交换机）
        messageProducer.sendToQueue("ticket.generation.queue", task);
        
        log.info("电子票生成任务已发送：orderNo={}", orderNo);
    }
}

/**
 * 电子票生成任务
 */
@Data
public class TicketGenerationTask implements Serializable {
    private String orderNo;
    private List<String> seatNos;
    private LocalDateTime taskTime;
}
```

---

## 消息消费

### 1. 基础消息消费

```java
/**
 * 订单消息消费者
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderMessageConsumer {
    
    private final TicketService ticketService;
    private final NotificationService notificationService;
    
    /**
     * 消费订单创建消息
     */
    @MessageHandler(topic = "order.created")
    public void handleOrderCreated(Message<OrderCreatedEvent> message) {
        OrderCreatedEvent event = message.getPayload();
        
        log.info("接收到订单创建消息：orderNo={}, messageId={}", 
                event.getOrderNo(), message.getId());
        
        // 业务处理：记录日志
        logService.log("订单已创建", event);
        
        log.info("订单创建消息处理完成：orderNo={}", event.getOrderNo());
    }
    
    /**
     * 消费订单支付成功消息
     */
    @MessageHandler(topic = "order.paid")
    public void handleOrderPaid(Message<OrderPaidEvent> message) {
        OrderPaidEvent event = message.getPayload();
        
        log.info("接收到订单支付消息：orderNo={}", event.getOrderNo());
        
        // 1. 生成电子票
        List<String> ticketNos = ticketService.generateTickets(event.getOrderNo());
        
        // 2. 发送通知
        notificationService.sendOrderPaidNotification(event.getOrderNo());
        
        log.info("订单支付消息处理完成：orderNo={}, 生成{}张电子票", 
                event.getOrderNo(), ticketNos.size());
    }
}
```

### 2. 消费指定队列消息

```java
/**
 * 电子票生成消费者
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TicketGenerationConsumer {
    
    private final TicketService ticketService;
    private final QRCodeService qrCodeService;
    private final StorageService storageService;
    
    /**
     * 消费电子票生成任务（监听队列）
     */
    @MessageHandler(queue = "ticket.generation.queue")
    public void handleTicketGeneration(Message<TicketGenerationTask> message) {
        TicketGenerationTask task = message.getPayload();
        
        log.info("接收到电子票生成任务：orderNo={}, 座位数={}", 
                task.getOrderNo(), task.getSeatNos().size());
        
        // 1. 批量生成电子票
        List<Ticket> tickets = ticketService.batchGenerateTickets(
                task.getOrderNo(), task.getSeatNos());
        
        // 2. 为每张票生成二维码
        for (Ticket ticket : tickets) {
            byte[] qrCode = qrCodeService.generateQRCode(ticket.getTicketNo());
            
            // 3. 上传二维码到存储服务
            String qrCodeUrl = storageService.upload(
                    "tickets/qr/" + ticket.getTicketNo() + ".png", qrCode);
            
            // 4. 更新电子票二维码URL
            ticket.setQrCodeUrl(qrCodeUrl);
            ticketService.updateTicket(ticket);
        }
        
        log.info("电子票生成完成：orderNo={}, 共{}张", 
                task.getOrderNo(), tickets.size());
    }
}
```

---

## 延迟消息

### 1. 发送延迟消息

```java
/**
 * 延迟消息示例
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderTimeoutService {
    
    private final DelayMessageProducer delayMessageProducer;
    
    /**
     * 订单创建时发送延迟取消消息
     * 30分钟后如果订单未支付，自动取消
     */
    public void scheduleOrderCancellation(String orderNo) {
        OrderCancellationTask task = new OrderCancellationTask();
        task.setOrderNo(orderNo);
        task.setScheduleTime(LocalDateTime.now());
        
        // 发送延迟消息，30分钟后投递
        delayMessageProducer.sendDelayMessage(
                "order.cancellation", 
                task, 
                Duration.ofMinutes(30)
        );
        
        log.info("订单取消延迟消息已发送：orderNo={}, 将在30分钟后投递", orderNo);
    }
    
    /**
     * 发送演出开始前的提醒
     */
    public void scheduleShowtimeReminder(Long userId, Long showtimeId, LocalDateTime showTime) {
        ShowtimeReminderTask task = new ShowtimeReminderTask();
        task.setUserId(userId);
        task.setShowtimeId(showtimeId);
        task.setShowTime(showTime);
        
        // 演出开始前2小时提醒
        LocalDateTime reminderTime = showTime.minusHours(2);
        long delaySeconds = Duration.between(LocalDateTime.now(), reminderTime).getSeconds();
        
        if (delaySeconds > 0) {
            delayMessageProducer.sendDelayMessage(
                    "showtime.reminder",
                    task,
                    Duration.ofSeconds(delaySeconds)
            );
            
            log.info("演出提醒已安排：userId={}, showtimeId={}, 提醒时间={}", 
                    userId, showtimeId, reminderTime);
        }
    }
}

/**
 * 订单取消任务
 */
@Data
public class OrderCancellationTask implements Serializable {
    private String orderNo;
    private LocalDateTime scheduleTime;
}

/**
 * 演出提醒任务
 */
@Data
public class ShowtimeReminderTask implements Serializable {
    private Long userId;
    private Long showtimeId;
    private LocalDateTime showTime;
}
```

### 2. 消费延迟消息

```java
/**
 * 延迟消息消费者
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DelayMessageConsumer {
    
    private final OrderService orderService;
    private final NotificationService notificationService;
    
    /**
     * 处理订单自动取消
     */
    @DelayMessageListener(topic = "order.cancellation")
    public void handleOrderCancellation(DelayMessage<OrderCancellationTask> message) {
        OrderCancellationTask task = message.getPayload();
        
        log.info("处理订单自动取消：orderNo={}", task.getOrderNo());
        
        // 1. 查询订单状态
        Order order = orderService.getOrderByOrderNo(task.getOrderNo());
        
        // 2. 如果订单仍未支付，则取消
        if ("PENDING".equals(order.getStatus())) {
            orderService.cancelOrder(task.getOrderNo(), "超时未支付，系统自动取消");
            log.info("订单已自动取消：orderNo={}", task.getOrderNo());
        } else {
            log.info("订单状态非待支付，跳过取消：orderNo={}, status={}", 
                    task.getOrderNo(), order.getStatus());
        }
    }
    
    /**
     * 处理演出提醒
     */
    @DelayMessageListener(topic = "showtime.reminder")
    public void handleShowtimeReminder(DelayMessage<ShowtimeReminderTask> message) {
        ShowtimeReminderTask task = message.getPayload();
        
        log.info("发送演出提醒：userId={}, showtimeId={}", 
                task.getUserId(), task.getShowtimeId());
        
        // 发送提醒通知
        notificationService.sendShowtimeReminder(
                task.getUserId(), 
                task.getShowtimeId(), 
                task.getShowTime()
        );
        
        log.info("演出提醒已发送");
    }
}
```

---

## 死信队列

### 1. 配置死信队列

```java
/**
 * 死信队列配置
 */
@Configuration
public class DeadLetterQueueConfig {
    
    /**
     * 定义死信交换机
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("dlx.exchange", true, false);
    }
    
    /**
     * 定义死信队列
     */
    @Bean
    public Queue deadLetterQueue() {
        return new Queue("dlx.queue", true);
    }
    
    /**
     * 绑定死信队列到死信交换机
     */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("dlx");
    }
}
```

### 2. 消费死信消息

```java
/**
 * 死信队列消费者
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeadLetterConsumer {
    
    private final AlertService alertService;
    private final ErrorLogService errorLogService;
    
    /**
     * 处理死信消息
     */
    @MessageHandler(queue = "dlx.queue")
    public void handleDeadLetter(Message<Object> message) {
        log.error("收到死信消息：messageId={}, topic={}, payload={}", 
                message.getId(), message.getTopic(), message.getPayload());
        
        // 1. 记录错误日志
        errorLogService.logDeadLetter(message);
        
        // 2. 发送告警
        alertService.sendAlert(
                "死信消息警告",
                String.format("消息处理失败，已进入死信队列。MessageID: %s, Topic: %s", 
                        message.getId(), message.getTopic())
        );
        
        // 3. 根据消息类型进行特殊处理
        if ("order.paid".equals(message.getTopic())) {
            // 订单支付消息进入死信，需要人工处理
            handleOrderPaidDeadLetter(message);
        }
    }
    
    /**
     * 处理订单支付消息死信
     */
    private void handleOrderPaidDeadLetter(Message<Object> message) {
        log.error("订单支付消息处理失败，进入死信队列：{}", message);
        
        // 创建人工处理工单
        alertService.createManualTask(
                "订单支付消息处理失败",
                "需要人工检查订单状态并重新处理",
                message
        );
    }
}
```

---

## 消息重试

### 1. 自动重试配置

```yaml
nebula:
  messaging:
    rabbitmq:
      consumer:
        retry-count: 3           # 重试3次
        retry-interval: 2000     # 每次重试间隔2秒
```

### 2. 自定义重试策略

```java
/**
 * 自定义重试策略
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderMessageConsumerWithRetry {
    
    private final OrderService orderService;
    
    /**
     * 处理订单消息（带重试）
     */
    @MessageHandler(
            topic = "order.process",
            retryCount = 5,           // 自定义重试次数
            retryInterval = 3000      // 自定义重试间隔（毫秒）
    )
    public void handleOrderProcess(Message<OrderProcessTask> message) {
        OrderProcessTask task = message.getPayload();
        
        // 获取当前重试次数
        int retryCount = message.getHeaders().getOrDefault("x-retry-count", 0);
        
        log.info("处理订单任务：orderNo={}, 重试次数={}", 
                task.getOrderNo(), retryCount);
        
        try {
            // 业务处理
            orderService.processOrder(task.getOrderNo());
            
        } catch (Exception e) {
            log.error("订单处理失败：orderNo={}, 重试次数={}", 
                    task.getOrderNo(), retryCount, e);
            
            // 抛出异常触发重试
            throw e;
        }
    }
    
    /**
     * 处理订单消息（不重试）
     */
    @MessageHandler(
            topic = "order.notification",
            retryCount = 0  // 不重试，失败直接进入死信队列
    )
    public void handleOrderNotification(Message<OrderNotificationTask> message) {
        OrderNotificationTask task = message.getPayload();
        
        log.info("发送订单通知：orderNo={}", task.getOrderNo());
        
        // 通知发送失败不重试，避免重复通知用户
        notificationService.send(task);
    }
}
```

---

## 事务消息

### 1. 发送事务消息

```java
/**
 * 事务消息示例
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderTransactionService {
    
    private final RabbitTemplate rabbitTemplate;
    private final OrderService orderService;
    private final OrderMapper orderMapper;
    
    /**
     * 创建订单并发送消息（事务一致性）
     */
    @Transactional(rollbackFor = Exception.class)
    public String createOrderWithMessage(CreateOrderRequest request) {
        // 1. 创建订单（数据库操作）
        String orderNo = orderService.createOrder(
                request.getUserId(),
                request.getShowtimeId(),
                request.getQuantity(),
                request.getSeats()
        );
        
        // 2. 发送消息（在事务提交后发送）
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        // 数据库事务提交成功后，发送消息
                        OrderCreatedEvent event = new OrderCreatedEvent();
                        event.setOrderNo(orderNo);
                        event.setUserId(request.getUserId());
                        
                        rabbitTemplate.convertAndSend("order.created", event);
                        
                        log.info("订单创建消息已发送：orderNo={}", orderNo);
                    }
                }
        );
        
        log.info("订单创建成功：orderNo={}", orderNo);
        
        return orderNo;
    }
}
```

### 2. 本地消息表方案

```java
/**
 * 本地消息表方案（更可靠的事务消息）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReliableMessageService {
    
    private final MessageProducer messageProducer;
    private final OutboxMessageMapper outboxMessageMapper;
    
    /**
     * 保存消息到本地消息表
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveOrderAndMessage(Order order) {
        // 1. 保存订单
        orderMapper.insert(order);
        
        // 2. 保存消息到本地消息表
        OutboxMessage outboxMessage = new OutboxMessage();
        outboxMessage.setAggregateId(order.getId());
        outboxMessage.setAggregateType("Order");
        outboxMessage.setEventType("OrderCreated");
        outboxMessage.setPayload(JsonUtils.toJson(order));
        outboxMessage.setStatus("PENDING");
        outboxMessage.setCreateTime(LocalDateTime.now());
        
        outboxMessageMapper.insert(outboxMessage);
        
        log.info("订单和消息已保存：orderNo={}", order.getOrderNo());
    }
    
    /**
     * 定时任务：发送待发送的消息
     */
    @Scheduled(fixedRate = 5000)
    public void sendPendingMessages() {
        // 1. 查询待发送的消息
        List<OutboxMessage> pendingMessages = outboxMessageMapper.selectPendingMessages(100);
        
        if (pendingMessages.isEmpty()) {
            return;
        }
        
        log.info("发送待发送消息，数量：{}", pendingMessages.size());
        
        // 2. 逐条发送
        for (OutboxMessage message : pendingMessages) {
            try {
                // 发送消息
                messageProducer.send(message.getEventType().toLowerCase(), message.getPayload());
                
                // 3. 更新消息状态为已发送
                message.setStatus("SENT");
                message.setSendTime(LocalDateTime.now());
                outboxMessageMapper.updateById(message);
                
                log.info("消息发送成功：id={}, eventType={}", 
                        message.getId(), message.getEventType());
            } catch (Exception e) {
                log.error("消息发送失败：id={}, eventType={}", 
                        message.getId(), message.getEventType(), e);
                
                // 更新重试次数
                message.setRetryCount(message.getRetryCount() + 1);
                outboxMessageMapper.updateById(message);
            }
        }
    }
}

/**
 * 本地消息表
 */
@Data
@TableName("t_outbox_message")
public class OutboxMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long aggregateId;
    private String aggregateType;
    private String eventType;
    private String payload;
    private String status;  // PENDING, SENT, FAILED
    private Integer retryCount;
    private LocalDateTime createTime;
    private LocalDateTime sendTime;
}
```

---

## 批量发送

```java
/**
 * 批量发送消息
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchMessageService {
    
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * 批量发送订单消息
     */
    public void batchSendOrderMessages(List<Order> orders) {
        log.info("批量发送订单消息，数量：{}", orders.size());
        
        // 使用批量发送提高性能
        rabbitTemplate.execute(channel -> {
            // 开启发送方确认（批量）
            channel.confirmSelect();
            
            for (Order order : orders) {
                OrderCreatedEvent event = new OrderCreatedEvent();
                event.setOrderNo(order.getOrderNo());
                event.setUserId(order.getUserId());
                
                // 发送消息
                byte[] messageBody = SerializationUtils.serialize(event);
                channel.basicPublish(
                        "order.exchange",
                        "order.created",
                        MessageProperties.PERSISTENT_TEXT_PLAIN,
                        messageBody
                );
            }
            
            // 等待批量确认
            channel.waitForConfirmsOrDie(5000);
            
            log.info("批量消息发送成功，数量：{}", orders.size());
            
            return null;
        });
    }
}
```

---

## 消息确认

### 1. 发送确认

```java
/**
 * 发送确认回调
 */
@Configuration
public class ProducerConfirmConfig {
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        
        // 设置发送确认回调
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("消息发送成功：correlationId={}", 
                        correlationData != null ? correlationData.getId() : "unknown");
            } else {
                log.error("消息发送失败：correlationId={}, 原因：{}", 
                        correlationData != null ? correlationData.getId() : "unknown", cause);
            }
        });
        
        // 设置消息回退回调（消息无法路由时触发）
        template.setReturnsCallback(returned -> {
            log.error("消息无法路由：exchange={}, routingKey={}, replyText={}", 
                    returned.getExchange(), returned.getRoutingKey(), returned.getReplyText());
        });
        
        return template;
    }
}
```

### 2. 消费确认

```java
/**
 * 手动确认消息
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ManualAckConsumer {
    
    /**
     * 手动确认消息（成功）
     */
    @RabbitListener(queues = "order.process.queue")
    public void handleOrderProcess(
            Message<OrderProcessTask> message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        
        try {
            OrderProcessTask task = message.getPayload();
            
            log.info("处理订单：orderNo={}", task.getOrderNo());
            
            // 业务处理
            processOrder(task);
            
            // 手动确认（ACK）
            channel.basicAck(deliveryTag, false);
            
            log.info("订单处理成功，消息已确认：orderNo={}", task.getOrderNo());
        } catch (Exception e) {
            log.error("订单处理失败", e);
            
            // 手动拒绝（NACK）并重新入队
            channel.basicNack(deliveryTag, false, true);
        }
    }
    
    /**
     * 拒绝消息（不重新入队）
     */
    @RabbitListener(queues = "invalid.message.queue")
    public void handleInvalidMessage(
            Message<Object> message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        
        try {
            log.warn("收到无效消息：{}", message.getPayload());
            
            // 业务处理
            processInvalidMessage(message);
            
            // 确认消息
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("无效消息处理失败", e);
            
            // 拒绝消息，不重新入队（进入死信队列）
            channel.basicReject(deliveryTag, false);
        }
    }
    
    private void processOrder(OrderProcessTask task) {
        // 业务逻辑
    }
    
    private void processInvalidMessage(Message<Object> message) {
        // 业务逻辑
    }
}
```

---

## 票务系统完整示例

### 完整的购票流程消息处理

```java
/**
 * 票务购买服务（完整消息流）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketPurchaseMessageService {
    
    private final MessageProducer messageProducer;
    private final DelayMessageProducer delayMessageProducer;
    private final OrderService orderService;
    private final TicketService ticketService;
    private final NotificationService notificationService;
    
    /**
     * 1. 创建订单后发送消息
     */
    @Transactional(rollbackFor = Exception.class)
    public String createOrderAndPublishEvents(CreateOrderRequest request) {
        // 1.1 创建订单
        String orderNo = orderService.createOrder(request);
        Order order = orderService.getOrderByOrderNo(orderNo);
        
        // 1.2 发送订单创建事件（立即）
        publishOrderCreatedEvent(order);
        
        // 1.3 发送延迟取消消息（30分钟后）
        scheduleOrderCancellation(orderNo);
        
        log.info("订单创建完成，消息已发送：orderNo={}", orderNo);
        
        return orderNo;
    }
    
    /**
     * 2. 订单支付后发送消息
     */
    @Transactional(rollbackFor = Exception.class)
    public void payOrderAndPublishEvents(String orderNo) {
        // 2.1 更新订单状态
        boolean success = orderService.markOrderPaid(orderNo);
        if (!success) {
            throw new BusinessException("订单状态更新失败");
        }
        
        Order order = orderService.getOrderByOrderNo(orderNo);
        
        // 2.2 发送订单支付事件（触发电子票生成）
        publishOrderPaidEvent(order);
        
        // 2.3 发送演出提醒任务（延迟消息）
        scheduleShowtimeReminder(order);
        
        log.info("订单支付完成，消息已发送：orderNo={}", orderNo);
    }
    
    /**
     * 发送订单创建事件
     */
    private void publishOrderCreatedEvent(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderNo(order.getOrderNo());
        event.setUserId(order.getUserId());
        event.setShowtimeId(order.getShowtimeId());
        event.setTotalAmount(order.getTotalAmount());
        event.setQuantity(order.getQuantity());
        event.setSeats(order.getSeats());
        event.setCreateTime(order.getCreateTime());
        
        messageProducer.send("order.created", event);
        
        log.info("订单创建事件已发送：orderNo={}", order.getOrderNo());
    }
    
    /**
     * 发送订单支付事件
     */
    private void publishOrderPaidEvent(Order order) {
        OrderPaidEvent event = new OrderPaidEvent();
        event.setOrderNo(order.getOrderNo());
        event.setUserId(order.getUserId());
        event.setShowtimeId(order.getShowtimeId());
        event.setPayTime(order.getPayTime());
        
        messageProducer.send("order.paid", event);
        
        log.info("订单支付事件已发送：orderNo={}", order.getOrderNo());
    }
    
    /**
     * 安排订单自动取消
     */
    private void scheduleOrderCancellation(String orderNo) {
        OrderCancellationTask task = new OrderCancellationTask();
        task.setOrderNo(orderNo);
        task.setScheduleTime(LocalDateTime.now());
        
        // 30分钟后自动取消未支付订单
        delayMessageProducer.sendDelayMessage(
                "order.cancellation",
                task,
                Duration.ofMinutes(30)
        );
        
        log.info("订单自动取消任务已安排：orderNo={}", orderNo);
    }
    
    /**
     * 安排演出提醒
     */
    private void scheduleShowtimeReminder(Order order) {
        Showtime showtime = showtimeService.getById(order.getShowtimeId());
        
        ShowtimeReminderTask task = new ShowtimeReminderTask();
        task.setUserId(order.getUserId());
        task.setShowtimeId(order.getShowtimeId());
        task.setOrderNo(order.getOrderNo());
        task.setShowTime(showtime.getShowTime());
        
        // 演出开始前2小时提醒
        LocalDateTime reminderTime = showtime.getShowTime().minusHours(2);
        long delaySeconds = Duration.between(LocalDateTime.now(), reminderTime).getSeconds();
        
        if (delaySeconds > 0) {
            delayMessageProducer.sendDelayMessage(
                    "showtime.reminder",
                    task,
                    Duration.ofSeconds(delaySeconds)
            );
            
            log.info("演出提醒已安排：orderNo={}, 提醒时间={}", order.getOrderNo(), reminderTime);
        }
    }
}

/**
 * 完整的消息消费处理
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TicketPurchaseMessageConsumer {
    
    private final OrderLogService orderLogService;
    private final TicketService ticketService;
    private final NotificationService notificationService;
    private final OrderService orderService;
    
    /**
     * 1. 处理订单创建事件（记录日志）
     */
    @MessageHandler(topic = "order.created")
    public void handleOrderCreated(Message<OrderCreatedEvent> message) {
        OrderCreatedEvent event = message.getPayload();
        
        log.info("处理订单创建事件：orderNo={}", event.getOrderNo());
        
        // 记录订单日志
        orderLogService.log(event.getOrderNo(), "ORDER_CREATED", "订单已创建");
        
        // 发送创建通知（可选）
        notificationService.sendOrderCreatedNotification(event.getUserId(), event.getOrderNo());
    }
    
    /**
     * 2. 处理订单支付事件（生成电子票）
     */
    @MessageHandler(topic = "order.paid", retryCount = 5, retryInterval = 3000)
    public void handleOrderPaid(Message<OrderPaidEvent> message) {
        OrderPaidEvent event = message.getPayload();
        
        log.info("处理订单支付事件：orderNo={}", event.getOrderNo());
        
        // 1. 生成电子票
        Order order = orderService.getOrderByOrderNo(event.getOrderNo());
        List<String> ticketNos = ticketService.batchGenerateTickets(order);
        
        // 2. 记录日志
        orderLogService.log(event.getOrderNo(), "ORDER_PAID", 
                String.format("订单已支付，生成%d张电子票", ticketNos.size()));
        
        // 3. 发送支付成功通知
        notificationService.sendOrderPaidNotification(
                event.getUserId(), event.getOrderNo(), ticketNos);
        
        log.info("订单支付事件处理完成：orderNo={}", event.getOrderNo());
    }
    
    /**
     * 3. 处理订单自动取消（延迟消息）
     */
    @DelayMessageListener(topic = "order.cancellation")
    public void handleOrderCancellation(DelayMessage<OrderCancellationTask> message) {
        OrderCancellationTask task = message.getPayload();
        
        log.info("处理订单自动取消：orderNo={}", task.getOrderNo());
        
        // 1. 检查订单状态
        Order order = orderService.getOrderByOrderNo(task.getOrderNo());
        
        if ("PENDING".equals(order.getStatus())) {
            // 2. 取消订单
            orderService.cancelOrder(task.getOrderNo(), "超时未支付，系统自动取消");
            
            // 3. 释放座位
            seatService.releaseSeats(order.getShowtimeId(), 
                    Arrays.asList(order.getSeats().split(",")));
            
            // 4. 恢复库存
            showtimeService.restoreStock(order.getShowtimeId(), order.getQuantity());
            
            // 5. 发送取消通知
            notificationService.sendOrderCancelledNotification(
                    order.getUserId(), order.getOrderNo(), "超时未支付");
            
            log.info("订单已自动取消：orderNo={}", task.getOrderNo());
        } else {
            log.info("订单状态非待支付，跳过取消：orderNo={}, status={}", 
                    task.getOrderNo(), order.getStatus());
        }
    }
    
    /**
     * 4. 处理演出提醒（延迟消息）
     */
    @DelayMessageListener(topic = "showtime.reminder")
    public void handleShowtimeReminder(DelayMessage<ShowtimeReminderTask> message) {
        ShowtimeReminderTask task = message.getPayload();
        
        log.info("发送演出提醒：userId={}, showtimeId={}, orderNo={}", 
                task.getUserId(), task.getShowtimeId(), task.getOrderNo());
        
        // 发送演出提醒通知
        notificationService.sendShowtimeReminder(
                task.getUserId(),
                task.getShowtimeId(),
                task.getOrderNo(),
                task.getShowTime()
        );
        
        log.info("演出提醒已发送");
    }
}
```

---

## 最佳实践

### 1. 消息幂等性

```java
/**
 * 消息幂等性处理
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotentMessageConsumer {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final OrderService orderService;
    
    /**
     * 幂等消费订单支付消息
     */
    @MessageHandler(topic = "order.paid")
    public void handleOrderPaid(Message<OrderPaidEvent> message) {
        OrderPaidEvent event = message.getPayload();
        String messageId = message.getId();
        
        // 1. 检查消息是否已处理（使用Redis）
        String idempotentKey = "msg:processed:" + messageId;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(
                idempotentKey, "1", Duration.ofDays(7));
        
        if (Boolean.FALSE.equals(isNew)) {
            log.info("消息已处理，跳过：messageId={}, orderNo={}", 
                    messageId, event.getOrderNo());
            return;
        }
        
        try {
            // 2. 处理业务逻辑
            log.info("处理订单支付消息：messageId={}, orderNo={}", 
                    messageId, event.getOrderNo());
            
            ticketService.generateTickets(event.getOrderNo());
            
            log.info("订单支付消息处理完成：orderNo={}", event.getOrderNo());
        } catch (Exception e) {
            // 3. 处理失败，删除幂等键，允许重试
            redisTemplate.delete(idempotentKey);
            throw e;
        }
    }
}
```

### 2. 消息顺序性

```java
/**
 * 保证消息顺序性
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderedMessageService {
    
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * 发送有序消息（同一订单的消息使用相同的routing key）
     */
    public void sendOrderedMessage(String orderNo, String eventType, Object payload) {
        // 使用订单号作为routing key的一部分，确保同一订单的消息路由到同一队列
        String routingKey = eventType + "." + (orderNo.hashCode() % 10);
        
        rabbitTemplate.convertAndSend("order.exchange", routingKey, payload);
        
        log.info("有序消息已发送：orderNo={}, eventType={}, routingKey={}", 
                orderNo, eventType, routingKey);
    }
}
```

### 3. 消息监控

```java
/**
 * 消息监控
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageMonitor {
    
    private final MeterRegistry meterRegistry;
    
    /**
     * 记录消息发送指标
     */
    public void recordMessageSent(String topic) {
        meterRegistry.counter("message.sent", "topic", topic).increment();
    }
    
    /**
     * 记录消息消费指标
     */
    public void recordMessageConsumed(String topic, boolean success) {
        meterRegistry.counter("message.consumed", 
                "topic", topic, 
                "success", String.valueOf(success)
        ).increment();
    }
    
    /**
     * 记录消息处理时间
     */
    public void recordMessageProcessTime(String topic, long timeMs) {
        meterRegistry.timer("message.process.time", "topic", topic)
                .record(Duration.ofMillis(timeMs));
    }
}
```

### 4. 异常处理策略

- **业务异常**：重试后进入死信队列
- **系统异常**：立即重试，多次失败后进入死信队列
- **超时异常**：不重试，直接进入死信队列
- **非法消息**：不重试，记录日志后丢弃

### 5. 性能优化

- **批量发送**：减少网络开销
- **消息压缩**：减少带宽占用
- **预取数量**：根据消费能力调整（prefetch-count）
- **连接池**：复用连接，减少连接开销
- **延迟消息分级**：不同延迟时间使用不同队列

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [CONFIG.md](./CONFIG.md) - 配置指南
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 发展路线图

---

**最后更新**: 2025-11-20  
**文档版本**: v2.0

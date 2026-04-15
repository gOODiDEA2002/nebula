# Nebula Messaging RabbitMQ 功能测试指南

## 概述

这个指南详细介绍如何测试 Nebula 消息传递层的各种功能，包括消息发送消息消费批量操作异步处理消息统计等

## 启动应用

### 1. 启动 RabbitMQ

```bash
# 使用 Docker 启动 RabbitMQ
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=guest \
  -e RABBITMQ_DEFAULT_PASS=guest \
  rabbitmq:3-management

# 或使用项目提供的 docker-compose
cd nebula-middleware
docker-compose up -d rabbitmq
```

### 2. 访问 RabbitMQ 管理界面

浏览器访问：http://localhost:15672
- 用户名：guest
- 密码：guest

### 3. 启动应用

```bash
cd nebula-example
mvn spring-boot:run
```

应用启动后，访问：http://localhost:8000

## API 接口测试

### 1. 发送订单通知

#### 1.1 发送订单创建通知

```bash
curl -X POST http://localhost:8000/messaging/order/notification \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1001,
    "orderNo": "ORD20250108001",
    "userId": 100,
    "productName": "智能手机",
    "amount": 3999.00,
    "status": "CREATED",
    "notificationType": "ORDER_CREATED"
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "订单通知发送成功",
  "data": {
    "messageId": "MSG_1704672000000_123",
    "success": true,
    "elapsedTime": 15,
    "errorMessage": null
  },
  "success": true
}
```

**验证消息消费**：
查看应用日志，应该能看到消息处理器的日志：
```
收到订单通知: orderId=1001, orderNo=ORD20250108001, type=ORDER_CREATED, status=CREATED
订单通知处理完成: orderId=1001
```

#### 1.2 发送订单支付通知

```bash
curl -X POST http://localhost:8000/messaging/order/notification \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1001,
    "orderNo": "ORD20250108001",
    "userId": 100,
    "productName": "智能手机",
    "amount": 3999.00,
    "status": "PAID",
    "notificationType": "ORDER_PAID"
  }'
```

#### 1.3 发送订单完成通知

```bash
curl -X POST http://localhost:8000/messaging/order/notification \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1001,
    "orderNo": "ORD20250108001",
    "userId": 100,
    "productName": "智能手机",
    "amount": 3999.00,
    "status": "COMPLETED",
    "notificationType": "ORDER_COMPLETED"
  }'
```

### 2. 发送订单状态更新通知

#### 2.1 同步发送

```bash
curl -X POST http://localhost:8000/messaging/order/status-update \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1001,
    "orderNo": "ORD20250108001",
    "oldStatus": "CREATED",
    "newStatus": "PAID",
    "remark": "用户已完成支付",
    "async": false
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "订单状态更新通知发送成功",
  "data": {
    "messageId": "MSG_1704672100000_456",
    "success": true,
    "elapsedTime": 12,
    "errorMessage": null
  },
  "success": true
}
```

**验证消息消费**：
查看应用日志，应该能看到：
```
收到订单状态更新: orderId=1001, orderNo=ORD20250108001, CREATED -> PAID
订单状态更新处理完成: orderId=1001, newStatus=PAID
```

#### 2.2 异步发送

```bash
curl -X POST http://localhost:8000/messaging/order/status-update \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1002,
    "orderNo": "ORD20250108002",
    "oldStatus": "PAID",
    "newStatus": "SHIPPED",
    "remark": "订单已发货",
    "async": true
  }'
```

异步发送响应更快，立即返回：
```json
{
  "code": "SUCCESS",
  "message": "订单状态更新通知发送成功",
  "data": {
    "messageId": "ASYNC_1704672200000",
    "success": true,
    "elapsedTime": 2,
    "errorMessage": null
  },
  "success": true
}
```

### 3. 批量发送通知

#### 3.1 批量发送订单通知

```bash
curl -X POST http://localhost:8000/messaging/order/batch-notification \
  -H "Content-Type: application/json" \
  -d '{
    "notifications": [
      {
        "orderId": 2001,
        "orderNo": "ORD20250108101",
        "userId": 101,
        "productName": "笔记本电脑",
        "amount": 5999.00,
        "status": "CREATED"
      },
      {
        "orderId": 2002,
        "orderNo": "ORD20250108102",
        "userId": 102,
        "productName": "平板电脑",
        "amount": 2999.00,
        "status": "CREATED"
      },
      {
        "orderId": 2003,
        "orderNo": "ORD20250108103",
        "userId": 103,
        "productName": "智能手表",
        "amount": 1999.00,
        "status": "CREATED"
      }
    ]
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "批量通知发送完成: 总数=3, 成功=3, 失败=0",
  "data": {
    "totalCount": 3,
    "successCount": 3,
    "failedCount": 0,
    "elapsedTime": 125,
    "failedMessageIds": []
  },
  "success": true
}
```

#### 3.2 批量发送（大批量）

测试大批量发送性能：

```bash
curl -X POST http://localhost:8000/messaging/order/batch-notification \
  -H "Content-Type: application/json" \
  -d '{
    "notifications": [
      {
        "orderId": 3001,
        "orderNo": "ORD20250108201",
        "userId": 201,
        "productName": "商品A",
        "amount": 100.00,
        "status": "CREATED"
      },
      {
        "orderId": 3002,
        "orderNo": "ORD20250108202",
        "userId": 202,
        "productName": "商品B",
        "amount": 200.00,
        "status": "CREATED"
      },
      ...
      // 可以添加更多条目测试性能
    ]
  }'
```

### 4. 查询消息统计信息

#### 4.1 查询生产者统计

```bash
curl "http://localhost:8000/messaging/stats?statsType=PRODUCER"
```

响应：
```json
{
  "code": "SUCCESS",
  "message": "获取消息统计信息成功",
  "data": {
    "producerStats": {
      "sentCount": 10,
      "successCount": 10,
      "failedCount": 0,
      "successRate": 1.0,
      "averageElapsedTime": 15.5,
      "startTime": 1704672000000
    },
    "consumerStats": null
  },
  "success": true
}
```

#### 4.2 查询消费者统计

```bash
curl "http://localhost:8000/messaging/stats?statsType=CONSUMER"
```

响应：
```json
{
  "code": "SUCCESS",
  "message": "获取消息统计信息成功",
  "data": {
    "producerStats": null,
    "consumerStats": {
      "consumedCount": 9,
      "successCount": 9,
      "failedCount": 0,
      "successRate": 1.0,
      "averageElapsedTime": 25.3,
      "processingCount": 1,
      "startTime": 1704672000000
    }
  },
  "success": true
}
```

#### 4.3 查询所有统计

```bash
curl "http://localhost:8000/messaging/stats?statsType=ALL"
```

响应：
```json
{
  "code": "SUCCESS",
  "message": "获取消息统计信息成功",
  "data": {
    "producerStats": {
      "sentCount": 10,
      "successCount": 10,
      "failedCount": 0,
      "successRate": 1.0,
      "averageElapsedTime": 15.5,
      "startTime": 1704672000000
    },
    "consumerStats": {
      "consumedCount": 10,
      "successCount": 10,
      "failedCount": 0,
      "successRate": 1.0,
      "averageElapsedTime": 25.3,
      "processingCount": 0,
      "startTime": 1704672000000
    }
  },
  "success": true
}
```

## RabbitMQ 管理界面验证

### 1. 查看 Exchanges（交换机）

访问：http://localhost:15672/#/exchanges

应该能看到以下 Exchange：
- `order.notification` (type: topic)
- `order.status.update` (type: topic)
- `order.notification.batch` (type: topic)

### 2. 查看 Queues（队列）

访问：http://localhost:15672/#/queues

应该能看到以下 Queue：
- `order-notification-queue`
- `order.status.update`
- `order-batch-queue`

点击队列可以查看：
- Messages（消息数量）
- Message rate（消息速率）
- Consumers（消费者数量）

### 3. 查看 Bindings（绑定关系）

在 Exchanges 页面点击具体的 Exchange，可以看到绑定关系：
- Exchange `order.notification` 绑定到 Queue `order-notification-queue`
- Exchange `order.status.update` 绑定到 Queue `order.status.update`
- Exchange `order.notification.batch` 绑定到 Queue `order-batch-queue`

## 功能验证清单

###  基础消息传递

- [x] 发送单条消息 - 成功发送并返回消息ID
- [x] 消息消费 - 消息处理器正确处理消息
- [x] 同步发送 - 等待发送完成返回结果
- [x] 异步发送 - 立即返回，异步处理

###  批量操作

- [x] 批量发送消息 - 正确处理多条消息
- [x] 批量发送统计 - 返回成功失败数量
- [x] 批量消费 - 消费者正确处理批量消息

###  消息路由

- [x] Topic 路由 - 消息正确路由到对应队列
- [x] Queue 绑定 - Exchange 和 Queue 正确绑定
- [x] 消息确认 - 消息被正确确认

###  注解处理器

- [x] @MessageListener 注解 - 自动注册处理器
- [x] 并发处理 - 支持并发消费
- [x] 重试机制 - 失败自动重试

###  性能监控

- [x] 生产者统计 - 发送数量成功率耗时
- [x] 消费者统计 - 消费数量成功率耗时
- [x] 实时监控 - RabbitMQ 管理界面监控

## 性能测试

### 1. 单条消息发送性能

```bash
# 测试 100 次单条发送
for i in {1..100}; do
  echo "发送第 $i 条消息"
  time curl -X POST http://localhost:8000/messaging/order/notification \
    -H "Content-Type: application/json" \
    -d '{
      "orderId": '"$i"',
      "orderNo": "ORD2025010'"$i"',
      "userId": 100,
      "productName": "测试商品",
      "amount": 100.00,
      "status": "CREATED",
      "notificationType": "ORDER_CREATED"
    }' -s > /dev/null
done
```

### 2. 批量发送性能

```bash
# 测试批量发送 100 条消息
time curl -X POST http://localhost:8000/messaging/order/batch-notification \
  -H "Content-Type: application/json" \
  -d '{
    "notifications": [
      // 构造 100 条消息的 JSON 数组
    ]
  }'
```

### 3. 并发发送测试

使用 Apache Bench 进行并发测试：

```bash
# 安装 ab 工具
# Ubuntu: sudo apt-get install apache2-utils
# macOS: 已预装

# 并发 10 个请求，总共 100 个请求
ab -n 100 -c 10 -p notification.json -T "application/json" \
  http://localhost:8000/messaging/order/notification
```

notification.json 内容：
```json
{
  "orderId": 1001,
  "orderNo": "ORD20250108001",
  "userId": 100,
  "productName": "测试商品",
  "amount": 100.00,
  "status": "CREATED",
  "notificationType": "ORDER_CREATED"
}
```

## 故障排查

### 1. RabbitMQ 连接失败

**问题**：应用启动时报连接错误
```
Failed to connect to RabbitMQ: Connection refused
```

**解决方案**：
- 检查 RabbitMQ 是否启动：`docker ps | grep rabbitmq`
- 检查端口是否正确：`netstat -an | grep 5672`
- 检查配置：确认 application.yml 中的 hostportusernamepassword 正确

### 2. 消息发送失败

**问题**：消息发送返回失败
```json
{
  "success": false,
  "errorMessage": "Failed to send message"
}
```

**解决方案**：
- 检查 Exchange 是否存在
- 查看 RabbitMQ 日志：`docker logs rabbitmq`
- 检查网络连接

### 3. 消息未被消费

**问题**：发送消息成功，但消息处理器未执行

**解决方案**：
- 检查 @MessageListener 注解配置是否正确
- 确认 topic 和 queue 名称一致
- 查看应用日志，确认处理器是否注册成功：
  ```
  注册消息处理器: bean=OrderNotificationHandler, method=handleOrderNotification, topic=order.notification
  ```

### 4. 消息处理异常

**问题**：消息被重复消费或消费失败

**解决方案**：
- 检查处理器代码是否有异常
- 查看重试配置：maxRetries
- 确认是否需要幂等性处理

## 开发建议

### 1. 消息设计

```java
/**
 * 消息事件应该：
 * 1. 包含完整的业务信息，避免消费者回查
 * 2. 使用不可变对象（final 字段）
 * 3. 实现 Serializable 接口
 * 4. 包含事件时间戳
 */
@Data
@Builder
public class OrderEvent implements Serializable {
    private final Long orderId;
    private final String orderNo;
    private final LocalDateTime eventTime;
    // ... 其他字段
}
```

### 2. 异常处理

```java
@MessageListener(topic = "order.notification", maxRetries = 5)
public void handleOrderNotification(Message<OrderEvent> message) {
    try {
        // 业务处理
        processOrder(message.getPayload());
        
    } catch (BusinessException e) {
        // 业务异常，不重试
        log.error("业务异常，不重试: {}", e.getMessage());
        throw new RuntimeException("业务异常", e);
        
    } catch (Exception e) {
        // 系统异常，允许重试
        log.error("系统异常，将重试: {}", e.getMessage());
        throw e;
    }
}
```

### 3. 幂等性保证

```java
@MessageListener("order.payment")
public void handlePayment(Message<PaymentEvent> message) {
    String messageId = message.getId();
    
    // 检查消息是否已处理
    if (isProcessed(messageId)) {
        log.info("消息已处理过，跳过: messageId={}", messageId);
        return;
    }
    
    try {
        // 处理业务
        processPayment(message.getPayload());
        
        // 标记已处理
        markAsProcessed(messageId);
        
    } catch (Exception e) {
        log.error("处理失败", e);
        throw e;
    }
}
```

### 4. 监控和日志

```java
@MessageListener(topic = "order.notification")
public void handleOrderNotification(Message<OrderEvent> message) {
    long startTime = System.currentTimeMillis();
    
    try {
        log.info("开始处理订单通知: orderId={}", message.getPayload().getOrderId());
        
        // 业务处理
        processOrder(message.getPayload());
        
        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("订单通知处理完成: orderId={}, elapsedTime={}ms", 
            message.getPayload().getOrderId(), elapsedTime);
        
    } catch (Exception e) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        log.error("订单通知处理失败: orderId={}, elapsedTime={}ms", 
            message.getPayload().getOrderId(), elapsedTime, e);
        throw e;
    }
}
```

---

更多详细信息，请参考：
- [Nebula Messaging RabbitMQ README](../../nebula/infrastructure/messaging/nebula-messaging-rabbitmq/README.md)
- [Nebula 框架使用指南](../../nebula/docs/Nebula框架使用指南.md)


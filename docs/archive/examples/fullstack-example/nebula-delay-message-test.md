# Nebula延时消息功能测试指南

## 概述

本文档详细说明如何测试Nebula框架的延时消息功能，包括发送延时消息、消费延时消息、重试机制和死信队列处理。

## 测试前准备

### 1. 启动RabbitMQ

```bash
# 使用Docker启动RabbitMQ
cd nebula-data
docker-compose up -d rabbitmq

# 访问管理界面
# http://localhost:15672
# 用户名: guest
# 密码: guest
```

### 2. 配置应用

在 `application.yml` 中配置延时消息：

```yaml
nebula:
  messaging:
    rabbitmq:
      enabled: true
      host: localhost
      port: 5672
      username: guest
      password: guest
      
      # 延时消息配置
      delay-message:
        enabled: true
        default-max-retries: 3
        default-retry-interval: 1000
        max-delay-millis: 604800000  # 7天
        min-delay-millis: 1000       # 1秒
        auto-create-resources: true
        enable-dead-letter-queue: true
```

## 测试用例

### 测试1: 基本延时消息

#### 1.1 定义事件

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderTimeoutEvent {
    private Long orderId;
    private Long userId;
    private String orderNo;
    private LocalDateTime createTime;
}
```

#### 1.2 发送延时消息

```java
@RestController
@RequestMapping("/api/test/delay-message")
@RequiredArgsConstructor
@Slf4j
public class DelayMessageTestController {
    
    private final DelayMessageProducer delayMessageProducer;
    
    /**
     * 测试发送延时消息
     * 
     * GET /api/test/delay-message/send?orderId=1001&delaySeconds=10
     */
    @GetMapping("/send")
    public Result<String> sendDelayMessage(
            @RequestParam Long orderId,
            @RequestParam(defaultValue = "10") int delaySeconds) {
        
        OrderTimeoutEvent event = new OrderTimeoutEvent(
            orderId, 
            1L, 
            "ORDER" + System.currentTimeMillis(),
            LocalDateTime.now()
        );
        
        DelayMessageResult result = delayMessageProducer.send(
            "order.timeout",
            "order.timeout.queue",
            event,
            Duration.ofSeconds(delaySeconds)
        );
        
        if (result.isSuccess()) {
            log.info("延时消息发送成功: messageId={}, orderId={}, delay={}秒",
                    result.getMessageId(), orderId, delaySeconds);
            return Result.success("延时消息发送成功，将在" + delaySeconds + "秒后处理");
        } else {
            log.error("延时消息发送失败: {}", result.getErrorMessage());
            return Result.failure("延时消息发送失败: " + result.getErrorMessage());
        }
    }
}
```

#### 1.3 消费延时消息

```java
@Component
@Slf4j
public class OrderTimeoutHandler {
    
    @Autowired
    private DelayMessageConsumer delayMessageConsumer;
    
    @Autowired
    private OrderService orderService;
    
    @PostConstruct
    public void init() throws IOException {
        // 订阅延时消息
        delayMessageConsumer.subscribe(
            "order.timeout.queue",
            OrderTimeoutEvent.class,
            this::handleOrderTimeout
        );
        
        log.info("订阅订单超时延时消息成功");
    }
    
    /**
     * 处理订单超时
     */
    private void handleOrderTimeout(OrderTimeoutEvent event, DelayMessageContext context) {
        log.info("========== 收到订单超时延时消息 ==========");
        log.info("消息ID: {}", context.getMessageId());
        log.info("订单ID: {}", event.getOrderId());
        log.info("订单号: {}", event.getOrderNo());
        log.info("创建时间: {}", event.getCreateTime());
        log.info("延时设置: {}ms", context.getDelayMillis());
        log.info("预期处理时间: {}", new Date(context.getExpectedTime()));
        log.info("实际处理时间: {}", new Date(context.getActualTime()));
        log.info("延时误差: {}ms", context.getDelayError());
        log.info("总延时: {}ms", context.getTotalDelay());
        log.info("重试次数: {}/{}", context.getCurrentRetry(), context.getMaxRetries());
        log.info("==========================================");
        
        // 业务处理：检查订单状态并取消未支付订单
        orderService.cancelUnpaidOrder(event.getOrderId());
    }
}
```

#### 1.4 执行测试

```bash
# 发送10秒延时消息
curl "http://localhost:8080/api/test/delay-message/send?orderId=1001&delaySeconds=10"

# 观察日志，10秒后应该看到消费日志
```

#### 1.5 预期结果

```
2025-11-03 10:00:00.123 - 延时消息发送成功: messageId=DELAY_xxx, orderId=1001, delay=10秒
...
2025-11-03 10:00:10.156 - ========== 收到订单超时延时消息 ==========
2025-11-03 10:00:10.156 - 消息ID: DELAY_xxx
2025-11-03 10:00:10.156 - 订单ID: 1001
2025-11-03 10:00:10.156 - 延时误差: 33ms  (约几十毫秒的误差是正常的)
2025-11-03 10:00:10.156 - 总延时: 10033ms
```

---

### 测试2: 批量延时消息

#### 2.1 发送批量延时消息

```java
@GetMapping("/send-batch")
public Result<String> sendBatchDelayMessages(@RequestParam int count) {
    List<DelayMessage<OrderTimeoutEvent>> messages = new ArrayList<>();
    
    for (int i = 0; i < count; i++) {
        OrderTimeoutEvent event = new OrderTimeoutEvent(
            1000L + i,
            1L,
            "ORDER" + System.currentTimeMillis() + i,
            LocalDateTime.now()
        );
        
        DelayMessage<OrderTimeoutEvent> message = DelayMessage.<OrderTimeoutEvent>builder()
            .topic("order.timeout")
            .queue("order.timeout.queue")
            .payload(event)
            .delay(Duration.ofSeconds(5 + i))  // 递增延时
            .maxRetries(3)
            .build();
        
        messages.add(message);
    }
    
    BatchDelayMessageResult result = delayMessageProducer.sendBatch(messages);
    
    log.info("批量发送延时消息: 总数={}, 成功={}, 失败={}, 成功率={}%",
            result.getTotalCount(),
            result.getSuccessCount(),
            result.getFailedCount(),
            result.getSuccessRate() * 100);
    
    return Result.success(String.format("批量发送完成，成功=%d, 失败=%d",
            result.getSuccessCount(), result.getFailedCount()));
}
```

#### 2.2 执行测试

```bash
# 批量发送5条延时消息
curl "http://localhost:8080/api/test/delay-message/send-batch?count=5"

# 观察日志，消息将按延时时间依次被消费
# 第1条: 5秒后
# 第2条: 6秒后
# 第3条: 7秒后
# ...
```

---

### 测试3: 重试机制

#### 3.1 模拟处理失败

```java
@GetMapping("/send-with-failure")
public Result<String> sendWithFailure(@RequestParam Long orderId) {
    OrderTimeoutEvent event = new OrderTimeoutEvent(
        orderId,
        1L,
        "FAIL_TEST_" + orderId,
        LocalDateTime.now()
    );
    
    DelayMessage<OrderTimeoutEvent> message = DelayMessage.<OrderTimeoutEvent>builder()
        .topic("order.timeout")
        .queue("order.timeout.fail.queue")
        .payload(event)
        .delay(Duration.ofSeconds(5))
        .maxRetries(3)
        .retryInterval(Duration.ofSeconds(2))
        .build();
    
    DelayMessageResult result = delayMessageProducer.send(message);
    
    return Result.success("已发送，将在处理时模拟失败并重试");
}
```

#### 3.2 消费者模拟失败

```java
@PostConstruct
public void initFailureTest() throws IOException {
    delayMessageConsumer.subscribe(
        "order.timeout.fail.queue",
        OrderTimeoutEvent.class,
        (event, context) -> {
            log.warn("模拟处理失败: orderId={}, retry={}/{}",
                    event.getOrderId(),
                    context.getCurrentRetry(),
                    context.getMaxRetries());
            
            // 故意抛出异常，触发重试
            throw new RuntimeException("模拟处理失败");
        }
    );
}
```

#### 3.3 执行测试

```bash
curl "http://localhost:8080/api/test/delay-message/send-with-failure?orderId=2001"

# 观察日志，应该看到3次重试
```

#### 3.4 预期结果

```
2025-11-03 10:00:05 - 模拟处理失败: orderId=2001, retry=0/3
2025-11-03 10:00:07 - 模拟处理失败: orderId=2001, retry=1/3
2025-11-03 10:00:09 - 模拟处理失败: orderId=2001, retry=2/3
2025-11-03 10:00:11 - 模拟处理失败: orderId=2001, retry=3/3
2025-11-03 10:00:11 - 超过最大重试次数，发送到死信队列
```

---

### 测试4: 死信队列

#### 4.1 检查死信队列

在RabbitMQ管理界面查看死信队列：

```
队列名称: nebula.dlx.queue
交换机: nebula.dlx.exchange
```

#### 4.2 消费死信队列

```java
@Component
@Slf4j
public class DeadLetterQueueHandler {
    
    @Autowired
    private DelayMessageConsumer delayMessageConsumer;
    
    @PostConstruct
    public void init() throws IOException {
        // 订阅死信队列
        delayMessageConsumer.subscribe(
            "nebula.dlx.queue",
            OrderTimeoutEvent.class,
            this::handleDeadLetter
        );
    }
    
    private void handleDeadLetter(OrderTimeoutEvent event, DelayMessageContext context) {
        log.error("========== 收到死信消息 ==========");
        log.error("消息ID: {}", context.getMessageId());
        log.error("订单ID: {}", event.getOrderId());
        log.error("失败原因: 已重试{}次仍然失败", context.getMaxRetries());
        log.error("===================================");
        
        // 记录到数据库或发送告警
        alertService.sendFailureAlert(event, context);
    }
}
```

---

### 测试5: 不同延时时间

#### 5.1 测试各种延时时间

```java
@GetMapping("/test-various-delays")
public Result<Map<String, String>> testVariousDelays() {
    Map<String, String> results = new LinkedHashMap<>();
    
    // 测试1秒延时
    testDelay("1秒", Duration.ofSeconds(1), results);
    
    // 测试1分钟延时
    testDelay("1分钟", Duration.ofMinutes(1), results);
    
    // 测试30分钟延时（订单超时）
    testDelay("30分钟", Duration.ofMinutes(30), results);
    
    // 测试1小时延时
    testDelay("1小时", Duration.ofHours(1), results);
    
    // 测试1天延时
    testDelay("1天", Duration.ofDays(1), results);
    
    return Result.success(results);
}

private void testDelay(String label, Duration delay, Map<String, String> results) {
    OrderTimeoutEvent event = new OrderTimeoutEvent(
        System.currentTimeMillis(),
        1L,
        "TEST_" + label,
        LocalDateTime.now()
    );
    
    DelayMessageResult result = delayMessageProducer.send(
        "order.timeout",
        "order.timeout.queue",
        event,
        delay
    );
    
    results.put(label, result.isSuccess() ? "成功" : "失败: " + result.getErrorMessage());
}
```

---

## 监控和调试

### 1. RabbitMQ管理界面

访问 http://localhost:15672 查看：

- 延时交换机: `nebula.delay.exchange.*`
- 延时队列: `nebula.delay.queue.*`
- 目标交换机和队列
- 死信交换机和队列

### 2. 查看队列详情

```bash
# 列出所有队列
rabbitmqctl list_queues

# 查看延时队列详情
rabbitmqctl list_queues name messages arguments | grep nebula.delay

# 查看死信队列
rabbitmqctl list_queues name messages | grep nebula.dlx
```

### 3. 应用日志

关键日志点：

- 延时消息发送成功
- 延时消息消费
- 延时误差统计
- 重试记录
- 死信队列消息

---

## 性能测试

### 1. 吞吐量测试

```java
@GetMapping("/performance-test")
public Result<String> performanceTest(@RequestParam int count) {
    long startTime = System.currentTimeMillis();
    
    CountDownLatch latch = new CountDownLatch(count);
    
    for (int i = 0; i < count; i++) {
        CompletableFuture.runAsync(() -> {
            try {
                OrderTimeoutEvent event = new OrderTimeoutEvent(
                    System.nanoTime(),
                    1L,
                    "PERF_TEST",
                    LocalDateTime.now()
                );
                
                delayMessageProducer.send(
                    "order.timeout",
                    "order.timeout.queue",
                    event,
                    Duration.ofMinutes(30)
                );
            } finally {
                latch.countDown();
            }
        });
    }
    
    try {
        latch.await();
        long elapsed = System.currentTimeMillis() - startTime;
        double qps = (count * 1000.0) / elapsed;
        
        return Result.success(String.format(
            "发送%d条消息，耗时%dms，QPS=%.2f",
            count, elapsed, qps
        ));
    } catch (InterruptedException e) {
        return Result.failure("测试中断");
    }
}
```

### 2. 延时精度测试

收集100条消息的延时误差，计算平均值和标准差：

```java
private List<Long> delayErrors = new CopyOnWriteArrayList<>();

@GetMapping("/accuracy-test")
public Result<Map<String, Object>> accuracyTest() {
    delayErrors.clear();
    
    // 发送100条10秒延时消息
    for (int i = 0; i < 100; i++) {
        // ... 发送消息
    }
    
    // 等待所有消息处理完成
    Thread.sleep(15000);
    
    // 统计延时误差
    DoubleSummaryStatistics stats = delayErrors.stream()
        .mapToDouble(Long::doubleValue)
        .summaryStatistics();
    
    Map<String, Object> result = new HashMap<>();
    result.put("样本数", stats.getCount());
    result.put("平均延时误差(ms)", stats.getAverage());
    result.put("最小延时误差(ms)", stats.getMin());
    result.put("最大延时误差(ms)", stats.getMax());
    
    return Result.success(result);
}
```

---

## 常见问题

### 1. 延时消息未被消费

检查项：
- RabbitMQ是否运行
- 延时队列是否创建
- 消费者是否正常订阅
- TTL是否到期

### 2. 延时精度较差

可能原因：
- RabbitMQ负载过高
- 消费者处理速度慢
- 网络延迟

### 3. 消息进入死信队列

检查项：
- 业务处理是否抛出异常
- 是否超过最大重试次数
- 查看异常日志

### 4. 内存占用过高

优化建议：
- 减少单个消息体大小
- 及时消费延时消息
- 清理死信队列

---

## 总结

本测试指南覆盖了Nebula延时消息功能的：

1. 基本功能：发送和消费延时消息
2. 批量处理：批量发送延时消息
3. 可靠性：重试机制和死信队列
4. 灵活性：支持不同延时时间
5. 监控：RabbitMQ管理界面和日志
6. 性能：吞吐量和延时精度测试

通过这些测试用例，可以全面验证延时消息功能的正确性和性能。


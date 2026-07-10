# nebula-messaging-rabbitmq 模块单元测试清单

## 模块说明

基于RabbitMQ的消息传递模块，提供统一的消息抽象和强大的消息传递能力，支持同步/异步发送、延迟消息、顺序消息等。

## 核心功能

1. 消息生产（同步、异步、批量发送）
2. 消息消费（推模式、拉模式，@MessageHandler注解）
3. 延迟消息（基于TTL+DLX机制）
4. 消息序列化（JSON）
5. Exchange管理

## 测试类清单

### 1. MessageProducerTest

**测试类路径**: `io.nebula.messaging.rabbitmq.producer.MessageProducer`  
**测试目的**: 验证消息生产者的发送功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testSendMessage() | send(Message) | 测试同步发送消息 | RabbitTemplate |
| testSendWithTopic() | send(String, Object) | 测试指定topic发送消息 | RabbitTemplate |
| testSendAsync() | sendAsync(String, Object) | 测试异步发送消息 | RabbitTemplate |
| testSendBatch() | sendBatch(List) | 测试批量发送消息 | RabbitTemplate |
| testSendWithMetadata() | send(Message) | 测试带元数据的消息发送 | RabbitTemplate |

**测试数据准备**:
- Mock RabbitTemplate
- 准备测试消息对象
- 准备测试topic

**验证要点**:
- send方法调用成功
- 消息序列化正确
- 异步发送返回CompletableFuture
- 批量发送调用正确次数

**Mock示例**:
```java
@Mock
private RabbitTemplate rabbitTemplate;

@BeforeEach
void setUp() {
    doNothing().when(rabbitTemplate).convertAndSend(anyString(), any());
}
```

---

### 2. MessageConsumerTest

**测试类路径**: `io.nebula.messaging.rabbitmq.consumer.MessageConsumer`  
**测试目的**: 验证消息消费者的订阅和拉取功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testSubscribe() | subscribe(String, MessageHandler) | 测试订阅消息 | Channel |
| testPullOne() | pullOne(String, Duration) | 测试拉取单个消息 | Channel |
| testPullBatch() | pull(String, int, Duration) | 测试批量拉取消息 | Channel |

**测试数据准备**:
- Mock Channel
- Mock MessageHandler
- 准备测试消息

**验证要点**:
- 订阅成功
- MessageHandler被调用
- 拉取返回正确消息

---

### 3. MessageHandlerAnnotationTest

**测试类路径**: `@MessageHandler`注解处理  
**测试目的**: 验证注解方式消费消息

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testMessageHandlerRegistration() | - | 测试@MessageHandler注解的方法自动注册 | MessageListenerContainer |
| testHandlerInvocation() | - | 测试消息到达时Handler被调用 | Message |
| testHandlerWithConcurrency() | - | 测试并发消费设置 | - |

**测试数据准备**:
- 创建带@MessageHandler注解的测试类
- Mock MessageListenerContainer

**验证要点**:
- 注解方法被扫描到
- Handler正确调用
- 并发配置生效

---

### 4. DelayMessageProducerTest

**测试类路径**: `io.nebula.messaging.rabbitmq.delay.DelayMessageProducer`  
**测试目的**: 验证延时消息发送功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testSendDelayMessage() | send(String, Object, Duration) | 测试发送延时消息 | RabbitTemplate |
| testSendDelayMessageWithConfig() | send(DelayMessage) | 测试带配置的延时消息 | RabbitTemplate |
| testSendBatchDelayMessages() | sendBatch(List) | 测试批量发送延时消息 | RabbitTemplate |
| testDelayDuration() | - | 测试延时时间正确设置 | - |

**测试数据准备**:
- Mock RabbitTemplate
- 准备延时消息对象
- 准备延时时间Duration

**验证要点**:
- 消息发送到延时Exchange
- TTL正确设置
- DLX正确配置

**Mock示例**:
```java
@Mock
private RabbitTemplate rabbitTemplate;

@Test
void testSendDelayMessage() {
    DelayMessageProducer producer = new DelayMessageProducer(rabbitTemplate);
    
    DelayMessage<String> message = DelayMessage.<String>builder()
        .topic("test.topic")
        .payload("test message")
        .delay(Duration.ofMinutes(5))
        .build();
    
    DelayMessageResult result = producer.send(message);
    
    verify(rabbitTemplate).convertAndSend(
        eq("delay.exchange"),
        any(),
        any(),
        argThat(processor -> {
            // 验证TTL设置
            return true;
        })
    );
    
    assertThat(result.isSuccess()).isTrue();
}
```

---

### 5. DelayMessageConsumerTest

**测试类路径**: `io.nebula.messaging.rabbitmq.delay.DelayMessageConsumer`  
**测试目的**: 验证延时消息消费功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testSubscribe() | subscribe(String, Class, DelayMessageHandler) | 测试订阅延时消息 | Channel |
| testDelayContext() | - | 测试DelayMessageContext信息正确 | - |

**测试数据准备**:
- Mock Channel
- Mock DelayMessageHandler
- 准备延时消息

**验证要点**:
- 订阅成功
- Handler被调用
- Context包含延时误差等信息

---

## Mock策略

### 需要Mock的对象

| Mock对象 | 使用场景 | Mock行为 |
|---------|-----------|---------|
| RabbitTemplate | 消息发送测试 | Mock convertAndSend() |
| Channel | 消息消费测试 | Mock basicConsume(), basicGet() |
| Connection | 连接管理 | Mock createChannel() |
| MessageHandler | 消费测试 | Mock handle() |
| MessageListenerContainer | 注解处理 | Mock start(), stop() |

### 不需要真实RabbitMQ
**所有测试都应该Mock RabbitMQ客户端，不需要启动真实的RabbitMQ服务**。

---

## 测试依赖

```xml
<dependencies>
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- AssertJ -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Spring AMQP Test -->
    <dependency>
        <groupId>org.springframework.amqp</groupId>
        <artifactId>spring-rabbit-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 测试执行

运行测试：
```bash
mvn test -pl nebula/infrastructure/messaging/nebula-messaging-rabbitmq
```

查看测试报告：
```bash
mvn surefire-report:report
```

---

## 验收标准

- 所有测试方法通过
- 核心功能测试覆盖率 >= 90%
- Mock对象使用正确，无真实RabbitMQ依赖
- 延时消息测试通过
- 注解处理测试通过


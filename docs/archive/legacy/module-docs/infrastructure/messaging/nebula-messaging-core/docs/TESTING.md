# Nebula Messaging Core - 测试指南

> 消息传递核心抽象模块的测试策略与实践

## 目录

- [测试策略](#测试策略)
- [单元测试](#单元测试)
- [接口契约测试](#接口契约测试)
- [Mock测试](#mock测试)
- [测试工具](#测试工具)

---

## 测试策略

### 测试层次

由于 `nebula-messaging-core` 是抽象模块，测试主要分为：

1. **单元测试**：测试消息模型和工具类
2. **接口契约测试**：定义实现模块必须遵守的契约
3. **Mock测试**：使用Mock进行消息处理测试

### 测试覆盖目标

- 消息模型类：100%
- 接口定义完整性：100%
- 注解处理器：100%

---

## 单元测试

### 1. Message模型测试

```java
/**
 * Message模型测试
 */
class MessageTest {
    
    @Test
    void testMessageBuilder() {
        LocalDateTime now = LocalDateTime.now();
        
        Message<String> message = Message.<String>builder()
                .id("msg-001")
                .topic("test-topic")
                .payload("Hello, World!")
                .createTime(now)
                .priority(5)
                .requireAck(true)
                .persistent(true)
                .build();
        
        assertThat(message.getId()).isEqualTo("msg-001");
        assertThat(message.getTopic()).isEqualTo("test-topic");
        assertThat(message.getPayload()).isEqualTo("Hello, World!");
        assertThat(message.getPriority()).isEqualTo(5);
        assertThat(message.isRequireAck()).isTrue();
        assertThat(message.isPersistent()).isTrue();
    }
    
    @Test
    void testMessageFactoryMethod() {
        Message<String> message = Message.of("test-topic", "Test payload");
        
        assertThat(message.getTopic()).isEqualTo("test-topic");
        assertThat(message.getPayload()).isEqualTo("Test payload");
        assertThat(message.getId()).isNotBlank();
        assertThat(message.getCreateTime()).isNotNull();
    }
    
    @Test
    void testMessageWithHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("source", "test-service");
        headers.put("version", "1.0");
        
        Message<String> message = Message.<String>builder()
                .topic("test-topic")
                .payload("test")
                .headers(headers)
                .build();
        
        assertThat(message.getHeaders())
                .containsEntry("source", "test-service")
                .containsEntry("version", "1.0");
    }
    
    @Test
    void testRetryCount() {
        Message<String> message = Message.<String>builder()
                .topic("test-topic")
                .payload("test")
                .maxRetryCount(3)
                .build();
        
        assertThat(message.canRetry()).isTrue();
        assertThat(message.getRetryCount()).isEqualTo(0);
        
        message.incrementRetryCount();
        assertThat(message.getRetryCount()).isEqualTo(1);
        assertThat(message.canRetry()).isTrue();
        
        message.incrementRetryCount();
        message.incrementRetryCount();
        assertThat(message.getRetryCount()).isEqualTo(3);
        assertThat(message.canRetry()).isFalse();
    }
    
    @Test
    void testMessageExpiry() throws InterruptedException {
        Message<String> message = Message.<String>builder()
                .topic("test-topic")
                .payload("test")
                .ttl(Duration.ofMillis(100))
                .createTime(LocalDateTime.now())
                .build();
        
        assertThat(message.isExpired()).isFalse();
        
        Thread.sleep(150);
        
        assertThat(message.isExpired()).isTrue();
    }
}
```

### 2. MessageProducer接口测试

```java
/**
 * MessageProducer接口测试（使用Mock）
 */
@ExtendWith(MockitoExtension.class)
class MessageProducerTest {
    
    @Mock
    private MessageProducer messageProducer;
    
    @Test
    void testSendMessage() {
        Message<String> message = Message.of("test-topic", "test payload");
        
        when(messageProducer.send(any(Message.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        SendResult.success(message.getId())
                ));
        
        CompletableFuture<SendResult> future = messageProducer.send(message);
        
        assertThat(future).isCompleted();
        assertThat(future.join().isSuccess()).isTrue();
        
        verify(messageProducer).send(message);
    }
    
    @Test
    void testSendToTopic() {
        when(messageProducer.sendToTopic(eq("test-topic"), eq("payload")))
                .thenReturn(CompletableFuture.completedFuture(
                        SendResult.success("msg-123")
                ));
        
        CompletableFuture<SendResult> future = messageProducer.sendToTopic("test-topic", "payload");
        
        assertThat(future).isCompleted();
        assertThat(future.join().isSuccess()).isTrue();
        assertThat(future.join().getMessageId()).isEqualTo("msg-123");
    }
    
    @Test
    void testSendFailure() {
        Message<String> message = Message.of("test-topic", "test");
        
        when(messageProducer.send(any(Message.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        SendResult.failure("Network error")
                ));
        
        CompletableFuture<SendResult> future = messageProducer.send(message);
        
        assertThat(future.join().isSuccess()).isFalse();
        assertThat(future.join().getErrorMessage()).isEqualTo("Network error");
    }
}
```

### 3. SendResult测试

```java
/**
 * SendResult测试
 */
class SendResultTest {
    
    @Test
    void testSuccessResult() {
        SendResult result = SendResult.success("msg-001");
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessageId()).isEqualTo("msg-001");
        assertThat(result.getErrorMessage()).isNull();
    }
    
    @Test
    void testSuccessResultWithDetails() {
        SendResult result = SendResult.success("msg-001", "broker-1");
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessageId()).isEqualTo("msg-001");
        assertThat(result.getBroker()).isEqualTo("broker-1");
    }
    
    @Test
    void testFailureResult() {
        SendResult result = SendResult.failure("Send timeout");
        
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Send timeout");
        assertThat(result.getMessageId()).isNull();
    }
    
    @Test
    void testPartialFailureResult() {
        SendResult result = SendResult.builder()
                .success(false)
                .messageId("msg-001")
                .errorMessage("Partial send failure")
                .build();
        
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessageId()).isEqualTo("msg-001");
        assertThat(result.getErrorMessage()).isEqualTo("Partial send failure");
    }
}
```

---

## 接口契约测试

### 1. MessageProducer契约测试基类

实现模块应该继承此基类进行测试：

```java
/**
 * MessageProducer接口契约测试基类
 * 
 * 所有MessageProducer的实现类都应该继承此类并实现抽象方法
 */
public abstract class MessageProducerContractTest {
    
    /**
     * 子类需要提供MessageProducer实现
     */
    protected abstract MessageProducer getMessageProducer();
    
    /**
     * 子类需要提供测试Topic
     */
    protected abstract String getTestTopic();
    
    @Test
    void testSendSimpleMessage() {
        MessageProducer producer = getMessageProducer();
        String topic = getTestTopic();
        
        Message<String> message = Message.of(topic, "test payload");
        
        CompletableFuture<SendResult> future = producer.send(message);
        SendResult result = future.join();
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessageId()).isNotBlank();
    }
    
    @Test
    void testSendToTopic() {
        MessageProducer producer = getMessageProducer();
        String topic = getTestTopic();
        
        CompletableFuture<SendResult> future = producer.sendToTopic(topic, "test");
        SendResult result = future.join();
        
        assertThat(result.isSuccess()).isTrue();
    }
    
    @Test
    void testSendToQueue() {
        MessageProducer producer = getMessageProducer();
        
        CompletableFuture<SendResult> future = producer.sendToQueue("test-queue", "test");
        SendResult result = future.join();
        
        assertThat(result.isSuccess()).isTrue();
    }
    
    @Test
    void testSendWithHeaders() {
        MessageProducer producer = getMessageProducer();
        String topic = getTestTopic();
        
        Map<String, String> headers = new HashMap<>();
        headers.put("key1", "value1");
        headers.put("key2", "value2");
        
        Message<String> message = Message.<String>builder()
                .topic(topic)
                .payload("test")
                .headers(headers)
                .build();
        
        CompletableFuture<SendResult> future = producer.send(message);
        SendResult result = future.join();
        
        assertThat(result.isSuccess()).isTrue();
    }
    
    @Test
    void testSendWithPriority() {
        MessageProducer producer = getMessageProducer();
        String topic = getTestTopic();
        
        Message<String> message = Message.<String>builder()
                .topic(topic)
                .payload("high priority")
                .priority(9)
                .build();
        
        CompletableFuture<SendResult> future = producer.send(message);
        SendResult result = future.join();
        
        assertThat(result.isSuccess()).isTrue();
    }
    
    @Test
    void testSendPersistentMessage() {
        MessageProducer producer = getMessageProducer();
        String topic = getTestTopic();
        
        Message<String> message = Message.<String>builder()
                .topic(topic)
                .payload("persistent message")
                .persistent(true)
                .build();
        
        CompletableFuture<SendResult> future = producer.send(message);
        SendResult result = future.join();
        
        assertThat(result.isSuccess()).isTrue();
    }
    
    @Test
    void testSendDelayedMessage() {
        MessageProducer producer = getMessageProducer();
        String topic = getTestTopic();
        
        Message<String> message = Message.<String>builder()
                .topic(topic)
                .payload("delayed message")
                .delay(Duration.ofSeconds(5))
                .build();
        
        CompletableFuture<SendResult> future = producer.send(message);
        SendResult result = future.join();
        
        assertThat(result.isSuccess()).isTrue();
    }
    
    @Test
    void testBatchSend() {
        MessageProducer producer = getMessageProducer();
        String topic = getTestTopic();
        
        List<Message<String>> messages = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            messages.add(Message.of(topic, "batch message " + i));
        }
        
        List<CompletableFuture<SendResult>> futures = producer.sendBatch(messages);
        
        assertThat(futures).hasSize(10);
        
        for (CompletableFuture<SendResult> future : futures) {
            SendResult result = future.join();
            assertThat(result.isSuccess()).isTrue();
        }
    }
}
```

### 2. MessageConsumer契约测试基类

```java
/**
 * MessageConsumer接口契约测试基类
 */
public abstract class MessageConsumerContractTest {
    
    /**
     * 子类需要提供MessageConsumer实现
     */
    protected abstract MessageConsumer getMessageConsumer();
    
    /**
     * 子类需要提供MessageProducer实现（用于发送测试消息）
     */
    protected abstract MessageProducer getMessageProducer();
    
    /**
     * 子类需要提供测试Topic
     */
    protected abstract String getTestTopic();
    
    @Test
    void testSubscribeTopic() throws InterruptedException {
        MessageConsumer consumer = getMessageConsumer();
        MessageProducer producer = getMessageProducer();
        String topic = getTestTopic();
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Message<?>> receivedMessage = new AtomicReference<>();
        
        // 订阅消息
        consumer.subscribe(topic, message -> {
            receivedMessage.set(message);
            latch.countDown();
        });
        
        // 发送测试消息
        Message<String> testMessage = Message.of(topic, "test payload");
        producer.send(testMessage).join();
        
        // 等待接收
        boolean received = latch.await(5, TimeUnit.SECONDS);
        
        assertThat(received).isTrue();
        assertThat(receivedMessage.get()).isNotNull();
        assertThat(receivedMessage.get().getPayload()).isEqualTo("test payload");
    }
    
    @Test
    void testUnsubscribe() throws InterruptedException {
        MessageConsumer consumer = getMessageConsumer();
        MessageProducer producer = getMessageProducer();
        String topic = getTestTopic();
        
        AtomicInteger receiveCount = new AtomicInteger(0);
        
        // 订阅
        consumer.subscribe(topic, message -> receiveCount.incrementAndGet());
        
        // 发送消息
        producer.sendToTopic(topic, "message 1").join();
        
        Thread.sleep(1000);
        
        // 取消订阅
        consumer.unsubscribe(topic);
        
        // 再次发送消息
        producer.sendToTopic(topic, "message 2").join();
        
        Thread.sleep(1000);
        
        // 应该只收到第一条消息
        assertThat(receiveCount.get()).isEqualTo(1);
    }
}
```

---

## Mock测试

### 1. 消息监听器测试

```java
/**
 * 消息监听器Mock测试
 */
@ExtendWith(MockitoExtension.class)
class MessageListenerTest {
    
    @Mock
    private MessageListener<String> messageListener;
    
    @Test
    void testOnMessage() {
        Message<String> message = Message.of("test-topic", "test payload");
        
        doNothing().when(messageListener).onMessage(any(Message.class));
        
        messageListener.onMessage(message);
        
        verify(messageListener).onMessage(message);
    }
    
    @Test
    void testOnMessageException() {
        Message<String> message = Message.of("test-topic", "test");
        
        doThrow(new RuntimeException("Processing error"))
                .when(messageListener).onMessage(any(Message.class));
        
        assertThatThrownBy(() -> messageListener.onMessage(message))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Processing error");
    }
}
```

### 2. 消息处理器测试

```java
/**
 * 消息处理器测试
 */
@SpringBootTest
class MessageHandlerTest {
    
    @MockBean
    private MessageProducer messageProducer;
    
    @Autowired
    private TestMessageHandler messageHandler;
    
    @Test
    void testHandleMessage() {
        Message<String> message = Message.of("test-topic", "test payload");
        
        when(messageProducer.sendToTopic(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(
                        SendResult.success("msg-123")
                ));
        
        messageHandler.handleMessage(message);
        
        // 验证处理逻辑
        verify(messageProducer).sendToTopic(eq("response-topic"), anyString());
    }
    
    @Component
    static class TestMessageHandler {
        
        private final MessageProducer messageProducer;
        
        TestMessageHandler(MessageProducer messageProducer) {
            this.messageProducer = messageProducer;
        }
        
        public void handleMessage(Message<String> message) {
            String response = "Processed: " + message.getPayload();
            messageProducer.sendToTopic("response-topic", response);
        }
    }
}
```

---

## 测试工具

### 1. 测试消息构建器

```java
/**
 * 测试消息构建器
 */
public class TestMessageBuilder {
    
    /**
     * 创建简单测试消息
     */
    public static <T> Message<T> createSimple(String topic, T payload) {
        return Message.<T>builder()
                .id(UUID.randomUUID().toString())
                .topic(topic)
                .payload(payload)
                .createTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建带标签的测试消息
     */
    public static <T> Message<T> createWithTag(String topic, String tag, T payload) {
        return Message.<T>builder()
                .id(UUID.randomUUID().toString())
                .topic(topic)
                .tag(tag)
                .payload(payload)
                .createTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建带优先级的测试消息
     */
    public static <T> Message<T> createWithPriority(String topic, T payload, int priority) {
        return Message.<T>builder()
                .id(UUID.randomUUID().toString())
                .topic(topic)
                .payload(payload)
                .priority(priority)
                .createTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建延迟消息
     */
    public static <T> Message<T> createDelayed(String topic, T payload, Duration delay) {
        return Message.<T>builder()
                .id(UUID.randomUUID().toString())
                .topic(topic)
                .payload(payload)
                .delay(delay)
                .createTime(LocalDateTime.now())
                .build();
    }
}
```

### 2. 测试消息监听器

```java
/**
 * 测试消息监听器
 */
public class TestMessageListener<T> implements MessageListener<T> {
    
    private final List<Message<T>> receivedMessages = new CopyOnWriteArrayList<>();
    private final CountDownLatch latch;
    
    public TestMessageListener(int expectedCount) {
        this.latch = new CountDownLatch(expectedCount);
    }
    
    @Override
    public void onMessage(Message<T> message) {
        receivedMessages.add(message);
        latch.countDown();
    }
    
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return latch.await(timeout, unit);
    }
    
    public List<Message<T>> getReceivedMessages() {
        return new ArrayList<>(receivedMessages);
    }
    
    public int getReceivedCount() {
        return receivedMessages.size();
    }
    
    public void reset(int expectedCount) {
        receivedMessages.clear();
        latch = new CountDownLatch(expectedCount);
    }
}
```

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [EXAMPLE.md](./EXAMPLE.md) - 使用示例
- [CONFIG.md](./CONFIG.md) - 配置指南
- [ROADMAP.md](./ROADMAP.md) - 发展路线图

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0


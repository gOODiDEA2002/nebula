package io.nebula.messaging.rabbitmq.delay;

import com.rabbitmq.client.*;
import io.nebula.messaging.core.serializer.MessageSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DelayMessageConsumer单元测试
 * 
 * 测试目的: 验证延时消息消费功能
 */
@ExtendWith(MockitoExtension.class)
class DelayMessageConsumerTest {
    
    @Mock
    private Connection connection;
    
    @Mock
    private Channel channel;
    
    @Mock
    private MessageSerializer messageSerializer;
    
    @Mock
    private RabbitDelayMessageProperties properties;
    
    private DelayMessageConsumer consumer;
    
    @BeforeEach
    void setUp() throws Exception {
        lenient().when(connection.createChannel()).thenReturn(channel);
        lenient().when(connection.isOpen()).thenReturn(true);
        
        // 设置默认属性
        lenient().when(properties.getDefaultMaxRetries()).thenReturn(3);
        lenient().when(properties.getDefaultRetryInterval()).thenReturn(java.time.Duration.ofSeconds(5));
        lenient().when(properties.isEnableDeadLetterQueue()).thenReturn(true);
        
        RabbitDelayMessageProperties.DeadLetterQueue dlqConfig = new RabbitDelayMessageProperties.DeadLetterQueue();
        dlqConfig.setExchange("nebula.dlx.exchange");
        dlqConfig.setQueue("nebula.dlx.queue");
        dlqConfig.setDurable(true);
        dlqConfig.setAutoDelete(false);
        lenient().when(properties.getDeadLetterQueue()).thenReturn(dlqConfig);
        
        consumer = new DelayMessageConsumer(connection, messageSerializer, properties);
    }
    
    @Test
    void testSubscribe() throws Exception {
        // 准备测试数据
        String queue = "test.delay.queue";
        String messageBody = "test message";
        
        lenient().when(messageSerializer.deserialize(any(byte[].class), eq(String.class)))
                .thenReturn(messageBody);
        
        // 捕获DeliverCallback
        ArgumentCaptor<DeliverCallback> callbackCaptor = ArgumentCaptor.forClass(DeliverCallback.class);
        when(channel.basicConsume(eq(queue), eq(false), callbackCaptor.capture(), any(CancelCallback.class)))
                .thenReturn("consumer-tag-123");
        
        // 创建测试handler
        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        AtomicReference<DelayMessageContext> capturedContext = new AtomicReference<>();
        
        DelayMessageConsumer.DelayMessageHandler<String> handler = (payload, context) -> {
            handlerCalled.set(true);
            capturedContext.set(context);
            assertThat(payload).isEqualTo(messageBody);
        };
        
        // 执行订阅
        consumer.subscribe(queue, String.class, handler);
        
        // 验证队列声明
        verify(channel).queueDeclare(eq(queue), eq(true), eq(false), eq(false), isNull());
        verify(channel).basicQos(1);
        verify(channel).basicConsume(eq(queue), eq(false), any(DeliverCallback.class), any(CancelCallback.class));
    }
    
    @Test
    void testHandleMessageSuccess() throws Exception {
        // 准备测试数据
        String queue = "test.delay.queue";
        String messageBody = "test payload";
        String messageId = "msg-123";
        
        when(messageSerializer.deserialize(any(byte[].class), eq(String.class)))
                .thenReturn(messageBody);
        
        // 创建mock消息
        Envelope envelope = new Envelope(1L, false, "exchange", "routing.key");
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .messageId(messageId)
                .timestamp(new java.util.Date())
                .headers(createDelayHeaders())
                .build();
        
        byte[] body = "test".getBytes();
        Delivery delivery = new Delivery(envelope, props, body);
        
        // 捕获DeliverCallback
        ArgumentCaptor<DeliverCallback> callbackCaptor = ArgumentCaptor.forClass(DeliverCallback.class);
        when(channel.basicConsume(eq(queue), eq(false), callbackCaptor.capture(), any(CancelCallback.class)))
                .thenReturn("consumer-tag-123");
        
        // 创建测试handler
        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        AtomicReference<DelayMessageContext> capturedContext = new AtomicReference<>();
        
        DelayMessageConsumer.DelayMessageHandler<String> handler = (payload, context) -> {
            handlerCalled.set(true);
            capturedContext.set(context);
        };
        
        // 订阅
        consumer.subscribe(queue, String.class, handler);
        
        // 获取callback并触发
        DeliverCallback callback = callbackCaptor.getValue();
        callback.handle("consumer-tag", delivery);
        
        // 验证
        assertThat(handlerCalled.get()).isTrue();
        verify(channel).basicAck(1L, false);
        
        // 验证context信息
        DelayMessageContext context = capturedContext.get();
        assertThat(context).isNotNull();
        assertThat(context.getMessageId()).isEqualTo(messageId);
        assertThat(context.getOriginalTopic()).isEqualTo("test.topic");
        assertThat(context.getOriginalQueue()).isEqualTo("test.queue");
        assertThat(context.getDelayMillis()).isEqualTo(5000L);
    }
    
    @Test
    void testHandleMessageFailureWithRetry() throws Exception {
        // 准备测试数据
        String queue = "test.delay.queue";
        
        when(messageSerializer.deserialize(any(byte[].class), eq(String.class)))
                .thenReturn("test");
        
        // 创建mock消息，currentRetry=0
        Map<String, Object> headers = createDelayHeaders();
        headers.put("x-delay-current-retry", 0);
        headers.put("x-delay-max-retries", 3);
        
        Envelope envelope = new Envelope(1L, false, "exchange", "routing.key");
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .messageId("msg-123")
                .headers(headers)
                .build();
        
        byte[] body = "test".getBytes();
        Delivery delivery = new Delivery(envelope, props, body);
        
        // 捕获DeliverCallback
        ArgumentCaptor<DeliverCallback> callbackCaptor = ArgumentCaptor.forClass(DeliverCallback.class);
        when(channel.basicConsume(eq(queue), eq(false), callbackCaptor.capture(), any(CancelCallback.class)))
                .thenReturn("consumer-tag-123");
        
        // 创建抛出异常的handler
        DelayMessageConsumer.DelayMessageHandler<String> handler = (payload, context) -> {
            throw new RuntimeException("Handler error");
        };
        
        // 订阅
        consumer.subscribe(queue, String.class, handler);
        
        // 获取callback并触发
        DeliverCallback callback = callbackCaptor.getValue();
        callback.handle("consumer-tag", delivery);
        
        // 验证重试消息被发送
        verify(channel).basicPublish(
                eq("test.topic"),
                eq("test.queue"),
                any(AMQP.BasicProperties.class),
                any(byte[].class)
        );
        
        // 验证原消息被确认
        verify(channel).basicAck(1L, false);
    }
    
    @Test
    void testHandleMessageFailureMaxRetries() throws Exception {
        // 准备测试数据
        String queue = "test.delay.queue";
        
        when(messageSerializer.deserialize(any(byte[].class), eq(String.class)))
                .thenReturn("test");
        
        // 创建mock消息，currentRetry=3（达到最大重试次数）
        Map<String, Object> headers = createDelayHeaders();
        headers.put("x-delay-current-retry", 3);
        headers.put("x-delay-max-retries", 3);
        
        Envelope envelope = new Envelope(1L, false, "exchange", "routing.key");
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .messageId("msg-123")
                .headers(headers)
                .build();
        
        byte[] body = "test".getBytes();
        Delivery delivery = new Delivery(envelope, props, body);
        
        // 捕获DeliverCallback
        ArgumentCaptor<DeliverCallback> callbackCaptor = ArgumentCaptor.forClass(DeliverCallback.class);
        when(channel.basicConsume(eq(queue), eq(false), callbackCaptor.capture(), any(CancelCallback.class)))
                .thenReturn("consumer-tag-123");
        
        // 创建抛出异常的handler
        DelayMessageConsumer.DelayMessageHandler<String> handler = (payload, context) -> {
            throw new RuntimeException("Handler error");
        };
        
        // 订阅
        consumer.subscribe(queue, String.class, handler);
        
        // 获取callback并触发
        DeliverCallback callback = callbackCaptor.getValue();
        callback.handle("consumer-tag", delivery);
        
        // 验证死信交换机和队列被创建
        verify(channel).exchangeDeclare(
                eq("nebula.dlx.exchange"),
                eq("direct"),
                eq(true),
                eq(false),
                isNull()
        );
        verify(channel).queueDeclare(
                eq("nebula.dlx.queue"),
                eq(true),
                eq(false),
                eq(false),
                isNull()
        );
        
        // 验证消息被发送到死信队列
        verify(channel).basicPublish(
                eq("nebula.dlx.exchange"),
                eq("nebula.dlx.queue"),
                any(AMQP.BasicProperties.class),
                any(byte[].class)
        );
        
        // 验证原消息被确认
        verify(channel).basicAck(1L, false);
    }
    
    @Test
    void testDelayContext() throws Exception {
        // 准备测试数据
        String queue = "test.delay.queue";
        String messageBody = "test payload";
        
        when(messageSerializer.deserialize(any(byte[].class), eq(String.class)))
                .thenReturn(messageBody);
        
        // 创建包含完整延时信息的headers
        Map<String, Object> headers = new HashMap<>();
        headers.put("x-delay-original-topic", "test.topic");
        headers.put("x-delay-original-queue", "test.queue");
        headers.put("x-delay-millis", 10000L);
        headers.put("x-delay-expected-time", System.currentTimeMillis() + 10000);
        headers.put("x-delay-max-retries", 5);
        headers.put("x-delay-current-retry", 1);
        
        Envelope envelope = new Envelope(1L, false, "exchange", "routing.key");
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .messageId("msg-456")
                .timestamp(new java.util.Date())
                .headers(headers)
                .build();
        
        byte[] body = "test".getBytes();
        Delivery delivery = new Delivery(envelope, props, body);
        
        // 捕获DeliverCallback
        ArgumentCaptor<DeliverCallback> callbackCaptor = ArgumentCaptor.forClass(DeliverCallback.class);
        when(channel.basicConsume(eq(queue), eq(false), callbackCaptor.capture(), any(CancelCallback.class)))
                .thenReturn("consumer-tag-123");
        
        // 创建测试handler
        AtomicReference<DelayMessageContext> capturedContext = new AtomicReference<>();
        
        DelayMessageConsumer.DelayMessageHandler<String> handler = (payload, context) -> {
            capturedContext.set(context);
        };
        
        // 订阅
        consumer.subscribe(queue, String.class, handler);
        
        // 获取callback并触发
        DeliverCallback callback = callbackCaptor.getValue();
        callback.handle("consumer-tag", delivery);
        
        // 验证DelayMessageContext信息
        DelayMessageContext context = capturedContext.get();
        assertThat(context).isNotNull();
        assertThat(context.getMessageId()).isEqualTo("msg-456");
        assertThat(context.getOriginalTopic()).isEqualTo("test.topic");
        assertThat(context.getOriginalQueue()).isEqualTo("test.queue");
        assertThat(context.getDelayMillis()).isEqualTo(10000L);
        assertThat(context.getMaxRetries()).isEqualTo(5);
        assertThat(context.getCurrentRetry()).isEqualTo(1);
        assertThat(context.getExpectedTime()).isGreaterThan(0);
        
        // 验证延时误差可以计算
        long actualTime = context.getTimestamp();
        long expectedTime = context.getExpectedTime();
        long delayError = Math.abs(actualTime - expectedTime);
        assertThat(delayError).isGreaterThanOrEqualTo(0);
    }
    
    @Test
    void testUnsubscribe() throws Exception {
        String queue = "test.delay.queue";
        String consumerTag = "consumer-tag-123";
        
        // 先订阅
        when(channel.basicConsume(eq(queue), eq(false), any(DeliverCallback.class), any(CancelCallback.class)))
                .thenReturn(consumerTag);
        
        DelayMessageConsumer.DelayMessageHandler<String> handler = (payload, context) -> {};
        consumer.subscribe(queue, String.class, handler);
        
        // 取消订阅
        consumer.unsubscribe(queue);
        
        // 验证
        verify(channel).basicCancel(consumerTag);
    }
    
    @Test
    void testIsAvailable() {
        when(connection.isOpen()).thenReturn(true);
        assertThat(consumer.isAvailable()).isTrue();
        
        when(connection.isOpen()).thenReturn(false);
        assertThat(consumer.isAvailable()).isFalse();
    }
    
    /**
     * 创建延时消息headers
     */
    private Map<String, Object> createDelayHeaders() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("x-delay-original-topic", "test.topic");
        headers.put("x-delay-original-queue", "test.queue");
        headers.put("x-delay-millis", 5000L);
        headers.put("x-delay-expected-time", System.currentTimeMillis() + 5000);
        return headers;
    }
}


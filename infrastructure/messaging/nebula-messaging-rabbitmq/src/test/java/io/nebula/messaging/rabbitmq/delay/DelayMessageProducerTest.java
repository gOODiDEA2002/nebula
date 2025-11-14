package io.nebula.messaging.rabbitmq.delay;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import io.nebula.messaging.core.serializer.MessageSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

/**
 * 延时消息生产者测试
 * 
 * 测试覆盖:
 * - 单条延时消息发送
 * - 批量延时消息发送
 * - 延时时间验证
 * - 消息属性构建
 * - 异常处理
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("延时消息生产者测试")
class DelayMessageProducerTest {
    
    @Mock
    private Connection connection;
    
    @Mock
    private Channel channel;
    
    @Mock
    private MessageSerializer messageSerializer;
    
    private DelayMessageProducer producer;
    
    @BeforeEach
    void setUp() throws IOException, TimeoutException {
        when(connection.createChannel()).thenReturn(channel);
        when(connection.isOpen()).thenReturn(true);
        producer = new DelayMessageProducer(connection, messageSerializer);
    }
    
    @AfterEach
    void tearDown() {
        // 清理资源
    }
    
    @Test
    @DisplayName("测试发送简单延时消息")
    void testSendDelayMessage() throws Exception {
        // 准备测试数据
        String topic = "test.topic";
        TestPayload payload = new TestPayload("test-message");
        Duration delay = Duration.ofSeconds(10);
        
        // 模拟序列化
        byte[] serializedData = "serialized-data".getBytes();
        when(messageSerializer.serialize(payload)).thenReturn(serializedData);
        
        // 执行测试
        DelayMessageResult result = producer.send(topic, payload, delay);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessageId()).isNotNull();
        assertThat(result.getMessageId()).startsWith("DELAY_");
        
        // 验证交互
        verify(channel).exchangeDeclare(eq(topic), eq("topic"), eq(true), eq(false), isNull());
        verify(channel).queueDeclare(anyString(), eq(true), eq(false), eq(false), anyMap());
        verify(channel).basicPublish(anyString(), anyString(), any(), eq(serializedData));
    }
    
    @Test
    @DisplayName("测试发送延时消息到指定队列")
    void testSendDelayMessageWithQueue() throws Exception {
        // 准备测试数据
        String topic = "test.topic";
        String queue = "test.queue";
        TestPayload payload = new TestPayload("test-message");
        Duration delay = Duration.ofSeconds(30);
        
        // 模拟序列化
        byte[] serializedData = "serialized-data".getBytes();
        when(messageSerializer.serialize(payload)).thenReturn(serializedData);
        
        // 执行测试
        DelayMessageResult result = producer.send(topic, queue, payload, delay);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessageId()).isNotNull();
        
        // 验证队列创建
        ArgumentCaptor<String> queueCaptor = ArgumentCaptor.forClass(String.class);
        verify(channel, atLeastOnce()).queueDeclare(queueCaptor.capture(), eq(true), eq(false), eq(false), anyMap());
        
        // 验证队列名称包含topic或queue
        List<String> queueNames = queueCaptor.getAllValues();
        assertThat(queueNames).anySatisfy(queueName -> 
            assertThat(queueName).containsAnyOf(topic, queue)
        );
    }
    
    @Test
    @DisplayName("测试批量发送延时消息")
    void testSendBatchDelayMessages() throws Exception {
        // 准备测试数据
        int messageCount = 5;
        List<DelayMessage<TestPayload>> messages = new ArrayList<>();
        
        for (int i = 0; i < messageCount; i++) {
            DelayMessage<TestPayload> message = DelayMessage.<TestPayload>builder()
                    .topic("test.topic")
                    .queue("test.queue." + i)
                    .payload(new TestPayload("message-" + i))
                    .delay(Duration.ofSeconds(10 + i))
                    .build();
            messages.add(message);
        }
        
        // 模拟序列化
        when(messageSerializer.serialize(any())).thenReturn("serialized".getBytes());
        
        // 执行测试
        BatchDelayMessageResult result = producer.sendBatch(messages);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isEqualTo(messageCount);
        assertThat(result.getSuccessCount()).isEqualTo(messageCount);
        assertThat(result.getFailedCount()).isEqualTo(0);
        assertThat(result.getSuccessRate()).isEqualTo(1.0);
        
        // 验证每条消息都被发送
        verify(channel, times(messageCount)).basicPublish(anyString(), anyString(), any(), any(byte[].class));
    }
    
    @Test
    @DisplayName("测试发送延时消息时的重试配置")
    void testSendDelayMessageWithRetry() throws Exception {
        // 准备测试数据
        int maxRetries = 5;
        Duration retryInterval = Duration.ofSeconds(3);
        
        DelayMessage<TestPayload> message = DelayMessage.<TestPayload>builder()
                .topic("test.topic")
                .payload(new TestPayload("test"))
                .delay(Duration.ofMinutes(10))
                .maxRetries(maxRetries)
                .retryInterval(retryInterval)
                .build();
        
        // 模拟序列化
        when(messageSerializer.serialize(any())).thenReturn("data".getBytes());
        
        // 执行测试
        DelayMessageResult result = producer.send(message);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        
        // 验证消息属性中包含重试配置
        ArgumentCaptor<com.rabbitmq.client.AMQP.BasicProperties> propsCaptor = 
            ArgumentCaptor.forClass(com.rabbitmq.client.AMQP.BasicProperties.class);
        verify(channel).basicPublish(anyString(), anyString(), propsCaptor.capture(), any(byte[].class));
        
        com.rabbitmq.client.AMQP.BasicProperties props = propsCaptor.getValue();
        assertThat(props.getHeaders()).containsEntry("x-delay-max-retries", maxRetries);
    }
    
    @Test
    @DisplayName("测试发送延时消息时的优先级设置")
    void testSendDelayMessageWithPriority() throws Exception {
        // 准备测试数据
        int priority = 9;
        
        DelayMessage<TestPayload> message = DelayMessage.<TestPayload>builder()
                .topic("test.topic")
                .payload(new TestPayload("urgent-message"))
                .delay(Duration.ofSeconds(5))
                .priority(priority)
                .build();
        
        // 模拟序列化
        when(messageSerializer.serialize(any())).thenReturn("data".getBytes());
        
        // 执行测试
        DelayMessageResult result = producer.send(message);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        
        // 验证消息属性中包含优先级
        ArgumentCaptor<com.rabbitmq.client.AMQP.BasicProperties> propsCaptor = 
            ArgumentCaptor.forClass(com.rabbitmq.client.AMQP.BasicProperties.class);
        verify(channel).basicPublish(anyString(), anyString(), propsCaptor.capture(), any(byte[].class));
        
        com.rabbitmq.client.AMQP.BasicProperties props = propsCaptor.getValue();
        assertThat(props.getPriority()).isEqualTo(priority);
    }
    
    @Test
    @DisplayName("测试无效的延时时间")
    void testInvalidDelayTime() {
        // 测试null延时
        DelayMessage<TestPayload> message1 = DelayMessage.<TestPayload>builder()
                .topic("test.topic")
                .payload(new TestPayload("test"))
                .delay(null)
                .build();
        
        assertThatThrownBy(() -> producer.send(message1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Delay duration must be positive");
        
        // 测试负数延时
        DelayMessage<TestPayload> message2 = DelayMessage.<TestPayload>builder()
                .topic("test.topic")
                .payload(new TestPayload("test"))
                .delay(Duration.ofSeconds(-1))
                .build();
        
        assertThatThrownBy(() -> producer.send(message2))
                .isInstanceOf(IllegalArgumentException.class);
        
        // 测试零延时
        DelayMessage<TestPayload> message3 = DelayMessage.<TestPayload>builder()
                .topic("test.topic")
                .payload(new TestPayload("test"))
                .delay(Duration.ZERO)
                .build();
        
        assertThatThrownBy(() -> producer.send(message3))
                .isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    @DisplayName("测试连接不可用时的处理")
    void testConnectionUnavailable() {
        // 模拟连接关闭
        when(connection.isOpen()).thenReturn(false);
        
        // 验证isAvailable返回false
        assertThat(producer.isAvailable()).isFalse();
    }
    
    @Test
    @DisplayName("测试发送失败时的异常处理")
    void testSendFailureHandling() throws Exception {
        // 模拟发送失败
        when(messageSerializer.serialize(any())).thenReturn("data".getBytes());
        doThrow(new IOException("Network error"))
                .when(channel).basicPublish(anyString(), anyString(), any(), any(byte[].class));
        
        // 执行测试
        DelayMessageResult result = producer.send("test.topic", new TestPayload("test"), Duration.ofSeconds(10));
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Network error");
        assertThat(result.getException()).isInstanceOf(IOException.class);
    }
    
    @Test
    @DisplayName("测试批量发送部分失败")
    void testBatchSendPartialFailure() throws Exception {
        // 准备测试数据
        List<DelayMessage<TestPayload>> messages = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            messages.add(DelayMessage.<TestPayload>builder()
                    .topic("test.topic")
                    .payload(new TestPayload("message-" + i))
                    .delay(Duration.ofSeconds(10))
                    .build());
        }
        
        // 模拟第2条消息发送失败
        when(messageSerializer.serialize(any())).thenReturn("data".getBytes());
        doNothing()  // 第1条成功
                .doThrow(new IOException("Failure"))  // 第2条失败
                .doNothing()  // 第3条成功
                .when(channel).basicPublish(anyString(), anyString(), any(), any(byte[].class));
        
        // 执行测试
        BatchDelayMessageResult result = producer.sendBatch(messages);
        
        // 验证结果
        assertThat(result.getTotalCount()).isEqualTo(3);
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getFailedCount()).isEqualTo(1);
        assertThat(result.getSuccessRate()).isCloseTo(0.67, within(0.01));
    }
    
    /**
     * 测试载荷类
     */
    private static class TestPayload {
        private final String content;
        
        public TestPayload(String content) {
            this.content = content;
        }
        
        public String getContent() {
            return content;
        }
    }
}


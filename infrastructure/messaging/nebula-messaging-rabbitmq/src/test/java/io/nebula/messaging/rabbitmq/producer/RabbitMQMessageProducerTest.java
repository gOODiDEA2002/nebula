package io.nebula.messaging.rabbitmq.producer;

import io.nebula.messaging.core.message.Message;
import io.nebula.messaging.core.producer.MessageProducer;
import io.nebula.messaging.core.serializer.MessageSerializer;
import io.nebula.messaging.rabbitmq.delay.DelayMessageProducer;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RabbitMQMessageProducer单元测试
 */
@ExtendWith(MockitoExtension.class)
class RabbitMQMessageProducerTest {
    
    @Mock
    private Connection connection;
    
    @Mock
    private Channel channel;
    
    @Mock
    private MessageSerializer messageSerializer;
    
    @Mock
    private DelayMessageProducer delayMessageProducer;
    
    private RabbitMQMessageProducer<String> producer;
    
    @BeforeEach
    void setUp() throws Exception {
        lenient().when(connection.createChannel()).thenReturn(channel);
        lenient().when(messageSerializer.serialize(any())).thenReturn("test-message".getBytes());
        
        producer = new RabbitMQMessageProducer<>(connection, messageSerializer, delayMessageProducer);
    }
    
    @Test
    void testSendWithTopicAndPayload() throws Exception {
        String topic = "test.topic";
        String payload = "test message";
        
        MessageProducer.SendResult result = producer.send(topic, payload);
        
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTopic()).isEqualTo(topic);
        verify(messageSerializer).serialize(payload);
        verify(channel).basicPublish(eq(topic), anyString(), any(), any(byte[].class));
    }
    
    @Test
    void testSendWithMessage() throws Exception {
        String topic = "test.topic";
        String payload = "test payload";
        Message<String> message = Message.<String>builder()
                .topic(topic)
                .payload(payload)
                .build();
        
        MessageProducer.SendResult result = producer.send(message);
        
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        verify(channel).basicPublish(eq(topic), anyString(), any(), any(byte[].class));
    }
    
    @Test
    void testSendWithTopicQueueAndPayload() throws Exception {
        String topic = "test.topic";
        String queue = "test.queue";
        String payload = "test message";
        
        MessageProducer.SendResult result = producer.send(topic, queue, payload);
        
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTopic()).isEqualTo(topic);
        assertThat(result.getQueue()).isEqualTo(queue);
        verify(channel).queueDeclare(eq(queue), anyBoolean(), anyBoolean(), anyBoolean(), any());
        verify(channel).queueBind(eq(queue), eq(topic), anyString());
    }
    
    @Test
    void testSendWithHeaders() throws Exception {
        String topic = "test.topic";
        String payload = "test message";
        Map<String, String> headers = new HashMap<>();
        headers.put("key1", "value1");
        headers.put("key2", "value2");
        
        MessageProducer.SendResult result = producer.send(topic, payload, headers);
        
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        verify(channel).basicPublish(eq(topic), anyString(), any(), any(byte[].class));
    }
    
    @Test
    void testSendException() throws Exception {
        String topic = "test.topic";
        String payload = "test message";
        
        doThrow(new IOException("Connection error")).when(channel).basicPublish(anyString(), anyString(), any(), any(byte[].class));
        
        MessageProducer.SendResult result = producer.send(topic, payload);
        
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Connection error");
    }
    
    @Test
    void testStart() {
        assertThatCode(() -> producer.start()).doesNotThrowAnyException();
    }
    
    @Test
    void testStop() {
        assertThatCode(() -> producer.stop()).doesNotThrowAnyException();
    }
    
    @Test
    void testIsAvailable() throws Exception {
        when(connection.isOpen()).thenReturn(true);
        producer.start(); // isAvailable需要started标志为true
        assertThat(producer.isAvailable()).isTrue();
    }
}


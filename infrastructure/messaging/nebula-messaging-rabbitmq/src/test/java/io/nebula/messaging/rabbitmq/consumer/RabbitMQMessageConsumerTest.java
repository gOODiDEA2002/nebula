package io.nebula.messaging.rabbitmq.consumer;

import io.nebula.messaging.core.consumer.MessageHandler;
import io.nebula.messaging.core.message.Message;
import io.nebula.messaging.core.router.MessageRouter;
import io.nebula.messaging.core.serializer.MessageSerializer;
import com.rabbitmq.client.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * RabbitMQMessageConsumer单元测试
 */
@ExtendWith(MockitoExtension.class)
class RabbitMQMessageConsumerTest {
    
    @Mock
    private Connection connection;
    
    @Mock
    private Channel channel;
    
    @Mock
    private MessageSerializer messageSerializer;
    
    @Mock
    private MessageRouter messageRouter;
    
    @Mock
    private MessageHandler<Object> messageHandler;
    
    private RabbitMQMessageConsumer consumer;
    
    @BeforeEach
    void setUp() throws IOException {
        lenient().when(connection.createChannel()).thenReturn(channel);
        lenient().when(channel.basicConsume(anyString(), anyBoolean(), any(DeliverCallback.class), any(CancelCallback.class))).thenReturn("consumerTag");
        lenient().when(channel.isOpen()).thenReturn(true);
        
        consumer = new RabbitMQMessageConsumer(connection, messageSerializer, messageRouter);
    }
    
    @Test
    void testSubscribe() throws Exception {
        String topic = "test.topic";
        
        consumer.subscribe(topic, messageHandler);
        
        verify(connection).createChannel();
        verify(channel).exchangeDeclare(eq(topic), eq("topic"), eq(true), eq(false), any());
        verify(channel).queueDeclare(eq(topic), eq(true), eq(false), eq(false), any());
        verify(channel).queueBind(eq(topic), eq(topic), eq(topic));
    }
    
    @Test
    void testSubscribeWithQueue() throws Exception {
        String topic = "test.topic";
        String queue = "test.queue";
        
        consumer.subscribe(topic, queue, messageHandler);
        
        verify(channel).exchangeDeclare(eq(topic), anyString(), anyBoolean(), anyBoolean(), any());
        verify(channel).queueDeclare(eq(queue), anyBoolean(), anyBoolean(), anyBoolean(), any());
        verify(channel).queueBind(eq(queue), eq(topic), eq(topic));
    }
    
    @Test
    void testUnsubscribe() throws Exception {
        String topic = "test.topic";
        
        consumer.subscribe(topic, messageHandler);
        consumer.unsubscribe(topic);
        
        verify(channel).close();
    }
    
    @Test
    void testUnsubscribeWithQueue() throws Exception {
        String topic = "test.topic";
        String queue = "test.queue";
        
        consumer.subscribe(topic, queue, messageHandler);
        consumer.unsubscribe(topic, queue);
        
        verify(channel).close();
    }
    
    @Test
    void testStart() {
        assertThatCode(() -> consumer.start()).doesNotThrowAnyException();
    }
    
    @Test
    void testStop() {
        assertThatCode(() -> consumer.stop()).doesNotThrowAnyException();
    }
    
    @Test
    void testIsRunning() {
        consumer.start();
        assertThat(consumer.isRunning()).isTrue();
        
        consumer.stop();
        assertThat(consumer.isRunning()).isFalse();
    }
    
    @Test
    void testPause() {
        consumer.pause();
        assertThat(consumer.isPaused()).isTrue();
    }
    
    @Test
    void testResume() {
        consumer.pause();
        consumer.resume();
        assertThat(consumer.isPaused()).isFalse();
    }
}


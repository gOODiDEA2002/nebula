package io.nebula.messaging.rabbitmq.annotation;

import io.nebula.messaging.core.annotation.MessageHandler;
import io.nebula.messaging.core.annotation.MessageHandlerProcessor;
import io.nebula.messaging.core.consumer.MessageConsumer;
import io.nebula.messaging.core.manager.MessageManager;
import io.nebula.messaging.core.message.Message;
import io.nebula.messaging.core.producer.MessageProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MessageHandler注解功能测试
 * 
 * 测试目的: 验证@MessageHandler注解方式消费消息
 */
@ExtendWith(MockitoExtension.class)
class MessageHandlerAnnotationTest {
    
    @Mock
    private MessageManager messageManager;
    
    @Mock
    private MessageConsumer<Object> consumer;
    
    @Mock
    private MessageProducer producer;
    
    private MessageHandlerProcessor processor;
    
    @BeforeEach
    void setUp() {
        lenient().when(messageManager.getConsumer()).thenReturn(consumer);
        lenient().when(messageManager.getProducer()).thenReturn(producer);
        lenient().when(consumer.getConfig()).thenReturn(null);
        processor = new MessageHandlerProcessor(messageManager);
    }
    
    @Test
    void testMessageHandlerRegistration() {
        // 创建带@MessageHandler注解的测试Bean
        TestMessageHandler testHandler = new TestMessageHandler();
        
        // 处理Bean（模拟Spring容器调用）
        processor.postProcessAfterInitialization(testHandler, "testMessageHandler");
        
        // 验证注册了处理器
        verify(consumer).subscribe(eq("test.topic"), eq("test.queue"), any());
    }
    
    @Test
    void testHandlerInvocation() throws Exception {
        // 创建测试Bean
        TestMessageHandler testHandler = new TestMessageHandler();
        
        // 捕获注册的处理器
        ArgumentCaptor<io.nebula.messaging.core.consumer.MessageHandler<Object>> handlerCaptor = 
                ArgumentCaptor.forClass(io.nebula.messaging.core.consumer.MessageHandler.class);
        
        // 处理Bean
        processor.postProcessAfterInitialization(testHandler, "testMessageHandler");
        
        // 获取注册的处理器
        verify(consumer).subscribe(eq("test.topic"), eq("test.queue"), handlerCaptor.capture());
        io.nebula.messaging.core.consumer.MessageHandler<Object> handler = handlerCaptor.getValue();
        
        // 创建测试消息
        TestMessage payload = new TestMessage("test-content");
        Message<Object> message = Message.<Object>builder()
                .id("msg-123")
                .topic("test.topic")
                .payload(payload)
                .build();
        
        // 调用处理器
        handler.handle(message);
        
        // 验证处理方法被调用
        assertThat(testHandler.isHandled()).isTrue();
        assertThat(testHandler.getHandledMessage()).isEqualTo(payload);
    }
    
    @Test
    void testHandlerWithConcurrency() {
        // 创建带并发配置的测试Bean
        ConcurrentMessageHandler testHandler = new ConcurrentMessageHandler();
        
        // 处理Bean
        processor.postProcessAfterInitialization(testHandler, "concurrentMessageHandler");
        
        // 验证注册了处理器
        verify(consumer).subscribe(eq("concurrent.topic"), eq("concurrent.queue"), any());
    }
    
    @Test
    void testMultipleHandlers() {
        // 创建有多个处理器方法的Bean
        MultipleMessageHandler testHandler = new MultipleMessageHandler();
        
        // 处理Bean
        processor.postProcessAfterInitialization(testHandler, "multipleMessageHandler");
        
        // 验证注册了2个处理器
        verify(consumer).subscribe(eq("topic1"), eq("topic1"), any());
        verify(consumer).subscribe(eq("topic2"), eq("queue2"), any());
    }
    
    @Test
    void testHandlerWithTag() {
        // 创建带tag的测试Bean
        TaggedMessageHandler testHandler = new TaggedMessageHandler();
        
        // 处理Bean
        processor.postProcessAfterInitialization(testHandler, "taggedMessageHandler");
        
        // 验证使用了subscribeWithTag方法
        verify(consumer).subscribeWithTag(eq("tagged.topic"), eq("important"), any());
    }
    
    @Test
    void testHandlerWithAutoAck() {
        // 创建带autoAck配置的测试Bean
        AutoAckMessageHandler testHandler = new AutoAckMessageHandler();
        
        // 处理Bean
        processor.postProcessAfterInitialization(testHandler, "autoAckMessageHandler");
        
        // 验证注册了处理器
        verify(consumer).subscribe(eq("autoack.topic"), eq("autoack.topic"), any());
    }
    
    @Test
    void testHandlerMethodWithoutParameters() {
        // 创建没有参数的处理器方法
        InvalidMessageHandler testHandler = new InvalidMessageHandler();
        
        // 处理Bean时不应该抛出异常
        assertThatCode(() -> processor.postProcessAfterInitialization(testHandler, "invalidMessageHandler"))
                .doesNotThrowAnyException();
        
        // 验证仍然会注册处理器（即使没有参数）
        verify(consumer).subscribe(eq("invalid.topic"), eq("invalid.topic"), any());
    }
    
    @Test
    void testHandlerException() throws Exception {
        // 创建会抛出异常的测试Bean
        ExceptionMessageHandler testHandler = new ExceptionMessageHandler();
        
        // 捕获注册的处理器
        ArgumentCaptor<io.nebula.messaging.core.consumer.MessageHandler<Object>> handlerCaptor = 
                ArgumentCaptor.forClass(io.nebula.messaging.core.consumer.MessageHandler.class);
        
        // 处理Bean
        processor.postProcessAfterInitialization(testHandler, "exceptionMessageHandler");
        
        // 获取注册的处理器
        verify(consumer).subscribe(eq("exception.topic"), eq("exception.topic"), handlerCaptor.capture());
        io.nebula.messaging.core.consumer.MessageHandler<Object> handler = handlerCaptor.getValue();
        
        // 创建测试消息
        TestMessage payload = new TestMessage("test-content");
        Message<Object> message = Message.<Object>builder()
                .id("msg-123")
                .topic("exception.topic")
                .payload(payload)
                .build();
        
        // 调用处理器应该抛出异常（RuntimeException包装了原始异常）
        assertThatThrownBy(() -> handler.handle(message))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("消息处理失败");
    }
    
    // ============= 测试用Bean类 =============
    
    /**
     * 基本消息处理器测试类
     */
    @Component
    public static class TestMessageHandler {
        private final AtomicBoolean handled = new AtomicBoolean(false);
        private final AtomicReference<TestMessage> handledMessage = new AtomicReference<>();
        
        @MessageHandler(topic = "test.topic", queue = "test.queue")
        public void handleTestMessage(Message<TestMessage> message) {
            handled.set(true);
            handledMessage.set(message.getPayload());
        }
        
        public boolean isHandled() {
            return handled.get();
        }
        
        public TestMessage getHandledMessage() {
            return handledMessage.get();
        }
    }
    
    /**
     * 并发消息处理器测试类
     */
    @Component
    public static class ConcurrentMessageHandler {
        @MessageHandler(topic = "concurrent.topic", queue = "concurrent.queue", concurrency = 5)
        public void handleConcurrentMessage(Message<TestMessage> message) {
            // 处理逻辑
        }
    }
    
    /**
     * 多个处理器方法的测试类
     */
    @Component
    public static class MultipleMessageHandler {
        @MessageHandler(topic = "topic1")
        public void handleMessage1(Message<TestMessage> message) {
            // 处理逻辑
        }
        
        @MessageHandler(topic = "topic2", queue = "queue2")
        public void handleMessage2(Message<TestMessage> message) {
            // 处理逻辑
        }
    }
    
    /**
     * 带tag的消息处理器测试类
     */
    @Component
    public static class TaggedMessageHandler {
        @MessageHandler(topic = "tagged.topic", tag = "important")
        public void handleTaggedMessage(Message<TestMessage> message) {
            // 处理逻辑
        }
    }
    
    /**
     * 带autoAck配置的消息处理器测试类
     */
    @Component
    public static class AutoAckMessageHandler {
        @MessageHandler(topic = "autoack.topic", autoAck = true)
        public void handleAutoAckMessage(Message<TestMessage> message) {
            // 处理逻辑
        }
    }
    
    /**
     * 无效的消息处理器测试类（没有参数）
     */
    @Component
    public static class InvalidMessageHandler {
        @MessageHandler(topic = "invalid.topic")
        public void handleInvalidMessage() {
            // 无效：没有参数
        }
    }
    
    /**
     * 抛出异常的消息处理器测试类
     */
    @Component
    public static class ExceptionMessageHandler {
        @MessageHandler(topic = "exception.topic")
        public void handleExceptionMessage(Message<TestMessage> message) {
            throw new RuntimeException("Handler error");
        }
    }
    
    /**
     * 测试消息类
     */
    public static class TestMessage {
        private String content;
        
        public TestMessage() {
        }
        
        public TestMessage(String content) {
            this.content = content;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestMessage that = (TestMessage) o;
            return content != null ? content.equals(that.content) : that.content == null;
        }
        
        @Override
        public int hashCode() {
            return content != null ? content.hashCode() : 0;
        }
    }
}


package io.nebula.messaging.core.annotation;

import io.nebula.messaging.core.consumer.MessageConsumer;
import io.nebula.messaging.core.manager.MessageManager;
import io.nebula.messaging.core.message.Message;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link MessageHandlerProcessor} 会扫描并注册 {@link MessageListener} 注解的方法(修复前只扫 @MessageHandler,
 * 导致按新注解写的消费者静默不消费), 同时保持对已废弃 {@link MessageHandler} 的兼容。
 * <p>
 * 只验证扫描注册链路(subscribe 是否被调用), 与实际 broker 无关。
 */
class MessageHandlerProcessorTest {

    @SuppressWarnings("unchecked")
    private MessageHandlerProcessor newProcessor(MessageConsumer<Object> consumer) {
        MessageManager manager = mock(MessageManager.class);
        when(manager.getConsumer()).thenReturn(consumer);
        // getConfig 返回 null -> configureConsumer 直接返回, 不走配置分支
        when(consumer.getConfig()).thenReturn(null);
        return new MessageHandlerProcessor(manager);
    }

    @Test
    @SuppressWarnings("unchecked")
    void scansMessageListenerAnnotatedMethod() {
        MessageConsumer<Object> consumer = mock(MessageConsumer.class);
        MessageHandlerProcessor processor = newProcessor(consumer);

        processor.postProcessAfterInitialization(new ListenerBean(), "listenerBean");

        // @MessageListener(value="order-created") -> topic=queue="order-created" -> subscribe(topic, queue, handler)
        verify(consumer).subscribe(eq("order-created"), eq("order-created"), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void stillSupportsDeprecatedMessageHandler() {
        MessageConsumer<Object> consumer = mock(MessageConsumer.class);
        MessageHandlerProcessor processor = newProcessor(consumer);

        processor.postProcessAfterInitialization(new LegacyBean(), "legacyBean");

        verify(consumer).subscribe(eq("legacy-topic"), eq("legacy-topic"), any());
    }

    static class ListenerBean {
        @MessageListener(value = "order-created")
        public void onOrder(Message<String> message) {
            // no-op
        }
    }

    static class LegacyBean {
        @SuppressWarnings("deprecation")
        @MessageHandler(value = "legacy-topic")
        public void onLegacy(Message<String> message) {
            // no-op
        }
    }
}

package io.nebula.example.modules.messaging.handler;

import io.nebula.example.modules.messaging.config.MessageConfig;
import io.nebula.example.modules.messaging.event.delay.CustomNotificationEvent;
import io.nebula.messaging.rabbitmq.delay.DelayMessageContext;
import io.nebula.messaging.rabbitmq.delay.DelayMessageListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "nebula.messaging.rabbitmq", name = "enabled", havingValue = "true")
public class OrderTimeoutHandler {

    @DelayMessageListener(
            topic = MessageConfig.CUSTOM_DELAY_TOPIC,
            queue = MessageConfig.CUSTOM_DELAY_QUEUE)
    public void handleCustomNotification(CustomNotificationEvent event, DelayMessageContext context) {
        log.info("延迟通知处理完成: title={}, messageId={}, totalDelay={}ms",
                event.getTitle(), context.getMessageId(), context.getTotalDelay());
    }
}

package io.nebula.example.modules.messaging.controller;

import io.nebula.core.common.result.Result;
import io.nebula.example.modules.messaging.config.MessageConfig;
import io.nebula.example.modules.messaging.event.delay.CustomDelayRequest;
import io.nebula.example.modules.messaging.event.delay.CustomNotificationEvent;
import io.nebula.messaging.rabbitmq.delay.DelayMessageProducer;
import io.nebula.messaging.rabbitmq.delay.DelayMessageResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/messaging/delay")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "nebula.messaging.rabbitmq", name = "enabled", havingValue = "true")
public class DelayMessageController {

    private final DelayMessageProducer delayMessageProducer;

    @PostMapping("/custom")
    public Result<Map<String, Object>> customDelayMessage(@Valid @RequestBody CustomDelayRequest request) {
        CustomNotificationEvent event = new CustomNotificationEvent(
                request.getTitle(), request.getContent(), LocalDateTime.now());
        DelayMessageResult sendResult = delayMessageProducer.send(
                MessageConfig.CUSTOM_DELAY_TOPIC,
                MessageConfig.CUSTOM_DELAY_QUEUE,
                event,
                Duration.ofSeconds(request.getDelaySeconds()));
        if (!sendResult.isSuccess()) {
            return Result.error("DELAY_MESSAGE_SEND_FAILED", sendResult.getErrorMessage());
        }
        return Result.success(Map.of(
                "messageId", sendResult.getMessageId(),
                "delaySeconds", request.getDelaySeconds()));
    }
}

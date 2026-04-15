package io.nebula.example.modules.messaging.event.delay;

import lombok.Data;

/**
 * 自定义延时请求
 */
@Data
public class CustomDelayRequest {
    private String title;
    private String content;
    private int delaySeconds;
}


package io.nebula.example.modules.messaging.event.delay;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 自定义通知事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomNotificationEvent implements Serializable {
    private String title;
    private String content;
    private LocalDateTime createTime;
}


package io.nebula.example.modules.messaging.event.delay;

import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 自定义延时请求
 */
@Data
public class CustomDelayRequest {
    @NotBlank(message = "标题不能为空")
    private String title;
    @NotBlank(message = "内容不能为空")
    private String content;
    @Min(value = 1, message = "延迟时间不能小于1秒")
    private int delaySeconds;
}

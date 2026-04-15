package io.nebula.example.modules.messaging.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息发送结果VO
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "消息发送结果")
public class MessageResultVo {
    
    @Schema(description = "消息ID", example = "MSG_1704672000000_123")
    private String messageId;
    
    @Schema(description = "主题", example = "order.notification")
    private String topic;
    
    @Schema(description = "队列", example = "order-notification-queue")
    private String queue;
    
    @Schema(description = "发送状态", example = "true")
    private Boolean success;
    
    @Schema(description = "时间戳", example = "1704672000000")
    private Long timestamp;
    
    @Schema(description = "发送耗时（毫秒）", example = "15")
    private Long elapsedTime;
    
    @Schema(description = "错误信息", example = "")
    private String errorMessage;
}


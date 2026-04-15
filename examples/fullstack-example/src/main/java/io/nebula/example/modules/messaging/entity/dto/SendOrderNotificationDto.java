package io.nebula.example.modules.messaging.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 发送订单通知接口DTO
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@Schema(description = "发送订单通知接口DTO")
public class SendOrderNotificationDto {
    
    /**
     * 发送订单通知请求
     */
    @Data
    @Schema(description = "发送订单通知请求")
    public static class Request {
        
        @Schema(description = "订单ID", example = "1001")
        @NotNull(message = "订单ID不能为空")
        private Long orderId;
        
        @Schema(description = "订单编号", example = "ORD20250108001")
        @NotBlank(message = "订单编号不能为空")
        private String orderNo;
        
        @Schema(description = "用户ID", example = "100")
        @NotNull(message = "用户ID不能为空")
        private Long userId;
        
        @Schema(description = "产品名称", example = "智能手机")
        @NotBlank(message = "产品名称不能为空")
        private String productName;
        
        @Schema(description = "订单金额", example = "3999.00")
        @NotNull(message = "订单金额不能为空")
        private BigDecimal amount;
        
        @Schema(description = "订单状态", example = "CREATED", 
                allowableValues = {"CREATED", "PAID", "SHIPPED", "COMPLETED", "CANCELLED"})
        @NotBlank(message = "订单状态不能为空")
        private String status;
        
        @Schema(description = "通知类型", example = "ORDER_CREATED",
                allowableValues = {"ORDER_CREATED", "ORDER_PAID", "ORDER_SHIPPED", "ORDER_COMPLETED"})
        @NotBlank(message = "通知类型不能为空")
        private String notificationType;
    }
    
    /**
     * 发送订单通知响应
     */
    @Data
    @Schema(description = "发送订单通知响应")
    public static class Response {
        
        @Schema(description = "消息ID", example = "MSG_1704672000000_123")
        private String messageId;
        
        @Schema(description = "发送状态", example = "true")
        private Boolean success;
        
        @Schema(description = "发送耗时（毫秒）", example = "15")
        private Long elapsedTime;
        
        @Schema(description = "错误信息", example = "")
        private String errorMessage;
    }
}


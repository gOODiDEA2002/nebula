package io.nebula.example.modules.messaging.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发送订单状态更新通知接口DTO
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@Schema(description = "发送订单状态更新通知接口DTO")
public class SendOrderStatusUpdateDto {
    
    /**
     * 发送订单状态更新通知请求
     */
    @Data
    @Schema(description = "发送订单状态更新通知请求")
    public static class Request {
        
        @Schema(description = "订单ID", example = "1001")
        @NotNull(message = "订单ID不能为空")
        private Long orderId;
        
        @Schema(description = "订单编号", example = "ORD20250108001")
        @NotBlank(message = "订单编号不能为空")
        private String orderNo;
        
        @Schema(description = "原状态", example = "CREATED")
        @NotBlank(message = "原状态不能为空")
        private String oldStatus;
        
        @Schema(description = "新状态", example = "PAID")
        @NotBlank(message = "新状态不能为空")
        private String newStatus;
        
        @Schema(description = "备注", example = "用户已完成支付")
        private String remark;
        
        @Schema(description = "是否异步发送", example = "false")
        private Boolean async = false;
    }
    
    /**
     * 发送订单状态更新通知响应
     */
    @Data
    @Schema(description = "发送订单状态更新通知响应")
    public static class Response {
        
        @Schema(description = "消息ID", example = "MSG_1704672000000_456")
        private String messageId;
        
        @Schema(description = "发送状态", example = "true")
        private Boolean success;
        
        @Schema(description = "发送耗时（毫秒）", example = "12")
        private Long elapsedTime;
        
        @Schema(description = "错误信息", example = "")
        private String errorMessage;
    }
}


package io.nebula.example.modules.messaging.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 发送批量通知接口DTO
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@Schema(description = "发送批量通知接口DTO")
public class SendBatchNotificationDto {
    
    /**
     * 发送批量通知请求
     */
    @Data
    @Schema(description = "发送批量通知请求")
    public static class Request {
        
        @Schema(description = "订单通知列表")
        @NotEmpty(message = "订单通知列表不能为空")
        private List<OrderNotificationItem> notifications;
    }
    
    /**
     * 订单通知项
     */
    @Data
    @Schema(description = "订单通知项")
    public static class OrderNotificationItem {
        
        @Schema(description = "订单ID", example = "1001")
        @NotNull(message = "订单ID不能为空")
        private Long orderId;
        
        @Schema(description = "订单编号", example = "ORD20250108001")
        private String orderNo;
        
        @Schema(description = "用户ID", example = "100")
        private Long userId;
        
        @Schema(description = "产品名称", example = "智能手机")
        private String productName;
        
        @Schema(description = "订单金额", example = "3999.00")
        private BigDecimal amount;
        
        @Schema(description = "订单状态", example = "CREATED")
        private String status;
    }
    
    /**
     * 发送批量通知响应
     */
    @Data
    @Schema(description = "发送批量通知响应")
    public static class Response {
        
        @Schema(description = "总数", example = "10")
        private Integer totalCount;
        
        @Schema(description = "成功数", example = "9")
        private Integer successCount;
        
        @Schema(description = "失败数", example = "1")
        private Integer failedCount;
        
        @Schema(description = "发送耗时（毫秒）", example = "125")
        private Long elapsedTime;
        
        @Schema(description = "失败的消息ID列表")
        private List<String> failedMessageIds;
    }
}


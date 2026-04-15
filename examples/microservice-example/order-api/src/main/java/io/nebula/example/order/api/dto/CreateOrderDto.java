package io.nebula.example.order.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建订单DTO
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public class CreateOrderDto {
    
    /**
     * 创建订单请求
     */
    @Data
    public static class Request {
        /**
         * 用户ID
         */
        @NotNull(message = "用户ID不能为空")
        private Long userId;
        
        /**
         * 商品名称
         */
        @NotNull(message = "商品名称不能为空")
        private String productName;
        
        /**
         * 数量
         */
        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量必须大于0")
        private Integer quantity;
        
        /**
         * 单价
         */
        @NotNull(message = "单价不能为空")
        @Min(value = 0, message = "单价不能为负数")
        private BigDecimal price;
    }
    
    /**
     * 创建订单响应
     */
    @Data
    public static class Response {
        /**
         * 订单ID
         */
        private Long orderId;
        
        /**
         * 订单号
         */
        private String orderNo;
        
        /**
         * 订单总金额
         */
        private BigDecimal totalAmount;
    }
}


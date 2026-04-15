package io.nebula.example.order.api.dto;

import io.nebula.example.order.api.vo.OrderVo;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 获取订单详情DTO
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public class GetOrderDto {
    
    /**
     * 获取订单详情请求
     */
    @Data
    public static class Request {
        /**
         * 订单ID
         */
        @NotNull(message = "订单ID不能为空")
        private Long orderId;
    }
    
    /**
     * 获取订单详情响应
     */
    @Data
    public static class Response {
        /**
         * 订单信息
         */
        private OrderVo order;
    }
}


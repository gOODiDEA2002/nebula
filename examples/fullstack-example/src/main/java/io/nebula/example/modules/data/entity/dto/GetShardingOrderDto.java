package io.nebula.example.modules.data.entity.dto;

import io.nebula.example.modules.data.entity.vo.OrderVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 获取订单详情（分片演示）接口DTO
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class GetShardingOrderDto {

    @Data
    @Schema(description = "获取订单详情（分片演示）请求")
    public static class Request {
        @Schema(description = "订单ID", example = "1851234567890123456")
        @NotNull(message = "订单ID不能为空")
        private Long orderId;

        @Schema(description = "用户ID (分库键)", example = "100")
        @NotNull(message = "用户ID不能为空")
        private Long userId;
    }

    @Data
    @Schema(description = "获取订单详情（分片演示）响应")
    public static class Response {
        @Schema(description = "订单信息")
        private OrderVo order;
    }
}

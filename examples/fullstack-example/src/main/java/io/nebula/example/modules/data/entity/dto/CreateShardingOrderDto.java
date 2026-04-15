package io.nebula.example.modules.data.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 创建订单（分片演示）接口DTO
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class CreateShardingOrderDto {

    @Data
    @Schema(description = "创建订单（分片演示）请求")
    public static class Request {
        @Schema(description = "用户ID (分库键)", example = "100")
        @NotNull(message = "用户ID不能为空")
        private Long userId;

        @Schema(description = "订单ID (可选, 不填则由ShardingSphere自动生成)", example = "1851234567890123456")
        private Long orderId;

        @Schema(description = "产品名称", example = "分片测试产品A")
        @NotBlank(message = "产品名称不能为空")
        private String productName;

        @Schema(description = "订单金额", example = "299.99")
        @NotNull(message = "订单金额不能为空")
        @DecimalMin(value = "0.01", message = "订单金额必须大于0")
        private BigDecimal amount;

        @Schema(description = "订单状态", example = "PENDING", allowableValues = {"PENDING", "PAID", "SHIPPED", "COMPLETED", "CANCELLED"})
        @NotBlank(message = "订单状态不能为空")
        private String status;
    }

    @Data
    @Schema(description = "创建订单（分片演示）响应")
    public static class Response {
        @Schema(description = "创建的订单ID", example = "1851234567890123456")
        private Long orderId;
    }
}

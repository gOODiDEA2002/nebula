package io.nebula.example.modules.data.entity.dto;

import io.nebula.example.modules.data.entity.vo.OrderVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 获取订单列表（分片演示）接口DTO
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class GetShardingOrdersDto {

    @Data
    @Schema(description = "获取订单列表（分片演示）请求")
    public static class Request {
        @Schema(description = "用户ID (分库键)", example = "100")
        @NotNull(message = "用户ID不能为空")
        private Long userId;

        @Schema(description = "页码", example = "1")
        @NotNull(message = "页码不能为空")
        @Min(value = 1, message = "页码必须大于0")
        private Integer page;

        @Schema(description = "每页大小", example = "10")
        @NotNull(message = "每页大小不能为空")
        @Min(value = 1, message = "每页大小必须大于0")
        @Max(value = 100, message = "每页大小不能超过100")
        private Integer size;
    }

    @Data
    @Schema(description = "获取订单列表（分片演示）响应")
    public static class Response {
        @Schema(description = "订单列表")
        private List<OrderVo> orders;

        @Schema(description = "总记录数", example = "25")
        private Long total;

        @Schema(description = "当前页", example = "1")
        private Integer page;

        @Schema(description = "每页大小", example = "10")
        private Integer size;
    }
}

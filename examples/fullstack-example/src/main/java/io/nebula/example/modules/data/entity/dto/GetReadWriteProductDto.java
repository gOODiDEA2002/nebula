package io.nebula.example.modules.data.entity.dto;

import io.nebula.example.modules.data.entity.vo.ProductVo;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 获取产品详情（读写分离演示）接口DTO
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class GetReadWriteProductDto {

    /**
     * 获取产品详情（读写分离演示）请求
     */
    @Data
    public static class Request {
        /**
         * 产品ID
         */
        @NotNull(message = "产品ID不能为空")
        private Long id;
    }

    /**
     * 获取产品详情（读写分离演示）响应
     */
    @Data
    public static class Response {
        /**
         * 产品信息
         */
        private ProductVo product;
    }
}

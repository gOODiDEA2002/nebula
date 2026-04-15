package io.nebula.example.modules.search.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 索引单个产品DTO
 *
 * @author nebula
 */
@Data
public class IndexProductDto {

    /**
     * 索引单个产品请求
     */
    @Data
    public static class Request {
        /**
         * 产品ID
         */
        @NotNull(message = "产品ID不能为空")
        private Long productId;
    }

    /**
     * 索引单个产品响应
     */
    @Data
    public static class Response {
        /**
         * 是否索引成功
         */
        private Boolean success;
        
        /**
         * 索引名称
         */
        private String index;
        
        /**
         * 文档ID
         */
        private String id;
        
        /**
         * 错误信息
         */
        private String errorMessage;
    }
}


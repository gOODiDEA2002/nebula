package io.nebula.example.modules.search.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 删除产品索引DTO
 *
 * @author nebula
 */
@Data
public class DeleteProductIndexDto {

    /**
     * 删除产品索引请求
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
     * 删除产品索引响应
     */
    @Data
    public static class Response {
        /**
         * 是否删除成功
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

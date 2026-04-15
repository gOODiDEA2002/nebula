package io.nebula.example.modules.search.entity.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量索引产品DTO
 *
 * @author nebula
 */
@Data
public class BulkIndexProductsDto {

    /**
     * 批量索引产品请求
     */
    @Data
    public static class Request {
        /**
         * 产品ID列表
         */
        @NotEmpty(message = "产品ID列表不能为空")
        private List<Long> productIds;
    }

    /**
     * 批量索引产品响应
     */
    @Data
    public static class Response {
        /**
         * 是否全部成功
         */
        private Boolean success;
        
        /**
         * 总数
         */
        private Integer totalCount;
        
        /**
         * 成功数
         */
        private Integer successCount;
        
        /**
         * 失败数
         */
        private Integer failureCount;
        
        /**
         * 错误信息列表
         */
        private List<String> errors;
    }
}


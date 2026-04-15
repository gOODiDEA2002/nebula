package io.nebula.example.modules.search.entity.dto;

import lombok.Data;

/**
 * 检查索引是否存在DTO
 *
 * @author nebula
 */
@Data
public class IndexExistsDto {

    /**
     * 检查索引是否存在请求
     */
    @Data
    public static class Request {
        /**
         * 索引名称(可选,默认为"products")
         */
        private String indexName;
    }

    /**
     * 检查索引是否存在响应
     */
    @Data
    public static class Response {
        /**
         * 索引是否存在
         */
        private Boolean exists;
        
        /**
         * 检查的索引名称
         */
        private String indexName;
        
        /**
         * 错误信息(查询失败时填充)
         */
        private String errorMessage;
    }
}


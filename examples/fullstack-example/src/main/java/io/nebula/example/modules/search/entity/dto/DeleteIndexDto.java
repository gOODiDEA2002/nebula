package io.nebula.example.modules.search.entity.dto;

import lombok.Data;

/**
 * 删除索引DTO
 *
 * @author nebula
 */
@Data
public class DeleteIndexDto {

    /**
     * 删除索引请求
     */
    @Data
    public static class Request {
        /**
         * 索引名称(可选,默认为"products")
         */
        private String indexName;
    }

    /**
     * 删除索引响应
     */
    @Data
    public static class Response {
        /**
         * 是否删除成功
         */
        private Boolean success;
        
        /**
         * 被删除的索引名称
         */
        private String indexName;
        
        /**
         * 错误信息(失败时填充)
         */
        private String errorMessage;
    }
}


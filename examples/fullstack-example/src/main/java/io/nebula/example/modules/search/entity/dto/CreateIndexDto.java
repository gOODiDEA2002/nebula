package io.nebula.example.modules.search.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建索引DTO
 *
 * @author nebula
 */
@Data
public class CreateIndexDto {

    /**
     * 创建索引请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        /**
         * 索引名称(可选,默认为"products")
         * 用于演示自定义索引名称的场景
         */
        private String indexName;
        
        /**
         * 分片数(可选,默认为1)
         */
        @Builder.Default
        private Integer shards = 1;
        
        /**
         * 副本数(可选,默认为0,单节点环境建议为0)
         */
        @Builder.Default
        private Integer replicas = 0;
    }

    /**
     * 创建索引响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        /**
         * 是否创建成功
         */
        private Boolean success;
        
        /**
         * 索引名称
         */
        private String indexName;
        
        /**
         * 错误信息(失败时填充)
         */
        private String errorMessage;
    }
}


package io.nebula.example.modules.cache.entity.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

/**
 * 缓存键查询接口DTO
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class CacheKeysDto {

    /**
     * 缓存键查询请求
     */
    @Data
    @Schema(description = "缓存键查询请求")
    public static class Request {
        
        @Schema(description = "匹配模式（支持*通配符）", example = "user:*")
        @NotBlank(message = "匹配模式不能为空")
        private String pattern;
    }

    /**
     * 缓存键查询响应
     */
    @Data
    @Schema(description = "缓存键查询响应")
    public static class Response {
        
        @Schema(description = "匹配模式", example = "user:*")
        private String pattern;
        
        @Schema(description = "匹配的键列表")
        private Set<String> keys;
        
        @Schema(description = "匹配的键数量", example = "5")
        private Integer count;
    }
}

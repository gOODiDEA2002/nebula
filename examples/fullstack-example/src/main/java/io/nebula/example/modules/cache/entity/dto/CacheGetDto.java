package io.nebula.example.modules.cache.entity.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 缓存获取接口DTO
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class CacheGetDto {

    /**
     * 缓存获取请求
     */
    @Data
    @Schema(description = "缓存获取请求")
    public static class Request {
        
        @Schema(description = "缓存键", example = "user:123")
        @NotBlank(message = "缓存键不能为空")
        private String key;
        
        @Schema(description = "值类型", example = "String", allowableValues = {"String", "Integer", "Long", "Object"})
        private String valueType = "String";
    }

    /**
     * 缓存获取响应
     */
    @Data
    @Schema(description = "缓存获取响应")
    public static class Response {
        
        @Schema(description = "缓存键", example = "user:123")
        private String key;
        
        @Schema(description = "缓存值")
        private Object value;
        
        @Schema(description = "是否存在", example = "true")
        private Boolean exists;
        
        @Schema(description = "剩余过期时间（秒）", example = "280")
        private Long remainingTtlSeconds;
        
        @Schema(description = "缓存来源", example = "L1", allowableValues = {"L1", "L2", "MISS"})
        private String source;
    }
}

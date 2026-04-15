package io.nebula.example.modules.cache.entity.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 缓存设置接口DTO
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class CacheSetDto {

    /**
     * 缓存设置请求
     */
    @Data
    @Schema(description = "缓存设置请求")
    public static class Request {
        
        @Schema(description = "缓存键", example = "user:123")
        @NotBlank(message = "缓存键不能为空")
        private String key;
        
        @Schema(description = "缓存值", example = "张三")
        @NotNull(message = "缓存值不能为空")
        private Object value;
        
        @Schema(description = "过期时间（秒）", example = "300")
        @Min(value = 1, message = "过期时间必须大于0")
        private Integer ttlSeconds;
    }

    /**
     * 缓存设置响应
     */
    @Data
    @Schema(description = "缓存设置响应")
    public static class Response {
        
        @Schema(description = "是否设置成功", example = "true")
        private Boolean success;
        
        @Schema(description = "缓存键", example = "user:123")
        private String key;
        
        @Schema(description = "设置的值")
        private Object value;
        
        @Schema(description = "过期时间（秒）", example = "300")
        private Integer ttlSeconds;
    }
}

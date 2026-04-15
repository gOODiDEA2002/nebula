package io.nebula.example.modules.cache.entity.dto;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 缓存删除接口DTO
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class CacheDeleteDto {

    /**
     * 缓存删除请求
     */
    @Data
    @Schema(description = "缓存删除请求")
    public static class Request {
        
        @Schema(description = "要删除的缓存键列表", example = "[\"user:123\", \"user:456\"]")
        @NotEmpty(message = "缓存键列表不能为空")
        private List<String> keys;
    }

    /**
     * 缓存删除响应
     */
    @Data
    @Schema(description = "缓存删除响应")
    public static class Response {
        
        @Schema(description = "删除的键数量", example = "2")
        private Long deletedCount;
        
        @Schema(description = "请求删除的键列表")
        private List<String> requestedKeys;
        
        @Schema(description = "实际删除的键列表")
        private List<String> deletedKeys;
    }
}

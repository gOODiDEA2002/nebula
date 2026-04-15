package io.nebula.example.modules.cache.entity.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 缓存统计接口DTO
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class CacheStatsDto {

    /**
     * 缓存统计响应
     */
    @Data
    @Schema(description = "缓存统计响应")
    public static class Response {
        
        @Schema(description = "缓存名称", example = "MultiLevelCache[L1:LocalCache,L2:DefaultCache]")
        private String cacheName;
        
        @Schema(description = "总命中次数", example = "150")
        private Long hitCount;
        
        @Schema(description = "总未命中次数", example = "50")
        private Long missCount;
        
        @Schema(description = "命中率", example = "0.75")
        private Double hitRate;
        
        @Schema(description = "缓存大小", example = "1024")
        private Long size;
        
        @Schema(description = "驱逐次数", example = "10")
        private Long evictionCount;
        
        @Schema(description = "缓存是否可用", example = "true")
        private Boolean available;
        
        // 多级缓存特有统计
        @Schema(description = "L1缓存命中次数", example = "100")
        private Long l1HitCount;
        
        @Schema(description = "L2缓存命中次数", example = "50")
        private Long l2HitCount;
        
        @Schema(description = "L1缓存命中率", example = "0.50")
        private Double l1HitRate;
        
        @Schema(description = "L2缓存命中率", example = "0.25")
        private Double l2HitRate;
        
        @Schema(description = "总请求数", example = "200")
        private Long totalRequestCount;
    }
}

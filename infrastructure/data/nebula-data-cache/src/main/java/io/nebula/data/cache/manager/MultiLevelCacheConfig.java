package io.nebula.data.cache.manager;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;

/**
 * 多级缓存配置
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@Builder
public class MultiLevelCacheConfig {
    
    /**
     * L1缓存读取是否启用
     */
    @Builder.Default
    private boolean l1ReadEnabled = true;
    
    /**
     * L1缓存写入是否启用
     */
    @Builder.Default
    private boolean l1WriteEnabled = true;
    
    /**
     * L2缓存读取是否启用
     */
    @Builder.Default
    private boolean l2ReadEnabled = true;
    
    /**
     * L2缓存写入是否启用
     */
    @Builder.Default
    private boolean l2WriteEnabled = true;
    
    /**
     * L1缓存回写是否启用
     * 当L2命中时，是否将数据写回L1
     */
    @Builder.Default
    private boolean l1WriteBackEnabled = true;
    
    /**
     * 默认TTL
     */
    @Builder.Default
    private Duration defaultTtl = Duration.ofHours(1);
    
    /**
     * L1缓存默认TTL
     */
    @Builder.Default
    private Duration l1DefaultTtl = Duration.ofMinutes(10);
    
    /**
     * L1缓存回写TTL
     */
    @Builder.Default
    private Duration l1WriteBackTtl = Duration.ofMinutes(5);
    
    /**
     * L1缓存最小TTL
     */
    @Builder.Default
    private Duration l1MinTtl = Duration.ofMinutes(1);
    
    /**
     * L1缓存TTL比例
     * L1的TTL = L2的TTL * l1TtlRatio
     */
    @Builder.Default
    private double l1TtlRatio = 0.5;
    
    /**
     * L1缓存最大条目数
     */
    @Builder.Default
    private int l1MaxSize = 10000;
    
    /**
     * L1缓存过期策略
     */
    @Builder.Default
    private L1EvictionPolicy l1EvictionPolicy = L1EvictionPolicy.LRU;
    
    /**
     * 是否启用缓存统计
     */
    @Builder.Default
    private boolean statsEnabled = true;
    
    /**
     * 是否启用缓存预热
     */
    @Builder.Default
    private boolean warmupEnabled = false;
    
    /**
     * 缓存预热线程数
     */
    @Builder.Default
    private int warmupThreads = 2;
    
    /**
     * 是否启用缓存同步
     * 当L1缓存更新时，是否同步更新L2
     */
    @Builder.Default
    private boolean syncEnabled = false;
    
    /**
     * 缓存同步超时时间
     */
    @Builder.Default
    private Duration syncTimeout = Duration.ofSeconds(5);
    
    /**
     * L1缓存驱逐策略
     */
    public enum L1EvictionPolicy {
        /**
         * 最近最少使用
         */
        LRU,
        
        /**
         * 最近最少访问
         */
        LFU,
        
        /**
         * 先进先出
         */
        FIFO,
        
        /**
         * 基于时间的过期
         */
        TIME_BASED
    }
    
    /**
     * 创建默认配置
     */
    public static MultiLevelCacheConfig defaultConfig() {
        return MultiLevelCacheConfig.builder().build();
    }
    
    /**
     * 创建高性能配置（更大的L1缓存）
     */
    public static MultiLevelCacheConfig highPerformanceConfig() {
        return MultiLevelCacheConfig.builder()
                .l1MaxSize(50000)
                .l1DefaultTtl(Duration.ofMinutes(30))
                .l1WriteBackTtl(Duration.ofMinutes(15))
                .l1TtlRatio(0.8)
                .build();
    }
    
    /**
     * 创建内存优化配置（较小的L1缓存）
     */
    public static MultiLevelCacheConfig memoryOptimizedConfig() {
        return MultiLevelCacheConfig.builder()
                .l1MaxSize(1000)
                .l1DefaultTtl(Duration.ofMinutes(5))
                .l1WriteBackTtl(Duration.ofMinutes(2))
                .l1TtlRatio(0.3)
                .build();
    }
    
    /**
     * 创建只读配置（禁用L1写入）
     */
    public static MultiLevelCacheConfig readOnlyConfig() {
        return MultiLevelCacheConfig.builder()
                .l1WriteEnabled(false)
                .l1WriteBackEnabled(false)
                .syncEnabled(false)
                .build();
    }
    
    /**
     * 创建L2优先配置（更多依赖L2缓存）
     */
    public static MultiLevelCacheConfig l2PreferredConfig() {
        return MultiLevelCacheConfig.builder()
                .l1MaxSize(5000)
                .l1DefaultTtl(Duration.ofMinutes(5))
                .l1WriteBackEnabled(false)
                .l1TtlRatio(0.2)
                .build();
    }
    
    /**
     * 验证配置的有效性
     */
    public void validate() {
        if (defaultTtl.isNegative() || defaultTtl.isZero()) {
            throw new IllegalArgumentException("Default TTL must be positive");
        }
        
        if (l1DefaultTtl.isNegative() || l1DefaultTtl.isZero()) {
            throw new IllegalArgumentException("L1 default TTL must be positive");
        }
        
        if (l1WriteBackTtl.isNegative() || l1WriteBackTtl.isZero()) {
            throw new IllegalArgumentException("L1 write back TTL must be positive");
        }
        
        if (l1MinTtl.isNegative() || l1MinTtl.isZero()) {
            throw new IllegalArgumentException("L1 min TTL must be positive");
        }
        
        if (l1TtlRatio <= 0 || l1TtlRatio > 1) {
            throw new IllegalArgumentException("L1 TTL ratio must be between 0 and 1");
        }
        
        if (l1MaxSize <= 0) {
            throw new IllegalArgumentException("L1 max size must be positive");
        }
        
        if (warmupThreads <= 0) {
            throw new IllegalArgumentException("Warmup threads must be positive");
        }
        
        if (syncTimeout.isNegative() || syncTimeout.isZero()) {
            throw new IllegalArgumentException("Sync timeout must be positive");
        }
    }
    
    /**
     * 复制配置
     */
    public MultiLevelCacheConfig copy() {
        return MultiLevelCacheConfig.builder()
                .l1ReadEnabled(l1ReadEnabled)
                .l1WriteEnabled(l1WriteEnabled)
                .l2ReadEnabled(l2ReadEnabled)
                .l2WriteEnabled(l2WriteEnabled)
                .l1WriteBackEnabled(l1WriteBackEnabled)
                .defaultTtl(defaultTtl)
                .l1DefaultTtl(l1DefaultTtl)
                .l1WriteBackTtl(l1WriteBackTtl)
                .l1MinTtl(l1MinTtl)
                .l1TtlRatio(l1TtlRatio)
                .l1MaxSize(l1MaxSize)
                .l1EvictionPolicy(l1EvictionPolicy)
                .statsEnabled(statsEnabled)
                .warmupEnabled(warmupEnabled)
                .warmupThreads(warmupThreads)
                .syncEnabled(syncEnabled)
                .syncTimeout(syncTimeout)
                .build();
    }
}

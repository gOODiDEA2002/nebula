package io.nebula.data.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 缓存配置属性类
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@ConfigurationProperties(prefix = "nebula.data.cache")
public class CacheProperties {
    
    /**
     * 是否启用缓存
     */
    private boolean enabled = true;
    
    /**
     * 缓存类型：local, redis, multi-level
     */
    private CacheType type = CacheType.LOCAL;
    
    /**
     * 默认TTL
     */
    private Duration defaultTtl = Duration.ofHours(1);
    
    /**
     * 默认最大缓存条目数
     */
    private int defaultMaxSize = 10000;
    
    /**
     * 本地缓存配置
     */
    private LocalCache local = new LocalCache();
    
    /**
     * Redis缓存配置
     */
    private RedisCache redis = new RedisCache();
    
    /**
     * 多级缓存配置
     */
    private MultiLevel multiLevel = new MultiLevel();
    
    /**
     * 缓存类型枚举
     */
    public enum CacheType {
        /**
         * 本地缓存
         */
        LOCAL("local"),
        
        /**
         * Redis缓存
         */
        REDIS("redis"),
        
        /**
         * 多级缓存
         */
        MULTI_LEVEL("multi-level");
        
        private final String value;
        
        CacheType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static CacheType fromValue(String value) {
            for (CacheType type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            return LOCAL; // 默认返回本地缓存
        }
    }
    
    /**
     * 本地缓存配置
     */
    @Data
    public static class LocalCache {
        /**
         * 是否启用
         */
        private boolean enabled = true;
        
        /**
         * 最大缓存大小
         */
        private int maxSize = 10000;
        
        /**
         * 写入后过期时间
         */
        private Duration expireAfterWrite = Duration.ofMinutes(30);
        
        /**
         * 访问后过期时间
         */
        private Duration expireAfterAccess = Duration.ofHours(1);
        
        /**
         * 初始容量
         */
        private int initialCapacity = 1000;
        
        /**
         * 清理间隔
         */
        private Duration cleanupInterval = Duration.ofMinutes(5);
        
        /**
         * 驱逐策略
         */
        private EvictionPolicy evictionPolicy = EvictionPolicy.LRU;
        
        /**
         * 是否启用统计
         */
        private boolean statsEnabled = true;
    }
    
    /**
     * Redis缓存配置
     */
    @Data
    public static class RedisCache {
        /**
         * 是否启用
         */
        private boolean enabled = false;
        
        /**
         * 数据库索引
         */
        private int database = 0;
        
        /**
         * 键前缀
         */
        private String keyPrefix = "nebula:cache:";
        
        /**
         * 序列化类型
         */
        private SerializationType serialization = SerializationType.JSON;
    }
    
    /**
     * 多级缓存配置
     */
    @Data
    public static class MultiLevel {
        /**
         * 是否启用
         */
        private boolean enabled = false;
        
        /**
         * 本地缓存是否启用
         */
        private boolean localCacheEnabled = true;
        
        /**
         * 远程缓存是否启用
         */
        private boolean remoteCacheEnabled = true;
        
        /**
         * 更新时是否同步到所有缓存层
         */
        private boolean syncOnUpdate = true;
        
        /**
         * L1缓存回写是否启用
         */
        private boolean l1WriteBackEnabled = true;
        
        /**
         * L1缓存默认TTL
         */
        private Duration l1DefaultTtl = Duration.ofMinutes(10);
        
        /**
         * L1缓存回写TTL
         */
        private Duration l1WriteBackTtl = Duration.ofMinutes(5);
        
        /**
         * L1缓存TTL比例
         */
        private double l1TtlRatio = 0.5;
        
        /**
         * L1缓存最大条目数
         */
        private int l1MaxSize = 10000;
    }
    
    /**
     * 驱逐策略
     */
    public enum EvictionPolicy {
        LRU, LFU, FIFO
    }
    
    /**
     * 序列化类型
     */
    public enum SerializationType {
        JSON, JDK, KRYO
    }
}

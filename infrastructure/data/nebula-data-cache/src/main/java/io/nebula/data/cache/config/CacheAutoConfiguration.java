package io.nebula.data.cache.config;

import io.nebula.data.cache.manager.CacheManager;
import io.nebula.data.cache.manager.MultiLevelCacheConfig;
import io.nebula.data.cache.manager.MultiLevelCacheManager;
import io.nebula.data.cache.manager.impl.DefaultCacheManager;
import io.nebula.data.cache.manager.impl.LocalCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

/**
 * 缓存自动配置类
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@AutoConfiguration
@EnableCaching
@ConditionalOnProperty(prefix = "nebula.data.cache", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration {
    
    /**
     * 本地缓存管理器
     */
    @Bean("localCacheManager")
    @Primary
    @ConditionalOnProperty(prefix = "nebula.data.cache", name = "type", havingValue = "local", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "localCacheManager")
    public CacheManager localCacheManager(CacheProperties properties) {
        log.info("Configuring Local Cache Manager");
        
        CacheProperties.LocalCache localConfig = properties.getLocal();
        
        LocalCacheManager.LocalCacheConfig config = new LocalCacheManager.LocalCacheConfig(
                localConfig.getMaxSize(),
                localConfig.getInitialCapacity(),
                localConfig.getExpireAfterWrite(),
                localConfig.getCleanupInterval(),
                convertEvictionPolicy(localConfig.getEvictionPolicy())
        );
        
        return new LocalCacheManager(config);
    }
    
    /**
     * Redis缓存管理器
     */
    @Bean("redisCacheManager")
    @Primary
    @ConditionalOnProperty(prefix = "nebula.data.cache", name = "type", havingValue = "redis")
    @ConditionalOnClass(RedisTemplate.class)
    @ConditionalOnMissingBean(name = "redisCacheManager")
    public CacheManager redisCacheManager(RedisTemplate<String, Object> redisTemplate) {
        log.info("Configuring Redis Cache Manager");
        return new DefaultCacheManager(redisTemplate);
    }
    
    /**
     * 多级缓存管理器
     */
    @Bean("multiLevelCacheManager")
    @Primary
    @ConditionalOnProperty(prefix = "nebula.data.cache", name = "type", havingValue = "multi-level")
    @ConditionalOnClass(RedisTemplate.class)
    @ConditionalOnMissingBean(name = "multiLevelCacheManager")
    public CacheManager multiLevelCacheManager(
            CacheProperties properties,
            RedisTemplate<String, Object> redisTemplate) {
        
        log.info("Configuring Multi-Level Cache Manager");
        
        // 创建L1本地缓存
        CacheProperties.LocalCache localConfig = properties.getLocal();
        LocalCacheManager.LocalCacheConfig l1Config = new LocalCacheManager.LocalCacheConfig(
                localConfig.getMaxSize(),
                localConfig.getInitialCapacity(),
                localConfig.getExpireAfterWrite(),
                localConfig.getCleanupInterval(),
                convertEvictionPolicy(localConfig.getEvictionPolicy())
        );
        CacheManager l1Cache = new LocalCacheManager(l1Config);
        
        // 创建L2远程缓存
        CacheManager l2Cache = new DefaultCacheManager(redisTemplate);
        
        // 创建多级缓存配置
        CacheProperties.MultiLevel multiConfig = properties.getMultiLevel();
        MultiLevelCacheConfig config = MultiLevelCacheConfig.builder()
                .l1ReadEnabled(multiConfig.isLocalCacheEnabled())
                .l1WriteEnabled(multiConfig.isLocalCacheEnabled())
                .l2ReadEnabled(multiConfig.isRemoteCacheEnabled())
                .l2WriteEnabled(multiConfig.isRemoteCacheEnabled())
                .l1WriteBackEnabled(multiConfig.isL1WriteBackEnabled())
                .defaultTtl(properties.getDefaultTtl())
                .l1DefaultTtl(multiConfig.getL1DefaultTtl())
                .l1WriteBackTtl(multiConfig.getL1WriteBackTtl())
                .l1TtlRatio(multiConfig.getL1TtlRatio())
                .l1MaxSize(multiConfig.getL1MaxSize())
                .syncEnabled(multiConfig.isSyncOnUpdate())
                .build();
        
        return new MultiLevelCacheManager(l1Cache, l2Cache, config);
    }
    
    /**
     * 默认缓存管理器（当没有其他缓存管理器时使用本地缓存）
     */
    @Bean("defaultCacheManager")
    @Primary
    @ConditionalOnMissingBean(name = {"localCacheManager", "redisCacheManager", "multiLevelCacheManager"})
    public CacheManager defaultCacheManager(CacheProperties properties) {
        log.info("Configuring Default Cache Manager (Local Cache - Fallback)");
        
        CacheProperties.LocalCache localConfig = properties.getLocal();
        
        LocalCacheManager.LocalCacheConfig config = new LocalCacheManager.LocalCacheConfig(
                localConfig.getMaxSize(),
                localConfig.getInitialCapacity(),
                localConfig.getExpireAfterWrite(),
                localConfig.getCleanupInterval(),
                convertEvictionPolicy(localConfig.getEvictionPolicy())
        );
        
        return new LocalCacheManager(config);
    }
    
    /**
     * 多级缓存配置Bean
     */
    @Bean
    @ConditionalOnProperty(prefix = "nebula.data.cache", name = "type", havingValue = "multi-level")
    @ConditionalOnMissingBean(MultiLevelCacheConfig.class)
    public MultiLevelCacheConfig multiLevelCacheConfig(CacheProperties properties) {
        CacheProperties.MultiLevel multiConfig = properties.getMultiLevel();
        
        return MultiLevelCacheConfig.builder()
                .l1ReadEnabled(multiConfig.isLocalCacheEnabled())
                .l1WriteEnabled(multiConfig.isLocalCacheEnabled())
                .l2ReadEnabled(multiConfig.isRemoteCacheEnabled())
                .l2WriteEnabled(multiConfig.isRemoteCacheEnabled())
                .l1WriteBackEnabled(multiConfig.isL1WriteBackEnabled())
                .defaultTtl(properties.getDefaultTtl())
                .l1DefaultTtl(multiConfig.getL1DefaultTtl())
                .l1WriteBackTtl(multiConfig.getL1WriteBackTtl())
                .l1TtlRatio(multiConfig.getL1TtlRatio())
                .l1MaxSize(multiConfig.getL1MaxSize())
                .syncEnabled(multiConfig.isSyncOnUpdate())
                .build();
    }
    
    /**
     * 转换驱逐策略
     */
    private LocalCacheManager.EvictionPolicy convertEvictionPolicy(CacheProperties.EvictionPolicy policy) {
        if (policy == null) {
            return LocalCacheManager.EvictionPolicy.LRU;
        }
        
        switch (policy) {
            case LFU:
                return LocalCacheManager.EvictionPolicy.LFU;
            case FIFO:
                return LocalCacheManager.EvictionPolicy.FIFO;
            case LRU:
            default:
                return LocalCacheManager.EvictionPolicy.LRU;
        }
    }
}

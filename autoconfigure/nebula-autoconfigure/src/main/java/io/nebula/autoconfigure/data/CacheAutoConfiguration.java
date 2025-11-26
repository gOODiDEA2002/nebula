package io.nebula.autoconfigure.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.nebula.data.cache.config.CacheProperties;
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
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

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
     * Redis连接工厂配置
     */
    @Bean("redisConnectionFactory")
    @ConditionalOnClass(RedisConnectionFactory.class)
    @ConditionalOnProperty(prefix = "nebula.data.cache", name = "type", havingValue = "redis")
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public RedisConnectionFactory redisConnectionFactory(CacheProperties properties) {
        log.info("Configuring Redis Connection Factory");
        
        CacheProperties.RedisCache redisConfig = properties.getRedis();
        
        // Redis单机配置
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisConfig.getHost());
        config.setPort(redisConfig.getPort());
        config.setDatabase(redisConfig.getDatabase());
        if (redisConfig.getPassword() != null) {
            config.setPassword(redisConfig.getPassword());
        }
        
        // Lettuce连接池配置
        @SuppressWarnings("unchecked")
        GenericObjectPoolConfig<Object> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(redisConfig.getPool().getMaxActive());
        poolConfig.setMaxIdle(redisConfig.getPool().getMaxIdle());
        poolConfig.setMinIdle(redisConfig.getPool().getMinIdle());
        poolConfig.setMaxWait(redisConfig.getPool().getMaxWait());
        
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder = 
                LettucePoolingClientConfiguration.builder()
                        .commandTimeout(redisConfig.getTimeout())
                        .poolConfig((GenericObjectPoolConfig) poolConfig);
        
        if (redisConfig.getPool().getConnectTimeout() != null) {
            builder.commandTimeout(redisConfig.getPool().getConnectTimeout());
        }
        
        LettucePoolingClientConfiguration clientConfig = builder.build();
        
        return new LettuceConnectionFactory(config, clientConfig);
    }
    
    /**
     * Redis连接工厂配置（多级缓存）
     */
    @Bean("multiLevelRedisConnectionFactory")
    @ConditionalOnClass(RedisConnectionFactory.class)
    @ConditionalOnProperty(prefix = "nebula.data.cache", name = "type", havingValue = "multi-level")
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public RedisConnectionFactory multiLevelRedisConnectionFactory(CacheProperties properties) {
        return redisConnectionFactory(properties);
    }
    
    /**
     * RedisTemplate配置
     */
    @Bean("redisTemplate")
    @ConditionalOnClass(RedisTemplate.class)
    @ConditionalOnProperty(prefix = "nebula.data.cache", name = "type", havingValue = "redis")
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("Configuring RedisTemplate with JSR310 support");
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        
        // 设置key序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // 创建支持 Java 8 日期时间类型并启用类型信息的 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        
        // 设置value序列化器（支持 LocalDateTime 等 Java 8 时间类型，保留类型信息）
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
    
    /**
     * RedisTemplate配置（多级缓存）
     */
    @Bean("multiLevelRedisTemplate")
    @ConditionalOnClass(RedisTemplate.class)
    @ConditionalOnProperty(prefix = "nebula.data.cache", name = "type", havingValue = "multi-level")
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> multiLevelRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return redisTemplate(redisConnectionFactory);
    }
    
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

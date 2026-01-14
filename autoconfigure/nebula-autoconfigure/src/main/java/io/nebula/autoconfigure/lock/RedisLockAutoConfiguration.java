package io.nebula.lock.redis;

import io.nebula.lock.LockManager;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Redis分布式锁自动配置
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@Configuration
@ConditionalOnClass({RedissonClient.class, LockManager.class})
@ConditionalOnProperty(prefix = "nebula.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(RedisLockProperties.class)
@EnableAspectJAutoProxy
public class RedisLockAutoConfiguration {
    
    /**
     * 配置 RedissonClient
     * 当配置了 nebula.lock.redis 时，创建独立的 RedissonClient
     * 否则依赖 Spring Boot 自动配置的 RedissonClient
     */
    @Bean("nebulaLockRedissonClient")
    @ConditionalOnProperty(prefix = "nebula.lock.redis", name = "host")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient nebulaLockRedissonClient(RedisLockProperties properties) {
        RedisLockProperties.RedisConfig redisConfig = properties.getRedis();
        
        log.info("配置 Nebula Lock RedissonClient: host={}, port={}, database={}", 
                redisConfig.getHost(), redisConfig.getPort(), redisConfig.getDatabase());
        
        Config config = new Config();
        
        // 单机模式配置
        String address = String.format("redis://%s:%d", redisConfig.getHost(), redisConfig.getPort());
        SingleServerConfig serverConfig = config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisConfig.getDatabase())
                .setConnectionMinimumIdleSize(redisConfig.getConnectionMinimumIdleSize())
                .setConnectionPoolSize(redisConfig.getConnectionPoolSize())
                .setTimeout(redisConfig.getTimeout());
        
        // 设置密码（如果有）
        if (redisConfig.getPassword() != null && !redisConfig.getPassword().isEmpty()) {
            serverConfig.setPassword(redisConfig.getPassword());
        }
        
        return Redisson.create(config);
    }
    
    /**
     * 配置Redis锁管理器
     */
    @Bean
    @ConditionalOnMissingBean(LockManager.class)
    public RedisLockManager redisLockManager(RedissonClient redissonClient) {
        log.info("初始化Redis锁管理器");
        return new RedisLockManager(redissonClient);
    }
    
    /**
     * 配置@Locked注解切面
     */
    @Bean
    @ConditionalOnMissingBean(LockedAspect.class)
    @ConditionalOnProperty(prefix = "nebula.lock", name = "enable-aspect", havingValue = "true", matchIfMissing = true)
    public LockedAspect lockedAspect(LockManager lockManager) {
        log.info("初始化@Locked注解切面");
        return new LockedAspect(lockManager);
    }
}


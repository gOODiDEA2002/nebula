package io.nebula.lock.redis;

import io.nebula.lock.LockManager;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
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


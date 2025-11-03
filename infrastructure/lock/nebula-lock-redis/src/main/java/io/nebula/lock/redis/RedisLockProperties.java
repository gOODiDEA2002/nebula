package io.nebula.lock.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Redis分布式锁配置属性
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@ConfigurationProperties(prefix = "nebula.lock")
public class RedisLockProperties {
    
    /**
     * 是否启用分布式锁
     * 默认true
     */
    private boolean enabled = true;
    
    /**
     * 是否启用@Locked注解切面
     * 默认true
     */
    private boolean enableAspect = true;
    
    /**
     * 默认等待锁的超时时间
     * 默认30秒
     */
    private Duration defaultWaitTime = Duration.ofSeconds(30);
    
    /**
     * 默认锁的租约时间(自动释放时间)
     * 默认60秒
     */
    private Duration defaultLeaseTime = Duration.ofSeconds(60);
    
    /**
     * 是否启用看门狗机制
     * 看门狗会自动续期,防止业务执行时间过长导致锁自动释放
     * 默认true
     */
    private boolean enableWatchdog = true;
    
    /**
     * 看门狗续期间隔
     * 默认租约时间的1/3
     */
    private Duration watchdogInterval;
    
    /**
     * 是否启用公平锁
     * 公平锁保证先来先得,但性能较差
     * 默认false(非公平锁)
     */
    private boolean fair = false;
    
    /**
     * Redlock配置(用于多Redis实例)
     */
    private RedlockConfig redlock = new RedlockConfig();
    
    /**
     * Redlock配置
     */
    @Data
    public static class RedlockConfig {
        /**
         * 是否启用Redlock
         * 默认false
         */
        private boolean enabled = false;
        
        /**
         * Redis实例地址列表
         * 格式: redis://host:port
         */
        private String[] addresses;
        
        /**
         * 最小获取锁的实例数
         * 默认 (N/2 + 1)
         */
        private Integer quorum;
    }
    
    /**
     * 获取看门狗续期间隔
     */
    public Duration getWatchdogInterval() {
        if (watchdogInterval == null && defaultLeaseTime != null) {
            return Duration.ofMillis(defaultLeaseTime.toMillis() / 3);
        }
        return watchdogInterval;
    }
}


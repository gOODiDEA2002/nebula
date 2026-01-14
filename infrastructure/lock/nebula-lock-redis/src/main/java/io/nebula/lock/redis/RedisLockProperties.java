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
     * Redis连接配置
     * 如果配置了此项，将使用独立的 RedissonClient
     * 如果未配置，将使用 Spring Boot 自动配置的 RedissonClient
     */
    private RedisConfig redis;
    
    /**
     * Redlock配置(用于多Redis实例)
     */
    private RedlockConfig redlock = new RedlockConfig();
    
    /**
     * Redis连接配置
     */
    @Data
    public static class RedisConfig {
        /**
         * Redis服务器主机名
         */
        private String host = "localhost";
        
        /**
         * Redis服务器端口
         */
        private int port = 6379;
        
        /**
         * Redis密码
         */
        private String password;
        
        /**
         * 数据库索引
         */
        private int database = 0;
        
        /**
         * 连接超时时间(毫秒)
         */
        private int timeout = 3000;
        
        /**
         * 最小空闲连接数
         */
        private int connectionMinimumIdleSize = 5;
        
        /**
         * 连接池大小
         */
        private int connectionPoolSize = 20;
    }
    
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


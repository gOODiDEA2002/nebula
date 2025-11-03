package io.nebula.lock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

/**
 * 锁配置类
 * 
 * 封装锁的各种配置参数
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockConfig {
    
    /**
     * 等待锁的超时时间
     * 默认30秒
     */
    @Builder.Default
    private Duration waitTime = Duration.ofSeconds(30);
    
    /**
     * 锁的租约时间(自动释放时间)
     * 默认60秒
     * 
     * 注意: 如果业务执行时间超过租约时间,锁会自动释放,可能导致并发问题
     * 建议配合看门狗机制使用
     */
    @Builder.Default
    private Duration leaseTime = Duration.ofSeconds(60);
    
    /**
     * 锁类型
     * 默认可重入锁
     */
    @Builder.Default
    private LockType lockType = LockType.REENTRANT;
    
    /**
     * 锁模式
     * 默认独占模式
     */
    @Builder.Default
    private LockMode lockMode = LockMode.EXCLUSIVE;
    
    /**
     * 是否启用看门狗机制
     * 看门狗会自动续期,防止业务执行时间过长导致锁自动释放
     * 默认true
     */
    @Builder.Default
    private boolean enableWatchdog = true;
    
    /**
     * 看门狗续期间隔
     * 默认租约时间的1/3
     */
    private Duration watchdogInterval;
    
    /**
     * 是否公平锁
     * 公平锁保证先来先得,但性能较差
     * 默认false(非公平锁)
     */
    @Builder.Default
    private boolean fair = false;
    
    /**
     * 获取看门狗续期间隔
     * 如果未设置,默认为租约时间的1/3
     */
    public Duration getWatchdogInterval() {
        if (watchdogInterval == null && leaseTime != null) {
            return Duration.ofMillis(leaseTime.toMillis() / 3);
        }
        return watchdogInterval;
    }
    
    /**
     * 创建默认配置
     */
    public static LockConfig defaultConfig() {
        return LockConfig.builder().build();
    }
    
    /**
     * 创建快速失败配置(不等待)
     */
    public static LockConfig tryLockConfig() {
        return LockConfig.builder()
                .waitTime(Duration.ZERO)
                .build();
    }
    
    /**
     * 创建短时锁配置(10秒租约)
     */
    public static LockConfig shortLeaseConfig() {
        return LockConfig.builder()
                .leaseTime(Duration.ofSeconds(10))
                .build();
    }
    
    /**
     * 创建长时锁配置(5分钟租约)
     */
    public static LockConfig longLeaseConfig() {
        return LockConfig.builder()
                .leaseTime(Duration.ofMinutes(5))
                .build();
    }
}


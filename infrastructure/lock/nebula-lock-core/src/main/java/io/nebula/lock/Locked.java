package io.nebula.lock;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 * 
 * 标记在方法上,自动在方法执行前获取锁,执行后释放锁
 * 
 * 使用示例:
 * <pre>{@code
 * @Locked(key = "'seat:' + #seatId", waitTime = 10, leaseTime = 60)
 * public boolean lockSeat(Long seatId) {
 *     // 业务逻辑
 *     return true;
 * }
 * }</pre>
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Locked {
    
    /**
     * 锁的key
     * 支持SpEL表达式
     * 
     * 示例:
     * - "order:lock" - 固定key
     * - "'order:' + #orderId" - 动态key
     * - "#user.id + ':' + #order.id" - 复杂表达式
     */
    String key();
    
    /**
     * 等待锁的超时时间
     * 默认30秒
     */
    long waitTime() default 30;
    
    /**
     * 锁的租约时间(自动释放时间)
     * 默认60秒
     */
    long leaseTime() default 60;
    
    /**
     * 时间单位
     * 默认秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
    /**
     * 锁类型
     * 默认可重入锁
     */
    LockType lockType() default LockType.REENTRANT;
    
    /**
     * 是否启用看门狗机制
     * 默认true
     */
    boolean watchdog() default true;
    
    /**
     * 获取锁失败时的处理策略
     * - THROW_EXCEPTION: 抛出异常(默认)
     * - RETURN_NULL: 返回null
     * - RETURN_FALSE: 返回false(仅适用于boolean返回值)
     * - SKIP: 跳过锁,直接执行方法
     */
    FailStrategy failStrategy() default FailStrategy.THROW_EXCEPTION;
    
    /**
     * 获取锁失败时的异常消息
     * 支持SpEL表达式
     */
    String failMessage() default "Failed to acquire lock";
    
    /**
     * 获取锁失败处理策略
     */
    enum FailStrategy {
        /**
         * 抛出异常
         */
        THROW_EXCEPTION,
        
        /**
         * 返回null
         */
        RETURN_NULL,
        
        /**
         * 返回false(仅适用于boolean返回值)
         */
        RETURN_FALSE,
        
        /**
         * 跳过锁,直接执行方法
         */
        SKIP
    }
}


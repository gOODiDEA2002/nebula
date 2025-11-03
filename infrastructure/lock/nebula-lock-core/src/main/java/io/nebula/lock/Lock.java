package io.nebula.lock;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁接口
 * 
 * 定义了分布式锁的基本操作，包括获取锁、释放锁、尝试获取锁等。
 * 参考 java.util.concurrent.locks.Lock 接口设计。
 * 
 * 使用示例：
 * <pre>{@code
 * Lock lock = lockManager.getLock("order:1001");
 * try {
 *     lock.lock();
 *     // 执行业务逻辑
 * } finally {
 *     lock.unlock();
 * }
 * }</pre>
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public interface Lock {
    
    /**
     * 获取锁
     * 
     * 如果锁不可用，当前线程将被阻塞，直到获取到锁
     * 
     * @throws LockException 如果获取锁失败
     */
    void lock() throws LockException;
    
    /**
     * 获取锁（可中断）
     * 
     * 如果锁不可用，当前线程将被阻塞，直到获取到锁或被中断
     * 
     * @throws LockException 如果获取锁失败
     * @throws InterruptedException 如果线程被中断
     */
    void lockInterruptibly() throws LockException, InterruptedException;
    
    /**
     * 尝试获取锁
     * 
     * 立即返回，不阻塞。如果锁可用则获取锁并返回true，否则返回false
     * 
     * @return 是否成功获取锁
     */
    boolean tryLock();
    
    /**
     * 尝试获取锁（超时）
     * 
     * 在指定时间内尝试获取锁，超时则返回false
     * 
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 是否成功获取锁
     * @throws InterruptedException 如果线程被中断
     */
    boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException;
    
    /**
     * 尝试获取锁（Duration超时）
     * 
     * @param timeout 超时时间
     * @return 是否成功获取锁
     * @throws InterruptedException 如果线程被中断
     */
    default boolean tryLock(Duration timeout) throws InterruptedException {
        return tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }
    
    /**
     * 释放锁
     * 
     * 必须由锁的持有者调用
     * 
     * @throws LockException 如果释放锁失败
     */
    void unlock() throws LockException;
    
    /**
     * 获取锁的名称/键
     * 
     * @return 锁名称
     */
    String getKey();
    
    /**
     * 判断当前线程是否持有锁
     * 
     * @return 是否持有锁
     */
    boolean isHeldByCurrentThread();
    
    /**
     * 判断锁是否被任意线程持有
     * 
     * @return 是否被锁定
     */
    boolean isLocked();
    
    /**
     * 获取锁的剩余租约时间（毫秒）
     * 
     * @return 剩余租约时间，如果锁未被持有则返回0
     */
    long getRemainingLeaseTime();
}


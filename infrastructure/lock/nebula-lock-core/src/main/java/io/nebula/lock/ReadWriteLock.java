package io.nebula.lock;

/**
 * 读写锁接口
 * 
 * 支持多个读锁同时持有,但写锁互斥
 * 适用于读多写少的场景
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public interface ReadWriteLock {
    
    /**
     * 获取读锁
     * 
     * @return 读锁实例
     */
    Lock readLock();
    
    /**
     * 获取写锁
     * 
     * @return 写锁实例
     */
    Lock writeLock();
    
    /**
     * 获取锁的名称/键
     * 
     * @return 锁名称
     */
    String getKey();
}


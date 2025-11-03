package io.nebula.lock;

/**
 * 锁管理器接口
 * 
 * 负责创建和管理分布式锁实例
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public interface LockManager {
    
    /**
     * 获取分布式锁
     * 
     * @param key 锁的唯一标识
     * @return 锁实例
     */
    Lock getLock(String key);
    
    /**
     * 获取分布式锁（带配置）
     * 
     * @param key 锁的唯一标识
     * @param config 锁配置
     * @return 锁实例
     */
    Lock getLock(String key, LockConfig config);
    
    /**
     * 获取读写锁
     * 
     * @param key 锁的唯一标识
     * @return 读写锁实例
     */
    ReadWriteLock getReadWriteLock(String key);
    
    /**
     * 获取读写锁（带配置）
     * 
     * @param key 锁的唯一标识
     * @param config 锁配置
     * @return 读写锁实例
     */
    ReadWriteLock getReadWriteLock(String key, LockConfig config);
    
    /**
     * 释放指定的锁
     * 
     * @param key 锁的唯一标识
     */
    void releaseLock(String key);
    
    /**
     * 释放所有锁
     */
    void releaseAllLocks();
    
    /**
     * 判断锁管理器是否可用
     * 
     * @return 是否可用
     */
    boolean isAvailable();
}


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
    
    /**
     * 使用锁回调执行业务逻辑（阻塞式获取锁）
     * 
     * @param key 锁的唯一标识
     * @param callback 在持有锁期间执行的回调
     * @param <T> 回调返回值类型
     * @return 回调执行结果
     */
    <T> T execute(String key, LockCallback<T> callback);
    
    /**
     * 使用锁回调执行业务逻辑（阻塞式获取锁，带配置）
     * 
     * @param key 锁的唯一标识
     * @param config 锁配置
     * @param callback 在持有锁期间执行的回调
     * @param <T> 回调返回值类型
     * @return 回调执行结果
     */
    <T> T execute(String key, LockConfig config, LockCallback<T> callback);
    
    /**
     * 尝试使用锁执行业务逻辑（非阻塞，获取锁失败返回 null）
     * 
     * @param key 锁的唯一标识
     * @param callback 在持有锁期间执行的回调
     * @param <T> 回调返回值类型
     * @return 回调执行结果，获取锁失败时返回 null
     */
    <T> T tryExecute(String key, LockCallback<T> callback);
    
    /**
     * 尝试使用锁执行业务逻辑（非阻塞，带配置）
     * 
     * @param key 锁的唯一标识
     * @param config 锁配置
     * @param callback 在持有锁期间执行的回调
     * @param <T> 回调返回值类型
     * @return 回调执行结果，获取锁失败时返回 null
     */
    <T> T tryExecute(String key, LockConfig config, LockCallback<T> callback);
}


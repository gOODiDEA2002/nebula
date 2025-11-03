package io.nebula.lock.redis;

import io.nebula.lock.*;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis锁管理器
 * 
 * 负责创建和管理Redis锁实例
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@Component
public class RedisLockManager implements LockManager {
    
    private final RedissonClient redissonClient;
    private final Map<String, Lock> lockCache = new ConcurrentHashMap<>();
    private final Map<String, ReadWriteLock> rwLockCache = new ConcurrentHashMap<>();
    
    public RedisLockManager(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        log.info("RedisLockManager initialized with Redisson client");
    }
    
    @Override
    public Lock getLock(String key) {
        return getLock(key, LockConfig.defaultConfig());
    }
    
    @Override
    public Lock getLock(String key, LockConfig config) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Lock key cannot be null or empty");
        }
        
        // 根据锁类型创建不同的锁
        if (config != null && config.getLockType() == LockType.FAIR) {
            // 公平锁
            RLock rLock = redissonClient.getFairLock(key);
            return new RedisLock(rLock, key, config);
        } else {
            // 默认可重入锁
            RLock rLock = redissonClient.getLock(key);
            return new RedisLock(rLock, key, config);
        }
    }
    
    @Override
    public ReadWriteLock getReadWriteLock(String key) {
        return getReadWriteLock(key, LockConfig.defaultConfig());
    }
    
    @Override
    public ReadWriteLock getReadWriteLock(String key, LockConfig config) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Lock key cannot be null or empty");
        }
        
        return rwLockCache.computeIfAbsent(key, k -> {
            RReadWriteLock rRwLock = redissonClient.getReadWriteLock(k);
            return new RedisReadWriteLock(rRwLock, k, config);
        });
    }
    
    @Override
    public void releaseLock(String key) {
        Lock lock = lockCache.remove(key);
        if (lock != null && lock.isHeldByCurrentThread()) {
            try {
                lock.unlock();
                log.debug("释放并移除锁: key={}", key);
            } catch (Exception e) {
                log.error("释放锁失败: key={}", key, e);
            }
        }
    }
    
    @Override
    public void releaseAllLocks() {
        log.info("释放所有锁, 总数: {}", lockCache.size());
        lockCache.forEach((key, lock) -> {
            if (lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                    log.debug("释放锁: key={}", key);
                } catch (Exception e) {
                    log.error("释放锁失败: key={}", key, e);
                }
            }
        });
        lockCache.clear();
        rwLockCache.clear();
    }
    
    @Override
    public boolean isAvailable() {
        try {
            return redissonClient != null && !redissonClient.isShutdown();
        } catch (Exception e) {
            log.error("检查锁管理器可用性失败", e);
            return false;
        }
    }
    
    /**
     * 使用锁回调执行业务逻辑
     * 
     * @param key 锁key
     * @param callback 回调函数
     * @param <T> 返回值类型
     * @return 执行结果
     */
    public <T> T execute(String key, LockCallback<T> callback) {
        return execute(key, LockConfig.defaultConfig(), callback);
    }
    
    /**
     * 使用锁回调执行业务逻辑（带配置）
     * 
     * @param key 锁key
     * @param config 锁配置
     * @param callback 回调函数
     * @param <T> 返回值类型
     * @return 执行结果
     */
    public <T> T execute(String key, LockConfig config, LockCallback<T> callback) {
        Lock lock = getLock(key, config);
        try {
            lock.lock();
            return callback.execute();
        } catch (Exception e) {
            log.error("执行锁回调失败: key={}", key, e);
            throw new RuntimeException("Failed to execute with lock: " + key, e);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 尝试使用锁执行业务逻辑
     * 如果获取锁失败,返回null
     * 
     * @param key 锁key
     * @param callback 回调函数
     * @param <T> 返回值类型
     * @return 执行结果,获取锁失败返回null
     */
    public <T> T tryExecute(String key, LockCallback<T> callback) {
        return tryExecute(key, LockConfig.tryLockConfig(), callback);
    }
    
    /**
     * 尝试使用锁执行业务逻辑（带配置）
     * 
     * @param key 锁key
     * @param config 锁配置
     * @param callback 回调函数
     * @param <T> 返回值类型
     * @return 执行结果,获取锁失败返回null
     */
    public <T> T tryExecute(String key, LockConfig config, LockCallback<T> callback) {
        Lock lock = getLock(key, config);
        try {
            if (lock.tryLock(config.getWaitTime())) {
                try {
                    return callback.execute();
                } finally {
                    lock.unlock();
                }
            } else {
                log.debug("获取锁失败,跳过执行: key={}", key);
                return null;
            }
        } catch (Exception e) {
            log.error("尝试执行锁回调失败: key={}", key, e);
            return null;
        }
    }
    
    /**
     * 获取Redlock(红锁)
     * 需要配置多个Redis实例
     * 
     * @param key 锁key
     * @param redissonClients 多个Redisson客户端
     * @return 红锁实例
     */
    public Lock getRedLock(String key, RedissonClient... redissonClients) {
        if (redissonClients == null || redissonClients.length == 0) {
            throw new IllegalArgumentException("At least one RedissonClient is required for RedLock");
        }
        
        RLock[] locks = new RLock[redissonClients.length];
        for (int i = 0; i < redissonClients.length; i++) {
            locks[i] = redissonClients[i].getLock(key);
        }
        
        // 使用第一个客户端创建红锁
        RLock redLock = redissonClients[0].getRedLock(locks);
        return new RedisLock(redLock, key, LockConfig.defaultConfig());
    }
}


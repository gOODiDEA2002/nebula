package io.nebula.lock.redis;

import io.nebula.lock.*;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;

/**
 * Redis锁管理器
 * 
 * 负责创建和管理Redis锁实例
 * 
 * 注：通过 RedisLockAutoConfiguration 自动配置，不使用 @Component
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class RedisLockManager implements LockManager {
    
    private final RedissonClient redissonClient;
    
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
        
        RReadWriteLock rRwLock = redissonClient.getReadWriteLock(key);
        return new RedisReadWriteLock(rRwLock, key, config);
    }
    
    @Override
    public void releaseLock(String key) {
        try {
            RLock rLock = redissonClient.getLock(key);
            if (rLock.isHeldByCurrentThread()) {
                rLock.unlock();
                log.debug("释放锁: key={}", key);
            }
        } catch (Exception e) {
            log.error("释放锁失败: key={}", key, e);
        }
    }
    
    @Override
    public void releaseAllLocks() {
        log.info("releaseAllLocks 被调用，Redisson 管理的锁由各持有者自行释放");
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
    
    @Override
    public <T> T execute(String key, LockCallback<T> callback) {
        return execute(key, LockConfig.defaultConfig(), callback);
    }
    
    @Override
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
    
    @Override
    public <T> T tryExecute(String key, LockCallback<T> callback) {
        return tryExecute(key, LockConfig.tryLockConfig(), callback);
    }
    
    @Override
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


package io.nebula.lock.redis;

import io.nebula.lock.Lock;
import io.nebula.lock.LockConfig;
import io.nebula.lock.ReadWriteLock;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RReadWriteLock;

/**
 * 基于Redis的读写锁实现
 * 
 * 支持多个读锁同时持有,但写锁互斥
 * 适用于读多写少的场景
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class RedisReadWriteLock implements ReadWriteLock {
    
    private final RReadWriteLock rRwLock;
    private final String key;
    private final LockConfig config;
    private final Lock readLock;
    private final Lock writeLock;
    
    public RedisReadWriteLock(RReadWriteLock rRwLock, String key, LockConfig config) {
        this.rRwLock = rRwLock;
        this.key = key;
        this.config = config != null ? config : LockConfig.defaultConfig();
        
        // 创建读锁和写锁实例
        this.readLock = new RedisLock(rRwLock.readLock(), key + ":read", this.config);
        this.writeLock = new RedisLock(rRwLock.writeLock(), key + ":write", this.config);
        
        log.debug("创建读写锁: key={}", key);
    }
    
    @Override
    public Lock readLock() {
        return readLock;
    }
    
    @Override
    public Lock writeLock() {
        return writeLock;
    }
    
    @Override
    public String getKey() {
        return key;
    }
}


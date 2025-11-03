package io.nebula.lock.redis;

import io.nebula.lock.*;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的分布式锁实现
 * 
 * 使用Redisson作为底层实现,提供:
 * - 可重入锁
 * - 看门狗自动续期
 * - 锁等待和超时
 * - 线程安全
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class RedisLock implements Lock {
    
    private final RLock rLock;
    private final String key;
    private final LockConfig config;
    
    public RedisLock(RLock rLock, String key, LockConfig config) {
        this.rLock = rLock;
        this.key = key;
        this.config = config != null ? config : LockConfig.defaultConfig();
    }
    
    @Override
    public void lock() throws LockException {
        try {
            if (config.isEnableWatchdog()) {
                // 启用看门狗,锁会自动续期
                rLock.lock(config.getLeaseTime().toMillis(), TimeUnit.MILLISECONDS);
            } else {
                // 不启用看门狗,锁到期自动释放
                rLock.lock(config.getLeaseTime().toMillis(), TimeUnit.MILLISECONDS);
            }
            log.debug("成功获取锁: key={}, thread={}", key, Thread.currentThread().getName());
        } catch (Exception e) {
            log.error("获取锁失败: key={}, thread={}", key, Thread.currentThread().getName(), e);
            throw new LockAcquisitionException("Failed to acquire lock: " + key, e);
        }
    }
    
    @Override
    public void lockInterruptibly() throws LockException, InterruptedException {
        try {
            rLock.lockInterruptibly(config.getLeaseTime().toMillis(), TimeUnit.MILLISECONDS);
            log.debug("成功获取锁(可中断): key={}, thread={}", key, Thread.currentThread().getName());
        } catch (InterruptedException e) {
            log.warn("获取锁被中断: key={}, thread={}", key, Thread.currentThread().getName());
            throw e;
        } catch (Exception e) {
            log.error("获取锁失败(可中断): key={}, thread={}", key, Thread.currentThread().getName(), e);
            throw new LockAcquisitionException("Failed to acquire lock interruptibly: " + key, e);
        }
    }
    
    @Override
    public boolean tryLock() {
        try {
            boolean acquired = rLock.tryLock();
            if (acquired) {
                log.debug("成功尝试获取锁: key={}, thread={}", key, Thread.currentThread().getName());
            } else {
                log.debug("尝试获取锁失败(锁已被占用): key={}, thread={}", key, Thread.currentThread().getName());
            }
            return acquired;
        } catch (Exception e) {
            log.error("尝试获取锁异常: key={}, thread={}", key, Thread.currentThread().getName(), e);
            return false;
        }
    }
    
    @Override
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        try {
            long waitTime = timeout;
            TimeUnit waitUnit = unit;
            
            // 如果配置了等待时间,使用配置的等待时间
            if (config.getWaitTime() != null && config.getWaitTime().toMillis() > 0) {
                waitTime = config.getWaitTime().toMillis();
                waitUnit = TimeUnit.MILLISECONDS;
            }
            
            boolean acquired = rLock.tryLock(
                    waitTime,
                    config.getLeaseTime().toMillis(),
                    waitUnit
            );
            
            if (acquired) {
                log.debug("成功尝试获取锁(超时): key={}, waitTime={}ms, thread={}",
                        key, unit.toMillis(timeout), Thread.currentThread().getName());
            } else {
                log.debug("尝试获取锁超时: key={}, waitTime={}ms, thread={}",
                        key, unit.toMillis(timeout), Thread.currentThread().getName());
            }
            
            return acquired;
        } catch (InterruptedException e) {
            log.warn("尝试获取锁被中断: key={}, thread={}", key, Thread.currentThread().getName());
            throw e;
        } catch (Exception e) {
            log.error("尝试获取锁异常(超时): key={}, thread={}", key, Thread.currentThread().getName(), e);
            return false;
        }
    }
    
    @Override
    public void unlock() throws LockException {
        try {
            // 只有锁的持有者才能释放锁
            if (rLock.isHeldByCurrentThread()) {
                rLock.unlock();
                log.debug("成功释放锁: key={}, thread={}", key, Thread.currentThread().getName());
            } else {
                log.warn("尝试释放非当前线程持有的锁: key={}, thread={}", key, Thread.currentThread().getName());
            }
        } catch (IllegalMonitorStateException e) {
            // 尝试释放未持有的锁,记录警告但不抛出异常
            log.warn("尝试释放未持有的锁: key={}, thread={}", key, Thread.currentThread().getName());
        } catch (Exception e) {
            log.error("释放锁失败: key={}, thread={}", key, Thread.currentThread().getName(), e);
            throw new LockReleaseException("Failed to release lock: " + key, e);
        }
    }
    
    @Override
    public String getKey() {
        return key;
    }
    
    @Override
    public boolean isHeldByCurrentThread() {
        try {
            return rLock.isHeldByCurrentThread();
        } catch (Exception e) {
            log.error("检查锁持有状态失败: key={}", key, e);
            return false;
        }
    }
    
    @Override
    public boolean isLocked() {
        try {
            return rLock.isLocked();
        } catch (Exception e) {
            log.error("检查锁状态失败: key={}", key, e);
            return false;
        }
    }
    
    @Override
    public long getRemainingLeaseTime() {
        try {
            long remaining = rLock.remainTimeToLive();
            return remaining > 0 ? remaining : 0;
        } catch (Exception e) {
            log.error("获取锁剩余时间失败: key={}", key, e);
            return 0;
        }
    }
    
    /**
     * 强制释放锁(无论是否由当前线程持有)
     * 
     * 注意: 谨慎使用,可能导致并发问题
     */
    public void forceUnlock() {
        try {
            rLock.forceUnlock();
            log.warn("强制释放锁: key={}", key);
        } catch (Exception e) {
            log.error("强制释放锁失败: key={}", key, e);
        }
    }
    
    /**
     * 获取锁的持有数(重入次数)
     */
    public int getHoldCount() {
        try {
            return rLock.getHoldCount();
        } catch (Exception e) {
            log.error("获取锁持有数失败: key={}", key, e);
            return 0;
        }
    }
}


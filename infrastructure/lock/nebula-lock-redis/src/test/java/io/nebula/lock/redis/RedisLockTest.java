package io.nebula.lock.redis;

import io.nebula.lock.LockAcquisitionException;
import io.nebula.lock.LockConfig;
import io.nebula.lock.LockReleaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RedisLock单元测试
 */
@ExtendWith(MockitoExtension.class)
class RedisLockTest {
    
    @Mock
    private RLock rLock;
    
    private String lockKey = "test:lock:key";
    private LockConfig config;
    
    @BeforeEach
    void setUp() {
        config = LockConfig.builder()
                .waitTime(Duration.ofSeconds(10))
                .leaseTime(Duration.ofSeconds(30))
                .build();
    }
    
    @Test
    void testLock() {
        RedisLock lock = new RedisLock(rLock, lockKey, config);
        
        lock.lock();
        
        verify(rLock).lock(eq(30000L), eq(TimeUnit.MILLISECONDS));
    }
    
    @Test
    void testLockWithException() {
        doThrow(new RuntimeException("Redis error")).when(rLock).lock(anyLong(), any(TimeUnit.class));
        
        RedisLock lock = new RedisLock(rLock, lockKey, config);
        
        assertThatThrownBy(lock::lock)
                .isInstanceOf(LockAcquisitionException.class)
                .hasMessageContaining("Failed to acquire lock");
    }
    
    @Test
    void testLockInterruptibly() throws InterruptedException {
        RedisLock lock = new RedisLock(rLock, lockKey, config);
        
        lock.lockInterruptibly();
        
        verify(rLock).lockInterruptibly(eq(30000L), eq(TimeUnit.MILLISECONDS));
    }
    
    @Test
    void testLockInterruptiblyThrowsInterruptedException() throws InterruptedException {
        doThrow(new InterruptedException("Interrupted")).when(rLock).lockInterruptibly(anyLong(), any(TimeUnit.class));
        
        RedisLock lock = new RedisLock(rLock, lockKey, config);
        
        assertThatThrownBy(lock::lockInterruptibly)
                .isInstanceOf(InterruptedException.class);
    }
    
    @Test
    void testTryLock() {
        when(rLock.tryLock()).thenReturn(true);
        
        RedisLock lock = new RedisLock(rLock, lockKey, config);
        boolean acquired = lock.tryLock();
        
        assertThat(acquired).isTrue();
        verify(rLock).tryLock();
    }
    
    @Test
    void testTryLockFailed() {
        when(rLock.tryLock()).thenReturn(false);
        
        RedisLock lock = new RedisLock(rLock, lockKey, config);
        boolean acquired = lock.tryLock();
        
        assertThat(acquired).isFalse();
    }
    
    @Test
    void testTryLockWithTimeout() throws InterruptedException {
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        
        RedisLock lock = new RedisLock(rLock, lockKey, config);
        boolean acquired = lock.tryLock(5, TimeUnit.SECONDS);
        
        assertThat(acquired).isTrue();
        verify(rLock).tryLock(anyLong(), eq(30000L), any(TimeUnit.class));
    }
    
    @Test
    void testTryLockWithTimeoutFailed() throws InterruptedException {
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);
        
        RedisLock lock = new RedisLock(rLock, lockKey, config);
        boolean acquired = lock.tryLock(5, TimeUnit.SECONDS);
        
        assertThat(acquired).isFalse();
    }
    
    @Test
    void testTryLockInterrupted() throws InterruptedException {
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException());
        
        RedisLock lock = new RedisLock(rLock, lockKey, config);
        
        assertThatThrownBy(() -> lock.tryLock(5, TimeUnit.SECONDS))
                .isInstanceOf(InterruptedException.class);
    }
    
    @Test
    void testUnlock() {
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        
        RedisLock lock = new RedisLock(rLock, lockKey, config);
        lock.unlock();
        
        verify(rLock).unlock();
    }
    
    @Test
    void testUnlockNotHeld() {
        when(rLock.isHeldByCurrentThread()).thenReturn(false);
        
        RedisLock lock = new RedisLock(rLock, lockKey, config);
        lock.unlock();
        
        verify(rLock, never()).unlock();
    }
    
    @Test
    void testUnlockWithIllegalMonitorState() {
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        doThrow(new IllegalMonitorStateException()).when(rLock).unlock();
        
        RedisLock lock = new RedisLock(rLock, lockKey, config);
        
        // 不应该抛出异常，只记录警告
        assertThatCode(lock::unlock).doesNotThrowAnyException();
    }
    
    @Test
    void testUnlockWithException() {
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        doThrow(new RuntimeException("Redis error")).when(rLock).unlock();
        
        RedisLock lock = new RedisLock(rLock, lockKey, config);
        
        assertThatThrownBy(lock::unlock)
                .isInstanceOf(LockReleaseException.class)
                .hasMessageContaining("Failed to release lock");
    }
    
    @Test
    void testGetKey() {
        RedisLock lock = new RedisLock(rLock, lockKey, config);
        
        assertThat(lock.getKey()).isEqualTo(lockKey);
    }
    
    @Test
    void testIsHeldByCurrentThread() {
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        
        RedisLock lock = new RedisLock(rLock, lockKey, config);
        
        assertThat(lock.isHeldByCurrentThread()).isTrue();
    }
    
    @Test
    void testIsLocked() {
        when(rLock.isLocked()).thenReturn(true);
        
        RedisLock lock = new RedisLock(rLock, lockKey, config);
        
        assertThat(lock.isLocked()).isTrue();
    }
    
    @Test
    void testGetRemainingLeaseTime() {
        when(rLock.remainTimeToLive()).thenReturn(15000L);
        
        RedisLock lock = new RedisLock(rLock, lockKey, config);
        long remaining = lock.getRemainingLeaseTime();
        
        assertThat(remaining).isEqualTo(15000L);
    }
    
    @Test
    void testGetRemainingLeaseTimeExpired() {
        when(rLock.remainTimeToLive()).thenReturn(-1L);
        
        RedisLock lock = new RedisLock(rLock, lockKey, config);
        long remaining = lock.getRemainingLeaseTime();
        
        assertThat(remaining).isEqualTo(0L);
    }
    
    @Test
    void testForceUnlock() {
        RedisLock lock = new RedisLock(rLock, lockKey, config);
        
        lock.forceUnlock();
        
        verify(rLock).forceUnlock();
    }
    
    @Test
    void testGetHoldCount() {
        when(rLock.getHoldCount()).thenReturn(3);
        
        RedisLock lock = new RedisLock(rLock, lockKey, config);
        int count = lock.getHoldCount();
        
        assertThat(count).isEqualTo(3);
    }
}

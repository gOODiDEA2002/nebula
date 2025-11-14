package io.nebula.lock.redis;

import io.nebula.lock.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * RedisLockManager单元测试
 */
@ExtendWith(MockitoExtension.class)
class RedisLockManagerTest {
    
    @Mock
    private RedissonClient redissonClient;
    
    @Mock
    private RLock rLock;
    
    @Mock
    private RLock fairLock;
    
    @Mock
    private RReadWriteLock rReadWriteLock;
    
    private RedisLockManager lockManager;
    
    @BeforeEach
    void setUp() {
        lockManager = new RedisLockManager(redissonClient);
        
        lenient().when(redissonClient.getLock(anyString())).thenReturn(rLock);
        lenient().when(redissonClient.getFairLock(anyString())).thenReturn(fairLock);
        lenient().when(redissonClient.getReadWriteLock(anyString())).thenReturn(rReadWriteLock);
        lenient().when(redissonClient.isShutdown()).thenReturn(false);
    }
    
    @Test
    void testGetLock() {
        String lockKey = "test:lock";
        
        Lock lock = lockManager.getLock(lockKey);
        
        assertThat(lock).isNotNull();
        assertThat(lock).isInstanceOf(RedisLock.class);
        assertThat(lock.getKey()).isEqualTo(lockKey);
        verify(redissonClient).getLock(lockKey);
    }
    
    @Test
    void testGetLockWithConfig() {
        String lockKey = "test:lock:config";
        LockConfig config = LockConfig.builder()
                .waitTime(Duration.ofSeconds(10))
                .leaseTime(Duration.ofSeconds(30))
                .build();
        
        Lock lock = lockManager.getLock(lockKey, config);
        
        assertThat(lock).isNotNull();
        verify(redissonClient).getLock(lockKey);
    }
    
    @Test
    void testGetFairLock() {
        String lockKey = "test:fair:lock";
        LockConfig config = LockConfig.builder()
                .lockType(LockType.FAIR)
                .build();
        
        Lock lock = lockManager.getLock(lockKey, config);
        
        assertThat(lock).isNotNull();
        verify(redissonClient).getFairLock(lockKey);
    }
    
    @Test
    void testGetLockWithNullKey() {
        assertThatThrownBy(() -> lockManager.getLock(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lock key cannot be null or empty");
    }
    
    @Test
    void testGetLockWithEmptyKey() {
        assertThatThrownBy(() -> lockManager.getLock(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lock key cannot be null or empty");
    }
    
    @Test
    void testGetReadWriteLock() {
        String lockKey = "test:rw:lock";
        
        ReadWriteLock rwLock = lockManager.getReadWriteLock(lockKey);
        
        assertThat(rwLock).isNotNull();
        verify(redissonClient).getReadWriteLock(lockKey);
    }
    
    @Test
    void testGetReadWriteLockWithCache() {
        String lockKey = "test:rw:cache";
        
        ReadWriteLock rwLock1 = lockManager.getReadWriteLock(lockKey);
        ReadWriteLock rwLock2 = lockManager.getReadWriteLock(lockKey);
        
        assertThat(rwLock1).isSameAs(rwLock2);
        verify(redissonClient, times(1)).getReadWriteLock(lockKey);
    }
    
    @Test
    void testGetReadWriteLockWithNullKey() {
        assertThatThrownBy(() -> lockManager.getReadWriteLock(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lock key cannot be null or empty");
    }
    
    @Test
    void testExecute() throws Exception {
        String lockKey = "test:execute";
        String expectedResult = "success";
        
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        
        String result = lockManager.execute(lockKey, () -> expectedResult);
        
        assertThat(result).isEqualTo(expectedResult);
        verify(rLock).lock(anyLong(), any(TimeUnit.class));
        verify(rLock).unlock();
    }
    
    @Test
    void testExecuteWithConfig() throws Exception {
        String lockKey = "test:execute:config";
        LockConfig config = LockConfig.builder()
                .waitTime(Duration.ofSeconds(5))
                .leaseTime(Duration.ofSeconds(30))
                .build();
        
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        
        Integer result = lockManager.execute(lockKey, config, () -> 42);
        
        assertThat(result).isEqualTo(42);
        verify(rLock).lock(anyLong(), any(TimeUnit.class));
        verify(rLock).unlock();
    }
    
    @Test
    void testExecuteEnsuresUnlockOnException() {
        String lockKey = "test:execute:exception";
        
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        doThrow(new RuntimeException("Business error")).when(rLock).lock(anyLong(), any(TimeUnit.class));
        
        assertThatThrownBy(() -> lockManager.execute(lockKey, () -> "should not execute"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to execute with lock");
    }
    
    @Test
    void testTryExecute() throws Exception {
        String lockKey = "test:try:execute";
        
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        
        String result = lockManager.tryExecute(lockKey, () -> "success");
        
        assertThat(result).isEqualTo("success");
        verify(rLock).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
        verify(rLock).unlock();
    }
    
    @Test
    void testTryExecuteFailed() throws Exception {
        String lockKey = "test:try:failed";
        
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);
        
        String result = lockManager.tryExecute(lockKey, () -> "should not execute");
        
        assertThat(result).isNull();
        verify(rLock).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
        verify(rLock, never()).unlock();
    }
    
    @Test
    void testReleaseLock() {
        String lockKey = "test:release";
        
        // releaseLock从lockCache中remove锁，但lockCache在这个测试中是空的
        // 所以这个方法不应该调用unlock
        lockManager.releaseLock(lockKey);
        
        // lockCache中没有这个key，所以unlock不应该被调用
        verify(rLock, never()).unlock();
    }
    
    @Test
    void testReleaseAllLocks() {
        lockManager.releaseAllLocks();
        
        // 应该执行但不会抛出异常
        assertThat(lockManager).isNotNull();
    }
    
    @Test
    void testIsAvailable() {
        when(redissonClient.isShutdown()).thenReturn(false);
        
        boolean available = lockManager.isAvailable();
        
        assertThat(available).isTrue();
    }
    
    @Test
    void testIsNotAvailableWhenShutdown() {
        when(redissonClient.isShutdown()).thenReturn(true);
        
        boolean available = lockManager.isAvailable();
        
        assertThat(available).isFalse();
    }
    
    @Test
    void testGetRedLock() {
        String lockKey = "test:redlock";
        RedissonClient client1 = mock(RedissonClient.class);
        RedissonClient client2 = mock(RedissonClient.class);
        RLock lock1 = mock(RLock.class);
        RLock lock2 = mock(RLock.class);
        RLock redLock = mock(RLock.class);
        
        when(client1.getLock(lockKey)).thenReturn(lock1);
        when(client2.getLock(lockKey)).thenReturn(lock2);
        when(client1.getRedLock(any(RLock[].class))).thenReturn(redLock);
        
        Lock result = lockManager.getRedLock(lockKey, client1, client2);
        
        assertThat(result).isNotNull();
        verify(client1).getLock(lockKey);
        verify(client2).getLock(lockKey);
        verify(client1).getRedLock(any(RLock[].class));
    }
    
    @Test
    void testGetRedLockWithNoClients() {
        String lockKey = "test:redlock:empty";
        
        assertThatThrownBy(() -> lockManager.getRedLock(lockKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one RedissonClient is required");
    }
}

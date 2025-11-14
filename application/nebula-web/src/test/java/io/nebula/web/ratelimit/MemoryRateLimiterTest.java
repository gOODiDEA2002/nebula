package io.nebula.web.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * MemoryRateLimiter单元测试
 */
class MemoryRateLimiterTest {
    
    private MemoryRateLimiter rateLimiter;
    
    @BeforeEach
    void setUp() {
        // 10次请求/1秒的限流配置
        rateLimiter = new MemoryRateLimiter(10, 1000);
    }
    
    @Test
    void testAllowRequest() {
        String key = "user:123";
        
        boolean allowed = rateLimiter.tryAcquire(key, 1);
        
        assertThat(allowed).isTrue();
    }
    
    @Test
    void testBlockRequestWhenExceedLimit() {
        String key = "user:456";
        
        // 发送10次请求（达到限制）
        for (int i = 0; i < 10; i++) {
            boolean allowed = rateLimiter.tryAcquire(key, 1);
            assertThat(allowed).isTrue();
        }
        
        // 第11次请求应该被拒绝
        boolean blocked = rateLimiter.tryAcquire(key, 1);
        assertThat(blocked).isFalse();
    }
    
    @Test
    void testWindowReset() throws InterruptedException {
        String key = "user:789";
        
        // 发送10次请求达到限制
        for (int i = 0; i < 10; i++) {
            rateLimiter.tryAcquire(key, 1);
        }
        
        // 被拒绝
        assertThat(rateLimiter.tryAcquire(key, 1)).isFalse();
        
        // 等待窗口重置（1.1秒）
        TimeUnit.MILLISECONDS.sleep(1100);
        
        // 新窗口应该允许请求
        assertThat(rateLimiter.tryAcquire(key, 1)).isTrue();
    }
    
    @Test
    void testGetAvailablePermits() {
        String key = "user:abc";
        
        // 初始可用许可应该是10
        int available = rateLimiter.getAvailablePermits(key);
        assertThat(available).isEqualTo(10);
        
        // 使用5个许可
        rateLimiter.tryAcquire(key, 5);
        
        // 剩余5个
        available = rateLimiter.getAvailablePermits(key);
        assertThat(available).isEqualTo(5);
    }
    
    @Test
    void testMultipleKeys() {
        String key1 = "user:1";
        String key2 = "user:2";
        
        // key1达到限制
        for (int i = 0; i < 10; i++) {
            rateLimiter.tryAcquire(key1, 1);
        }
        
        // key1被拒绝
        assertThat(rateLimiter.tryAcquire(key1, 1)).isFalse();
        
        // key2仍然可以使用
        assertThat(rateLimiter.tryAcquire(key2, 1)).isTrue();
    }
    
    @Test
    void testReset() {
        String key = "user:xyz";
        
        // 使用一些许可
        rateLimiter.tryAcquire(key, 5);
        assertThat(rateLimiter.getAvailablePermits(key)).isEqualTo(5);
        
        // 重置
        rateLimiter.reset(key);
        
        // 应该恢复到初始状态
        assertThat(rateLimiter.getAvailablePermits(key)).isEqualTo(10);
    }
}


package io.nebula.web.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * 响应缓存测试
 */
class ResponseCacheTest {
    
    private MemoryResponseCache responseCache;
    
    @BeforeEach
    void setUp() {
        responseCache = new MemoryResponseCache(10); // 最多缓存10个响应
    }
    
    @Test
    void testCacheResponse() {
        // 准备测试数据
        String key = "test-key";
        CachedResponse response = createCachedResponse("test content");
        
        // 缓存响应
        responseCache.put(key, response, 60);
        
        // 验证缓存成功
        assertThat(responseCache.exists(key)).isTrue();
        assertThat(responseCache.size()).isEqualTo(1);
    }
    
    @Test
    void testGetCachedResponse() {
        // 准备测试数据
        String key = "test-key";
        String content = "test content";
        CachedResponse response = createCachedResponse(content);
        
        // 缓存响应
        responseCache.put(key, response, 60);
        
        // 获取缓存
        CachedResponse cached = responseCache.get(key);
        
        // 验证获取成功
        assertThat(cached).isNotNull();
        assertThat(cached.getBody()).isEqualTo(content.getBytes());
    }
    
    @Test
    void testCacheExpiration() throws InterruptedException {
        // 准备测试数据
        String key = "expire-key";
        CachedResponse response = createCachedResponse("expiring content");
        
        // 缓存响应，TTL为1秒
        responseCache.put(key, response, 1);
        
        // 验证缓存存在
        assertThat(responseCache.exists(key)).isTrue();
        
        // 等待2秒让缓存过期
        Thread.sleep(2000);
        
        // 验证缓存已过期
        assertThat(responseCache.exists(key)).isFalse();
        assertThat(responseCache.get(key)).isNull();
    }
    
    @Test
    void testEvictCache() {
        // 准备测试数据
        String key = "evict-key";
        CachedResponse response = createCachedResponse("content to evict");
        
        // 缓存响应
        responseCache.put(key, response, 60);
        assertThat(responseCache.exists(key)).isTrue();
        
        // 清除缓存
        responseCache.remove(key);
        
        // 验证缓存已清除
        assertThat(responseCache.exists(key)).isFalse();
        assertThat(responseCache.size()).isEqualTo(0);
    }
    
    @Test
    void testClearAllCache() {
        // 缓存多个响应
        for (int i = 0; i < 5; i++) {
            String key = "key-" + i;
            CachedResponse response = createCachedResponse("content-" + i);
            responseCache.put(key, response, 60);
        }
        
        // 验证缓存数量
        assertThat(responseCache.size()).isEqualTo(5);
        
        // 清空所有缓存
        responseCache.clear();
        
        // 验证缓存已清空
        assertThat(responseCache.size()).isEqualTo(0);
    }
    
    @Test
    void testCacheSizeLimit() {
        // 缓存超过最大限制的响应（maxSize=10）
        for (int i = 0; i < 15; i++) {
            String key = "key-" + i;
            CachedResponse response = createCachedResponse("content-" + i);
            responseCache.put(key, response, 60);
        }
        
        // 验证缓存数量不超过限制
        assertThat(responseCache.size()).isLessThanOrEqualTo(10);
    }
    
    @Test
    void testCacheUpdate() {
        // 准备测试数据
        String key = "update-key";
        CachedResponse response1 = createCachedResponse("original content");
        CachedResponse response2 = createCachedResponse("updated content");
        
        // 首次缓存
        responseCache.put(key, response1, 60);
        assertThat(responseCache.get(key).getBody()).isEqualTo("original content".getBytes());
        
        // 更新缓存
        responseCache.put(key, response2, 60);
        assertThat(responseCache.get(key).getBody()).isEqualTo("updated content".getBytes());
        
        // 验证大小没有增加
        assertThat(responseCache.size()).isEqualTo(1);
    }
    
    @Test
    void testCacheStats() {
        // 缓存一些响应
        for (int i = 0; i < 3; i++) {
            String key = "key-" + i;
            CachedResponse response = createCachedResponse("content-" + i);
            responseCache.put(key, response, 60);
        }
        
        // 获取统计信息
        MemoryResponseCache.CacheStats stats = responseCache.getStats();
        
        // 验证统计信息
        assertThat(stats.getCurrentSize()).isEqualTo(3);
        assertThat(stats.getMaxSize()).isEqualTo(10);
        assertThat(stats.getUsageRatio()).isCloseTo(0.3, within(0.01));
    }
    
    /**
     * 创建测试用的缓存响应
     */
    private CachedResponse createCachedResponse(String content) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        
        CachedResponse response = new CachedResponse();
        response.setStatus(200);
        response.setBody(content.getBytes());
        response.setHeaders(headers);
        response.setContentType("application/json");
        response.setCreatedTime(System.currentTimeMillis());
        
        return response;
    }
}


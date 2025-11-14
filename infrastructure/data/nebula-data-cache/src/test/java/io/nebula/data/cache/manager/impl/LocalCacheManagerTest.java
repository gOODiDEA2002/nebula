package io.nebula.data.cache.manager.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * LocalCacheManager单元测试
 */
class LocalCacheManagerTest {
    
    private LocalCacheManager cacheManager;
    
    @BeforeEach
    void setUp() {
        cacheManager = new LocalCacheManager();
    }
    
    @AfterEach
    void tearDown() {
        if (cacheManager != null) {
            cacheManager.clear();
        }
    }
    
    @Test
    void testSetAndGet() {
        String key = "test:key";
        String value = "test value";
        
        cacheManager.set(key, value);
        Optional<String> cached = cacheManager.get(key, String.class);
        
        assertThat(cached).isPresent();
        assertThat(cached.get()).isEqualTo(value);
    }
    
    @Test
    void testSetWithTtl() throws InterruptedException {
        String key = "test:expiring";
        String value = "will expire";
        
        cacheManager.set(key, value, Duration.ofMillis(100));
        
        Optional<String> cached1 = cacheManager.get(key, String.class);
        assertThat(cached1).isPresent();
        
        TimeUnit.MILLISECONDS.sleep(150);
        
        Optional<String> cached2 = cacheManager.get(key, String.class);
        assertThat(cached2).isEmpty();
    }
    
    @Test
    void testGetWithDefault() {
        String key = "nonexistent";
        String defaultValue = "default";
        
        String result = cacheManager.get(key, String.class, defaultValue);
        
        assertThat(result).isEqualTo(defaultValue);
    }
    
    @Test
    void testGetOrSet() {
        String key = "test:compute";
        String computedValue = "computed value";
        
        String result = cacheManager.getOrSet(key, String.class, () -> computedValue);
        
        assertThat(result).isEqualTo(computedValue);
        
        // 第二次获取应该从缓存
        String cachedResult = cacheManager.getOrSet(key, String.class, () -> "should not compute");
        assertThat(cachedResult).isEqualTo(computedValue);
    }
    
    @Test
    void testDelete() {
        String key = "test:delete";
        String value = "to be deleted";
        
        cacheManager.set(key, value);
        assertThat(cacheManager.exists(key)).isTrue();
        
        cacheManager.delete(key);
        assertThat(cacheManager.exists(key)).isFalse();
    }
    
    @Test
    void testExists() {
        String key = "test:exists";
        
        assertThat(cacheManager.exists(key)).isFalse();
        
        cacheManager.set(key, "value");
        assertThat(cacheManager.exists(key)).isTrue();
    }
    
    @Test
    void testClear() {
        cacheManager.set("key1", "value1");
        cacheManager.set("key2", "value2");
        
        assertThat(cacheManager.exists("key1")).isTrue();
        assertThat(cacheManager.exists("key2")).isTrue();
        
        cacheManager.clear();
        
        assertThat(cacheManager.exists("key1")).isFalse();
        assertThat(cacheManager.exists("key2")).isFalse();
    }
    
    @Test
    void testCacheMiss() {
        Optional<String> result = cacheManager.get("nonexistent", String.class);
        
        assertThat(result).isEmpty();
    }
}


package io.nebula.data.cache.manager.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证缓存 key 命名空间化与 clear() 的安全性：
 * - 所有 key 统一加前缀存储；
 * - clear() 用 SCAN 按前缀圈定，绝不 KEYS "*" 清全库；
 * - 无前缀时 clear() 拒绝执行。
 */
@ExtendWith(MockitoExtension.class)
class DefaultCacheManagerKeyPrefixTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    private DefaultCacheManager manager;

    @BeforeEach
    void setUp() {
        manager = new DefaultCacheManager(redisTemplate);
        manager.setKeyPrefix("nebula:cache:");
    }

    @Test
    void setAppliesKeyPrefix() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        manager.set("orders:1", "v");

        verify(valueOps).set(eq("nebula:cache:orders:1"), eq("v"));
    }

    @Test
    void clearRefusesWhenPrefixBlank() {
        manager.setKeyPrefix("");

        manager.clear();

        verify(redisTemplate, never()).scan(any(ScanOptions.class));
        verify(redisTemplate, never()).delete(anyCollection());
    }

    @Test
    @SuppressWarnings("unchecked")
    void clearUsesScanNotBlockingKeysStar() {
        Cursor<String> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(false);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);

        manager.clear();

        verify(redisTemplate).scan(any(ScanOptions.class));
        verify(redisTemplate, never()).keys(anyString());
    }
}

package io.nebula.web.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于内存的响应缓存实现
 */
public class MemoryResponseCache implements ResponseCache {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryResponseCache.class);
    
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final int maxSize;
    private final AtomicInteger currentSize = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "nebula-response-cache-cleaner");
        thread.setDaemon(true);
        return thread;
    });
    
    public MemoryResponseCache(int maxSize) {
        this.maxSize = maxSize;
        // 每分钟清理一次过期缓存
        scheduler.scheduleAtFixedRate(this::cleanupExpired, 1, 1, TimeUnit.MINUTES);
    }
    
    @Override
    public CachedResponse get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        
        // 检查是否过期
        if (entry.isExpired()) {
            cache.remove(key);
            currentSize.decrementAndGet();
            logger.debug("Cache entry expired and removed: {}", key);
            return null;
        }
        
        logger.debug("Cache hit: {}", key);
        return entry.getResponse();
    }
    
    @Override
    public void put(String key, CachedResponse response, int ttlSeconds) {
        if (response == null || ttlSeconds <= 0) {
            return;
        }
        
        // 检查缓存大小限制
        if (currentSize.get() >= maxSize && !cache.containsKey(key)) {
            evictOldest();
        }
        
        CacheEntry entry = new CacheEntry(response, ttlSeconds);
        CacheEntry previous = cache.put(key, entry);
        
        if (previous == null) {
            currentSize.incrementAndGet();
        }
        
        logger.debug("Cache stored: {} (ttl: {}s, size: {})", key, ttlSeconds, response.getSize());
    }
    
    @Override
    public void remove(String key) {
        CacheEntry removed = cache.remove(key);
        if (removed != null) {
            currentSize.decrementAndGet();
            logger.debug("Cache removed: {}", key);
        }
    }
    
    @Override
    public void clear() {
        int size = cache.size();
        cache.clear();
        currentSize.set(0);
        logger.info("Cache cleared: {} entries removed", size);
    }
    
    @Override
    public boolean exists(String key) {
        CacheEntry entry = cache.get(key);
        if (entry != null && entry.isExpired()) {
            cache.remove(key);
            currentSize.decrementAndGet();
            return false;
        }
        return entry != null;
    }
    
    @Override
    public int size() {
        return currentSize.get();
    }
    
    /**
     * 清理过期缓存
     */
    private void cleanupExpired() {
        int removedCount = 0;
        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                cache.remove(entry.getKey());
                currentSize.decrementAndGet();
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            logger.debug("Cleaned up {} expired cache entries", removedCount);
        }
    }
    
    /**
     * 驱逐最旧的缓存条目
     */
    private void evictOldest() {
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;
        
        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            CacheEntry cacheEntry = entry.getValue();
            if (cacheEntry.getResponse().getCreatedTime() < oldestTime) {
                oldestTime = cacheEntry.getResponse().getCreatedTime();
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null) {
            cache.remove(oldestKey);
            currentSize.decrementAndGet();
            logger.debug("Evicted oldest cache entry: {}", oldestKey);
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStats getStats() {
        return new CacheStats(currentSize.get(), maxSize, cache.size());
    }
    
    /**
     * 关闭缓存
     */
    public void shutdown() {
        scheduler.shutdown();
        clear();
        logger.info("Memory response cache shutdown");
    }
    
    /**
     * 缓存条目
     */
    private static class CacheEntry {
        private final CachedResponse response;
        private final long expiryTime;
        
        public CacheEntry(CachedResponse response, int ttlSeconds) {
            this.response = response;
            this.expiryTime = System.currentTimeMillis() + (ttlSeconds * 1000L);
        }
        
        public CachedResponse getResponse() {
            return response;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
    
    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        private final int currentSize;
        private final int maxSize;
        private final int actualEntries;
        
        public CacheStats(int currentSize, int maxSize, int actualEntries) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.actualEntries = actualEntries;
        }
        
        public int getCurrentSize() {
            return currentSize;
        }
        
        public int getMaxSize() {
            return maxSize;
        }
        
        public int getActualEntries() {
            return actualEntries;
        }
        
        public double getUsageRatio() {
            return maxSize > 0 ? (double) currentSize / maxSize : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("CacheStats{size=%d/%d (%.1f%%), entries=%d}", 
                currentSize, maxSize, getUsageRatio() * 100, actualEntries);
        }
    }
}

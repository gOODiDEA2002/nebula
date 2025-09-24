package io.nebula.data.cache.manager.impl;

import io.nebula.data.cache.manager.CacheManager;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * 本地缓存管理器
 * 基于内存的本地缓存实现，适合作为L1缓存使用
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class LocalCacheManager implements CacheManager {
    
    private final ConcurrentHashMap<String, CacheEntry> cache;
    private final LocalCacheConfig config;
    private final ScheduledExecutorService cleanupExecutor;
    
    // 统计信息
    private volatile long hitCount = 0;
    private volatile long missCount = 0;
    private volatile long evictionCount = 0;
    
    public LocalCacheManager() {
        this(LocalCacheConfig.defaultConfig());
    }
    
    public LocalCacheManager(LocalCacheConfig config) {
        this.config = config;
        this.cache = new ConcurrentHashMap<>(config.getInitialCapacity());
        
        // 启动清理任务
        this.cleanupExecutor = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "LocalCache-Cleanup");
            t.setDaemon(true);
            return t;
        });
        
        this.cleanupExecutor.scheduleWithFixedDelay(
                this::cleanupExpired,
                config.getCleanupInterval().toMillis(),
                config.getCleanupInterval().toMillis(),
                TimeUnit.MILLISECONDS
        );
        
        log.info("LocalCacheManager initialized with max size: {}", config.getMaxSize());
    }
    
    @Override
    public void set(String key, Object value) {
        set(key, value, config.getDefaultTtl());
    }
    
    @Override
    public void set(String key, Object value, Duration duration) {
        if (key == null || value == null) {
            return;
        }
        
        try {
            LocalDateTime expireTime = duration != null ? 
                    LocalDateTime.now().plus(duration) : null;
                    
            CacheEntry entry = new CacheEntry(value, expireTime);
            
            // 检查缓存大小限制
            if (cache.size() >= config.getMaxSize() && !cache.containsKey(key)) {
                evictEntries();
            }
            
            cache.put(key, entry);
            log.debug("Set local cache key: {} with TTL: {}", key, duration);
            
        } catch (Exception e) {
            log.error("Error setting local cache key: {}", key, e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        if (key == null) {
            return Optional.empty();
        }
        
        try {
            CacheEntry entry = cache.get(key);
            if (entry == null) {
                missCount++;
                return Optional.empty();
            }
            
            // 检查是否过期
            if (entry.isExpired()) {
                cache.remove(key);
                missCount++;
                return Optional.empty();
            }
            
            // 更新访问时间
            entry.updateAccessTime();
            hitCount++;
            
            Object value = entry.getValue();
            if (value != null && type.isAssignableFrom(value.getClass())) {
                return Optional.of((T) value);
            } else {
                log.warn("Type mismatch for cache key: {}, expected: {}, actual: {}", 
                        key, type.getName(), value != null ? value.getClass().getName() : "null");
                missCount++;
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("Error getting local cache key: {}", key, e);
            missCount++;
            return Optional.empty();
        }
    }
    
    @Override
    public <T> T get(String key, Class<T> type, T defaultValue) {
        return get(key, type).orElse(defaultValue);
    }
    
    @Override
    public <T> T getOrSet(String key, Class<T> type, Supplier<T> supplier) {
        return getOrSet(key, type, supplier, config.getDefaultTtl());
    }
    
    @Override
    public <T> T getOrSet(String key, Class<T> type, Supplier<T> supplier, Duration duration) {
        Optional<T> cached = get(key, type);
        if (cached.isPresent()) {
            return cached.get();
        }
        
        T value = supplier.get();
        if (value != null) {
            set(key, value, duration);
        }
        
        return value;
    }
    
    @Override
    public boolean delete(String key) {
        if (key == null) {
            return false;
        }
        
        try {
            CacheEntry removed = cache.remove(key);
            boolean result = removed != null;
            log.debug("Deleted local cache key: {}, existed: {}", key, result);
            return result;
        } catch (Exception e) {
            log.error("Error deleting local cache key: {}", key, e);
            return false;
        }
    }
    
    @Override
    public long delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        
        long count = 0;
        for (String key : keys) {
            if (delete(key)) {
                count++;
            }
        }
        return count;
    }
    
    @Override
    public boolean exists(String key) {
        if (key == null) {
            return false;
        }
        
        try {
            CacheEntry entry = cache.get(key);
            if (entry == null) {
                return false;
            }
            
            if (entry.isExpired()) {
                cache.remove(key);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error checking existence of local cache key: {}", key, e);
            return false;
        }
    }
    
    @Override
    public boolean expire(String key, Duration duration) {
        if (key == null || duration == null) {
            return false;
        }
        
        try {
            CacheEntry entry = cache.get(key);
            if (entry == null || entry.isExpired()) {
                return false;
            }
            
            LocalDateTime expireTime = LocalDateTime.now().plus(duration);
            entry.setExpireTime(expireTime);
            return true;
        } catch (Exception e) {
            log.error("Error setting expiration for local cache key: {}", key, e);
            return false;
        }
    }
    
    @Override
    public Duration getExpire(String key) {
        if (key == null) {
            return Duration.ofSeconds(-1);
        }
        
        try {
            CacheEntry entry = cache.get(key);
            if (entry == null || entry.isExpired()) {
                return Duration.ofSeconds(-1);
            }
            
            LocalDateTime expireTime = entry.getExpireTime();
            if (expireTime == null) {
                return Duration.ofSeconds(-1); // 永不过期
            }
            
            LocalDateTime now = LocalDateTime.now();
            if (expireTime.isBefore(now)) {
                return Duration.ofSeconds(-1);
            }
            
            return Duration.between(now, expireTime);
        } catch (Exception e) {
            log.error("Error getting expiration for local cache key: {}", key, e);
            return Duration.ofSeconds(-1);
        }
    }
    
    @Override
    public boolean persist(String key) {
        if (key == null) {
            return false;
        }
        
        try {
            CacheEntry entry = cache.get(key);
            if (entry == null || entry.isExpired()) {
                return false;
            }
            
            entry.setExpireTime(null); // 设置为永不过期
            return true;
        } catch (Exception e) {
            log.error("Error persisting local cache key: {}", key, e);
            return false;
        }
    }
    
    // ========== 本地缓存不支持的复杂操作 ==========
    
    @Override
    public long increment(String key) {
        throw new UnsupportedOperationException("Local cache does not support increment operation");
    }
    
    @Override
    public long increment(String key, long delta) {
        throw new UnsupportedOperationException("Local cache does not support increment operation");
    }
    
    @Override
    public long decrement(String key) {
        throw new UnsupportedOperationException("Local cache does not support decrement operation");
    }
    
    @Override
    public long decrement(String key, long delta) {
        throw new UnsupportedOperationException("Local cache does not support decrement operation");
    }
    
    @Override
    public void hSet(String key, String field, Object value) {
        throw new UnsupportedOperationException("Local cache does not support hash operations");
    }
    
    @Override
    public void hMSet(String key, Map<String, Object> fields) {
        throw new UnsupportedOperationException("Local cache does not support hash operations");
    }
    
    @Override
    public <T> Optional<T> hGet(String key, String field, Class<T> type) {
        throw new UnsupportedOperationException("Local cache does not support hash operations");
    }
    
    @Override
    public Map<String, Object> hGetAll(String key) {
        throw new UnsupportedOperationException("Local cache does not support hash operations");
    }
    
    @Override
    public long hDelete(String key, String... fields) {
        throw new UnsupportedOperationException("Local cache does not support hash operations");
    }
    
    @Override
    public boolean hExists(String key, String field) {
        throw new UnsupportedOperationException("Local cache does not support hash operations");
    }
    
    @Override
    public long hLen(String key) {
        throw new UnsupportedOperationException("Local cache does not support hash operations");
    }
    
    @Override
    public Set<String> hKeys(String key) {
        throw new UnsupportedOperationException("Local cache does not support hash operations");
    }
    
    @Override
    public long hIncrement(String key, String field, long delta) {
        throw new UnsupportedOperationException("Local cache does not support hash operations");
    }
    
    @Override
    public long lPush(String key, Object... values) {
        throw new UnsupportedOperationException("Local cache does not support list operations");
    }
    
    @Override
    public long rPush(String key, Object... values) {
        throw new UnsupportedOperationException("Local cache does not support list operations");
    }
    
    @Override
    public <T> Optional<T> lPop(String key, Class<T> type) {
        throw new UnsupportedOperationException("Local cache does not support list operations");
    }
    
    @Override
    public <T> Optional<T> rPop(String key, Class<T> type) {
        throw new UnsupportedOperationException("Local cache does not support list operations");
    }
    
    @Override
    public <T> List<T> lRange(String key, long start, long end, Class<T> type) {
        throw new UnsupportedOperationException("Local cache does not support list operations");
    }
    
    @Override
    public long lLen(String key) {
        throw new UnsupportedOperationException("Local cache does not support list operations");
    }
    
    @Override
    public long sAdd(String key, Object... values) {
        throw new UnsupportedOperationException("Local cache does not support set operations");
    }
    
    @Override
    public long sRem(String key, Object... values) {
        throw new UnsupportedOperationException("Local cache does not support set operations");
    }
    
    @Override
    public boolean sIsMember(String key, Object value) {
        throw new UnsupportedOperationException("Local cache does not support set operations");
    }
    
    @Override
    public <T> Set<T> sMembers(String key, Class<T> type) {
        throw new UnsupportedOperationException("Local cache does not support set operations");
    }
    
    @Override
    public long sCard(String key) {
        throw new UnsupportedOperationException("Local cache does not support set operations");
    }
    
    @Override
    public boolean zAdd(String key, Object value, double score) {
        throw new UnsupportedOperationException("Local cache does not support sorted set operations");
    }
    
    @Override
    public long zAdd(String key, Map<Object, Double> values) {
        throw new UnsupportedOperationException("Local cache does not support sorted set operations");
    }
    
    @Override
    public <T> List<T> zRange(String key, long start, long end, Class<T> type) {
        throw new UnsupportedOperationException("Local cache does not support sorted set operations");
    }
    
    @Override
    public <T> List<T> zRangeByScore(String key, double minScore, double maxScore, Class<T> type) {
        throw new UnsupportedOperationException("Local cache does not support sorted set operations");
    }
    
    @Override
    public Long zRank(String key, Object value) {
        throw new UnsupportedOperationException("Local cache does not support sorted set operations");
    }
    
    @Override
    public Double zScore(String key, Object value) {
        throw new UnsupportedOperationException("Local cache does not support sorted set operations");
    }
    
    @Override
    public long zCard(String key) {
        throw new UnsupportedOperationException("Local cache does not support sorted set operations");
    }
    
    @Override
    public Set<String> keys(String pattern) {
        // 简单的模式匹配实现
        Set<String> result = new HashSet<>();
        for (String key : cache.keySet()) {
            if (matchesPattern(key, pattern)) {
                result.add(key);
            }
        }
        return result;
    }
    
    @Override
    public Set<String> scan(String pattern, long count) {
        // 简化实现，忽略count参数
        return keys(pattern);
    }
    
    // ========== 异步操作 ==========
    
    @Override
    public CompletableFuture<Void> setAsync(String key, Object value) {
        return CompletableFuture.runAsync(() -> set(key, value));
    }
    
    @Override
    public <T> CompletableFuture<Optional<T>> getAsync(String key, Class<T> type) {
        return CompletableFuture.supplyAsync(() -> get(key, type));
    }
    
    @Override
    public CompletableFuture<Boolean> deleteAsync(String key) {
        return CompletableFuture.supplyAsync(() -> delete(key));
    }
    
    // ========== 管理操作 ==========
    
    @Override
    public void clear() {
        try {
            cache.clear();
            hitCount = 0;
            missCount = 0;
            evictionCount = 0;
            log.info("Local cache cleared");
        } catch (Exception e) {
            log.error("Error clearing local cache", e);
        }
    }
    
    @Override
    public CacheStats getStats() {
        return new LocalCacheStats();
    }
    
    @Override
    public String getName() {
        return "LocalCache";
    }
    
    @Override
    public boolean isAvailable() {
        return true; // 本地缓存总是可用的
    }
    
    /**
     * 清理过期条目
     */
    private void cleanupExpired() {
        try {
            Iterator<Map.Entry<String, CacheEntry>> iterator = cache.entrySet().iterator();
            int removedCount = 0;
            
            while (iterator.hasNext()) {
                Map.Entry<String, CacheEntry> entry = iterator.next();
                if (entry.getValue().isExpired()) {
                    iterator.remove();
                    removedCount++;
                }
            }
            
            if (removedCount > 0) {
                log.debug("Cleaned up {} expired entries from local cache", removedCount);
            }
        } catch (Exception e) {
            log.error("Error during local cache cleanup", e);
        }
    }
    
    /**
     * 驱逐条目以释放空间
     */
    private void evictEntries() {
        int evictCount = Math.max(1, (int) (config.getMaxSize() * 0.1)); // 驱逐10%的条目
        
        List<Map.Entry<String, CacheEntry>> entries = new ArrayList<>(cache.entrySet());
        
        // 根据驱逐策略排序
        switch (config.getEvictionPolicy()) {
            case LRU:
                entries.sort((e1, e2) -> e1.getValue().getLastAccessTime().compareTo(e2.getValue().getLastAccessTime()));
                break;
            case LFU:
                entries.sort((e1, e2) -> Long.compare(e1.getValue().getAccessCount(), e2.getValue().getAccessCount()));
                break;
            case FIFO:
                entries.sort((e1, e2) -> e1.getValue().getCreateTime().compareTo(e2.getValue().getCreateTime()));
                break;
        }
        
        // 驱逐最旧的条目
        for (int i = 0; i < evictCount && i < entries.size(); i++) {
            String key = entries.get(i).getKey();
            cache.remove(key);
            evictionCount++;
        }
        
        log.debug("Evicted {} entries from local cache", evictCount);
    }
    
    /**
     * 简单的模式匹配
     */
    private boolean matchesPattern(String key, String pattern) {
        if (pattern == null || "*".equals(pattern)) {
            return true;
        }
        
        // 简化实现，只支持*通配符
        return key.matches(pattern.replace("*", ".*"));
    }
    
    /**
     * 销毁缓存管理器
     */
    public void destroy() {
        if (cleanupExecutor != null && !cleanupExecutor.isShutdown()) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        clear();
        log.info("LocalCacheManager destroyed");
    }
    
    /**
     * 本地缓存统计信息
     */
    private class LocalCacheStats implements CacheStats {
        
        @Override
        public long getHitCount() {
            return hitCount;
        }
        
        @Override
        public long getMissCount() {
            return missCount;
        }
        
        @Override
        public double getHitRate() {
            long total = hitCount + missCount;
            return total == 0 ? 0.0 : (double) hitCount / total;
        }
        
        @Override
        public long getSize() {
            return cache.size();
        }
        
        @Override
        public long getEvictionCount() {
            return evictionCount;
        }
    }
    
    /**
     * 缓存条目
     */
    private static class CacheEntry {
        private final Object value;
        private final LocalDateTime createTime;
        private volatile LocalDateTime lastAccessTime;
        private volatile LocalDateTime expireTime;
        private volatile long accessCount;
        
        public CacheEntry(Object value, LocalDateTime expireTime) {
            this.value = value;
            this.createTime = LocalDateTime.now();
            this.lastAccessTime = this.createTime;
            this.expireTime = expireTime;
            this.accessCount = 0;
        }
        
        public Object getValue() {
            return value;
        }
        
        public LocalDateTime getCreateTime() {
            return createTime;
        }
        
        public LocalDateTime getLastAccessTime() {
            return lastAccessTime;
        }
        
        public LocalDateTime getExpireTime() {
            return expireTime;
        }
        
        public void setExpireTime(LocalDateTime expireTime) {
            this.expireTime = expireTime;
        }
        
        public long getAccessCount() {
            return accessCount;
        }
        
        public void updateAccessTime() {
            this.lastAccessTime = LocalDateTime.now();
            this.accessCount++;
        }
        
        public boolean isExpired() {
            return expireTime != null && LocalDateTime.now().isAfter(expireTime);
        }
    }
    
    /**
     * 本地缓存配置
     */
    public static class LocalCacheConfig {
        private final int maxSize;
        private final int initialCapacity;
        private final Duration defaultTtl;
        private final Duration cleanupInterval;
        private final EvictionPolicy evictionPolicy;
        
        public LocalCacheConfig(int maxSize, int initialCapacity, Duration defaultTtl, 
                               Duration cleanupInterval, EvictionPolicy evictionPolicy) {
            this.maxSize = maxSize;
            this.initialCapacity = initialCapacity;
            this.defaultTtl = defaultTtl;
            this.cleanupInterval = cleanupInterval;
            this.evictionPolicy = evictionPolicy;
        }
        
        public static LocalCacheConfig defaultConfig() {
            return new LocalCacheConfig(
                    10000,                          // maxSize
                    1000,                           // initialCapacity
                    Duration.ofMinutes(10),         // defaultTtl
                    Duration.ofMinutes(5),          // cleanupInterval
                    EvictionPolicy.LRU              // evictionPolicy
            );
        }
        
        public int getMaxSize() { return maxSize; }
        public int getInitialCapacity() { return initialCapacity; }
        public Duration getDefaultTtl() { return defaultTtl; }
        public Duration getCleanupInterval() { return cleanupInterval; }
        public EvictionPolicy getEvictionPolicy() { return evictionPolicy; }
    }
    
    /**
     * 驱逐策略
     */
    public enum EvictionPolicy {
        LRU, LFU, FIFO
    }
}

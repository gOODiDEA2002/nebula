package io.nebula.data.cache.manager;

import io.nebula.data.cache.manager.CacheManager;
import io.nebula.data.cache.sync.CacheInvalidationBroadcaster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 多级缓存管理器
 * 支持L1本地缓存 + L2远程缓存的多级缓存架构
 *
 * <p>CD-10 可靠性增强:</p>
 * <ul>
 *   <li>跨节点失效: 注入 {@link CacheInvalidationBroadcaster} 后, set/delete/clear 等写操作
 *       会广播失效通知, 其他节点驱逐各自 L1 的过期副本(否则旧值会残留到 L1 TTL 自然到期)</li>
 *   <li>击穿防护: {@code getOrSet} 按 key single-flight, 并发同 key miss 只有一个线程回源,
 *       其余线程等待其结果</li>
 *   <li>穿透防护: 可配置空值哨兵({@code nullCachingEnabled}), 回源 null 时写入短 TTL 哨兵,
 *       窗口期内不再重复回源不存在的 key。哨兵仅由 getOrSet 识别, 直接 get 该 key 视为未命中</li>
 * </ul>
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class MultiLevelCacheManager implements CacheManager {
    
    /**
     * 空值哨兵: 以普通字符串值存入 L1/L2(可跨节点、可序列化), getOrSet 读到时返回 null 且不回源
     */
    static final String NULL_SENTINEL = "__NEBULA_NULL__";
    
    private final CacheManager l1Cache;    // 本地缓存（L1）
    private final CacheManager l2Cache;    // 远程缓存（L2）
    private final MultiLevelCacheConfig config;
    
    /**
     * single-flight 登记表: key -> 正在回源的加载 Future, 跟随线程直接等待结果
     */
    private final ConcurrentHashMap<String, CompletableFuture<Object>> inFlightLoads = new ConcurrentHashMap<>();
    
    /**
     * 跨节点失效广播器(可选, 由自动配置在 multi-level + Redis 场景注入)
     */
    private volatile CacheInvalidationBroadcaster invalidationBroadcaster;
    
    // 统计信息
    private volatile long l1HitCount = 0;
    private volatile long l2HitCount = 0;
    private volatile long missCount = 0;
    private volatile long totalRequestCount = 0;
    
    public MultiLevelCacheManager(CacheManager l1Cache, CacheManager l2Cache) {
        this(l1Cache, l2Cache, MultiLevelCacheConfig.defaultConfig());
    }
    
    public MultiLevelCacheManager(CacheManager l1Cache, CacheManager l2Cache, MultiLevelCacheConfig config) {
        Assert.notNull(l1Cache, "L1 cache cannot be null");
        Assert.notNull(l2Cache, "L2 cache cannot be null");
        Assert.notNull(config, "Config cannot be null");
        
        this.l1Cache = l1Cache;
        this.l2Cache = l2Cache;
        this.config = config;
        
        log.info("MultiLevelCacheManager initialized with L1: {}, L2: {}", 
                l1Cache.getName(), l2Cache.getName());
    }
    
    @Override
    public void set(String key, Object value) {
        set(key, value, config.getDefaultTtl());
    }
    
    @Override
    public void set(String key, Object value, Duration duration) {
        try {
            // 同时写入L1和L2缓存
            if (config.isL1WriteEnabled()) {
                l1Cache.set(key, value, getL1Duration(duration));
            }
            
            if (config.isL2WriteEnabled()) {
                l2Cache.set(key, value, duration);
            }
            
            // 通知其他节点驱逐 L1 旧副本(它们下次读取会从 L2 拿到新值)
            broadcastEvict(key);
            
            log.debug("Set cache key: {} with duration: {}", key, duration);
        } catch (Exception e) {
            log.error("Error setting cache key: {}", key, e);
            throw new RuntimeException("Failed to set cache", e);
        }
    }
    
    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        totalRequestCount++;
        
        try {
            // 先从L1缓存获取
            if (config.isL1ReadEnabled()) {
                Optional<T> l1Result = l1Cache.get(key, type);
                if (l1Result.isPresent()) {
                    l1HitCount++;
                    log.debug("L1 cache hit for key: {}", key);
                    return l1Result;
                }
            }
            
            // L1未命中，从L2缓存获取
            if (config.isL2ReadEnabled()) {
                Optional<T> l2Result = l2Cache.get(key, type);
                if (l2Result.isPresent()) {
                    l2HitCount++;
                    log.debug("L2 cache hit for key: {}", key);
                    
                    // 将L2的数据回写到L1（缓存预热）
                    if (config.isL1WriteBackEnabled()) {
                        try {
                            l1Cache.set(key, l2Result.get(), config.getL1WriteBackTtl());
                            log.debug("Write back to L1 cache for key: {}", key);
                        } catch (Exception e) {
                            log.warn("Failed to write back to L1 cache for key: {}", key, e);
                        }
                    }
                    
                    return l2Result;
                }
            }
            
            // 都未命中
            missCount++;
            log.debug("Cache miss for key: {}", key);
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Error getting cache key: {}", key, e);
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
    
    /**
     * 命中空值哨兵时 lookupRaw 的内部返回标记(引用比较, 区别于"未命中"的 null)
     */
    private static final Object NULL_HIT = new Object();
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getOrSet(String key, Class<T> type, Supplier<T> supplier, Duration duration) {
        Object cached = lookupRaw(key, type);
        if (cached == NULL_HIT) {
            return null;
        }
        if (cached != null) {
            return (T) cached;
        }
        
        // 击穿防护(single-flight): 并发同 key miss 只允许一个线程回源, 其余等待其结果
        CompletableFuture<Object> myLoad = new CompletableFuture<>();
        CompletableFuture<Object> existing = inFlightLoads.putIfAbsent(key, myLoad);
        if (existing != null) {
            try {
                return (T) existing.join();
            } catch (CompletionException | CancellationException e) {
                // 首航线程回源失败: 跟随线程自行回源(不再嵌套 single-flight, 避免级联等待)
                log.debug("In-flight load failed for key: {}, falling back to direct load", key);
                return loadAndCache(key, supplier, duration);
            }
        }
        
        try {
            // double-check: 排队期间首航线程可能已把值写入缓存
            Object recheck = lookupRaw(key, type);
            if (recheck != null) {
                T result = recheck == NULL_HIT ? null : (T) recheck;
                myLoad.complete(result);
                return result;
            }
            T value = loadAndCache(key, supplier, duration);
            myLoad.complete(value);
            return value;
        } catch (Throwable t) {
            myLoad.completeExceptionally(t);
            throw t;
        } finally {
            inFlightLoads.remove(key, myLoad);
        }
    }
    
    /**
     * 缓存查找: 未命中返回 null; 命中空值哨兵返回 {@link #NULL_HIT}; 否则返回命中的值
     */
    private Object lookupRaw(String key, Class<?> type) {
        // 先以 Object 读取: 同时识别空值哨兵与正常值, 避免以业务类型读哨兵触发类型不匹配告警
        Optional<Object> raw = get(key, Object.class);
        if (raw.isEmpty()) {
            return null;
        }
        Object value = raw.get();
        if (config.isNullCachingEnabled() && NULL_SENTINEL.equals(value)) {
            log.debug("Null sentinel hit for key: {}, skip loading", key);
            return NULL_HIT;
        }
        if (type.isInstance(value)) {
            return value;
        }
        // 反序列化产物与目标类型不一致(如 L2 JSON 反序列化出 Map): 走类型感知的 get 让实现层转换
        return get(key, type).orElse(null);
    }
    
    /**
     * 回源加载并写缓存: 非空值正常写入; null 且启用穿透防护时写短 TTL 空值哨兵
     */
    private <T> T loadAndCache(String key, Supplier<T> supplier, Duration duration) {
        T value = supplier.get();
        if (value != null) {
            set(key, value, duration);
        } else if (config.isNullCachingEnabled()) {
            set(key, NULL_SENTINEL, config.getNullValueTtl());
            log.debug("Cached null sentinel for key: {} with TTL: {}", key, config.getNullValueTtl());
        }
        return value;
    }
    
    @Override
    public boolean delete(String key) {
        boolean l1Deleted = false;
        boolean l2Deleted = false;
        
        try {
            if (config.isL1WriteEnabled()) {
                l1Deleted = l1Cache.delete(key);
            }
            
            if (config.isL2WriteEnabled()) {
                l2Deleted = l2Cache.delete(key);
            }
            
            boolean result = l1Deleted || l2Deleted;
            
            // 通知其他节点驱逐 L1 副本, 否则删除后旧值仍会从其他节点的 L1 读出
            broadcastEvict(key);
            
            log.debug("Deleted cache key: {}, L1: {}, L2: {}", key, l1Deleted, l2Deleted);
            return result;
            
        } catch (Exception e) {
            log.error("Error deleting cache key: {}", key, e);
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
        try {
            // 检查L1缓存
            if (config.isL1ReadEnabled() && l1Cache.exists(key)) {
                return true;
            }
            
            // 检查L2缓存
            if (config.isL2ReadEnabled() && l2Cache.exists(key)) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error("Error checking existence of cache key: {}", key, e);
            return false;
        }
    }
    
    @Override
    public boolean expire(String key, Duration duration) {
        boolean l1Result = false;
        boolean l2Result = false;
        
        try {
            if (config.isL1WriteEnabled()) {
                l1Result = l1Cache.expire(key, getL1Duration(duration));
            }
            
            if (config.isL2WriteEnabled()) {
                l2Result = l2Cache.expire(key, duration);
            }
            
            return l1Result || l2Result;
        } catch (Exception e) {
            log.error("Error setting expiration for cache key: {}", key, e);
            return false;
        }
    }
    
    @Override
    public Duration getExpire(String key) {
        try {
            // 优先从L2获取过期时间（更准确）
            if (config.isL2ReadEnabled()) {
                Duration l2Expire = l2Cache.getExpire(key);
                if (!l2Expire.equals(Duration.ofSeconds(-1))) {
                    return l2Expire;
                }
            }
            
            // 从L1获取
            if (config.isL1ReadEnabled()) {
                return l1Cache.getExpire(key);
            }
            
            return Duration.ofSeconds(-1);
        } catch (Exception e) {
            log.error("Error getting expiration for cache key: {}", key, e);
            return Duration.ofSeconds(-1);
        }
    }
    
    @Override
    public boolean persist(String key) {
        boolean l1Result = false;
        boolean l2Result = false;
        
        try {
            if (config.isL1WriteEnabled()) {
                l1Result = l1Cache.persist(key);
            }
            
            if (config.isL2WriteEnabled()) {
                l2Result = l2Cache.persist(key);
            }
            
            return l1Result || l2Result;
        } catch (Exception e) {
            log.error("Error persisting cache key: {}", key, e);
            return false;
        }
    }
    
    // ========== 数值操作（委托给L2缓存） ==========
    
    @Override
    public long increment(String key) {
        return l2Cache.increment(key);
    }
    
    @Override
    public long increment(String key, long delta) {
        return l2Cache.increment(key, delta);
    }
    
    @Override
    public long decrement(String key) {
        return l2Cache.decrement(key);
    }
    
    @Override
    public long decrement(String key, long delta) {
        return l2Cache.decrement(key, delta);
    }
    
    // ========== Hash操作（委托给L2缓存） ==========
    
    @Override
    public void hSet(String key, String field, Object value) {
        l2Cache.hSet(key, field, value);
        // 清除L1中可能存在的过期数据
        if (config.isL1WriteEnabled()) {
            l1Cache.delete(key);
        }
        broadcastEvict(key);
    }
    
    @Override
    public void hMSet(String key, Map<String, Object> fields) {
        l2Cache.hMSet(key, fields);
        // 清除L1中可能存在的过期数据
        if (config.isL1WriteEnabled()) {
            l1Cache.delete(key);
        }
        broadcastEvict(key);
    }
    
    @Override
    public <T> Optional<T> hGet(String key, String field, Class<T> type) {
        return l2Cache.hGet(key, field, type);
    }
    
    @Override
    public Map<String, Object> hGetAll(String key) {
        return l2Cache.hGetAll(key);
    }
    
    @Override
    public long hDelete(String key, String... fields) {
        long result = l2Cache.hDelete(key, fields);
        // 清除L1中可能存在的过期数据
        if (config.isL1WriteEnabled()) {
            l1Cache.delete(key);
        }
        broadcastEvict(key);
        return result;
    }
    
    @Override
    public boolean hExists(String key, String field) {
        return l2Cache.hExists(key, field);
    }
    
    @Override
    public long hLen(String key) {
        return l2Cache.hLen(key);
    }
    
    @Override
    public Set<String> hKeys(String key) {
        return l2Cache.hKeys(key);
    }
    
    @Override
    public long hIncrement(String key, String field, long delta) {
        return l2Cache.hIncrement(key, field, delta);
    }
    
    // ========== List操作（委托给L2缓存） ==========
    
    @Override
    public long lPush(String key, Object... values) {
        return l2Cache.lPush(key, values);
    }
    
    @Override
    public long rPush(String key, Object... values) {
        return l2Cache.rPush(key, values);
    }
    
    @Override
    public <T> Optional<T> lPop(String key, Class<T> type) {
        return l2Cache.lPop(key, type);
    }
    
    @Override
    public <T> Optional<T> rPop(String key, Class<T> type) {
        return l2Cache.rPop(key, type);
    }
    
    @Override
    public <T> List<T> lRange(String key, long start, long end, Class<T> type) {
        return l2Cache.lRange(key, start, end, type);
    }
    
    @Override
    public long lLen(String key) {
        return l2Cache.lLen(key);
    }
    
    // ========== Set操作（委托给L2缓存） ==========
    
    @Override
    public long sAdd(String key, Object... values) {
        return l2Cache.sAdd(key, values);
    }
    
    @Override
    public long sRem(String key, Object... values) {
        return l2Cache.sRem(key, values);
    }
    
    @Override
    public boolean sIsMember(String key, Object value) {
        return l2Cache.sIsMember(key, value);
    }
    
    @Override
    public <T> Set<T> sMembers(String key, Class<T> type) {
        return l2Cache.sMembers(key, type);
    }
    
    @Override
    public long sCard(String key) {
        return l2Cache.sCard(key);
    }
    
    // ========== ZSet操作（委托给L2缓存） ==========
    
    @Override
    public boolean zAdd(String key, Object value, double score) {
        return l2Cache.zAdd(key, value, score);
    }
    
    @Override
    public long zAdd(String key, Map<Object, Double> values) {
        return l2Cache.zAdd(key, values);
    }
    
    @Override
    public <T> List<T> zRange(String key, long start, long end, Class<T> type) {
        return l2Cache.zRange(key, start, end, type);
    }
    
    @Override
    public <T> List<T> zRangeByScore(String key, double minScore, double maxScore, Class<T> type) {
        return l2Cache.zRangeByScore(key, minScore, maxScore, type);
    }
    
    @Override
    public Long zRank(String key, Object value) {
        return l2Cache.zRank(key, value);
    }
    
    @Override
    public Double zScore(String key, Object value) {
        return l2Cache.zScore(key, value);
    }
    
    @Override
    public long zCard(String key) {
        return l2Cache.zCard(key);
    }
    
    // ========== 模式匹配（委托给L2缓存） ==========
    
    @Override
    public Set<String> keys(String pattern) {
        return l2Cache.keys(pattern);
    }
    
    @Override
    public Set<String> scan(String pattern, long count) {
        return l2Cache.scan(pattern, count);
    }
    
    // ========== 异步操作 ==========
    
    @Override
    public CompletableFuture<Void> setAsync(String key, Object value) {
        return CompletableFuture.allOf(
            config.isL1WriteEnabled() ? l1Cache.setAsync(key, value) : CompletableFuture.completedFuture(null),
            config.isL2WriteEnabled() ? l2Cache.setAsync(key, value) : CompletableFuture.completedFuture(null)
        ).thenRun(() -> broadcastEvict(key));
    }
    
    @Override
    public <T> CompletableFuture<Optional<T>> getAsync(String key, Class<T> type) {
        if (config.isL1ReadEnabled()) {
            return l1Cache.getAsync(key, type)
                    .thenCompose(l1Result -> {
                        if (l1Result.isPresent()) {
                            l1HitCount++;
                            return CompletableFuture.completedFuture(l1Result);
                        } else if (config.isL2ReadEnabled()) {
                            return l2Cache.getAsync(key, type)
                                    .thenApply(l2Result -> {
                                        if (l2Result.isPresent()) {
                                            l2HitCount++;
                                            // 异步回写到L1
                                            if (config.isL1WriteBackEnabled()) {
                                                l1Cache.setAsync(key, l2Result.get());
                                            }
                                        } else {
                                            missCount++;
                                        }
                                        return l2Result;
                                    });
                        } else {
                            missCount++;
                            return CompletableFuture.completedFuture(Optional.empty());
                        }
                    });
        } else if (config.isL2ReadEnabled()) {
            return l2Cache.getAsync(key, type);
        } else {
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }
    
    @Override
    public CompletableFuture<Boolean> deleteAsync(String key) {
        CompletableFuture<Boolean> l1Future = config.isL1WriteEnabled() ? 
                l1Cache.deleteAsync(key) : CompletableFuture.completedFuture(false);
        CompletableFuture<Boolean> l2Future = config.isL2WriteEnabled() ? 
                l2Cache.deleteAsync(key) : CompletableFuture.completedFuture(false);
        
        return CompletableFuture.allOf(l1Future, l2Future)
                .thenApply(v -> {
                    broadcastEvict(key);
                    return l1Future.join() || l2Future.join();
                });
    }
    
    // ========== 管理操作 ==========
    
    @Override
    public void clear() {
        try {
            if (config.isL1WriteEnabled()) {
                l1Cache.clear();
            }
            if (config.isL2WriteEnabled()) {
                l2Cache.clear();
            }
            
            // 通知其他节点清空各自的 L1
            CacheInvalidationBroadcaster broadcaster = this.invalidationBroadcaster;
            if (broadcaster != null) {
                broadcaster.publishClear();
            }
            
            // 重置统计信息
            l1HitCount = 0;
            l2HitCount = 0;
            missCount = 0;
            totalRequestCount = 0;
            
            log.info("Multi-level cache cleared");
        } catch (Exception e) {
            log.error("Error clearing multi-level cache", e);
            throw new RuntimeException("Failed to clear cache", e);
        }
    }
    
    @Override
    public CacheStats getStats() {
        return new MultiLevelCacheStats();
    }
    
    @Override
    public String getName() {
        return "MultiLevelCache[L1:" + l1Cache.getName() + ",L2:" + l2Cache.getName() + "]";
    }
    
    @Override
    public boolean isAvailable() {
        boolean l1Available = l1Cache.isAvailable();
        boolean l2Available = l2Cache.isAvailable();
        
        // 至少有一个缓存可用即认为可用
        return l1Available || l2Available;
    }
    
    /**
     * 获取L1缓存的过期时间（通常比L2短）
     */
    private Duration getL1Duration(Duration originalDuration) {
        if (originalDuration == null) {
            return config.getL1DefaultTtl();
        }
        
        // L1缓存时间为原时间的一定比例
        long originalMillis = originalDuration.toMillis();
        long l1Millis = (long) (originalMillis * config.getL1TtlRatio());
        long minimumMillis = config.getL1MinTtl().toMillis();
        return Duration.ofMillis(Math.min(originalMillis, Math.max(l1Millis, minimumMillis)));
    }
    
    /**
     * 多级缓存统计信息
     */
    public class MultiLevelCacheStats implements CacheStats {
        
        @Override
        public long getHitCount() {
            return l1HitCount + l2HitCount;
        }
        
        @Override
        public long getMissCount() {
            return missCount;
        }
        
        @Override
        public double getHitRate() {
            if (totalRequestCount == 0) {
                return 0.0;
            }
            return (double) getHitCount() / totalRequestCount;
        }
        
        @Override
        public long getSize() {
            return l1Cache.getStats().getSize() + l2Cache.getStats().getSize();
        }
        
        @Override
        public long getEvictionCount() {
            return l1Cache.getStats().getEvictionCount() + l2Cache.getStats().getEvictionCount();
        }
        
        /**
         * 获取L1缓存命中次数
         */
        public long getL1HitCount() {
            return l1HitCount;
        }
        
        /**
         * 获取L2缓存命中次数
         */
        public long getL2HitCount() {
            return l2HitCount;
        }
        
        /**
         * 获取L1缓存命中率
         */
        public double getL1HitRate() {
            if (totalRequestCount == 0) {
                return 0.0;
            }
            return (double) l1HitCount / totalRequestCount;
        }
        
        /**
         * 获取L2缓存命中率
         */
        public double getL2HitRate() {
            if (totalRequestCount == 0) {
                return 0.0;
            }
            return (double) l2HitCount / totalRequestCount;
        }
        
        /**
         * 获取总请求数
         */
        public long getTotalRequestCount() {
            return totalRequestCount;
        }
    }
    
    /**
     * 清除L1缓存
     */
    public void clearL1() {
        if (config.isL1WriteEnabled()) {
            l1Cache.clear();
            log.info("L1 cache cleared");
        }
    }
    
    /**
     * 清除L2缓存
     */
    public void clearL2() {
        if (config.isL2WriteEnabled()) {
            l2Cache.clear();
            log.info("L2 cache cleared");
        }
    }
    
    // ========== 跨节点失效（CD-10） ==========
    
    /**
     * 注入跨节点失效广播器（由自动配置在 multi-level + Redis 场景调用）
     */
    public void setInvalidationBroadcaster(CacheInvalidationBroadcaster broadcaster) {
        this.invalidationBroadcaster = broadcaster;
        log.info("Cache invalidation broadcaster attached: {}", broadcaster.getClass().getSimpleName());
    }
    
    /**
     * 处理其他节点的失效广播: 仅驱逐本地 L1, 不回发广播（否则消息风暴）
     */
    public void onRemoteEvict(String key) {
        if (config.isL1WriteEnabled()) {
            l1Cache.delete(key);
        }
    }
    
    /**
     * 处理其他节点的清空广播: 仅清空本地 L1, 不回发广播
     */
    public void onRemoteClear() {
        if (config.isL1WriteEnabled()) {
            l1Cache.clear();
        }
    }
    
    /**
     * 写路径广播失效通知（尽力而为, 失败不影响本地结果）
     */
    private void broadcastEvict(String key) {
        CacheInvalidationBroadcaster broadcaster = this.invalidationBroadcaster;
        if (broadcaster != null) {
            broadcaster.publishEvict(key);
        }
    }
    
    /**
     * 获取L1缓存管理器
     */
    public CacheManager getL1Cache() {
        return l1Cache;
    }
    
    /**
     * 获取L2缓存管理器
     */
    public CacheManager getL2Cache() {
        return l2Cache;
    }
    
    /**
     * 获取配置
     */
    public MultiLevelCacheConfig getConfig() {
        return config;
    }
}

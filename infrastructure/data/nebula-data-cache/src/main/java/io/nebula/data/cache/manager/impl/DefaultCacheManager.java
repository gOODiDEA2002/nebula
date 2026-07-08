package io.nebula.data.cache.manager.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.nebula.data.cache.manager.CacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 基于Redis的缓存管理器默认实现
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultCacheManager implements CacheManager {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CACHE_NAME = "DefaultCache";
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
    private final LongAdder hitCount = new LongAdder();
    private final LongAdder missCount = new LongAdder();
    private final LongAdder evictionCount = new LongAdder();

    /**
     * 缓存 key 前缀(命名空间)。所有 key 统一加此前缀存储，使 clear()/getSize() 能按前缀圈定范围，
     * 避免 KEYS "*" 误删/统计同库其他业务数据。由 CacheProperties.redis.keyPrefix 注入。
     */
    private String keyPrefix = "nebula:cache:";
    private static final long SCAN_COUNT = 500;

    public void setKeyPrefix(String keyPrefix) {
        if (keyPrefix != null) {
            this.keyPrefix = keyPrefix;
        }
    }

    private String buildKey(String key) {
        return keyPrefix + key;
    }

    /**
     * 用 SCAN 游标(非阻塞)收集匹配 pattern 的 key，替代阻塞且危险的 KEYS。
     */
    private Set<String> scanKeys(String pattern) {
        Set<String> result = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(SCAN_COUNT).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                result.add(cursor.next());
            }
        } catch (Exception e) {
            log.error("SCAN 失败: pattern={}", pattern, e);
        }
        return result;
    }
    
    @Override
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(buildKey(key), value);
            log.debug("设置缓存: key={}", key);
        } catch (Exception e) {
            log.error("设置缓存失败: key={}", key, e);
        }
    }
    
    @Override
    public void set(String key, Object value, Duration duration) {
        try {
            redisTemplate.opsForValue().set(buildKey(key), value, duration);
            log.debug("设置缓存（带过期时间）: key={}, duration={}", key, duration);
        } catch (Exception e) {
            log.error("设置缓存失败: key={}, duration={}", key, duration, e);
        }
    }
    
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(buildKey(key), value, timeout, unit);
            log.debug("设置缓存（带过期时间）: key={}, timeout={}, unit={}", key, timeout, unit);
        } catch (Exception e) {
            log.error("设置缓存失败: key={}, timeout={}, unit={}", key, timeout, unit, e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(buildKey(key));
            if (value != null) {
                hitCount.increment();
                log.debug("缓存命中: key={}", key);
                if (type.isInstance(value)) {
                    return Optional.of((T) value);
                } else {
                    try {
                        if (value instanceof Map) {
                            T converted = objectMapper.convertValue(value, type);
                            return Optional.of(converted);
                        }
                    } catch (IllegalArgumentException ex) {
                        log.warn("缓存值转换失败: key={}, expected={}, actual={}",
                                key, type.getSimpleName(), value.getClass().getSimpleName());
                    }
                    log.warn("缓存值类型不匹配: key={}, expected={}, actual={}",
                            key, type.getSimpleName(), value.getClass().getSimpleName());
                    return Optional.empty();
                }
            } else {
                missCount.increment();
                log.debug("缓存未命中: key={}", key);
                return Optional.empty();
            }
        } catch (Exception e) {
            missCount.increment();
            try {
                StringRedisSerializer keySerializer = (StringRedisSerializer) redisTemplate.getKeySerializer();
                byte[] rawKey = keySerializer.serialize(buildKey(key));
                byte[] rawValue = redisTemplate.execute((RedisCallback<byte[]>) connection -> connection.stringCommands().get(rawKey));
                if (rawValue != null) {
                    T converted = objectMapper.readValue(rawValue, type);
                    return Optional.of(converted);
                }
            } catch (Exception ex) {
                log.error("获取缓存失败: key={}", key, e);
            }
            return Optional.empty();
        }
    }
    
    
    @Override
    public <T> T get(String key, Class<T> type, T defaultValue) {
        return get(key, type).orElse(defaultValue);
    }
    
    @Override
    public <T> T getOrSet(String key, Class<T> type, Supplier<T> supplier) {
        Optional<T> cached = get(key, type);
        if (cached.isPresent()) {
            return cached.get();
        }
        
        T value = supplier.get();
        if (value != null) {
            set(key, value);
        }
        return value;
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
        try {
            Boolean result = redisTemplate.delete(buildKey(key));
            boolean deleted = Boolean.TRUE.equals(result);
            if (deleted) {
                evictionCount.increment();
                log.debug("删除缓存: key={}", key);
            }
            return deleted;
        } catch (Exception e) {
            log.error("删除缓存失败: key={}", key, e);
            return false;
        }
    }
    
    @Override
    public long delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        
        try {
            List<String> prefixed = keys.stream().map(this::buildKey).collect(Collectors.toList());
            Long result = redisTemplate.delete(prefixed);
            long deleted = result != null ? result : 0;
            if (deleted > 0) {
                evictionCount.add(deleted);
                log.debug("批量删除缓存: keys={}, deleted={}", keys.size(), deleted);
            }
            return deleted;
        } catch (Exception e) {
            log.error("批量删除缓存失败: keys={}", keys.size(), e);
            return 0;
        }
    }
    
    @Override
    public boolean exists(String key) {
        try {
            Boolean result = redisTemplate.hasKey(buildKey(key));
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("检查缓存存在性失败: key={}", key, e);
            return false;
        }
    }
    
    @Override
    public boolean expire(String key, Duration duration) {
        try {
            Boolean result = redisTemplate.expire(buildKey(key), duration);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("设置缓存过期时间失败: key={}, duration={}", key, duration, e);
            return false;
        }
    }
    
    @Override
    public Duration getExpire(String key) {
        try {
            Long seconds = redisTemplate.getExpire(buildKey(key));
            if (seconds != null) {
                return Duration.ofSeconds(seconds);
            }
            return Duration.ofSeconds(-1);
        } catch (Exception e) {
            log.error("获取缓存过期时间失败: key={}", key, e);
            return Duration.ofSeconds(-1);
        }
    }
    
    @Override
    public boolean persist(String key) {
        try {
            Boolean result = redisTemplate.persist(buildKey(key));
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("移除缓存过期时间失败: key={}", key, e);
            return false;
        }
    }
    
    @Override
    public long increment(String key) {
        return increment(key, 1);
    }
    
    @Override
    public long increment(String key, long delta) {
        try {
            Long result = redisTemplate.opsForValue().increment(buildKey(key), delta);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("递增操作失败: key={}, delta={}", key, delta, e);
            return 0;
        }
    }
    
    @Override
    public long decrement(String key) {
        return decrement(key, 1);
    }
    
    @Override
    public long decrement(String key, long delta) {
        try {
            Long result = redisTemplate.opsForValue().decrement(buildKey(key), delta);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("递减操作失败: key={}, delta={}", key, delta, e);
            return 0;
        }
    }
    
    @Override
    public void hSet(String key, String field, Object value) {
        try {
            redisTemplate.opsForHash().put(buildKey(key), field, value);
            log.debug("设置Hash缓存: key={}, field={}", key, field);
        } catch (Exception e) {
            log.error("设置Hash缓存失败: key={}, field={}", key, field, e);
        }
    }
    
    @Override
    public void hMSet(String key, Map<String, Object> fields) {
        try {
            redisTemplate.opsForHash().putAll(buildKey(key), fields);
            log.debug("批量设置Hash缓存: key={}, fields={}", key, fields.size());
        } catch (Exception e) {
            log.error("批量设置Hash缓存失败: key={}, fields={}", key, fields.size(), e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> hGet(String key, String field, Class<T> type) {
        try {
            Object value = redisTemplate.opsForHash().get(buildKey(key), field);
            if (value != null && type.isInstance(value)) {
                return Optional.of((T) value);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("获取Hash缓存失败: key={}, field={}", key, field, e);
            return Optional.empty();
        }
    }
    
    @Override
    public Map<String, Object> hGetAll(String key) {
        try {
            Map<Object, Object> result = redisTemplate.opsForHash().entries(buildKey(key));
            return result.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> entry.getKey().toString(),
                            Map.Entry::getValue
                    ));
        } catch (Exception e) {
            log.error("获取Hash所有字段失败: key={}", key, e);
            return new HashMap<>();
        }
    }
    
    @Override
    public long hDelete(String key, String... fields) {
        try {
            Long result = redisTemplate.opsForHash().delete(buildKey(key), (Object[]) fields);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("删除Hash字段失败: key={}, fields={}", key, Arrays.toString(fields), e);
            return 0;
        }
    }
    
    @Override
    public boolean hExists(String key, String field) {
        try {
            Boolean result = redisTemplate.opsForHash().hasKey(buildKey(key), field);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("检查Hash字段存在性失败: key={}, field={}", key, field, e);
            return false;
        }
    }
    
    @Override
    public long hLen(String key) {
        try {
            Long result = redisTemplate.opsForHash().size(buildKey(key));
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("获取Hash长度失败: key={}", key, e);
            return 0;
        }
    }
    
    @Override
    public Set<String> hKeys(String key) {
        try {
            Set<Object> keys = redisTemplate.opsForHash().keys(buildKey(key));
            return keys.stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("获取Hash所有键失败: key={}", key, e);
            return new HashSet<>();
        }
    }
    
    @Override
    public long hIncrement(String key, String field, long delta) {
        try {
            Long result = redisTemplate.opsForHash().increment(buildKey(key), field, delta);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("Hash字段递增失败: key={}, field={}, delta={}", key, field, delta, e);
            return 0;
        }
    }
    
    @Override
    public long lPush(String key, Object... values) {
        try {
            Long result = redisTemplate.opsForList().leftPushAll(buildKey(key), values);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("List左推入失败: key={}, count={}", key, values.length, e);
            return 0;
        }
    }
    
    @Override
    public long rPush(String key, Object... values) {
        try {
            Long result = redisTemplate.opsForList().rightPushAll(buildKey(key), values);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("List右推入失败: key={}, count={}", key, values.length, e);
            return 0;
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> lPop(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForList().leftPop(buildKey(key));
            if (value != null && type.isInstance(value)) {
                return Optional.of((T) value);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("List左弹出失败: key={}", key, e);
            return Optional.empty();
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> rPop(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForList().rightPop(buildKey(key));
            if (value != null && type.isInstance(value)) {
                return Optional.of((T) value);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("List右弹出失败: key={}", key, e);
            return Optional.empty();
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> lRange(String key, long start, long end, Class<T> type) {
        try {
            List<Object> values = redisTemplate.opsForList().range(buildKey(key), start, end);
            if (values != null) {
                return values.stream()
                        .filter(type::isInstance)
                        .map(value -> (T) value)
                        .collect(Collectors.toList());
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("List范围查询失败: key={}, start={}, end={}", key, start, end, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public long lLen(String key) {
        try {
            Long result = redisTemplate.opsForList().size(buildKey(key));
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("获取List长度失败: key={}", key, e);
            return 0;
        }
    }
    
    @Override
    public long sAdd(String key, Object... values) {
        try {
            Long result = redisTemplate.opsForSet().add(buildKey(key), values);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("Set添加元素失败: key={}, count={}", key, values.length, e);
            return 0;
        }
    }
    
    @Override
    public long sRem(String key, Object... values) {
        try {
            Long result = redisTemplate.opsForSet().remove(buildKey(key), values);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("Set移除元素失败: key={}, count={}", key, values.length, e);
            return 0;
        }
    }
    
    @Override
    public boolean sIsMember(String key, Object value) {
        try {
            Boolean result = redisTemplate.opsForSet().isMember(buildKey(key), value);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("检查Set成员失败: key={}", key, e);
            return false;
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Set<T> sMembers(String key, Class<T> type) {
        try {
            Set<Object> members = redisTemplate.opsForSet().members(buildKey(key));
            if (members != null) {
                return members.stream()
                        .filter(type::isInstance)
                        .map(member -> (T) member)
                        .collect(Collectors.toSet());
            }
            return new HashSet<>();
        } catch (Exception e) {
            log.error("获取Set所有成员失败: key={}", key, e);
            return new HashSet<>();
        }
    }
    
    @Override
    public long sCard(String key) {
        try {
            Long result = redisTemplate.opsForSet().size(buildKey(key));
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("获取Set大小失败: key={}", key, e);
            return 0;
        }
    }
    
    @Override
    public boolean zAdd(String key, Object value, double score) {
        try {
            Boolean result = redisTemplate.opsForZSet().add(buildKey(key), value, score);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("ZSet添加元素失败: key={}, value={}, score={}", key, value, score, e);
            return false;
        }
    }
    
    @Override
    public long zAdd(String key, Map<Object, Double> values) {
        try {
            Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> tuples = 
                    values.entrySet().stream()
                            .map(entry -> new org.springframework.data.redis.core.DefaultTypedTuple<>(
                                    entry.getKey(), entry.getValue()))
                            .collect(Collectors.toSet());
            Long result = redisTemplate.opsForZSet().add(buildKey(key), tuples);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("ZSet批量添加元素失败: key={}, count={}", key, values.size(), e);
            return 0;
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> zRange(String key, long start, long end, Class<T> type) {
        try {
            Set<Object> values = redisTemplate.opsForZSet().range(buildKey(key), start, end);
            if (values != null) {
                return values.stream()
                        .filter(type::isInstance)
                        .map(value -> (T) value)
                        .collect(Collectors.toList());
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("ZSet范围查询失败: key={}, start={}, end={}", key, start, end, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> zRangeByScore(String key, double minScore, double maxScore, Class<T> type) {
        try {
            Set<Object> values = redisTemplate.opsForZSet().rangeByScore(buildKey(key), minScore, maxScore);
            if (values != null) {
                return values.stream()
                        .filter(type::isInstance)
                        .map(value -> (T) value)
                        .collect(Collectors.toList());
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("ZSet按分数范围查询失败: key={}, minScore={}, maxScore={}", key, minScore, maxScore, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public Long zRank(String key, Object value) {
        try {
            return redisTemplate.opsForZSet().rank(buildKey(key), value);
        } catch (Exception e) {
            log.error("获取ZSet排名失败: key={}, value={}", key, value, e);
            return null;
        }
    }
    
    @Override
    public Double zScore(String key, Object value) {
        try {
            return redisTemplate.opsForZSet().score(buildKey(key), value);
        } catch (Exception e) {
            log.error("获取ZSet分数失败: key={}, value={}", key, value, e);
            return null;
        }
    }
    
    @Override
    public long zCard(String key) {
        try {
            Long result = redisTemplate.opsForZSet().size(buildKey(key));
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("获取ZSet大小失败: key={}", key, e);
            return 0;
        }
    }
    
    @Override
    public Set<String> keys(String pattern) {
        // 用 SCAN 游标替代阻塞的 KEYS；在缓存前缀命名空间内匹配调用方 pattern
        Set<String> raw = scanKeys(buildKey(pattern));
        // 返回给调用方时剥去前缀，保持对外 key 视图一致
        Set<String> result = new HashSet<>(raw.size());
        for (String k : raw) {
            result.add(k.startsWith(keyPrefix) ? k.substring(keyPrefix.length()) : k);
        }
        return result;
    }

    @Override
    public Set<String> scan(String pattern, long count) {
        return keys(pattern);
    }
    
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
    
    @Override
    public void clear() {
        if (keyPrefix == null || keyPrefix.isBlank()) {
            // 无前缀无法安全区分缓存 key 与业务 key，拒绝执行以免误删同库数据
            log.error("未配置缓存 key 前缀，clear() 拒绝执行以避免误删同库其他业务数据");
            return;
        }
        try {
            // 仅 SCAN + 删除本前缀命名空间下的 key，绝不 KEYS "*" 清全库
            Set<String> keys = scanKeys(keyPrefix + "*");
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                evictionCount.add(keys.size());
                log.info("清空缓存(前缀 {}): count={}", keyPrefix, keys.size());
            }
        } catch (Exception e) {
            log.error("清空缓存失败", e);
        }
    }
    
    @Override
    public CacheStats getStats() {
        return new DefaultCacheStats();
    }
    
    @Override
    public String getName() {
        return CACHE_NAME;
    }
    
    @Override
    public boolean isAvailable() {
        try {
            redisTemplate.opsForValue().set(keyPrefix + "health:check", "ok", Duration.ofSeconds(1));
            return true;
        } catch (Exception e) {
            log.error("缓存不可用", e);
            return false;
        }
    }
    
    /**
     * 默认缓存统计实现
     */
    private class DefaultCacheStats implements CacheStats {
        
        @Override
        public long getHitCount() {
            return hitCount.sum();
        }
        
        @Override
        public long getMissCount() {
            return missCount.sum();
        }
        
        @Override
        public double getHitRate() {
            long h = hitCount.sum();
            long total = h + missCount.sum();
            return total > 0 ? (double) h / total : 0.0;
        }
        
        @Override
        public long getSize() {
            try {
                // 仅统计本缓存前缀命名空间, 用 SCAN 而非阻塞 KEYS
                return scanKeys(keyPrefix + "*").size();
            } catch (Exception e) {
                log.error("获取缓存大小失败", e);
                return 0;
            }
        }
        
        @Override
        public long getEvictionCount() {
            return evictionCount.sum();
        }
    }
}

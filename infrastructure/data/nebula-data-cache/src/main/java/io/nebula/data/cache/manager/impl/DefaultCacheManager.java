package io.nebula.data.cache.manager.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.nebula.data.cache.manager.CacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 基于Redis的缓存管理器默认实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultCacheManager implements CacheManager {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CACHE_NAME = "DefaultCache";
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
    // 统计信息
    private volatile long hitCount = 0;
    private volatile long missCount = 0;
    private volatile long evictionCount = 0;
    
    @Override
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("设置缓存: key={}", key);
        } catch (Exception e) {
            log.error("设置缓存失败: key={}", key, e);
        }
    }
    
    @Override
    public void set(String key, Object value, Duration duration) {
        try {
            redisTemplate.opsForValue().set(key, value, duration);
            log.debug("设置缓存（带过期时间）: key={}, duration={}", key, duration);
        } catch (Exception e) {
            log.error("设置缓存失败: key={}, duration={}", key, duration, e);
        }
    }
    
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("设置缓存（带过期时间）: key={}, timeout={}, unit={}", key, timeout, unit);
        } catch (Exception e) {
            log.error("设置缓存失败: key={}, timeout={}, unit={}", key, timeout, unit, e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                hitCount++;
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
                missCount++;
                log.debug("缓存未命中: key={}", key);
                return Optional.empty();
            }
        } catch (Exception e) {
            missCount++;
            try {
                StringRedisSerializer keySerializer = (StringRedisSerializer) redisTemplate.getKeySerializer();
                byte[] rawKey = keySerializer.serialize(key);
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
            Boolean result = redisTemplate.delete(key);
            boolean deleted = Boolean.TRUE.equals(result);
            if (deleted) {
                evictionCount++;
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
            Long result = redisTemplate.delete(keys);
            long deleted = result != null ? result : 0;
            if (deleted > 0) {
                evictionCount += deleted;
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
            Boolean result = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("检查缓存存在性失败: key={}", key, e);
            return false;
        }
    }
    
    @Override
    public boolean expire(String key, Duration duration) {
        try {
            Boolean result = redisTemplate.expire(key, duration);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("设置缓存过期时间失败: key={}, duration={}", key, duration, e);
            return false;
        }
    }
    
    @Override
    public Duration getExpire(String key) {
        try {
            Long seconds = redisTemplate.getExpire(key);
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
            Boolean result = redisTemplate.persist(key);
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
            Long result = redisTemplate.opsForValue().increment(key, delta);
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
            Long result = redisTemplate.opsForValue().decrement(key, delta);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("递减操作失败: key={}, delta={}", key, delta, e);
            return 0;
        }
    }
    
    @Override
    public void hSet(String key, String field, Object value) {
        try {
            redisTemplate.opsForHash().put(key, field, value);
            log.debug("设置Hash缓存: key={}, field={}", key, field);
        } catch (Exception e) {
            log.error("设置Hash缓存失败: key={}, field={}", key, field, e);
        }
    }
    
    @Override
    public void hMSet(String key, Map<String, Object> fields) {
        try {
            redisTemplate.opsForHash().putAll(key, fields);
            log.debug("批量设置Hash缓存: key={}, fields={}", key, fields.size());
        } catch (Exception e) {
            log.error("批量设置Hash缓存失败: key={}, fields={}", key, fields.size(), e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> hGet(String key, String field, Class<T> type) {
        try {
            Object value = redisTemplate.opsForHash().get(key, field);
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
            Map<Object, Object> result = redisTemplate.opsForHash().entries(key);
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
            Long result = redisTemplate.opsForHash().delete(key, (Object[]) fields);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("删除Hash字段失败: key={}, fields={}", key, Arrays.toString(fields), e);
            return 0;
        }
    }
    
    @Override
    public boolean hExists(String key, String field) {
        try {
            Boolean result = redisTemplate.opsForHash().hasKey(key, field);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("检查Hash字段存在性失败: key={}, field={}", key, field, e);
            return false;
        }
    }
    
    @Override
    public long hLen(String key) {
        try {
            Long result = redisTemplate.opsForHash().size(key);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("获取Hash长度失败: key={}", key, e);
            return 0;
        }
    }
    
    @Override
    public Set<String> hKeys(String key) {
        try {
            Set<Object> keys = redisTemplate.opsForHash().keys(key);
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
            Long result = redisTemplate.opsForHash().increment(key, field, delta);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("Hash字段递增失败: key={}, field={}, delta={}", key, field, delta, e);
            return 0;
        }
    }
    
    @Override
    public long lPush(String key, Object... values) {
        try {
            Long result = redisTemplate.opsForList().leftPushAll(key, values);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("List左推入失败: key={}, count={}", key, values.length, e);
            return 0;
        }
    }
    
    @Override
    public long rPush(String key, Object... values) {
        try {
            Long result = redisTemplate.opsForList().rightPushAll(key, values);
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
            Object value = redisTemplate.opsForList().leftPop(key);
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
            Object value = redisTemplate.opsForList().rightPop(key);
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
            List<Object> values = redisTemplate.opsForList().range(key, start, end);
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
            Long result = redisTemplate.opsForList().size(key);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("获取List长度失败: key={}", key, e);
            return 0;
        }
    }
    
    @Override
    public long sAdd(String key, Object... values) {
        try {
            Long result = redisTemplate.opsForSet().add(key, values);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("Set添加元素失败: key={}, count={}", key, values.length, e);
            return 0;
        }
    }
    
    @Override
    public long sRem(String key, Object... values) {
        try {
            Long result = redisTemplate.opsForSet().remove(key, values);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("Set移除元素失败: key={}, count={}", key, values.length, e);
            return 0;
        }
    }
    
    @Override
    public boolean sIsMember(String key, Object value) {
        try {
            Boolean result = redisTemplate.opsForSet().isMember(key, value);
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
            Set<Object> members = redisTemplate.opsForSet().members(key);
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
            Long result = redisTemplate.opsForSet().size(key);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("获取Set大小失败: key={}", key, e);
            return 0;
        }
    }
    
    @Override
    public boolean zAdd(String key, Object value, double score) {
        try {
            Boolean result = redisTemplate.opsForZSet().add(key, value, score);
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
            Long result = redisTemplate.opsForZSet().add(key, tuples);
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
            Set<Object> values = redisTemplate.opsForZSet().range(key, start, end);
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
            Set<Object> values = redisTemplate.opsForZSet().rangeByScore(key, minScore, maxScore);
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
            return redisTemplate.opsForZSet().rank(key, value);
        } catch (Exception e) {
            log.error("获取ZSet排名失败: key={}, value={}", key, value, e);
            return null;
        }
    }
    
    @Override
    public Double zScore(String key, Object value) {
        try {
            return redisTemplate.opsForZSet().score(key, value);
        } catch (Exception e) {
            log.error("获取ZSet分数失败: key={}, value={}", key, value, e);
            return null;
        }
    }
    
    @Override
    public long zCard(String key) {
        try {
            Long result = redisTemplate.opsForZSet().size(key);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("获取ZSet大小失败: key={}", key, e);
            return 0;
        }
    }
    
    @Override
    public Set<String> keys(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            return keys != null ? keys : new HashSet<>();
        } catch (Exception e) {
            log.error("模式匹配失败: pattern={}", pattern, e);
            return new HashSet<>();
        }
    }
    
    @Override
    public Set<String> scan(String pattern, long count) {
        // 简化实现，生产环境应该使用更复杂的扫描逻辑
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
        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                evictionCount += keys.size();
                log.info("清空所有缓存: count={}", keys.size());
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
            redisTemplate.opsForValue().set("health:check", "ok", Duration.ofSeconds(1));
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
            return hitCount;
        }
        
        @Override
        public long getMissCount() {
            return missCount;
        }
        
        @Override
        public double getHitRate() {
            long total = hitCount + missCount;
            return total > 0 ? (double) hitCount / total : 0.0;
        }
        
        @Override
        public long getSize() {
            try {
                Set<String> keys = redisTemplate.keys("*");
                return keys != null ? keys.size() : 0;
            } catch (Exception e) {
                log.error("获取缓存大小失败", e);
                return 0;
            }
        }
        
        @Override
        public long getEvictionCount() {
            return evictionCount;
        }
    }
}

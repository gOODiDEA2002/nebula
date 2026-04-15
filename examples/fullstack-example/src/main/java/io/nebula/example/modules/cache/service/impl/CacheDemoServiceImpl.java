package io.nebula.example.modules.cache.service.impl;

import io.nebula.data.cache.manager.CacheManager;
import io.nebula.data.cache.manager.MultiLevelCacheManager;
import io.nebula.example.modules.cache.entity.dto.*;
import io.nebula.example.modules.cache.service.CacheDemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存演示服务实现类
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheDemoServiceImpl implements CacheDemoService {

    private final CacheManager cacheManager;
    
    // 模拟用户数据存储
    private final Map<Long, GetUserDto.Response> userDatabase = new ConcurrentHashMap<>();
    private final AtomicLong userIdGenerator = new AtomicLong(1);
    
    @Override
    public CacheSetDto.Response setCache(CacheSetDto.Request request) {
        log.info("设置缓存，key: {}, value: {}, ttl: {}", request.getKey(), request.getValue(), request.getTtlSeconds());
        
        try {
            if (request.getTtlSeconds() != null && request.getTtlSeconds() > 0) {
                cacheManager.set(request.getKey(), request.getValue(), Duration.ofSeconds(request.getTtlSeconds()));
            } else {
                cacheManager.set(request.getKey(), request.getValue());
            }
            
            CacheSetDto.Response response = new CacheSetDto.Response();
            response.setSuccess(true);
            response.setKey(request.getKey());
            response.setValue(request.getValue());
            response.setTtlSeconds(request.getTtlSeconds());
            
            log.info("缓存设置成功，key: {}", request.getKey());
            return response;
            
        } catch (Exception e) {
            log.error("设置缓存失败，key: {}", request.getKey(), e);
            
            CacheSetDto.Response response = new CacheSetDto.Response();
            response.setSuccess(false);
            response.setKey(request.getKey());
            return response;
        }
    }

    @Override
    public CacheGetDto.Response getCache(CacheGetDto.Request request) {
        log.info("获取缓存，key: {}, valueType: {}", request.getKey(), request.getValueType());
        
        try {
            // 根据类型获取缓存值
            Optional<?> valueOpt = switch (request.getValueType().toLowerCase()) {
                case "string" -> cacheManager.get(request.getKey(), String.class);
                case "integer" -> cacheManager.get(request.getKey(), Integer.class);
                case "long" -> cacheManager.get(request.getKey(), Long.class);
                default -> cacheManager.get(request.getKey(), Object.class);
            };
            
            CacheGetDto.Response response = new CacheGetDto.Response();
            response.setKey(request.getKey());
            response.setExists(valueOpt.isPresent());
            response.setValue(valueOpt.orElse(null));
            
            // 获取剩余TTL
            Duration remainingTtl = cacheManager.getExpire(request.getKey());
            if (!remainingTtl.equals(Duration.ofSeconds(-1))) {
                response.setRemainingTtlSeconds(remainingTtl.getSeconds());
            }
            
            // 判断缓存来源（如果是多级缓存）
            if (valueOpt.isPresent()) {
                response.setSource(determineCacheSource(request.getKey()));
            } else {
                response.setSource("MISS");
            }
            
            log.info("缓存获取{}，key: {}, exists: {}", 
                    valueOpt.isPresent() ? "成功" : "未命中", 
                    request.getKey(), response.getExists());
            
            return response;
            
        } catch (Exception e) {
            log.error("获取缓存失败，key: {}", request.getKey(), e);
            
            CacheGetDto.Response response = new CacheGetDto.Response();
            response.setKey(request.getKey());
            response.setExists(false);
            response.setSource("ERROR");
            return response;
        }
    }

    @Override
    public CacheDeleteDto.Response deleteCache(CacheDeleteDto.Request request) {
        log.info("删除缓存，keys: {}", request.getKeys());
        
        try {
            List<String> deletedKeys = new ArrayList<>();
            long deletedCount = 0;
            
            for (String key : request.getKeys()) {
                if (cacheManager.delete(key)) {
                    deletedKeys.add(key);
                    deletedCount++;
                }
            }
            
            CacheDeleteDto.Response response = new CacheDeleteDto.Response();
            response.setDeletedCount(deletedCount);
            response.setRequestedKeys(request.getKeys());
            response.setDeletedKeys(deletedKeys);
            
            log.info("缓存删除完成，删除数量: {}/{}", deletedCount, request.getKeys().size());
            return response;
            
        } catch (Exception e) {
            log.error("删除缓存失败", e);
            
            CacheDeleteDto.Response response = new CacheDeleteDto.Response();
            response.setDeletedCount(0L);
            response.setRequestedKeys(request.getKeys());
            response.setDeletedKeys(Collections.emptyList());
            return response;
        }
    }

    @Override
    public CacheStatsDto.Response getCacheStats() {
        log.info("获取缓存统计信息");
        
        try {
            CacheManager.CacheStats stats = cacheManager.getStats();
            
            CacheStatsDto.Response response = new CacheStatsDto.Response();
            response.setCacheName(cacheManager.getName());
            response.setHitCount(stats.getHitCount());
            response.setMissCount(stats.getMissCount());
            response.setHitRate(stats.getHitRate());
            response.setSize(stats.getSize());
            response.setEvictionCount(stats.getEvictionCount());
            response.setAvailable(cacheManager.isAvailable());
            
            // 如果是多级缓存，获取详细统计
            if (cacheManager instanceof MultiLevelCacheManager) {
                MultiLevelCacheManager multiLevelCacheManager = (MultiLevelCacheManager) cacheManager;
                MultiLevelCacheManager.MultiLevelCacheStats multiStats = 
                        (MultiLevelCacheManager.MultiLevelCacheStats) multiLevelCacheManager.getStats();
                
                response.setL1HitCount(multiStats.getL1HitCount());
                response.setL2HitCount(multiStats.getL2HitCount());
                response.setL1HitRate(multiStats.getL1HitRate());
                response.setL2HitRate(multiStats.getL2HitRate());
                response.setTotalRequestCount(multiStats.getTotalRequestCount());
            }
            
            log.info("缓存统计获取成功，命中率: {:.2f}%", stats.getHitRate() * 100);
            return response;
            
        } catch (Exception e) {
            log.error("获取缓存统计失败", e);
            
            CacheStatsDto.Response response = new CacheStatsDto.Response();
            response.setCacheName("Unknown");
            response.setAvailable(false);
            return response;
        }
    }

    @Override
    public CacheKeysDto.Response getCacheKeys(CacheKeysDto.Request request) {
        log.info("查询缓存键，pattern: {}", request.getPattern());
        
        try {
            Set<String> keys = cacheManager.keys(request.getPattern());
            
            CacheKeysDto.Response response = new CacheKeysDto.Response();
            response.setPattern(request.getPattern());
            response.setKeys(keys);
            response.setCount(keys.size());
            
            log.info("缓存键查询完成，匹配数量: {}", keys.size());
            return response;
            
        } catch (Exception e) {
            log.error("查询缓存键失败，pattern: {}", request.getPattern(), e);
            
            CacheKeysDto.Response response = new CacheKeysDto.Response();
            response.setPattern(request.getPattern());
            response.setKeys(Collections.emptySet());
            response.setCount(0);
            return response;
        }
    }

    @Override
    public void clearAllCache() {
        log.info("清空所有缓存");
        
        try {
            cacheManager.clear();
            log.info("所有缓存清空成功");
        } catch (Exception e) {
            log.error("清空缓存失败", e);
            throw new RuntimeException("清空缓存失败", e);
        }
    }

    // ========== Spring Cache注解演示 ==========

    @Override
    @Cacheable(value = "users", key = "#request.userId")
    public GetUserDto.Response getUser(GetUserDto.Request request) {
        log.info("从数据库获取用户信息，userId: {}", request.getUserId());
        
        // 模拟数据库查询延迟
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        GetUserDto.Response user = userDatabase.get(request.getUserId());
        if (user != null) {
            user.setSource("Database");
            log.info("用户信息查询成功，userId: {}", request.getUserId());
        } else {
            log.warn("用户不存在，userId: {}", request.getUserId());
        }
        
        return user;
    }

    @Override
    @CachePut(value = "users", key = "#result.userId")
    public CreateUserDto.Response createUser(CreateUserDto.Request request) {
        log.info("创建用户，username: {}", request.getUsername());
        
        Long userId = userIdGenerator.getAndIncrement();
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        CreateUserDto.Response user = new CreateUserDto.Response();
        user.setUserId(userId);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setAge(request.getAge());
        user.setSource("Database");
        user.setCreateTime(currentTime);
        user.setUpdateTime(currentTime);
        
        // 将用户数据存储到数据库的时候，需要转换为GetUserDto.Response格式
        GetUserDto.Response userForDatabase = new GetUserDto.Response();
        userForDatabase.setUserId(userId);
        userForDatabase.setUsername(request.getUsername());
        userForDatabase.setEmail(request.getEmail());
        userForDatabase.setAge(request.getAge());
        userForDatabase.setSource("Database");
        userForDatabase.setCreateTime(currentTime);
        userForDatabase.setUpdateTime(currentTime);
        userDatabase.put(userId, userForDatabase);
        
        log.info("用户创建成功，userId: {}", userId);
        return user;
    }

    @Override
    @CachePut(value = "users", key = "#request.userId")
    public UpdateUserDto.Response updateUser(UpdateUserDto.Request request) {
        log.info("更新用户，userId: {}", request.getUserId());
        
        GetUserDto.Response existingUser = userDatabase.get(request.getUserId());
        if (existingUser == null) {
            log.warn("用户不存在，无法更新，userId: {}", request.getUserId());
            return null;
        }
        
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        if (request.getUsername() != null) {
            existingUser.setUsername(request.getUsername());
        }
        if (request.getEmail() != null) {
            existingUser.setEmail(request.getEmail());
        }
        if (request.getAge() != null) {
            existingUser.setAge(request.getAge());
        }
        existingUser.setSource("Database");
        existingUser.setUpdateTime(currentTime);
        
        userDatabase.put(request.getUserId(), existingUser);
        
        // 返回 UpdateUserDto.Response 格式
        UpdateUserDto.Response response = new UpdateUserDto.Response();
        response.setUserId(existingUser.getUserId());
        response.setUsername(existingUser.getUsername());
        response.setEmail(existingUser.getEmail());
        response.setAge(existingUser.getAge());
        response.setSource(existingUser.getSource());
        response.setCreateTime(existingUser.getCreateTime());
        response.setUpdateTime(existingUser.getUpdateTime());
        
        log.info("用户更新成功，userId: {}", request.getUserId());
        return response;
    }

    @Override
    @CacheEvict(value = "users", key = "#request.userId")
    public DeleteUserDto.Response deleteUser(DeleteUserDto.Request request) {
        log.info("删除用户，userId: {}", request.getUserId());
        
        GetUserDto.Response removedUser = userDatabase.remove(request.getUserId());
        boolean success = removedUser != null;
        
        DeleteUserDto.Response response = new DeleteUserDto.Response();
        response.setSuccess(success);
        response.setUserId(request.getUserId());
        
        if (success) {
            log.info("用户删除成功，userId: {}", request.getUserId());
        } else {
            log.warn("用户不存在，无法删除，userId: {}", request.getUserId());
        }
        
        return response;
    }

    // ========== 私有方法 ==========

    /**
     * 判断缓存来源（简化实现）
     */
    private String determineCacheSource(String key) {
        // 这里只是简化实现，实际可以通过监控缓存层级来判断
        if (cacheManager instanceof MultiLevelCacheManager) {
            MultiLevelCacheManager multiLevel = (MultiLevelCacheManager) cacheManager;
            
            // 检查L1缓存
            Optional<?> l1Value = multiLevel.getL1Cache().get(key, Object.class);
            if (l1Value.isPresent()) {
                return "L1";
            }
            
            // 检查L2缓存
            Optional<?> l2Value = multiLevel.getL2Cache().get(key, Object.class);
            if (l2Value.isPresent()) {
                return "L2";
            }
            
            return "MISS";
        } else {
            return "SINGLE";
        }
    }
}

package io.nebula.example.modules.cache.service;

import io.nebula.example.modules.cache.entity.dto.*;

/**
 * 缓存演示服务接口
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public interface CacheDemoService {

    /**
     * 设置缓存
     */
    CacheSetDto.Response setCache(CacheSetDto.Request request);

    /**
     * 获取缓存
     */
    CacheGetDto.Response getCache(CacheGetDto.Request request);

    /**
     * 删除缓存
     */
    CacheDeleteDto.Response deleteCache(CacheDeleteDto.Request request);

    /**
     * 获取缓存统计信息
     */
    CacheStatsDto.Response getCacheStats();

    /**
     * 查询缓存键
     */
    CacheKeysDto.Response getCacheKeys(CacheKeysDto.Request request);

    /**
     * 清空所有缓存
     */
    void clearAllCache();

    /**
     * 获取用户信息（Spring Cache注解演示）
     */
    GetUserDto.Response getUser(GetUserDto.Request request);

    /**
     * 创建用户（Spring Cache注解演示）
     */
    CreateUserDto.Response createUser(CreateUserDto.Request request);

    /**
     * 更新用户（Spring Cache注解演示）
     */
    UpdateUserDto.Response updateUser(UpdateUserDto.Request request);

    /**
     * 删除用户（Spring Cache注解演示）
     */
    DeleteUserDto.Response deleteUser(DeleteUserDto.Request request);
}

package io.nebula.example.modules.cache.controller;

import io.nebula.core.common.result.Result;
import io.nebula.example.modules.cache.entity.dto.*;
import io.nebula.example.modules.cache.service.CacheDemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 缓存演示控制器
 * 演示 Nebula 缓存模块的完整功能
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor
@Validated
@Tag(name = "缓存演示", description = "Nebula 缓存模块功能演示API")
public class CacheController {
    
    private final CacheDemoService cacheDemoService;
    
    // ========== 基础缓存操作 ==========
    
    @Operation(summary = "设置缓存", description = "设置缓存键值对，支持自定义TTL")
    @PostMapping("/set")
    public Result<CacheSetDto.Response> setCache(@Valid @RequestBody CacheSetDto.Request request) {
        log.info("接收设置缓存请求: key={}", request.getKey());
        CacheSetDto.Response response = cacheDemoService.setCache(request);
        return Result.success(response, "缓存设置成功");
    }
    
    @Operation(summary = "获取缓存", description = "根据键获取缓存值，支持多种数据类型")
    @PostMapping("/get")
    public Result<CacheGetDto.Response> getCache(@Valid @RequestBody CacheGetDto.Request request) {
        log.info("获取缓存，key: {}", request.getKey());
        CacheGetDto.Response response = cacheDemoService.getCache(request);
        String message = response.getExists() ? "缓存获取成功" : "缓存不存在";
        return Result.success(response, message);
    }
    
    @Operation(summary = "删除缓存", description = "删除指定的缓存键")
    @DeleteMapping("/delete")
    public Result<CacheDeleteDto.Response> deleteCache(@Valid @RequestBody CacheDeleteDto.Request request) {
        log.info("删除缓存，keys: {}", request.getKeys());
        CacheDeleteDto.Response response = cacheDemoService.deleteCache(request);
        return Result.success(response, "缓存删除成功");
    }
    
    @Operation(summary = "获取缓存统计", description = "获取缓存系统的统计信息（命中率、大小等）")
    @GetMapping("/stats")
    public Result<CacheStatsDto.Response> getCacheStats() {
        log.info("获取缓存统计信息");
        CacheStatsDto.Response response = cacheDemoService.getCacheStats();
        return Result.success(response, "缓存统计获取成功");
    }
    
    @Operation(summary = "查询缓存键", description = "根据模式查询匹配的缓存键")
    @PostMapping("/keys")
    public Result<CacheKeysDto.Response> getCacheKeys(@Valid @RequestBody CacheKeysDto.Request request) {
        log.info("查询缓存键，pattern: {}", request.getPattern());
        CacheKeysDto.Response response = cacheDemoService.getCacheKeys(request);
        return Result.success(response, "缓存键查询成功");
    }
    
    @Operation(summary = "清空所有缓存", description = "清空缓存系统中的所有数据")
    @PostMapping("/clear")
    public Result<Void> clearAllCache() {
        log.info("清空所有缓存");
        cacheDemoService.clearAllCache();
        return Result.success(null, "所有缓存清空成功");
    }
    
    // ========== Spring Cache注解演示 ==========
    
    @Operation(summary = "获取用户信息", description = "使用@Cacheable注解演示缓存功能")
    @PostMapping("/users/get")
    public Result<GetUserDto.Response> getUser(@Valid @RequestBody GetUserDto.Request request) {
        log.info("获取用户信息，userId: {}", request.getUserId());
        GetUserDto.Response response = cacheDemoService.getUser(request);
        if (response != null) {
            return Result.success(response, "用户信息获取成功");
        } else {
            return Result.error("USER_NOT_FOUND", "用户不存在");
        }
    }
    
    @Operation(summary = "创建用户", description = "使用@CachePut注解演示缓存更新功能")
    @PostMapping("/users/create")
    public Result<CreateUserDto.Response> createUser(@Valid @RequestBody CreateUserDto.Request request) {
        log.info("创建用户，username: {}", request.getUsername());
        CreateUserDto.Response response = cacheDemoService.createUser(request);
        return Result.success(response, "用户创建成功");
    }
    
    @Operation(summary = "更新用户信息", description = "使用@CachePut注解演示缓存更新功能")
    @PutMapping("/users/update")
    public Result<UpdateUserDto.Response> updateUser(@Valid @RequestBody UpdateUserDto.Request request) {
        log.info("更新用户，userId: {}", request.getUserId());
        UpdateUserDto.Response response = cacheDemoService.updateUser(request);
        if (response != null) {
            return Result.success(response, "用户更新成功");
        } else {
            return Result.error("USER_NOT_FOUND", "用户不存在");
        }
    }
    
    @Operation(summary = "删除用户", description = "使用@CacheEvict注解演示缓存失效功能")
    @DeleteMapping("/users/delete")
    public Result<DeleteUserDto.Response> deleteUser(@Valid @RequestBody DeleteUserDto.Request request) {
        log.info("删除用户，userId: {}", request.getUserId());
        DeleteUserDto.Response response = cacheDemoService.deleteUser(request);
        String message = response.getSuccess() ? "用户删除成功" : "用户不存在";
        return Result.success(response, message);
    }
}

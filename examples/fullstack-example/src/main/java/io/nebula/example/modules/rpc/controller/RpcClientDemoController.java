package io.nebula.example.modules.rpc.controller;

import io.nebula.core.common.result.Result;

import io.nebula.example.user.api.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.nebula.example.user.api.rpc.UserRpcClient;
import lombok.RequiredArgsConstructor;
/**
 * RPC客户端演示控制器
 * 展示如何使用RPC客户端调用远程服务
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/rpc-client/users")
@Validated
@RequiredArgsConstructor
@Tag(name = "RPC客户端演示", description = "Nebula RPC客户端调用演示API")
public class RpcClientDemoController {

    private final UserRpcClient userRpcService;

    @Operation(summary = "RPC创建用户", description = "通过RPC调用创建用户")
    @PostMapping
    public Result<CreateUserDto.Response> createUserViaRpc(@Valid @RequestBody CreateUserDto.Request request) {
        log.info("RPC创建用户: {}", request.getUsername());
        CreateUserDto.Response response = userRpcService.createUser(request);
        return Result.success(response, "RPC创建用户成功");
    }
    
    @Operation(summary = "RPC获取用户详情", description = "通过RPC调用获取用户详情")
    @GetMapping("/{id}")
    public Result<GetUserDto.Response> getUserByIdViaRpc(@PathVariable Long id) {
        log.info("RPC获取用户详情: id={}", id);
        GetUserDto.Response response = userRpcService.getUserById(id);
        return Result.success(response, "RPC获取用户详情成功");
    }
    
    @Operation(summary = "RPC获取用户列表", description = "通过RPC调用获取用户列表")
    @GetMapping
    public Result<GetUsersDto.Response> getUsersViaRpc(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("RPC查询用户列表: page={}, size={}", page, size);
        GetUsersDto.Response response = userRpcService.getUsers(username, name, status, page, size);
        return Result.success(response, "RPC查询用户列表成功");
    }
    
    @Operation(summary = "RPC更新用户", description = "通过RPC调用更新用户")
    @PutMapping("/{id}")
    public Result<UpdateUserDto.Response> updateUserViaRpc(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserDto.Request request) {
        log.info("RPC更新用户: id={}", id);
        request.setId(id);
        UpdateUserDto.Response response = userRpcService.updateUser(id, request);
        return Result.success(response, "RPC更新用户成功");
    }
    
    @Operation(summary = "RPC删除用户", description = "通过RPC调用删除用户")
    @DeleteMapping("/{id}")
    public Result<DeleteUserDto.Response> deleteUserViaRpc(@PathVariable Long id) {
        log.info("RPC删除用户: id={}", id);
        DeleteUserDto.Response response = userRpcService.deleteUser(id);
        return Result.success(response, "RPC删除用户成功");
    }
}


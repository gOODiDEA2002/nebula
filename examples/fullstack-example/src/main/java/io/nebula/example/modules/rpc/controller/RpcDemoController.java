// package io.nebula.example.modules.rpc.controller;

// import io.nebula.core.common.result.Result;
// import io.nebula.example.api.dto.*;
// import io.nebula.example.api.rpc.UserRpcService;
// import io.swagger.v3.oas.annotations.Operation;
// import io.swagger.v3.oas.annotations.tags.Tag;
// import jakarta.validation.Valid;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Qualifier;
// import org.springframework.validation.annotation.Validated;
// import org.springframework.web.bind.annotation.*;

// /**
//  * RPC演示控制器
//  * 提供用户管理的REST API接口，通过RPC调用远程服务
//  * 
//  * @author Nebula Framework
//  * @since 2.0.0
//  */
// @Slf4j
// @RestController
// @RequestMapping("/rpc/users")
// @Validated
// @Tag(name = "RPC演示", description = "Nebula RPC功能演示API - 通过RPC调用远程服务")
// public class RpcDemoController {
    
//     private final UserRpcService userRpcService;
    
//     public RpcDemoController(@Qualifier("userRpcClient") UserRpcService userRpcService) {
//         this.userRpcService = userRpcService;
//     }
    
//     @Operation(summary = "创建用户", description = "创建新用户 - 通过RPC调用远程服务")
//     @PostMapping
//     public Result<CreateUserDto.Response> createUser(@Valid @RequestBody CreateUserDto.Request request) {
//         log.info("接收创建用户请求: {} - 将通过RPC调用远程服务", request.getUsername());
//         CreateUserDto.Response response = userRpcService.createUser(request);
//         return Result.success(response);
//     }
    
//     @Operation(summary = "获取用户详情", description = "根据用户ID获取详情 - 通过RPC调用远程服务")
//     @GetMapping("/{id}")
//     public Result<GetUserDto.Response> getUserById(@PathVariable Long id) {
//         log.info("获取用户详情: id={} - 将通过RPC调用远程服务", id);
//         GetUserDto.Response response = userRpcService.getUserById(id);
//         return Result.success(response, "获取用户详情成功");
//     }
    
//     @Operation(summary = "获取用户列表", description = "分页查询用户列表 - 通过RPC调用远程服务")
//     @GetMapping
//     public Result<GetUsersDto.Response> getUsers(
//             @RequestParam(required = false) String username,
//             @RequestParam(required = false) String name,
//             @RequestParam(required = false) String status,
//             @RequestParam(defaultValue = "1") Integer page,
//             @RequestParam(defaultValue = "10") Integer size) {
//         log.info("查询用户列表: page={}, size={} - 将通过RPC调用远程服务", page, size);
        
//         GetUsersDto.Response response = userRpcService.getUsers(username, name, status, page, size);
//         return Result.success(response, "查询用户列表成功");
//     }
    
//     @Operation(summary = "更新用户", description = "更新用户信息 - 通过RPC调用远程服务")
//     @PutMapping("/{id}")
//     public Result<UpdateUserDto.Response> updateUser(
//             @PathVariable Long id,
//             @Valid @RequestBody UpdateUserDto.Request request) {
//         log.info("更新用户: id={} - 将通过RPC调用远程服务", id);
//         UpdateUserDto.Response response = userRpcService.updateUser(id, request);
//         return Result.success(response, "更新用户成功");
//     }
    
//     @Operation(summary = "删除用户", description = "删除用户 - 通过RPC调用远程服务")
//     @DeleteMapping("/{id}")
//     public Result<DeleteUserDto.Response> deleteUser(@PathVariable Long id) {
//         log.info("删除用户: id={} - 将通过RPC调用远程服务", id);
//         DeleteUserDto.Response response = userRpcService.deleteUser(id);
//         return Result.success(response, "删除用户成功");
//     }
// }


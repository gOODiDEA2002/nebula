package io.nebula.example.user.api.rpc;

import io.nebula.example.user.api.dto.*;
import io.nebula.rpc.core.annotation.RemoteService;
import io.nebula.example.user.api.dto.*;

/**
 * 用户RPC服务接口
 * 使用声明式注解方式定义RPC客户端
 * 
 * 此接口定义在独立的API模块中，可以被服务提供方和消费方共享
 * 
 * 优化说明：
 * 1. 无需 @RpcCall 注解，框架会自动基于方法名调用
 * 2. 无需 @RequestBody、@PathVariable、@RequestParam 注解，参数自动传递
 * 3. value 和 contextId 可省略，框架会自动推导
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@RemoteService
public interface UserRpcClient {
    
    /**
     * 创建用户
     * RPC 路径自动映射为：{serviceName}/createUser
     */
    CreateUserDto.Response createUser(CreateUserDto.Request request);
    
    /**
     * 获取用户详情
     * RPC 路径自动映射为：{serviceName}/getUserById
     */
    GetUserDto.Response getUserById(Long id);
    
    /**
     * 获取用户列表
     * RPC 路径自动映射为：{serviceName}/getUsers
     * 
     * 注意：所有参数会作为请求对象的字段传递
     */
    GetUsersDto.Response getUsers(
        String username,
        String name,
        String status,
        Integer page,
        Integer size
    );
    
    /**
     * 更新用户
     * RPC 路径自动映射为：{serviceName}/updateUser
     */
    UpdateUserDto.Response updateUser(Long id, UpdateUserDto.Request request);
    
    /**
     * 删除用户
     * RPC 路径自动映射为：{serviceName}/deleteUser
     */
    DeleteUserDto.Response deleteUser(Long id);
}


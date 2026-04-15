package io.nebula.example.user.service.business;

import io.nebula.example.user.api.dto.*;

/**
 * RPC演示服务接口
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public interface RpcDemoService {
    
    /**
     * 创建用户
     */
    CreateUserDto.Response createUser(CreateUserDto.Request request);
    
    /**
     * 获取用户详情
     */
    GetUserDto.Response getUserById(GetUserDto.Request request);
    
    /**
     * 获取用户列表
     */
    GetUsersDto.Response getUsers(GetUsersDto.Request request);
    
    /**
     * 更新用户
     */
    UpdateUserDto.Response updateUser(UpdateUserDto.Request request);
    
    /**
     * 删除用户
     */
    DeleteUserDto.Response deleteUser(DeleteUserDto.Request request);
}


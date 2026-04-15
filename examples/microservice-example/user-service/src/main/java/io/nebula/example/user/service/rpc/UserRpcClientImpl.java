package io.nebula.example.user.service.rpc;

import io.nebula.example.user.api.dto.*;
import io.nebula.example.user.api.rpc.UserRpcClient;
import io.nebula.example.user.service.business.RpcDemoService;
import io.nebula.rpc.core.annotation.RpcService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
    
/**
 * UserRpcClient RPC 服务端实现
 * 作为 RPC Server 端的接口实现,负责接收并处理 RPC 调用
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@RpcService
@RequiredArgsConstructor
public class UserRpcClientImpl implements UserRpcClient {
    
    private final RpcDemoService rpcDemoService;
    
    @Override
    public CreateUserDto.Response createUser(CreateUserDto.Request request) {
        log.info("RPC服务端: createUser, username={}", request.getUsername());
        return rpcDemoService.createUser(request);
    }
    
    @Override
    public GetUserDto.Response getUserById(Long id) {
        log.info("RPC服务端: getUserById, id={}", id);
        GetUserDto.Request request = new GetUserDto.Request();
        request.setId(id);
        return rpcDemoService.getUserById(request);
    }
    
    @Override
    public GetUsersDto.Response getUsers(String username, String name, String status, Integer page, Integer size) {
        log.info("RPC服务端: getUsers, page={}, size={}", page, size);
        GetUsersDto.Request request = new GetUsersDto.Request();
        request.setUsername(username);
        request.setName(name);
        request.setStatus(status);
        request.setPage(page);
        request.setSize(size);
        return rpcDemoService.getUsers(request);
    }
    
    @Override
    public UpdateUserDto.Response updateUser(Long id, UpdateUserDto.Request request) {
        log.info("RPC服务端: updateUser, id={}", id);
        request.setId(id);
        return rpcDemoService.updateUser(request);
    }
    
    @Override
    public DeleteUserDto.Response deleteUser(Long id) {
        log.info("RPC服务端: deleteUser, id={}", id);
        DeleteUserDto.Request request = new DeleteUserDto.Request();
        request.setId(id);
        return rpcDemoService.deleteUser(request);
    }
}


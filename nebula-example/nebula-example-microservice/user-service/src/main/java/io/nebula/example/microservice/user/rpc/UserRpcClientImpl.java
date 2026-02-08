package io.nebula.example.microservice.user.rpc;

import io.nebula.example.microservice.api.user.CreateUserRequest;
import io.nebula.example.microservice.api.user.UserDto;
import io.nebula.example.microservice.api.user.UserRpcClient;
import io.nebula.example.microservice.user.service.UserService;
import io.nebula.rpc.core.annotation.RpcService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 用户 RPC 服务实现
 * 
 * @author Nebula Framework Team
 * @since 1.0.0
 */
@Slf4j
@RpcService
@RequiredArgsConstructor
public class UserRpcClientImpl implements UserRpcClient {

    private final UserService userService;

    @Override
    public UserDto createUser(CreateUserRequest request) {
        log.info("RPC: 创建用户 username={}", request.getUsername());
        return userService.createUser(request);
    }

    @Override
    public UserDto getUserById(Long userId) {
        log.info("RPC: 获取用户 userId={}", userId);
        return userService.getUserById(userId);
    }

    @Override
    public List<UserDto> listUsers() {
        log.info("RPC: 获取用户列表");
        return userService.listUsers();
    }
}

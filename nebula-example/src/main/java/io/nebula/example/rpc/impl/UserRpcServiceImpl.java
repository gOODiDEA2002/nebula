package io.nebula.example.rpc.impl;

import io.nebula.example.model.User;
import io.nebula.example.rpc.UserRpcService;
import io.nebula.example.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户RPC服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRpcServiceImpl implements UserRpcService {
    
    private final UserService userService;
    
    @Override
    public User getUserById(Long id) {
        log.info("RPC调用: 根据ID获取用户, id={}", id);
        return userService.getUserById(id);
    }
    
    @Override
    public User getUserByUsername(String username) {
        log.info("RPC调用: 根据用户名获取用户, username={}", username);
        return userService.getUserByUsername(username);
    }
    
    @Override
    public List<User> getAllUsers() {
        log.info("RPC调用: 获取所有用户");
        return userService.getAllUsers();
    }
    
    @Override
    public User createUser(User user) {
        log.info("RPC调用: 创建用户, username={}", user.getUsername());
        return userService.createUser(user);
    }
    
    @Override
    public User updateUser(User user) {
        log.info("RPC调用: 更新用户, id={}", user.getId());
        return userService.updateUser(user);
    }
    
    @Override
    public boolean deleteUser(Long id) {
        log.info("RPC调用: 删除用户, id={}", id);
        return userService.deleteUser(id);
    }
    
    @Override
    public boolean validateCredentials(String username, String password) {
        log.info("RPC调用: 验证用户凭据, username={}", username);
        return userService.validateCredentials(username, password);
    }
}
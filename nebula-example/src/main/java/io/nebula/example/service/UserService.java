package io.nebula.example.service;

import io.nebula.example.model.User;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 创建用户
     */
    User createUser(User user);
    
    /**
     * 根据ID获取用户
     */
    User getUserById(Long id);
    
    /**
     * 根据用户名获取用户
     */
    User getUserByUsername(String username);
    
    /**
     * 更新用户
     */
    User updateUser(User user);
    
    /**
     * 删除用户
     */
    boolean deleteUser(Long id);
    
    /**
     * 获取所有用户
     */
    List<User> getAllUsers();
    
    /**
     * 分页查询用户
     */
    List<User> getUsersWithPagination(int page, int size);
    
    /**
     * 验证用户凭据
     */
    boolean validateCredentials(String username, String password);
}

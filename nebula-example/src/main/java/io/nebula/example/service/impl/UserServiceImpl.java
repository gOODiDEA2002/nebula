package io.nebula.example.service.impl;

import io.nebula.example.mapper.UserMapper;
import io.nebula.example.model.User;
import io.nebula.example.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    
    @Override
    public User createUser(User user) {
        log.info("创建用户: {}", user.getUsername());
        
        // 密码加密
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        // 设置默认值
        user.setStatus(user.getStatus() != null ? user.getStatus() : 1);
        user.setRole(user.getRole() != null ? user.getRole() : "USER");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setVersion(1);
        user.setDeleted(0);
        
        userMapper.insert(user);
        log.info("用户创建成功，ID: {}", user.getId());
        return user;
    }
    
    @Override
    public User getUserById(Long id) {
        log.debug("根据ID获取用户: {}", id);
        return userMapper.selectById(id);
    }
    
    @Override
    public User getUserByUsername(String username) {
        log.debug("根据用户名获取用户: {}", username);
        return userMapper.findByUsername(username);
    }
    
    @Override
    public User updateUser(User user) {
        log.info("更新用户: {}", user.getId());
        
        // 如果密码被修改，需要重新加密
        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("用户更新成功，ID: {}", user.getId());
        return user;
    }
    
    @Override
    public boolean deleteUser(Long id) {
        log.info("删除用户: {}", id);
        int result = userMapper.deleteById(id);
        boolean success = result > 0;
        log.info("用户删除{}: {}", success ? "成功" : "失败", id);
        return success;
    }
    
    @Override
    public List<User> getAllUsers() {
        log.debug("获取所有用户");
        return userMapper.selectList(null);
    }
    
    @Override
    public List<User> getUsersWithPagination(int page, int size) {
        log.debug("分页查询用户，页码: {}, 大小: {}", page, size);
        int offset = (page - 1) * size;
        return userMapper.findUsersWithPagination(offset, size);
    }
    
    @Override
    public boolean validateCredentials(String username, String password) {
        log.debug("验证用户凭据: {}", username);
        User user = getUserByUsername(username);
        if (user == null || user.getStatus() == 0) {
            log.warn("用户不存在或已禁用: {}", username);
            return false;
        }
        
        boolean valid = passwordEncoder.matches(password, user.getPassword());
        log.debug("用户凭据验证{}: {}", valid ? "成功" : "失败", username);
        
        if (valid) {
            // 更新最后登录时间
            user.setLastLoginTime(LocalDateTime.now());
            // 这里可以添加IP记录逻辑
            userMapper.updateById(user);
        }
        
        return valid;
    }
}

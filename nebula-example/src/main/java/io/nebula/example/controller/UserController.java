package io.nebula.example.controller;

import io.nebula.core.common.result.Result;
import io.nebula.example.model.User;
import io.nebula.example.service.UserService;
import io.nebula.web.controller.BaseController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController extends BaseController {
    
    private final UserService userService;
    
    /**
     * 获取所有用户
     */
    @GetMapping
    public Result<List<User>> getAllUsers() {
        log.info("获取所有用户列表");
        List<User> users = userService.getAllUsers();
        return success(users);
    }
    
    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        log.info("获取用户详情，ID: {}", id);
        User user = userService.getUserById(id);
        if (user == null) {
            return error("用户不存在");
        }
        return success(user);
    }
    
    /**
     * 根据用户名获取用户
     */
    @GetMapping("/username/{username}")
    public Result<User> getUserByUsername(@PathVariable String username) {
        log.info("根据用户名获取用户: {}", username);
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return error("用户不存在");
        }
        return success(user);
    }
    
    /**
     * 创建用户
     */
    @PostMapping
    public Result<User> createUser(@RequestBody User user) {
        log.info("创建用户: {}", user.getUsername());
        User createdUser = userService.createUser(user);
        return success(createdUser);
    }
    
    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public Result<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        log.info("更新用户，ID: {}", id);
        user.setId(id);
        User updatedUser = userService.updateUser(user);
        return success(updatedUser);
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        log.info("删除用户，ID: {}", id);
        boolean deleted = userService.deleteUser(id);
        if (deleted) {
            return success();
        } else {
            return error("删除失败");
        }
    }
    
    /**
     * 分页查询用户
     */
    @GetMapping("/page")
    public Result<List<User>> getUsersWithPagination(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("分页查询用户，页码: {}, 大小: {}", page, size);
        List<User> users = userService.getUsersWithPagination(page, size);
        return success(users);
    }
    
    /**
     * 验证用户凭据
     */
    @PostMapping("/validate")
    public Result<Boolean> validateCredentials(@RequestBody LoginRequest request) {
        log.info("验证用户凭据: {}", request.getUsername());
        boolean valid = userService.validateCredentials(request.getUsername(), request.getPassword());
        return success(valid);
    }
    
    @Override
    protected Long getCurrentUserId() {
        // TODO: 从SecurityContext或JWT中获取当前用户ID
        return 1L; // 临时返回固定值
    }
    
    @Override
    protected String getCurrentUsername() {
        // TODO: 从SecurityContext或JWT中获取当前用户名
        return "admin"; // 临时返回固定值
    }
    
    /**
     * 登录请求DTO
     */
    public static class LoginRequest {
        private String username;
        private String password;
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
}

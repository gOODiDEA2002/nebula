package io.nebula.example.controller;

import io.nebula.example.model.User;
import io.nebula.example.rpc.UserRpcService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户RPC控制器 - 暴露RPC服务端点
 */
@Slf4j
@RestController
@RequestMapping("/rpc/user")
@RequiredArgsConstructor
public class UserRpcController {
    
    private final UserRpcService userRpcService;
    
    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        log.info("RPC端点: 根据ID获取用户, id={}", id);
        return userRpcService.getUserById(id);
    }
    
    /**
     * 根据用户名获取用户
     */
    @GetMapping("/username/{username}")
    public User getUserByUsername(@PathVariable String username) {
        log.info("RPC端点: 根据用户名获取用户, username={}", username);
        return userRpcService.getUserByUsername(username);
    }
    
    /**
     * 获取所有用户
     */
    @GetMapping("/all")
    public List<User> getAllUsers() {
        log.info("RPC端点: 获取所有用户");
        return userRpcService.getAllUsers();
    }
    
    /**
     * 创建用户
     */
    @PostMapping
    public User createUser(@RequestBody User user) {
        log.info("RPC端点: 创建用户, username={}", user.getUsername());
        return userRpcService.createUser(user);
    }
    
    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        log.info("RPC端点: 更新用户, id={}", id);
        user.setId(id);
        return userRpcService.updateUser(user);
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteUser(@PathVariable Long id) {
        log.info("RPC端点: 删除用户, id={}", id);
        boolean result = userRpcService.deleteUser(id);
        return Map.of(
            "success", result,
            "message", result ? "用户删除成功" : "用户删除失败",
            "userId", id
        );
    }
    
    /**
     * 验证用户凭据
     */
    @PostMapping("/validate")
    public Map<String, Object> validateCredentials(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        log.info("RPC端点: 验证用户凭据, username={}", username);
        
        boolean valid = userRpcService.validateCredentials(username, password);
        return Map.of(
            "valid", valid,
            "message", valid ? "凭据验证成功" : "凭据验证失败",
            "username", username
        );
    }
    
    /**
     * RPC服务健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "service", "UserRpcService",
            "message", "用户RPC服务运行正常"
        );
    }
}

package io.nebula.example.modules.web.controller;

import io.nebula.core.common.result.Result;
import io.nebula.example.modules.web.entity.dto.TokenDto;
import io.nebula.example.modules.web.entity.dto.LoginDto;
import io.nebula.example.modules.web.entity.dos.UserInfo;
import io.nebula.example.modules.web.entity.dos.UserProfile;
import io.nebula.web.auth.AuthContext;
import io.nebula.web.auth.AuthService;
import io.nebula.web.auth.AuthUser;
import io.nebula.web.controller.BaseController;
import io.nebula.web.mask.MaskType;
import io.nebula.web.mask.SensitiveData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 认证控制器
 * 演示 Nebula Web 认证、数据脱敏等功能
 */
@RestController
@RequestMapping("/auth")
public class AuthController extends BaseController {

    @Autowired(required = false)
    private AuthService authService;

    /**
     * 简单的认证检查接口
     * 注意：此接口需要认证，会被 AuthInterceptor 拦截
     */
    @GetMapping
    public Result<String> checkAuth() {
        return success("认证通过！您已成功访问受保护的资源。");
    }

    /**
     * 登录接口
     */
    @PostMapping("/login")
    public Result<LoginDto.Response> login(@RequestBody LoginDto.Request request) {
        if ("admin".equals(request.getUsername()) && "password".equals(request.getPassword())) {
            // 创建用户信息
            AuthUser user = new AuthUser("1001", "admin");
            user.setRoles(Set.of("ADMIN", "USER"));
            user.setPermissions(Set.of("READ", "WRITE", "DELETE"));
            
            // 生成 JWT Token
            String token = null;
            if (authService != null) {
                token = authService.generateToken(user);
            } else {
                token = "mock-jwt-token-" + System.currentTimeMillis();
            }
            
            LoginDto.Response response = new LoginDto.Response();
            response.setToken(token);
            response.setUser(new UserInfo("1001", "admin", "admin@example.com", "13888888888"));
            response.setLoginTime(LocalDateTime.now());
            
            return success(response);
        } else {
            return error("INVALID_CREDENTIALS", "用户名或密码错误");
        }
    }

    /**
     * 获取当前用户信息
     * 演示认证上下文的使用
     */
    @GetMapping("/profile")
    public Result<UserProfile> getProfile() {
        // 从认证上下文获取当前用户信息
        String userId = AuthContext.getCurrentUserId();
        String username = AuthContext.getCurrentUsername();
        
        if (userId == null) {
            return error("NOT_AUTHENTICATED", "用户未认证");
        }
        
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setUsername(username);
        profile.setEmail("user@example.com");
        profile.setMobile("13888888888");
        profile.setIdCard("110101199001011234");
        profile.setLastLoginTime(LocalDateTime.now());
        
        return success(profile);
    }

    /**
     * 权限检查演示
     */
    @GetMapping("/admin/users")
    public Result<List<UserInfo>> getUsers() {
        // 检查是否有管理员权限
        if (!AuthContext.hasRole("ADMIN")) {
            return error("FORBIDDEN", "需要管理员权限");
        }
        
        List<UserInfo> result = new ArrayList<>();
        result.add(new UserInfo("1001", "admin", "admin@example.com", "13888888888"));
        result.add(new UserInfo("1002", "user1", "user1@example.com", "13888888889"));
        result.add(new UserInfo("1003", "user2", "user2@example.com", "13888888880"));
        
        return success(result);
    }

    /**
     * Token 刷新接口
     */
    @PostMapping("/refresh")
    public Result<TokenDto.Response> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            
            if (authService != null) {
                String newToken = authService.refreshToken(token);
                
                TokenDto.Response response = new TokenDto.Response();
                response.setToken(newToken);
                response.setRefreshTime(LocalDateTime.now());
                
                return success(response);
            } else {
                return error("SERVICE_UNAVAILABLE", "认证服务不可用");
            }
        } catch (Exception e) {
            return error("REFRESH_FAILED", "Token 刷新失败：" + e.getMessage());
        }
    }

    /**
     * 登出接口
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            
            if (authService != null) {
                authService.logout(token);
            }
            
            return success();
        } catch (Exception e) {
            return error("LOGOUT_FAILED", "登出失败：" + e.getMessage());
        }
    }
}

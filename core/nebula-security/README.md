# Nebula Security 模块

## 模块简介

`nebula-security` 是 Nebula 框架的安全模块，提供JWT认证和RBAC授权功能。

## 功能特性

核心功能
- JWT认证：基于Token的无状态认证
- RBAC授权：基于角色的权限控制
- 安全注解：@RequiresAuthentication、@RequiresPermission、@RequiresRole
- 安全上下文：ThreadLocal存储当前用户信息

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-security</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置

```yaml
nebula:
  security:
    enabled: true
    
    # JWT配置
    jwt:
      enabled: true
      secret: your-secret-key
      expiration: 24h
      refresh-expiration: 7d
      header-name: Authorization
      token-prefix: "Bearer "
    
    # RBAC配置
    rbac:
      enabled: true
      enable-cache: true
      cache-expiration: 30m
      super-admin-role: SUPER_ADMIN
    
    # 匿名访问URL
    anonymous-urls:
      - /api/auth/login
      - /api/auth/register
      - /api/public/**
```

## 使用示例

### 1. 认证检查

```java
@RestController
@RequestMapping("/api/user")
public class UserController {
    
    /**
     * 需要登录才能访问
     */
    @RequiresAuthentication
    @GetMapping("/profile")
    public User getProfile() {
        Long userId = SecurityContext.getCurrentUserId();
        return userService.getById(userId);
    }
}
```

### 2. 权限检查

```java
@RestController
@RequestMapping("/api/order")
public class OrderController {
    
    /**
     * 需要order:delete权限
     */
    @RequiresPermission("order:delete")
    @DeleteMapping("/{id}")
    public void deleteOrder(@PathVariable Long id) {
        orderService.delete(id);
    }
    
    /**
     * 需要order:create或order:update权限(任意一个)
     */
    @RequiresPermission(value = {"order:create", "order:update"}, logical = RequiresPermission.Logical.OR)
    @PostMapping
    public Order saveOrder(@RequestBody Order order) {
        return orderService.save(order);
    }
    
    /**
     * 需要order:view和order:export权限(同时拥有)
     */
    @RequiresPermission(value = {"order:view", "order:export"}, logical = RequiresPermission.Logical.AND)
    @GetMapping("/export")
    public void exportOrders() {
        orderService.export();
    }
}
```

### 3. 角色检查

```java
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    /**
     * 需要ADMIN角色
     */
    @RequiresRole("ADMIN")
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userService.findAll();
    }
    
    /**
     * 需要ADMIN或SUPER_ADMIN角色(任意一个)
     */
    @RequiresRole(value = {"ADMIN", "SUPER_ADMIN"}, logical = RequiresPermission.Logical.OR)
    @DeleteMapping("/user/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.delete(id);
    }
}
```

### 4. 编程式权限检查

```java
@Service
public class OrderService {
    
    public void processOrder(Long orderId) {
        // 获取当前用户
        Authentication auth = SecurityContext.getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }
        
        // 获取用户权限
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        boolean hasPermission = authorities.stream()
            .anyMatch(a -> a.getAuthority().equals("order:process"));
        
        if (!hasPermission) {
            throw new SecurityException("Permission denied");
        }
        
        // 业务逻辑...
    }
}
```

### 5. 获取当前用户信息

```java
@Service
public class UserService {
    
    public void updateProfile(UserDto userDto) {
        // 获取当前用户ID
        Long currentUserId = SecurityContext.getCurrentUserId();
        
        // 获取当前用户名
        String currentUsername = SecurityContext.getCurrentUsername();
        
        // 获取完整的认证信息
        Authentication auth = SecurityContext.getAuthentication();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        
        // 更新用户信息...
    }
}
```

## JWT认证实现

基于nebula-foundation的JwtUtils工具类实现JWT认证。

```java
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final JwtUtils jwtUtils;
    
    /**
     * 登录并生成Token
     */
    public String login(LoginRequest request) {
        // 验证用户名密码
        User user = userService.authenticate(request.getUsername(), request.getPassword());
        
        // 生成JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("roles", user.getRoles());
        
        return jwtUtils.generate(claims);
    }
    
    /**
     * 验证Token并设置安全上下文
     */
    public void authenticateToken(String token) {
        // 解析Token
        Map<String, Object> claims = jwtUtils.parse(token);
        
        // 创建用户主体
        Long userId = Long.valueOf(claims.get("userId").toString());
        String username = claims.get("username").toString();
        
        // 加载用户权限
        Collection<GrantedAuthority> authorities = loadUserAuthorities(userId);
        
        // 创建认证Token
        JwtAuthenticationToken authToken = new JwtAuthenticationToken(
            token,
            new UserPrincipal(userId, username, authorities),
            authorities
        );
        
        // 设置到安全上下文
        SecurityContext.setAuthentication(authToken);
    }
}
```

## 许可证

本项目基于 Apache 2.0 许可证开源


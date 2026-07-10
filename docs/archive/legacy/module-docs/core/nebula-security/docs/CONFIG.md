# Nebula Security 配置指南

> Nebula框架安全模块配置说明

## 目录

- [概述](#概述)
- [基本配置](#基本配置)
- [JWT配置](#jwt配置)
- [RBAC配置](#rbac配置)
- [安全过滤器配置](#安全过滤器配置)
- [票务系统场景配置](#票务系统场景配置)
- [环境区分配置](#环境区分配置)
- [安全最佳实践](#安全最佳实践)

## 概述

`nebula-security` 模块提供JWT认证和RBAC授权功能。主要通过 Spring Boot 配置文件和Java配置类进行配置。

### 配置层次

```
1. application-{profile}.yml  (环境特定配置)
2. application.yml            (通用配置)
3. Java @Configuration       (代码配置)
4. 默认值                     (模块内置)
```

## 基本配置

### Maven 依赖

```xml
<dependency>
    <groupId>com.andy.nebula</groupId>
    <artifactId>nebula-security</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>

<!-- 依赖 nebula-foundation -->
<dependency>
    <groupId>com.andy.nebula</groupId>
    <artifactId>nebula-foundation</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### 基本启用

```yaml
nebula:
  security:
    # 是否启用安全模块
    enabled: true
```

## JWT配置

### 完整配置项

```yaml
nebula:
  security:
    jwt:
      # 是否启用JWT认证
      enabled: true
      
      # JWT密钥 (Base64编码, 至少256位)
      # ⚠️ 生产环境必须从环境变量或密钥管理服务读取
      secret: ${JWT_SECRET:}
      
      # Access Token 过期时间
      expiration: 86400              # 24小时 (秒)
      
      # Refresh Token 过期时间
      refresh-expiration: 604800     # 7天 (秒)
      
      # Token header名称
      header-name: Authorization
      
      # Token前缀
      token-prefix: "Bearer "
      
      # Token签发者
      issuer: ticket-system
      
      # 是否验证签发者
      validate-issuer: true
```

### 票务系统JWT配置

**不同角色的Token有效期**:

```yaml
nebula:
  security:
    jwt:
      # 普通用户: 24小时
      expiration: 86400
      # 管理员: 8小时 (更严格)
      admin-expiration: 28800
      # 系统服务: 30天 (服务间调用)
      service-expiration: 2592000
      
      refresh-expiration: 604800
```

**Java配置**:

```java
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class JwtSecurityConfig {
    
    @Autowired
    private SecurityProperties securityProperties;
    
    /**
     * JWT密钥
     */
    @Bean
    public SecretKey jwtSecretKey() {
        String secret = securityProperties.getJwt().getSecret();
        
        if (Strings.isBlank(secret)) {
            // 开发环境自动生成密钥
            if (isDevEnvironment()) {
                log.warn("未配置JWT密钥,使用自动生成的密钥");
                return JwtUtils.generateKey();
            }
            
            throw new IllegalStateException(
                "JWT密钥未配置,请设置环境变量 JWT_SECRET"
            );
        }
        
        return JwtUtils.keyFromBase64(secret);
    }
    
    /**
     * Token过期时间 (根据角色动态配置)
     */
    @Bean
    public Function<String, Duration> jwtExpirationProvider() {
        return role -> {
            if ("ADMIN".equals(role)) {
                return Duration.ofSeconds(securityProperties.getJwt().getAdminExpiration());
            } else if ("SERVICE".equals(role)) {
                return Duration.ofSeconds(securityProperties.getJwt().getServiceExpiration());
            } else {
                return Duration.ofSeconds(securityProperties.getJwt().getExpiration());
            }
        };
    }
}
```

### 生成JWT密钥

**开发环境**:

```bash
# 生成密钥并保存到环境变量
export JWT_SECRET=$(java -cp nebula-foundation.jar io.nebula.core.common.security.JwtUtils generateKey)
```

**生产环境** (使用密钥管理服务):

```bash
# AWS Secrets Manager
export JWT_SECRET=$(aws secretsmanager get-secret-value --secret-id ticket/jwt-secret --query SecretString --output text)

# 阿里云KMS
export JWT_SECRET=$(aliyun kms GetSecretValue --SecretName ticket-jwt-secret --query SecretData --output text)
```

## RBAC配置

### 完整配置项

```yaml
nebula:
  security:
    rbac:
      # 是否启用RBAC
      enabled: true
      
      # 是否启用权限缓存
      enable-cache: true
      
      # 权限缓存过期时间 (秒)
      cache-expiration: 1800         # 30分钟
      
      # 超级管理员角色
      super-admin-role: SUPER_ADMIN
      
      # 是否严格模式 (权限不存在时拒绝访问)
      strict-mode: true
```

### 票务系统角色权限设计

**角色定义**:

```yaml
ticket:
  rbac:
    roles:
      # 超级管理员
      - name: SUPER_ADMIN
        permissions: ["*:*"]
      
      # 影院管理员
      - name: CINEMA_ADMIN
        permissions:
          - "cinema:*"
          - "showtime:*"
          - "hall:*"
          - "order:view"
      
      # 财务人员
      - name: FINANCE
        permissions:
          - "order:view"
          - "payment:view"
          - "refund:*"
          - "report:view"
      
      # 运营人员
      - name: OPERATOR
        permissions:
          - "movie:*"
          - "showtime:view"
          - "order:view"
          - "marketing:*"
      
      # 客服人员
      - name: CUSTOMER_SERVICE
        permissions:
          - "order:view"
          - "ticket:view"
          - "ticket:verify"
          - "user:view"
      
      # 普通用户
      - name: USER
        permissions:
          - "movie:view"
          - "showtime:view"
          - "order:create"
          - "order:view:own"
          - "ticket:view:own"
```

**Java配置**:

```java
@Configuration
public class RbacConfig {
    
    /**
     * 权限数据加载器
     */
    @Bean
    public PermissionLoader permissionLoader() {
        return userId -> {
            // 从数据库加载用户权限
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return Collections.emptyList();
            }
            
            // 加载用户的所有角色
            List<Role> roles = roleRepository.findByUserId(userId);
            
            // 加载每个角色的权限
            Set<String> permissions = new HashSet<>();
            for (Role role : roles) {
                List<Permission> rolePermissions = permissionRepository.findByRoleId(role.getId());
                rolePermissions.forEach(p -> permissions.add(p.getCode()));
            }
            
            return new ArrayList<>(permissions);
        };
    }
    
    /**
     * 权限缓存管理器
     */
    @Bean
    @ConditionalOnProperty(name = "nebula.security.rbac.enable-cache", havingValue = "true")
    public PermissionCacheManager permissionCacheManager() {
        return new RedisCacheManager(
            redisTemplate,
            Duration.ofSeconds(securityProperties.getRbac().getCacheExpiration())
        );
    }
}
```

### 权限表达式

Nebula Security 支持灵活的权限表达式:

```java
// 精确匹配
@RequiresPermission("order:delete")

// 通配符
@RequiresPermission("order:*")          // order模块的所有权限
@RequiresPermission("*:view")           // 所有模块的view权限
@RequiresPermission("*:*")              // 所有权限 (超级管理员)

// 多个权限 (AND)
@RequiresPermission(value = {"order:view", "order:export"}, logical = Logical.AND)

// 多个权限 (OR)
@RequiresPermission(value = {"order:create", "order:update"}, logical = Logical.OR)

// 数据权限 (自定义)
@RequiresPermission("order:view:own")   // 只能查看自己的订单
@RequiresPermission("order:view:dept")  // 可以查看部门的订单
@RequiresPermission("order:view:all")   // 可以查看所有订单
```

## 安全过滤器配置

### 配置匿名访问URL

```yaml
nebula:
  security:
    # 不需要认证的URL列表
    anonymous-urls:
      # 认证接口
      - /api/auth/login
      - /api/auth/register
      - /api/auth/refresh
      
      # 公开接口
      - /api/public/**
      
      # 健康检查
      - /actuator/health
      - /actuator/info
      
      # 静态资源
      - /static/**
      - /favicon.ico
      
      # Swagger文档
      - /swagger-ui/**
      - /v3/api-docs/**
```

### Java配置

```java
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    
    @Autowired
    private SecurityProperties securityProperties;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 1. 禁用CSRF (使用JWT不需要CSRF保护)
        http.csrf().disable();
        
        // 2. 禁用Session (使用JWT无状态认证)
        http.sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        
        // 3. 配置URL访问权限
        http.authorizeHttpRequests(auth -> {
            // 匿名访问URL
            String[] anonymousUrls = securityProperties.getAnonymousUrls()
                .toArray(new String[0]);
            auth.requestMatchers(anonymousUrls).permitAll();
            
            // 其他URL需要认证
            auth.anyRequest().authenticated();
        });
        
        // 4. 添加JWT过滤器
        http.addFilterBefore(
            jwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class
        );
        
        // 5. 异常处理
        http.exceptionHandling()
            .authenticationEntryPoint(authenticationEntryPoint())
            .accessDeniedHandler(accessDeniedHandler());
        
        return http.build();
    }
    
    /**
     * 认证失败处理器
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            
            Result<Void> result = Result.unauthorized("未登录或Token已过期");
            response.getWriter().write(JsonUtils.toJson(result));
        };
    }
    
    /**
     * 权限不足处理器
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            
            Result<Void> result = Result.forbidden("权限不足");
            response.getWriter().write(JsonUtils.toJson(result));
        };
    }
}
```

## 票务系统场景配置

### 多租户隔离配置

```yaml
ticket:
  security:
    multi-tenant:
      # 是否启用多租户
      enabled: true
      # 租户ID来源 (header, token, domain)
      source: header
      # 租户ID header名称
      header-name: X-Tenant-ID
```

```java
@Component
public class TenantFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        // 从请求头获取租户ID
        String tenantId = request.getHeader("X-Tenant-ID");
        
        if (tenantId != null) {
            // 设置到ThreadLocal
            TenantContext.setTenantId(tenantId);
        }
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
```

### API限流配置

```yaml
ticket:
  security:
    rate-limit:
      # 是否启用限流
      enabled: true
      # 默认限流规则 (每分钟请求数)
      default-limit: 60
      # 不同接口的限流规则
      rules:
        - path: /api/orders
          limit: 30
        - path: /api/auth/login
          limit: 10
```

### 防重放攻击配置

```yaml
ticket:
  security:
    anti-replay:
      # 是否启用防重放
      enabled: true
      # 时间窗口 (秒)
      window: 60
      # Nonce缓存时间 (秒)
      nonce-ttl: 120
```

## 环境区分配置

### 开发环境 (`application-dev.yml`)

```yaml
nebula:
  security:
    enabled: true
    
    jwt:
      enabled: true
      secret: dev-jwt-secret-for-testing-only-do-not-use-in-production
      expiration: 3600           # 1小时 (开发环境短一些方便测试)
      refresh-expiration: 86400  # 1天
      validate-issuer: false     # 开发环境不验证签发者
    
    rbac:
      enabled: true
      enable-cache: false        # 开发环境不缓存,便于测试
      strict-mode: false         # 宽松模式,便于调试
    
    # 开发环境允许更多匿名访问
    anonymous-urls:
      - /api/**
      - /actuator/**
      - /swagger-ui/**
      - /v3/api-docs/**

# 开发环境日志
logging:
  level:
    io.nebula.security: DEBUG
    org.springframework.security: DEBUG
```

### 测试环境 (`application-test.yml`)

```yaml
nebula:
  security:
    enabled: true
    
    jwt:
      enabled: true
      secret: ${JWT_SECRET}      # 从环境变量读取
      expiration: 7200           # 2小时
      refresh-expiration: 86400  # 1天
      validate-issuer: true
    
    rbac:
      enabled: true
      enable-cache: true
      cache-expiration: 1800
      strict-mode: true          # 测试环境使用严格模式
    
    anonymous-urls:
      - /api/auth/**
      - /api/public/**
      - /actuator/health
      - /swagger-ui/**

logging:
  level:
    io.nebula.security: INFO
```

### 生产环境 (`application-prod.yml`)

```yaml
nebula:
  security:
    enabled: true
    
    jwt:
      enabled: true
      # ⚠️ 生产环境必须从环境变量或密钥管理服务读取
      secret: ${JWT_SECRET}
      expiration: 86400          # 24小时
      refresh-expiration: 604800 # 7天
      admin-expiration: 28800    # 8小时 (管理员)
      header-name: Authorization
      token-prefix: "Bearer "
      issuer: ticket-production
      validate-issuer: true
    
    rbac:
      enabled: true
      enable-cache: true
      cache-expiration: 1800     # 30分钟
      super-admin-role: SUPER_ADMIN
      strict-mode: true          # 生产环境必须严格模式
    
    # 生产环境严格控制匿名访问
    anonymous-urls:
      - /api/auth/login
      - /api/auth/register
      - /api/public/health
      - /actuator/health

ticket:
  security:
    # 多租户隔离
    multi-tenant:
      enabled: true
      source: header
    
    # API限流
    rate-limit:
      enabled: true
      default-limit: 60
      rules:
        - path: /api/orders
          limit: 30
        - path: /api/auth/login
          limit: 10
    
    # 防重放攻击
    anti-replay:
      enabled: true
      window: 60
      nonce-ttl: 120

# 生产环境日志
logging:
  level:
    root: INFO
    io.nebula.security: INFO
    org.springframework.security: WARN
  file:
    name: logs/security.log
    max-size: 100MB
    max-history: 30
```

## 安全最佳实践

### 1. 密钥管理

❌ **不要**:

```yaml
# 不要在配置文件中硬编码密钥
nebula:
  security:
    jwt:
      secret: "my-secret-key"
```

✅ **应该**:

```yaml
# 从环境变量读取
nebula:
  security:
    jwt:
      secret: ${JWT_SECRET}
```

```bash
# 使用密钥管理服务
export JWT_SECRET=$(aws secretsmanager get-secret-value --secret-id jwt-secret --query SecretString --output text)
```

### 2. Token有效期

不同场景使用不同的有效期:

```yaml
nebula:
  security:
    jwt:
      expiration: 86400          # 普通用户: 24小时
      admin-expiration: 28800    # 管理员: 8小时
      service-expiration: 2592000  # 服务: 30天
```

### 3. HTTPS

生产环境必须使用HTTPS:

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

### 4. 安全Headers

```java
@Configuration
public class SecurityHeadersConfig {
    
    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SecurityHeadersFilter());
        registration.addUrlPatterns("/*");
        return registration;
    }
}

public class SecurityHeadersFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        // 防止XSS攻击
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // 防止点击劫持
        response.setHeader("X-Frame-Options", "DENY");
        
        // 强制HTTPS
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        
        // CSP
        response.setHeader("Content-Security-Policy", "default-src 'self'");
        
        filterChain.doFilter(request, response);
    }
}
```

### 5. 敏感操作二次验证

重要操作(如删除订单、退款)需要二次验证:

```java
@Service
public class OrderService {
    
    @RequiresPermission("order:delete")
    public void deleteOrder(Long orderId, String verifyCode) {
        // 验证验证码
        if (!verifyCodeService.validate(SecurityContext.getCurrentUserId(), verifyCode)) {
            throw BusinessException.of("验证码错误");
        }
        
        // 删除订单
        orderRepository.deleteById(orderId);
    }
}
```

## 相关文档

- [模块 README](README.md) - 模块功能介绍
- [示例文档](EXAMPLE.md) - 完整使用示例
- [测试文档](TESTING.md) - 测试指南
- [发展路线图](ROADMAP.md) - 未来规划

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0


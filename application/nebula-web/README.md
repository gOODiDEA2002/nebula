# Nebula Web 框架使用指南

## 概述

Nebula Web 是一个基于 Spring Boot 的现代化 Web 框架，提供了认证限流缓存性能监控健康检查等企业级功能

## 核心功能

###  认证系统 (Authentication)
- 基于 JWT 的认证机制
- 支持自定义认证服务
- 可配置忽略路径
- 自动设置认证上下文

###  限流 (Rate Limiting)
- 基于滑动窗口算法
- 支持多种限流策略（IP用户API）
- 内存或分布式限流器
- 可配置限流规则

###  响应缓存 (Response Caching)
- GET 请求响应缓存
- 支持多种缓存策略
- 自动缓存键生成
- TTL 过期管理

###  性能监控 (Performance Monitoring)
- 请求响应时间统计
- 慢请求检测
- 系统资源监控
- JVM 指标收集

###  健康检查 (Health Checks)
- 应用健康状态检查
- 内存磁盘空间检查
- 自定义健康检查器
- Kubernetes 探针支持

###  数据脱敏 (Data Masking)
- 敏感数据自动脱敏
- 多种脱敏策略
- 注解驱动
- JSON 序列化集成

###  请求日志 (Request Logging)
- 详细的请求响应日志
- 敏感信息脱敏
- 可配置日志级别
- 性能影响控制

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-web</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 基础配置

```yaml
nebula:
  web:
    # 全局异常处理
    exception-handler:
      enabled: true
      log-stack-trace: true
      include-exception-details: false
    
    # API 文档
    api-doc:
      enabled: true
      title: "My API"
      description: "My Application API"
      version: "1.0.0"
    
    # CORS 配置
    cors:
      enabled: true
      allowed-origins: ["*"]
      allowed-methods: ["GET", "POST", "PUT", "DELETE"]
      allow-credentials: true
```

## 详细配置指南

### 认证配置

```yaml
nebula:
  web:
    auth:
      enabled: true
      jwt-secret: "your-secret-key-at-least-256-bits"
      jwt-expiration: 86400  # 24小时
      auth-header: "Authorization"
      auth-header-prefix: "Bearer "
      ignore-paths:
        - "/public/**"
        - "/auth/login"
        - "/health/**"
```

#### 使用示例

```java
@RestController
@RequestMapping("/api")
public class UserController extends BaseController {
    
    @Autowired
    private AuthService authService;
    
    // 登录接口（在忽略路径中）
    @PostMapping("/auth/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        // 验证用户凭据
        AuthUser user = validateUser(request.getUsername(), request.getPassword());
        
        // 生成 JWT Token
        String token = authService.generateToken(user);
        
        return success(new LoginResponse(token, user));
    }
    
    // 需要认证的接口
    @GetMapping("/profile")
    public Result<UserProfile> getProfile() {
        // 从认证上下文获取当前用户
        String userId = AuthContext.getCurrentUserId();
        String username = AuthContext.getCurrentUsername();
        
        return success(new UserProfile(userId, username));
    }
    
    // 检查权限
    @GetMapping("/admin/users")
    public Result<List<User>> getUsers() {
        if (!AuthContext.hasRole("ADMIN")) {
            return error("FORBIDDEN", "需要管理员权限");
        }
        
        List<User> users = userService.getAllUsers();
        return success(users);
    }
}
```

### 限流配置

```yaml
nebula:
  web:
    rate-limit:
      enabled: true
      default-requests-per-second: 100
      time-window: 60  # 秒
      key-strategy: "IP"  # IP, USER, API, IP_API, USER_API
      limit-exceeded-message: "请求过于频繁，请稍后再试"
```

#### 限流策略说明

- **IP**: 基于客户端 IP 地址限流
- **USER**: 基于用户标识限流
- **API**: 基于 API 路径限流
- **IP_API**: 基于 IP + API 组合限流
- **USER_API**: 基于用户 + API 组合限流

#### 自定义限流

```java
@Component
public class CustomRateLimitKeyGenerator implements RateLimitKeyGenerator {
    
    @Override
    public String generateKey(HttpServletRequest request) {
        // 自定义限流键生成逻辑
        String userId = request.getHeader("X-User-ID");
        String api = request.getRequestURI();
        return "custom:" + userId + ":" + api;
    }
}
```

### 响应缓存配置

```yaml
nebula:
  web:
    cache:
      enabled: true
      default-ttl: 300  # 5分钟
      max-size: 1000
      key-prefix: "nebula:cache:"
```

#### 缓存使用示例

```java
@RestController
public class DataController extends BaseController {
    
    // 该接口的 GET 请求会被自动缓存
    @GetMapping("/api/data/{id}")
    public Result<Data> getData(@PathVariable Long id) {
        // 数据库查询等耗时操作
        Data data = dataService.findById(id);
        return success(data);
    }
    
    // POST/PUT/DELETE 请求不会被缓存
    @PostMapping("/api/data")
    public Result<Data> createData(@RequestBody CreateDataRequest request) {
        Data data = dataService.create(request);
        return success(data);
    }
}
```

### 性能监控配置

```yaml
nebula:
  web:
    performance:
      enabled: true
      slow-request-threshold: 1000  # 1秒
      enable-detailed-metrics: true
      metrics-interval: 60  # 秒
```

#### 性能监控接口

```bash
# 获取应用性能指标
GET /performance/metrics

# 获取系统指标
GET /performance/system

# 获取综合状态
GET /performance/status

# 重置性能指标
POST /performance/reset
```

#### 响应示例

```json
{
  "totalRequests": 1000,
  "successfulRequests": 950,
  "failedRequests": 50,
  "activeRequests": 5,
  "averageResponseTime": 250.5,
  "maxResponseTime": 2000,
  "slowRequestCount": 10,
  "successRate": 95.0,
  "failureRate": 5.0,
  "statusCounts": {
    "200": 900,
    "404": 30,
    "500": 20
  }
}
```

### 健康检查配置

```yaml
nebula:
  web:
    health:
      enabled: true
      endpoint: "/health"
      show-details: true
      check-interval: 30
```

#### 健康检查接口

```bash
# 整体健康状态
GET /health

# 简化状态
GET /health/status

# 指定组件状态
GET /health/component/memory

# 存活探针
GET /health/liveness

# 就绪探针
GET /health/readiness
```

#### 自定义健康检查器

```java
@Component
public class DatabaseHealthChecker implements HealthChecker {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public String getName() {
        return "database";
    }
    
    @Override
    public HealthCheckResult check() {
        try {
            // 执行数据库连接测试
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(5)) {
                    return HealthCheckResult.up()
                        .withDetail("database", "MySQL")
                        .withDetail("driver", connection.getMetaData().getDriverName());
                }
            }
            return HealthCheckResult.down("数据库连接测试失败");
        } catch (Exception e) {
            return HealthCheckResult.down("数据库连接异常: " + e.getMessage());
        }
    }
    
    @Override
    public int getOrder() {
        return 20; // 优先级
    }
}
```

### 数据脱敏配置

```yaml
nebula:
  web:
    data-masking:
      enabled: true
      sensitive-fields: ["password", "mobile", "email", "idCard"]
      strategy: "MASK"
      mask-char: "*"
```

#### 数据脱敏使用

```java
public class UserInfo {
    
    private String username;
    
    @SensitiveData(type = MaskType.PHONE)
    private String mobile;
    
    @SensitiveData(type = MaskType.EMAIL)
    private String email;
    
    @SensitiveData(type = MaskType.ID_CARD)
    private String idCard;
    
    @SensitiveData(type = MaskType.PASSWORD)
    private String password;
    
    // getters and setters
}
```

#### 脱敏效果

```json
{
  "username": "张三",
  "mobile": "138****8888",
  "email": "test***@example.com",
  "idCard": "110***********1234",
  "password": "******"
}
```

### 请求日志配置

```yaml
nebula:
  web:
    request-logging:
      enabled: true
      include-headers: true
      include-request-body: false
      include-response-body: false
      ignore-paths: ["/actuator/**", "/health/**"]
      max-request-body-length: 1024
      max-response-body-length: 1024
```

## 最佳实践

### 1. 安全配置

```yaml
# 生产环境配置
nebula:
  web:
    auth:
      jwt-secret: "${JWT_SECRET}"  # 使用环境变量
      jwt-expiration: 3600  # 1小时
    
    cors:
      enabled: true
      allowed-origins: ["https://yourdomain.com"]
      allow-credentials: true
    
    request-logging:
      include-request-body: false  # 避免记录敏感信息
      include-response-body: false
```

### 2. 性能优化

```yaml
nebula:
  web:
    cache:
      enabled: true
      max-size: 10000  # 根据内存调整
    
    rate-limit:
      enabled: true
      default-requests-per-second: 1000  # 根据服务器能力调整
    
    performance:
      slow-request-threshold: 500  # 500ms
```

### 3. 监控配置

```yaml
nebula:
  web:
    health:
      enabled: true
      show-details: false  # 生产环境建议关闭详细信息
    
    performance:
      enabled: true
      enable-detailed-metrics: true
```

## 故障排查

### 常见问题

1. **认证不生效**
   - 检查 `auth.enabled` 是否为 true
   - 确认路径不在 `ignore-paths` 中
   - 验证 JWT secret 配置

2. **限流不工作**
   - 确认 `rate-limit.enabled` 为 true
   - 检查限流策略配置
   - 查看日志中的限流信息

3. **缓存不生效**
   - 只有 GET 请求会被缓存
   - 检查缓存配置
   - 确认响应状态码为 2xx

4. **健康检查失败**
   - 检查各个 HealthChecker 的实现
   - 查看详细错误信息
   - 验证依赖服务状态

### 调试技巧

1. **启用调试日志**
```yaml
logging:
  level:
    io.nebula.web: DEBUG
```

2. **查看性能指标**
```bash
curl http://localhost:8080/performance/metrics
```

3. **检查健康状态**
```bash
curl http://localhost:8080/health
```

## 扩展开发

### 自定义认证服务

```java
@Component
public class CustomAuthService implements AuthService {
    
    @Override
    public AuthUser getUser(String token) {
        // 自定义用户获取逻辑
        return parseCustomToken(token);
    }
    
    @Override
    public String generateToken(AuthUser user) {
        // 自定义 Token 生成逻辑
        return createCustomToken(user);
    }
    
    // 实现其他方法...
}
```

### 自定义缓存实现

```java
@Component
public class RedisResponseCache implements ResponseCache {
    
    @Autowired
    private RedisTemplate<String, CachedResponse> redisTemplate;
    
    @Override
    public CachedResponse get(String key) {
        return redisTemplate.opsForValue().get(key);
    }
    
    @Override
    public void put(String key, CachedResponse response, int ttlSeconds) {
        redisTemplate.opsForValue().set(key, response, ttlSeconds, TimeUnit.SECONDS);
    }
    
    // 实现其他方法...
}
```

## 版本兼容性

- Spring Boot 3.x
- Java 17+
- Jakarta EE 9+

## 更新日志

### 2.0.0-SNAPSHOT
- 全面重构，支持 Spring Boot 3
- 新增数据脱敏功能
- 优化性能监控
- 增强健康检查

---

更多详细信息，请参考源码和测试用例

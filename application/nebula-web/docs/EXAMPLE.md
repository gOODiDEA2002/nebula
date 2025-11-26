# Nebula Web - 使用示例

> Web框架完整使用指南，以票务系统Web应用为例

## 目录

- [快速开始](#快速开始)
- [控制器开发](#控制器开发)
- [异常处理](#异常处理)
- [认证授权](#认证授权)
- [限流控制](#限流控制)
- [响应缓存](#响应缓存)
- [数据脱敏](#数据脱敏)
- [性能监控](#性能监控)
- [健康检查](#健康检查)
- [票务系统完整示例](#票务系统完整示例)
- [最佳实践](#最佳实践)

---

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-web</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 基础配置

```yaml
nebula:
  web:
    enabled: true
    
    # 认证配置
    auth:
      enabled: true
      jwt-secret: ${JWT_SECRET}
      token-expiration: 7200  # 2小时
    
    # 限流配置
    rate-limit:
      enabled: true
      default-limit: 100
      default-duration: 60
    
    # 响应缓存配置
    response-cache:
      enabled: true
      default-ttl: 300  # 5分钟
```

---

## 控制器开发

### 1. 基础控制器

```java
/**
 * 演出控制器
 */
@RestController
@RequestMapping("/api/showtimes")
@RequiredArgsConstructor
@Slf4j
public class ShowtimeController extends BaseController {
    
    private final ShowtimeService showtimeService;
    
    /**
     * 获取演出列表
     */
    @GetMapping
    public Result<PageResult<ShowtimeVO>> list(ShowtimeQueryRequest request) {
        log.info("查询演出列表：{}", request);
        
        PageResult<ShowtimeVO> result = showtimeService.queryShowtimes(request);
        
        return success(result);
    }
    
    /**
     * 获取演出详情
     */
    @GetMapping("/{id}")
    public Result<ShowtimeDetailVO> detail(@PathVariable Long id) {
        log.info("查询演出详情：id={}", id);
        
        ShowtimeDetailVO detail = showtimeService.getShowtimeDetail(id);
        
        if (detail == null) {
            return error("SHOWTIME_NOT_FOUND", "演出不存在");
        }
        
        return success(detail);
    }
    
    /**
     * 创建演出
     */
    @PostMapping
    public Result<Long> create(@Validated @RequestBody CreateShowtimeRequest request) {
        log.info("创建演出：{}", request);
        
        Long showtimeId = showtimeService.createShowtime(request);
        
        return success(showtimeId);
    }
    
    /**
     * 更新演出
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, 
                               @Validated @RequestBody UpdateShowtimeRequest request) {
        log.info("更新演出：id={}, request={}", id, request);
        
        showtimeService.updateShowtime(id, request);
        
        return success();
    }
    
    /**
     * 删除演出
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除演出：id={}", id);
        
        showtimeService.deleteShowtime(id);
        
        return success();
    }
}
```

### 2. 文件上传

```java
/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController extends BaseController {
    
    private final FileStorageService fileStorageService;
    
    /**
     * 上传演出海报
     */
    @PostMapping("/posters")
    public Result<FileUploadResponse> uploadPoster(
            @RequestParam("file") MultipartFile file,
            @RequestParam("showtimeId") Long showtimeId) {
        
        log.info("上传演出海报：showtimeId={}, fileName={}", 
                showtimeId, file.getOriginalFilename());
        
        // 验证文件
        validateImageFile(file);
        
        // 上传文件
        String fileUrl = fileStorageService.uploadShowtimePoster(showtimeId, file);
        
        FileUploadResponse response = new FileUploadResponse();
        response.setFileUrl(fileUrl);
        response.setFileName(file.getOriginalFilename());
        response.setFileSize(file.getSize());
        
        return success(response);
    }
    
    /**
     * 批量上传
     */
    @PostMapping("/batch")
    public Result<List<FileUploadResponse>> batchUpload(
            @RequestParam("files") MultipartFile[] files) {
        
        log.info("批量上传文件：数量={}", files.length);
        
        List<FileUploadResponse> responses = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                String fileUrl = fileStorageService.uploadFile(file);
                
                FileUploadResponse response = new FileUploadResponse();
                response.setFileUrl(fileUrl);
                response.setFileName(file.getOriginalFilename());
                response.setFileSize(file.getSize());
                
                responses.add(response);
            } catch (Exception e) {
                log.error("文件上传失败：{}", file.getOriginalFilename(), e);
            }
        }
        
        return success(responses);
    }
    
    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ValidationException("文件不能为空");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ValidationException("只能上传图片文件");
        }
        
        if (file.getSize() > 10 * 1024 * 1024) {  // 10MB
            throw new ValidationException("文件大小不能超过10MB");
        }
    }
}

@Data
public class FileUploadResponse {
    private String fileUrl;
    private String fileName;
    private Long fileSize;
}
```

---

## 异常处理

### 1. 全局异常处理

```java
/**
 * 全局异常处理器（已内置在nebula-web中）
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：code={}, message={}", e.getCode(), e.getMessage());
        
        return Result.error(e.getCode(), e.getMessage());
    }
    
    /**
     * 验证异常
     */
    @ExceptionHandler(ValidationException.class)
    public Result<Void> handleValidationException(ValidationException e) {
        log.warn("验证异常：{}", e.getMessage());
        
        return Result.error("VALIDATION_ERROR", e.getMessage());
    }
    
    /**
     * 参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        
        String message = e.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));
        
        log.warn("参数验证失败：{}", message);
        
        return Result.error("VALIDATION_ERROR", message);
    }
    
    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        
        return Result.error("SYSTEM_ERROR", "系统错误，请稍后重试");
    }
}
```

### 2. 自定义异常

```java
/**
 * 票务系统业务异常
 */
public class TicketingException extends BusinessException {
    
    public static final String TICKET_SOLD_OUT = "TICKET_SOLD_OUT";
    public static final String ORDER_EXPIRED = "ORDER_EXPIRED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    
    public TicketingException(String code, String message) {
        super(code, message);
    }
    
    public static TicketingException soldOut(String showtimeName) {
        return new TicketingException(TICKET_SOLD_OUT, 
                String.format("演出《%s》门票已售罄", showtimeName));
    }
    
    public static TicketingException orderExpired(String orderNo) {
        return new TicketingException(ORDER_EXPIRED, 
                String.format("订单%s已过期", orderNo));
    }
    
    public static TicketingException paymentFailed(String reason) {
        return new TicketingException(PAYMENT_FAILED, 
                String.format("支付失败：%s", reason));
    }
}
```

---

## 认证授权

### 1. JWT认证

```java
/**
 * 用户登录控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController extends BaseController {
    
    private final UserService userService;
    private final AuthService authService;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        log.info("用户登录：username={}", request.getUsername());
        
        // 1. 验证用户名密码
        User user = userService.validateUser(
                request.getUsername(), request.getPassword());
        
        if (user == null) {
            return error("LOGIN_FAILED", "用户名或密码错误");
        }
        
        // 2. 生成JWT token
        String token = authService.generateToken(user);
        
        // 3. 构建响应
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        
        return success(response);
    }
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Long> register(@Validated @RequestBody RegisterRequest request) {
        log.info("用户注册：username={}", request.getUsername());
        
        Long userId = userService.registerUser(request);
        
        return success(userId);
    }
    
    /**
     * 刷新token
     */
    @PostMapping("/refresh")
    public Result<String> refreshToken(@RequestHeader("Authorization") String token) {
        log.info("刷新token");
        
        String newToken = authService.refreshToken(token);
        
        return success(newToken);
    }
}

@Data
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String nickname;
}
```

### 2. 权限控制

```java
/**
 * 需要认证的控制器
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController extends BaseController {
    
    private final UserService userService;
    
    /**
     * 获取当前用户信息（需要登录）
     */
    @GetMapping("/profile")
    @RequiresAuth  // 需要认证
    public Result<UserProfileVO> getProfile() {
        // 从认证上下文获取当前用户
        AuthUser currentUser = AuthContext.getCurrentUser();
        
        log.info("获取用户信息：userId={}", currentUser.getUserId());
        
        UserProfileVO profile = userService.getUserProfile(currentUser.getUserId());
        
        return success(profile);
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/profile")
    @RequiresAuth
    public Result<Void> updateProfile(@Validated @RequestBody UpdateProfileRequest request) {
        AuthUser currentUser = AuthContext.getCurrentUser();
        
        log.info("更新用户信息：userId={}", currentUser.getUserId());
        
        userService.updateProfile(currentUser.getUserId(), request);
        
        return success();
    }
    
    /**
     * 管理员功能（需要管理员权限）
     */
    @GetMapping("/admin/users")
    @RequiresAuth
    @RequiresRole("ADMIN")  // 需要管理员角色
    public Result<PageResult<UserVO>> listUsers(UserQueryRequest request) {
        log.info("管理员查询用户列表");
        
        PageResult<UserVO> result = userService.queryUsers(request);
        
        return success(result);
    }
}
```

---

## 限流控制

### 1. 基础限流

```java
/**
 * 限流示例
 */
@RestController
@RequestMapping("/api/showtimes")
@RequiredArgsConstructor
@Slf4j
public class ShowtimeControllerWithRateLimit extends BaseController {
    
    private final ShowtimeService showtimeService;
    
    /**
     * 搜索演出（限流保护）
     * 每个用户每分钟最多10次请求
     */
    @GetMapping("/search")
    @RateLimit(limit = 10, duration = 60, keyType = KeyType.USER)
    public Result<List<ShowtimeVO>> search(@RequestParam String keyword) {
        log.info("搜索演出：keyword={}", keyword);
        
        List<ShowtimeVO> result = showtimeService.searchShowtimes(keyword);
        
        return success(result);
    }
    
    /**
     * 秒杀接口（严格限流）
     * 每个IP每秒最多1次请求
     */
    @PostMapping("/seckill")
    @RateLimit(limit = 1, duration = 1, keyType = KeyType.IP)
    public Result<String> seckill(@RequestBody SeckillRequest request) {
        log.info("秒杀请求：{}", request);
        
        String orderNo = showtimeService.seckill(request);
        
        return success(orderNo);
    }
}
```

---

## 响应缓存

### 1. 缓存响应

```java
/**
 * 响应缓存示例
 */
@RestController
@RequestMapping("/api/showtimes")
@RequiredArgsConstructor
@Slf4j
public class ShowtimeControllerWithCache extends BaseController {
    
    private final ShowtimeService showtimeService;
    
    /**
     * 获取热门演出（缓存5分钟）
     */
    @GetMapping("/hot")
    @ResponseCache(ttl = 300)  // 缓存5分钟
    public Result<List<ShowtimeVO>> getHotShowtimes() {
        log.info("查询热门演出");
        
        List<ShowtimeVO> result = showtimeService.getHotShowtimes(10);
        
        return success(result);
    }
    
    /**
     * 获取演出详情（缓存1小时）
     */
    @GetMapping("/{id}")
    @ResponseCache(ttl = 3600, keyPrefix = "showtime")
    public Result<ShowtimeDetailVO> detail(@PathVariable Long id) {
        log.info("查询演出详情：id={}", id);
        
        ShowtimeDetailVO detail = showtimeService.getShowtimeDetail(id);
        
        return success(detail);
    }
}
```

---

## 数据脱敏

### 1. 敏感数据脱敏

```java
/**
 * 用户信息VO（带数据脱敏）
 */
@Data
public class UserProfileVO {
    
    private Long userId;
    
    private String username;
    
    private String nickname;
    
    /**
     * 手机号脱敏：138****1234
     */
    @SensitiveData(type = MaskType.PHONE)
    private String phone;
    
    /**
     * 邮箱脱敏：u***@gmail.com
     */
    @SensitiveData(type = MaskType.EMAIL)
    private String email;
    
    /**
     * 身份证号脱敏：110***********1234
     */
    @SensitiveData(type = MaskType.ID_CARD)
    private String idCard;
    
    /**
     * 地址脱敏：北京市***
     */
    @SensitiveData(type = MaskType.ADDRESS)
    private String address;
}

/**
 * 用户控制器（返回脱敏后的数据）
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController extends BaseController {
    
    private final UserService userService;
    
    /**
     * 获取用户信息（自动脱敏）
     */
    @GetMapping("/profile")
    @RequiresAuth
    public Result<UserProfileVO> getProfile() {
        AuthUser currentUser = AuthContext.getCurrentUser();
        
        UserProfileVO profile = userService.getUserProfile(currentUser.getUserId());
        
        // 返回时自动脱敏
        return success(profile);
    }
}
```

---

## 性能监控

### 1. 性能监控

```java
/**
 * 性能监控示例
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderControllerWithMonitor extends BaseController {
    
    private final OrderService orderService;
    
    /**
     * 创建订单（监控性能）
     */
    @PostMapping
    @PerformanceMonitor  // 自动监控性能
    public Result<String> createOrder(@Validated @RequestBody CreateOrderRequest request) {
        log.info("创建订单：{}", request);
        
        String orderNo = orderService.createOrder(request);
        
        return success(orderNo);
    }
}
```

### 2. 获取性能指标

```java
/**
 * 性能指标控制器
 */
@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
@Slf4j
public class PerformanceController extends BaseController {
    
    private final PerformanceMonitor performanceMonitor;
    
    /**
     * 获取性能指标
     */
    @GetMapping("/metrics")
    @RequiresAuth
    @RequiresRole("ADMIN")
    public Result<PerformanceMetrics> getMetrics() {
        PerformanceMetrics metrics = performanceMonitor.getMetrics();
        
        return success(metrics);
    }
    
    /**
     * 获取系统指标
     */
    @GetMapping("/system")
    @RequiresAuth
    @RequiresRole("ADMIN")
    public Result<SystemMetrics> getSystemMetrics() {
        SystemMetrics metrics = performanceMonitor.getSystemMetrics();
        
        return success(metrics);
    }
}
```

---

## 健康检查

### 1. 健康检查

```java
/**
 * 健康检查控制器（已内置）
 */
@RestController
@RequestMapping("/actuator")
@RequiredArgsConstructor
@Slf4j
public class HealthController {
    
    private final HealthCheckService healthCheckService;
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public HealthCheckResult health() {
        return healthCheckService.check();
    }
}
```

### 2. 自定义健康检查器

```java
/**
 * 自定义健康检查器
 */
@Component
@Slf4j
public class DatabaseHealthChecker implements HealthChecker {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public String getName() {
        return "database";
    }
    
    @Override
    public HealthCheckResult check() {
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(3);
            
            if (valid) {
                return HealthCheckResult.up()
                        .withDetail("database", "MySQL")
                        .withDetail("status", "connected")
                        .build();
            } else {
                return HealthCheckResult.down()
                        .withDetail("error", "数据库连接无效")
                        .build();
            }
        } catch (Exception e) {
            log.error("数据库健康检查失败", e);
            
            return HealthCheckResult.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
```

---

## 票务系统完整示例

### 完整的票务Web应用

```java
/**
 * 票务系统主应用
 */
@SpringBootApplication
@EnableNebulaWeb
public class TicketingApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketingApplication.class, args);
    }
}

/**
 * 完整的票务控制器
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketingController extends BaseController {
    
    private final TicketingService ticketingService;
    
    /**
     * 1. 搜索演出（限流+缓存）
     */
    @GetMapping("/search")
    @RateLimit(limit = 20, duration = 60)
    @ResponseCache(ttl = 60)
    public Result<List<ShowtimeVO>> search(@RequestParam String keyword) {
        log.info("搜索演出：keyword={}", keyword);
        
        List<ShowtimeVO> result = ticketingService.searchShowtimes(keyword);
        
        return success(result);
    }
    
    /**
     * 2. 创建订单（需要登录+限流+性能监控）
     */
    @PostMapping("/orders")
    @RequiresAuth
    @RateLimit(limit = 5, duration = 60, keyType = KeyType.USER)
    @PerformanceMonitor
    public Result<OrderResponse> createOrder(
            @Validated @RequestBody CreateOrderRequest request) {
        
        AuthUser currentUser = AuthContext.getCurrentUser();
        
        log.info("创建订单：userId={}, request={}", currentUser.getUserId(), request);
        
        OrderResponse response = ticketingService.createOrder(
                currentUser.getUserId(), request);
        
        return success(response);
    }
    
    /**
     * 3. 支付订单（需要登录+限流）
     */
    @PostMapping("/orders/{orderNo}/pay")
    @RequiresAuth
    @RateLimit(limit = 10, duration = 60, keyType = KeyType.USER)
    public Result<PaymentResponse> payOrder(@PathVariable String orderNo) {
        AuthUser currentUser = AuthContext.getCurrentUser();
        
        log.info("支付订单：userId={}, orderNo={}", currentUser.getUserId(), orderNo);
        
        PaymentResponse response = ticketingService.payOrder(
                currentUser.getUserId(), orderNo);
        
        return success(response);
    }
    
    /**
     * 4. 获取订单列表（需要登录+缓存+数据脱敏）
     */
    @GetMapping("/orders")
    @RequiresAuth
    @ResponseCache(ttl = 30, keyPrefix = "user-orders")
    public Result<PageResult<OrderVO>> listOrders(OrderQueryRequest request) {
        AuthUser currentUser = AuthContext.getCurrentUser();
        
        log.info("查询订单列表：userId={}", currentUser.getUserId());
        
        request.setUserId(currentUser.getUserId());
        PageResult<OrderVO> result = ticketingService.queryOrders(request);
        
        // OrderVO中的敏感数据会自动脱敏
        return success(result);
    }
    
    /**
     * 5. 获取电子票（需要登录）
     */
    @GetMapping("/tickets/{ticketNo}")
    @RequiresAuth
    public Result<TicketDetailVO> getTicket(@PathVariable String ticketNo) {
        AuthUser currentUser = AuthContext.getCurrentUser();
        
        log.info("获取电子票：userId={}, ticketNo={}", currentUser.getUserId(), ticketNo);
        
        TicketDetailVO ticket = ticketingService.getTicket(ticketNo, currentUser.getUserId());
        
        return success(ticket);
    }
    
    /**
     * 6. 下载电子票二维码
     */
    @GetMapping("/tickets/{ticketNo}/qrcode")
    @RequiresAuth
    public void downloadQRCode(@PathVariable String ticketNo, HttpServletResponse response) {
        AuthUser currentUser = AuthContext.getCurrentUser();
        
        log.info("下载电子票二维码：userId={}, ticketNo={}", currentUser.getUserId(), ticketNo);
        
        try {
            byte[] qrCode = ticketingService.getTicketQRCode(ticketNo, currentUser.getUserId());
            
            response.setContentType("image/png");
            response.setHeader("Content-Disposition", 
                    String.format("attachment; filename=%s-qrcode.png", ticketNo));
            
            response.getOutputStream().write(qrCode);
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("下载二维码失败", e);
            throw new SystemException("下载失败");
        }
    }
}
```

---

## 最佳实践

### 1. RESTful API设计

- **使用标准HTTP方法**：GET（查询）、POST（创建）、PUT（更新）、DELETE（删除）
- **合理的URL设计**：`/api/resources/{id}`
- **统一的响应格式**：使用`Result<T>`包装
- **合理的状态码**：200（成功）、400（参数错误）、401（未认证）、403（无权限）、500（服务器错误）

### 2. 参数验证

- **使用JSR-303验证**：`@NotNull`、`@NotEmpty`、`@Pattern`等
- **自定义验证器**：实现复杂验证逻辑
- **统一错误处理**：全局异常处理器

### 3. 安全防护

- **认证授权**：JWT + RBAC
- **限流保护**：防止接口被刷
- **数据脱敏**：敏感数据脱敏展示
- **CSRF防护**：防跨站请求伪造
- **XSS防护**：过滤恶意脚本

### 4. 性能优化

- **响应缓存**：缓存不常变化的数据
- **分页查询**：大数据量分页返回
- **异步处理**：耗时操作异步执行
- **连接池**：数据库连接池优化

### 5. 可观测性

- **日志记录**：记录关键操作日志
- **性能监控**：监控接口性能
- **健康检查**：提供健康检查接口
- **链路追踪**：分布式链路追踪

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [CONFIG.md](./CONFIG.md) - 配置指南
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 发展路线图

---

**最后更新**: 2025-11-20  
**文档版本**: v2.0


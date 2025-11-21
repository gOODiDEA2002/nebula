# Nebula Foundation 配置指南

> Nebula 框架核心基础模块配置说明

## 目录

- [概述](#概述)
- [通用配置](#通用配置)
- [异常处理配置](#异常处理配置)
- [ID生成器配置](#id生成器配置)
- [JWT配置](#jwt配置)
- [加密配置](#加密配置)
- [JSON配置](#json配置)
- [日期时间配置](#日期时间配置)
- [票务系统场景配置](#票务系统场景配置)
- [环境区分配置](#环境区分配置)

## 概述

`nebula-foundation` 模块是纯工具类模块,主要通过代码级别配置使用,部分功能可通过 Spring 配置文件进行定制。

### 配置优先级

```
1. 代码级配置 (直接调用)
2. application-{profile}.yml
3. application.yml
4. 默认值
```

## 通用配置

### Maven 依赖

```xml
<dependency>
    <groupId>com.andy.nebula</groupId>
    <artifactId>nebula-foundation</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

## 异常处理配置

### 全局异常处理器

在票务系统中,统一异常处理是保证系统稳定性的关键。

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * 业务异常处理
     * 场景: 用户购票时库存不足、订单不存在等业务错误
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        log.warn("业务异常: code={}, message={}, requestId={}", 
            e.getErrorCode(), e.getFormattedMessage(), requestId);
        
        return Result.error(e.getErrorCode(), e.getFormattedMessage())
                .withRequestId(requestId);
    }
    
    /**
     * 验证异常处理
     * 场景: 表单验证失败、参数不合法等
     */
    @ExceptionHandler(ValidationException.class)
    public Result<Object> handleValidationException(ValidationException e, HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        log.warn("参数验证失败: code={}, message={}, requestId={}", 
            e.getErrorCode(), e.getFormattedMessage(), requestId);
        
        // 返回验证错误详情
        Map<String, String> errors = extractFieldErrors(e);
        return Result.validationError("参数验证失败", errors)
                .withRequestId(requestId);
    }
    
    /**
     * 系统异常处理
     * 场景: 数据库连接失败、第三方服务调用异常等
     */
    @ExceptionHandler(SystemException.class)
    public Result<Void> handleSystemException(SystemException e, HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        log.error("系统异常: code={}, message={}, requestId={}", 
            e.getErrorCode(), e.getFormattedMessage(), requestId, e);
        
        // 生产环境不返回详细错误信息
        String message = isProd() ? "系统繁忙，请稍后重试" : e.getFormattedMessage();
        return Result.systemError(message).withRequestId(requestId);
    }
    
    /**
     * 未知异常处理
     * 场景: 未预期的运行时异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        log.error("未知异常: requestId={}", requestId, e);
        
        return Result.systemError("系统内部错误").withRequestId(requestId);
    }
    
    /**
     * 提取字段验证错误
     */
    private Map<String, String> extractFieldErrors(ValidationException e) {
        Map<String, String> errors = new HashMap<>();
        // 从异常中提取字段错误信息
        // 具体实现根据 ValidationException 的结构
        return errors;
    }
    
    private boolean isProd() {
        // 判断是否生产环境
        return "prod".equals(System.getProperty("spring.profiles.active"));
    }
}
```

### 自定义业务异常

票务系统常见业务异常定义:

```java
/**
 * 订单相关异常
 */
public class OrderException extends BusinessException {
    
    public OrderException(String message) {
        super("ORDER_ERROR", message);
    }
    
    // 订单不存在
    public static OrderException notFound(String orderNo) {
        return new OrderException("订单不存在: " + orderNo);
    }
    
    // 订单已支付
    public static OrderException alreadyPaid(String orderNo) {
        return new OrderException("订单已支付: " + orderNo);
    }
    
    // 订单已取消
    public static OrderException cancelled(String orderNo) {
        return new OrderException("订单已取消: " + orderNo);
    }
    
    // 订单超时
    public static OrderException timeout(String orderNo) {
        return new OrderException("订单支付超时: " + orderNo);
    }
}

/**
 * 票务相关异常
 */
public class TicketException extends BusinessException {
    
    public TicketException(String message) {
        super("TICKET_ERROR", message);
    }
    
    // 座位已被占用
    public static TicketException seatOccupied(String seatNo) {
        return new TicketException("座位已被占用: " + seatNo);
    }
    
    // 场次已满
    public static TicketException showtimeFull(String showtimeId) {
        return new TicketException("场次座位已满: " + showtimeId);
    }
    
    // 票已核销
    public static TicketException alreadyUsed(String ticketNo) {
        return new TicketException("票已核销，不可重复使用: " + ticketNo);
    }
}

/**
 * 库存相关异常
 */
public class StockException extends BusinessException {
    
    public StockException(String message) {
        super("STOCK_ERROR", message);
    }
    
    // 库存不足
    public static StockException insufficient(String itemName, int stock) {
        return new StockException(
            String.format("库存不足: %s (剩余: %d)", itemName, stock)
        );
    }
}
```

## ID生成器配置

### 雪花算法配置

票务系统中,订单号、票号等需要分布式唯一ID:

```java
@Configuration
public class IdGeneratorConfig {
    
    /**
     * 订单雪花算法生成器
     * 根据服务器IP或配置分配 workerId 和 datacenterId
     */
    @Bean
    public IdGenerator.SnowflakeIdGenerator orderIdGenerator(
            @Value("${nebula.id.worker-id:1}") long workerId,
            @Value("${nebula.id.datacenter-id:1}") long datacenterId) {
        return IdGenerator.createSnowflake(workerId, datacenterId);
    }
}
```

**配置文件** (`application.yml`):

```yaml
nebula:
  id:
    # 机器ID (0-31), 根据服务器分配
    worker-id: ${WORKER_ID:1}
    # 数据中心ID (0-31), 根据机房分配
    datacenter-id: ${DATACENTER_ID:1}
```

**Docker 部署时的配置** (`docker-compose.yml`):

```yaml
services:
  order-service-1:
    image: ticket-order-service:latest
    environment:
      - WORKER_ID=1
      - DATACENTER_ID=1
  
  order-service-2:
    image: ticket-order-service:latest
    environment:
      - WORKER_ID=2
      - DATACENTER_ID=1
```

### 业务ID生成器配置

票务系统中不同业务对象的ID生成策略:

```java
@Service
public class BusinessIdService {
    
    @Autowired
    private IdGenerator.SnowflakeIdGenerator orderIdGenerator;
    
    /**
     * 生成订单号
     * 格式: O + yyyyMMddHHmmss + 8位随机数
     * 示例: O20251120103045A7k9Xm2p
     */
    public String generateOrderNo() {
        return "O" + IdGenerator.orderNo();
    }
    
    /**
     * 生成票号
     * 格式: T + 雪花ID
     * 示例: T1729234567890123456
     */
    public String generateTicketNo() {
        return "T" + orderIdGenerator.nextId();
    }
    
    /**
     * 生成退款单号
     * 格式: R + yyyyMMddHHmmss + 8位随机数
     * 示例: R20251120103045B8m3Yn4q
     */
    public String generateRefundNo() {
        return "R" + IdGenerator.orderNo();
    }
    
    /**
     * 生成核销码
     * 格式: 8位大写字母+数字
     * 示例: A7K9XM2P
     */
    public String generateVerifyCode() {
        return IdGenerator.shortId(8).toUpperCase();
    }
}
```

## JWT配置

### 基本配置

JWT在票务系统中用于用户认证,需要配置密钥和过期时间:

```java
@Configuration
public class JwtConfig {
    
    @Value("${nebula.security.jwt.secret}")
    private String jwtSecret;
    
    @Value("${nebula.security.jwt.expiration:86400}")
    private long jwtExpiration; // 默认24小时
    
    @Value("${nebula.security.jwt.refresh-expiration:604800}")
    private long refreshExpiration; // 默认7天
    
    private SecretKey jwtKey;
    
    @PostConstruct
    public void init() {
        // 从配置加载或生成密钥
        this.jwtKey = Strings.isNotBlank(jwtSecret) 
            ? JwtUtils.keyFromBase64(jwtSecret)
            : JwtUtils.generateKey();
    }
    
    @Bean
    public SecretKey jwtSecretKey() {
        return jwtKey;
    }
    
    @Bean
    public Duration jwtExpirationDuration() {
        return Duration.ofSeconds(jwtExpiration);
    }
    
    @Bean
    public Duration refreshExpirationDuration() {
        return Duration.ofSeconds(refreshExpiration);
    }
}
```

**配置文件**:

```yaml
nebula:
  security:
    jwt:
      # JWT密钥 (Base64编码, 至少256位)
      # 生产环境必须从环境变量或密钥管理服务读取
      secret: ${JWT_SECRET:}
      # Access Token 过期时间 (秒)
      expiration: 86400      # 24小时
      # Refresh Token 过期时间 (秒)
      refresh-expiration: 604800  # 7天
```

### 票务系统JWT使用示例

```java
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final SecretKey jwtKey;
    private final Duration jwtExpiration;
    private final UserRepository userRepository;
    
    /**
     * 用户登录
     */
    public LoginVO login(LoginDTO dto) {
        // 1. 验证用户名密码
        User user = userRepository.findByUsername(dto.getUsername())
            .orElseThrow(() -> BusinessException.of("用户不存在"));
        
        if (!CryptoUtils.matches(dto.getPassword(), user.getPassword())) {
            throw BusinessException.of("密码错误");
        }
        
        // 2. 生成Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole());
        claims.put("vipLevel", user.getVipLevel());  // 会员等级
        
        String accessToken = JwtUtils.generateToken(
            user.getId().toString(),
            claims,
            jwtExpiration,
            jwtKey
        );
        
        // 3. 生成Refresh Token
        String refreshToken = JwtUtils.generateToken(
            user.getId().toString(),
            Collections.emptyMap(),
            refreshExpiration,
            jwtKey
        );
        
        return LoginVO.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .userId(user.getId())
            .username(user.getUsername())
            .build();
    }
    
    /**
     * 刷新Token
     */
    public TokenVO refreshToken(String refreshToken) {
        // 1. 验证Refresh Token
        JwtParseResult result = JwtUtils.parseToken(refreshToken, jwtKey);
        
        if (!result.isValid()) {
            throw BusinessException.of("Refresh Token无效");
        }
        
        // 2. 重新加载用户信息
        String userId = result.getSubject();
        User user = userRepository.findById(Long.valueOf(userId))
            .orElseThrow(() -> BusinessException.of("用户不存在"));
        
        // 3. 生成新的Access Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole());
        claims.put("vipLevel", user.getVipLevel());
        
        String newAccessToken = JwtUtils.generateToken(
            user.getId().toString(),
            claims,
            jwtExpiration,
            jwtKey
        );
        
        return TokenVO.builder()
            .accessToken(newAccessToken)
            .refreshToken(refreshToken)
            .build();
    }
}
```

## 加密配置

### 密码加密配置

票务系统用户密码加密:

```java
@Configuration
public class CryptoConfig {
    
    @Value("${nebula.security.password.salt-length:16}")
    private int saltLength;
    
    @Value("${nebula.security.password.enforce-strong:true}")
    private boolean enforceStrongPassword;
    
    /**
     * 密码加密服务
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            
            @Override
            public String encode(CharSequence rawPassword) {
                // 检查密码强度
                if (enforceStrongPassword && 
                    !CryptoUtils.isStrongPassword(rawPassword.toString())) {
                    throw ValidationException.of("密码强度不足，需包含大小写字母、数字和特殊字符，至少8位");
                }
                
                return CryptoUtils.encrypt(rawPassword.toString());
            }
            
            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return CryptoUtils.matches(rawPassword.toString(), encodedPassword);
            }
        };
    }
}
```

### 敏感数据加密配置

票务系统中需要加密的敏感数据(如手机号、身份证号):

```java
@Configuration
public class SensitiveDataConfig {
    
    @Value("${nebula.security.aes.key}")
    private String aesKey;
    
    /**
     * 敏感数据加密服务
     */
    @Bean
    public SensitiveDataEncryptor sensitiveDataEncryptor() {
        return new SensitiveDataEncryptor() {
            
            @Override
            public String encrypt(String plainText) {
                if (Strings.isBlank(plainText)) {
                    return plainText;
                }
                return CryptoUtils.aesEncrypt(plainText, aesKey);
            }
            
            @Override
            public String decrypt(String cipherText) {
                if (Strings.isBlank(cipherText)) {
                    return cipherText;
                }
                return CryptoUtils.aesDecrypt(cipherText, aesKey);
            }
        };
    }
}
```

**配置文件**:

```yaml
nebula:
  security:
    password:
      salt-length: 16
      enforce-strong: true  # 是否强制使用强密码
    aes:
      # AES密钥 (从环境变量读取)
      key: ${AES_KEY:}
```

## JSON配置

### 自定义JSON序列化配置

票务系统中的日期时间、金额等特殊字段序列化:

```java
@Configuration
public class JacksonConfig {
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = JsonUtils.getMapper();
        
        // 1. 日期时间格式化
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, 
            new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addSerializer(LocalDate.class, 
            new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        javaTimeModule.addDeserializer(LocalDateTime.class, 
            new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addDeserializer(LocalDate.class, 
            new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        
        mapper.registerModule(javaTimeModule);
        
        // 2. 金额序列化(保留2位小数)
        SimpleModule moneyModule = new SimpleModule();
        moneyModule.addSerializer(BigDecimal.class, new JsonSerializer<BigDecimal>() {
            @Override
            public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) 
                    throws IOException {
                if (value != null) {
                    gen.writeString(value.setScale(2, RoundingMode.HALF_UP).toString());
                }
            }
        });
        mapper.registerModule(moneyModule);
        
        // 3. null值处理
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        // 4. 枚举序列化
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        mapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
        
        return mapper;
    }
}
```

## 日期时间配置

### 时区配置

票务系统需要统一时区处理:

```java
@Configuration
public class DateTimeConfig {
    
    @Value("${nebula.datetime.timezone:Asia/Shanghai}")
    private String timezone;
    
    @PostConstruct
    public void init() {
        // 设置JVM默认时区
        TimeZone.setDefault(TimeZone.getTimeZone(timezone));
        
        // 设置Spring默认时区
        System.setProperty("user.timezone", timezone);
    }
}
```

**配置文件**:

```yaml
nebula:
  datetime:
    timezone: Asia/Shanghai  # 中国时区
```

### 场次时间处理

票务系统场次时间处理示例:

```java
@Service
public class ShowtimeService {
    
    /**
     * 创建场次
     */
    public Showtime createShowtime(CreateShowtimeDTO dto) {
        // 解析开始时间
        LocalDateTime startTime = DateUtils.parseDateTime(
            dto.getStartTime(), 
            "yyyy-MM-dd HH:mm"
        );
        
        // 计算结束时间(加上电影时长)
        LocalDateTime endTime = DateUtils.plusMinutes(
            startTime, 
            dto.getDuration()
        );
        
        // 检查时间是否有效(不能早于当前时间)
        if (startTime.isBefore(LocalDateTime.now())) {
            throw ValidationException.of("场次开始时间不能早于当前时间");
        }
        
        Showtime showtime = new Showtime();
        showtime.setStartTime(startTime);
        showtime.setEndTime(endTime);
        showtime.setCreateTime(DateUtils.nowDateTime());
        
        return showtimeRepository.save(showtime);
    }
    
    /**
     * 查询指定日期的场次
     */
    public List<Showtime> getShowtimesByDate(LocalDate date) {
        LocalDateTime dayStart = DateUtils.startOfDay(date);
        LocalDateTime dayEnd = DateUtils.endOfDay(date);
        
        return showtimeRepository.findByStartTimeBetween(dayStart, dayEnd);
    }
}
```

## 票务系统场景配置

### 订单超时配置

```yaml
ticket:
  order:
    # 订单支付超时时间(分钟)
    payment-timeout: 15
    # 订单自动取消检查间隔(分钟)
    cancel-check-interval: 5
```

```java
@Service
public class OrderTimeoutService {
    
    @Value("${ticket.order.payment-timeout:15}")
    private int paymentTimeoutMinutes;
    
    /**
     * 创建订单时设置超时时间
     */
    public Order createOrder(CreateOrderDTO dto) {
        Order order = new Order();
        order.setOrderNo(businessIdService.generateOrderNo());
        order.setCreateTime(DateUtils.nowDateTime());
        
        // 计算超时时间
        LocalDateTime expireTime = DateUtils.plusMinutes(
            order.getCreateTime(), 
            paymentTimeoutMinutes
        );
        order.setExpireTime(expireTime);
        
        return orderRepository.save(order);
    }
    
    /**
     * 检查订单是否超时
     */
    public boolean isOrderExpired(Order order) {
        return LocalDateTime.now().isAfter(order.getExpireTime());
    }
}
```

### 座位锁定配置

```yaml
ticket:
  seat:
    # 座位锁定时间(分钟)
    lock-timeout: 5
```

```java
@Service
public class SeatLockService {
    
    @Value("${ticket.seat.lock-timeout:5}")
    private int lockTimeoutMinutes;
    
    /**
     * 锁定座位
     */
    public void lockSeat(Long showtimeId, List<String> seatNos, Long userId) {
        String lockKey = "seat:lock:" + showtimeId;
        
        // 使用分布式锁锁定座位
        distributedLock.lock(lockKey, Duration.ofMinutes(lockTimeoutMinutes), () -> {
            // 检查座位是否可用
            for (String seatNo : seatNos) {
                if (isSeatOccupied(showtimeId, seatNo)) {
                    throw TicketException.seatOccupied(seatNo);
                }
            }
            
            // 锁定座位(写入Redis, 设置过期时间)
            seatNos.forEach(seatNo -> {
                String seatKey = "seat:" + showtimeId + ":" + seatNo;
                redisTemplate.opsForValue().set(
                    seatKey, 
                    userId.toString(), 
                    lockTimeoutMinutes, 
                    TimeUnit.MINUTES
                );
            });
        });
    }
}
```

## 环境区分配置

### 开发环境 (`application-dev.yml`)

```yaml
nebula:
  security:
    jwt:
      secret: dev-secret-key-for-testing-only
      expiration: 3600  # 1小时(开发环境短一些)
    password:
      enforce-strong: false  # 开发环境不强制强密码
    aes:
      key: dev-aes-key-for-testing
  
  id:
    worker-id: 1
    datacenter-id: 1
  
  datetime:
    timezone: Asia/Shanghai

ticket:
  order:
    payment-timeout: 30  # 开发环境超时时间长一些
  seat:
    lock-timeout: 10
```

### 测试环境 (`application-test.yml`)

```yaml
nebula:
  security:
    jwt:
      secret: test-secret-key
      expiration: 7200  # 2小时
    password:
      enforce-strong: true
    aes:
      key: ${AES_KEY}  # 从环境变量读取
  
  id:
    worker-id: ${WORKER_ID:1}
    datacenter-id: ${DATACENTER_ID:1}
  
  datetime:
    timezone: Asia/Shanghai

ticket:
  order:
    payment-timeout: 15
  seat:
    lock-timeout: 5
```

### 生产环境 (`application-prod.yml`)

```yaml
nebula:
  security:
    jwt:
      # 生产环境必须从环境变量或密钥管理服务读取
      secret: ${JWT_SECRET}
      expiration: 86400  # 24小时
      refresh-expiration: 604800  # 7天
    password:
      enforce-strong: true
      salt-length: 16
    aes:
      key: ${AES_KEY}  # 从密钥管理服务读取
  
  id:
    worker-id: ${WORKER_ID}  # 从环境变量读取
    datacenter-id: ${DATACENTER_ID}
  
  datetime:
    timezone: Asia/Shanghai

ticket:
  order:
    payment-timeout: 15
    cancel-check-interval: 5
  seat:
    lock-timeout: 5

# 生产环境日志配置
logging:
  level:
    root: INFO
    io.nebula: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/application.log
    max-size: 100MB
    max-history: 30
```

## 最佳实践

### 1. 密钥管理

生产环境密钥不要写在配置文件中:

```bash
# 使用环境变量
export JWT_SECRET="your-jwt-secret-key"
export AES_KEY="your-aes-encryption-key"

# 使用密钥管理服务 (如 Vault, AWS Secrets Manager)
# 在应用启动时从密钥管理服务加载密钥
```

### 2. 配置外部化

```bash
# 使用外部配置文件
java -jar app.jar --spring.config.location=file:/etc/ticket/application.yml

# 使用配置中心 (如 Nacos, Apollo)
```

### 3. 配置加密

```bash
# 使用 Jasypt 加密敏感配置
jasypt.encryptor.password=your-master-password

# 在配置文件中使用加密值
nebula:
  security:
    jwt:
      secret: ENC(encrypted-value-here)
```

## 相关文档

- [模块 README](README.md) - 模块功能介绍
- [示例文档](EXAMPLE.md) - 完整使用示例
- [测试文档](TESTING.md) - 测试指南
- [发展路线图](ROADMAP.md) - 未来规划

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0


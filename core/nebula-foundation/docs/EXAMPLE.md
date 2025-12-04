# Nebula Foundation 使用示例

> 基于票务系统的完整使用示例

## 目录

- [快速开始](#快速开始)
- [统一结果封装](#统一结果封装)
- [异常处理](#异常处理)
- [ID生成器](#id生成器)
- [JWT认证](#jwt认证)
- [加密工具](#加密工具)
- [JSON处理](#json处理)
- [日期时间处理](#日期时间处理)
- [完整业务场景](#完整业务场景)

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>com.andy.nebula</groupId>
    <artifactId>nebula-foundation</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### 最简单的使用

```java
// 生成ID
String orderId = IdGenerator.orderNo();

// 加密密码
String encrypted = CryptoUtils.encrypt("password123");

// 生成JWT Token
SecretKey key = JwtUtils.generateKey();
String token = JwtUtils.generateToken("user123", key);

// JSON序列化
User user = new User("张三", 25);
String json = JsonUtils.toJson(user);

// 日期格式化
String dateStr = DateUtils.formatDateTime(LocalDateTime.now());
```

## 统一结果封装

### 基本使用

**Controller 示例**:

```java
@RestController
@RequestMapping("/api/movies")
public class MovieController {
    
    @Autowired
    private MovieService movieService;
    
    /**
     * 查询电影详情
     */
    @GetMapping("/{id}")
    public Result<MovieVO> getMovie(@PathVariable Long id) {
        MovieVO movie = movieService.getById(id);
        if (movie == null) {
            return Result.notFound("电影不存在");
        }
        return Result.success(movie);
    }
    
    /**
     * 查询电影列表(分页)
     */
    @GetMapping
    public PageResult<MovieVO>> listMovies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        
        PageResult<MovieVO> result = movieService.searchMovies(keyword, page, size);
        return Result.success(result);
    }
    
    /**
     * 创建电影
     */
    @PostMapping
    public Result<MovieVO> createMovie(@RequestBody @Valid CreateMovieDTO dto) {
        MovieVO movie = movieService.create(dto);
        return Result.success(movie, "电影创建成功");
    }
    
    /**
     * 更新电影
     */
    @PutMapping("/{id}")
    public Result<MovieVO> updateMovie(
            @PathVariable Long id,
            @RequestBody @Valid UpdateMovieDTO dto) {
        MovieVO movie = movieService.update(id, dto);
        return Result.success(movie, "电影更新成功");
    }
    
    /**
     * 删除电影
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteMovie(@PathVariable Long id) {
        movieService.delete(id);
        return Result.success(null, "电影删除成功");
    }
}
```

### 响应示例

**成功响应**:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "id": 1,
    "title": "流浪地球3",
    "director": "郭帆",
    "duration": 120,
    "releaseDate": "2025-01-25"
  },
  "timestamp": "2025-11-20T10:30:00",
  "requestId": "a1b2c3d4"
}
```

**错误响应**:

```json
{
  "success": false,
  "code": "MOVIE_NOT_FOUND",
  "message": "电影不存在",
  "timestamp": "2025-11-20T10:30:00",
  "requestId": "a1b2c3d4"
}
```

**分页响应**:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "items": [
      {
        "id": 1,
        "title": "流浪地球3"
      },
      {
        "id": 2,
        "title": "长津湖"
      }
    ],
    "total": 100,
    "page": 1,
    "size": 20,
    "totalPages": 5,
    "hasNext": true,
    "hasPrevious": false
  },
  "timestamp": "2025-11-20T10:30:00"
}
```

## 异常处理

### 业务异常

**票务业务异常示例**:

```java
@Service
@Slf4j
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ShowtimeService showtimeService;
    
    @Autowired
    private SeatService seatService;
    
    /**
     * 创建订单
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(CreateOrderDTO dto) {
        // 1. 检查场次是否存在
        Showtime showtime = showtimeService.getById(dto.getShowtimeId());
        if (showtime == null) {
            throw BusinessException.withCode(
                "SHOWTIME_NOT_FOUND",
                "场次不存在: %d",
                dto.getShowtimeId()
            );
        }
        
        // 2. 检查场次是否已开始
        if (LocalDateTime.now().isAfter(showtime.getStartTime())) {
            throw BusinessException.withCode(
                "SHOWTIME_STARTED",
                "场次已开始，无法购票"
            );
        }
        
        // 3. 检查座位是否可用
        List<String> seatNos = dto.getSeatNos();
        for (String seatNo : seatNos) {
            if (seatService.isOccupied(dto.getShowtimeId(), seatNo)) {
                throw BusinessException.withCode(
                    "SEAT_OCCUPIED",
                    "座位已被占用: %s",
                    seatNo
                );
            }
        }
        
        // 4. 检查库存
        int availableSeats = seatService.getAvailableCount(dto.getShowtimeId());
        if (availableSeats < seatNos.size()) {
            throw BusinessException.withCode(
                "INSUFFICIENT_SEATS",
                "剩余座位不足: %d (需要: %d)",
                availableSeats,
                seatNos.size()
            );
        }
        
        // 5. 创建订单
        Order order = new Order();
        order.setOrderNo(IdGenerator.orderNo());
        order.setUserId(dto.getUserId());
        order.setShowtimeId(dto.getShowtimeId());
        order.setSeats(String.join(",", seatNos));
        order.setAmount(calculateAmount(showtime, seatNos.size()));
        order.setStatus("PENDING");
        order.setCreateTime(DateUtils.nowDateTime());
        
        // 设置超时时间(15分钟后)
        order.setExpireTime(DateUtils.plusMinutes(order.getCreateTime(), 15));
        
        orderRepository.save(order);
        
        // 6. 锁定座位
        seatService.lockSeats(dto.getShowtimeId(), seatNos, order.getId());
        
        log.info("订单创建成功: orderNo={}, userId={}", order.getOrderNo(), dto.getUserId());
        
        return toVO(order, showtime);
    }
    
    /**
     * 支付订单
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO payOrder(String orderNo) {
        // 1. 查询订单
        Order order = orderRepository.findByOrderNo(orderNo)
            .orElseThrow(() -> BusinessException.withCode(
                "ORDER_NOT_FOUND",
                "订单不存在: %s",
                orderNo
            ));
        
        // 2. 检查订单状态
        if (!"PENDING".equals(order.getStatus())) {
            if ("PAID".equals(order.getStatus())) {
                throw BusinessException.withCode(
                    "ORDER_ALREADY_PAID",
                    "订单已支付: %s",
                    orderNo
                );
            } else if ("CANCELLED".equals(order.getStatus())) {
                throw BusinessException.withCode(
                    "ORDER_CANCELLED",
                    "订单已取消: %s",
                    orderNo
                );
            } else if ("EXPIRED".equals(order.getStatus())) {
                throw BusinessException.withCode(
                    "ORDER_EXPIRED",
                    "订单已过期: %s",
                    orderNo
                );
            }
        }
        
        // 3. 检查是否超时
        if (LocalDateTime.now().isAfter(order.getExpireTime())) {
            // 取消订单
            order.setStatus("EXPIRED");
            orderRepository.save(order);
            
            // 释放座位
            seatService.releaseSeats(order.getShowtimeId(), Arrays.asList(order.getSeats().split(",")));
            
            throw BusinessException.withCode(
                "ORDER_EXPIRED",
                "订单已过期: %s",
                orderNo
            );
        }
        
        // 4. 执行支付(调用支付服务)
        paymentService.pay(order);
        
        // 5. 更新订单状态
        order.setStatus("PAID");
        order.setPayTime(DateUtils.nowDateTime());
        orderRepository.save(order);
        
        // 6. 生成电子票
        ticketService.generateTickets(order);
        
        log.info("订单支付成功: orderNo={}", orderNo);
        
        return toVO(order);
    }
}
```

### 参数验证异常

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody @Valid RegisterDTO dto) {
        // @Valid 会自动验证参数
        // 验证失败会抛出 ValidationException
        
        UserVO user = userService.register(dto);
        return Result.success(user, "注册成功");
    }
}

/**
 * 注册DTO
 */
@Data
public class RegisterDTO {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20之间")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度必须在8-32之间")
    private String password;
    
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    @Email(message = "邮箱格式不正确")
    private String email;
}
```

**验证异常响应**:

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "参数验证失败",
  "data": {
    "username": "用户名长度必须在3-20之间",
    "password": "密码长度必须在8-32之间",
    "phone": "手机号格式不正确"
  },
  "timestamp": "2025-11-20T10:30:00"
}
```

## ID生成器

### 票务系统ID生成示例

```java
@Service
public class BusinessIdService {
    
    // 注入配置的雪花算法生成器
    @Autowired
    private IdGenerator.SnowflakeIdGenerator snowflakeGenerator;
    
    /**
     * 生成订单号
     * 格式: O + yyyyMMddHHmmss + 8位随机字母数字
     * 示例: O20251120103045A7k9Xm2p
     */
    public String generateOrderNo() {
        String timestamp = DateUtils.formatDateTime(
            LocalDateTime.now(), 
            "yyyyMMddHHmmss"
        );
        String random = IdGenerator.shortId(8);
        return "O" + timestamp + random;
    }
    
    /**
     * 生成票号 (使用雪花ID)
     * 格式: T + 19位数字ID
     * 示例: T1729234567890123456
     */
    public String generateTicketNo() {
        return "T" + snowflakeGenerator.nextId();
    }
    
    /**
     * 生成核销码 (8位大写字母+数字)
     * 格式: 8位大写字母数字
     * 示例: A7K9XM2P
     */
    public String generateVerifyCode() {
        return IdGenerator.alphanumericId(8).toUpperCase();
    }
    
    /**
     * 生成退款单号
     * 格式: R + yyyyMMddHHmmss + 8位随机字母数字
     * 示例: R20251120103045B8m3Yn4q
     */
    public String generateRefundNo() {
        String timestamp = DateUtils.formatDateTime(
            LocalDateTime.now(), 
            "yyyyMMddHHmmss"
        );
        String random = IdGenerator.shortId(8);
        return "R" + timestamp + random;
    }
    
    /**
     * 生成会员卡号 (12位数字)
     * 格式: 12位数字
     * 示例: 100123456789
     */
    public String generateMemberNo() {
        return IdGenerator.numericId(12);
    }
    
    /**
     * 解析雪花ID信息 (用于排查问题)
     */
    public IdInfo parseTicketNo(String ticketNo) {
        // 去掉前缀 "T"
        long id = Long.parseLong(ticketNo.substring(1));
        return snowflakeGenerator.parseId(id);
    }
}

/**
 * 使用示例
 */
@Service
public class TicketService {
    
    @Autowired
    private BusinessIdService businessIdService;
    
    public Ticket generateTicket(Order order, String seatNo) {
        Ticket ticket = new Ticket();
        ticket.setTicketNo(businessIdService.generateTicketNo());
        ticket.setVerifyCode(businessIdService.generateVerifyCode());
        ticket.setOrderNo(order.getOrderNo());
        ticket.setSeatNo(seatNo);
        ticket.setStatus("VALID");
        ticket.setCreateTime(DateUtils.nowDateTime());
        
        return ticketRepository.save(ticket);
    }
}
```

### 分布式环境ID生成

```java
/**
 * 配置类
 */
@Configuration
public class IdGeneratorConfig {
    
    /**
     * 从配置或环境变量读取 workerId 和 datacenterId
     */
    @Bean
    public IdGenerator.SnowflakeIdGenerator snowflakeIdGenerator(
            @Value("${nebula.id.worker-id:1}") long workerId,
            @Value("${nebula.id.datacenter-id:1}") long datacenterId) {
        
        log.info("初始化雪花ID生成器: workerId={}, datacenterId={}", workerId, datacenterId);
        return IdGenerator.createSnowflake(workerId, datacenterId);
    }
}
```

## JWT认证

### 完整的登录认证流程

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final SecretKey jwtKey;
    private final Duration jwtExpiration;
    private final Duration refreshExpiration;
    
    /**
     * 用户登录
     */
    public LoginVO login(LoginDTO dto) {
        log.info("用户登录: username={}", dto.getUsername());
        
        // 1. 查询用户
        User user = userRepository.findByUsername(dto.getUsername())
            .orElseThrow(() -> BusinessException.withCode(
                "USER_NOT_FOUND",
                "用户不存在: %s",
                dto.getUsername()
            ));
        
        // 2. 验证密码
        if (!CryptoUtils.matches(dto.getPassword(), user.getPassword())) {
            log.warn("密码错误: username={}", dto.getUsername());
            throw BusinessException.withCode(
                "PASSWORD_ERROR",
                "密码错误"
            );
        }
        
        // 3. 检查用户状态
        if ("LOCKED".equals(user.getStatus())) {
            throw BusinessException.withCode(
                "USER_LOCKED",
                "账号已被锁定，请联系管理员"
            );
        }
        
        // 4. 生成 Access Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole());
        claims.put("vipLevel", user.getVipLevel());
        
        String accessToken = JwtUtils.generateToken(
            user.getId().toString(),
            claims,
            jwtExpiration,
            jwtKey
        );
        
        // 5. 生成 Refresh Token
        String refreshToken = JwtUtils.generateToken(
            user.getId().toString(),
            Collections.emptyMap(),
            refreshExpiration,
            jwtKey
        );
        
        // 6. 更新最后登录时间
        user.setLastLoginTime(DateUtils.nowDateTime());
        userRepository.save(user);
        
        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        
        return LoginVO.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtExpiration.toSeconds())
            .userId(user.getId())
            .username(user.getUsername())
            .vipLevel(user.getVipLevel())
            .build();
    }
    
    /**
     * 刷新Token
     */
    public TokenVO refreshToken(String refreshToken) {
        // 1. 解析Refresh Token
        JwtParseResult result = JwtUtils.parseToken(refreshToken, jwtKey);
        
        if (!result.isValid()) {
            throw BusinessException.withCode(
                "INVALID_REFRESH_TOKEN",
                "Refresh Token无效: %s",
                result.getErrorMessage()
            );
        }
        
        // 2. 重新加载用户信息
        String userId = result.getSubject();
        User user = userRepository.findById(Long.valueOf(userId))
            .orElseThrow(() -> BusinessException.withCode(
                "USER_NOT_FOUND",
                "用户不存在"
            ));
        
        // 3. 检查用户状态
        if ("LOCKED".equals(user.getStatus())) {
            throw BusinessException.withCode(
                "USER_LOCKED",
                "账号已被锁定"
            );
        }
        
        // 4. 生成新的Access Token
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
        
        log.info("Token刷新成功: userId={}", userId);
        
        return TokenVO.builder()
            .accessToken(newAccessToken)
            .refreshToken(refreshToken)  // Refresh Token不变
            .tokenType("Bearer")
            .expiresIn(jwtExpiration.toSeconds())
            .build();
    }
    
    /**
     * Token验证 (过滤器中使用)
     */
    public UserPrincipal validateToken(String token) {
        JwtParseResult result = JwtUtils.parseToken(token, jwtKey);
        
        if (!result.isValid()) {
            throw BusinessException.withCode(
                "INVALID_TOKEN",
                "Token无效: %s",
                result.getErrorMessage()
            );
        }
        
        // 提取用户信息
        Long userId = result.getClaim("userId", Long.class);
        String username = result.getClaim("username", String.class);
        String role = result.getClaim("role", String.class);
        
        return new UserPrincipal(userId, username, role);
    }
}
```

### JWT过滤器

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final AuthService authService;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        // 1. 从请求头获取Token
        String token = extractToken(request);
        
        if (token != null) {
            try {
                // 2. 验证Token
                UserPrincipal principal = authService.validateToken(token);
                
                // 3. 设置到Security Context
                SecurityContextHolder.getContext().setAuthentication(
                    new JwtAuthenticationToken(token, principal, principal.getAuthorities())
                );
                
                log.debug("Token验证成功: userId={}", principal.getUserId());
                
            } catch (BusinessException e) {
                log.warn("Token验证失败: {}", e.getMessage());
                // Token无效，不设置认证信息，继续执行过滤链
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

## 加密工具

### 用户密码加密

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 用户注册
     */
    public UserVO register(RegisterDTO dto) {
        // 1. 检查用户名是否存在
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw BusinessException.withCode(
                "USERNAME_EXISTS",
                "用户名已存在: %s",
                dto.getUsername()
            );
        }
        
        // 2. 检查密码强度
        if (!CryptoUtils.isStrongPassword(dto.getPassword())) {
            throw ValidationException.of(
                "密码强度不足，需包含大小写字母、数字和特殊字符，至少8位"
            );
        }
        
        // 3. 加密密码
        String encryptedPassword = CryptoUtils.encrypt(dto.getPassword());
        
        // 4. 创建用户
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(encryptedPassword);
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus("ACTIVE");
        user.setVipLevel(0);  // 普通用户
        user.setCreateTime(DateUtils.nowDateTime());
        
        userRepository.save(user);
        
        return toVO(user);
    }
    
    /**
     * 修改密码
     */
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        // 1. 查询用户
        User user = userRepository.findById(userId)
            .orElseThrow(() -> BusinessException.of("用户不存在"));
        
        // 2. 验证旧密码
        if (!CryptoUtils.matches(dto.getOldPassword(), user.getPassword())) {
            throw BusinessException.of("旧密码错误");
        }
        
        // 3. 检查新密码强度
        if (!CryptoUtils.isStrongPassword(dto.getNewPassword())) {
            throw ValidationException.of("密码强度不足");
        }
        
        // 4. 加密新密码
        String encryptedPassword = CryptoUtils.encrypt(dto.getNewPassword());
        
        // 5. 更新密码
        user.setPassword(encryptedPassword);
        user.setUpdateTime(DateUtils.nowDateTime());
        userRepository.save(user);
    }
}
```

### 敏感数据加密

票务系统中的手机号、身份证号等敏感信息需要加密存储:

```java
@Service
public class UserProfileService {
    
    @Value("${nebula.security.aes.key}")
    private String aesKey;
    
    /**
     * 保存用户实名信息
     */
    public void saveRealNameInfo(Long userId, RealNameDTO dto) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> BusinessException.of("用户不存在"));
        
        // 加密身份证号
        String encryptedIdCard = CryptoUtils.aesEncrypt(dto.getIdCard(), aesKey);
        
        // 加密真实姓名
        String encryptedRealName = CryptoUtils.aesEncrypt(dto.getRealName(), aesKey);
        
        user.setIdCard(encryptedIdCard);
        user.setRealName(encryptedRealName);
        user.setVerified(true);
        user.setVerifyTime(DateUtils.nowDateTime());
        
        userRepository.save(user);
    }
    
    /**
     * 获取用户实名信息 (解密)
     */
    public RealNameVO getRealNameInfo(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> BusinessException.of("用户不存在"));
        
        if (!user.getVerified()) {
            throw BusinessException.of("用户未实名认证");
        }
        
        // 解密身份证号
        String idCard = CryptoUtils.aesDecrypt(user.getIdCard(), aesKey);
        
        // 解密真实姓名
        String realName = CryptoUtils.aesDecrypt(user.getRealName(), aesKey);
        
        return RealNameVO.builder()
            .realName(realName)
            .idCard(maskIdCard(idCard))  // 前端显示时脱敏
            .verified(true)
            .verifyTime(user.getVerifyTime())
            .build();
    }
    
    /**
     * 身份证号脱敏显示
     * 示例: 110101199001011234 -> 110101******1234
     */
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 18) {
            return idCard;
        }
        return idCard.substring(0, 6) + "******" + idCard.substring(14);
    }
}
```

## JSON处理

### 票务系统实体序列化

```java
/**
 * 订单VO
 */
@Data
@Builder
public class OrderVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private String username;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
    
    private BigDecimal amount;
    private String status;
    private String statusText;
    
    // 场次信息
    private ShowtimeVO showtime;
    
    // 座位信息
    private List<SeatVO> seats;
    
    // 电子票
    private List<TicketVO> tickets;
}

/**
 * Service层使用
 */
@Service
public class OrderService {
    
    /**
     * 序列化订单 (保存到缓存)
     */
    public void cacheOrder(OrderVO order) {
        String orderJson = JsonUtils.toJson(order);
        redisTemplate.opsForValue().set(
            "order:" + order.getOrderNo(),
            orderJson,
            15,
            TimeUnit.MINUTES
        );
    }
    
    /**
     * 反序列化订单 (从缓存读取)
     */
    public OrderVO getOrderFromCache(String orderNo) {
        String orderJson = redisTemplate.opsForValue().get("order:" + orderNo);
        if (orderJson != null) {
            return JsonUtils.fromJson(orderJson, OrderVO.class);
        }
        return null;
    }
    
    /**
     * 序列化列表
     */
    public void cacheOrders(List<OrderVO> orders) {
        String ordersJson = JsonUtils.toJson(orders);
        redisTemplate.opsForValue().set("orders:list", ordersJson, 5, TimeUnit.MINUTES);
    }
    
    /**
     * 反序列化列表
     */
    public List<OrderVO> getOrdersFromCache() {
        String ordersJson = redisTemplate.opsForValue().get("orders:list");
        if (ordersJson != null) {
            return JsonUtils.toList(ordersJson, OrderVO.class);
        }
        return Collections.emptyList();
    }
}
```

## 日期时间处理

### 场次管理

```java
@Service
public class ShowtimeService {
    
    /**
     * 创建场次
     */
    public ShowtimeVO createShowtime(CreateShowtimeDTO dto) {
        // 1. 解析开始时间
        LocalDateTime startTime = DateUtils.parseDateTime(
            dto.getStartTime(), 
            "yyyy-MM-dd HH:mm"
        );
        
        // 2. 计算结束时间
        LocalDateTime endTime = DateUtils.plusMinutes(
            startTime, 
            dto.getDuration()
        );
        
        // 3. 验证时间
        if (startTime.isBefore(LocalDateTime.now())) {
            throw ValidationException.of("场次开始时间不能早于当前时间");
        }
        
        // 4. 检查时间冲突
        if (hasTimeConflict(dto.getCinemaHallId(), startTime, endTime)) {
            throw BusinessException.of("该时间段已有其他场次");
        }
        
        // 5. 保存场次
        Showtime showtime = new Showtime();
        showtime.setMovieId(dto.getMovieId());
        showtime.setCinemaHallId(dto.getCinemaHallId());
        showtime.setStartTime(startTime);
        showtime.setEndTime(endTime);
        showtime.setPrice(dto.getPrice());
        showtime.setStatus("AVAILABLE");
        showtime.setCreateTime(DateUtils.nowDateTime());
        
        showtimeRepository.save(showtime);
        
        return toVO(showtime);
    }
    
    /**
     * 查询指定日期的场次
     */
    public List<ShowtimeVO> getShowtimesByDate(Long movieId, LocalDate date) {
        // 获取当天的开始和结束时间
        LocalDateTime dayStart = DateUtils.startOfDay(date);
        LocalDateTime dayEnd = DateUtils.endOfDay(date);
        
        List<Showtime> showtimes = showtimeRepository.findByMovieIdAndStartTimeBetween(
            movieId, 
            dayStart, 
            dayEnd
        );
        
        return showtimes.stream()
            .map(this::toVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取最近7天的场次
     */
    public List<ShowtimeVO> getRecentShowtimes(Long movieId) {
        LocalDateTime now = DateUtils.nowDateTime();
        LocalDateTime weekLater = DateUtils.plusDays(now.toLocalDate(), 7)
            .atTime(23, 59, 59);
        
        List<Showtime> showtimes = showtimeRepository.findByMovieIdAndStartTimeBetween(
            movieId,
            now,
            weekLater
        );
        
        return showtimes.stream()
            .map(this::toVO)
            .collect(Collectors.toList());
    }
}
```

### 订单超时处理

```java
@Service
@Slf4j
public class OrderTimeoutService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private SeatService seatService;
    
    /**
     * 定时任务: 检查并取消超时订单
     */
    @Scheduled(cron = "0 */5 * * * *")  // 每5分钟执行一次
    public void cancelExpiredOrders() {
        LocalDateTime now = DateUtils.nowDateTime();
        
        // 查询超时的待支付订单
        List<Order> expiredOrders = orderRepository.findByStatusAndExpireTimeBefore(
            "PENDING", 
            now
        );
        
        log.info("发现超时订单: count={}", expiredOrders.size());
        
        for (Order order : expiredOrders) {
            try {
                // 取消订单
                order.setStatus("EXPIRED");
                order.setUpdateTime(now);
                orderRepository.save(order);
                
                // 释放座位
                List<String> seatNos = Arrays.asList(order.getSeats().split(","));
                seatService.releaseSeats(order.getShowtimeId(), seatNos);
                
                log.info("订单已取消: orderNo={}, expireTime={}", 
                    order.getOrderNo(), 
                    DateUtils.formatDateTime(order.getExpireTime())
                );
                
            } catch (Exception e) {
                log.error("取消订单失败: orderNo={}", order.getOrderNo(), e);
            }
        }
    }
    
    /**
     * 计算订单剩余时间
     */
    public long getRemainingMinutes(Order order) {
        LocalDateTime now = DateUtils.nowDateTime();
        return DateUtils.minutesBetween(now, order.getExpireTime());
    }
}
```

## 完整业务场景

### 购票完整流程

```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class TicketPurchaseService {
    
    private final ShowtimeService showtimeService;
    private final SeatService seatService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final TicketService ticketService;
    private final BusinessIdService businessIdService;
    
    /**
     * 购票完整流程
     */
    public PurchaseResultVO purchaseTicket(PurchaseTicketDTO dto) {
        log.info("开始购票流程: userId={}, showtimeId={}, seats={}", 
            dto.getUserId(), dto.getShowtimeId(), dto.getSeatNos());
        
        try {
            // 1. 验证场次
            Showtime showtime = validateShowtime(dto.getShowtimeId());
            
            // 2. 锁定座位
            lockSeats(dto.getShowtimeId(), dto.getSeatNos(), dto.getUserId());
            
            // 3. 创建订单
            Order order = createOrder(dto, showtime);
            
            // 4. 执行支付
            Payment payment = executePayment(order, dto.getPaymentMethod());
            
            // 5. 生成电子票
            List<Ticket> tickets = generateTickets(order);
            
            // 6. 发送通知
            sendNotification(order, tickets);
            
            log.info("购票成功: orderNo={}, ticketCount={}", 
                order.getOrderNo(), tickets.size());
            
            return buildResult(order, payment, tickets);
            
        } catch (Exception e) {
            log.error("购票失败: userId={}, error={}", dto.getUserId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 验证场次
     */
    private Showtime validateShowtime(Long showtimeId) {
        Showtime showtime = showtimeService.getById(showtimeId);
        if (showtime == null) {
            throw BusinessException.withCode("SHOWTIME_NOT_FOUND", "场次不存在");
        }
        
        if (LocalDateTime.now().isAfter(showtime.getStartTime())) {
            throw BusinessException.withCode("SHOWTIME_STARTED", "场次已开始");
        }
        
        return showtime;
    }
    
    /**
     * 锁定座位
     */
    private void lockSeats(Long showtimeId, List<String> seatNos, Long userId) {
        for (String seatNo : seatNos) {
            if (!seatService.tryLock(showtimeId, seatNo, userId)) {
                throw BusinessException.withCode(
                    "SEAT_OCCUPIED", 
                    "座位已被占用: " + seatNo
                );
            }
        }
    }
    
    /**
     * 创建订单
     */
    private Order createOrder(PurchaseTicketDTO dto, Showtime showtime) {
        Order order = new Order();
        order.setOrderNo(businessIdService.generateOrderNo());
        order.setUserId(dto.getUserId());
        order.setShowtimeId(dto.getShowtimeId());
        order.setSeats(String.join(",", dto.getSeatNos()));
        order.setAmount(calculateAmount(showtime, dto.getSeatNos().size()));
        order.setStatus("PENDING");
        order.setCreateTime(DateUtils.nowDateTime());
        order.setExpireTime(DateUtils.plusMinutes(order.getCreateTime(), 15));
        
        return orderRepository.save(order);
    }
    
    /**
     * 执行支付
     */
    private Payment executePayment(Order order, String paymentMethod) {
        Payment payment = paymentService.createPayment(
            order, 
            paymentMethod, 
            businessIdService.generatePaymentNo()
        );
        
        // 调用支付网关...
        paymentService.process(payment);
        
        // 更新订单状态
        order.setStatus("PAID");
        order.setPayTime(DateUtils.nowDateTime());
        orderRepository.save(order);
        
        return payment;
    }
    
    /**
     * 生成电子票
     */
    private List<Ticket> generateTickets(Order order) {
        List<String> seatNos = Arrays.asList(order.getSeats().split(","));
        List<Ticket> tickets = new ArrayList<>();
        
        for (String seatNo : seatNos) {
            Ticket ticket = new Ticket();
            ticket.setTicketNo(businessIdService.generateTicketNo());
            ticket.setVerifyCode(businessIdService.generateVerifyCode());
            ticket.setOrderNo(order.getOrderNo());
            ticket.setUserId(order.getUserId());
            ticket.setShowtimeId(order.getShowtimeId());
            ticket.setSeatNo(seatNo);
            ticket.setStatus("VALID");
            ticket.setCreateTime(DateUtils.nowDateTime());
            
            tickets.add(ticketRepository.save(ticket));
        }
        
        return tickets;
    }
    
    /**
     * 发送通知
     */
    private void sendNotification(Order order, List<Ticket> tickets) {
        // 发送购票成功通知...
        notificationService.sendPurchaseSuccess(order, tickets);
    }
    
    /**
     * 构建返回结果
     */
    private PurchaseResultVO buildResult(Order order, Payment payment, List<Ticket> tickets) {
        return PurchaseResultVO.builder()
            .orderNo(order.getOrderNo())
            .amount(order.getAmount())
            .paymentMethod(payment.getMethod())
            .tickets(tickets.stream().map(this::toTicketVO).collect(Collectors.toList()))
            .build();
    }
}
```

## 相关文档

- [模块 README](README.md) - 模块功能介绍
- [配置文档](CONFIG.md) - 详细配置说明
- [测试文档](TESTING.md) - 测试指南
- [发展路线图](ROADMAP.md) - 未来规划

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0


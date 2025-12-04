# Nebula Security 使用示例

> 基于票务系统的完整认证授权示例

## 目录

- [快速开始](#快速开始)
- [用户认证](#用户认证)
- [权限控制](#权限控制)
- [安全注解](#安全注解)
- [完整业务场景](#完整业务场景)
- [常见问题](#常见问题)

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>com.andy.nebula</groupId>
    <artifactId>nebula-security</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### 最简单的使用

```yaml
# application.yml
nebula:
  security:
    enabled: true
    jwt:
      secret: ${JWT_SECRET}
      expiration: 86400
```

```java
// 1. Controller 使用注解
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/profile")
    @RequiresAuthentication  // 需要登录
    public Result<UserVO> getProfile() {
        Long userId = SecurityContext.getCurrentUserId();
        User user = userService.getById(userId);
        return Result.success(toVO(user));
    }
}

// 2. Service 获取当前用户
@Service
public class OrderService {
    
    public OrderVO createOrder(CreateOrderDTO dto) {
        // 获取当前用户ID
        Long userId = SecurityContext.getCurrentUserId();
        
        // 创建订单...
        return orderVO;
    }
}
```

## 用户认证

### 用户注册

```java
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AuthService authService;
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<RegisterResultVO> register(@RequestBody @Valid RegisterDTO dto) {
        log.info("用户注册: username={}, phone={}", dto.getUsername(), dto.getPhone());
        
        try {
            // 1. 注册用户
            UserVO user = userService.register(dto);
            
            // 2. 自动登录
            LoginVO loginVO = authService.autoLogin(user.getId());
            
            log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());
            
            return Result.success(
                RegisterResultVO.builder()
                    .user(user)
                    .accessToken(loginVO.getAccessToken())
                    .refreshToken(loginVO.getRefreshToken())
                    .build(),
                "注册成功"
            );
            
        } catch (BusinessException e) {
            log.warn("用户注册失败: {}", e.getMessage());
            throw e;
        }
    }
}

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    
    /**
     * 注册用户
     */
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(RegisterDTO dto) {
        // 1. 检查用户名是否存在
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw BusinessException.withCode(
                "USERNAME_EXISTS",
                "用户名已存在: %s",
                dto.getUsername()
            );
        }
        
        // 2. 检查手机号是否存在
        if (userRepository.existsByPhone(dto.getPhone())) {
            throw BusinessException.withCode(
                "PHONE_EXISTS",
                "手机号已注册: %s",
                dto.getPhone()
            );
        }
        
        // 3. 检查密码强度
        if (!CryptoUtils.isStrongPassword(dto.getPassword())) {
            throw ValidationException.of(
                "密码强度不足，需包含大小写字母、数字和特殊字符，至少8位"
            );
        }
        
        // 4. 加密密码
        String encryptedPassword = CryptoUtils.encrypt(dto.getPassword());
        
        // 5. 创建用户
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(encryptedPassword);
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus("ACTIVE");
        user.setVipLevel(0);  // 普通用户
        user.setCreateTime(DateUtils.nowDateTime());
        
        userRepository.save(user);
        
        // 6. 分配默认角色 (USER)
        Role userRole = roleRepository.findByCode("USER")
            .orElseThrow(() -> new SystemException("ROLE_NOT_FOUND", "默认角色不存在"));
        
        UserRole userRoleRel = new UserRole();
        userRoleRel.setUserId(user.getId());
        userRoleRel.setRoleId(userRole.getId());
        userRoleRel.setCreateTime(DateUtils.nowDateTime());
        userRoleRepository.save(userRoleRel);
        
        log.info("用户创建成功: userId={}, username={}", user.getId(), user.getUsername());
        
        return toVO(user);
    }
}
```

### 用户登录

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        log.info("用户登录: username={}", dto.getUsername());
        
        try {
            LoginVO loginVO = authService.login(dto);
            
            log.info("用户登录成功: username={}, userId={}", 
                dto.getUsername(), loginVO.getUserId());
            
            return Result.success(loginVO, "登录成功");
            
        } catch (BusinessException e) {
            log.warn("用户登录失败: username={}, error={}", 
                dto.getUsername(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public Result<TokenVO> refreshToken(@RequestBody RefreshTokenDTO dto) {
        log.info("刷新Token");
        
        try {
            TokenVO tokenVO = authService.refreshToken(dto.getRefreshToken());
            
            log.info("Token刷新成功");
            
            return Result.success(tokenVO);
            
        } catch (BusinessException e) {
            log.warn("Token刷新失败: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 登出
     */
    @PostMapping("/logout")
    @RequiresAuthentication
    public Result<Void> logout() {
        Long userId = SecurityContext.getCurrentUserId();
        
        log.info("用户登出: userId={}", userId);
        
        // 清除用户缓存
        authService.logout(userId);
        
        // 清除Security Context
        SecurityContext.clear();
        
        return Result.success(null, "登出成功");
    }
}

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final SecretKey jwtKey;
    private final Duration jwtExpiration;
    private final Duration refreshExpiration;
    private final RedisTemplate<String, String> redisTemplate;
    
    /**
     * 用户登录
     */
    public LoginVO login(LoginDTO dto) {
        // 1. 查询用户
        User user = userRepository.findByUsername(dto.getUsername())
            .orElseThrow(() -> BusinessException.withCode(
                "USER_NOT_FOUND",
                "用户不存在: %s",
                dto.getUsername()
            ));
        
        // 2. 验证密码
        if (!CryptoUtils.matches(dto.getPassword(), user.getPassword())) {
            // 记录登录失败次数
            incrementLoginFailCount(user.getId());
            
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
        
        // 4. 加载用户权限
        List<String> permissions = loadUserPermissions(user.getId());
        
        // 5. 生成 Access Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole());
        claims.put("vipLevel", user.getVipLevel());
        claims.put("permissions", permissions);
        
        String accessToken = JwtUtils.generateToken(
            user.getId().toString(),
            claims,
            jwtExpiration,
            jwtKey
        );
        
        // 6. 生成 Refresh Token
        String refreshToken = JwtUtils.generateToken(
            user.getId().toString(),
            Collections.emptyMap(),
            refreshExpiration,
            jwtKey
        );
        
        // 7. 更新最后登录时间
        user.setLastLoginTime(DateUtils.nowDateTime());
        user.setLastLoginIp(getClientIp());
        userRepository.save(user);
        
        // 8. 清除登录失败记录
        clearLoginFailCount(user.getId());
        
        // 9. 缓存Refresh Token
        cacheRefreshToken(user.getId(), refreshToken);
        
        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        
        return LoginVO.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtExpiration.toSeconds())
            .userId(user.getId())
            .username(user.getUsername())
            .role(user.getRole())
            .vipLevel(user.getVipLevel())
            .permissions(permissions)
            .build();
    }
    
    /**
     * 加载用户权限
     */
    private List<String> loadUserPermissions(Long userId) {
        // 1. 从缓存加载
        String cacheKey = "user:permissions:" + userId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return JsonUtils.toList(cached, String.class);
        }
        
        // 2. 从数据库加载
        Set<String> permissions = new HashSet<>();
        
        // 加载用户的所有角色
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        
        for (UserRole userRole : userRoles) {
            // 加载角色的所有权限
            List<RolePermission> rolePermissions = 
                rolePermissionRepository.findByRoleId(userRole.getRoleId());
            
            for (RolePermission rolePermission : rolePermissions) {
                Permission permission = permissionRepository.findById(rolePermission.getPermissionId())
                    .orElse(null);
                if (permission != null) {
                    permissions.add(permission.getCode());
                }
            }
        }
        
        List<String> result = new ArrayList<>(permissions);
        
        // 3. 缓存权限 (30分钟)
        redisTemplate.opsForValue().set(
            cacheKey,
            JsonUtils.toJson(result),
            30,
            TimeUnit.MINUTES
        );
        
        return result;
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
        
        // 2. 验证Refresh Token是否在缓存中
        String userId = result.getSubject();
        String cachedToken = redisTemplate.opsForValue().get("refresh_token:" + userId);
        if (!refreshToken.equals(cachedToken)) {
            throw BusinessException.withCode(
                "INVALID_REFRESH_TOKEN",
                "Refresh Token已失效"
            );
        }
        
        // 3. 重新加载用户信息
        User user = userRepository.findById(Long.valueOf(userId))
            .orElseThrow(() -> BusinessException.withCode(
                "USER_NOT_FOUND",
                "用户不存在"
            ));
        
        // 4. 检查用户状态
        if ("LOCKED".equals(user.getStatus())) {
            throw BusinessException.withCode(
                "USER_LOCKED",
                "账号已被锁定"
            );
        }
        
        // 5. 重新加载权限
        List<String> permissions = loadUserPermissions(user.getId());
        
        // 6. 生成新的Access Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole());
        claims.put("vipLevel", user.getVipLevel());
        claims.put("permissions", permissions);
        
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
}
```

### JWT过滤器

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final SecretKey jwtKey;
    private final SecurityProperties securityProperties;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        // 1. 检查是否是匿名访问URL
        if (isAnonymousUrl(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 2. 从请求头获取Token
        String token = extractToken(request);
        
        if (token != null) {
            try {
                // 3. 验证Token
                JwtParseResult result = JwtUtils.parseToken(token, jwtKey);
                
                if (!result.isValid()) {
                    log.warn("Token验证失败: {}", result.getErrorMessage());
                    sendUnauthorizedResponse(response, "Token无效");
                    return;
                }
                
                // 4. 提取用户信息
                Long userId = result.getClaim("userId", Long.class);
                String username = result.getClaim("username", String.class);
                String role = result.getClaim("role", String.class);
                
                @SuppressWarnings("unchecked")
                List<String> permissions = result.getClaim("permissions", List.class);
                
                // 5. 创建权限列表
                List<GrantedAuthority> authorities = permissions.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
                
                // 6. 创建用户主体
                UserPrincipal principal = new UserPrincipal(userId, username, role, authorities);
                
                // 7. 创建认证Token
                JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                    token,
                    principal,
                    authorities
                );
                
                // 8. 设置到Security Context
                SecurityContext.setAuthentication(authentication);
                
                log.debug("Token验证成功: userId={}, username={}", userId, username);
                
            } catch (Exception e) {
                log.error("Token验证异常", e);
                sendUnauthorizedResponse(response, "Token验证异常");
                return;
            }
        } else {
            log.warn("请求头缺少Token: {}", request.getRequestURI());
            sendUnauthorizedResponse(response, "未登录");
            return;
        }
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 9. 清除Security Context
            SecurityContext.clear();
        }
    }
    
    /**
     * 从请求头提取Token
     */
    private String extractToken(HttpServletRequest request) {
        String headerName = securityProperties.getJwt().getHeaderName();
        String tokenPrefix = securityProperties.getJwt().getTokenPrefix();
        
        String bearerToken = request.getHeader(headerName);
        if (bearerToken != null && bearerToken.startsWith(tokenPrefix)) {
            return bearerToken.substring(tokenPrefix.length());
        }
        return null;
    }
    
    /**
     * 检查是否是匿名访问URL
     */
    private boolean isAnonymousUrl(String requestUri) {
        List<String> anonymousUrls = securityProperties.getAnonymousUrls();
        return anonymousUrls.stream()
            .anyMatch(pattern -> new AntPathMatcher().match(pattern, requestUri));
    }
    
    /**
     * 发送未授权响应
     */
    private void sendUnauthorizedResponse(
            HttpServletResponse response,
            String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        Result<Void> result = Result.unauthorized(message);
        response.getWriter().write(JsonUtils.toJson(result));
    }
}
```

## 权限控制

### 基于注解的权限控制

```java
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    
    /**
     * 查询订单列表 (需要 order:view 权限)
     */
    @GetMapping
    @RequiresPermission("order:view")
    public PageResult<OrderVO>> listOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Long userId = SecurityContext.getCurrentUserId();
        PageResult<OrderVO> result = orderService.listOrders(userId, page, size);
        return Result.success(result);
    }
    
    /**
     * 创建订单 (需要 order:create 权限)
     */
    @PostMapping
    @RequiresPermission("order:create")
    public Result<OrderVO> createOrder(@RequestBody @Valid CreateOrderDTO dto) {
        Long userId = SecurityContext.getCurrentUserId();
        dto.setUserId(userId);
        
        OrderVO order = orderService.createOrder(dto);
        return Result.success(order, "订单创建成功");
    }
    
    /**
     * 取消订单 (需要 order:cancel 权限)
     */
    @PostMapping("/{orderNo}/cancel")
    @RequiresPermission("order:cancel")
    public Result<Void> cancelOrder(@PathVariable String orderNo) {
        orderService.cancelOrder(orderNo);
        return Result.success(null, "订单已取消");
    }
    
    /**
     * 删除订单 (需要 order:delete 权限，通常只有管理员有)
     */
    @DeleteMapping("/{orderNo}")
    @RequiresPermission("order:delete")
    public Result<Void> deleteOrder(@PathVariable String orderNo) {
        orderService.deleteOrder(orderNo);
        return Result.success(null, "订单已删除");
    }
    
    /**
     * 导出订单 (需要 order:view 和 order:export 权限)
     */
    @GetMapping("/export")
    @RequiresPermission(
        value = {"order:view", "order:export"},
        logical = RequiresPermission.Logical.AND
    )
    public void exportOrders(HttpServletResponse response) {
        orderService.exportOrders(response);
    }
}
```

### 基于角色的权限控制

```java
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final UserService userService;
    private final SystemService systemService;
    
    /**
     * 管理员访问 (需要 ADMIN 或 SUPER_ADMIN 角色)
     */
    @GetMapping("/users")
    @RequiresRole(
        value = {"ADMIN", "SUPER_ADMIN"},
        logical = RequiresPermission.Logical.OR
    )
    public PageResult<UserVO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageResult<UserVO> result = userService.listAllUsers(page, size);
        return Result.success(result);
    }
    
    /**
     * 超级管理员访问 (只有 SUPER_ADMIN 角色)
     */
    @PostMapping("/system/config")
    @RequiresRole("SUPER_ADMIN")
    public Result<Void> updateSystemConfig(@RequestBody SystemConfigDTO dto) {
        systemService.updateConfig(dto);
        return Result.success(null, "系统配置已更新");
    }
}
```

### 数据权限控制

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final OrderRepository orderRepository;
    
    /**
     * 根据用户权限查询订单
     */
    public PageResult<OrderVO> listOrders(Long userId, int page, int size) {
        // 获取当前用户的认证信息
        Authentication auth = SecurityContext.getAuthentication();
        
        // 检查权限级别
        if (hasPermission(auth, "order:view:all")) {
            // 可以查看所有订单 (管理员)
            return listAllOrders(page, size);
            
        } else if (hasPermission(auth, "order:view:dept")) {
            // 可以查看部门订单 (部门经理)
            String deptId = getCurrentUserDept(userId);
            return listDeptOrders(deptId, page, size);
            
        } else if (hasPermission(auth, "order:view:own")) {
            // 只能查看自己的订单 (普通用户)
            return listUserOrders(userId, page, size);
            
        } else {
            throw BusinessException.forbidden("无权限查看订单");
        }
    }
    
    private boolean hasPermission(Authentication auth, String permission) {
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals(permission));
    }
}
```

## 安全注解

### @RequiresAuthentication

```java
/**
 * 需要登录才能访问
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/profile")
    @RequiresAuthentication
    public Result<UserVO> getProfile() {
        Long userId = SecurityContext.getCurrentUserId();
        User user = userService.getById(userId);
        return Result.success(toVO(user));
    }
    
    @PutMapping("/profile")
    @RequiresAuthentication
    public Result<UserVO> updateProfile(@RequestBody @Valid UpdateProfileDTO dto) {
        Long userId = SecurityContext.getCurrentUserId();
        UserVO user = userService.updateProfile(userId, dto);
        return Result.success(user, "资料更新成功");
    }
}
```

### @RequiresPermission

```java
/**
 * 需要特定权限
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    
    /**
     * 核销票 (需要 ticket:verify 权限)
     */
    @PostMapping("/{ticketNo}/verify")
    @RequiresPermission("ticket:verify")
    public Result<TicketVO> verifyTicket(@PathVariable String ticketNo) {
        TicketVO ticket = ticketService.verify(ticketNo);
        return Result.success(ticket, "核销成功");
    }
    
    /**
     * 退票 (需要 ticket:refund 权限)
     */
    @PostMapping("/{ticketNo}/refund")
    @RequiresPermission("ticket:refund")
    public Result<RefundVO> refundTicket(
            @PathVariable String ticketNo,
            @RequestBody @Valid RefundDTO dto) {
        RefundVO refund = ticketService.refund(ticketNo, dto);
        return Result.success(refund, "退票成功");
    }
}
```

### @RequiresRole

```java
/**
 * 需要特定角色
 */
@RestController
@RequestMapping("/api/cinemas")
public class CinemaController {
    
    /**
     * 创建影院 (需要 CINEMA_ADMIN 角色)
     */
    @PostMapping
    @RequiresRole("CINEMA_ADMIN")
    public Result<CinemaVO> createCinema(@RequestBody @Valid CreateCinemaDTO dto) {
        CinemaVO cinema = cinemaService.create(dto);
        return Result.success(cinema, "影院创建成功");
    }
    
    /**
     * 删除影院 (需要 SUPER_ADMIN 角色)
     */
    @DeleteMapping("/{id}")
    @RequiresRole("SUPER_ADMIN")
    public Result<Void> deleteCinema(@PathVariable Long id) {
        cinemaService.delete(id);
        return Result.success(null, "影院已删除");
    }
}
```

## 完整业务场景

### 购票流程 (认证 + 授权)

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
    
    /**
     * 购票流程
     * 
     * 权限要求:
     * - 用户必须登录
     * - 用户必须有 order:create 权限
     */
    @RequiresAuthentication
    @RequiresPermission("order:create")
    public PurchaseResultVO purchaseTicket(PurchaseTicketDTO dto) {
        // 1. 获取当前用户
        Long userId = SecurityContext.getCurrentUserId();
        String username = SecurityContext.getCurrentUsername();
        
        log.info("开始购票: userId={}, username={}, showtimeId={}, seats={}",
            userId, username, dto.getShowtimeId(), dto.getSeatNos());
        
        // 2. 验证场次
        Showtime showtime = showtimeService.getById(dto.getShowtimeId());
        if (showtime == null) {
            throw BusinessException.of("场次不存在");
        }
        
        if (LocalDateTime.now().isAfter(showtime.getStartTime())) {
            throw BusinessException.of("场次已开始，无法购票");
        }
        
        // 3. 检查VIP权限 (VIP用户可以提前购票)
        if (showtime.isVipOnly()) {
            // 检查用户VIP等级
            Authentication auth = SecurityContext.getAuthentication();
            if (auth instanceof JwtAuthenticationToken) {
                JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) auth;
                UserPrincipal principal = (UserPrincipal) jwtAuth.getPrincipal();
                
                if (principal.getVipLevel() < showtime.getRequiredVipLevel()) {
                    throw BusinessException.forbidden(
                        String.format("该场次仅限VIP%d及以上会员购买",
                            showtime.getRequiredVipLevel())
                    );
                }
            }
        }
        
        // 4. 锁定座位
        seatService.lockSeats(dto.getShowtimeId(), dto.getSeatNos(), userId);
        
        // 5. 创建订单
        Order order = createOrder(userId, dto, showtime);
        
        // 6. 执行支付
        Payment payment = paymentService.process(order, dto.getPaymentMethod());
        
        // 7. 生成电子票
        List<Ticket> tickets = ticketService.generate(order);
        
        log.info("购票成功: orderNo={}, ticketCount={}",
            order.getOrderNo(), tickets.size());
        
        return buildResult(order, payment, tickets);
    }
    
    /**
     * 退票
     * 
     * 权限要求:
     * - 用户必须登录
     * - 用户可以退自己的票
     * - 客服人员可以退任何票 (ticket:refund权限)
     */
    @RequiresAuthentication
    public RefundVO refundTicket(String ticketNo, RefundDTO dto) {
        Long userId = SecurityContext.getCurrentUserId();
        
        // 1. 查询票
        Ticket ticket = ticketService.getByTicketNo(ticketNo);
        if (ticket == null) {
            throw BusinessException.of("票不存在");
        }
        
        // 2. 权限检查
        Authentication auth = SecurityContext.getAuthentication();
        boolean hasRefundPermission = hasPermission(auth, "ticket:refund");
        boolean isOwner = ticket.getUserId().equals(userId);
        
        if (!hasRefundPermission && !isOwner) {
            throw BusinessException.forbidden("无权限退票");
        }
        
        // 3. 检查退票条件
        if ("USED".equals(ticket.getStatus())) {
            throw BusinessException.of("票已核销，无法退票");
        }
        
        Showtime showtime = showtimeService.getById(ticket.getShowtimeId());
        long minutesUntilStart = DateUtils.minutesBetween(
            LocalDateTime.now(),
            showtime.getStartTime()
        );
        
        if (minutesUntilStart < 30) {
            throw BusinessException.of("距离开场不足30分钟，无法退票");
        }
        
        // 4. 执行退票
        return ticketService.refund(ticket, dto);
    }
}
```

### 影院管理 (多角色协作)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CinemaManagementService {
    
    /**
     * 创建影院
     * 
     * 权限要求:
     * - CINEMA_ADMIN 或 SUPER_ADMIN 角色
     */
    @RequiresRole(
        value = {"CINEMA_ADMIN", "SUPER_ADMIN"},
        logical = RequiresPermission.Logical.OR
    )
    public CinemaVO createCinema(CreateCinemaDTO dto) {
        Long userId = SecurityContext.getCurrentUserId();
        
        // 创建影院...
        Cinema cinema = new Cinema();
        cinema.setName(dto.getName());
        cinema.setAddress(dto.getAddress());
        cinema.setCreateBy(userId);
        cinema.setCreateTime(DateUtils.nowDateTime());
        
        cinemaRepository.save(cinema);
        
        log.info("影院创建成功: cinemaId={}, name={}, createBy={}",
            cinema.getId(), cinema.getName(), userId);
        
        return toVO(cinema);
    }
    
    /**
     * 创建影厅
     * 
     * 权限要求:
     * - cinema:hall:create 权限
     */
    @RequiresPermission("cinema:hall:create")
    public HallVO createHall(Long cinemaId, CreateHallDTO dto) {
        // 创建影厅...
        return hallVO;
    }
    
    /**
     * 创建场次
     * 
     * 权限要求:
     * - showtime:create 权限
     * - 场次必须属于自己管理的影院 (数据权限)
     */
    @RequiresPermission("showtime:create")
    public ShowtimeVO createShowtime(CreateShowtimeDTO dto) {
        Long userId = SecurityContext.getCurrentUserId();
        
        // 1. 检查影院是否属于当前用户管理
        Cinema cinema = cinemaRepository.findById(dto.getCinemaId())
            .orElseThrow(() -> BusinessException.of("影院不存在"));
        
        // 检查数据权限
        if (!hasDataPermission(userId, cinema)) {
            throw BusinessException.forbidden("无权限在该影院创建场次");
        }
        
        // 2. 创建场次
        Showtime showtime = new Showtime();
        // ...设置属性
        showtimeRepository.save(showtime);
        
        return toVO(showtime);
    }
    
    /**
     * 检查数据权限
     */
    private boolean hasDataPermission(Long userId, Cinema cinema) {
        Authentication auth = SecurityContext.getAuthentication();
        
        // 超级管理员有所有权限
        if (hasRole(auth, "SUPER_ADMIN")) {
            return true;
        }
        
        // 影院管理员只能管理自己的影院
        if (hasRole(auth, "CINEMA_ADMIN")) {
            return cinema.getCreateBy().equals(userId);
        }
        
        return false;
    }
}
```

## 常见问题

### 1. 如何获取当前用户信息？

```java
// 获取用户ID
Long userId = SecurityContext.getCurrentUserId();

// 获取用户名
String username = SecurityContext.getCurrentUsername();

// 获取完整的认证信息
Authentication auth = SecurityContext.getAuthentication();
if (auth instanceof JwtAuthenticationToken) {
    JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) auth;
    UserPrincipal principal = (UserPrincipal) jwtAuth.getPrincipal();
    
    Long userId = principal.getUserId();
    String username = principal.getUsername();
    String role = principal.getRole();
    int vipLevel = principal.getVipLevel();
}
```

### 2. 如何检查用户是否有某个权限？

```java
// 在Service中检查
Authentication auth = SecurityContext.getAuthentication();
boolean hasPermission = auth.getAuthorities().stream()
    .anyMatch(a -> a.getAuthority().equals("order:delete"));

if (!hasPermission) {
    throw BusinessException.forbidden("无权限删除订单");
}

// 或者使用注解
@RequiresPermission("order:delete")
public void deleteOrder(Long orderId) {
    // ...
}
```

### 3. 如何实现"只能操作自己的数据"？

```java
@Service
public class OrderService {
    
    public void deleteOrder(Long orderId) {
        Long userId = SecurityContext.getCurrentUserId();
        
        // 1. 查询订单
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> BusinessException.of("订单不存在"));
        
        // 2. 检查是否是订单所有者
        if (!order.getUserId().equals(userId)) {
            // 检查是否有管理员权限
            Authentication auth = SecurityContext.getAuthentication();
            boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("order:delete:all"));
            
            if (!isAdmin) {
                throw BusinessException.forbidden("只能删除自己的订单");
            }
        }
        
        // 3. 删除订单
        orderRepository.delete(order);
    }
}
```

### 4. 如何实现Token自动刷新？

```java
// 前端拦截器
axios.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401) {
      // Token过期，尝试刷新
      try {
        const refreshToken = localStorage.getItem('refreshToken');
        const response = await axios.post('/api/auth/refresh', {
          refreshToken
        });
        
        // 保存新Token
        localStorage.setItem('accessToken', response.data.data.accessToken);
        
        // 重试原请求
        error.config.headers.Authorization = 'Bearer ' + response.data.data.accessToken;
        return axios(error.config);
        
      } catch (refreshError) {
        // 刷新失败，跳转登录页
        router.push('/login');
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);
```

## 相关文档

- [模块 README](README.md) - 模块功能介绍
- [配置文档](CONFIG.md) - 详细配置说明
- [测试文档](TESTING.md) - 测试指南
- [发展路线图](ROADMAP.md) - 未来规划

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0


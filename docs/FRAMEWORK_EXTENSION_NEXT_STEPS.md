# Nebula 框架扩展后续工作计划

## 文档信息

- **文档版本**: v1.0
- **创建日期**: 2025-11-03
- **优先级分类**: P0(必须)/P1(高)/P2(中)/P3(低)

---

## 总览

本文档详细列出了Nebula框架扩展完成后的后续工作计划，包括测试补充、示例开发、功能增强、性能优化等。

---

## P0: 必须完成 (紧急且重要)

### 1. 编译和基础验证

#### 1.1 Maven构建验证
```bash
# 验证所有模块编译通过
cd /path/to/nebula-projects
mvn clean install -DskipTests

# 预期结果: BUILD SUCCESS
```

**验收标准**:
- ✅ 所有模块编译通过
- ✅ 无编译错误
- ✅ 依赖关系正确

#### 1.2 依赖冲突检查
```bash
# 检查依赖冲突
mvn dependency:tree

# 检查重复依赖
mvn dependency:analyze
```

**验收标准**:
- ✅ 无版本冲突
- ✅ 无循环依赖
- ✅ 无未使用的依赖

---

## P1: 高优先级 (重要)

### 2. 单元测试补充

#### 2.1 messaging延时消息测试

**测试文件**: `nebula-messaging-rabbitmq/src/test/java/`

```java
// DelayMessageProducerTest.java
@SpringBootTest
class DelayMessageProducerTest {
    
    @Autowired
    private DelayMessageProducer producer;
    
    @Test
    void testSendDelayMessage() {
        // 测试发送延时消息
        DelayMessageResult result = producer.send(
            "test.topic",
            "test.queue",
            new TestEvent("test"),
            Duration.ofSeconds(10)
        );
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessageId()).isNotNull();
    }
    
    @Test
    void testBatchSendDelayMessage() {
        // 测试批量发送
    }
}
```

**测试覆盖**:
- [x] 单条延时消息发送
- [x] 批量延时消息发送
- [x] 消息重试机制
- [x] 死信队列处理
- [x] 延时精度测试

**目标覆盖率**: >80%

#### 2.2 Lock模块测试

**测试文件**: `nebula-lock-redis/src/test/java/`

```java
// RedisLockTest.java
@SpringBootTest
@EmbeddedRedis  // 使用嵌入式Redis测试
class RedisLockTest {
    
    @Autowired
    private LockManager lockManager;
    
    @Test
    void testLockAcquisition() {
        Lock lock = lockManager.getLock("test:lock");
        try {
            lock.lock();
            assertThat(lock.isHeldByCurrentThread()).isTrue();
        } finally {
            lock.unlock();
        }
    }
    
    @Test
    void testTryLockTimeout() {
        // 测试超时
    }
    
    @Test
    void testWatchdog() {
        // 测试看门狗续期
    }
    
    @Test
    void testConcurrency() throws Exception {
        // 并发测试:1000个线程同时获取锁
        CountDownLatch latch = new CountDownLatch(1000);
        AtomicInteger counter = new AtomicInteger(0);
        
        for (int i = 0; i < 1000; i++) {
            new Thread(() -> {
                Lock lock = lockManager.getLock("concurrent:lock");
                try {
                    lock.lock();
                    counter.incrementAndGet();
                } finally {
                    lock.unlock();
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await(30, TimeUnit.SECONDS);
        assertThat(counter.get()).isEqualTo(1000);
    }
}
```

**测试覆盖**:
- [x] 基本锁获取和释放
- [x] tryLock超时测试
- [x] 看门狗自动续期
- [x] 并发测试(1000线程)
- [x] 读写锁测试
- [x] @Locked注解测试
- [x] SpEL表达式解析

**目标覆盖率**: >80%

#### 2.3 Security模块测试

**测试文件**: `nebula-security/src/test/java/`

```java
// SecurityAspectTest.java
@SpringBootTest
class SecurityAspectTest {
    
    @Test
    void testRequiresAuthentication() {
        // 未认证时应抛出异常
        assertThrows(SecurityException.class, () -> {
            testService.authenticatedMethod();
        });
        
        // 已认证时应正常执行
        Authentication auth = mockAuthentication();
        SecurityContext.setAuthentication(auth);
        assertDoesNotThrow(() -> {
            testService.authenticatedMethod();
        });
    }
    
    @Test
    void testRequiresPermission() {
        // 测试权限检查
    }
    
    @Test
    void testRequiresRole() {
        // 测试角色检查
    }
}
```

**测试覆盖**:
- [x] @RequiresAuthentication测试
- [x] @RequiresPermission测试(AND/OR逻辑)
- [x] @RequiresRole测试
- [x] SecurityContext测试
- [x] JWT认证流程测试

**目标覆盖率**: >80%

---

### 3. 集成测试

#### 3.1 延时消息集成测试

**测试场景**: 订单超时取消

```java
@SpringBootTest
@TestPropertySource(properties = {
    "nebula.messaging.rabbitmq.host=localhost",
    "nebula.messaging.rabbitmq.port=5672"
})
class OrderTimeoutIntegrationTest {
    
    @Autowired
    private DelayMessageProducer producer;
    
    @Autowired
    private OrderService orderService;
    
    @Test
    void testOrderTimeoutCancel() throws InterruptedException {
        // 创建订单
        Long orderId = orderService.createOrder(...);
        
        // 发送30分钟超时消息
        producer.send(
            "order.timeout",
            "order.timeout.queue",
            new OrderTimeoutEvent(orderId),
            Duration.ofMinutes(30)
        );
        
        // 等待消息处理(实际测试中可以缩短延时时间)
        Thread.sleep(31 * 60 * 1000);
        
        // 验证订单已取消
        Order order = orderService.getById(orderId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
```

#### 3.2 分布式锁集成测试

**测试场景**: 座位锁定

```java
@SpringBootTest
class SeatLockIntegrationTest {
    
    @Autowired
    private SeatService seatService;
    
    @Test
    void testConcurrentSeatLock() throws Exception {
        Long seatId = 1001L;
        int threadCount = 100;
        
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // 100个用户同时锁定同一个座位
        for (int i = 0; i < threadCount; i++) {
            final Long userId = (long) i;
            new Thread(() -> {
                try {
                    boolean locked = seatService.lockSeat(seatId, userId);
                    if (locked) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await();
        
        // 只有1个用户能成功锁定座位
        assertThat(successCount.get()).isEqualTo(1);
    }
}
```

#### 3.3 Security集成测试

**测试场景**: 权限验证流程

```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testAuthenticationFlow() throws Exception {
        // 未登录访问受保护资源,应返回401
        mockMvc.perform(get("/api/user/profile"))
            .andExpect(status().isUnauthorized());
        
        // 登录获取Token
        String token = loginAndGetToken("testuser", "password");
        
        // 携带Token访问,应返回200
        mockMvc.perform(get("/api/user/profile")
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }
    
    @Test
    void testPermissionCheck() throws Exception {
        // 测试权限验证
    }
}
```

---

### 4. Example示例项目

> **示例风格规范**: 所有示例的编写方式请参考 `example/nebula-example/README.md` 现有示例进行修改，保持风格一致。

**示例风格要点**:
1. **代码示例**: 使用完整的、可运行的代码片段，包含必要的注解和注释
2. **测试方式**: 优先使用 `curl` 命令进行接口测试，简单直观
3. **文档结构**: 
   - 概述 -> 测试前准备 -> 测试用例 -> 监控和调试 -> 性能测试 -> 常见问题 -> 总结
4. **命令格式**: 使用 bash 代码块展示 curl 命令，包含完整的 URL 和参数
5. **预期结果**: 每个测试用例都要包含预期的输出结果，便于用户验证
6. **步骤编号**: 使用清晰的层级结构(### 1. / #### 1.1 / 1.1.1)
7. **日志示例**: 提供关键业务节点的日志输出示例
8. **注释说明**: 在代码中添加中文注释，解释关键逻辑和业务流程

**参考文档**:
- `example/nebula-example/README.md` - 总体示例结构
- `example/nebula-example/docs/nebula-*-test.md` - 各功能测试指南
- `example/nebula-example/docs/nebula-delay-message-test.md` - 延时消息示例

---

#### 4.1 延时消息示例

**文件**: `example/nebula-example/src/main/java/.../DelayMessageExample.java`

```java
@RestController
@RequestMapping("/api/example/delay-message")
@RequiredArgsConstructor
public class DelayMessageExample {
    
    private final DelayMessageProducer producer;
    
    /**
     * 示例1: 订单超时取消
     * 
     * GET /api/example/delay-message/order-timeout?orderId=1001
     */
    @GetMapping("/order-timeout")
    public Result<String> orderTimeout(@RequestParam Long orderId) {
        // 发送30分钟延时消息
        DelayMessageResult result = producer.send(
            "order.timeout",
            "order.timeout.queue",
            new OrderTimeoutEvent(orderId, LocalDateTime.now()),
            Duration.ofMinutes(30)
        );
        
        return Result.success("订单超时消息已发送: " + result.getMessageId());
    }
    
    /**
     * 示例2: 优惠券过期提醒
     */
    @GetMapping("/coupon-expire-reminder")
    public Result<String> couponExpireReminder(@RequestParam Long couponId) {
        // 发送3天后的过期提醒
        producer.send(
            "coupon.expire.reminder",
            "coupon.reminder.queue",
            new CouponExpireEvent(couponId),
            Duration.ofDays(3)
        );
        
        return Result.success("优惠券过期提醒已设置");
    }
}

@Component
@Slf4j
class OrderTimeoutHandler {
    
    @Autowired
    private OrderService orderService;
    
    @PostConstruct
    public void init() throws IOException {
        delayMessageConsumer.subscribe(
            "order.timeout.queue",
            OrderTimeoutEvent.class,
            this::handleOrderTimeout
        );
    }
    
    private void handleOrderTimeout(OrderTimeoutEvent event, DelayMessageContext context) {
        log.info("处理订单超时: orderId={}, 延时={}ms", 
            event.getOrderId(), context.getTotalDelay());
        
        // 取消未支付订单
        orderService.cancelUnpaidOrder(event.getOrderId());
    }
}
```

#### 4.2 分布式锁示例

**文件**: `example/nebula-example/src/main/java/.../LockExample.java`

```java
@RestController
@RequestMapping("/api/example/lock")
@RequiredArgsConstructor
public class LockExample {
    
    private final SeatService seatService;
    
    /**
     * 示例1: 座位锁定(使用@Locked注解)
     * 
     * POST /api/example/lock/seat?seatId=1001&userId=1
     */
    @PostMapping("/seat")
    public Result<Boolean> lockSeat(
            @RequestParam Long seatId,
            @RequestParam Long userId) {
        
        boolean locked = seatService.lockSeat(seatId, userId);
        
        return locked ? 
            Result.success(true, "座位锁定成功") : 
            Result.failure("座位已被锁定");
    }
    
    /**
     * 示例2: 库存扣减(防止超卖)
     */
    @PostMapping("/inventory/deduct")
    public Result<Boolean> deductInventory(
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        
        boolean success = inventoryService.deduct(productId, quantity);
        
        return success ?
            Result.success(true, "库存扣减成功") :
            Result.failure("库存不足");
    }
}

@Service
public class SeatService {
    
    /**
     * 锁定座位
     * 使用@Locked注解自动加锁
     */
    @Locked(
        key = "'seat:' + #seatId",
        waitTime = 3,
        leaseTime = 10,
        timeUnit = TimeUnit.SECONDS,
        failStrategy = Locked.FailStrategy.RETURN_FALSE
    )
    public boolean lockSeat(Long seatId, Long userId) {
        // 检查座位状态
        Seat seat = seatRepository.findById(seatId).orElse(null);
        if (seat == null || !seat.isAvailable()) {
            return false;
        }
        
        // 锁定座位
        seat.setStatus(SeatStatus.LOCKED);
        seat.setUserId(userId);
        seat.setLockTime(LocalDateTime.now());
        seatRepository.save(seat);
        
        return true;
    }
}
```

#### 4.3 Security示例

**文件**: `example/nebula-example/src/main/java/.../SecurityExample.java`

```java
@RestController
@RequestMapping("/api/example/auth")
@RequiredArgsConstructor
public class SecurityExample {
    
    private final AuthService authService;
    
    /**
     * 示例1: 登录获取Token
     * 
     * POST /api/example/auth/login
     * Body: {"username": "admin", "password": "123456"}
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        String token = authService.login(request);
        return Result.success(new LoginResponse(token));
    }
    
    /**
     * 示例2: 需要认证的接口
     */
    @RequiresAuthentication
    @GetMapping("/profile")
    public Result<UserProfile> getProfile() {
        Long userId = SecurityContext.getCurrentUserId();
        UserProfile profile = userService.getProfile(userId);
        return Result.success(profile);
    }
    
    /**
     * 示例3: 需要权限的接口
     */
    @RequiresPermission("order:delete")
    @DeleteMapping("/order/{id}")
    public Result<Void> deleteOrder(@PathVariable Long id) {
        orderService.delete(id);
        return Result.success();
    }
    
    /**
     * 示例4: 需要角色的接口
     */
    @RequiresRole("ADMIN")
    @GetMapping("/users")
    public Result<List<User>> getAllUsers() {
        List<User> users = userService.findAll();
        return Result.success(users);
    }
}
```

**文档**: 更新 `example/nebula-example/README.md` 添加示例说明

---

### 5. Autoconfigure整合

#### 5.1 更新nebula-autoconfigure

**文件**: `nebula/autoconfigure/nebula-autoconfigure/pom.xml`

```xml
<!-- 添加新模块依赖 -->
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-lock-redis</artifactId>
    <version>${project.version}</version>
    <optional>true</optional>
</dependency>

<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-security</artifactId>
    <version>${project.version}</version>
    <optional>true</optional>
</dependency>

<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-integration-notification</artifactId>
    <version>${project.version}</version>
    <optional>true</optional>
</dependency>
```

**文件**: `NebulaAutoConfiguration.java`

```java
@Configuration
@Import({
    // 现有配置...
    RedisLockAutoConfiguration.class,
    SecurityAutoConfiguration.class,
    NotificationAutoConfiguration.class
})
public class NebulaAutoConfiguration {
    // ...
}
```

#### 5.2 更新nebula-starter

**文件**: `nebula/starter/nebula-starter/pom.xml`

```xml
<!-- 添加新模块依赖 -->
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-lock-redis</artifactId>
</dependency>

<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-security</artifactId>
</dependency>

<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-integration-notification</artifactId>
</dependency>
```

**文件**: 更新 `nebula-starter/README.md`

---

## P2: 中优先级 (改进)

### 6. 功能增强

#### 6.1 Lock模块增强

**Redlock完整实现**:

```java
// RedlockManager.java
@Component
public class RedlockManager {
    
    private final List<RedissonClient> redissonClients;
    
    /**
     * 获取Redlock
     * 需要在多个Redis实例上同时获取锁
     */
    public Lock getRedLock(String key) {
        RLock[] locks = redissonClients.stream()
            .map(client -> client.getLock(key))
            .toArray(RLock[]::new);
        
        // 使用第一个客户端创建红锁
        RLock redLock = redissonClients.get(0).getRedLock(locks);
        return new RedisLock(redLock, key, LockConfig.defaultConfig());
    }
}
```

**公平锁完整实现**:

```java
// 在LockManager中添加
public Lock getFairLock(String key) {
    RLock fairLock = redissonClient.getFairLock(key);
    return new RedisLock(fairLock, key, LockConfig.defaultConfig());
}
```

#### 6.2 Security模块增强

**RBAC完整实现**:

```java
// Role.java
@Entity
@Table(name = "sys_role")
@Data
public class Role {
    @Id
    private Long id;
    private String roleName;
    private String roleCode;
    private String description;
    
    @ManyToMany
    @JoinTable(
        name = "sys_role_permission",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions;
}

// Permission.java
@Entity
@Table(name = "sys_permission")
@Data
public class Permission {
    @Id
    private Long id;
    private String permissionName;
    private String permissionCode;
    private String resourceType;  // menu/button/api
    private String resourcePath;
}

// UserRole.java
@Entity
@Table(name = "sys_user_role")
@Data
public class UserRole {
    @Id
    private Long id;
    private Long userId;
    private Long roleId;
}

// PermissionService.java
@Service
public class PermissionService {
    
    /**
     * 加载用户权限
     */
    public Set<String> loadUserPermissions(Long userId) {
        // 1. 查询用户角色
        List<Role> roles = roleRepository.findByUserId(userId);
        
        // 2. 查询角色权限
        Set<String> permissions = new HashSet<>();
        for (Role role : roles) {
            permissions.addAll(
                role.getPermissions().stream()
                    .map(Permission::getPermissionCode)
                    .collect(Collectors.toSet())
            );
        }
        
        return permissions;
    }
    
    /**
     * 检查用户是否拥有权限
     */
    public boolean hasPermission(Long userId, String permission) {
        Set<String> permissions = loadUserPermissions(userId);
        return permissions.contains(permission);
    }
}
```

**权限缓存**:

```java
@Service
@RequiredArgsConstructor
public class PermissionCacheService {
    
    private final RedisTemplate<String, Set<String>> redisTemplate;
    private final PermissionService permissionService;
    
    /**
     * 获取用户权限(带缓存)
     */
    public Set<String> getUserPermissions(Long userId) {
        String cacheKey = "user:permissions:" + userId;
        
        // 从缓存获取
        Set<String> permissions = redisTemplate.opsForValue().get(cacheKey);
        if (permissions != null) {
            return permissions;
        }
        
        // 从数据库加载
        permissions = permissionService.loadUserPermissions(userId);
        
        // 写入缓存(30分钟过期)
        redisTemplate.opsForValue().set(
            cacheKey, 
            permissions, 
            Duration.ofMinutes(30)
        );
        
        return permissions;
    }
    
    /**
     * 清除用户权限缓存
     */
    public void clearUserPermissions(Long userId) {
        String cacheKey = "user:permissions:" + userId;
        redisTemplate.delete(cacheKey);
    }
}
```

#### 6.3 Notification模块增强

**阿里云短信实现**:

```java
@Service
@RequiredArgsConstructor
public class AliyunSmsService implements SmsService {
    
    private final NotificationProperties properties;
    private Client client;
    
    @PostConstruct
    public void init() throws Exception {
        Config config = new Config()
            .setAccessKeyId(properties.getSms().getAccessKeyId())
            .setAccessKeySecret(properties.getSms().getAccessKeySecret());
        config.endpoint = "dysmsapi.aliyuncs.com";
        
        client = new Client(config);
    }
    
    @Override
    public boolean send(String phone, String templateCode, String... params) {
        try {
            SendSmsRequest request = new SendSmsRequest()
                .setPhoneNumbers(phone)
                .setSignName(properties.getSms().getSignName())
                .setTemplateCode(templateCode)
                .setTemplateParam(buildTemplateParam(params));
            
            SendSmsResponse response = client.sendSms(request);
            
            return "OK".equals(response.getBody().getCode());
        } catch (Exception e) {
            log.error("发送短信失败: phone={}, template={}", phone, templateCode, e);
            return false;
        }
    }
    
    @Override
    public boolean sendVerificationCode(String phone, String code) {
        return send(phone, "SMS_VERIFICATION_CODE", code);
    }
    
    private String buildTemplateParam(String... params) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < params.length; i++) {
            map.put("param" + (i + 1), params[i]);
        }
        return JSON.toJSONString(map);
    }
}
```

**短信限流器**:

```java
@Component
@RequiredArgsConstructor
public class SmsRateLimiter {
    
    private final RedisTemplate<String, Integer> redisTemplate;
    
    /**
     * 检查是否允许发送短信
     * 
     * 规则:
     * - 同一手机号1分钟内只能发送1次
     * - 同一手机号1小时内只能发送5次
     * - 同一手机号1天内只能发送10次
     */
    public boolean allowSend(String phone) {
        // 1分钟限制
        String key1min = "sms:limit:1min:" + phone;
        if (!checkAndIncrement(key1min, 1, Duration.ofMinutes(1))) {
            log.warn("短信发送频率过快: phone={}", phone);
            return false;
        }
        
        // 1小时限制
        String key1hour = "sms:limit:1hour:" + phone;
        if (!checkAndIncrement(key1hour, 5, Duration.ofHours(1))) {
            log.warn("短信发送超过小时限制: phone={}", phone);
            return false;
        }
        
        // 1天限制
        String key1day = "sms:limit:1day:" + phone;
        if (!checkAndIncrement(key1day, 10, Duration.ofDays(1))) {
            log.warn("短信发送超过日限制: phone={}", phone);
            return false;
        }
        
        return true;
    }
    
    private boolean checkAndIncrement(String key, int maxCount, Duration duration) {
        Integer count = redisTemplate.opsForValue().get(key);
        if (count != null && count >= maxCount) {
            return false;
        }
        
        redisTemplate.opsForValue().increment(key);
        if (count == null) {
            redisTemplate.expire(key, duration);
        }
        
        return true;
    }
}
```

---

### 7. 性能优化

#### 7.1 分布式锁性能测试

**性能指标**:
- QPS目标: >10,000
- 响应时间目标: <100ms
- 并发测试: 1000线程

**性能测试代码**:

```java
@SpringBootTest
class LockPerformanceTest {
    
    @Autowired
    private LockManager lockManager;
    
    @Test
    void testQPS() throws Exception {
        int threadCount = 1000;
        int loopCount = 100;
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < loopCount; j++) {
                        String lockKey = "perf:lock:" + threadIndex + ":" + j;
                        Lock lock = lockManager.getLock(lockKey);
                        lock.lock();
                        try {
                            // 模拟业务处理
                            Thread.sleep(1);
                        } finally {
                            lock.unlock();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }
        
        startLatch.countDown();
        endLatch.await();
        
        long elapsed = System.currentTimeMillis() - startTime;
        int totalOps = threadCount * loopCount * 2; // lock + unlock
        double qps = (double) totalOps / elapsed * 1000;
        
        System.out.printf("性能测试结果: 总操作数=%d, 耗时=%dms, QPS=%.2f%n", 
            totalOps, elapsed, qps);
        
        assertThat(qps).isGreaterThan(10000);
    }
}
```

#### 7.2 延时消息精度测试

**精度测试代码**:

```java
@SpringBootTest
class DelayMessageAccuracyTest {
    
    @Autowired
    private DelayMessageProducer producer;
    
    @Test
    void testDelayAccuracy() throws Exception {
        int sampleCount = 100;
        List<Long> errors = new ArrayList<>();
        
        CountDownLatch latch = new CountDownLatch(sampleCount);
        
        for (int i = 0; i < sampleCount; i++) {
            long sendTime = System.currentTimeMillis();
            long expectedDelay = 10000; // 10秒
            
            producer.send(
                "accuracy.test",
                "accuracy.test.queue",
                new TestEvent(sendTime, expectedDelay),
                Duration.ofMillis(expectedDelay)
            );
        }
        
        // 等待所有消息处理完成
        latch.await(20, TimeUnit.SECONDS);
        
        // 统计误差
        DoubleSummaryStatistics stats = errors.stream()
            .mapToDouble(Long::doubleValue)
            .summaryStatistics();
        
        System.out.printf("延时精度测试: 样本数=%d, 平均误差=%.2fms, 最大误差=%dms%n",
            stats.getCount(), stats.getAverage(), (long) stats.getMax());
        
        // 验证平均误差<100ms
        assertThat(stats.getAverage()).isLessThan(100);
    }
}
```

---

## P3: 低优先级 (优化)

### 8. 高级特性

#### 8.1 数据权限

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataPermission {
    
    /**
     * 数据范围
     */
    DataScope scope() default DataScope.ALL;
    
    /**
     * 用户表别名
     */
    String userAlias() default "u";
    
    /**
     * 部门表别名
     */
    String deptAlias() default "d";
    
    enum DataScope {
        ALL,         // 全部数据
        DEPT,        // 本部门数据
        DEPT_BELOW,  // 本部门及以下
        SELF         // 仅本人
    }
}
```

#### 8.2 消息模板管理

```java
@Entity
public class SmsTemplate {
    @Id
    private Long id;
    private String templateCode;
    private String templateName;
    private String templateContent;
    private List<String> paramNames;
}

@Service
public class SmsTemplateService {
    
    public String renderTemplate(String templateCode, Map<String, String> params) {
        SmsTemplate template = templateRepository.findByCode(templateCode);
        String content = template.getTemplateContent();
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            content = content.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        
        return content;
    }
}
```

---

## 总结

本文档详细列出了Nebula框架扩展的后续工作计划，按优先级分为P0/P1/P2/P3四个级别：

- **P0**: 编译验证、依赖检查(必须完成)
- **P1**: 单元测试、集成测试、Example示例、Autoconfigure整合(高优先级)
- **P2**: 功能增强、性能优化(中优先级)
- **P3**: 高级特性(低优先级)

建议按优先级顺序逐步完成，确保框架的稳定性和可用性。

---

**文档维护**: Nebula 架构团队  
**最后更新**: 2025-11-03  
**文档版本**: v1.0


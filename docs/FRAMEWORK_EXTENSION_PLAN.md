# Nebula 框架扩展计划（重新设计版）

## 文档信息

- **文档版本**: v2.0
- **创建日期**: 2025-11-03
- **更新日期**: 2025-11-03
- **状态**: 待评审

---

## 概述

本文档是基于 ticket-projects 需求分析后，重新梳理的 Nebula 框架扩展计划。与之前的方案不同，本方案更加注重：

1. **与现有架构的融合**：遵循现有框架的分层结构
2. **通用性**：确保新增功能是通用的基础设施，而非业务特定
3. **最小化复杂度**：优先增强现有模块，而非总是新增模块
4. **清晰的边界**：明确哪些是基础设施，哪些是应用层，哪些是集成层

---

## 一、现有框架架构回顾

### 1.1 分层结构

```
nebula/
├── core/                          # 核心层
│   └── nebula-foundation/         # 核心工具（JWT、加密、ID生成等）
│
├── infrastructure/                # 基础设施层
│   ├── data/                      # 数据访问
│   │   ├── nebula-data-persistence
│   │   ├── nebula-data-cache
│   │   └── nebula-data-mongodb
│   ├── messaging/                 # 消息队列
│   │   ├── nebula-messaging-core
│   │   └── nebula-messaging-rabbitmq
│   ├── rpc/                       # RPC通信
│   ├── discovery/                 # 服务发现
│   ├── storage/                   # 对象存储
│   ├── search/                    # 全文搜索
│   └── ai/                        # AI能力
│
├── application/                   # 应用层
│   ├── nebula-web/                # Web功能（限流、监控、脱敏）
│   └── nebula-task/               # 任务调度
│
├── integration/                   # 集成层
│   └── nebula-integration-payment/ # 支付集成
│
├── autoconfigure/                 # 自动配置
└── starter/                       # 启动器
```

### 1.2 设计原则

1. **分层清晰**：core → infrastructure → application → integration
2. **职责单一**：每个模块只负责一个领域
3. **抽象优先**：先定义接口（core），再提供实现
4. **可插拔**：每个模块都是可选的，可以独立使用

---

## 二、问题分析

### 2.1 原方案的问题

原扩展方案提出了 4 个独立模块：
1. nebula-lock-redis（分布式锁）
2. nebula-delay-queue-rabbitmq（延时队列）
3. nebula-auth-rbac（权限管理）
4. nebula-notification-sms（短信通知）

**存在的问题**：
- 模块定位不清：有些应该是基础设施，有些应该是集成层
- 与现有模块关系不明确：是否有重复或冲突
- 通用性不足：有些模块过于业务特定

### 2.2 重新思考的出发点

**什么是基础设施？**
- 技术性的，而非业务性的
- 底层的，被上层依赖的
- 通用的，可以在多个场景复用的
- 稳定的，不经常变化的

**什么应该放在框架中？**
- 核心能力：如安全、锁、缓存
- 技术抽象：如消息队列、RPC
- 第三方集成：如支付、通知

**什么不应该放在框架中？**
- 业务逻辑：如订单处理、座位管理
- 特定场景：如票务、电商

---

## 三、重新设计的扩展方案

### 3.1 扩展概览

| 序号 | 扩展内容 | 类型 | 层级 | 优先级 |
|------|---------|------|------|--------|
| 1 | infrastructure/lock | 新增模块 | 基础设施层 | P0 |
| 2 | messaging/rabbitmq增强 | 模块增强 | 基础设施层 | P0 |
| 3 | core/security | 新增模块 | 核心层 | P1 |
| 4 | integration/notification | 新增模块 | 集成层 | P2 |

---

## 四、详细设计

### 4.1 infrastructure/lock - 分布式锁（新增）

#### 定位
提供分布式环境下的锁机制，解决并发访问共享资源的问题。

#### 为什么放在 infrastructure 层？
- 这是一个技术性的基础设施
- 与 data、messaging 同级，都是分布式系统的基础能力
- 可以被上层（application、业务代码）广泛使用

#### 模块结构

```
infrastructure/lock/
├── nebula-lock-core/
│   ├── pom.xml
│   ├── README.md
│   └── src/main/java/io/nebula/lock/
│       ├── Lock.java                   # 锁接口
│       ├── LockManager.java            # 锁管理器接口
│       ├── LockCallback.java           # 锁回调
│       ├── LockConfig.java             # 锁配置
│       ├── LockException.java          # 锁异常
│       ├── enums/
│       │   ├── LockType.java           # 锁类型（互斥锁、读写锁）
│       │   └── LockMode.java           # 锁模式（公平、非公平）
│       └── annotation/
│           └── Locked.java             # 锁注解
│
└── nebula-lock-redis/
    ├── pom.xml
    ├── README.md
    └── src/main/java/io/nebula/lock/redis/
        ├── RedisLock.java              # Redis锁实现
        ├── RedisLockManager.java       # Redis锁管理器
        ├── RedisLockWatchdog.java      # 看门狗（自动续期）
        ├── RedisLockScript.java        # Lua脚本
        └── config/
            └── RedisLockAutoConfiguration.java
```

#### 核心接口设计

```java
public interface Lock {
    /**
     * 阻塞获取锁
     */
    void lock();
    
    /**
     * 尝试获取锁（非阻塞）
     */
    boolean tryLock();
    
    /**
     * 尝试获取锁（带超时）
     */
    boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException;
    
    /**
     * 释放锁
     */
    void unlock();
    
    /**
     * 检查是否持有锁
     */
    boolean isHeldByCurrentThread();
}

public interface LockManager {
    /**
     * 获取锁实例
     */
    Lock getLock(String key);
    
    /**
     * 获取读写锁
     */
    ReadWriteLock getReadWriteLock(String key);
}
```

#### 使用示例

```java
@Service
public class SeatService {
    
    @Autowired
    private LockManager lockManager;
    
    // 方式1：手动获取锁
    public boolean reserveSeat(Long seatId) {
        Lock lock = lockManager.getLock("seat:" + seatId);
        
        try {
            if (lock.tryLock(10, TimeUnit.SECONDS)) {
                try {
                    // 业务逻辑
                    return true;
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }
    
    // 方式2：使用注解
    @Locked(key = "'seat:' + #seatId", waitTime = 10, leaseTime = 30)
    public boolean reserveSeatWithAnnotation(Long seatId) {
        // 业务逻辑
        return true;
    }
}
```

#### 配置示例

```yaml
nebula:
  lock:
    redis:
      enabled: true
      lease-time: 30000          # 默认锁租期30秒
      wait-time: 10000           # 默认等待时间10秒
      watch-dog-timeout: 30000   # 看门狗超时30秒
      retry-interval: 100        # 重试间隔100ms
```

#### 技术方案
- **实现方式**：Redis SETNX + Lua脚本
- **续期机制**：后台线程定时续期（看门狗）
- **高可用**：支持 Redis Cluster 和 Sentinel
- **防脑裂**：Redlock 算法（可选）

#### 实施计划
- **工期**：2周
- **里程碑**：
  - Week 1：核心接口、Redis实现、自动续期
  - Week 2：注解支持、Redlock算法、测试验证

---

### 4.2 messaging/rabbitmq - 延时消息增强（模块增强）

#### 定位
在现有 RabbitMQ 消息队列基础上，增加延时消息能力。

#### 为什么不新增模块？
- 延时消息是 RabbitMQ 的一个特性，不是独立的概念
- 与消息队列紧密相关，应该作为消息队列的增强
- 避免模块过度拆分，降低框架复杂度

#### 增强内容

在现有 `nebula-messaging-rabbitmq` 模块中增加：

```
messaging/nebula-messaging-rabbitmq/
└── src/main/java/io/nebula/messaging/rabbitmq/
    ├── delay/                          # 新增延时消息包
    │   ├── DelayMessage.java           # 延时消息
    │   ├── DelayMessageProducer.java   # 延时消息生产者
    │   ├── DelayMessageConsumer.java   # 延时消息消费者
    │   └── annotation/
    │       └── DelayMessageListener.java  # 延时消息监听器注解
    └── config/
        └── RabbitDelayMessageConfig.java  # 延时消息配置
```

#### 核心接口设计

```java
public class DelayMessage<T> {
    private String id;
    private T body;
    private long delayTime;         // 延时时间（毫秒）
    private int retryTimes;         // 重试次数
    private Map<String, Object> headers;
}

public interface DelayMessageProducer {
    /**
     * 发送延时消息
     */
    <T> void send(String queue, DelayMessage<T> message);
    
    /**
     * 批量发送延时消息
     */
    <T> void sendBatch(String queue, List<DelayMessage<T>> messages);
}
```

#### 使用示例

```java
@Service
public class OrderService {
    
    @Autowired
    private DelayMessageProducer delayMessageProducer;
    
    // 创建订单时发送延时消息
    public void createOrder(Order order) {
        orderRepository.save(order);
        
        // 15分钟后检查订单状态
        DelayMessage<Long> message = DelayMessage.<Long>builder()
                .body(order.getId())
                .delayTime(15 * 60 * 1000L)  // 15分钟
                .retryTimes(3)
                .build();
                
        delayMessageProducer.send("order.timeout", message);
    }
}

// 监听延时消息
@Component
public class OrderTimeoutListener {
    
    @DelayMessageListener(queue = "order.timeout")
    public void handleOrderTimeout(Long orderId) {
        Order order = orderRepository.findById(orderId);
        if (order.getStatus() == OrderStatus.UNPAID) {
            orderService.cancelOrder(orderId);
        }
    }
}
```

#### 配置示例

```yaml
nebula:
  messaging:
    rabbitmq:
      delay-message:
        enabled: true
        retry-times: 3
        retry-interval: 60000      # 重试间隔60秒
        dead-letter-enabled: true  # 启用死信队列
```

#### 技术方案
- **实现方式**：RabbitMQ死信队列 + TTL
- **原理**：消息发送到延时队列（设置TTL），过期后转发到死信交换机
- **可靠性**：消息持久化、消费者确认、死信队列
- **幂等性**：消息去重机制

#### 实施计划
- **工期**：1周
- **里程碑**：
  - Day 1-3：延时消息实现
  - Day 4-5：重试和死信处理
  - Day 6-7：测试和文档

---

### 4.3 core/security - 安全体系（新增）

#### 定位
提供完整的安全体系，包括认证（Authentication）、授权（Authorization）、用户管理。

#### 为什么放在 core 层？
- 安全是系统的核心能力，不是可选的
- 几乎所有系统都需要安全机制
- 应该作为框架的基础能力提供

#### 与现有 foundation 的关系
- `nebula-foundation` 提供了 JWT 工具类
- `nebula-security` 提供完整的认证授权体系
- security 依赖并复用 foundation 的 JWT 工具

#### 模块结构

```
core/nebula-security/
├── pom.xml
├── README.md
└── src/main/java/io/nebula/security/
    ├── authentication/                 # 认证
    │   ├── Authentication.java         # 认证信息接口
    │   ├── AuthenticationManager.java  # 认证管理器
    │   ├── AuthenticationProvider.java # 认证提供者接口
    │   ├── JwtAuthenticationProvider.java  # JWT实现
    │   ├── OAuth2AuthenticationProvider.java  # OAuth2实现
    │   └── SecurityContext.java        # 安全上下文
    │
    ├── authorization/                  # 授权
    │   ├── AccessControl.java          # 访问控制接口
    │   ├── AccessDecisionManager.java  # 访问决策管理器
    │   ├── rbac/                       # RBAC实现
    │   │   ├── RbacAccessControl.java
    │   │   ├── Role.java
    │   │   ├── Permission.java
    │   │   ├── Resource.java
    │   │   └── RoleRepository.java
    │   └── annotation/
    │       ├── RequiresAuthentication.java
    │       ├── RequiresPermission.java
    │       └── RequiresRole.java
    │
    ├── user/                           # 用户管理
    │   ├── UserDetails.java            # 用户详情接口
    │   ├── UserService.java            # 用户服务接口
    │   └── UserRepository.java         # 用户仓储接口
    │
    └── spring/                         # Spring集成
        ├── SecurityInterceptor.java    # 安全拦截器
        ├── SecurityContextHolder.java  # 上下文持有者
        └── config/
            └── SecurityAutoConfiguration.java
```

#### 核心接口设计

```java
// 认证信息
public interface Authentication {
    Object getPrincipal();              // 主体（通常是用户）
    Object getCredentials();            // 凭证（通常是密码）
    Collection<? extends GrantedAuthority> getAuthorities();  // 权限
    boolean isAuthenticated();
}

// 认证管理器
public interface AuthenticationManager {
    Authentication authenticate(Authentication authentication) 
        throws AuthenticationException;
}

// 访问控制
public interface AccessControl {
    boolean hasPermission(String permission);
    boolean hasRole(String role);
    boolean hasAnyRole(String... roles);
    boolean hasAllRoles(String... roles);
}

// 用户详情
public interface UserDetails {
    Long getUserId();
    String getUsername();
    Collection<? extends Role> getRoles();
    Collection<? extends Permission> getPermissions();
}
```

#### 使用示例

```java
@RestController
public class OrderController {
    
    // 需要认证
    @GetMapping("/orders")
    @RequiresAuthentication
    public Result<List<Order>> getMyOrders() {
        Long userId = SecurityContextHolder.getUserId();
        return Result.success(orderService.getByUserId(userId));
    }
    
    // 需要特定角色
    @PostMapping("/orders/{id}/refund")
    @RequiresRole("admin")
    public Result<Void> refundOrder(@PathVariable Long id) {
        orderService.refund(id);
        return Result.success();
    }
    
    // 需要特定权限
    @GetMapping("/admin/orders")
    @RequiresPermission("order:view:all")
    public Result<List<Order>> getAllOrders() {
        return Result.success(orderService.getAll());
    }
    
    // 编程式权限检查
    @GetMapping("/orders/{id}")
    public Result<Order> getOrder(@PathVariable Long id) {
        Order order = orderService.getById(id);
        
        // 检查是否有权限访问此订单
        if (!order.getUserId().equals(SecurityContextHolder.getUserId())
                && !SecurityContextHolder.hasRole("admin")) {
            throw new AccessDeniedException("无权访问此订单");
        }
        
        return Result.success(order);
    }
}
```

#### 配置示例

```yaml
nebula:
  security:
    enabled: true
    jwt:
      secret: "your-secret-key"
      expiration: 7200              # 2小时
      refresh-expiration: 604800    # 7天
    rbac:
      enabled: true
      cache-enabled: true           # 启用权限缓存
      cache-expiration: 1800        # 缓存30分钟
    anonymous-urls:                 # 匿名访问URL
      - /api/auth/login
      - /api/auth/register
      - /health
```

#### 技术方案
- **认证**：JWT Token + Spring Security（可选）
- **授权**：RBAC（Role-Based Access Control）
- **权限存储**：MySQL + Redis缓存
- **权限校验**：AOP拦截器 + 注解

#### 实施计划
- **工期**：2周
- **里程碑**：
  - Week 1：认证体系（JWT集成）、RBAC模型
  - Week 2：权限注解、Spring集成、测试

---

### 4.4 integration/notification - 通知服务（新增）

#### 定位
提供统一的通知服务抽象，集成多种通知渠道（短信、邮件、推送）。

#### 为什么放在 integration 层？
- 通知是对第三方服务的集成，不是框架的核心能力
- 与 payment 同级，都是可选的第三方服务集成
- 业务系统可以选择性使用

#### 模块结构

```
integration/nebula-integration-notification/
├── pom.xml
├── README.md
└── src/main/java/io/nebula/notification/
    ├── Notification.java               # 通知消息
    ├── NotificationChannel.java        # 通知渠道接口
    ├── NotificationSender.java         # 通知发送器
    ├── NotificationTemplate.java       # 通知模板
    ├── NotificationResult.java         # 发送结果
    │
    ├── sms/                            # 短信通知
    │   ├── SmsChannel.java
    │   ├── SmsTemplate.java
    │   ├── provider/
    │   │   ├── SmsProvider.java
    │   │   ├── AliyunSmsProvider.java
    │   │   └── TencentSmsProvider.java
    │   ├── limiter/
    │   │   └── SmsRateLimiter.java
    │   └── config/
    │       └── SmsConfig.java
    │
    ├── email/                          # 邮件通知
    │   ├── EmailChannel.java
    │   ├── EmailTemplate.java
    │   └── config/
    │       └── EmailConfig.java
    │
    ├── push/                           # 推送通知
    │   ├── PushChannel.java
    │   ├── provider/
    │   │   ├── PushProvider.java
    │   │   ├── JpushProvider.java
    │   │   └── UmengProvider.java
    │   └── config/
    │       └── PushConfig.java
    │
    └── config/
        └── NotificationAutoConfiguration.java
```

#### 核心接口设计

```java
// 通知渠道接口
public interface NotificationChannel {
    String getChannelType();  // SMS, EMAIL, PUSH
    NotificationResult send(Notification notification);
}

// 通知消息
public class Notification {
    private String id;
    private String channelType;         // SMS, EMAIL, PUSH
    private String template;            // 模板ID
    private Map<String, Object> params; // 模板参数
    private List<String> recipients;    // 接收人
}

// 通知发送器
public interface NotificationSender {
    NotificationResult send(Notification notification);
    NotificationResult sendBatch(List<Notification> notifications);
}
```

#### 使用示例

```java
@Service
public class UserService {
    
    @Autowired
    private NotificationSender notificationSender;
    
    // 发送验证码
    public void sendVerifyCode(String phone) {
        String code = RandomStringUtils.randomNumeric(6);
        
        // 缓存验证码
        redisTemplate.opsForValue().set(
            "verify:code:" + phone, code, 5, TimeUnit.MINUTES
        );
        
        // 发送短信
        Notification notification = Notification.builder()
                .channelType("SMS")
                .template("VERIFY_CODE")
                .params(Map.of("code", code))
                .recipients(List.of(phone))
                .build();
                
        notificationSender.send(notification);
    }
    
    // 发送订单通知
    public void sendOrderNotification(Order order) {
        Notification notification = Notification.builder()
                .channelType("SMS")
                .template("ORDER_STATUS")
                .params(Map.of(
                    "orderNo", order.getOrderNo(),
                    "status", order.getStatus()
                ))
                .recipients(List.of(order.getUserPhone()))
                .build();
                
        notificationSender.send(notification);
    }
}
```

#### 配置示例

```yaml
nebula:
  notification:
    sms:
      enabled: true
      provider: aliyun              # aliyun, tencent
      aliyun:
        access-key-id: "your-key"
        access-key-secret: "your-secret"
        sign-name: "票务平台"
        templates:
          VERIFY_CODE: "SMS_123456"
          ORDER_STATUS: "SMS_123457"
      rate-limit:
        enabled: true
        per-phone: 5                # 每个手机号每天5条
        per-ip: 10                  # 每个IP每天10条
    
    email:
      enabled: true
      host: "smtp.gmail.com"
      port: 587
      username: "your-email@gmail.com"
      password: "your-password"
    
    push:
      enabled: false
      provider: jpush              # jpush, umeng
```

#### 技术方案
- **短信**：集成阿里云、腾讯云SMS SDK
- **邮件**：Spring Mail
- **推送**：集成极光、友盟SDK
- **限流**：Redis + 令牌桶算法
- **模板**：数据库管理

#### 实施计划
- **工期**：1周
- **里程碑**：
  - Day 1-3：短信渠道实现
  - Day 4-5：邮件和推送渠道
  - Day 6-7：限流和测试

---

## 五、实施计划

### 5.1 总体时间表

| 阶段 | 模块 | 类型 | 工期 | 依赖 |
|------|------|------|------|------|
| 第1周 | messaging/rabbitmq增强 | 模块增强 | 1周 | nebula-messaging-rabbitmq |
| 第2-3周 | infrastructure/lock | 新增模块 | 2周 | nebula-data-cache（Redis） |
| 第4-5周 | core/security | 新增模块 | 2周 | nebula-foundation（JWT） |
| 第6周 | integration/notification | 新增模块 | 1周 | - |

**总计：6周（1.5个月）**

### 5.2 优先级

- **P0（立即实施）**：lock, messaging增强
  - 解决高并发座位锁定和订单超时问题
  
- **P1（短期实施）**：security
  - 完善安全体系，支持多角色权限控制
  
- **P2（中期实施）**：notification
  - 可选模块，提升用户体验

---

## 六、与原方案的对比

### 6.1 模块对比

| 原方案 | 新方案 | 变化 |
|--------|--------|------|
| nebula-lock-redis | infrastructure/lock | 位置更合理，结构更清晰 |
| nebula-delay-queue-rabbitmq | messaging增强 | 不新增模块，降低复杂度 |
| nebula-auth-rbac | core/security | 扩展为完整的安全体系 |
| nebula-notification-sms | integration/notification | 扩展为统一的通知服务 |

### 6.2 优势

1. **更符合现有架构**：遵循现有分层结构
2. **通用性更强**：每个模块都是通用的基础能力
3. **复杂度更低**：优先增强现有模块，减少新增模块数量
4. **职责更清晰**：明确每个模块的定位和边界

---

## 七、验收标准

### 7.1 功能标准
- [ ] 分布式锁支持多种场景（座位锁定、库存扣减等）
- [ ] 延时消息准确可靠（误差<1秒）
- [ ] 安全体系完整（认证、授权、用户管理）
- [ ] 通知服务支持多渠道（短信、邮件、推送）

### 7.2 性能标准
- [ ] 分布式锁响应时间 <100ms
- [ ] 分布式锁 QPS >10000
- [ ] 权限校验响应时间 <5ms
- [ ] 短信发送成功率 >99%

### 7.3 质量标准
- [ ] 所有模块有完整文档
- [ ] 所有模块有使用示例
- [ ] 单元测试覆盖率 >80%
- [ ] 与现有模块无冲突

---

## 八、总结

本方案是基于 ticket-projects 需求分析和现有框架架构深入思考后的结果，主要特点：

1. **遵循现有架构**：新增模块都按照现有分层结构组织
2. **提升通用性**：确保每个模块都是通用的基础能力
3. **降低复杂度**：优先增强现有模块（如 messaging），而非总是新增
4. **清晰的边界**：明确哪些是核心（core）、基础设施（infrastructure）、集成（integration）

**最终扩展清单**：
- 2个新增模块：infrastructure/lock, core/security
- 1个现有模块增强：messaging/rabbitmq
- 1个可选集成模块：integration/notification

这个方案更加合理，既满足了 ticket-projects 的需求，又保持了框架的通用性和可扩展性。

---

**文档维护**: Nebula 架构团队  
**最后更新**: 2025-11-03  
**文档版本**: v2.0（重新设计版）


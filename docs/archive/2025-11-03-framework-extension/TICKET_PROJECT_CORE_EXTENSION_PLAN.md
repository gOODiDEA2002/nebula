# Nebula 框架核心扩展计划 - 支持票务运营平台核心功能

## 文档信息

- **文档版本**: v1.0
- **创建日期**: 2025-11-03
- **目标项目**: ticket-projects（票务运营平台）核心功能
- **评估状态**: 已完成

---

## 概述

本文档是 Nebula 框架扩展计划的精简版，聚焦于支持票务运营平台的核心业务功能。通过分析 ticket-projects 的核心业务流程和技术挑战，我们识别出 4 个必须立即实现的核心模块。

### 票务系统核心业务流程

**用户购票流程**：
```
浏览影片 → 选择影院 → 选择场次 → 选座 → 下单 → 支付 → 出票
```

**影院管理流程**：
```
排期管理 → 座位管理 → 价格管理
```

### 核心技术挑战

票务运营平台的核心技术挑战包括：

1. **高并发座位锁定**
   - 场景：秒杀级购票场景，多人同时抢购同一座位
   - 挑战：防止超卖、保证数据一致性
   - 解决方案：分布式锁

2. **订单超时处理**
   - 场景：用户下单后15分钟未支付
   - 挑战：自动取消订单、释放座位资源
   - 解决方案：延时队列

3. **权限控制**
   - 场景：用户、影院工作人员、管理员多角色
   - 挑战：细粒度权限控制、数据安全
   - 解决方案：RBAC权限模型

4. **短信通知**
   - 场景：注册验证码、订单状态通知
   - 挑战：多渠道集成、防刷限流
   - 解决方案：统一通知服务

### Nebula 框架现有能力

Nebula 框架已经具备了大部分基础能力：

- 数据访问：MyBatis-Plus、读写分离、分库分表
- 缓存管理：多级缓存
- 消息队列：RabbitMQ集成
- RPC通信：HTTP、gRPC
- 服务发现：Nacos
- 对象存储：MinIO
- 全文搜索：Elasticsearch
- 支付集成：支付宝、微信支付
- Web功能：健康检查、性能监控、限流

### 需要扩展的内容

为了支持票务系统的核心业务，需要新增 4 个核心模块：

1. nebula-lock-redis（分布式锁）
2. nebula-delay-queue-rabbitmq（延时队列）
3. nebula-auth-rbac（权限管理）
4. nebula-notification-sms（短信通知）

以及 2 个现有模块的关键增强。

---

## 一、核心扩展模块

### 1.1 分布式锁模块

#### 模块名称
`nebula-infrastructure-lock / nebula-lock-redis`

#### 业务价值
解决高并发座位抢购问题，防止超卖，保证数据一致性。

#### 核心功能
1. **座位锁定**
   ```java
   String lockKey = "seat:lock:" + cinemaId + ":" + hallId + ":" + seatId;
   if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
       try {
           // 检查座位状态
           if (seatService.isAvailable(seatId)) {
               // 锁定座位
               seatService.lockSeat(seatId, userId);
               // 创建订单
               orderService.createOrder(...);
               return true;
           }
       } finally {
           lock.unlock();
       }
   }
   ```

2. **库存锁定**
   ```java
   String lockKey = "product:stock:" + productId;
   if (lock.tryLock()) {
       try {
           // 扣减库存
           if (stockService.decrease(productId, quantity)) {
               // 创建订单
               orderService.createOrder(...);
           }
       } finally {
           lock.unlock();
       }
   }
   ```

#### 技术方案
- **实现**: Redis SETNX + Lua脚本
- **续期**: 看门狗机制（每10秒续期一次）
- **高可用**: 支持Redis Cluster
- **防脑裂**: Redlock算法

#### 配置示例
```yaml
nebula:
  lock:
    redis:
      enabled: true
      lease-time: 30000      # 锁租期30秒
      watch-dog-timeout: 30000  # 看门狗超时
```

#### 实施计划
- **工期**: 2周
- **里程碑**:
  - Week 1: 基础锁实现、自动续期
  - Week 2: Redlock算法、测试验证

#### 验收标准
- [ ] 并发测试通过（1000线程）
- [ ] 性能测试通过（>10000 QPS）
- [ ] 锁自动续期正常
- [ ] Redlock防止脑裂

---

### 1.2 延时队列模块

#### 模块名称
`nebula-infrastructure-delay-queue / nebula-delay-queue-rabbitmq`

#### 业务价值
处理订单超时自动取消，释放座位资源，提升用户体验。

#### 核心功能
1. **订单超时取消**
   ```java
   // 创建订单时发送延时消息
   public void createOrder(Order order) {
       orderRepository.save(order);
       
       // 15分钟后检查订单状态
       DelayMessage message = DelayMessage.builder()
           .body(order.getId())
           .delayTime(15 * 60 * 1000L)
           .build();
       delayQueueProducer.send("order.timeout", message);
   }
   
   // 监听延时消息
   @DelayQueueListener(queue = "order.timeout")
   public void handleOrderTimeout(Long orderId) {
       Order order = orderRepository.findById(orderId);
       if (order.getStatus() == OrderStatus.UNPAID) {
           // 取消订单
           orderService.cancelOrder(orderId);
           // 释放座位
           seatService.releaseSeat(order.getSeatId());
       }
   }
   ```

2. **优惠券过期处理**
   ```java
   // 发送延时消息
   DelayMessage message = DelayMessage.builder()
       .body(couponId)
       .delayTime(expireTime - System.currentTimeMillis())
       .build();
   delayQueueProducer.send("coupon.expire", message);
   ```

#### 技术方案
- **实现**: RabbitMQ死信队列 + TTL
- **可靠性**: 消息持久化、确认机制
- **重试**: 3次重试，失败进入死信队列

#### 配置示例
```yaml
nebula:
  delay-queue:
    rabbitmq:
      enabled: true
      retry-times: 3
      retry-interval: 60000
```

#### 实施计划
- **工期**: 1周
- **里程碑**:
  - Day 1-3: RabbitMQ延时队列实现
  - Day 4-5: 重试和死信处理
  - Day 6-7: 测试和文档

#### 验收标准
- [ ] 延时准确（误差<1秒）
- [ ] 消息不丢失
- [ ] 重试机制正常
- [ ] 性能测试通过（>1000 msg/s）

---

### 1.3 权限管理模块

#### 模块名称
`nebula-infrastructure-auth / nebula-auth-rbac`

#### 业务价值
保护系统资源，支持多角色权限控制，保证数据安全。

#### 核心功能
1. **角色定义**
   - user: 普通用户（购票、查看订单）
   - cinema_staff: 影院工作人员（检票、查看排期）
   - cinema_manager: 影院经理（排期管理、价格管理）
   - operation: 运营人员（营销活动、数据分析）
   - admin: 系统管理员（全部权限）

2. **权限控制**
   ```java
   @RestController
   public class OrderController {
       
       // 用户查看自己的订单
       @GetMapping("/orders/{id}")
       @RequiresAuthentication
       public Result<Order> getOrder(@PathVariable Long id) {
           // 验证订单归属
           Order order = orderService.getById(id);
           if (!order.getUserId().equals(getCurrentUserId())) {
               throw new AccessDeniedException("无权访问此订单");
           }
           return Result.success(order);
       }
       
       // 影院工作人员检票
       @PostMapping("/tickets/{id}/check")
       @RequiresRole("cinema_staff")
       public Result<Void> checkTicket(@PathVariable Long id) {
           ticketService.checkTicket(id);
           return Result.success();
       }
       
       // 影院经理管理排期
       @PostMapping("/schedules")
       @RequiresPermission("schedule:manage")
       public Result<Schedule> createSchedule(@RequestBody CreateScheduleRequest request) {
           return Result.success(scheduleService.create(request));
       }
   }
   ```

3. **数据权限**
   ```java
   // 影院经理只能查看自己影院的数据
   @DataPermission(entity = "Cinema", field = "cinemaId")
   public List<Order> getOrders(Long cinemaId) {
       return orderRepository.findByCinemaId(cinemaId);
   }
   ```

#### 技术方案
- **认证**: JWT Token + Spring Security
- **权限模型**: RBAC（Role-Based Access Control）
- **权限存储**: MySQL + Redis缓存
- **权限校验**: AOP拦截器

#### 配置示例
```yaml
nebula:
  auth:
    jwt:
      secret: "your-secret-key"
      expiration: 7200
    rbac:
      enabled: true
      cache-enabled: true
```

#### 实施计划
- **工期**: 2周
- **里程碑**:
  - Week 1: JWT认证、RBAC模型
  - Week 2: 权限注解、数据权限

#### 验收标准
- [ ] JWT认证正常
- [ ] RBAC权限校验准确
- [ ] 权限注解生效
- [ ] 性能测试通过（<5ms）

---

### 1.4 短信通知模块

#### 模块名称
`nebula-infrastructure-notification / nebula-notification-sms`

#### 业务价值
支持用户注册验证码、订单状态通知，提升用户体验，形成业务闭环。

#### 核心功能
1. **验证码短信**
   ```java
   @Service
   public class UserService {
       
       @Autowired
       private SmsSender smsSender;
       
       // 发送验证码
       public void sendVerifyCode(String phone) {
           // 生成6位验证码
           String code = RandomStringUtils.randomNumeric(6);
           
           // 缓存5分钟
           redisTemplate.opsForValue().set(
               "verify:code:" + phone,
               code,
               5,
               TimeUnit.MINUTES
           );
           
           // 发送短信
           SmsTemplate template = SmsTemplate.builder()
               .templateCode("SMS_VERIFY_CODE")
               .params(Map.of("code", code))
               .build();
           smsSender.send(phone, template);
       }
       
       // 验证验证码
       public boolean verifyCode(String phone, String code) {
           String cached = (String) redisTemplate.opsForValue()
               .get("verify:code:" + phone);
           return code.equals(cached);
       }
   }
   ```

2. **订单状态通知**
   ```java
   // 订单支付成功通知
   public void notifyOrderPaid(Order order) {
       SmsTemplate template = SmsTemplate.builder()
           .templateCode("SMS_ORDER_PAID")
           .params(Map.of(
               "orderNo", order.getOrderNo(),
               "movieName", order.getMovieName(),
               "cinemaName", order.getCinemaName(),
               "showTime", order.getShowTime(),
               "seatNo", order.getSeatNo()
           ))
           .build();
       smsSender.send(order.getUserPhone(), template);
   }
   ```

3. **防刷限流**
   ```java
   // 每个手机号每天最多发送5条验证码
   @RateLimit(key = "'sms:' + #phone", limit = 5, period = 86400)
   public void sendVerifyCode(String phone) {
       // 发送验证码逻辑
   }
   ```

#### 技术方案
- **服务商**: 阿里云短信、腾讯云短信
- **限流**: Redis + 令牌桶算法
- **验证码**: Redis缓存，5分钟过期
- **模板**: 数据库管理

#### 配置示例
```yaml
nebula:
  notification:
    sms:
      provider: aliyun
      aliyun:
        access-key-id: "your-key"
        access-key-secret: "your-secret"
        sign-name: "票务平台"
      rate-limit:
        per-phone: 5
        per-ip: 10
```

#### 实施计划
- **工期**: 1周
- **里程碑**:
  - Day 1-3: 阿里云短信集成
  - Day 4-5: 限流和防刷
  - Day 6-7: 测试和文档

#### 验收标准
- [ ] 短信发送成功率 >99%
- [ ] 验证码正常工作
- [ ] 限流机制生效
- [ ] 模板管理正常

---

## 二、现有模块关键增强

### 2.1 缓存模块增强 - 分布式限流器

**模块**: `nebula-data-cache`

**需求**: 防止抢票刷单、恶意请求

**实现**:
```java
@Service
public class TicketService {
    
    // IP级别限流：每秒最多10次请求
    @RateLimit(key = "'ip:' + #request.remoteAddr", limit = 10, period = 1)
    public void reserveSeat(HttpServletRequest request, Long seatId) {
        // 座位预订逻辑
    }
    
    // 用户级别限流：每分钟最多购买5张票
    @RateLimit(key = "'user:' + #userId", limit = 5, period = 60)
    public void createOrder(Long userId, List<Long> seatIds) {
        // 创建订单逻辑
    }
}
```

**工期**: 3天

---

### 2.2 数据持久化模块增强 - 慢SQL监控

**模块**: `nebula-data-persistence`

**需求**: 保证数据库查询性能

**实现**:
```java
// 自动捕获慢SQL（>1秒）
@Component
public class SlowSqlInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = invocation.proceed();
        long cost = System.currentTimeMillis() - start;
        
        if (cost > 1000) {
            // 记录慢SQL
            log.warn("慢SQL: {} ms, SQL: {}", cost, sql);
            // 发送告警
            alertService.sendAlert("慢SQL告警", sql);
        }
        
        return result;
    }
}
```

**工期**: 2天

---

## 三、实施计划

### 总体时间表

| 阶段 | 模块 | 工期 | 依赖 |
|------|------|------|------|
| 第1周 | nebula-delay-queue-rabbitmq | 1周 | nebula-messaging-rabbitmq |
| 第2-3周 | nebula-lock-redis | 2周 | nebula-data-cache |
| 第4-5周 | nebula-auth-rbac | 2周 | nebula-data-persistence |
| 第6周 | nebula-notification-sms | 1周 | nebula-data-cache |
| 第7周 | 现有模块增强 + 集成测试 | 1周 | 全部模块 |

**总计: 6-7周（1.5个月）**

### 里程碑

**Week 2 - 延时队列完成**
- 订单超时自动取消功能可用

**Week 4 - 分布式锁完成**
- 高并发座位锁定功能可用

**Week 6 - 权限管理完成**
- 多角色权限控制功能可用

**Week 7 - 短信通知完成**
- 验证码和订单通知功能可用

**Week 8 - 集成验收**
- 完整购票流程测试通过

---

## 四、技术风险和应对

### 4.1 分布式锁风险
- **风险**: 锁超时导致重复操作
- **应对**: 看门狗自动续期、Redlock算法

### 4.2 延时队列风险
- **风险**: 消息丢失或延时不准确
- **应对**: 消息持久化、幂等性设计

### 4.3 权限系统风险
- **风险**: 权限逻辑复杂导致性能问题
- **应对**: 多级缓存、权限预加载

### 4.4 短信通知风险
- **风险**: 短信被恶意刷取
- **应对**: 多层限流（IP级、用户级、接口级）

---

## 五、验收标准

### 5.1 功能标准
- [ ] 支持完整购票流程
- [ ] 高并发座位锁定正常工作
- [ ] 订单超时自动取消
- [ ] 多角色权限控制生效
- [ ] 短信验证码和通知正常

### 5.2 性能标准
- [ ] 座位锁定响应时间 <100ms
- [ ] 订单超时处理准确（误差<1秒）
- [ ] 权限校验响应时间 <5ms
- [ ] 短信发送成功率 >99%
- [ ] 系统QPS >5000（初期目标）

### 5.3 质量标准
- [ ] 所有模块有完整文档
- [ ] 所有模块有使用示例
- [ ] 单元测试覆盖率 >80%
- [ ] 集成测试通过

---

## 六、与完整扩展计划的对比

### 精简前（完整版）
- **新增模块**: 8个
- **模块增强**: 8个
- **总工期**: 10-16周（2.5-4个月）
- **覆盖范围**: 全部业务场景

### 精简后（核心版）
- **新增模块**: 4个
- **模块增强**: 2个
- **总工期**: 6-7周（1.5个月）
- **覆盖范围**: 核心购票流程

### 延后的功能
以下功能可以在后续迭代中实现：

1. **P1功能（3-6个月内）**:
   - nebula-rule-engine（规则引擎）- 支持复杂业务规则
   - nebula-user-tag（用户标签）- 支持精准营销

2. **P2功能（6-12个月内）**:
   - nebula-realtime（实时数据）- 监控大屏
   - nebula-tenant（多租户）- 支持多影院

---

## 七、后续演进

### 第二阶段：业务增强（3-6个月）
- 规则引擎：动态定价、优惠券规则
- 用户标签：用户画像、精准营销
- 商城系统：卖品销售

### 第三阶段：智能化（6-12个月）
- 实时数据：监控大屏、实时统计
- AI推荐：智能座位推荐、影片推荐
- 大数据分析：用户行为分析

---

## 八、总结

本精简版扩展计划聚焦于票务运营平台的核心业务流程（购票流程），通过实现 4 个核心模块来解决 4 个核心技术挑战：

1. **分布式锁** → 解决高并发座位锁定
2. **延时队列** → 解决订单超时处理
3. **权限管理** → 解决多角色权限控制
4. **短信通知** → 解决验证码和通知

相比完整版扩展计划，精简版具有以下优势：
- **时间缩短40%**：从10-16周缩短到6-7周
- **聚焦核心**：只实现核心业务必需功能
- **快速交付**：1.5个月即可上线核心功能
- **迭代演进**：其他功能可以后续迭代

这样可以让票务运营平台更快地启动开发，尽早验证业务模式，然后根据实际需求再进行功能扩展。

---

**文档维护**: Nebula 架构团队  
**最后更新**: 2025-11-03  
**文档版本**: v1.0（核心版）


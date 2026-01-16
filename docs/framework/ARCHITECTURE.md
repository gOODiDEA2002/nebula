# Nebula 框架架构设计

> 深入了解 Nebula 框架的架构设计和核心组件

## 架构概述

### 总体架构

Nebula 框架采用分层模块化架构，每一层职责清晰，相互独立又紧密协作：

```
┌─────────────────────────────────────────────────────────┐
│                    应用层 (Application)                   │
│  ┌──────────────┐            ┌──────────────┐          │
│  │  nebula-web  │            │  nebula-task │          │
│  │  Web框架支持  │            │  任务调度     │          │
│  └──────────────┘            └──────────────┘          │
├─────────────────────────────────────────────────────────┤
│                 基础设施层 (Infrastructure)                │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐         │
│  │  Data  │ │Messaging│ │  RPC   │ │Discovery│        │
│  └────────┘ └────────┘ └────────┘ └────────┘         │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐         │
│  │Storage │ │ Search │ │   AI   │ │  Lock  │         │
│  └────────┘ └────────┘ └────────┘ └────────┘         │
├─────────────────────────────────────────────────────────┤
│                   核心层 (Core)                           │
│  ┌──────────────────┐  ┌──────────────────┐          │
│  │nebula-foundation │  │ nebula-security  │          │
│  │  基础工具和异常   │  │ 安全认证和权限    │          │
│  └──────────────────┘  └──────────────────┘          │
├─────────────────────────────────────────────────────────┤
│              自动配置层 (Auto-Configuration)              │
│              nebula-autoconfigure                       │
├─────────────────────────────────────────────────────────┤
│                  Starter 层 (Starters)                   │
│  nebula-starter-minimal / web / service / ai / all     │
└─────────────────────────────────────────────────────────┘
                          ↓
              ┌───────────────────────┐
              │   Spring Boot 3.x      │
              │   Java 21              │
              └───────────────────────┘
```

### 设计原则

#### 1. 分层架构

**核心层**：
- 提供最基础的功能
- 无业务逻辑
- 被其他层依赖

**基础设施层**：
- 提供各种基础能力
- 可独立使用
- 模块间低耦合

**应用层**：
- 面向应用场景
- 集成多个基础设施模块
- 提供完整解决方案

#### 2. 模块化

每个模块遵循：
- **单一职责**：一个模块只做一件事
- **高内聚**：模块内部功能紧密相关
- **低耦合**：模块间依赖最小化
- **可替换**：相同接口的不同实现可替换

#### 3. 面向接口

所有核心功能都定义接口：
```java
// 定义接口
public interface CacheService {
    void set(String key, Object value, int ttl);
    <T> T get(String key, Class<T> type);
}

// 多种实现
public class RedisCacheService implements CacheService { }
public class CaffeineCacheService implements CacheService { }
```

## 核心层设计

### nebula-foundation

**职责**：提供框架的基础工具和异常处理

**核心组件**：

#### 1. 异常体系

```java
NebulaException (基础异常)
    ├─ ValidationException (验证异常)
    ├─ BusinessException (业务异常)
    ├─ SystemException (系统异常)
    └─ ThirdPartyException (第三方异常)
```

#### 2. 通用工具类

- `StringUtils`: 字符串工具
- `DateUtils`: 日期工具
- `JsonUtils`: JSON 工具
- `IdGenerator`: ID 生成器

#### 3. 结果封装

```java
public class Result<T> {
    private boolean success;
    private String code;
    private String message;
    private T data;
}
```

### nebula-security

**职责**：提供安全认证和权限控制

**核心组件**：

#### 1. JWT 认证

```
用户登录
    ↓
生成 JWT Token
    ↓
Token 存入 Redis (可选)
    ↓
后续请求携带 Token
    ↓
验证 Token
    ↓
获取用户信息
```

#### 2. RBAC 权限控制

```
用户 (User)
    ↓ 拥有
角色 (Role)
    ↓ 拥有
权限 (Permission)
    ↓ 控制
资源 (Resource)
```

#### 3. 权限注解

```java
@PreAuthorize("hasRole('ADMIN')")
public void adminOperation() { }

@PreAuthorize("hasPermission('order:create')")
public Order createOrder() { }
```

## 基础设施层设计

### 数据访问 (Data)

#### 架构设计

```
┌─────────────────────────────────────────┐
│          Application Layer               │
│   ┌──────────┐  ┌──────────┐           │
│   │ Service  │  │Repository│           │
│   └─────┬────┘  └─────┬────┘           │
│         │             │                 │
│         ↓             ↓                 │
├─────────────────────────────────────────┤
│       Data Access Layer (Nebula)        │
│   ┌──────────────────────────────┐     │
│   │    Data Access Abstraction    │     │
│   │  ┌────────┐  ┌────────┐      │     │
│   │  │Persistence│ │ Cache │      │     │
│   │  └────────┘  └────────┘      │     │
│   └───────┬──────────┬────────────┘     │
│           │          │                  │
├───────────┼──────────┼──────────────────┤
│           ↓          ↓                  │
│   ┌────────┐    ┌─────────┐           │
│   │ MySQL  │    │  Redis  │  MongoDB  │
│   └────────┘    └─────────┘           │
└─────────────────────────────────────────┘
```

#### nebula-data-persistence

**特性**：
- MyBatis-Plus 集成
- 读写分离
- 分库分表
- 多数据源

**核心组件**：

```java
// 1. 基础 Mapper
public interface BaseMapper<T> extends 
    com.baomidou.mybatisplus.core.mapper.BaseMapper<T> {
    // 扩展方法
}

// 2. 基础 Service
public interface BaseService<T> extends 
    IService<T> {
    // 扩展方法
}

// 3. 数据源配置
@Configuration
public class DataSourceConfig {
    @Bean
    public DataSource masterDataSource() { }
    
    @Bean
    public DataSource slaveDataSource() { }
}
```

#### nebula-data-cache

**多级缓存架构**：

```
请求
  ↓
L1: Caffeine 本地缓存 (1-10ms)
  ↓ 未命中
L2: Redis 分布式缓存 (10-50ms)
  ↓ 未命中
L3: 数据库 (50-200ms)
  ↓
响应
```

**核心接口**：

```java
public interface CacheService {
    // 基础操作
    void set(String key, Object value, int ttl);
    <T> T get(String key, Class<T> type);
    void delete(String key);
    
    // 批量操作
    void multiSet(Map<String, Object> map, int ttl);
    <T> Map<String, T> multiGet(List<String> keys, Class<T> type);
    
    // 高级操作
    <T> T getOrLoad(String key, Supplier<T> loader, int ttl);
}
```

**票务场景应用**：

```java
// 票务信息缓存（热点数据）
@Cacheable(value = "ticket", key = "#id")
public Ticket getTicketById(Long id) {
    return ticketMapper.selectById(id);
}

// 座位状态缓存（实时数据）
String key = "showtime:seats:" + showtimeId;
List<Seat> seats = cacheService.get(key, List.class);
if (seats == null) {
    seats = seatService.getAvailableSeats(showtimeId);
    cacheService.set(key, seats, 10); // 10秒过期
}
```

### 消息传递 (Messaging)

#### 架构设计

```
┌──────────────────────────────────────────┐
│         Application Layer                 │
│   ┌────────────┐    ┌─────────────┐     │
│   │  Producer  │    │  Consumer   │     │
│   └──────┬─────┘    └──────┬──────┘     │
│          │                 │             │
├──────────┼─────────────────┼─────────────┤
│          ↓                 ↓             │
│   ┌─────────────────────────────────┐   │
│   │  Messaging Abstraction Layer    │   │
│   │  (nebula-messaging-core)        │   │
│   └─────────────┬───────────────────┘   │
│                 │                        │
├─────────────────┼────────────────────────┤
│                 ↓                        │
│   ┌────────────────────────┐           │
│   │  RabbitMQ / Kafka      │           │
│   └────────────────────────┘           │
└──────────────────────────────────────────┘
```

**核心接口**：

```java
// 消息生产者
public interface MessageProducer {
    void send(String topic, Object message);
    void sendAsync(String topic, Object message);
    void sendDelayed(String topic, Object message, long delayMs);
}

// 消息消费者
@MessageHandler(topic = "order.created")
public void handleOrderCreated(OrderMessage message) {
    // 处理订单创建消息
}
```

**票务场景应用**：

```java
// 1. 订单创建后发送消息
Order order = orderService.create(orderDto);
messageProducer.send("order.created", order);

// 2. 异步处理订单
@MessageHandler(topic = "order.created")
public void handleOrderCreated(Order order) {
    // 生成电子票
    ticketService.generateETicket(order);
    
    // 发送通知
    notificationService.sendOrderNotification(order);
}

// 3. 延时取消订单
messageProducer.sendDelayed("order.timeout", orderId, 15 * 60 * 1000); // 15分钟后
```

### RPC 通信 (RPC)

#### 架构设计

```
┌──────────────────────────────────────────┐
│         Application Layer                 │
│   ┌────────────┐    ┌─────────────┐     │
│   │  RPC Client │    │ RPC Server  │     │
│   └──────┬─────┘    └──────┬──────┘     │
│          │                 │             │
├──────────┼─────────────────┼─────────────┤
│          ↓                 ↓             │
│   ┌─────────────────────────────────┐   │
│   │    RPC Abstraction Layer        │   │
│   │    (nebula-rpc-core)            │   │
│   └─────┬───────────────┬───────────┘   │
│         │               │                │
│         ↓               ↓                │
│   ┌──────────┐    ┌──────────┐         │
│   │   HTTP   │    │   gRPC   │         │
│   └──────────┘    └──────────┘         │
└──────────────────────────────────────────┘
```

**协议选择**：

| 场景 | 推荐协议 | 原因 |
|-----|---------|------|
| 内部服务通信 | gRPC | 高性能、强类型 |
| 外部API | HTTP | 通用、易集成 |
| 跨语言 | HTTP或gRPC | 都支持多语言 |

**票务场景应用**：

```java
// gRPC 服务定义
service UserService {
    rpc GetUser(UserIdRequest) returns (UserResponse);
}

// gRPC 客户端调用
@GrpcClient("user-service")
private UserServiceStub userService;

public User getUserInfo(Long userId) {
    UserIdRequest request = UserIdRequest.newBuilder()
        .setUserId(userId)
        .build();
    UserResponse response = userService.getUser(request);
    return convert(response);
}
```

### 服务发现 (Discovery)

#### 架构设计

```
┌────────────────────────────────────────┐
│         Service Instances               │
│  ┌──────┐  ┌──────┐  ┌──────┐        │
│  │Service│  │Service│  │Service│        │
│  │  A   │  │  B   │  │  C   │        │
│  └──┬───┘  └──┬───┘  └──┬───┘        │
│     │  注册   │  注册   │  注册        │
├─────┼────────┼────────┼───────────────┤
│     ↓        ↓        ↓               │
│  ┌────────────────────────────┐      │
│  │    Nacos Server            │      │
│  │  - 服务注册                 │      │
│  │  - 服务发现                 │      │
│  │  - 健康检查                 │      │
│  │  - 配置管理                 │      │
│  └────────────────────────────┘      │
└────────────────────────────────────────┘
```

**核心功能**：
- 服务注册
- 服务发现
- 健康检查
- 负载均衡
- 配置管理

**票务场景应用**：

```yaml
# 服务注册
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        service: order-service
        
# 服务调用
@GrpcClient("user-service")  # 自动通过 Nacos 发现服务
private UserServiceStub userService;
```

### 分布式锁 (Lock)

#### 架构设计

```
┌────────────────────────────────────────┐
│         Application Layer               │
│   @DistributedLock(key = "#orderId")   │
│   public void createOrder(orderId) {}  │
│                  ↓                      │
├─────────────────┼───────────────────────┤
│                 ↓                       │
│   ┌─────────────────────────────────┐  │
│   │    Lock Abstraction Layer       │  │
│   │  ┌──────────────────────────┐   │  │
│   │  │  - 锁获取/释放           │   │  │
│   │  │  - 自动续期             │   │  │
│   │  │  - 死锁检测             │   │  │
│   │  └──────────────────────────┘   │  │
│   └──────────────┬──────────────────┘  │
│                  │                      │
├──────────────────┼──────────────────────┤
│                  ↓                      │
│   ┌────────────────────────┐          │
│   │   Redis (Redisson)     │          │
│   └────────────────────────┘          │
└────────────────────────────────────────┘
```

**核心特性**：
- 可重入锁
- 自动续期
- 公平/非公平锁
- 读写锁

**票务场景应用**：

```java
// 防止超卖的关键代码
@Service
public class OrderService {
    
    @DistributedLock(
        key = "'ticket:stock:' + #showtimeId",
        leaseTime = 30,
        waitTime = 3
    )
    public Order createOrder(String showtimeId, List<String> seatIds) {
        // 1. 检查库存
        int availableStock = ticketService.getAvailableStock(showtimeId);
        if (availableStock < seatIds.size()) {
            throw new BusinessException("库存不足");
        }
        
        // 2. 扣减库存
        ticketService.deductStock(showtimeId, seatIds.size());
        
        // 3. 创建订单
        Order order = new Order();
        order.setShowtimeId(showtimeId);
        order.setSeatCount(seatIds.size());
        orderMapper.insert(order);
        
        return order;
    }
}
```

## 应用层设计

### nebula-web

**职责**：提供 Web 应用支持

**核心功能**：
- RESTful API 支持
- 统一返回格式
- 全局异常处理
- 参数验证
- Swagger 文档

**架构**：

```
HTTP 请求
    ↓
Controller (接口层)
    ↓
Service (业务层)
    ↓
Repository (数据层)
    ↓
数据库/缓存
```

**票务场景应用**：

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @PostMapping
    public Result<Order> createOrder(@RequestBody @Valid OrderCreateDto dto) {
        Order order = orderService.createOrder(dto);
        return Result.success(order);
    }
    
    @GetMapping("/{id}")
    public Result<Order> getOrder(@PathVariable Long id) {
        Order order = orderService.getById(id);
        return Result.success(order);
    }
}
```

### nebula-task

**职责**：提供定时任务支持

**集成方案**：
- XXL-Job 集成
- 分布式任务调度
- 任务监控
- 失败重试

**票务场景应用**：

```java
@Component
public class OrderTimeoutJob {
    
    @XxlJob("orderTimeoutHandler")
    public void handleTimeout() {
        // 1. 查询超时未支付订单
        List<Order> timeoutOrders = orderService.findTimeoutOrders();
        
        // 2. 取消订单
        for (Order order : timeoutOrders) {
            orderService.cancelOrder(order.getId());
        }
        
        // 3. 恢复库存
        for (Order order : timeoutOrders) {
            ticketService.restoreStock(order.getShowtimeId(), order.getSeatCount());
        }
    }
}
```

## 自动配置设计

### nebula-autoconfigure

**职责**：统一管理所有模块的自动配置

**核心机制**：

```java
@Configuration
@ConditionalOnProperty(prefix = "nebula.data.cache", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public CacheService cacheService(CacheProperties properties) {
        // 根据配置创建缓存服务
        return new CompositeCacheService(properties);
    }
}
```

**配置优先级**：
1. 外部配置文件
2. 环境变量
3. 默认配置

## Starter 设计

### Starter 模块

| Starter | 包含模块 | 适用场景 |
|---------|---------|---------|
| nebula-starter-minimal | foundation | 最小化应用 |
| nebula-starter-web | foundation, security, web | Web 应用 |
| nebula-starter-service | foundation, security, data, messaging, rpc, discovery | 微服务 |
| nebula-starter-ai | foundation, ai-spring | AI 应用 |
| nebula-starter-all | 所有模块 | 单体应用 |

### 使用方式

```xml
<!-- 只需引入一个 Starter -->
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-service</artifactId>
</dependency>

<!-- 自动包含所有必需的模块 -->
```

## 性能优化设计

### 1. 连接池优化

```yaml
# 数据库连接池
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 30000

# Redis 连接池
spring:
  redis:
    lettuce:
      pool:
        max-active: 100
        max-idle: 20
```

### 2. 缓存策略

- **热点数据**：永久缓存 + 定期刷新
- **频繁访问**：1-2小时过期
- **一般数据**：15-30分钟过期
- **实时数据**：10-60秒过期

### 3. 异步处理

```java
// 同步操作 -> 异步优化
// Before
order = orderService.create(dto);
ticketService.generateETicket(order);
notificationService.send(order);

// After
order = orderService.create(dto);
messageProducer.sendAsync("order.created", order); // 异步处理
```

### 4. 批量操作

```java
// 逐个插入 -> 批量插入
// Before
for (Order order : orders) {
    orderMapper.insert(order);
}

// After
orderService.saveBatch(orders); // 批量插入
```

## 监控和可观测性

### 监控指标

```
┌──────────────────────────────────┐
│         Metrics                   │
│  - QPS/TPS                       │
│  - 响应时间                       │
│  - 错误率                         │
│  - 资源使用                       │
└──────────────────────────────────┘
         ↓
┌──────────────────────────────────┐
│      Prometheus                   │
└──────────────────────────────────┘
         ↓
┌──────────────────────────────────┐
│       Grafana                     │
└──────────────────────────────────┘
```

### 链路追踪

```
请求 → Gateway → UserService → OrderService → PaymentService
         |           |              |                |
         ↓           ↓              ↓                ↓
              Jaeger / Zipkin (链路追踪)
```

## 安全设计

### 1. 认证安全

- JWT Token 加密
- Token 过期机制
- Refresh Token 机制
- 多设备登录管理

### 2. 数据安全

- 敏感数据加密存储
- 传输层 TLS 加密
- SQL 注入防护
- XSS 防护

### 3. 接口安全

- 限流保护
- 防重放攻击
- 签名验证
- IP 白名单

## 相关文档

- [框架概览](OVERVIEW.md) - 框架整体介绍
- [快速开始](QUICK_START.md) - 快速开始指南
- [Starter 选择指南](../STARTER_SELECTION_GUIDE.md) - 模块选择指南
- [最佳实践](../Nebula框架使用指南.md#最佳实践) - 开发最佳实践

---

**Nebula 团队**  
**最后更新**: 2025-11-20  
**文档版本**: v1.0


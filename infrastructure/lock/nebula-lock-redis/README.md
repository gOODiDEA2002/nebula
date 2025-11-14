# Nebula Lock Redis 模块

## 模块简介

`nebula-lock-redis` 是 Nebula 框架基于Redis的分布式锁实现模块，底层使用Redisson提供高性能、高可靠的分布式锁功能。

## 功能特性

核心功能
- 可重入锁：同一线程可以多次获取同一把锁
- 公平锁：按照请求锁的顺序获取锁(FIFO)
- 读写锁：支持多个读锁，但写锁互斥
- 看门狗机制：自动续期，防止业务执行时间过长导致锁自动释放
- 红锁(Redlock)：多Redis实例容错

增强特性
- 注解式锁：`@Locked`注解，声明式编程
- SpEL表达式支持：动态生成锁key
- 灵活的失败策略：异常/返回null/返回false/跳过
- 锁回调机制：自动处理锁的获取和释放
- 完善的异常处理

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-lock-redis</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置Redis和锁

```yaml
# Redis配置
spring:
  redis:
    host: localhost
    port: 6379
    password: 
    database: 0

# Nebula锁配置
nebula:
  lock:
    enabled: true                          # 是否启用分布式锁
    enable-aspect: true                    # 是否启用@Locked注解切面
    default-wait-time: 30s                 # 默认等待锁的超时时间
    default-lease-time: 60s                # 默认锁的租约时间
    enable-watchdog: true                  # 是否启用看门狗机制
    fair: false                            # 是否启用公平锁
    
    # Redlock配置(可选)
    redlock:
      enabled: false
      addresses:
        - redis://127.0.0.1:6379
        - redis://127.0.0.1:6380
        - redis://127.0.0.1:6381
      quorum: 2                            # 最小获取锁的实例数
```

## 使用示例

### 1. 编程式使用

```java
@Service
@RequiredArgsConstructor
public class SeatService {
    
    private final LockManager lockManager;
    
    /**
     * 锁定座位
     */
    public boolean lockSeat(Long seatId) {
        Lock lock = lockManager.getLock("seat:" + seatId);
        try {
            // 获取锁
            lock.lock();
            
            // 业务逻辑：检查座位状态、锁定座位
            if (seat.isAvailable()) {
                seat.lock();
                return true;
            }
            return false;
        } finally {
            // 释放锁
            lock.unlock();
        }
    }
    
    /**
     * 尝试锁定座位(超时)
     */
    public boolean tryLockSeat(Long seatId, Duration timeout) {
        Lock lock = lockManager.getLock("seat:" + seatId);
        try {
            // 尝试获取锁，最多等待timeout时间
            if (lock.tryLock(timeout)) {
                try {
                    // 业务逻辑
                    return seat.lock();
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("获取座位锁超时: seatId={}", seatId);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取座位锁被中断: seatId={}", seatId, e);
            return false;
        }
    }
}
```

### 2. 回调方式

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final RedisLockManager lockManager;
    
    /**
     * 创建订单(使用锁回调)
     */
    public Order createOrder(CreateOrderRequest request) {
        return lockManager.execute(
            "order:create:" + request.getUserId(),
            () -> {
                // 业务逻辑，自动处理锁的获取和释放
                Order order = new Order();
                // ...
                return orderRepository.save(order);
            }
        );
    }
    
    /**
     * 尝试创建订单(使用tryExecute)
     */
    public Order tryCreateOrder(CreateOrderRequest request) {
        return lockManager.tryExecute(
            "order:create:" + request.getUserId(),
            LockConfig.tryLockConfig(),
            () -> {
                // 如果获取锁失败，直接返回null
                return orderRepository.save(order);
            }
        );
    }
}
```

### 3. 注解方式(推荐)

```java
@Service
public class InventoryService {
    
    /**
     * 固定key
     */
    @Locked(key = "inventory:deduct", waitTime = 10, leaseTime = 60)
    public boolean deductInventory(Long productId, int quantity) {
        // 业务逻辑
        return inventoryRepository.deduct(productId, quantity);
    }
    
    /**
     * 动态key(SpEL表达式)
     */
    @Locked(
        key = "'product:' + #productId",
        waitTime = 5,
        leaseTime = 30,
        lockType = LockType.REENTRANT,
        watchdog = true,
        failStrategy = Locked.FailStrategy.RETURN_FALSE,
        failMessage = "'库存扣减失败：商品' + #productId + '已被锁定'"
    )
    public boolean deductProductInventory(Long productId, int quantity) {
        // 业务逻辑
        return true;
    }
    
    /**
     * 复杂表达式
     */
    @Locked(
        key = "#user.id + ':seat:' + #seat.id",
        waitTime = 3,
        leaseTime = 10,
        timeUnit = TimeUnit.SECONDS
    )
    public boolean lockSeatForUser(User user, Seat seat) {
        // 业务逻辑
        return seatRepository.lock(seat.getId(), user.getId());
    }
    
    /**
     * 失败返回false
     */
    @Locked(
        key = "'order:' + #orderId",
        waitTime = 1,
        failStrategy = Locked.FailStrategy.RETURN_FALSE
    )
    public boolean processOrder(Long orderId) {
        // 如果获取锁失败,直接返回false,不抛出异常
        return orderService.process(orderId);
    }
    
    /**
     * 失败跳过锁
     */
    @Locked(
        key = "'data:sync'",
        waitTime = 0,
        failStrategy = Locked.FailStrategy.SKIP
    )
    public void syncData() {
        // 如果获取锁失败,跳过锁,直接执行方法
        // 适用于非关键性的定时任务
        dataService.sync();
    }
}
```

### 4. 读写锁

```java
@Service
@RequiredArgsConstructor
public class CacheService {
    
    private final LockManager lockManager;
    private final Map<String, Data> cache = new ConcurrentHashMap<>();
    
    /**
     * 读取缓存
     */
    public Data read(String key) {
        ReadWriteLock rwLock = lockManager.getReadWriteLock("cache:" + key);
        Lock readLock = rwLock.readLock();
        
        try {
            readLock.lock();
            return cache.get(key);
        } finally {
            readLock.unlock();
        }
    }
    
    /**
     * 写入缓存
     */
    public void write(String key, Data data) {
        ReadWriteLock rwLock = lockManager.getReadWriteLock("cache:" + key);
        Lock writeLock = rwLock.writeLock();
        
        try {
            writeLock.lock();
            cache.put(key, data);
        } finally {
            writeLock.unlock();
        }
    }
}
```

### 5. 红锁(Redlock)

```java
@Service
public class HighAvailabilityService {
    
    @Autowired
    private RedisLockManager lockManager;
    
    @Autowired
    @Qualifier("redisson1")
    private RedissonClient redisson1;
    
    @Autowired
    @Qualifier("redisson2")
    private RedissonClient redisson2;
    
    @Autowired
    @Qualifier("redisson3")
    private RedissonClient redisson3;
    
    /**
     * 使用红锁,防止单点故障
     */
    public void criticalOperation() {
        Lock redlock = lockManager.getRedLock(
            "critical:operation",
            redisson1, redisson2, redisson3
        );
        
        try {
            redlock.lock();
            // 关键业务逻辑
        } finally {
            redlock.unlock();
        }
    }
}
```

## 高级特性

### 1. 看门狗机制

看门狗会自动续期锁，防止业务执行时间过长导致锁自动释放。

```java
LockConfig config = LockConfig.builder()
    .leaseTime(Duration.ofSeconds(30))    // 锁租约30秒
    .enableWatchdog(true)                 // 启用看门狗
    .watchdogInterval(Duration.ofSeconds(10))  // 每10秒续期一次
    .build();

Lock lock = lockManager.getLock("task:key", config);
try {
    lock.lock();
    // 即使业务逻辑执行超过30秒,锁也不会自动释放
    longRunningTask();
} finally {
    lock.unlock();
}
```

### 2. SpEL表达式

`@Locked`注解的key支持SpEL表达式，可以动态生成锁key：

```java
// 访问方法参数
@Locked(key = "'order:' + #orderId")
public void processOrder(Long orderId) { ... }

// 访问对象属性
@Locked(key = "#user.id + ':cart'")
public void addToCart(User user, Product product) { ... }

// 复杂表达式
@Locked(key = "#user.id + ':order:' + #order.id + ':' + #order.type")
public void createOrder(User user, Order order) { ... }

// 使用运算符
@Locked(key = "'seat:' + (#seatId / 100)")
public void lockSeat(Long seatId) { ... }
```

### 3. 失败策略

`@Locked`注解支持多种失败策略：

```java
// 1. THROW_EXCEPTION: 抛出异常(默认)
@Locked(key = "key1", failStrategy = Locked.FailStrategy.THROW_EXCEPTION)
public void method1() { ... }

// 2. RETURN_NULL: 返回null
@Locked(key = "key2", failStrategy = Locked.FailStrategy.RETURN_NULL)
public Order method2() { ... }

// 3. RETURN_FALSE: 返回false(仅适用于boolean返回值)
@Locked(key = "key3", failStrategy = Locked.FailStrategy.RETURN_FALSE)
public boolean method3() { ... }

// 4. SKIP: 跳过锁,直接执行方法
@Locked(key = "key4", failStrategy = Locked.FailStrategy.SKIP)
public void method4() { ... }
```

### 4. 自定义失败消息

```java
@Locked(
    key = "'product:' + #productId",
    failStrategy = Locked.FailStrategy.THROW_EXCEPTION,
    failMessage = "'无法锁定商品: ' + #productId + ', 商品可能正在被其他用户操作'"
)
public void updateProduct(Long productId) { ... }
```

## 性能优化

### 1. 锁粒度

锁的粒度要适中：

```java
// 不推荐：粒度过大，所有订单操作都串行化
@Locked(key = "order:lock")
public void processOrder(Order order) { ... }

// 推荐：合适的粒度，只锁定特定订单
@Locked(key = "'order:' + #order.id")
public void processOrder(Order order) { ... }

// 推荐：更细粒度，针对特定操作
@Locked(key = "'order:' + #order.id + ':payment'")
public void processPayment(Order order) { ... }
```

### 2. 超时设置

合理设置等待超时和租约时间：

```java
// 快速失败场景：不等待，立即返回
@Locked(key = "key", waitTime = 0, failStrategy = Locked.FailStrategy.RETURN_FALSE)
public boolean tryProcess() { ... }

// 短时任务：10秒租约足够
@Locked(key = "key", leaseTime = 10)
public void shortTask() { ... }

// 长时任务：使用看门狗自动续期
@Locked(key = "key", leaseTime = 60, watchdog = true)
public void longTask() { ... }
```

### 3. 读写分离

读多写少的场景使用读写锁：

```java
// 多个线程可以同时读
public Data getData() {
    ReadWriteLock rwLock = lockManager.getReadWriteLock("data:key");
    Lock readLock = rwLock.readLock();
    readLock.lock();
    try {
        return cache.get("key");
    } finally {
        readLock.unlock();
    }
}

// 写操作互斥
public void updateData(Data data) {
    ReadWriteLock rwLock = lockManager.getReadWriteLock("data:key");
    Lock writeLock = rwLock.writeLock();
    writeLock.lock();
    try {
        cache.put("key", data);
    } finally {
        writeLock.unlock();
    }
}
```

## 使用场景

### 1. 座位锁定(高并发)

```java
@Locked(
    key = "'seat:' + #seatId",
    waitTime = 3,
    leaseTime = 10,
    failStrategy = Locked.FailStrategy.RETURN_FALSE
)
public boolean lockSeat(Long seatId, Long userId) {
    return seatRepository.lock(seatId, userId);
}
```

### 2. 库存扣减(防止超卖)

```java
@Locked(
    key = "'inventory:' + #productId",
    waitTime = 5,
    leaseTime = 30
)
public boolean deductInventory(Long productId, int quantity) {
    return inventoryService.deduct(productId, quantity);
}
```

### 3. 订单支付(防止重复支付)

```java
@Locked(
    key = "'order:pay:' + #orderNo",
    waitTime = 10,
    leaseTime = 60
)
public PaymentResult pay(String orderNo, PaymentRequest request) {
    return paymentService.process(orderNo, request);
}
```

### 4. 定时任务(防止重复执行)

```java
@Scheduled(cron = "0 */5 * * * *")
@Locked(
    key = "scheduled:data-sync",
    waitTime = 0,
    failStrategy = Locked.FailStrategy.SKIP
)
public void syncData() {
    // 如果获取锁失败，跳过本次执行
    dataService.sync();
}
```

## 监控和调试

### 1. 日志级别

```yaml
logging:
  level:
    io.nebula.lock: DEBUG
```

### 2. 关键日志

- 成功获取锁：`成功获取锁: key=xxx, thread=xxx`
- 尝试获取锁超时：`尝试获取锁超时: key=xxx, waitTime=xxx`
- 释放锁：`释放锁: key=xxx, thread=xxx`
- SpEL解析：`解析锁key: expression=xxx, result=xxx`

### 3. Redis监控

```bash
# 查看所有锁key
redis-cli KEYS "*lock*"

# 查看锁的TTL
redis-cli TTL "your:lock:key"

# 查看锁的值
redis-cli GET "your:lock:key"
```

## 注意事项

1. **确保finally中释放锁**：避免死锁
2. **租约时间设置合理**：略大于业务执行时间
3. **启用看门狗**：长时间任务必须启用
4. **避免嵌套锁**：容易死锁
5. **合理的锁粒度**：平衡并发性能和数据一致性

## 故障排查

### 1. 获取锁超时

- 检查锁的waitTime设置
- 检查业务执行时间是否过长
- 检查是否有死锁
- 查看Redis连接是否正常

### 2. 锁自动释放

- 检查leaseTime设置是否足够
- 检查是否启用了看门狗
- 查看业务执行时间

### 3. 性能问题

- 检查锁粒度是否过大
- 考虑使用读写锁
- 检查Redis性能

## 依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-lock-core</artifactId>
</dependency>

<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
</dependency>
```

## 许可证

本项目基于 Apache 2.0 许可证开源


## 🧪 测试

本模块提供完整的单元测试文档和示例，详见 [TESTING.md](./TESTING.md)


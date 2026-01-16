# Nebula Lock Core 模块

## 模块简介

`nebula-lock-core` 是 Nebula 框架的分布式锁核心模块，定义了分布式锁的统一接口和抽象。

该模块提供：
- 统一的分布式锁接口定义
- 多种锁类型支持（可重入锁、读写锁、红锁等）
- 灵活的锁配置
- 基于注解的声明式锁
- 锁回调机制

## 核心接口

### Lock - 分布式锁接口

定义了分布式锁的基本操作：

```java
public interface Lock {
    void lock();                                    // 获取锁
    void lockInterruptibly();                       // 获取锁(可中断)
    boolean tryLock();                              // 尝试获取锁
    boolean tryLock(long timeout, TimeUnit unit);   // 尝试获取锁(超时)
    void unlock();                                  // 释放锁
    boolean isHeldByCurrentThread();                // 当前线程是否持有锁
    boolean isLocked();                             // 锁是否被持有
}
```

### LockManager - 锁管理器

负责创建和管理锁实例：

```java
public interface LockManager {
    Lock getLock(String key);                       // 获取锁
    Lock getLock(String key, LockConfig config);    // 获取锁(带配置)
    ReadWriteLock getReadWriteLock(String key);     // 获取读写锁
    void releaseLock(String key);                   // 释放锁
}
```

### ReadWriteLock - 读写锁

支持读写分离的锁：

```java
public interface ReadWriteLock {
    Lock readLock();    // 获取读锁
    Lock writeLock();   // 获取写锁
}
```

## 锁类型

### LockType枚举

- **REENTRANT**: 可重入锁，同一线程可以多次获取同一把锁
- **FAIR**: 公平锁，按照请求锁的顺序获取锁(FIFO)
- **READ_WRITE**: 读写锁，支持多个读锁，但写锁互斥
- **REDLOCK**: 红锁，多个Redis实例同时获取锁，防止单点故障
- **SEMAPHORE**: 信号量，限制同时访问资源的线程数

### LockMode枚举

- **EXCLUSIVE**: 独占模式，同一时间只有一个线程可以持有锁
- **SHARED**: 共享模式，多个线程可以同时持有锁(如读锁)

## 锁配置

### LockConfig配置类

```java
LockConfig config = LockConfig.builder()
    .waitTime(Duration.ofSeconds(30))      // 等待锁的超时时间
    .leaseTime(Duration.ofSeconds(60))     // 锁的租约时间
    .lockType(LockType.REENTRANT)          // 锁类型
    .lockMode(LockMode.EXCLUSIVE)          // 锁模式
    .enableWatchdog(true)                  // 启用看门狗机制
    .fair(false)                           // 是否公平锁
    .build();
```

### 预定义配置

```java
LockConfig.defaultConfig();        // 默认配置(30s等待,60s租约)
LockConfig.tryLockConfig();        // 快速失败配置(不等待)
LockConfig.shortLeaseConfig();     // 短时锁配置(10s租约)
LockConfig.longLeaseConfig();      // 长时锁配置(5分钟租约)
```

## 使用方式

### 1. 编程式使用

```java
@Service
@RequiredArgsConstructor
public class SeatService {
    
    private final LockManager lockManager;
    
    public boolean lockSeat(Long seatId) {
        Lock lock = lockManager.getLock("seat:" + seatId);
        try {
            lock.lock();
            // 执行业务逻辑
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    public boolean tryLockSeat(Long seatId) {
        Lock lock = lockManager.getLock("seat:" + seatId);
        if (lock.tryLock()) {
            try {
                // 执行业务逻辑
                return true;
            } finally {
                lock.unlock();
            }
        }
        return false;
    }
}
```

### 2. 回调方式

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final LockManager lockManager;
    
    public Order createOrder(CreateOrderRequest request) {
        return lockManager.execute("order:create:" + request.getUserId(), () -> {
            // 业务逻辑,自动处理锁的获取和释放
            return orderRepository.save(order);
        });
    }
}
```

### 3. 注解方式(推荐)

```java
@Service
public class InventoryService {
    
    /**
     * 使用固定key
     */
    @Locked(key = "inventory:deduct", waitTime = 10, leaseTime = 60)
    public boolean deductInventory(Long productId, int quantity) {
        // 业务逻辑
        return true;
    }
    
    /**
     * 使用动态key(SpEL表达式)
     */
    @Locked(
        key = "'product:' + #productId",
        waitTime = 5,
        leaseTime = 30,
        lockType = LockType.REENTRANT,
        watchdog = true,
        failStrategy = Locked.FailStrategy.RETURN_FALSE
    )
    public boolean deductProductInventory(Long productId, int quantity) {
        // 业务逻辑
        return true;
    }
    
    /**
     * 使用复杂表达式
     */
    @Locked(key = "#user.id + ':order:' + #order.id")
    public Order createUserOrder(User user, Order order) {
        // 业务逻辑
        return order;
    }
}
```

### 4. 读写锁

```java
@Service
@RequiredArgsConstructor
public class CacheService {
    
    private final LockManager lockManager;
    
    public Data read(String key) {
        ReadWriteLock rwLock = lockManager.getReadWriteLock("cache:" + key);
        Lock readLock = rwLock.readLock();
        try {
            readLock.lock();
            // 读取数据
            return cache.get(key);
        } finally {
            readLock.unlock();
        }
    }
    
    public void write(String key, Data data) {
        ReadWriteLock rwLock = lockManager.getReadWriteLock("cache:" + key);
        Lock writeLock = rwLock.writeLock();
        try {
            writeLock.lock();
            // 写入数据
            cache.put(key, data);
        } finally {
            writeLock.unlock();
        }
    }
}
```

## 异常处理

### 异常体系

- **LockException**: 锁异常基类
- **LockAcquisitionException**: 锁获取异常
- **LockReleaseException**: 锁释放异常

### 异常处理示例

```java
try {
    lock.lock();
    // 业务逻辑
} catch (LockAcquisitionException e) {
    log.error("获取锁失败: {}", e.getMessage());
    // 处理获取锁失败的情况
} catch (LockReleaseException e) {
    log.error("释放锁失败: {}", e.getMessage());
    // 处理释放锁失败的情况
} finally {
    // 确保锁被释放
}
```

## 设计思路

### 1. 接口/实现分离

核心模块只定义接口,具体实现由各个实现模块提供(如`nebula-lock-redis`)。
这样可以：
- 支持多种锁实现(Redis、Zookeeper、数据库等)
- 方便切换锁实现
- 降低耦合度

### 2. 配置灵活性

通过`LockConfig`类提供丰富的配置选项：
- 超时时间
- 租约时间
- 锁类型
- 看门狗机制
- 公平性

### 3. 多种使用方式

支持三种使用方式：
- **编程式**: 灵活控制，适合复杂场景
- **回调式**: 简化代码，自动处理锁的获取和释放
- **注解式**: 声明式编程，适合简单场景

### 4. 看门狗机制

自动续期机制，防止业务执行时间过长导致锁自动释放。
续期间隔默认为租约时间的1/3。

### 5. SpEL表达式支持

`@Locked`注解的key支持SpEL表达式，可以动态生成锁的key：
- 访问方法参数: `#paramName`
- 访问对象属性: `#user.id`
- 使用运算符: `'prefix:' + #id`

## 最佳实践

### 1. 锁的粒度

锁的粒度要适中：
- 粒度过大：并发性能差
- 粒度过小：锁开销大

示例：
```java
// 不推荐：粒度过大
@Locked(key = "global:lock")
public void processOrder(Order order) { ... }

// 推荐：合适的粒度
@Locked(key = "'order:' + #order.id")
public void processOrder(Order order) { ... }
```

### 2. 租约时间设置

租约时间应该略大于业务执行时间：
- 租约过短：业务未完成锁就释放了
- 租约过长：锁持有时间过长，影响并发

建议：
- 评估业务执行时间
- 租约时间 = 业务时间 * 1.5
- 启用看门狗机制

### 3. 异常处理

始终在finally块中释放锁：
```java
Lock lock = lockManager.getLock(key);
try {
    lock.lock();
    // 业务逻辑
} finally {
    lock.unlock();  // 确保锁被释放
}
```

### 4. 避免死锁

- 按照固定顺序获取多个锁
- 使用超时机制(tryLock)
- 避免在持有锁的情况下进行阻塞操作

### 5. 使用读写锁优化

读多写少的场景使用读写锁：
```java
// 多个线程可以同时读
ReadWriteLock rwLock = lockManager.getReadWriteLock(key);
Lock readLock = rwLock.readLock();

// 写锁互斥
Lock writeLock = rwLock.writeLock();
```

## 依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-lock-core</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

## 相关模块

- `nebula-lock-redis`: 基于Redis的分布式锁实现
- `nebula-autoconfigure`: 自动配置支持

## 许可证

本项目基于 MIT 许可证开源


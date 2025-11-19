# Nebula Lock Redis 使用示例

> **核心特性**：基于 Redis 实现的分布式锁，支持注解式调用 (`@Locked`) 和编程式调用，内置看门狗（Watchdog）自动续期机制。

## 1. 快速开始 (Quick Start)

### 引入依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-lock-redis</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

## 2. 配置示例 (Configuration)

```yaml
nebula:
  lock:
    enabled: true
    default-wait-time: 30s
    default-lease-time: 60s
    enable-watchdog: true # 启用看门狗自动续期
```

## 3. 代码示例 (Code Examples)

### 场景 1：注解式锁 (推荐)

最简单的使用方式，支持 SpEL 表达式动态生成 Key。

```java
@Service
public class OrderService {

    /**
     * 防止同一订单重复支付
     * key: 锁的键，支持 SpEL，如 #orderId
     * waitTime: 等待获取锁的时间 (秒)
     * leaseTime: 锁持有的最长时间 (秒)
     */
    @Locked(key = "'pay:order:' + #orderId", waitTime = 5, leaseTime = 30)
    public void payOrder(Long orderId) {
        // 业务逻辑：检查订单状态、扣款等
        // 方法执行结束后自动释放锁
    }
    
    /**
     * 自定义失败策略
     * failStrategy = SKIP: 获取锁失败时不抛异常，直接跳过执行
     */
    @Locked(key = "'daily:job'", waitTime = 0, failStrategy = FailStrategy.SKIP)
    public void dailyJob() {
        // 定时任务，同一时间只允许一个实例执行
    }
}
```

### 场景 2：编程式锁

适用于需要更精细控制锁范围的场景。

```java
@Service
public class InventoryService {

    @Autowired
    private LockManager lockManager;

    public void deductStock(Long skuId, int count) {
        String lockKey = "stock:" + skuId;
        
        // 获取锁对象
        Lock lock = lockManager.getLock(lockKey);
        
        try {
            // 尝试加锁 (等待 10秒，持有 30秒)
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                try {
                    // 扣减库存业务
                } finally {
                    lock.unlock();
                }
            } else {
                throw new BusinessException("系统繁忙，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 场景 3：读写锁

适用于读多写少的场景（需使用 `getReadWriteLock`）。

```java
// 暂未在 Locked 注解中直接支持 ReadWriteLock，需使用编程式
Lock readLock = lockManager.getReadWriteLock("product:1").readLock();
Lock writeLock = lockManager.getReadWriteLock("product:1").writeLock();
```

## 4. 最佳实践 (Best Practices)

1.  **Key 的设计**：锁的粒度越细越好。例如锁 `order:1` 优于锁 `order:all`。
2.  **看门狗机制**：默认启用。如果业务执行时间不确定，不要手动指定 `leaseTime`（或设为 -1），让看门狗自动续期，防止业务未完成锁就被释放。
3.  **幂等性**：分布式锁能防止并发，但不能替代数据库的唯一索引或状态机，建议结合使用以保证最终数据一致性。


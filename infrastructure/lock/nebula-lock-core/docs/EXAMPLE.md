# nebula-lock-core 模块示例

## 模块简介

`nebula-lock-core` 模块定义了 Nebula 框架的分布式锁核心抽象。它提供了一套统一的 API 和注解，用于在分布式环境中控制并发访问。

核心组件包括：
- **LockManager**: 锁管理接口，用于获取和管理锁实例。
- **@Locked**: 分布式锁注解，支持 SpEL 表达式、超时控制和自定义失败策略。
- **Lock / ReadWriteLock**: 标准的锁接口抽象。

## 核心功能示例

### 1. 使用 `@Locked` 注解

这是最推荐的使用方式，通过 AOP 自动处理锁的获取和释放。

**`io.nebula.example.lock.service.InventoryService`**:

```java
package io.nebula.example.lock.service;

import io.nebula.lock.Locked;
import io.nebula.lock.Locked.FailStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class InventoryService {

    /**
     * 扣减库存
     * 
     * key: 动态生成，如 "product:lock:1001"
     * waitTime: 等待锁 5 秒
     * leaseTime: 锁持有时间 10 秒
     * failStrategy: 获取失败时抛出异常
     */
    @Locked(
        key = "'product:lock:' + #productId",
        waitTime = 5,
        leaseTime = 10,
        timeUnit = TimeUnit.SECONDS,
        failStrategy = FailStrategy.THROW_EXCEPTION
    )
    public void decreaseStock(Long productId, int count) {
        log.info("正在扣减产品 {} 的库存...", productId);
        // 模拟业务处理
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("扣减成功");
    }

    /**
     * 尝试获取锁，失败则直接返回 false
     */
    @Locked(
        key = "'user:action:' + #userId",
        waitTime = 0, // 不等待
        failStrategy = FailStrategy.RETURN_FALSE
    )
    public boolean tryUserAction(Long userId) {
        log.info("用户 {} 获取到锁，执行操作...", userId);
        return true;
    }
}
```

### 2. 使用 `LockManager` 编程式加锁

适用于需要更细粒度控制锁范围的场景。

**`io.nebula.example.lock.service.ManualLockService`**:

```java
package io.nebula.example.lock.service;

import io.nebula.lock.Lock;
import io.nebula.lock.LockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManualLockService {

    private final LockManager lockManager;

    public void processWithLock(String resourceId) {
        String lockKey = "resource:" + resourceId;
        Lock lock = lockManager.getLock(lockKey);

        try {
            // 尝试加锁，最多等待 3 秒，持有锁 30 秒
            if (lock.tryLock(3, 30, TimeUnit.SECONDS)) {
                try {
                    log.info("获取到锁: {}", lockKey);
                    // 业务逻辑
                } finally {
                    lock.unlock();
                    log.info("释放锁: {}", lockKey);
                }
            } else {
                log.warn("获取锁超时: {}", lockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("加锁过程被打断", e);
        }
    }
}
```

## 总结

`nebula-lock-core` 提供了标准化的分布式锁接口。实际使用时，通常需要配合具体的实现模块（如 `nebula-lock-redis`）来提供底层的锁机制。


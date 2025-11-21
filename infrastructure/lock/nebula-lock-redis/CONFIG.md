# Nebula Lock Redis 配置指南

> Redis分布式锁配置说明

## 概述

`nebula-lock-redis` 提供基于 Redis 的分布式锁实现。

## 基本配置

### Maven依赖

```xml
<dependency>
    <groupId>com.andy.nebula</groupId>
    <artifactId>nebula-lock-redis</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### Redis锁配置

```yaml
nebula:
  lock:
    redis:
      enabled: true
      # 默认锁过期时间(秒)
      default-lease-time: 30
      # 等待锁超时(秒)
      wait-time: 10
      # 自动续期
      auto-renew: true
```

## 票务系统场景

### 防止超卖

```java
@Service
public class SeatService {
    
    @Autowired
    private RedissonClient redissonClient;
    
    public void lockSeat(Long showtimeId, String seatNo, Long userId) {
        String lockKey = "seat:lock:" + showtimeId + ":" + seatNo;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 尝试获取锁(等待10秒,锁定5分钟)
            boolean locked = lock.tryLock(10, 300, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException("座位已被锁定");
            }
            
            // 锁定座位
            lockSeatInDb(showtimeId, seatNo, userId);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("锁定座位失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

---

**最后更新**: 2025-11-20


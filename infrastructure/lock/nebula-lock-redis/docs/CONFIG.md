# Nebula Lock Redis 配置指南

> Redis分布式锁配置说明

## 概述

`nebula-lock-redis` 提供基于 Redis 的分布式锁实现，支持可重入锁、公平锁、读写锁、看门狗机制等特性。

## 基本配置

### Maven依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-lock-redis</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### 完整配置示例

```yaml
nebula:
  lock:
    enabled: true                          # 是否启用分布式锁
    enable-aspect: true                    # 是否启用@Locked注解切面
    default-wait-time: 30s                 # 默认等待锁的超时时间
    default-lease-time: 60s                # 默认锁的租约时间
    enable-watchdog: true                  # 是否启用看门狗机制
    fair: false                            # 是否启用公平锁
    
    # Redis连接配置（独立于缓存配置）
    redis:
      host: 192.168.2.130                  # Redis服务器地址
      port: 6379                           # Redis端口
      password: your_password              # Redis密码（可选）
      database: 0                          # 数据库索引
      timeout: 6000                        # 连接超时(毫秒)
      connection-minimum-idle-size: 5     # 最小空闲连接数
      connection-pool-size: 20            # 连接池大小
    
    # Redlock配置(可选，用于多Redis实例高可用)
    redlock:
      enabled: false
      addresses:
        - redis://127.0.0.1:6379
        - redis://127.0.0.1:6380
        - redis://127.0.0.1:6381
      quorum: 2                            # 最小获取锁的实例数
```

### 配置说明

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enabled` | boolean | true | 是否启用分布式锁 |
| `enable-aspect` | boolean | true | 是否启用 @Locked 注解切面 |
| `default-wait-time` | Duration | 30s | 默认等待锁超时时间 |
| `default-lease-time` | Duration | 60s | 默认锁租约时间 |
| `enable-watchdog` | boolean | true | 是否启用看门狗自动续期 |
| `fair` | boolean | false | 是否使用公平锁 |
| `redis.host` | String | localhost | Redis服务器地址 |
| `redis.port` | int | 6379 | Redis端口 |
| `redis.password` | String | null | Redis密码 |
| `redis.database` | int | 0 | 数据库索引 |
| `redis.timeout` | int | 3000 | 连接超时(毫秒) |
| `redis.connection-minimum-idle-size` | int | 5 | 最小空闲连接数 |
| `redis.connection-pool-size` | int | 20 | 连接池大小 |

### 备选配置方式

如果不配置 `nebula.lock.redis`，将使用 Spring Boot 自动配置的 RedissonClient（读取 `spring.data.redis` 配置）：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: 
      database: 0
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


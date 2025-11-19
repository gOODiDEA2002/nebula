# Nebula Data Cache 使用示例

> **注意**：Nebula Data Cache 提供了一套独立的高级缓存接口，支持多级缓存（本地 + Redis）、异步操作和丰富的数据结构（Hash, List, ZSet 等）。建议通过注入 `CacheManager` 进行编程式调用。

## 1. 快速开始 (Quick Start)

### 引入依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-data-cache</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

## 2. 配置示例 (Configuration)

### 基础配置 (Redis)

```yaml
nebula:
  data:
    cache:
      enabled: true
      type: redis # 可选: local, redis, multi-level
      redis:
        host: localhost
        port: 6379
        password: password
        key-prefix: "myapp:cache:"
        timeout: 2s
```

### 多级缓存配置 (L1 Caffeine + L2 Redis)

```yaml
nebula:
  data:
    cache:
      enabled: true
      type: multi-level
      multi-level:
        l1-max-size: 10000
        l1-default-ttl: 5m
        sync-on-update: true # 更新 L2 时同步淘汰 L1
```

## 3. 代码示例 (Code Examples)

### 场景 1：基础 KV 操作

```java
@Service
public class UserService {

    @Autowired
    private CacheManager cacheManager;

    public User getUser(Long id) {
        String key = "user:" + id;
        
        // Get or Set 模式 (推荐)
        return cacheManager.getOrSet(key, User.class, () -> {
            // 缓存未命中时执行数据库查询
            return userMapper.selectById(id);
        }, Duration.ofMinutes(30));
    }
    
    public void updateUser(User user) {
        userMapper.updateById(user);
        // 删除缓存
        cacheManager.delete("user:" + user.getId());
    }
}
```

### 场景 2：复杂数据结构 (Hash/List/ZSet)

`CacheManager` 封装了 Redis 的常用操作，且在多级缓存模式下，读取操作会优先查询 L1。

```java
@Service
public class RankingService {

    @Autowired
    private CacheManager cacheManager;

    // 排行榜 (ZSet)
    public void updateScore(Long userId, double score) {
        cacheManager.zAdd("rank:daily", userId, score);
    }

    // 获取 Top 10
    public List<Long> getTop10() {
        // 自动处理序列化
        return cacheManager.zRange("rank:daily", 0, 9, Long.class);
    }
}
```

### 场景 3：异步操作

适用于不阻塞主线程的缓存更新。

```java
public void logAccess(Long userId) {
    // 异步写入，不等待结果
    cacheManager.setAsync("access:log:" + userId, LocalDateTime.now());
}
```

## 4. 最佳实践 (Best Practices)

1.  **优先使用 getOrSet**：原子性更好，代码更简洁，避免缓存穿透代码冗余。
2.  **合理设置 TTL**：多级缓存模式下，L1 的 TTL 通常应短于 L2，以减少数据不一致窗口。
3.  **键名规范**：利用 `key-prefix` 配置隔离不同应用的缓存，避免冲突。


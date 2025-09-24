# Nebula Data Cache 模块

## 概述

`nebula-data-cache`是Nebula框架的缓存抽象层，提供统一的缓存操作接口，支持多种缓存后端，包括本地缓存（Caffeine）、分布式缓存（Redis）以及多级缓存架构。

## 核心特性

- 🚀 **统一接口**：提供一致的缓存API，支持多种缓存实现
- 🏗️ **多级缓存**：支持本地缓存+分布式缓存的多级架构
- ⚡ **高性能**：集成Caffeine本地缓存，提供极高的访问性能  
- 🌐 **分布式**：支持Redis分布式缓存，实现集群间数据共享
- 🔧 **灵活配置**：支持TTL、容量限制、淘汰策略等丰富配置
- 📊 **监控统计**：提供缓存命中率、性能指标等监控能力

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-data-cache</artifactId>
    <version>2.0.0</version>
</dependency>

<!-- Redis支持 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Caffeine本地缓存 -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

### 2. 基础配置

```yaml
nebula:
  data:
    cache:
      enabled: true
      default-ttl: 3600s              # 默认过期时间
      default-max-size: 10000         # 默认最大缓存条目数
      
      # 缓存类型：local, redis, multi-level
      type: multi-level
      
      # 本地缓存配置
      local:
        enabled: true
        max-size: 10000
        expire-after-write: 300s       # 写入后过期时间
        expire-after-access: 600s      # 访问后过期时间
        
      # Redis缓存配置
      redis:
        enabled: true
        database: 0
        key-prefix: "nebula:cache:"
        
      # 多级缓存配置
      multi-level:
        enabled: true
        local-cache-enabled: true
        remote-cache-enabled: true
        sync-on-update: true           # 更新时同步到所有缓存层

# Redis连接配置
spring:
  redis:
    host: localhost
    port: 6379
    password: 
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
```

### 3. 基本使用

```java
@Service
public class UserService {
    
    @Autowired
    private CacheManager cacheManager;
    
    public User getUserById(Long userId) {
        String cacheKey = "user:" + userId;
        
        // 从缓存获取
        User user = cacheManager.get(cacheKey, User.class);
        
        if (user == null) {
            // 缓存未命中，从数据库查询
            user = userRepository.findById(userId);
            
            if (user != null) {
                // 存入缓存，设置1小时过期
                cacheManager.put(cacheKey, user, Duration.ofHours(1));
            }
        }
        
        return user;
    }
    
    public void updateUser(User user) {
        // 更新数据库
        userRepository.update(user);
        
        // 更新缓存
        String cacheKey = "user:" + user.getId();
        cacheManager.put(cacheKey, user, Duration.ofHours(1));
        
        // 清除相关缓存
        cacheManager.evict("userList:*");
    }
    
    public void deleteUser(Long userId) {
        // 删除数据库记录
        userRepository.deleteById(userId);
        
        // 清除缓存
        String cacheKey = "user:" + userId;
        cacheManager.evict(cacheKey);
    }
}
```

## 核心组件

### 1. CacheManager接口

CacheManager提供统一的缓存操作接口：

```java
public interface CacheManager {
    
    // 基础操作
    <T> T get(String key, Class<T> type);
    <T> T get(String key, TypeReference<T> typeRef);
    void put(String key, Object value);
    void put(String key, Object value, Duration ttl);
    
    // 批量操作
    <T> Map<String, T> multiGet(Collection<String> keys, Class<T> type);
    void multiPut(Map<String, Object> keyValues);
    void multiPut(Map<String, Object> keyValues, Duration ttl);
    
    // 删除操作
    void evict(String key);
    void evictAll(Collection<String> keys);
    void evictPattern(String pattern);
    void clear();
    
    // 判断操作
    boolean exists(String key);
    Set<String> keys(String pattern);
    
    // 原子操作
    <T> T getAndPut(String key, Object value, Class<T> type);
    boolean putIfAbsent(String key, Object value);
    boolean putIfAbsent(String key, Object value, Duration ttl);
    
    // 统计信息
    CacheStats getStats();
    CacheStats getStats(String cacheName);
}
```

### 2. 注解驱动的缓存

使用Spring Cache注解简化缓存操作：

```java
@Service
public class ProductService {
    
    // 缓存方法结果
    @Cacheable(value = "products", key = "#id")
    public Product getProductById(Long id) {
        return productRepository.findById(id);
    }
    
    // 条件缓存
    @Cacheable(value = "products", key = "#id", condition = "#id > 0")
    public Product getProductConditional(Long id) {
        return productRepository.findById(id);
    }
    
    // 更新缓存
    @CachePut(value = "products", key = "#product.id")
    public Product updateProduct(Product product) {
        return productRepository.update(product);
    }
    
    // 清除缓存
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
    
    // 清除多个缓存
    @Caching(evict = {
        @CacheEvict(value = "products", key = "#product.id"),
        @CacheEvict(value = "productList", allEntries = true)
    })
    public void deleteProductAndClearList(Product product) {
        productRepository.delete(product);
    }
    
    // 自定义SpEL表达式
    @Cacheable(value = "products", 
               key = "#category + ':' + #status",
               unless = "#result.size() == 0")
    public List<Product> getProductsByCategoryAndStatus(String category, String status) {
        return productRepository.findByCategoryAndStatus(category, status);
    }
}
```

### 3. 多级缓存配置

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    @Primary
    public CacheManager multiLevelCacheManager(
            LocalCacheManager localCacheManager,
            RedisCacheManager redisCacheManager,
            MultiLevelCacheConfig config) {
        
        return new MultiLevelCacheManager(
            localCacheManager, 
            redisCacheManager, 
            config
        );
    }
    
    @Bean
    public LocalCacheManager localCacheManager() {
        return LocalCacheManager.builder()
            .maximumSize(10000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofHours(1))
            .recordStats(true)
            .build();
    }
    
    @Bean
    public MultiLevelCacheConfig multiLevelCacheConfig() {
        MultiLevelCacheConfig config = new MultiLevelCacheConfig();
        config.setLocalCacheEnabled(true);
        config.setRemoteCacheEnabled(true);
        config.setSyncOnUpdate(true);
        config.setLocalCacheSize(5000);
        config.setLocalCacheTtl(Duration.ofMinutes(10));
        config.setRemoteCacheTtl(Duration.ofHours(1));
        return config;
    }
}
```

## 多级缓存详解

### 1. 多级缓存架构

```
请求 -> L1缓存(本地) -> L2缓存(Redis) -> 数据源
       ↑________________↑
           数据同步
```

### 2. 多级缓存使用示例

```java
@Service
public class MultiLevelCacheService {
    
    @Autowired
    private MultiLevelCacheManager cacheManager;
    
    public UserProfile getUserProfile(Long userId) {
        String cacheKey = "profile:" + userId;
        
        // 多级缓存自动处理L1/L2查找
        UserProfile profile = cacheManager.get(cacheKey, UserProfile.class);
        
        if (profile == null) {
            // 缓存未命中，查询数据库
            profile = userProfileRepository.findByUserId(userId);
            
            if (profile != null) {
                // 存储到多级缓存
                // L1: 10分钟, L2: 1小时
                cacheManager.put(cacheKey, profile, Duration.ofHours(1));
            }
        }
        
        return profile;
    }
    
    public void updateUserProfile(UserProfile profile) {
        userProfileRepository.update(profile);
        
        String cacheKey = "profile:" + profile.getUserId();
        
        // 更新多级缓存，会同步到所有层级
        cacheManager.put(cacheKey, profile, Duration.ofHours(1));
    }
}
```

### 3. 缓存穿透保护

```java
@Service
public class CacheProtectionService {
    
    @Autowired
    private CacheManager cacheManager;
    
    // 防止缓存穿透
    public User getUserSafely(Long userId) {
        String cacheKey = "user:" + userId;
        String nullKey = "user:null:" + userId;
        
        // 检查是否已缓存空值
        if (cacheManager.exists(nullKey)) {
            return null;
        }
        
        User user = cacheManager.get(cacheKey, User.class);
        
        if (user == null) {
            user = userRepository.findById(userId);
            
            if (user != null) {
                cacheManager.put(cacheKey, user, Duration.ofHours(1));
            } else {
                // 缓存空值，防止穿透，设置较短过期时间
                cacheManager.put(nullKey, "null", Duration.ofMinutes(5));
            }
        }
        
        return user;
    }
    
    // 防止缓存雪崩
    public List<Product> getHotProducts() {
        String cacheKey = "hotProducts";
        
        List<Product> products = cacheManager.get(cacheKey, 
            new TypeReference<List<Product>>() {});
        
        if (products == null) {
            synchronized (this) {
                // 双重检查
                products = cacheManager.get(cacheKey, 
                    new TypeReference<List<Product>>() {});
                
                if (products == null) {
                    products = productRepository.findHotProducts();
                    
                    // 添加随机过期时间，防止雪崩
                    long randomSeconds = 3600 + ThreadLocalRandom.current().nextInt(600);
                    Duration ttl = Duration.ofSeconds(randomSeconds);
                    
                    cacheManager.put(cacheKey, products, ttl);
                }
            }
        }
        
        return products;
    }
}
```

## 缓存策略和模式

### 1. Cache-Aside模式

```java
@Service
public class CacheAsideService {
    
    public Order getOrder(Long orderId) {
        String cacheKey = "order:" + orderId;
        
        // 1. 先从缓存读取
        Order order = cacheManager.get(cacheKey, Order.class);
        
        if (order == null) {
            // 2. 缓存未命中，从数据库读取
            order = orderRepository.findById(orderId);
            
            if (order != null) {
                // 3. 写入缓存
                cacheManager.put(cacheKey, order, Duration.ofHours(2));
            }
        }
        
        return order;
    }
    
    public Order updateOrder(Order order) {
        // 1. 更新数据库
        Order updatedOrder = orderRepository.update(order);
        
        // 2. 删除缓存
        String cacheKey = "order:" + order.getId();
        cacheManager.evict(cacheKey);
        
        return updatedOrder;
    }
}
```

### 2. Write-Through模式

```java
@Service
public class WriteThroughService {
    
    public void updateUserSettings(Long userId, UserSettings settings) {
        String cacheKey = "settings:" + userId;
        
        // 同时更新数据库和缓存
        userSettingsRepository.update(settings);
        cacheManager.put(cacheKey, settings, Duration.ofDays(1));
    }
    
    // 使用注解实现Write-Through
    @CachePut(value = "settings", key = "#userId")
    public UserSettings updateUserSettingsAnnotated(Long userId, UserSettings settings) {
        return userSettingsRepository.update(settings);
    }
}
```

### 3. Write-Back模式

```java
@Service
public class WriteBackService {
    
    private final Map<String, Object> dirtyCache = new ConcurrentHashMap<>();
    
    public void updateUserScore(Long userId, Integer score) {
        String cacheKey = "score:" + userId;
        
        // 1. 更新缓存
        cacheManager.put(cacheKey, score, Duration.ofHours(6));
        
        // 2. 标记为脏数据
        dirtyCache.put(cacheKey, score);
    }
    
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void flushDirtyData() {
        if (dirtyCache.isEmpty()) {
            return;
        }
        
        // 批量写回数据库
        Map<String, Object> toFlush = new HashMap<>(dirtyCache);
        dirtyCache.clear();
        
        for (Map.Entry<String, Object> entry : toFlush.entrySet()) {
            String cacheKey = entry.getKey();
            Object value = entry.getValue();
            
            try {
                if (cacheKey.startsWith("score:")) {
                    Long userId = Long.parseLong(cacheKey.substring(6));
                    userScoreRepository.updateScore(userId, (Integer) value);
                }
            } catch (Exception e) {
                log.error("Failed to flush dirty data: {}", cacheKey, e);
                // 重新放回脏数据队列
                dirtyCache.put(cacheKey, value);
            }
        }
    }
}
```

## 缓存预热和更新策略

### 1. 缓存预热

```java
@Component
public class CacheWarmupService {
    
    @EventListener(ApplicationReadyEvent.class)
    public void warmupCache() {
        log.info("Starting cache warmup...");
        
        // 预热用户缓存
        warmupUserCache();
        
        // 预热产品缓存
        warmupProductCache();
        
        // 预热配置缓存
        warmupConfigCache();
        
        log.info("Cache warmup completed");
    }
    
    private void warmupUserCache() {
        List<User> activeUsers = userRepository.findActiveUsers(1000);
        
        for (User user : activeUsers) {
            String cacheKey = "user:" + user.getId();
            cacheManager.put(cacheKey, user, Duration.ofHours(2));
        }
        
        log.info("Warmed up {} active users", activeUsers.size());
    }
    
    private void warmupProductCache() {
        List<Product> hotProducts = productRepository.findHotProducts();
        
        for (Product product : hotProducts) {
            String cacheKey = "product:" + product.getId();
            cacheManager.put(cacheKey, product, Duration.ofHours(4));
        }
        
        log.info("Warmed up {} hot products", hotProducts.size());
    }
    
    private void warmupConfigCache() {
        Map<String, String> configs = configRepository.findAllConfigs();
        
        String cacheKey = "system:configs";
        cacheManager.put(cacheKey, configs, Duration.ofDays(1));
        
        log.info("Warmed up system configs");
    }
}
```

### 2. 定时更新策略

```java
@Component
public class CacheRefreshService {
    
    // 每5分钟刷新热点数据
    @Scheduled(fixedRate = 300000)
    public void refreshHotData() {
        refreshHotProducts();
        refreshTrendingTopics();
    }
    
    // 每小时刷新用户统计
    @Scheduled(cron = "0 0 * * * *")
    public void refreshUserStats() {
        List<Long> activeUserIds = userActivityRepository.findActiveUserIds();
        
        for (Long userId : activeUserIds) {
            UserStats stats = userStatsRepository.calculateStats(userId);
            String cacheKey = "stats:" + userId;
            cacheManager.put(cacheKey, stats, Duration.ofHours(2));
        }
        
        log.info("Refreshed stats for {} active users", activeUserIds.size());
    }
    
    private void refreshHotProducts() {
        List<Product> hotProducts = productRepository.findCurrentHotProducts();
        String cacheKey = "hotProducts";
        cacheManager.put(cacheKey, hotProducts, Duration.ofMinutes(10));
    }
    
    private void refreshTrendingTopics() {
        List<Topic> trendingTopics = topicRepository.findTrendingTopics();
        String cacheKey = "trendingTopics";
        cacheManager.put(cacheKey, trendingTopics, Duration.ofMinutes(5));
    }
}
```

### 3. 主动失效策略

```java
@Service
public class CacheInvalidationService {
    
    @Autowired
    private CacheManager cacheManager;
    
    @EventListener
    public void handleUserUpdated(UserUpdatedEvent event) {
        Long userId = event.getUserId();
        
        // 清除用户相关缓存
        cacheManager.evict("user:" + userId);
        cacheManager.evict("profile:" + userId);
        cacheManager.evict("settings:" + userId);
        
        // 清除用户列表缓存
        cacheManager.evictPattern("userList:*");
        
        log.debug("Invalidated cache for user: {}", userId);
    }
    
    @EventListener
    public void handleProductUpdated(ProductUpdatedEvent event) {
        Long productId = event.getProductId();
        String category = event.getCategory();
        
        // 清除产品缓存
        cacheManager.evict("product:" + productId);
        
        // 清除分类相关缓存
        cacheManager.evictPattern("category:" + category + ":*");
        
        // 如果是热门产品，清除热门产品缓存
        if (event.isHotProduct()) {
            cacheManager.evict("hotProducts");
        }
        
        log.debug("Invalidated cache for product: {}", productId);
    }
    
    @EventListener
    public void handleConfigChanged(ConfigChangedEvent event) {
        String configKey = event.getConfigKey();
        
        // 清除配置缓存
        cacheManager.evict("config:" + configKey);
        cacheManager.evict("system:configs");
        
        log.info("Invalidated config cache: {}", configKey);
    }
}
```

## 性能监控和调优

### 1. 缓存统计监控

```java
@RestController
@RequestMapping("/admin/cache")
public class CacheMonitorController {
    
    @Autowired
    private CacheManager cacheManager;
    
    @GetMapping("/stats")
    public CacheStats getCacheStats() {
        return cacheManager.getStats();
    }
    
    @GetMapping("/stats/{cacheName}")
    public CacheStats getCacheStats(@PathVariable String cacheName) {
        return cacheManager.getStats(cacheName);
    }
    
    @GetMapping("/keys")
    public Set<String> getCacheKeys(@RequestParam(required = false) String pattern) {
        if (pattern != null) {
            return cacheManager.keys(pattern);
        }
        return cacheManager.keys("*");
    }
    
    @PostMapping("/evict")
    public ResponseEntity<?> evictCache(@RequestParam String key) {
        cacheManager.evict(key);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/clear")
    public ResponseEntity<?> clearCache() {
        cacheManager.clear();
        return ResponseEntity.ok().build();
    }
}
```

### 2. 性能指标收集

```java
@Component
public class CacheMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    private final CacheManager cacheManager;
    
    public CacheMetricsCollector(MeterRegistry meterRegistry, CacheManager cacheManager) {
        this.meterRegistry = meterRegistry;
        this.cacheManager = cacheManager;
        
        // 注册缓存指标
        Gauge.builder("cache.hit.ratio")
            .description("Cache hit ratio")
            .register(meterRegistry, this, CacheMetricsCollector::getHitRatio);
            
        Gauge.builder("cache.size")
            .description("Cache size")
            .register(meterRegistry, this, CacheMetricsCollector::getCacheSize);
    }
    
    private double getHitRatio(CacheMetricsCollector collector) {
        CacheStats stats = cacheManager.getStats();
        if (stats.getRequestCount() == 0) {
            return 0.0;
        }
        return (double) stats.getHitCount() / stats.getRequestCount();
    }
    
    private double getCacheSize(CacheMetricsCollector collector) {
        CacheStats stats = cacheManager.getStats();
        return stats.getSize();
    }
    
    @EventListener
    public void onCacheHit(CacheHitEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("cache.access")
            .tag("result", "hit")
            .tag("cache", event.getCacheName())
            .register(meterRegistry));
    }
    
    @EventListener
    public void onCacheMiss(CacheMissEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("cache.access")
            .tag("result", "miss")
            .tag("cache", event.getCacheName())
            .register(meterRegistry));
    }
}
```

### 3. 缓存调优建议

```java
@Component
public class CacheOptimizationService {
    
    @Scheduled(fixedRate = 3600000) // 每小时分析一次
    public void analyzeCachePerformance() {
        CacheStats stats = cacheManager.getStats();
        
        // 分析命中率
        double hitRatio = (double) stats.getHitCount() / stats.getRequestCount();
        
        if (hitRatio < 0.7) {
            log.warn("Low cache hit ratio: {:.2f}%, consider reviewing cache strategy", hitRatio * 100);
        }
        
        // 分析缓存大小
        if (stats.getSize() > stats.getMaximumSize() * 0.9) {
            log.warn("Cache is nearly full: {}/{}, consider increasing size or adjusting TTL", 
                    stats.getSize(), stats.getMaximumSize());
        }
        
        // 分析淘汰率
        if (stats.getEvictionCount() > stats.getRequestCount() * 0.1) {
            log.warn("High eviction rate: {} evictions for {} requests", 
                    stats.getEvictionCount(), stats.getRequestCount());
        }
    }
    
    public void suggestOptimizations() {
        // 分析热点数据
        Set<String> hotKeys = analyzeHotKeys();
        
        // 分析冷数据
        Set<String> coldKeys = analyzeColdKeys();
        
        // 生成优化建议
        generateOptimizationReport(hotKeys, coldKeys);
    }
    
    private Set<String> analyzeHotKeys() {
        // 实现热点key分析逻辑
        return Collections.emptySet();
    }
    
    private Set<String> analyzeColdKeys() {
        // 实现冷数据分析逻辑
        return Collections.emptySet();
    }
    
    private void generateOptimizationReport(Set<String> hotKeys, Set<String> coldKeys) {
        log.info("Cache optimization report:");
        log.info("Hot keys: {}", hotKeys.size());
        log.info("Cold keys: {}", coldKeys.size());
        
        // 可以发送到监控系统或生成报告
    }
}
```

## 最佳实践

### 1. 缓存键设计

```java
public class CacheKeyUtils {
    
    private static final String DELIMITER = ":";
    
    // 用户相关缓存键
    public static String userKey(Long userId) {
        return "user" + DELIMITER + userId;
    }
    
    public static String userProfileKey(Long userId) {
        return "profile" + DELIMITER + userId;
    }
    
    // 业务相关缓存键
    public static String productKey(Long productId) {
        return "product" + DELIMITER + productId;
    }
    
    public static String categoryProductsKey(String category, int page, int size) {
        return "category" + DELIMITER + category + DELIMITER + "products" + DELIMITER + page + DELIMITER + size;
    }
    
    // 列表缓存键
    public static String userListKey(String department, String status) {
        return "userList" + DELIMITER + department + DELIMITER + status;
    }
    
    // 统计缓存键
    public static String dailyStatsKey(LocalDate date) {
        return "stats" + DELIMITER + "daily" + DELIMITER + date.toString();
    }
}
```

### 2. 缓存模板

```java
@Component
public class CacheTemplate {
    
    @Autowired
    private CacheManager cacheManager;
    
    /**
     * 通用缓存查询模板
     */
    public <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> loader) {
        T value = cacheManager.get(key, type);
        
        if (value == null) {
            value = loader.get();
            if (value != null) {
                cacheManager.put(key, value, ttl);
            }
        }
        
        return value;
    }
    
    /**
     * 带锁的缓存查询模板（防止缓存击穿）
     */
    public <T> T getOrLoadWithLock(String key, Class<T> type, Duration ttl, Supplier<T> loader) {
        T value = cacheManager.get(key, type);
        
        if (value == null) {
            String lockKey = "lock:" + key;
            
            // 尝试获取分布式锁
            if (cacheManager.putIfAbsent(lockKey, "locked", Duration.ofMinutes(1))) {
                try {
                    // 双重检查
                    value = cacheManager.get(key, type);
                    if (value == null) {
                        value = loader.get();
                        if (value != null) {
                            cacheManager.put(key, value, ttl);
                        }
                    }
                } finally {
                    // 释放锁
                    cacheManager.evict(lockKey);
                }
            } else {
                // 等待锁释放后重试
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return getOrLoad(key, type, ttl, loader);
            }
        }
        
        return value;
    }
}
```

### 3. 缓存使用规范

```java
@Service
public class BestPracticeService {
    
    @Autowired
    private CacheTemplate cacheTemplate;
    
    // ✅ 正确：使用合适的TTL
    public User getUserById(Long userId) {
        String key = CacheKeyUtils.userKey(userId);
        return cacheTemplate.getOrLoad(
            key, 
            User.class, 
            Duration.ofHours(1),  // 用户数据1小时过期
            () -> userRepository.findById(userId)
        );
    }
    
    // ✅ 正确：缓存适当大小的数据
    public UserSummary getUserSummary(Long userId) {
        String key = "summary:" + userId;
        return cacheTemplate.getOrLoad(
            key,
            UserSummary.class,
            Duration.ofMinutes(30),
            () -> buildUserSummary(userId)  // 只缓存摘要信息
        );
    }
    
    // ❌ 错误：缓存过大的对象
    public void badExample1(Long userId) {
        String key = "userWithAllDetails:" + userId;
        // 不要缓存包含大量关联数据的完整对象
        UserWithAllDetails bigObject = cacheTemplate.getOrLoad(
            key,
            UserWithAllDetails.class,
            Duration.ofHours(1),
            () -> userRepository.findWithAllDetails(userId)
        );
    }
    
    // ❌ 错误：TTL设置不当
    public void badExample2(Long userId) {
        String key = CacheKeyUtils.userKey(userId);
        // 不要设置过长的TTL
        User user = cacheTemplate.getOrLoad(
            key,
            User.class,
            Duration.ofDays(30),  // TTL过长
            () -> userRepository.findById(userId)
        );
    }
    
    // ✅ 正确：分层缓存不同类型的数据
    public ProductDetail getProductDetail(Long productId) {
        // 基础信息缓存较长时间
        Product product = cacheTemplate.getOrLoad(
            CacheKeyUtils.productKey(productId),
            Product.class,
            Duration.ofHours(4),
            () -> productRepository.findById(productId)
        );
        
        // 价格信息缓存较短时间
        ProductPrice price = cacheTemplate.getOrLoad(
            "price:" + productId,
            ProductPrice.class,
            Duration.ofMinutes(10),
            () -> priceService.getCurrentPrice(productId)
        );
        
        // 库存信息缓存很短时间
        ProductStock stock = cacheTemplate.getOrLoad(
            "stock:" + productId,
            ProductStock.class,
            Duration.ofMinutes(1),
            () -> stockService.getCurrentStock(productId)
        );
        
        return new ProductDetail(product, price, stock);
    }
}
```

## 故障排除

### 1. 常见问题诊断

```java
@Component
public class CacheDiagnosticService {
    
    public void diagnoseLowHitRatio() {
        CacheStats stats = cacheManager.getStats();
        double hitRatio = (double) stats.getHitCount() / stats.getRequestCount();
        
        if (hitRatio < 0.5) {
            log.warn("Very low cache hit ratio: {:.2f}%", hitRatio * 100);
            
            // 检查TTL设置
            checkTTLSettings();
            
            // 检查缓存大小
            checkCacheSize();
            
            // 检查数据访问模式
            checkAccessPattern();
        }
    }
    
    private void checkTTLSettings() {
        // 分析TTL是否过短
        log.info("Checking TTL settings...");
    }
    
    private void checkCacheSize() {
        CacheStats stats = cacheManager.getStats();
        if (stats.getEvictionCount() > 0) {
            log.warn("Cache evictions detected: {}, consider increasing cache size", 
                    stats.getEvictionCount());
        }
    }
    
    private void checkAccessPattern() {
        // 分析访问模式是否适合缓存
        log.info("Analyzing access patterns...");
    }
}
```

### 2. 缓存问题修复

```java
@Service
public class CacheRepairService {
    
    // 修复缓存不一致问题
    public void repairCacheInconsistency(String cacheKey) {
        log.info("Repairing cache inconsistency for key: {}", cacheKey);
        
        // 删除可能不一致的缓存
        cacheManager.evict(cacheKey);
        
        // 预热新数据
        if (cacheKey.startsWith("user:")) {
            Long userId = Long.parseLong(cacheKey.substring(5));
            User user = userRepository.findById(userId);
            if (user != null) {
                cacheManager.put(cacheKey, user, Duration.ofHours(1));
            }
        }
    }
    
    // 缓存雪崩恢复
    public void recoverFromCacheAvalanche() {
        log.info("Recovering from cache avalanche...");
        
        // 限流恢复
        RateLimiter limiter = RateLimiter.create(10.0); // 每秒10个请求
        
        // 分批预热关键缓存
        List<String> criticalKeys = getCriticalCacheKeys();
        
        for (String key : criticalKeys) {
            limiter.acquire(); // 限流
            
            try {
                warmupCacheKey(key);
            } catch (Exception e) {
                log.error("Failed to warmup cache key: {}", key, e);
            }
        }
    }
    
    private List<String> getCriticalCacheKeys() {
        // 返回关键缓存键列表
        return Arrays.asList("hotProducts", "systemConfigs", "userPermissions");
    }
    
    private void warmupCacheKey(String key) {
        // 根据key类型预热相应数据
        if ("hotProducts".equals(key)) {
            List<Product> products = productRepository.findHotProducts();
            cacheManager.put(key, products, Duration.ofMinutes(30));
        }
        // ... 其他key的预热逻辑
    }
}
```

通过以上配置和使用方式，你可以充分利用Nebula Cache模块提供的强大缓存功能，提高应用性能并确保数据一致性。

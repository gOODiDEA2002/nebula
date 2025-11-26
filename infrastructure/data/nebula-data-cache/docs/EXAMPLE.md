# Nebula Data Cache - 使用示例

> 多级缓存解决方案完整使用指南，以票务系统为例

## 目录

- [快速开始](#快速开始)
- [基础缓存操作](#基础缓存操作)
- [多级缓存](#多级缓存)
- [复杂数据结构](#复杂数据结构)
- [缓存更新策略](#缓存更新策略)
- [缓存预热](#缓存预热)
- [缓存问题处理](#缓存问题处理)
- [票务系统完整示例](#票务系统完整示例)
- [最佳实践](#最佳实践)

---

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-data-cache</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 基础配置

```yaml
nebula:
  data:
    cache:
      enabled: true
      type: redis  # local, redis, multi-level
      redis:
        host: localhost
        port: 6379
        password: password
        database: 0
        key-prefix: "ticket:cache:"
        timeout: 2s
        time-to-live: 1h
```

### 注入CacheManager

```java
@Service
@RequiredArgsConstructor
public class ShowtimeService {
    
    private final CacheManager cacheManager;
    private final ShowtimeMapper showtimeMapper;
    
    public Showtime getShowtime(Long id) {
        String key = "showtime:" + id;
        
        // 使用CacheManager的getOrSet方法
        return cacheManager.getOrSet(key, Showtime.class, () -> {
            // 缓存未命中时查询数据库
            return showtimeMapper.selectById(id);
        }, Duration.ofMinutes(30));
    }
}
```

---

## 基础缓存操作

### 1. 字符串操作（String）

```java
/**
 * 基础KV操作示例
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserCacheService {
    
    private final CacheManager cacheManager;
    private final UserMapper userMapper;
    
    /**
     * 查询用户（缓存优先）
     */
    public User getUser(Long userId) {
        String key = "user:" + userId;
        
        // 1. 先查缓存
        User cached = cacheManager.get(key, User.class);
        if (cached != null) {
            log.info("缓存命中：{}", key);
            return cached;
        }
        
        // 2. 缓存未命中，查询数据库
        User user = userMapper.selectById(userId);
        if (user != null) {
            // 3. 写入缓存
            cacheManager.set(key, user, Duration.ofHours(1));
        }
        
        return user;
    }
    
    /**
     * 更推荐的方式：使用getOrSet
     */
    public User getUserSimplified(Long userId) {
        String key = "user:" + userId;
        
        return cacheManager.getOrSet(key, User.class, () -> {
            return userMapper.selectById(userId);
        }, Duration.ofHours(1));
    }
    
    /**
     * 更新用户（删除缓存）
     */
    public void updateUser(User user) {
        // 1. 更新数据库
        userMapper.updateById(user);
        
        // 2. 删除缓存
        String key = "user:" + user.getId();
        cacheManager.delete(key);
        
        log.info("用户{}已更新，缓存已删除", user.getId());
    }
    
    /**
     * 批量查询用户
     */
    public Map<Long, User> batchGetUsers(List<Long> userIds) {
        List<String> keys = userIds.stream()
                .map(id -> "user:" + id)
                .collect(Collectors.toList());
        
        // 批量获取
        List<User> users = cacheManager.mGet(keys, User.class);
        
        return userIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> users.get(userIds.indexOf(id))
                ));
    }
    
    /**
     * 异步更新缓存
     */
    public void updateUserAsync(User user) {
        userMapper.updateById(user);
        
        String key = "user:" + user.getId();
        // 异步删除缓存，不阻塞主线程
        cacheManager.deleteAsync(key);
    }
}
```

### 2. 计数器操作

```java
/**
 * 计数器示例：统计演出浏览次数
 */
@Service
@RequiredArgsConstructor
public class ShowtimeViewCountService {
    
    private final CacheManager cacheManager;
    
    /**
     * 增加浏览次数
     */
    public long incrementViewCount(Long showtimeId) {
        String key = "showtime:view:" + showtimeId;
        
        // 原子递增
        return cacheManager.increment(key, 1L);
    }
    
    /**
     * 获取浏览次数
     */
    public long getViewCount(Long showtimeId) {
        String key = "showtime:view:" + showtimeId;
        
        Long count = cacheManager.get(key, Long.class);
        return count != null ? count : 0L;
    }
    
    /**
     * 批量获取浏览次数
     */
    public Map<Long, Long> batchGetViewCount(List<Long> showtimeIds) {
        List<String> keys = showtimeIds.stream()
                .map(id -> "showtime:view:" + id)
                .collect(Collectors.toList());
        
        List<Long> counts = cacheManager.mGet(keys, Long.class);
        
        Map<Long, Long> result = new HashMap<>();
        for (int i = 0; i < showtimeIds.size(); i++) {
            Long count = counts.get(i);
            result.put(showtimeIds.get(i), count != null ? count : 0L);
        }
        
        return result;
    }
}
```

---

## 多级缓存

### 1. 配置多级缓存

```yaml
nebula:
  data:
    cache:
      enabled: true
      type: multi-level  # 启用多级缓存
      multi-level:
        # L1缓存（本地缓存 - Caffeine）
        l1-enabled: true
        l1-max-size: 10000
        l1-default-ttl: 5m  # L1缓存TTL通常较短
        l1-expire-after-write: true
        
        # L2缓存（分布式缓存 - Redis）
        l2-enabled: true
        l2-default-ttl: 30m  # L2缓存TTL通常较长
        
        # 同步策略
        sync-on-update: true  # 更新L2时淘汰L1
        sync-on-delete: true  # 删除L2时淘汰L1
      
      redis:
        host: localhost
        port: 6379
        password: password
        key-prefix: "ticket:cache:"
```

### 2. 多级缓存使用示例

```java
/**
 * 多级缓存示例：演出信息缓存
 * L1（本地）：热门演出，TTL 5分钟
 * L2（Redis）：所有演出，TTL 30分钟
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShowtimeMultiLevelCacheService {
    
    private final CacheManager cacheManager;  // 多级缓存管理器
    private final ShowtimeMapper showtimeMapper;
    
    /**
     * 查询演出（多级缓存）
     * 1. 先查L1（本地Caffeine）
     * 2. L1未命中，查L2（Redis）
     * 3. L2未命中，查数据库，并依次填充L2和L1
     */
    public Showtime getShowtime(Long id) {
        String key = "showtime:" + id;
        
        return cacheManager.getOrSet(key, Showtime.class, () -> {
            log.info("多级缓存未命中，查询数据库：{}", id);
            return showtimeMapper.selectById(id);
        }, Duration.ofMinutes(30)); // L2 TTL
    }
    
    /**
     * 更新演出（更新数据库并清理所有缓存层）
     */
    public void updateShowtime(Showtime showtime) {
        // 1. 更新数据库
        showtimeMapper.updateById(showtime);
        
        // 2. 删除缓存（会同时删除L1和L2）
        String key = "showtime:" + showtime.getId();
        cacheManager.delete(key);
        
        log.info("演出{}已更新，多级缓存已清理", showtime.getId());
    }
    
    /**
     * 批量查询演出（充分利用L1缓存）
     */
    public List<Showtime> batchGetShowtimes(List<Long> showtimeIds) {
        return showtimeIds.stream()
                .map(this::getShowtime)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * 预热热门演出到L1缓存
     */
    public void warmUpHotShowtimes() {
        log.info("开始预热热门演出到L1缓存");
        
        // 查询热门演出列表
        List<Long> hotShowtimeIds = getHotShowtimeIds();
        
        // 批量加载到缓存
        for (Long id : hotShowtimeIds) {
            getShowtime(id); // 会自动填充L1和L2
        }
        
        log.info("预热完成，共{}个热门演出", hotShowtimeIds.size());
    }
    
    private List<Long> getHotShowtimeIds() {
        // 从数据库或其他服务获取热门演出ID列表
        // 此处省略实现
        return Arrays.asList(1L, 2L, 3L, 4L, 5L);
    }
}
```

### 3. 多级缓存性能对比

```java
/**
 * 多级缓存性能测试
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CachePerformanceTest {
    
    private final CacheManager cacheManager;
    private final ShowtimeMapper showtimeMapper;
    
    public void performanceTest(Long showtimeId) {
        String key = "showtime:" + showtimeId;
        
        // 1. 第一次查询（缓存未命中，查询数据库）
        long start1 = System.currentTimeMillis();
        Showtime showtime1 = cacheManager.getOrSet(key, Showtime.class, () -> {
            return showtimeMapper.selectById(showtimeId);
        }, Duration.ofMinutes(30));
        long time1 = System.currentTimeMillis() - start1;
        log.info("第一次查询（数据库）：{}ms", time1);  // 约100-200ms
        
        // 2. 第二次查询（L2缓存命中 - Redis）
        cacheManager.deleteFromLocal(key);  // 清除L1，保留L2
        long start2 = System.currentTimeMillis();
        Showtime showtime2 = cacheManager.get(key, Showtime.class);
        long time2 = System.currentTimeMillis() - start2;
        log.info("第二次查询（Redis）：{}ms", time2);  // 约5-10ms
        
        // 3. 第三次查询（L1缓存命中 - 本地Caffeine）
        long start3 = System.currentTimeMillis();
        Showtime showtime3 = cacheManager.get(key, Showtime.class);
        long time3 = System.currentTimeMillis() - start3;
        log.info("第三次查询（L1本地）：{}ms", time3);  // 约0.1-1ms
        
        log.info("性能提升：数据库→Redis: {}倍, Redis→L1: {}倍",
                time1 / time2, time2 / time3);
    }
}
```

---

## 复杂数据结构

### 1. Hash操作

```java
/**
 * Hash操作示例：订单详情缓存
 */
@Service
@RequiredArgsConstructor
public class OrderCacheService {
    
    private final CacheManager cacheManager;
    private final OrderMapper orderMapper;
    
    /**
     * 缓存订单详情（使用Hash存储多个字段）
     */
    public void cacheOrderDetails(Order order) {
        String key = "order:details:" + order.getOrderNo();
        
        Map<String, Object> fields = new HashMap<>();
        fields.put("orderNo", order.getOrderNo());
        fields.put("userId", order.getUserId());
        fields.put("showtimeId", order.getShowtimeId());
        fields.put("quantity", order.getQuantity());
        fields.put("totalAmount", order.getTotalAmount().toString());
        fields.put("status", order.getStatus());
        fields.put("seats", order.getSeats());
        fields.put("createTime", order.getCreateTime().toString());
        
        // 批量写入Hash
        cacheManager.hMSet(key, fields);
        cacheManager.expire(key, Duration.ofHours(2));
    }
    
    /**
     * 获取订单详情
     */
    public Order getOrderDetails(String orderNo) {
        String key = "order:details:" + orderNo;
        
        Map<String, Object> fields = cacheManager.hGetAll(key);
        if (fields.isEmpty()) {
            return null;
        }
        
        Order order = new Order();
        order.setOrderNo((String) fields.get("orderNo"));
        order.setUserId(Long.valueOf(fields.get("userId").toString()));
        order.setShowtimeId(Long.valueOf(fields.get("showtimeId").toString()));
        order.setQuantity(Integer.valueOf(fields.get("quantity").toString()));
        order.setTotalAmount(new BigDecimal(fields.get("totalAmount").toString()));
        order.setStatus((String) fields.get("status"));
        order.setSeats((String) fields.get("seats"));
        // ... 其他字段
        
        return order;
    }
    
    /**
     * 更新订单状态（只更新单个字段）
     */
    public void updateOrderStatus(String orderNo, String newStatus) {
        String key = "order:details:" + orderNo;
        
        cacheManager.hSet(key, "status", newStatus);
    }
    
    /**
     * 批量获取用户的所有订单号
     */
    public List<String> getUserOrderNos(Long userId) {
        String key = "user:orders:" + userId;
        
        return cacheManager.lRange(key, 0, -1, String.class);
    }
}
```

### 2. List操作

```java
/**
 * List操作示例：用户订单列表
 */
@Service
@RequiredArgsConstructor
public class UserOrderListService {
    
    private final CacheManager cacheManager;
    
    /**
     * 添加订单到用户订单列表（从头部插入）
     */
    public void addOrderToUserList(Long userId, String orderNo) {
        String key = "user:orders:" + userId;
        
        // 从列表头部插入
        cacheManager.lPush(key, orderNo);
        
        // 限制列表长度（保留最近100个订单）
        cacheManager.lTrim(key, 0, 99);
        
        // 设置过期时间
        cacheManager.expire(key, Duration.ofDays(30));
    }
    
    /**
     * 获取用户最近的订单列表
     */
    public List<String> getRecentOrders(Long userId, int count) {
        String key = "user:orders:" + userId;
        
        return cacheManager.lRange(key, 0, count - 1, String.class);
    }
    
    /**
     * 获取用户订单总数
     */
    public long getUserOrderCount(Long userId) {
        String key = "user:orders:" + userId;
        
        return cacheManager.lSize(key);
    }
}
```

### 3. ZSet操作（排行榜）

```java
/**
 * ZSet操作示例：演出热度排行榜
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShowtimeRankingService {
    
    private final CacheManager cacheManager;
    
    private static final String DAILY_RANKING_KEY = "ranking:daily";
    private static final String WEEKLY_RANKING_KEY = "ranking:weekly";
    
    /**
     * 更新演出热度分数
     */
    public void updateShowtimeScore(Long showtimeId, double score) {
        // 更新日榜
        cacheManager.zAdd(DAILY_RANKING_KEY, showtimeId, score);
        
        // 更新周榜
        cacheManager.zAdd(WEEKLY_RANKING_KEY, showtimeId, score);
    }
    
    /**
     * 增加演出热度（浏览、购买等行为）
     */
    public void incrementShowtimeScore(Long showtimeId, double increment) {
        // 增量更新分数
        cacheManager.zIncrementScore(DAILY_RANKING_KEY, showtimeId, increment);
        cacheManager.zIncrementScore(WEEKLY_RANKING_KEY, showtimeId, increment);
    }
    
    /**
     * 获取热度排行榜（Top N）
     */
    public List<ShowtimeRanking> getTopShowtimes(int topN) {
        // 获取分数最高的前N个演出ID（降序）
        Set<Object> showtimeIds = cacheManager.zReverseRange(DAILY_RANKING_KEY, 0, topN - 1);
        
        List<ShowtimeRanking> rankings = new ArrayList<>();
        int rank = 1;
        for (Object showtimeId : showtimeIds) {
            Long id = Long.valueOf(showtimeId.toString());
            Double score = cacheManager.zScore(DAILY_RANKING_KEY, id);
            
            ShowtimeRanking ranking = new ShowtimeRanking();
            ranking.setRank(rank++);
            ranking.setShowtimeId(id);
            ranking.setScore(score);
            
            rankings.add(ranking);
        }
        
        return rankings;
    }
    
    /**
     * 获取演出的排名
     */
    public Long getShowtimeRank(Long showtimeId) {
        // 获取排名（从0开始，需要+1）
        Long rank = cacheManager.zReverseRank(DAILY_RANKING_KEY, showtimeId);
        
        return rank != null ? rank + 1 : null;
    }
    
    /**
     * 获取演出的热度分数
     */
    public Double getShowtimeScore(Long showtimeId) {
        return cacheManager.zScore(DAILY_RANKING_KEY, showtimeId);
    }
    
    /**
     * 重置日榜（每日定时任务）
     */
    public void resetDailyRanking() {
        log.info("重置日榜");
        cacheManager.delete(DAILY_RANKING_KEY);
    }
    
    /**
     * 重置周榜（每周定时任务）
     */
    public void resetWeeklyRanking() {
        log.info("重置周榜");
        cacheManager.delete(WEEKLY_RANKING_KEY);
    }
}

/**
 * 演出排名VO
 */
@Data
public class ShowtimeRanking {
    private Integer rank;
    private Long showtimeId;
    private Double score;
}
```

### 4. Set操作

```java
/**
 * Set操作示例：座位占用标记
 */
@Service
@RequiredArgsConstructor
public class SeatOccupancyService {
    
    private final CacheManager cacheManager;
    
    /**
     * 标记座位已占用
     */
    public boolean occupySeats(Long showtimeId, List<String> seatNos) {
        String key = "showtime:occupied:" + showtimeId;
        
        // 批量添加到Set
        for (String seatNo : seatNos) {
            cacheManager.sAdd(key, seatNo);
        }
        
        // 设置过期时间（演出结束后自动清理）
        cacheManager.expire(key, Duration.ofDays(7));
        
        return true;
    }
    
    /**
     * 检查座位是否已占用
     */
    public boolean isSeatOccupied(Long showtimeId, String seatNo) {
        String key = "showtime:occupied:" + showtimeId;
        
        return cacheManager.sIsMember(key, seatNo);
    }
    
    /**
     * 批量检查座位占用情况
     */
    public Map<String, Boolean> batchCheckSeatsOccupied(Long showtimeId, List<String> seatNos) {
        String key = "showtime:occupied:" + showtimeId;
        
        Map<String, Boolean> result = new HashMap<>();
        for (String seatNo : seatNos) {
            result.put(seatNo, cacheManager.sIsMember(key, seatNo));
        }
        
        return result;
    }
    
    /**
     * 释放座位（订单取消/过期）
     */
    public void releaseSeats(Long showtimeId, List<String> seatNos) {
        String key = "showtime:occupied:" + showtimeId;
        
        for (String seatNo : seatNos) {
            cacheManager.sRemove(key, seatNo);
        }
    }
    
    /**
     * 获取已占用座位总数
     */
    public long getOccupiedCount(Long showtimeId) {
        String key = "showtime:occupied:" + showtimeId;
        
        return cacheManager.sSize(key);
    }
    
    /**
     * 获取所有已占用座位
     */
    public Set<String> getAllOccupiedSeats(Long showtimeId) {
        String key = "showtime:occupied:" + showtimeId;
        
        return cacheManager.sMembers(key, String.class);
    }
}
```

---

## 缓存更新策略

### 1. Cache-Aside Pattern（旁路缓存）

```java
/**
 * 旁路缓存模式：最常用的缓存模式
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheAsideService {
    
    private final CacheManager cacheManager;
    private final ShowtimeMapper showtimeMapper;
    
    /**
     * 读取数据
     */
    public Showtime getShowtime(Long id) {
        String key = "showtime:" + id;
        
        // 1. 先查缓存
        Showtime cached = cacheManager.get(key, Showtime.class);
        if (cached != null) {
            return cached;
        }
        
        // 2. 缓存未命中，查询数据库
        Showtime showtime = showtimeMapper.selectById(id);
        
        // 3. 写入缓存
        if (showtime != null) {
            cacheManager.set(key, showtime, Duration.ofMinutes(30));
        }
        
        return showtime;
    }
    
    /**
     * 更新数据
     */
    public void updateShowtime(Showtime showtime) {
        // 1. 先更新数据库
        showtimeMapper.updateById(showtime);
        
        // 2. 再删除缓存（而不是更新缓存）
        String key = "showtime:" + showtime.getId();
        cacheManager.delete(key);
        
        log.info("演出{}已更新，缓存已删除", showtime.getId());
    }
}
```

### 2. Write-Through Pattern（写穿缓存）

```java
/**
 * 写穿缓存模式：同时更新缓存和数据库
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WriteThroughService {
    
    private final CacheManager cacheManager;
    private final ShowtimeMapper showtimeMapper;
    
    /**
     * 更新数据（同时更新数据库和缓存）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateShowtime(Showtime showtime) {
        // 1. 更新数据库
        showtimeMapper.updateById(showtime);
        
        // 2. 同时更新缓存
        String key = "showtime:" + showtime.getId();
        cacheManager.set(key, showtime, Duration.ofMinutes(30));
        
        log.info("演出{}已更新，缓存已同步", showtime.getId());
    }
}
```

### 3. Write-Behind Pattern（写回缓存）

```java
/**
 * 写回缓存模式：先更新缓存，异步批量写入数据库
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WriteBehindService {
    
    private final CacheManager cacheManager;
    private final ShowtimeMapper showtimeMapper;
    private final Queue<Showtime> writeQueue = new ConcurrentLinkedQueue<>();
    
    /**
     * 更新数据（先写缓存）
     */
    public void updateShowtime(Showtime showtime) {
        // 1. 立即更新缓存
        String key = "showtime:" + showtime.getId();
        cacheManager.set(key, showtime, Duration.ofMinutes(30));
        
        // 2. 加入写队列（异步写入数据库）
        writeQueue.offer(showtime);
        
        log.info("演出{}已更新到缓存，等待写入数据库", showtime.getId());
    }
    
    /**
     * 定时任务：批量刷新到数据库
     */
    @Scheduled(fixedRate = 5000) // 每5秒执行一次
    public void flushToDatabase() {
        if (writeQueue.isEmpty()) {
            return;
        }
        
        log.info("开始批量刷新缓存到数据库，队列大小: {}", writeQueue.size());
        
        List<Showtime> batch = new ArrayList<>();
        Showtime showtime;
        while ((showtime = writeQueue.poll()) != null && batch.size() < 100) {
            batch.add(showtime);
        }
        
        if (!batch.isEmpty()) {
            // 批量更新数据库
            batch.forEach(showtimeMapper::updateById);
            log.info("批量刷新完成，共{}条记录", batch.size());
        }
    }
}
```

---

## 缓存预热

```java
/**
 * 缓存预热服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheWarmUpService {
    
    private final CacheManager cacheManager;
    private final ShowtimeMapper showtimeMapper;
    private final OrderMapper orderMapper;
    
    /**
     * 应用启动时预热热门数据
     */
    @PostConstruct
    public void warmUpOnStartup() {
        log.info("开始缓存预热...");
        
        // 1. 预热热门演出
        warmUpHotShowtimes();
        
        // 2. 预热热门场馆
        warmUpHotVenues();
        
        log.info("缓存预热完成");
    }
    
    /**
     * 预热热门演出
     */
    private void warmUpHotShowtimes() {
        log.info("预热热门演出");
        
        // 查询最近30天内有订单的演出
        LocalDateTime startTime = LocalDateTime.now().minusDays(30);
        List<Long> hotShowtimeIds = orderMapper.selectHotShowtimeIds(startTime, 100);
        
        log.info("发现{}个热门演出", hotShowtimeIds.size());
        
        // 批量加载到缓存
        for (Long id : hotShowtimeIds) {
            Showtime showtime = showtimeMapper.selectById(id);
            if (showtime != null) {
                String key = "showtime:" + id;
                cacheManager.set(key, showtime, Duration.ofHours(1));
            }
        }
        
        log.info("热门演出预热完成");
    }
    
    /**
     * 预热热门场馆
     */
    private void warmUpHotVenues() {
        log.info("预热热门场馆");
        
        // 查询热门场馆
        List<String> hotVenues = showtimeMapper.selectHotVenues(20);
        
        for (String venue : hotVenues) {
            // 查询该场馆的演出列表
            List<Showtime> showtimes = showtimeMapper.selectByVenue(venue);
            
            String key = "venue:showtimes:" + venue;
            cacheManager.set(key, showtimes, Duration.ofMinutes(30));
        }
        
        log.info("热门场馆预热完成");
    }
    
    /**
     * 定时预热（每小时执行一次）
     */
    @Scheduled(fixedRate = 3600000)
    public void scheduledWarmUp() {
        log.info("定时缓存预热开始");
        warmUpHotShowtimes();
        warmUpHotVenues();
        log.info("定时缓存预热完成");
    }
}
```

---

## 缓存问题处理

### 1. 缓存穿透（查询不存在的数据）

```java
/**
 * 缓存穿透解决方案：布隆过滤器 + 空值缓存
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CachePenetrationService {
    
    private final CacheManager cacheManager;
    private final ShowtimeMapper showtimeMapper;
    private final BloomFilter<Long> showtimeBloomFilter;
    
    /**
     * 查询演出（防止缓存穿透）
     */
    public Showtime getShowtime(Long id) {
        String key = "showtime:" + id;
        
        // 1. 布隆过滤器判断数据是否存在
        if (!showtimeBloomFilter.mightContain(id)) {
            log.warn("布隆过滤器判断演出{}不存在", id);
            return null;
        }
        
        // 2. 查询缓存
        Showtime cached = cacheManager.get(key, Showtime.class);
        if (cached != null) {
            // 检查是否是空值缓存
            if (cached.getId() == null) {
                return null;
            }
            return cached;
        }
        
        // 3. 查询数据库
        Showtime showtime = showtimeMapper.selectById(id);
        
        if (showtime != null) {
            // 4. 写入缓存
            cacheManager.set(key, showtime, Duration.ofMinutes(30));
        } else {
            // 5. 缓存空值，防止缓存穿透
            Showtime emptyShowtime = new Showtime();
            cacheManager.set(key, emptyShowtime, Duration.ofMinutes(5)); // 空值TTL较短
            log.info("缓存空值，防止穿透：{}", key);
        }
        
        return showtime;
    }
    
    /**
     * 初始化布隆过滤器
     */
    @PostConstruct
    public void initBloomFilter() {
        log.info("初始化布隆过滤器");
        
        // 查询所有演出ID
        List<Long> allShowtimeIds = showtimeMapper.selectAllIds();
        
        // 添加到布隆过滤器
        for (Long id : allShowtimeIds) {
            showtimeBloomFilter.put(id);
        }
        
        log.info("布隆过滤器初始化完成，共{}条数据", allShowtimeIds.size());
    }
}
```

### 2. 缓存击穿（热点数据过期）

```java
/**
 * 缓存击穿解决方案：分布式锁 + 永不过期
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheBreakdownService {
    
    private final CacheManager cacheManager;
    private final ShowtimeMapper showtimeMapper;
    private final LockService lockService;
    
    /**
     * 查询热门演出（防止缓存击穿）
     */
    public Showtime getHotShowtime(Long id) {
        String key = "hot:showtime:" + id;
        
        // 1. 查询缓存
        Showtime cached = cacheManager.get(key, Showtime.class);
        if (cached != null) {
            return cached;
        }
        
        // 2. 缓存未命中，使用分布式锁
        String lockKey = "lock:showtime:" + id;
        boolean locked = lockService.tryLock(lockKey, 10, TimeUnit.SECONDS);
        
        if (!locked) {
            // 获取锁失败，等待后重试
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return getHotShowtime(id); // 重试
        }
        
        try {
            // 3. 双重检查
            cached = cacheManager.get(key, Showtime.class);
            if (cached != null) {
                return cached;
            }
            
            // 4. 查询数据库
            Showtime showtime = showtimeMapper.selectById(id);
            
            if (showtime != null) {
                // 5. 写入缓存（永不过期，通过后台线程定时更新）
                cacheManager.set(key, showtime, Duration.ofDays(365));
            }
            
            return showtime;
        } finally {
            // 6. 释放锁
            lockService.unlock(lockKey);
        }
    }
    
    /**
     * 定时更新热门数据（防止数据过期）
     */
    @Scheduled(fixedRate = 600000) // 每10分钟执行一次
    public void refreshHotShowtimes() {
        log.info("定时刷新热门演出");
        
        List<Long> hotIds = getHotShowtimeIds();
        
        for (Long id : hotIds) {
            String key = "hot:showtime:" + id;
            
            // 查询最新数据
            Showtime showtime = showtimeMapper.selectById(id);
            if (showtime != null) {
                // 更新缓存（不改变过期时间）
                cacheManager.set(key, showtime, Duration.ofDays(365));
            }
        }
        
        log.info("热门演出刷新完成");
    }
    
    private List<Long> getHotShowtimeIds() {
        // 获取热门演出ID列表
        // 此处省略实现
        return Arrays.asList(1L, 2L, 3L);
    }
}
```

### 3. 缓存雪崩（大量缓存同时过期）

```java
/**
 * 缓存雪崩解决方案：随机过期时间 + 服务降级
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheAvalancheService {
    
    private final CacheManager cacheManager;
    private final ShowtimeMapper showtimeMapper;
    private final Random random = new Random();
    
    /**
     * 查询演出（防止缓存雪崩）
     */
    public Showtime getShowtime(Long id) {
        String key = "showtime:" + id;
        
        return cacheManager.getOrSet(key, Showtime.class, () -> {
            return showtimeMapper.selectById(id);
        }, getRandomTTL()); // 使用随机过期时间
    }
    
    /**
     * 获取随机过期时间（30±5分钟）
     * 防止大量缓存同时过期
     */
    private Duration getRandomTTL() {
        // 基础TTL: 30分钟
        long baseTTL = 30;
        // 随机偏移: ±5分钟
        long offset = random.nextInt(11) - 5;
        
        return Duration.ofMinutes(baseTTL + offset);
    }
    
    /**
     * 批量查询演出（使用不同的过期时间）
     */
    public List<Showtime> batchGetShowtimes(List<Long> ids) {
        return ids.stream()
                .map(id -> {
                    String key = "showtime:" + id;
                    return cacheManager.getOrSet(key, Showtime.class, () -> {
                        return showtimeMapper.selectById(id);
                    }, getRandomTTL());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * 服务降级：缓存失效时返回默认数据
     */
    public Showtime getShowtimeWithFallback(Long id) {
        try {
            return getShowtime(id);
        } catch (Exception e) {
            log.error("查询演出失败，使用降级策略", e);
            
            // 返回默认数据
            Showtime fallback = new Showtime();
            fallback.setId(id);
            fallback.setName("演出暂时不可用");
            fallback.setStatus("UNAVAILABLE");
            
            return fallback;
        }
    }
}
```

---

## 票务系统完整示例

### 完整购票流程的缓存应用

```java
/**
 * 票务购买服务（完整缓存应用）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketPurchaseServiceWithCache {
    
    private final CacheManager cacheManager;
    private final ShowtimeService showtimeService;
    private final OrderService orderService;
    private final TicketService ticketService;
    private final SeatOccupancyService seatOccupancyService;
    private final ShowtimeRankingService rankingService;
    private final LockService lockService;
    
    /**
     * 完整购票流程
     */
    @Transactional(rollbackFor = Exception.class)
    public PurchaseResult purchase(PurchaseRequest request) {
        Long userId = request.getUserId();
        Long showtimeId = request.getShowtimeId();
        List<String> seatNos = request.getSeatNos();
        
        log.info("开始购票：用户={}, 演出={}, 座位={}", userId, showtimeId, seatNos);
        
        // 1. 从缓存查询演出信息（多级缓存）
        Showtime showtime = getShowtimeFromCache(showtimeId);
        if (showtime == null) {
            throw new BusinessException("演出不存在");
        }
        
        // 2. 检查座位是否已占用（使用Set缓存）
        checkSeatsAvailable(showtimeId, seatNos);
        
        // 3. 使用分布式锁锁定座位
        String lockKey = "lock:purchase:" + showtimeId;
        boolean locked = lockService.tryLock(lockKey, 30, TimeUnit.SECONDS);
        if (!locked) {
            throw new BusinessException("系统繁忙，请稍后重试");
        }
        
        try {
            // 4. 再次检查座位（双重检查）
            checkSeatsAvailable(showtimeId, seatNos);
            
            // 5. 标记座位已占用（Set缓存）
            seatOccupancyService.occupySeats(showtimeId, seatNos);
            
            // 6. 扣减库存（缓存+数据库）
            decrementStock(showtimeId, seatNos.size());
            
            // 7. 创建订单
            String orderNo = orderService.createOrder(userId, showtimeId, seatNos.size(), String.join(",", seatNos));
            
            // 8. 缓存订单详情（Hash缓存）
            Order order = orderService.getOrderByOrderNo(orderNo);
            cacheOrderDetails(order);
            
            // 9. 生成电子票
            List<String> ticketNos = ticketService.batchGenerateTickets(order);
            
            // 10. 更新演出热度（ZSet排行榜）
            rankingService.incrementShowtimeScore(showtimeId, 10.0);
            
            // 11. 增加浏览计数（String计数器）
            incrementPurchaseCount(showtimeId);
            
            log.info("购票成功：订单号={}", orderNo);
            
            return PurchaseResult.builder()
                    .success(true)
                    .orderNo(orderNo)
                    .ticketNos(ticketNos)
                    .totalAmount(order.getTotalAmount())
                    .build();
        } finally {
            lockService.unlock(lockKey);
        }
    }
    
    /**
     * 从多级缓存查询演出
     */
    private Showtime getShowtimeFromCache(Long showtimeId) {
        String key = "showtime:" + showtimeId;
        
        return cacheManager.getOrSet(key, Showtime.class, () -> {
            return showtimeService.getById(showtimeId);
        }, Duration.ofMinutes(30));
    }
    
    /**
     * 检查座位是否可用
     */
    private void checkSeatsAvailable(Long showtimeId, List<String> seatNos) {
        for (String seatNo : seatNos) {
            if (seatOccupancyService.isSeatOccupied(showtimeId, seatNo)) {
                throw new BusinessException("座位 " + seatNo + " 已被占用");
            }
        }
    }
    
    /**
     * 扣减库存（缓存优先）
     */
    private void decrementStock(Long showtimeId, int quantity) {
        String key = "showtime:stock:" + showtimeId;
        
        // 1. 尝试从缓存扣减
        Long remaining = cacheManager.decrement(key, (long) quantity);
        
        if (remaining == null || remaining < 0) {
            // 2. 缓存不存在或库存不足，从数据库查询
            Showtime showtime = showtimeService.getById(showtimeId);
            if (showtime.getAvailableSeats() < quantity) {
                throw new BusinessException("库存不足");
            }
            
            // 3. 更新数据库
            boolean success = showtimeService.updateAvailableSeats(showtimeId, quantity);
            if (!success) {
                throw new BusinessException("库存扣减失败");
            }
            
            // 4. 同步到缓存
            cacheManager.set(key, showtime.getAvailableSeats() - quantity, Duration.ofMinutes(30));
        } else {
            // 5. 缓存扣减成功，异步更新数据库
            CompletableFuture.runAsync(() -> {
                showtimeService.updateAvailableSeats(showtimeId, quantity);
            });
        }
    }
    
    /**
     * 缓存订单详情
     */
    private void cacheOrderDetails(Order order) {
        String key = "order:" + order.getOrderNo();
        
        // 使用Hash存储订单详情
        Map<String, Object> fields = new HashMap<>();
        fields.put("orderNo", order.getOrderNo());
        fields.put("userId", order.getUserId());
        fields.put("showtimeId", order.getShowtimeId());
        fields.put("quantity", order.getQuantity());
        fields.put("totalAmount", order.getTotalAmount().toString());
        fields.put("status", order.getStatus());
        fields.put("seats", order.getSeats());
        
        cacheManager.hMSet(key, fields);
        cacheManager.expire(key, Duration.ofHours(24));
    }
    
    /**
     * 增加购买次数统计
     */
    private void incrementPurchaseCount(Long showtimeId) {
        String key = "showtime:purchase:count:" + showtimeId;
        cacheManager.increment(key, 1L);
        cacheManager.expire(key, Duration.ofDays(30));
    }
}
```

---

## 最佳实践

### 1. 缓存键命名规范

```java
/**
 * 缓存键管理工具
 */
public class CacheKeyUtils {
    
    // 业务前缀
    private static final String PREFIX_SHOWTIME = "showtime:";
    private static final String PREFIX_ORDER = "order:";
    private static final String PREFIX_USER = "user:";
    private static final String PREFIX_TICKET = "ticket:";
    
    // 功能后缀
    private static final String SUFFIX_DETAILS = ":details";
    private static final String SUFFIX_LIST = ":list";
    private static final String SUFFIX_COUNT = ":count";
    private static final String SUFFIX_LOCK = ":lock";
    
    /**
     * 演出详情键
     */
    public static String showtimeKey(Long id) {
        return PREFIX_SHOWTIME + id;
    }
    
    /**
     * 演出库存键
     */
    public static String showtimeStockKey(Long id) {
        return PREFIX_SHOWTIME + "stock:" + id;
    }
    
    /**
     * 订单详情键
     */
    public static String orderKey(String orderNo) {
        return PREFIX_ORDER + orderNo;
    }
    
    /**
     * 用户订单列表键
     */
    public static String userOrdersKey(Long userId) {
        return PREFIX_USER + userId + SUFFIX_LIST;
    }
    
    /**
     * 座位锁定键
     */
    public static String seatLockKey(Long showtimeId) {
        return "seat" + SUFFIX_LOCK + ":" + showtimeId;
    }
}
```

### 2. 缓存过期时间设置

```java
/**
 * 缓存TTL配置
 */
public class CacheTTL {
    
    // 热点数据：1小时
    public static final Duration HOT_DATA_TTL = Duration.ofHours(1);
    
    // 普通数据：30分钟
    public static final Duration NORMAL_DATA_TTL = Duration.ofMinutes(30);
    
    // 临时数据：5分钟
    public static final Duration TEMP_DATA_TTL = Duration.ofMinutes(5);
    
    // 空值缓存：3分钟
    public static final Duration NULL_VALUE_TTL = Duration.ofMinutes(3);
    
    // 计数器：1天
    public static final Duration COUNTER_TTL = Duration.ofDays(1);
    
    // 排行榜：6小时
    public static final Duration RANKING_TTL = Duration.ofHours(6);
}
```

### 3. 缓存监控

```java
/**
 * 缓存监控服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheMonitorService {
    
    private final CacheManager cacheManager;
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    
    /**
     * 查询数据（带监控）
     */
    public <T> T getWithMonitoring(String key, Class<T> clazz, Supplier<T> loader) {
        T cached = cacheManager.get(key, clazz);
        
        if (cached != null) {
            hitCount.incrementAndGet();
            return cached;
        }
        
        missCount.incrementAndGet();
        
        T data = loader.get();
        if (data != null) {
            cacheManager.set(key, data, Duration.ofMinutes(30));
        }
        
        return data;
    }
    
    /**
     * 获取缓存命中率
     */
    public double getHitRate() {
        long total = hitCount.get() + missCount.get();
        if (total == 0) {
            return 0.0;
        }
        
        return (double) hitCount.get() / total * 100;
    }
    
    /**
     * 定时打印缓存统计
     */
    @Scheduled(fixedRate = 60000) // 每分钟打印一次
    public void printStats() {
        log.info("缓存统计 - 命中: {}, 未命中: {}, 命中率: {:.2f}%",
                hitCount.get(), missCount.get(), getHitRate());
    }
    
    /**
     * 重置统计
     */
    public void resetStats() {
        hitCount.set(0);
        missCount.set(0);
    }
}
```

### 4. 缓存一致性保证

- **延迟双删策略**：删除缓存 → 更新数据库 → 延迟删除缓存
- **订阅Binlog**：监听数据库变更，自动更新缓存
- **设置合理的TTL**：即使出现不一致，也会在TTL后恢复一致

### 5. 缓存容量规划

- **L1缓存（本地）**：10,000个对象，约100MB
- **L2缓存（Redis）**：根据业务量调整，通常1-10GB
- **热点数据优先**：将访问频繁的数据缓存到L1

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [CONFIG.md](./CONFIG.md) - 配置指南（包含票务系统配置）
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 发展路线图

---

**最后更新**: 2025-11-20  
**文档版本**: v2.0

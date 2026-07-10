# Nebula Lock Redis - 使用示例

> Redis分布式锁完整使用指南，以票务系统防超卖为例

## 目录

- [快速开始](#快速开始)
- [注解式锁](#注解式锁)
- [编程式锁](#编程式锁)
- [读写锁](#读写锁)
- [可重入锁](#可重入锁)
- [公平锁](#公平锁)
- [信号量](#信号量)
- [票务系统完整示例](#票务系统完整示例)
- [最佳实践](#最佳实践)

---

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-lock-redis</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 基础配置

```yaml
nebula:
  lock:
    enabled: true
    default-wait-time: 30s      # 默认等待获取锁的时间
    default-lease-time: 60s     # 默认锁持有的最长时间
    enable-watchdog: true       # 启用看门狗自动续期
    watchdog-renewal-interval: 10s  # 看门狗续期间隔

spring:
  redis:
    host: localhost
    port: 6379
    password: password
    database: 0
```

---

## 注解式锁

### 1. 基础注解锁

```java
/**
 * 注解式锁基础示例
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderPaymentService {
    
    /**
     * 防止订单重复支付
     * key: 使用SpEL表达式生成锁键
     * waitTime: 等待获取锁的时间（秒）
     * leaseTime: 锁持有的最长时间（秒）
     */
    @Locked(key = "'pay:order:' + #orderNo", waitTime = 5, leaseTime = 30)
    public boolean payOrder(String orderNo) {
        log.info("处理订单支付：{}", orderNo);
        
        // 1. 检查订单状态
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException("订单状态不正确");
        }
        
        // 2. 调用支付接口
        boolean paymentSuccess = paymentGateway.pay(order);
        
        // 3. 更新订单状态
        if (paymentSuccess) {
            order.setStatus("PAID");
            order.setPayTime(LocalDateTime.now());
            orderMapper.updateById(order);
        }
        
        log.info("订单{}支付完成", orderNo);
        
        return paymentSuccess;
    }
    
    /**
     * 使用SpEL表达式访问对象属性
     */
    @Locked(key = "'user:payment:' + #request.userId", waitTime = 3, leaseTime = 10)
    public void processUserPayment(PaymentRequest request) {
        // 防止同一用户并发支付
        log.info("处理用户{}的支付请求", request.getUserId());
        // 业务逻辑
    }
}
```

### 2. 自定义失败策略

```java
/**
 * 自定义失败策略示例
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledJobService {
    
    /**
     * 定时任务（获取锁失败时跳过）
     * failStrategy = FailStrategy.SKIP: 获取锁失败时不抛异常，直接跳过
     */
    @Scheduled(cron = "0 */5 * * * ?")
    @Locked(key = "'job:cancel-expired-orders'", waitTime = 0, failStrategy = FailStrategy.SKIP)
    public void cancelExpiredOrders() {
        log.info("开始取消过期订单任务");
        
        // 只有一个实例能获取到锁并执行
        LocalDateTime expireTime = LocalDateTime.now();
        int count = orderService.cancelExpiredOrders(expireTime);
        
        log.info("取消过期订单任务完成，共取消{}个订单", count);
    }
    
    /**
     * 定时任务（获取锁失败时抛异常）
     * failStrategy = FailStrategy.EXCEPTION: 获取锁失败时抛出异常（默认行为）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Locked(key = "'job:daily-report'", waitTime = 5, failStrategy = FailStrategy.EXCEPTION)
    public void generateDailyReport() {
        log.info("开始生成日报");
        
        // 如果获取锁失败，会抛出异常
        reportService.generateDailyReport();
        
        log.info("日报生成完成");
    }
}
```

### 3. 看门狗机制

```java
/**
 * 看门狗自动续期示例
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LongRunningTaskService {
    
    /**
     * 长时间运行的任务（使用看门狗自动续期）
     * leaseTime = -1: 启用看门狗，自动续期，任务完成后释放锁
     */
    @Locked(key = "'task:export:' + #taskId", waitTime = 10, leaseTime = -1)
    public void exportLargeDataset(String taskId) {
        log.info("开始导出大数据集：{}", taskId);
        
        // 模拟长时间运行的任务（可能需要几分钟）
        for (int i = 0; i < 100; i++) {
            processDataBatch(i);
            try {
                Thread.sleep(1000); // 每批处理1秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("任务被中断");
            }
        }
        
        log.info("数据集导出完成：{}", taskId);
        
        // 方法执行完成后，锁会自动释放
    }
    
    private void processDataBatch(int batchNo) {
        log.debug("处理数据批次：{}", batchNo);
        // 数据处理逻辑
    }
}
```

---

## 编程式锁

### 1. 基础编程式锁

```java
/**
 * 编程式锁基础示例
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockDeductionService {
    
    private final LockManager lockManager;
    private final ShowtimeMapper showtimeMapper;
    
    /**
     * 扣减演出库存（防止超卖）
     */
    public boolean deductStock(Long showtimeId, int quantity) {
        String lockKey = "stock:showtime:" + showtimeId;
        
        // 获取锁对象
        Lock lock = lockManager.getLock(lockKey);
        
        try {
            // 尝试加锁（等待10秒，持有30秒）
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                try {
                    log.info("获取锁成功，开始扣减库存：演出={}, 数量={}", showtimeId, quantity);
                    
                    // 1. 查询当前库存
                    Showtime showtime = showtimeMapper.selectById(showtimeId);
                    if (showtime == null) {
                        throw new BusinessException("演出不存在");
                    }
                    
                    // 2. 检查库存是否足够
                    if (showtime.getAvailableSeats() < quantity) {
                        log.warn("库存不足：演出={}, 当前库存={}, 需要={}", 
                                showtimeId, showtime.getAvailableSeats(), quantity);
                        return false;
                    }
                    
                    // 3. 扣减库存
                    showtime.setAvailableSeats(showtime.getAvailableSeats() - quantity);
                    int updated = showtimeMapper.updateById(showtime);
                    
                    if (updated > 0) {
                        log.info("库存扣减成功：演出={}, 剩余库存={}", 
                                showtimeId, showtime.getAvailableSeats());
                        return true;
                    } else {
                        log.warn("库存扣减失败：数据库更新失败");
                        return false;
                    }
                } finally {
                    // 4. 释放锁
                    lock.unlock();
                    log.info("锁已释放：{}", lockKey);
                }
            } else {
                // 获取锁失败（超时）
                log.warn("获取锁失败（超时）：{}", lockKey);
                throw new BusinessException("系统繁忙，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取锁被中断：{}", lockKey, e);
            throw new RuntimeException("操作被中断");
        }
    }
    
    /**
     * 带重试的库存扣减
     */
    public boolean deductStockWithRetry(Long showtimeId, int quantity, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                return deductStock(showtimeId, quantity);
            } catch (BusinessException e) {
                if (e.getMessage().contains("系统繁忙")) {
                    log.info("第{}次重试扣减库存", i + 1);
                    try {
                        Thread.sleep(100 * (i + 1)); // 递增延迟
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试被中断");
                    }
                } else {
                    throw e;
                }
            }
        }
        
        throw new BusinessException("库存扣减失败，已达最大重试次数");
    }
}
```

### 2. 锁的自动释放

```java
/**
 * 使用try-with-resources自动释放锁
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoReleaseLockService {
    
    private final LockManager lockManager;
    
    /**
     * 自动释放锁示例
     */
    public void processOrderWithAutoRelease(String orderNo) {
        String lockKey = "order:process:" + orderNo;
        Lock lock = lockManager.getLock(lockKey);
        
        try {
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try (lock) { // 使用try-with-resources自动释放
                    log.info("开始处理订单：{}", orderNo);
                    
                    // 业务逻辑
                    processOrder(orderNo);
                    
                    log.info("订单处理完成：{}", orderNo);
                } // 离开作用域时自动释放锁
            } else {
                throw new BusinessException("订单处理中，请稍后");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("操作被中断");
        }
    }
    
    private void processOrder(String orderNo) {
        // 订单处理逻辑
    }
}
```

---

## 读写锁

### 1. 读写锁基础用法

```java
/**
 * 读写锁示例（适用于读多写少场景）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShowtimeReadWriteService {
    
    private final LockManager lockManager;
    private final ShowtimeMapper showtimeMapper;
    private final CacheManager cacheManager;
    
    /**
     * 读取演出信息（使用读锁）
     * 多个线程可以同时持有读锁
     */
    public Showtime getShowtime(Long showtimeId) {
        String lockKey = "showtime:rw:" + showtimeId;
        ReadWriteLock rwLock = lockManager.getReadWriteLock(lockKey);
        Lock readLock = rwLock.readLock();
        
        try {
            if (readLock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    log.info("获取读锁成功：{}", lockKey);
                    
                    // 先查缓存
                    String cacheKey = "showtime:" + showtimeId;
                    Showtime cached = cacheManager.get(cacheKey, Showtime.class);
                    if (cached != null) {
                        return cached;
                    }
                    
                    // 缓存未命中，查询数据库
                    Showtime showtime = showtimeMapper.selectById(showtimeId);
                    if (showtime != null) {
                        cacheManager.set(cacheKey, showtime, Duration.ofMinutes(30));
                    }
                    
                    return showtime;
                } finally {
                    readLock.unlock();
                    log.info("读锁已释放：{}", lockKey);
                }
            } else {
                throw new BusinessException("系统繁忙");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("操作被中断");
        }
    }
    
    /**
     * 更新演出信息（使用写锁）
     * 只有一个线程可以持有写锁，且此时不能有读锁
     */
    public void updateShowtime(Showtime showtime) {
        String lockKey = "showtime:rw:" + showtime.getId();
        ReadWriteLock rwLock = lockManager.getReadWriteLock(lockKey);
        Lock writeLock = rwLock.writeLock();
        
        try {
            if (writeLock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    log.info("获取写锁成功：{}", lockKey);
                    
                    // 1. 更新数据库
                    showtimeMapper.updateById(showtime);
                    
                    // 2. 清除缓存
                    String cacheKey = "showtime:" + showtime.getId();
                    cacheManager.delete(cacheKey);
                    
                    log.info("演出信息已更新：{}", showtime.getId());
                } finally {
                    writeLock.unlock();
                    log.info("写锁已释放：{}", lockKey);
                }
            } else {
                throw new BusinessException("系统繁忙");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("操作被中断");
        }
    }
}
```

### 2. 读写锁降级

```java
/**
 * 读写锁降级示例
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationService {
    
    private final LockManager lockManager;
    private volatile Map<String, String> configCache = new HashMap<>();
    
    /**
     * 读取配置（使用读锁）
     */
    public String getConfig(String key) {
        ReadWriteLock rwLock = lockManager.getReadWriteLock("config:lock");
        Lock readLock = rwLock.readLock();
        
        try {
            readLock.lock();
            return configCache.get(key);
        } finally {
            readLock.unlock();
        }
    }
    
    /**
     * 更新配置（写锁降级为读锁）
     */
    public void updateConfig(String key, String value) {
        ReadWriteLock rwLock = lockManager.getReadWriteLock("config:lock");
        Lock writeLock = rwLock.writeLock();
        Lock readLock = rwLock.readLock();
        
        try {
            // 1. 获取写锁
            writeLock.lock();
            
            // 2. 更新配置
            Map<String, String> newConfig = new HashMap<>(configCache);
            newConfig.put(key, value);
            
            // 3. 在释放写锁前获取读锁（锁降级）
            readLock.lock();
            
            // 4. 释放写锁
            writeLock.unlock();
            
            // 5. 在读锁保护下使用新配置
            configCache = newConfig;
            
            log.info("配置已更新：{}={}", key, value);
        } finally {
            // 6. 释放读锁
            readLock.unlock();
        }
    }
}
```

---

## 可重入锁

```java
/**
 * 可重入锁示例
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReentrantLockService {
    
    private final LockManager lockManager;
    
    /**
     * 外层方法（获取锁）
     */
    @Locked(key = "'user:' + #userId", waitTime = 5, leaseTime = 30)
    public void processUserData(Long userId) {
        log.info("外层方法：处理用户数据，用户ID={}", userId);
        
        // 调用内层方法（会再次尝试获取同一把锁）
        updateUserProfile(userId);
        updateUserSettings(userId);
        
        log.info("外层方法：用户数据处理完成，用户ID={}", userId);
    }
    
    /**
     * 内层方法1（可重入锁，同一线程可以再次获取）
     */
    @Locked(key = "'user:' + #userId", waitTime = 5, leaseTime = 30)
    public void updateUserProfile(Long userId) {
        log.info("内层方法1：更新用户资料，用户ID={}", userId);
        // 业务逻辑
    }
    
    /**
     * 内层方法2（可重入锁）
     */
    @Locked(key = "'user:' + #userId", waitTime = 5, leaseTime = 30)
    public void updateUserSettings(Long userId) {
        log.info("内层方法2：更新用户设置，用户ID={}", userId);
        // 业务逻辑
    }
    
    /**
     * 编程式可重入锁
     */
    public void reentrantLockExample(Long userId) {
        String lockKey = "user:reentrant:" + userId;
        Lock lock = lockManager.getLock(lockKey);
        
        try {
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    log.info("第一次获取锁成功");
                    
                    // 同一线程再次获取同一把锁（可重入）
                    if (lock.tryLock(1, 10, TimeUnit.SECONDS)) {
                        try {
                            log.info("第二次获取锁成功（可重入）");
                            // 业务逻辑
                        } finally {
                            lock.unlock();
                            log.info("第二次释放锁");
                        }
                    }
                } finally {
                    lock.unlock();
                    log.info("第一次释放锁");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

---

## 公平锁

```java
/**
 * 公平锁示例（先到先得）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FairLockService {
    
    private final LockManager lockManager;
    
    /**
     * 秒杀场景（公平锁，保证先到先得）
     */
    public boolean seckill(Long userId, Long showtimeId) {
        String lockKey = "seckill:" + showtimeId;
        
        // 获取公平锁
        Lock fairLock = lockManager.getFairLock(lockKey);
        
        try {
            if (fairLock.tryLock(3, 10, TimeUnit.SECONDS)) {
                try {
                    log.info("用户{}获取公平锁成功，开始秒杀", userId);
                    
                    // 秒杀逻辑
                    boolean success = processSeckill(userId, showtimeId);
                    
                    log.info("用户{}秒杀结果：{}", userId, success ? "成功" : "失败");
                    
                    return success;
                } finally {
                    fairLock.unlock();
                }
            } else {
                log.info("用户{}获取锁失败（超时）", userId);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    private boolean processSeckill(Long userId, Long showtimeId) {
        // 秒杀业务逻辑
        return true;
    }
}
```

---

## 信号量

```java
/**
 * 信号量示例（限流）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SemaphoreService {
    
    private final LockManager lockManager;
    
    /**
     * 限制并发访问数量（如限制同时处理的订单数）
     */
    public void processOrderWithLimit(String orderNo) {
        String semaphoreKey = "semaphore:order:process";
        
        // 获取信号量（允许最多10个并发）
        Semaphore semaphore = lockManager.getSemaphore(semaphoreKey, 10);
        
        try {
            // 尝试获取许可（等待5秒）
            if (semaphore.tryAcquire(5, TimeUnit.SECONDS)) {
                try {
                    log.info("获取信号量成功，开始处理订单：{}", orderNo);
                    
                    // 订单处理逻辑
                    processOrder(orderNo);
                    
                    log.info("订单处理完成：{}", orderNo);
                } finally {
                    // 释放许可
                    semaphore.release();
                    log.info("信号量已释放");
                }
            } else {
                log.warn("获取信号量失败（超时），订单：{}", orderNo);
                throw new BusinessException("系统繁忙，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("操作被中断");
        }
    }
    
    /**
     * 批量获取许可
     */
    public void batchProcessOrders(List<String> orderNos) {
        String semaphoreKey = "semaphore:order:batch";
        Semaphore semaphore = lockManager.getSemaphore(semaphoreKey, 10);
        
        int permits = Math.min(orderNos.size(), 5); // 最多一次获取5个许可
        
        try {
            if (semaphore.tryAcquire(permits, 10, TimeUnit.SECONDS)) {
                try {
                    log.info("批量获取{}个信号量成功", permits);
                    
                    // 批量处理订单
                    orderNos.stream().limit(permits).forEach(this::processOrder);
                    
                    log.info("批量处理完成");
                } finally {
                    semaphore.release(permits);
                    log.info("批量释放{}个信号量", permits);
                }
            } else {
                throw new BusinessException("系统繁忙");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("操作被中断");
        }
    }
    
    private void processOrder(String orderNo) {
        log.info("处理订单：{}", orderNo);
        // 业务逻辑
    }
}
```

---

## 票务系统完整示例

### 场景：完整的防超卖购票流程

```java
/**
 * 票务购买服务（完整防超卖方案）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketPurchaseServiceWithLock {
    
    private final LockManager lockManager;
    private final ShowtimeService showtimeService;
    private final OrderService orderService;
    private final TicketService ticketService;
    private final SeatService seatService;
    
    /**
     * 购买票务（完整防超卖方案）
     */
    @Transactional(rollbackFor = Exception.class)
    public PurchaseResult purchaseTickets(PurchaseRequest request) {
        Long userId = request.getUserId();
        Long showtimeId = request.getShowtimeId();
        List<String> seatNos = request.getSeatNos();
        int quantity = seatNos.size();
        
        log.info("开始购票：用户={}, 演出={}, 座位={}", userId, showtimeId, seatNos);
        
        // 1. 锁定演出（粗粒度锁，防止超卖）
        String showtimeLockKey = "lock:showtime:" + showtimeId;
        Lock showtimeLock = lockManager.getLock(showtimeLockKey);
        
        try {
            if (!showtimeLock.tryLock(10, 30, TimeUnit.SECONDS)) {
                throw new BusinessException("系统繁忙，请稍后重试");
            }
            
            try {
                log.info("获取演出锁成功：{}", showtimeLockKey);
                
                // 2. 检查演出信息
                Showtime showtime = showtimeService.getById(showtimeId);
                validateShowtime(showtime);
                
                // 3. 锁定座位（细粒度锁）
                List<Lock> seatLocks = lockSeats(showtimeId, seatNos);
                
                try {
                    // 4. 检查座位是否可用
                    validateSeats(showtimeId, seatNos);
                    
                    // 5. 扣减库存（乐观锁 + 分布式锁双重保护）
                    boolean stockReduced = deductStockWithRetry(showtimeId, quantity, 3);
                    if (!stockReduced) {
                        throw new BusinessException("库存不足");
                    }
                    
                    // 6. 标记座位已占用
                    seatService.occupySeats(showtimeId, seatNos);
                    
                    // 7. 创建订单
                    String orderNo = createOrder(userId, showtimeId, seatNos, showtime.getPrice());
                    
                    // 8. 生成电子票
                    List<String> ticketNos = generateTickets(orderNo, showtimeId, seatNos);
                    
                    log.info("购票成功：订单号={}, 票数={}", orderNo, ticketNos.size());
                    
                    return PurchaseResult.builder()
                            .success(true)
                            .orderNo(orderNo)
                            .ticketNos(ticketNos)
                            .totalAmount(showtime.getPrice().multiply(new BigDecimal(quantity)))
                            .build();
                } finally {
                    // 9. 释放座位锁
                    unlockSeats(seatLocks);
                }
            } finally {
                // 10. 释放演出锁
                showtimeLock.unlock();
                log.info("演出锁已释放：{}", showtimeLockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("购票操作被中断");
        }
    }
    
    /**
     * 锁定座位（批量）
     */
    private List<Lock> lockSeats(Long showtimeId, List<String> seatNos) throws InterruptedException {
        List<Lock> locks = new ArrayList<>();
        
        for (String seatNo : seatNos) {
            String lockKey = "lock:seat:" + showtimeId + ":" + seatNo;
            Lock seatLock = lockManager.getLock(lockKey);
            
            if (seatLock.tryLock(5, 30, TimeUnit.SECONDS)) {
                locks.add(seatLock);
                log.info("座位锁定成功：{}", seatNo);
            } else {
                // 锁定失败，释放已锁定的座位
                unlockSeats(locks);
                throw new BusinessException("座位 " + seatNo + " 正在被其他用户选择");
            }
        }
        
        return locks;
    }
    
    /**
     * 释放座位锁（批量）
     */
    private void unlockSeats(List<Lock> locks) {
        for (Lock lock : locks) {
            try {
                lock.unlock();
            } catch (Exception e) {
                log.error("释放座位锁失败", e);
            }
        }
        log.info("所有座位锁已释放，共{}个", locks.size());
    }
    
    /**
     * 验证演出
     */
    private void validateShowtime(Showtime showtime) {
        if (showtime == null) {
            throw new BusinessException("演出不存在");
        }
        
        if (!"UPCOMING".equals(showtime.getStatus())) {
            throw new BusinessException("演出已开始或已结束");
        }
        
        if (LocalDateTime.now().isAfter(showtime.getShowTime().minusHours(2))) {
            throw new BusinessException("演出开始前2小时停止售票");
        }
    }
    
    /**
     * 验证座位
     */
    private void validateSeats(Long showtimeId, List<String> seatNos) {
        for (String seatNo : seatNos) {
            if (seatService.isOccupied(showtimeId, seatNo)) {
                throw new BusinessException("座位 " + seatNo + " 已被占用");
            }
        }
    }
    
    /**
     * 扣减库存（带重试）
     */
    private boolean deductStockWithRetry(Long showtimeId, int quantity, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                boolean success = showtimeService.updateAvailableSeats(showtimeId, quantity);
                if (success) {
                    log.info("库存扣减成功：演出={}, 数量={}", showtimeId, quantity);
                    return true;
                }
                
                log.warn("库存扣减失败（乐观锁冲突），第{}次重试", i + 1);
                Thread.sleep(50 * (i + 1)); // 递增延迟
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("库存扣减被中断");
            }
        }
        
        return false;
    }
    
    /**
     * 创建订单
     */
    private String createOrder(Long userId, Long showtimeId, List<String> seatNos, BigDecimal price) {
        String seats = String.join(",", seatNos);
        int quantity = seatNos.size();
        
        return orderService.createOrder(userId, showtimeId, quantity, seats);
    }
    
    /**
     * 生成电子票
     */
    private List<String> generateTickets(String orderNo, Long showtimeId, List<String> seatNos) {
        Order order = orderService.getOrderByOrderNo(orderNo);
        return ticketService.batchGenerateTickets(order);
    }
}
```

---

## 最佳实践

### 1. 锁的粒度设计

```java
/**
 * 锁粒度设计最佳实践
 */
public class LockGranularityBestPractices {
    
    /**
     * ❌ 错误示例：粗粒度锁（锁整个演出）
     * 问题：所有用户购票都要等待，并发性能差
     */
    public void purchaseTicketsWrong(Long showtimeId, String seatNo) {
        String lockKey = "lock:showtime:" + showtimeId; // 锁整个演出
        Lock lock = lockManager.getLock(lockKey);
        
        // ... 业务逻辑
    }
    
    /**
     * ✅ 正确示例：细粒度锁（锁具体座位）
     * 优点：不同座位可以并发购买，提高系统吞吐量
     */
    public void purchaseTicketsCorrect(Long showtimeId, String seatNo) {
        String lockKey = "lock:seat:" + showtimeId + ":" + seatNo; // 锁具体座位
        Lock lock = lockManager.getLock(lockKey);
        
        // ... 业务逻辑
    }
}
```

### 2. 锁的超时处理

```java
/**
 * 锁超时处理最佳实践
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LockTimeoutBestPractices {
    
    private final LockManager lockManager;
    
    /**
     * ✅ 正确示例：设置合理的超时时间
     */
    public void processWithTimeout(String orderId) {
        String lockKey = "order:" + orderId;
        Lock lock = lockManager.getLock(lockKey);
        
        try {
            // waitTime: 根据业务场景设置合理的等待时间
            // leaseTime: 根据业务执行时间设置锁持有时间
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    // 业务逻辑
                    processOrder(orderId);
                } finally {
                    lock.unlock();
                }
            } else {
                // 获取锁失败，友好提示用户
                throw new BusinessException("系统繁忙，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("锁等待被中断", e);
            throw new RuntimeException("操作被中断");
        }
    }
    
    private void processOrder(String orderId) {
        // 业务逻辑
    }
}
```

### 3. 死锁避免

```java
/**
 * 死锁避免最佳实践
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeadlockAvoidanceBestPractices {
    
    private final LockManager lockManager;
    
    /**
     * ❌ 错误示例：可能导致死锁
     */
    public void transferWrong(String from, String to, BigDecimal amount) {
        Lock lockFrom = lockManager.getLock("account:" + from);
        Lock lockTo = lockManager.getLock("account:" + to);
        
        try {
            lockFrom.lock();
            // 如果另一个线程先锁定lockTo，再尝试锁定lockFrom，就会死锁
            lockTo.lock();
            
            // 转账逻辑
        } finally {
            lockTo.unlock();
            lockFrom.unlock();
        }
    }
    
    /**
     * ✅ 正确示例：统一加锁顺序，避免死锁
     */
    public void transferCorrect(String from, String to, BigDecimal amount) {
        // 1. 确定加锁顺序（按字典序）
        String first = from.compareTo(to) < 0 ? from : to;
        String second = from.compareTo(to) < 0 ? to : from;
        
        Lock lockFirst = lockManager.getLock("account:" + first);
        Lock lockSecond = lockManager.getLock("account:" + second);
        
        try {
            // 2. 按顺序加锁
            if (lockFirst.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    if (lockSecond.tryLock(5, 30, TimeUnit.SECONDS)) {
                        try {
                            // 3. 转账逻辑
                            doTransfer(from, to, amount);
                        } finally {
                            lockSecond.unlock();
                        }
                    } else {
                        throw new BusinessException("系统繁忙");
                    }
                } finally {
                    lockFirst.unlock();
                }
            } else {
                throw new BusinessException("系统繁忙");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("操作被中断");
        }
    }
    
    private void doTransfer(String from, String to, BigDecimal amount) {
        log.info("转账：{} -> {}, 金额：{}", from, to, amount);
        // 转账逻辑
    }
}
```

### 4. 看门狗使用建议

```java
/**
 * 看门狗使用最佳实践
 */
public class WatchdogBestPractices {
    
    /**
     * ✅ 场景1：业务执行时间不确定时使用看门狗
     */
    @Locked(key = "'export:' + #taskId", waitTime = 10, leaseTime = -1) // leaseTime = -1 启用看门狗
    public void exportLargeData(String taskId) {
        // 数据导出可能需要几分钟到几十分钟，时间不确定
        // 使用看门狗自动续期，确保任务执行期间锁不会过期
    }
    
    /**
     * ✅ 场景2：业务执行时间确定时不使用看门狗
     */
    @Locked(key = "'pay:' + #orderNo", waitTime = 5, leaseTime = 30) // 明确指定leaseTime
    public void payOrder(String orderNo) {
        // 支付通常在几秒内完成，设置30秒足够
        // 不需要看门狗续期，避免额外开销
    }
}
```

### 5. 分布式锁与数据库锁结合

```java
/**
 * 分布式锁与数据库锁结合使用
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HybridLockBestPractices {
    
    private final LockManager lockManager;
    private final ShowtimeMapper showtimeMapper;
    
    /**
     * ✅ 最佳实践：分布式锁 + 数据库乐观锁
     * 分布式锁：减少数据库压力，提高并发性能
     * 乐观锁：保证数据最终一致性
     */
    public boolean deductStockHybrid(Long showtimeId, int quantity) {
        String lockKey = "stock:" + showtimeId;
        Lock lock = lockManager.getLock(lockKey);
        
        try {
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                try {
                    // 1. 分布式锁保护，减少并发冲突
                    Showtime showtime = showtimeMapper.selectById(showtimeId);
                    
                    if (showtime.getAvailableSeats() < quantity) {
                        return false;
                    }
                    
                    // 2. 使用乐观锁更新（version字段）
                    showtime.setAvailableSeats(showtime.getAvailableSeats() - quantity);
                    int updated = showtimeMapper.updateById(showtime); // MyBatis-Plus自动检查version
                    
                    if (updated > 0) {
                        log.info("库存扣减成功");
                        return true;
                    } else {
                        log.warn("乐观锁更新失败，可能有并发冲突");
                        return false;
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                throw new BusinessException("系统繁忙");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("操作被中断");
        }
    }
}
```

### 6. 异常处理

```java
/**
 * 异常处理最佳实践
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExceptionHandlingBestPractices {
    
    private final LockManager lockManager;
    
    /**
     * ✅ 正确示例：确保锁一定会被释放
     */
    public void processWithExceptionHandling(String orderId) {
        String lockKey = "order:" + orderId;
        Lock lock = lockManager.getLock(lockKey);
        
        try {
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    // 业务逻辑（可能抛出异常）
                    processOrder(orderId);
                } catch (Exception e) {
                    log.error("订单处理失败：{}", orderId, e);
                    // 处理异常，但确保finally中的unlock会执行
                    throw e;
                } finally {
                    // 确保锁一定会被释放
                    try {
                        lock.unlock();
                        log.info("锁已释放：{}", lockKey);
                    } catch (Exception e) {
                        log.error("释放锁失败：{}", lockKey, e);
                    }
                }
            } else {
                throw new BusinessException("系统繁忙");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("操作被中断");
        }
    }
    
    private void processOrder(String orderId) {
        // 业务逻辑
    }
}
```

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [CONFIG.md](./CONFIG.md) - 配置指南
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 发展路线图

---

**最后更新**: 2025-11-20  
**文档版本**: v2.0

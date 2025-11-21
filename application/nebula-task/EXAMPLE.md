# Nebula Task - 使用示例

> 任务调度完整使用指南，以票务系统定时任务为例

## 目录

- [快速开始](#快速开始)
- [定时任务](#定时任务)
- [异步任务](#异步任务)
- [XXL-JOB集成](#xxl-job集成)
- [任务监控](#任务监控)
- [票务系统完整示例](#票务系统完整示例)
- [最佳实践](#最佳实践)

---

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-task</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 基础配置

```yaml
nebula:
  task:
    enabled: true
    
    # 线程池配置
    thread-pool:
      core-size: 10
      max-size: 50
      queue-capacity: 1000
    
    # XXL-JOB配置（可选）
    xxl-job:
      enabled: false
      admin-addresses: http://localhost:8080/xxl-job-admin
      app-name: ticket-system
      access-token: ${XXL_JOB_ACCESS_TOKEN}
```

---

## 定时任务

### 1. 基础定时任务

```java
/**
 * 定时任务基础示例
 */
@Component
@Slf4j
public class BasicScheduledTasks {
    
    /**
     * 每分钟执行一次
     */
    @Scheduled(cron = "0 * * * * ?")
    public void everyMinute() {
        log.info("每分钟执行一次：{}", LocalDateTime.now());
    }
    
    /**
     * 每5分钟执行一次
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void everyFiveMinutes() {
        log.info("每5分钟执行一次：{}", LocalDateTime.now());
    }
    
    /**
     * 每小时执行一次
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void everyHour() {
        log.info("每小时执行一次：{}", LocalDateTime.now());
    }
    
    /**
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void everyDayAt2AM() {
        log.info("每天凌晨2点执行：{}", LocalDateTime.now());
    }
}
```

### 2. 票务系统定时任务

```java
/**
 * 票务系统定时任务
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TicketingScheduledTasks {
    
    private final OrderService orderService;
    private final ShowtimeService showtimeService;
    private final NotificationService notificationService;
    private final LockManager lockManager;
    
    /**
     * 取消过期未支付订单（每5分钟）
     */
    @Scheduled(cron = "0 */5 * * * ?")
    @TaskHandler(name = "cancel-expired-orders", description = "取消过期未支付订单")
    public void cancelExpiredOrders() {
        String lockKey = "task:cancel-expired-orders";
        Lock lock = lockManager.getLock(lockKey);
        
        try {
            // 分布式锁，确保只有一个实例执行
            if (lock.tryLock(0, 60, TimeUnit.SECONDS)) {
                try {
                    log.info("开始执行：取消过期订单");
                    
                    // 查询30分钟前创建且未支付的订单
                    LocalDateTime expireTime = LocalDateTime.now().minusMinutes(30);
                    List<Order> expiredOrders = orderService.findExpiredOrders(expireTime);
                    
                    int cancelCount = 0;
                    for (Order order : expiredOrders) {
                        try {
                            // 取消订单
                            orderService.cancelOrder(order.getOrderNo(), "超时未支付");
                            
                            // 恢复库存
                            showtimeService.restoreStock(
                                    order.getShowtimeId(), order.getQuantity());
                            
                            // 发送通知
                            notificationService.sendOrderCancelledNotification(
                                    order.getUserId(), order.getOrderNo(), "超时未支付");
                            
                            cancelCount++;
                        } catch (Exception e) {
                            log.error("取消订单失败：orderNo={}", order.getOrderNo(), e);
                        }
                    }
                    
                    log.info("取消过期订单完成：共{}个", cancelCount);
                } finally {
                    lock.unlock();
                }
            } else {
                log.info("未获取到锁，跳过本次执行");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("任务执行被中断", e);
        }
    }
    
    /**
     * 更新热门演出排行榜（每10分钟）
     */
    @Scheduled(cron = "0 */10 * * * ?")
    @TaskHandler(name = "update-hot-showtimes", description = "更新热门演出排行榜")
    public void updateHotShowtimes() {
        log.info("开始执行：更新热门演出排行榜");
        
        try {
            // 计算热度并更新排行榜
            showtimeService.updateHotRanking();
            
            log.info("热门演出排行榜更新完成");
        } catch (Exception e) {
            log.error("更新热门演出排行榜失败", e);
        }
    }
    
    /**
     * 发送演出提醒（每小时）
     */
    @Scheduled(cron = "0 0 * * * ?")
    @TaskHandler(name = "send-showtime-reminders", description = "发送演出提醒")
    public void sendShowtimeReminders() {
        log.info("开始执行：发送演出提醒");
        
        try {
            // 查询2小时后开始的演出的订单
            LocalDateTime reminderTime = LocalDateTime.now().plusHours(2);
            List<Order> orders = orderService.findUpcomingOrders(reminderTime);
            
            int sentCount = 0;
            for (Order order : orders) {
                try {
                    Showtime showtime = showtimeService.getById(order.getShowtimeId());
                    
                    // 发送提醒通知
                    notificationService.sendShowtimeReminder(
                            order.getUserId(), 
                            order.getOrderNo(), 
                            showtime);
                    
                    // 标记已发送提醒
                    orderService.markReminderSent(order.getOrderNo());
                    
                    sentCount++;
                } catch (Exception e) {
                    log.error("发送提醒失败：orderNo={}", order.getOrderNo(), e);
                }
            }
            
            log.info("演出提醒发送完成：共{}条", sentCount);
        } catch (Exception e) {
            log.error("发送演出提醒失败", e);
        }
    }
    
    /**
     * 生成每日报表（每天凌晨2点）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @TaskHandler(name = "generate-daily-report", description = "生成每日报表")
    public void generateDailyReport() {
        log.info("开始执行：生成每日报表");
        
        String lockKey = "task:generate-daily-report";
        Lock lock = lockManager.getLock(lockKey);
        
        try {
            if (lock.tryLock(0, 600, TimeUnit.SECONDS)) {  // 10分钟超时
                try {
                    LocalDate yesterday = LocalDate.now().minusDays(1);
                    
                    // 生成报表
                    DailyReport report = orderService.generateDailyReport(yesterday);
                    
                    // 保存报表
                    reportService.saveDailyReport(report);
                    
                    // 发送给管理员
                    notificationService.sendDailyReportToAdmins(report);
                    
                    log.info("每日报表生成完成：date={}", yesterday);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("任务执行被中断", e);
        }
    }
    
    /**
     * 清理过期数据（每天凌晨3点）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @TaskHandler(name = "cleanup-expired-data", description = "清理过期数据")
    public void cleanupExpiredData() {
        log.info("开始执行：清理过期数据");
        
        try {
            // 清理30天前的日志
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30);
            
            int deletedLogs = orderLogService.deleteOldLogs(cutoffTime);
            log.info("清理订单日志：{}条", deletedLogs);
            
            // 清理临时文件
            int deletedFiles = fileStorageService.cleanupTempFiles();
            log.info("清理临时文件：{}个", deletedFiles);
            
            log.info("过期数据清理完成");
        } catch (Exception e) {
            log.error("清理过期数据失败", e);
        }
    }
}
```

---

## 异步任务

### 1. 基础异步任务

```java
/**
 * 异步任务服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncTaskService {
    
    private final TaskExecutor taskExecutor;
    
    /**
     * 异步执行任务
     */
    public void executeAsync(String taskName, Runnable task) {
        Task asyncTask = Task.builder()
                .name(taskName)
                .type(TaskType.ASYNC)
                .handler(task)
                .build();
        
        taskExecutor.execute(asyncTask);
        
        log.info("异步任务已提交：{}", taskName);
    }
    
    /**
     * 异步执行任务（带返回值）
     */
    public <T> CompletableFuture<T> executeAsyncWithResult(
            String taskName, Callable<T> task) {
        
        CompletableFuture<T> future = new CompletableFuture<>();
        
        Task asyncTask = Task.builder()
                .name(taskName)
                .type(TaskType.ASYNC)
                .handler(() -> {
                    try {
                        T result = task.call();
                        future.complete(result);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                })
                .build();
        
        taskExecutor.execute(asyncTask);
        
        log.info("异步任务已提交（带返回值）：{}", taskName);
        
        return future;
    }
}
```

### 2. 票务异步任务

```java
/**
 * 票务异步任务服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketingAsyncTaskService {
    
    private final AsyncTaskService asyncTaskService;
    private final TicketService ticketService;
    private final NotificationService notificationService;
    
    /**
     * 异步生成电子票
     */
    public void generateTicketsAsync(String orderNo) {
        asyncTaskService.executeAsync(
                "generate-tickets-" + orderNo,
                () -> {
                    log.info("开始生成电子票：orderNo={}", orderNo);
                    
                    try {
                        // 生成电子票
                        List<String> ticketNos = ticketService.generateTickets(orderNo);
                        
                        // 发送通知
                        Order order = orderService.getOrderByOrderNo(orderNo);
                        notificationService.sendTicketsGenerated(
                                order.getUserId(), orderNo, ticketNos);
                        
                        log.info("电子票生成完成：orderNo={}, 数量={}", 
                                orderNo, ticketNos.size());
                    } catch (Exception e) {
                        log.error("生成电子票失败：orderNo={}", orderNo, e);
                    }
                }
        );
    }
    
    /**
     * 异步发送邮件
     */
    public void sendEmailAsync(String to, String subject, String content) {
        asyncTaskService.executeAsync(
                "send-email-" + to,
                () -> {
                    log.info("发送邮件：to={}, subject={}", to, subject);
                    
                    try {
                        emailService.send(to, subject, content);
                        
                        log.info("邮件发送成功：to={}", to);
                    } catch (Exception e) {
                        log.error("邮件发送失败：to={}", to, e);
                    }
                }
        );
    }
    
    /**
     * 异步导出数据
     */
    public CompletableFuture<String> exportDataAsync(ExportRequest request) {
        return asyncTaskService.executeAsyncWithResult(
                "export-data-" + request.getExportType(),
                () -> {
                    log.info("开始导出数据：{}", request);
                    
                    // 导出数据
                    String fileUrl = dataExportService.export(request);
                    
                    log.info("数据导出完成：fileUrl={}", fileUrl);
                    
                    return fileUrl;
                }
        );
    }
}
```

---

## XXL-JOB集成

### 1. XXL-JOB配置

```yaml
nebula:
  task:
    xxl-job:
      enabled: true
      admin-addresses: http://localhost:8080/xxl-job-admin
      app-name: ticket-system
      access-token: ${XXL_JOB_ACCESS_TOKEN}
      
      # 执行器配置
      executor:
        port: 9999
        log-path: /data/applogs/xxl-job/jobhandler
        log-retention-days: 30
```

### 2. XXL-JOB任务处理器

```java
/**
 * XXL-JOB任务处理器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class XxlJobHandlers {
    
    private final OrderService orderService;
    private final ShowtimeService showtimeService;
    
    /**
     * 取消过期订单
     */
    @XxlJob("cancelExpiredOrdersJob")
    public ReturnT<String> cancelExpiredOrders(String param) {
        log.info("XXL-JOB: 取消过期订单，参数：{}", param);
        
        try {
            LocalDateTime expireTime = LocalDateTime.now().minusMinutes(30);
            List<Order> expiredOrders = orderService.findExpiredOrders(expireTime);
            
            int cancelCount = 0;
            for (Order order : expiredOrders) {
                orderService.cancelOrder(order.getOrderNo(), "超时未支付");
                cancelCount++;
            }
            
            String result = String.format("取消了%d个过期订单", cancelCount);
            log.info("XXL-JOB执行成功：{}", result);
            
            return ReturnT.SUCCESS(result);
        } catch (Exception e) {
            log.error("XXL-JOB执行失败", e);
            return ReturnT.FAIL(e.getMessage());
        }
    }
    
    /**
     * 同步演出数据到ES
     */
    @XxlJob("syncShowtimesToESJob")
    public ReturnT<String> syncShowtimesToES(String param) {
        log.info("XXL-JOB: 同步演出数据到ES，参数：{}", param);
        
        try {
            int syncCount = showtimeService.syncToElasticsearch();
            
            String result = String.format("同步了%d个演出到ES", syncCount);
            log.info("XXL-JOB执行成功：{}", result);
            
            return ReturnT.SUCCESS(result);
        } catch (Exception e) {
            log.error("XXL-JOB执行失败", e);
            return ReturnT.FAIL(e.getMessage());
        }
    }
    
    /**
     * 分片任务：批量处理订单
     */
    @XxlJob("batchProcessOrdersJob")
    public ReturnT<String> batchProcessOrders(String param) {
        // 获取分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        
        log.info("XXL-JOB: 批量处理订单，分片：{}/{}", shardIndex + 1, shardTotal);
        
        try {
            // 根据分片处理订单
            int processedCount = orderService.batchProcessOrders(shardIndex, shardTotal);
            
            String result = String.format("分片%d/%d处理了%d个订单", 
                    shardIndex + 1, shardTotal, processedCount);
            log.info("XXL-JOB执行成功：{}", result);
            
            return ReturnT.SUCCESS(result);
        } catch (Exception e) {
            log.error("XXL-JOB执行失败", e);
            return ReturnT.FAIL(e.getMessage());
        }
    }
}
```

---

## 任务监控

### 1. 任务执行监控

```java
/**
 * 任务监控服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskMonitorService {
    
    private final TaskRegistry taskRegistry;
    
    /**
     * 获取所有任务状态
     */
    public List<TaskStatus> getAllTaskStatus() {
        return taskRegistry.getAllTasks().stream()
                .map(this::getTaskStatus)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取任务状态
     */
    public TaskStatus getTaskStatus(String taskName) {
        Task task = taskRegistry.getTask(taskName);
        
        if (task == null) {
            return null;
        }
        
        TaskStatus status = new TaskStatus();
        status.setTaskName(task.getName());
        status.setTaskType(task.getType());
        status.setLastExecuteTime(task.getLastExecuteTime());
        status.setLastExecuteResult(task.getLastExecuteResult());
        status.setExecuteCount(task.getExecuteCount());
        status.setFailCount(task.getFailCount());
        
        return status;
    }
}

@Data
public class TaskStatus {
    private String taskName;
    private TaskType taskType;
    private LocalDateTime lastExecuteTime;
    private TaskResult lastExecuteResult;
    private long executeCount;
    private long failCount;
}
```

### 2. 任务管理接口

```java
/**
 * 任务管理控制器
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskManagementController extends BaseController {
    
    private final TaskMonitorService taskMonitorService;
    private final TaskExecutor taskExecutor;
    
    /**
     * 获取所有任务状态
     */
    @GetMapping("/status")
    @RequiresAuth
    @RequiresRole("ADMIN")
    public Result<List<TaskStatus>> getAllTaskStatus() {
        List<TaskStatus> statusList = taskMonitorService.getAllTaskStatus();
        
        return success(statusList);
    }
    
    /**
     * 手动触发任务
     */
    @PostMapping("/{taskName}/trigger")
    @RequiresAuth
    @RequiresRole("ADMIN")
    public Result<Void> triggerTask(@PathVariable String taskName) {
        log.info("手动触发任务：{}", taskName);
        
        Task task = taskRegistry.getTask(taskName);
        
        if (task == null) {
            return error("TASK_NOT_FOUND", "任务不存在");
        }
        
        taskExecutor.execute(task);
        
        return success();
    }
}
```

---

## 票务系统完整示例

### 完整的任务调度方案

```java
/**
 * 票务系统完整任务调度
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TicketingTaskScheduler {
    
    private final OrderService orderService;
    private final ShowtimeService showtimeService;
    private final TicketService ticketService;
    private final NotificationService notificationService;
    private final ReportService reportService;
    private final LockManager lockManager;
    
    /**
     * 1. 订单相关任务
     */
    
    /**
     * 取消过期未支付订单（每5分钟）
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void cancelExpiredOrders() {
        executeWithLock("task:cancel-expired-orders", () -> {
            log.info("开始取消过期订单");
            
            LocalDateTime expireTime = LocalDateTime.now().minusMinutes(30);
            List<Order> expiredOrders = orderService.findExpiredOrders(expireTime);
            
            for (Order order : expiredOrders) {
                try {
                    orderService.cancelOrder(order.getOrderNo(), "超时未支付");
                    showtimeService.restoreStock(order.getShowtimeId(), order.getQuantity());
                    notificationService.sendOrderCancelledNotification(
                            order.getUserId(), order.getOrderNo(), "超时未支付");
                } catch (Exception e) {
                    log.error("取消订单失败：{}", order.getOrderNo(), e);
                }
            }
            
            log.info("取消过期订单完成：共{}个", expiredOrders.size());
        });
    }
    
    /**
     * 2. 演出相关任务
     */
    
    /**
     * 更新演出状态（每10分钟）
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void updateShowtimeStatus() {
        log.info("开始更新演出状态");
        
        try {
            // 将已开始的演出状态更新为ONGOING
            int ongoingCount = showtimeService.updateStatusToOngoing();
            
            // 将已结束的演出状态更新为FINISHED
            int finishedCount = showtimeService.updateStatusToFinished();
            
            log.info("演出状态更新完成：开始{}个，结束{}个", ongoingCount, finishedCount);
        } catch (Exception e) {
            log.error("更新演出状态失败", e);
        }
    }
    
    /**
     * 更新热门排行榜（每10分钟）
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void updateHotRanking() {
        log.info("开始更新热门排行榜");
        
        try {
            showtimeService.updateHotRanking();
            
            log.info("热门排行榜更新完成");
        } catch (Exception e) {
            log.error("更新热门排行榜失败", e);
        }
    }
    
    /**
     * 3. 通知相关任务
     */
    
    /**
     * 发送演出提醒（每小时）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void sendShowtimeReminders() {
        log.info("开始发送演出提醒");
        
        try {
            LocalDateTime reminderTime = LocalDateTime.now().plusHours(2);
            List<Order> orders = orderService.findUpcomingOrders(reminderTime);
            
            for (Order order : orders) {
                try {
                    Showtime showtime = showtimeService.getById(order.getShowtimeId());
                    notificationService.sendShowtimeReminder(
                            order.getUserId(), order.getOrderNo(), showtime);
                    orderService.markReminderSent(order.getOrderNo());
                } catch (Exception e) {
                    log.error("发送提醒失败：{}", order.getOrderNo(), e);
                }
            }
            
            log.info("演出提醒发送完成：共{}条", orders.size());
        } catch (Exception e) {
            log.error("发送演出提醒失败", e);
        }
    }
    
    /**
     * 4. 数据同步任务
     */
    
    /**
     * 同步数据到Elasticsearch（每6小时）
     */
    @Scheduled(cron = "0 0 */6 * * ?")
    public void syncToElasticsearch() {
        executeWithLock("task:sync-to-es", () -> {
            log.info("开始同步数据到ES");
            
            try {
                int syncCount = showtimeService.syncToElasticsearch();
                
                log.info("数据同步到ES完成：共{}个演出", syncCount);
            } catch (Exception e) {
                log.error("同步数据到ES失败", e);
            }
        });
    }
    
    /**
     * 5. 报表和统计任务
     */
    
    /**
     * 生成每日报表（每天凌晨2点）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void generateDailyReport() {
        executeWithLock("task:daily-report", () -> {
            log.info("开始生成每日报表");
            
            try {
                LocalDate yesterday = LocalDate.now().minusDays(1);
                
                DailyReport report = reportService.generateDailyReport(yesterday);
                reportService.saveDailyReport(report);
                notificationService.sendDailyReportToAdmins(report);
                
                log.info("每日报表生成完成：date={}", yesterday);
            } catch (Exception e) {
                log.error("生成每日报表失败", e);
            }
        });
    }
    
    /**
     * 6. 数据清理任务
     */
    
    /**
     * 清理过期数据（每天凌晨3点）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredData() {
        log.info("开始清理过期数据");
        
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30);
            
            // 清理日志
            int deletedLogs = orderLogService.deleteOldLogs(cutoffTime);
            log.info("清理订单日志：{}条", deletedLogs);
            
            // 清理临时文件
            int deletedFiles = fileStorageService.cleanupTempFiles();
            log.info("清理临时文件：{}个", deletedFiles);
            
            log.info("过期数据清理完成");
        } catch (Exception e) {
            log.error("清理过期数据失败", e);
        }
    }
    
    // 辅助方法
    
    private void executeWithLock(String lockKey, Runnable task) {
        Lock lock = lockManager.getLock(lockKey);
        
        try {
            if (lock.tryLock(0, 300, TimeUnit.SECONDS)) {
                try {
                    task.run();
                } finally {
                    lock.unlock();
                }
            } else {
                log.info("未获取到锁，跳过本次执行：{}", lockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("任务执行被中断：{}", lockKey, e);
        }
    }
}
```

---

## 最佳实践

### 1. 分布式锁

- **使用分布式锁**：确保定时任务只在一个实例执行
- **合理超时时间**：根据任务执行时间设置
- **异常处理**：确保锁一定被释放

### 2. 任务幂等性

- **幂等设计**：任务可以重复执行而不产生副作用
- **去重机制**：使用唯一键防止重复处理
- **状态检查**：执行前检查数据状态

### 3. 错误处理

- **异常捕获**：捕获并记录异常
- **失败重试**：支持自动重试
- **告警通知**：任务失败时告警

### 4. 性能优化

- **批量处理**：批量操作提高效率
- **分页查询**：大数据量分页处理
- **异步执行**：耗时操作异步处理
- **分片执行**：使用XXL-JOB分片功能

### 5. 监控和维护

- **任务监控**：监控任务执行状态
- **执行日志**：记录详细执行日志
- **性能指标**：统计执行时间、成功率
- **手动触发**：提供手动触发接口

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [CONFIG.md](./CONFIG.md) - 配置指南
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 发展路线图

---

**最后更新**: 2025-11-20  
**文档版本**: v2.0


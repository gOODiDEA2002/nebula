# Nebula Starter Task 使用示例

## 基础用法

### 1. 每日任务

```java
@Component
@Slf4j
public class DailyReportTask implements EveryDayExecute {
    
    @Override
    public void execute() {
        log.info("生成每日报表");
        // 执行报表生成逻辑
    }
}
```

### 2. 每小时任务

```java
@Component
@Slf4j
public class HourlyCleanupTask implements EveryHourExecute {
    
    @Override
    public void execute() {
        log.info("执行小时清理任务");
        // 执行清理逻辑
    }
}
```

### 3. 每5分钟任务

```java
@Component
@Slf4j
public class HealthCheckTask implements EveryFiveMinuteExecute {
    
    @Override
    public void execute() {
        log.info("执行健康检查");
        // 执行健康检查逻辑
    }
}
```

## 通过 RPC 调用其他服务

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class CrawlerTask implements EveryDayExecute {
    
    private final PlatformCrawlerRpcClient crawlerRpcClient;
    
    @Override
    public void execute() {
        log.info("开始执行平台抓取任务");
        
        try {
            CrawlResultDto result = crawlerRpcClient.executeCrawl("vocoor");
            
            if (result.getSuccess()) {
                log.info("抓取完成: 发现={}, 成功={}, 失败={}",
                    result.getDiscoveredCount(),
                    result.getSuccessCount(),
                    result.getFailCount());
            } else {
                log.error("抓取失败: {}", result.getMessage());
            }
        } catch (Exception e) {
            log.error("任务执行异常", e);
        }
    }
}
```

## XXL-JOB 配置

在 XXL-JOB 管理后台配置任务：

1. 执行器管理 -> 新增执行器
   - AppName: `my-task-executor`
   - 名称: `我的任务执行器`
   - 注册方式: 自动注册

2. 任务管理 -> 新增任务
   - 执行器: `my-task-executor`
   - JobHandler: `everyDayExecuteJobHandler`
   - Cron: `0 0 1 * * ?`
   - 运行模式: BEAN

# Nebula Task - 任务调度模块

Nebula Task 是一个基于 XXL-JOB 的统一任务调度框架，提供了简洁的 API 和强大的定时任务功能。

## 特性

- 🚀 **统一架构**: 提供统一的任务调度和执行框架
- 🔧 **基于 XXL-JOB**: 采用成熟的分布式任务调度方案
- 📦 **开箱即用**: 提供 Spring Boot Starter，零配置快速启动
- 📊 **监控友好**: 集成指标监控，实时了解任务执行状态
- 🛡️ **异常处理**: 完善的异常处理和重试机制
- ⏰ **定时任务接口**: 提供标准的定时任务接口，简化定时任务开发
- 🔧 **统一配置**: 采用 `nebula.task` 配置前缀，避免与原生 XXL-JOB 配置冲突

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-task</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置

```yaml
nebula:
  task:
    enabled: true
    xxl-job:
      enabled: true
      executor-name: my-app-executor
      executor-port: 9999
      admin-addresses: http://localhost:8080/xxl-job-admin
      access-token: xxl-job
```

### 3. 创建任务执行器

```java
@TaskHandler("myTask")
@Component
public class MyTaskExecutor implements TaskExecutor {
    
    @Override
    public String getExecutorName() {
        return "myTask";
    }
    
    @Override
    public TaskResult execute(TaskContext context) {
        TaskLogger logger = context.getLogger();
        
        try {
            logger.info("开始执行任务");
            
            // 获取参数
            String param = context.getStringParameter("param", "default");
            
            // 执行业务逻辑
            // ...
            
            logger.info("任务执行完成");
            return TaskResult.success(context);
            
        } catch (Exception e) {
            logger.error("任务执行失败: %s", e.getMessage());
            return TaskResult.failure(context, e);
        }
    }
}
```

### 4. 创建定时任务

使用接口方式创建定时任务，通过XXL-JOB管理后台进行调度配置：

```java
// 每小时执行一次的数据清理任务
@Component
public class DataCleanupTask implements EveryHourExecute {
    
    @Override
    public void execute() {
        log.info("开始执行数据清理任务");
        
        // 执行清理逻辑
        cleanupTemporaryData();
        
        log.info("数据清理完成");
    }
}
```

**XXL-JOB 配置：**
- JobHandler: `everyHourExecuteJobHandler`
- Cron: `0 0 * * * ?`

## 核心概念

### TaskExecutor - 任务执行器

任务执行器是任务的具体实现，负责执行业务逻辑。

```java
public interface TaskExecutor {
    TaskResult execute(TaskContext context);
    String getExecutorName();
    boolean supports(TaskType taskType);
}
```

### 🕐 定时任务接口

框架提供了标准的定时任务接口，开发者只需实现对应的接口即可，具体的调度配置在XXL-JOB管理后台进行。

#### 可用的接口

| 接口 | 执行频率 | JobHandler | Cron 表达式 | 使用场景 |
|------|----------|------------|-------------|----------|
| `EveryMinuteExecute` | 每分钟 | `everyMinuteExecuteJobHandler` | `0 * * * * ?` | 高频监控、实时数据处理 |
| `EveryFiveMinuteExecute` | 每5分钟 | `everyFiveMinuteExecuteJobHandler` | `0 */5 * * * ?` | 健康检查、状态同步 |
| `EveryHourExecute` | 每小时 | `everyHourExecuteJobHandler` | `0 0 * * * ?` | 数据清理、统计报表 |
| `EveryDayExecute` | 每天 | `everyDayExecuteJobHandler` | `0 0 1 * * ?` | 备份任务、日报生成 |

#### 优势

- ✅ **简化开发**: 只需关注业务逻辑，调度配置在管理后台完成
- ✅ **类型安全**: 编译时确定任务类型的正确性
- ✅ **统一管理**: 所有定时任务使用统一的接口模式
- ✅ **自动注册**: 框架自动发现并注册定时任务实现
- ✅ **灵活配置**: 通过XXL-JOB管理后台灵活配置调度策略

#### 使用步骤

1. **实现接口**: 选择合适的定时任务接口实现，添加 `@Component` 注解
2. **配置XXL-JOB**: 在XXL-JOB管理后台添加任务，设置JobHandler和Cron表达式
3. **启动应用**: 框架会自动发现并注册任务实现

#### 完整示例

```java
// 系统健康检查 - 每5分钟执行
@Component
public class HealthCheckTask implements EveryFiveMinuteExecute {
    
    @Override
    public void execute() {
        try {
            log.info("开始执行系统健康检查");
            
            // 检查系统状态
            HealthStatus status = checkSystemHealth();
            
            if (status.isHealthy()) {
                log.info("系统运行正常");
            } else {
                log.warn("系统存在异常: {}", status.getIssues());
                // 可以在这里发送告警通知
            }
        } catch (Exception e) {
            log.error("健康检查失败", e);
        }
    }
    
    private HealthStatus checkSystemHealth() {
        // 具体的健康检查逻辑
        return new HealthStatus();
    }
}
```

**对应的XXL-JOB配置：**
- JobHandler: `everyFiveMinuteExecuteJobHandler`
- Cron: `0 */5 * * * ?`
- 运行模式: BEAN

### TaskContext - 任务上下文

任务上下文包含了任务执行时的所有信息：

```java
// 获取参数
String param = context.getStringParameter("param", "default");
int count = context.getIntParameter("count", 10);
boolean flag = context.getBooleanParameter("flag", false);

// 记录日志
TaskLogger logger = context.getLogger();
logger.info("执行进度: %d/%d", current, total);
```

## API 示例

### 手动执行任务

```java
@RestController
public class TaskController {
    
    @Autowired
    private TaskEngine taskEngine;
    
    @PostMapping("/tasks/{taskName}/execute")
    public TaskResult executeTask(@PathVariable String taskName, 
                                 @RequestBody Map<String, Object> parameters) {
        
        int logId = (int) System.currentTimeMillis();
        long logDateTime = System.currentTimeMillis();
        
        return taskEngine.executeSync(taskName, parameters, logId, logDateTime);
    }
    
    @GetMapping("/tasks/logs/{logId}")
    public String getTaskLog(@PathVariable int logId) {
        return taskEngine.getTaskLog(logId);
    }
}
```

## 与 XXL-JOB 集成

### 执行器注册

1. 启动应用后，执行器会自动注册到XXL-JOB管理端
2. 可以在管理端看到注册的执行器信息

### 任务配置

在XXL-JOB管理后台配置任务：

1. **基础信息**
   - 任务描述：任务的描述信息
   - 路由策略：选择合适的路由策略
   - Cron：设置调度时间表达式
   - 运行模式：选择 BEAN 模式

2. **任务配置**
   - JobHandler：填写对应的Handler名称（如 `everyHourExecuteJobHandler`）
   - 任务参数：可以传递给任务的参数

3. **高级配置**
   - 子任务：支持配置子任务
   - 任务超时时间：设置任务执行超时时间
   - 失败重试次数：设置失败重试策略

## 监控指标

框架自动集成监控指标：

- 任务执行次数
- 任务执行时间
- 任务成功/失败率
- 当前运行任务数

## 🔧 配置说明

### 重要变更：配置前缀统一

**从 v2.0.0 开始**，所有配置都统一使用 `nebula.task` 前缀，不再使用 `xxl.job` 前缀。这样做的好处：

- ✅ 避免与原生 XXL-JOB 配置冲突
- ✅ 保持 Nebula 框架配置的一致性
- ✅ 更清晰的配置层次结构

### 配置迁移

如果您之前使用的是 `xxl.job` 配置，请按照以下方式迁移：

```yaml
# ❌ 旧配置方式（已废弃）
xxl:
  job:
    admin:
      addresses: http://localhost:8080/xxl-job-admin
    executor:
      appname: my-executor
      port: 9999
    accessToken: token

# ✅ 新配置方式（推荐）
nebula:
  task:
    xxl-job:
      admin-addresses: http://localhost:8080/xxl-job-admin
      executor-name: my-executor
      executor-port: 9999
      access-token: token
```

### 完整配置参考

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| **基础配置** | | |
| `nebula.task.enabled` | `true` | 是否启用任务功能 |
| **执行器配置** | | |
| `nebula.task.executor.core-pool-size` | `10` | 线程池核心线程数 |
| `nebula.task.executor.max-pool-size` | `200` | 线程池最大线程数 |
| `nebula.task.executor.keep-alive-seconds` | `60` | 线程空闲时间（秒） |
| `nebula.task.executor.queue-capacity` | `1000` | 队列容量 |
| `nebula.task.executor.thread-name-prefix` | `nebula-task-` | 线程名前缀 |
| **XXL-JOB 配置** | | |
| `nebula.task.xxl-job.enabled` | `true` | 是否启用 XXL-JOB |
| `nebula.task.xxl-job.executor-name` | - | 执行器名称（必填） |
| `nebula.task.xxl-job.executor-ip` | 自动获取 | 执行器IP |
| `nebula.task.xxl-job.executor-port` | `9999` | 执行器端口 |
| `nebula.task.xxl-job.log-path` | `./logs/xxl-job` | 日志路径 |
| `nebula.task.xxl-job.log-retention-days` | `30` | 日志保留天数 |
| `nebula.task.xxl-job.admin-addresses` | - | XXL-JOB 管理端地址（必填） |
| `nebula.task.xxl-job.access-token` | `xxl-job` | 访问令牌 |

### 配置示例

```yaml
nebula:
  task:
    enabled: true
    
    # 线程池配置
    executor:
      core-pool-size: 20
      max-pool-size: 500
      keep-alive-seconds: 120
      queue-capacity: 2000
      thread-name-prefix: "my-app-task-"
    
    # XXL-JOB 配置
    xxl-job:
      enabled: true
      executor-name: my-application-executor
      executor-ip: 192.168.1.100  # 可选，默认自动获取
      executor-port: 9999
      log-path: ./logs/task
      log-retention-days: 15
      admin-addresses: http://xxl-job.example.com:8080/xxl-job-admin
      access-token: my-secret-token
```

## 最佳实践

1. **任务幂等性**: 确保任务可以重复执行
2. **参数验证**: 在任务开始时验证所有必需参数
3. **进度报告**: 对于长时间运行的任务，定期报告进度
4. **异常处理**: 妥善处理各种异常情况
5. **资源清理**: 确保任务结束后清理相关资源
6. **日志记录**: 记录详细的执行日志，便于问题排查
7. **监控告警**: 对关键任务配置监控告警

## XXL-JOB 管理后台配置指南

### 1. 创建执行器

在执行器管理页面添加执行器：
- AppName：对应配置中的 `executor-name`
- 名称：执行器显示名称
- 注册方式：自动注册
- 机器地址：留空，自动获取

### 2. 创建定时任务

在任务管理页面添加任务：

#### 每分钟任务示例
- 任务描述：数据监控任务
- Cron：`0 * * * * ?`
- 运行模式：BEAN
- JobHandler：`everyMinuteExecuteJobHandler`

#### 每小时任务示例
- 任务描述：数据清理任务
- Cron：`0 0 * * * ?`
- 运行模式：BEAN
- JobHandler：`everyHourExecuteJobHandler`

#### 每日任务示例
- 任务描述：日备份任务
- Cron：`0 0 1 * * ?`
- 运行模式：BEAN
- JobHandler：`everyDayExecuteJobHandler`

### 3. 任务监控

在调度日志页面可以查看：
- 任务执行历史
- 执行耗时
- 执行结果
- 错误信息

## 更多信息

- [XXL-JOB 官方文档](https://www.xuxueli.com/xxl-job/)
- [Nebula Framework 文档](../../../docs/)
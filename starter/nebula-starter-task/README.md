# Nebula Starter Task

任务调度应用快速启动器，基于 XXL-JOB 提供分布式任务调度能力。

## 功能特性

- 开箱即用的任务调度配置
- 基于 XXL-JOB 的分布式任务调度
- 内置定时任务接口（EveryMinuteExecute、EveryHourExecute 等）
- RPC 客户端支持，可调用其他微服务
- 服务发现集成（可选）

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-task</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 2. 配置

```yaml
nebula:
  task:
    enabled: true
    xxl-job:
      enabled: true
      admin-addresses: http://localhost:8080/xxl-job-admin
      executor-name: my-task-executor
      executor-port: 9999
      access-token: xxl-job
```

### 3. 创建定时任务

```java
@Component
@Slf4j
public class MyDailyTask implements EveryDayExecute {
    
    @Override
    public void execute() {
        log.info("执行每日任务");
        // 业务逻辑
    }
}
```

## 包含的模块

| 模块 | 说明 |
|------|------|
| nebula-task | 核心任务调度模块 |
| nebula-starter-minimal | 基础配置 |
| nebula-rpc-core | RPC 核心 |
| nebula-rpc-http | HTTP RPC 实现 |
| nebula-discovery-core | 服务发现核心 |
| nebula-discovery-nacos | Nacos 服务发现（可选） |

## 定时任务接口

| 接口 | 说明 | 建议 Cron |
|------|------|-----------|
| EveryMinuteExecute | 每分钟执行 | `0 * * * * ?` |
| EveryFiveMinuteExecute | 每5分钟执行 | `0 */5 * * * ?` |
| EveryHourExecute | 每小时执行 | `0 0 * * * ?` |
| EveryDayExecute | 每天执行 | `0 0 1 * * ?` |

## 与其他微服务通信

通过 RPC 客户端调用其他微服务：

```java
@Component
@RequiredArgsConstructor
public class MyTask implements EveryDayExecute {
    
    // 注入 RPC 客户端
    private final SomeServiceRpcClient someServiceRpcClient;
    
    @Override
    public void execute() {
        // 通过 RPC 调用其他服务
        Result result = someServiceRpcClient.doSomething();
    }
}
```

## 更多信息

- [Nebula Task 文档](../../application/nebula-task/README.md)
- [XXL-JOB 官方文档](https://www.xuxueli.com/xxl-job/)

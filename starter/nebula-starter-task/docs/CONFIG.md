# Nebula Starter Task 配置说明

## 配置参考

```yaml
nebula:
  task:
    enabled: true                    # 是否启用任务功能
    
    # 线程池配置
    executor:
      core-pool-size: 10             # 核心线程数
      max-pool-size: 200             # 最大线程数
      keep-alive-seconds: 60         # 线程空闲时间（秒）
      queue-capacity: 1000           # 队列容量
      thread-name-prefix: my-task-   # 线程名前缀
    
    # XXL-JOB 配置
    xxl-job:
      enabled: true                  # 是否启用 XXL-JOB
      admin-addresses: http://localhost:8080/xxl-job-admin  # 管理端地址
      executor-name: my-executor     # 执行器名称
      executor-port: 9999            # 执行器端口
      executor-ip:                   # 执行器IP（可选，自动获取）
      log-path: ./logs/xxl-job       # 日志路径
      log-retention-days: 30         # 日志保留天数
      access-token: xxl-job          # 访问令牌
```

## RPC 配置

用于调用其他微服务：

```yaml
nebula:
  rpc:
    enabled: true
    timeout: 30000                   # 超时时间（毫秒）
```

## 服务发现配置（可选）

```yaml
nebula:
  discovery:
    enabled: true
    type: nacos
    nacos:
      server-addr: localhost:8848
      namespace: 
      group: DEFAULT_GROUP
```

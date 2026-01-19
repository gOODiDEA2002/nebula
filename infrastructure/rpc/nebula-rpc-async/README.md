# Nebula RPC Async - 异步RPC执行框架

Nebula RPC 异步执行框架，提供声明式的异步RPC调用能力。

## 核心特性

- **@AsyncRpc注解** - 声明式标记异步方法
- **零配置存储** - 默认使用Nacos，复用已有连接
- **执行追踪** - 完整的执行状态和结果追踪
- **协议无关** - 同时支持HTTP和gRPC
- **多存储后端** - 支持Nacos/Redis/Database

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-rpc-async</artifactId>
</dependency>
```

### 2. 定义异步RPC接口

```java
@RpcClient("data-service")
public interface DataProcessRpcClient {
    
    // 同步调用（原有方式）
    ProcessResult processData(ProcessRequest request);
    
    // 异步调用（新增）
    @AsyncRpc(timeout = 600)  // 10分钟超时
    AsyncRpcResult<ProcessResult> processDataAsync(ProcessRequest request);
}
```

### 3. 调用异步方法

```java
@Service
@RequiredArgsConstructor
public class TaskService {
    
    private final DataProcessRpcClient dataProcessRpcClient;
    private final AsyncRpcExecutionManager executionManager;
    
    public String submitTask(ProcessRequest request) {
        // 提交异步执行
        AsyncRpcResult<ProcessResult> result = 
            dataProcessRpcClient.processDataAsync(request);
        
        // 立即返回执行ID
        return result.getExecutionId();
    }
    
    public ProcessResult getResult(String executionId) {
        // 查询执行状态
        AsyncRpcExecution execution = executionManager.getExecution(executionId);
        
        if (execution.getStatus() == ExecutionStatus.SUCCESS) {
            // 反序列化结果
            return objectMapper.readValue(
                execution.getResult(), ProcessResult.class);
        }
        
        return null;
    }
}
```

## 配置

### 零配置模式（推荐）

使用Nacos存储，无需任何额外配置：

```yaml
# 只需要已有的Nacos配置即可
nebula:
  discovery:
    nacos:
      server-addr: localhost:8848
      
  rpc:
    async:
      enabled: true  # 默认使用Nacos存储
```

### 完整配置

```yaml
nebula:
  rpc:
    async:
      enabled: true
      
      # 存储配置
      storage:
        type: nacos  # nacos(默认) / redis / database
        
      # 执行器配置
      executor:
        core-pool-size: 10
        max-pool-size: 50
        queue-capacity: 200
        
      # 清理策略
      cleanup:
        enabled: true
        retention-days: 7
```

## 存储方案

### Nacos（默认/推荐）

✅ 零配置 - 复用已有连接  
✅ 分布式 - 集群高可用  
✅ 简单 - 无需额外部署

### Redis（高性能）

```yaml
nebula.rpc.async.storage:
  type: redis
  redis:
    host: localhost
    port: 6379
```

### Database（持久化）

```yaml
nebula.rpc.async.storage:
  type: database
  # 使用项目已有数据源
```

## 架构设计

```
业务代码
  ↓
@AsyncRpc 注解检测 (RpcClientFactoryBean)
  ↓  
AsyncRpcExecutionManager
  ↓
协议层 (HttpRpcClient / GrpcRpcClient)
  ↓
网络传输
```

## 更多信息

- [设计文档](./docs/DESIGN.md)
- [API文档](./docs/API.md)
- [示例项目](./examples/)

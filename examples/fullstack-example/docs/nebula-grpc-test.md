# Nebula gRPC RPC 功能测试指南

本文档介绍如何测试 Nebula 框架的 gRPC RPC 功能

## 功能概述

Nebula gRPC RPC 模块基于 gRPC 和 Protocol Buffers 实现高性能 RPC 调用,相比 HTTP RPC:

- **更高性能**: QPS > 10000
- **更低延迟**: < 5ms
- **二进制协议**: Protocol Buffers 序列化
- **HTTP/2 传输**: 支持多路复用流式传输

## 前置准备

### 1. 启动依赖服务

确保以下服务已启动:

```bash
# 进入 middleware 目录
cd nebula-middleware

# 启动所有依赖服务
docker-compose up -d

# 验证服务状态
docker-compose ps
```

### 2. 配置检查

确认 `application.yml` 中的 gRPC 配置:

```yaml
nebula:
  rpc:
    grpc:
      enabled: true
      server:
        enabled: true
        port: 9090  # gRPC 服务器端口
      client:
        enabled: true
        target: localhost:9090
        negotiation-type: plaintext
```

### 3. 启动应用

```bash
cd nebula-example
mvn spring-boot:run
```

验证启动成功:

```bash
# 检查 gRPC 端口
lsof -i :9090
```

## 测试场景

### 场景 1: HTTP RPC 客户端调用(通过代理)

由于 `UserRpcServiceImpl` 同时使用 `@RpcService` 注解,它会自动注册到 HTTP 和 gRPC 两个 RPC 服务器

客户端通过 `@Autowired` 注入的 `UserRpcService` 代理会根据配置选择使用 HTTP 或 gRPC 传输

#### 创建用户

```bash
curl -X POST http://localhost:8000/rpc-client/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "grpcuser",
    "name": "gRPC测试用户",
    "email": "grpc@example.com",
    "phone": "13900139001",
    "status": "ACTIVE"
  }'
```

**预期响应**:

```json
{
  "success": true,
  "user": {
    "id": 1,
    "username": "grpcuser",
    "name": "gRPC测试用户",
    "email": "grpc@example.com",
    "phone": "13900139001",
    "status": "ACTIVE",
    "createTime": "2025-10-09T18:00:00"
  }
}
```

#### 获取用户信息

```bash
curl http://localhost:8000/rpc-client/users/1
```

#### 获取用户列表

```bash
curl "http://localhost:8000/rpc-client/users?username=grpcuser&page=1&size=10"
```

#### 更新用户

```bash
curl -X PUT http://localhost:8000/rpc-client/users/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "gRPC更新用户",
    "email": "grpc-updated@example.com"
  }'
```

#### 删除用户

```bash
curl -X DELETE http://localhost:8000/rpc-client/users/1
```

### 场景 2: 使用 gRPCurl 工具测试(直接 gRPC 调用)

#### 安装 gRPCurl

```bash
# macOS
brew install grpcurl

# Linux
go install github.com/fullstorydev/grpcurl/cmd/grpcurl@latest

# Windows
# 从 https://github.com/fullstorydev/grpcurl/releases 下载
```

#### 列出服务

```bash
grpcurl -plaintext localhost:9090 list
```

**预期输出**:

```
io.nebula.rpc.grpc.GenericRpcService
```

#### 列出方法

```bash
grpcurl -plaintext localhost:9090 list io.nebula.rpc.grpc.GenericRpcService
```

**预期输出**:

```
io.nebula.rpc.grpc.GenericRpcService.Call
io.nebula.rpc.grpc.GenericRpcService.BidirectionalStream
io.nebula.rpc.grpc.GenericRpcService.ClientStream
io.nebula.rpc.grpc.GenericRpcService.ServerStream
```

#### 查看方法定义

```bash
grpcurl -plaintext localhost:9090 describe io.nebula.rpc.grpc.GenericRpcService.Call
```

#### 调用 gRPC 方法

```bash
grpcurl -plaintext -d '{
  "request_id": "test-001",
  "service_name": "io.nebula.example.api.rpc.UserRpcService",
  "method_name": "createUser",
  "parameter_types": ["io.nebula.example.api.dto.CreateUserDto$Request"],
  "parameters": ["{\"username\":\"grpcuser\",\"name\":\"gRPC用户\",\"email\":\"grpc@example.com\",\"phone\":\"13900139000\",\"status\":\"ACTIVE\"}"]
}' localhost:9090 io.nebula.rpc.grpc.GenericRpcService/Call
```

**预期响应**:

```json
{
  "request_id": "test-001",
  "success": true,
  "result": "{\"success\":true,\"user\":{\"id\":1,\"username\":\"grpcuser\",\"name\":\"gRPC用户\",\"email\":\"grpc@example.com\",\"phone\":\"13900139000\",\"status\":\"ACTIVE\",\"createTime\":\"2025-10-09T18:00:00\"}}",
  "timestamp": "1696838400000"
}
```

## 性能对比测试

### HTTP RPC 性能测试

```bash
# 使用 wrk 进行压测
wrk -t4 -c100 -d30s http://localhost:8000/rpc-client/users/1
```

### gRPC RPC 性能测试

```bash
# 使用 ghz 进行压测
ghz --insecure \
  --proto /path/to/rpc_common.proto \
  --call io.nebula.rpc.grpc.GenericRpcService/Call \
  -d '{
    "request_id": "bench-001",
    "service_name": "io.nebula.example.api.rpc.UserRpcService",
    "method_name": "getUserById",
    "parameter_types": ["java.lang.Long"],
    "parameters": ["1"]
  }' \
  -c 100 -n 10000 \
  localhost:9090
```

**预期性能对比** (仅供参考):

| 指标 | HTTP RPC | gRPC RPC | 提升 |
|------|----------|----------|------|
| QPS | ~3000 | ~12000 | 4x |
| 平均延迟 | ~15ms | ~3ms | 5x |
| P99 延迟 | ~50ms | ~10ms | 5x |
| CPU 使用率 | 60% | 40% | -33% |
| 内存使用 | 500MB | 400MB | -20% |

## RPC 调用的优势

### 1. 类型安全

```java
// 编译时类型检查
CreateUserDto.Response response = userRpcService.createUser(request);
//  编译通过

CreateUserDto.Response response = userRpcService.getUser(request);
//  编译失败: 方法不存在
```

### 2. 代码简洁

```java
// HTTP 方式(使用 RestTemplate)
String url = "http://localhost:8000/rpc/users";
HttpEntity<CreateUserDto.Request> entity = new HttpEntity<>(request, headers);
ResponseEntity<CreateUserDto.Response> response = restTemplate.postForEntity(url, entity, CreateUserDto.Response.class);
CreateUserDto.Response result = response.getBody();

// gRPC RPC 方式
CreateUserDto.Response result = userRpcService.createUser(request);
```

### 3. 服务发现集成

```java
// 自动从 Nacos 获取服务实例
@RemoteService(value = "nebula-example")  // 应用名,从 Nacos 发现
public interface UserRpcService {
    // ...
}
```

### 4. 负载均衡

```yaml
nebula:
  rpc:
    discovery:
      load-balance-strategy: ROUND_ROBIN  # 轮询
```

### 5. 服务治理

- **重试机制**: 自动重试失败的请求
- **超时控制**: 防止请求永久阻塞
- **降级处理**: 服务不可用时的兜底方案

## 日志分析

### 查看 gRPC 服务注册日志

```bash
tail -f logs/nebula-example.log | grep "注册 gRPC RPC 服务"
```

**预期输出**:

```
注册 gRPC RPC 服务: io.nebula.example.api.rpc.UserRpcService
```

### 查看 gRPC 调用日志

```bash
tail -f logs/nebula-example.log | grep "gRPC RPC"
```

**预期输出**:

```
执行 gRPC RPC 调用: requestId=xxx, service=io.nebula.example.api.rpc.UserRpcService, method=createUser
gRPC RPC 调用成功: requestId=xxx, service=io.nebula.example.api.rpc.UserRpcService, method=createUser
```

## 常见问题

### 1. 端口冲突

**症状**: `Port 9090 was already in use.`

**解决方案**:

```bash
# 查找占用端口的进程
lsof -i :9090

# 终止进程
kill -9 <PID>

# 或修改配置使用其他端口
nebula.rpc.grpc.server.port=9091
```

### 2. 连接超时

**症状**: `DEADLINE_EXCEEDED`

**解决方案**:

```yaml
nebula:
  rpc:
    grpc:
      client:
        request-timeout: 120000  # 增加超时时间到 120 秒
```

### 3. 序列化错误

**症状**: `Cannot deserialize`

**解决方案**:

- 确保 DTO 有 `@NoArgsConstructor` 和 `@AllArgsConstructor`
- 检查 JSON 格式是否正确

### 4. 方法未找到

**症状**: `方法未找到: methodName`

**解决方案**:

- 确保服务实现类添加了 `@RpcService` 注解
- 检查方法签名是否与接口定义一致

## 总结

gRPC RPC 模块提供了:

1.  **高性能**: 基于 HTTP/2 和 Protocol Buffers
2.  **易用性**: 与 `@RpcService` 无缝集成
3.  **灵活性**: 支持 HTTP 和 gRPC 双协议
4.  **可靠性**: 内置重试超时降级机制
5.  **可维护性**: 类型安全的 RPC 调用

根据实际需求选择合适的 RPC 协议:

- **中低并发(QPS < 5000)**: 使用 HTTP RPC,简单易调试
- **高并发(QPS > 10000)**: 使用 gRPC RPC,高性能低延迟
- **混合场景**: 同时启用两种协议,根据客户端能力选择

## 参考资料

- [gRPC 官方文档](https://grpc.io/docs/)
- [Protocol Buffers](https://protobuf.dev/)
- [gRPCurl 使用指南](https://github.com/fullstorydev/grpcurl)
- [ghz 压测工具](https://ghz.sh/)


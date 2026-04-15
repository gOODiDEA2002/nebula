# Nebula RPC HTTP 功能测试指南

## 概述

这个指南详细介绍如何测试 Nebula RPC HTTP 模块的各种功能，包括声明式调用编程式调用服务发现集成等

## 启动应用

```bash
cd nebula-example
mvn spring-boot:run
```

应用启动后，访问：http://localhost:8000

## API 文档

访问 Swagger UI 查看完整的 API 文档：
- Swagger UI: http://localhost:8000/swagger-ui/index.html
- API Docs: http://localhost:8000/v3/api-docs

## 测试场景

### 场景一：基础 RPC 服务调用

#### 1. 创建用户

##### 1.1 直接调用（RPC Server）

```bash
curl -X POST http://localhost:8000/rpc/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "name": "测试用户",
    "email": "test@example.com",
    "phone": "13800138000",
    "status": "ACTIVE"
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "id": 11
  },
  "success": true
}
```

##### 1.2 通过 RPC 客户端调用

```bash
curl -X POST http://localhost:8000/rpc-client/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "rpcuser",
    "name": "RPC测试用户",
    "email": "rpc@example.com",
    "phone": "13900139000",
    "status": "ACTIVE"
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "RPC创建用户成功",
  "data": {
    "id": 12
  },
  "success": true
}
```

**说明**：
- `/rpc` - RPC Server 底层端点(接收序列化的 RPC 请求,一般不直接调用)
- `/rpc-client/users` - 通过 RPC 客户端调用远程服务(声明式RPC调用,推荐方式)
- RPC 客户端会自动将请求序列化并发送到 `/rpc` 端点,然后反序列化响应

#### 2. 查询用户

##### 2.1 根据 ID 查询

RPC 客户端调用：
```bash
curl "http://localhost:8000/rpc-client/users/1"
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "获取用户详情成功",
  "data": {
    "user": {
      "id": 1,
      "username": "user1",
      "name": "测试用户1",
      "email": "user1@example.com",
      "phone": "13800000001",
      "status": "ACTIVE"
    }
  },
  "success": true
}
```

##### 2.2 分页查询用户列表

RPC 客户端调用：
```bash
curl "http://localhost:8000/rpc-client/users?page=1&size=5"
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "查询用户列表成功",
  "data": {
    "users": [
      {
        "id": 1,
        "username": "user1",
        "name": "测试用户1",
        "email": "user1@example.com",
        "phone": "13800000001",
        "status": "ACTIVE"
      }
    ],
    "total": 10,
    "page": 1,
    "size": 5
  },
  "success": true
}
```

##### 2.3 条件查询

```bash
# 按用户名查询
curl "http://localhost:8000/rpc-client/users?username=user1"

# 按状态查询
curl "http://localhost:8000/rpc-client/users?status=ACTIVE&page=1&size=10"

# 组合查询
curl "http://localhost:8000/rpc-client/users?name=测试&status=ACTIVE"
```

#### 3. 更新用户

RPC 客户端调用：
```bash
curl -X PUT http://localhost:8000/rpc-client/users/1 \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "name": "RPC更新后的用户名",
    "email": "rpcnewemail@example.com"
  }'
```

**注意**：更新请求需要在请求体中包含 `id` 字段,即使 URL 路径中已经有 ID

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "更新用户成功",
  "data": {
    "user": {
      "id": 1,
      "username": "user1",
      "name": "RPC更新后的用户名",
      "email": "rpcnewemail@example.com",
      "phone": "13900000001",
      "status": "ACTIVE"
    }
  },
  "success": true
}
```

#### 4. 删除用户

RPC 客户端调用：
```bash
curl -X DELETE http://localhost:8000/rpc-client/users/12
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "删除用户成功",
  "data": {
    "success": true
  },
  "success": true
}
```

### 场景二：参数验证测试

#### 1. 缺少必填字段

```bash
curl -X POST http://localhost:8000/rpc-client/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "测试用户"
  }'
```

预期响应：400 Bad Request，包含验证错误信息

#### 2. 邮箱格式错误

```bash
curl -X POST http://localhost:8000/rpc-client/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test",
    "name": "测试",
    "email": "invalid-email",
    "phone": "13800138000"
  }'
```

#### 3. 用户名格式错误

```bash
curl -X POST http://localhost:8000/rpc-client/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test@user",
    "name": "测试",
    "email": "test@example.com"
  }'
```

### 场景三：RPC 调用性能测试

#### 1. 批量创建用户

```bash
# 创建测试脚本
cat > batch_create_users.sh << 'EOF'
#!/bin/bash
for i in {1..100}
do
  curl -s -X POST http://localhost:8000/rpc-client/users \
    -H "Content-Type: application/json" \
    -d "{
      \"username\": \"batchuser$i\",
      \"name\": \"批量用户$i\",
      \"email\": \"batch$i@example.com\",
      \"phone\": \"13800000$i\",
      \"status\": \"ACTIVE\"
    }" > /dev/null
  echo "Created user $i"
done
EOF

chmod +x batch_create_users.sh
time ./batch_create_users.sh
```

#### 2. 并发查询测试

使用 Apache Bench 进行并发测试：

```bash
# 安装 ab 工具 (如果未安装)
# brew install httpd  # macOS
# apt-get install apache2-utils  # Ubuntu

# 100个请求，10个并发
ab -n 100 -c 10 http://localhost:8000/rpc-client/users?page=1&size=10

# 1000个请求，50个并发
ab -n 1000 -c 50 http://localhost:8000/rpc-client/users/1
```

### 场景四：异常处理测试

#### 1. 查询不存在的用户

```bash
curl "http://localhost:8000/rpc-client/users/99999"
```

#### 2. 删除不存在的用户

```bash
curl -X DELETE http://localhost:8000/rpc-client/users/99999
```

#### 3. 超时测试

修改配置降低超时时间，然后执行查询：

```yaml
nebula:
  rpc:
    http:
      client:
        read-timeout: 100  # 设置很短的超时
```

## 功能验证清单

###  基础 RPC 功能
- [x] 创建用户 - RPC 客户端调用成功
- [x] 查询用户 - RPC 客户端调用成功
- [x] 更新用户 - RPC 客户端调用成功
- [x] 删除用户 - RPC 客户端调用成功
- [x] 分页查询 - RPC 客户端调用成功

###  声明式调用
- [x] @RemoteService 注解生效
- [x] @RpcCall 注解生效
- [x] 自动代理创建成功
- [x] 参数传递正确

###  数据验证
- [x] 必填字段验证
- [x] 格式验证（邮箱电话）
- [x] 长度限制验证
- [x] 枚举值验证

###  异常处理
- [x] 数据不存在处理
- [x] 参数验证失败处理
- [x] 超时处理
- [x] 连接失败处理

###  性能指标
- [x] 单次调用延迟 < 100ms
- [x] 并发支持 > 50 QPS
- [x] 批量操作成功率 > 99%

## RPC 调用的优势

使用 Nebula RPC HTTP 模块的声明式调用方式相比传统 HTTP REST 调用具有以下优势:

| 特性 | 传统 HTTP REST | Nebula RPC 调用 |
|------|----------------|-----------------|
| 调用方式 | 手动构建 HTTP 请求 | 声明式接口调用 |
| 透明度 | 低(需要知道具体 URL) | 高(只需要接口定义) |
| 耦合度 | 高 | 低 |
| 服务发现 | 需要额外配置 | 内置支持 |
| 负载均衡 | 需要额外实现 | 内置支持 |
| 降级处理 | 需要手动实现 | 支持 fallback |
| 重试机制 | 需要手动实现 | 内置支持 |
| 类型安全 | 弱(字符串拼接) | 强(编译时检查) |
| 参数验证 | 需要手动处理 | 自动验证 |

## 调试技巧

### 1. 启用详细日志

```yaml
logging:
  level:
    io.nebula.rpc: TRACE
    io.nebula.rpc.core: TRACE
    io.nebula.rpc.http: TRACE
    org.springframework.web.client: DEBUG
```

### 2. 查看 RPC 请求详情

在日志中查找以下信息：
- RPC 客户端创建: `注册RPC客户端`
- RPC 调用: `执行RPC调用`
- 请求响应: `RPC调用成功/失败`

### 3. 使用 Actuator 监控

访问：http://localhost:8000/actuator/health

## 故障排查

### 1. RPC 客户端未注册

**问题**: `No qualifying bean of type 'UserRpcService'`

**解决**:
- 确认 `@EnableRpcClients` 注解已添加
- 检查 basePackages 配置是否正确
- 确认 RPC 接口有 `@RemoteService` 注解

### 2. 连接超时

**问题**: `Connect timeout`

**解决**:
- 检查服务端是否正常运行
- 验证 base-url 配置
- 增加 connect-timeout 配置

### 3. 读取超时

**问题**: `Read timeout`

**解决**:
- 优化服务端处理性能
- 增加 read-timeout 配置
- 使用异步调用

## 开发建议

1. **接口设计**
   - 使用 DTO 规范，Request/Response 分离
   - 添加完整的参数验证注解
   - 提供清晰的接口文档

2. **性能优化**
   - 对非关键路径使用异步调用
   - 合理配置连接池大小
   - 启用请求压缩

3. **异常处理**
   - 实现 fallback 降级逻辑
   - 配置合理的重试策略
   - 记录详细的错误日志

4. **测试规范**
   - 编写单元测试覆盖核心逻辑
   - 进行压力测试验证性能
   - 模拟异常场景测试容错性

---

更多详细信息，请参考：
- [Nebula RPC HTTP 模块文档](../../nebula/infrastructure/rpc/nebula-rpc-http/README.md)
- [Nebula 框架使用指南](../../nebula/docs/Nebula框架使用指南.md)


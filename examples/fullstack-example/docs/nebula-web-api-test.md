# Nebula Web 功能测试指南

## 概述

这个指南详细介绍如何测试 Nebula Web 框架的各种功能，包括认证限流缓存性能监控数据脱敏等

## 启动应用

```bash
cd nebula-example
mvn spring-boot:run
```

应用启动后，访问：http://localhost:8000

## API 接口测试

### 1. 健康检查接口

#### 1.1 基础健康检查
```bash
curl http://localhost:8000/health
```

#### 1.2 详细健康状态
```bash
curl http://localhost:8000/health/status
```

#### 1.3 存活探针
```bash
curl http://localhost:8000/health/liveness
```

#### 1.4 就绪探针
```bash
curl http://localhost:8000/health/readiness
```

### 2. HelloController 功能测试（无需认证）

#### 2.1 基础接口
```bash
curl http://localhost:8000/hello
```

响应示例：
```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "message": "Hello from Nebula Framework!",
    "timestamp": "2025-09-25T10:30:00",
    "status": "success",
    "framework": "Nebula 2.0.1-SNAPSHOT",
    "requestId": 1,
    "features": [
      "认证系统", "限流控制", "响应缓存", "性能监控", 
      "健康检查", "数据脱敏", "请求日志", "全局异常处理"
    ]
  },
  "success": true
}
```

#### 2.2 缓存功能测试
第一次访问（慢）：
```bash
curl http://localhost:8000/hello/cached-data/123
```

第二次访问（快，从缓存返回）：
```bash
curl http://localhost:8000/hello/cached-data/123
```

注意响应头中的 `X-Cache: HIT` 或 `X-Cache: MISS`

#### 2.3 性能监控 - 慢请求测试
```bash
# 2秒延迟的慢请求
curl "http://localhost:8000/hello/slow?delayMs=2000"

# 5秒延迟的超慢请求
curl "http://localhost:8000/hello/slow?delayMs=5000"
```

#### 2.4 数据脱敏演示
```bash
curl http://localhost:8000/hello/sensitive-data
```

响应示例（敏感数据已脱敏）：
```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "name": "张*",
    "email": "zha***@example.com",
    "mobile": "138****8888",
    "idCard": "110***********1234",
    "bankCard": "6222****4567",
    "address": "北京市***区",
    "password": "******",
    "ipAddress": "192.168.*.*"
  },
  "success": true
}
```

#### 2.5 限流功能测试
快速多次调用来触发限流：
```bash
# 使用循环快速调用
for i in {1..15}; do
  echo "Request $i:"
  curl http://localhost:8000/hello/rate-limit-test
  echo -e "\n"
done
```

当触发限流时，会返回：
```json
{
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "请求过于频繁，请稍后再试",
  "success": false
}
```

#### 2.6 全局异常处理测试
```bash
# 业务异常
curl "http://localhost:8000/hello/error?type=business"

# 运行时异常
curl "http://localhost:8000/hello/error?type=runtime"

# 空指针异常
curl "http://localhost:8000/hello/error?type=null"
```

#### 2.7 批量数据接口
```bash
curl "http://localhost:8000/hello/batch-data?count=5"
```

#### 2.8 系统信息接口
```bash
curl http://localhost:8000/hello/system-info
```

#### 2.9 POST 请求测试
```bash
curl -X POST http://localhost:8000/hello/submit \
  -H "Content-Type: application/json" \
  -d '{"name": "test", "value": 123}'
```

### 3. 认证功能测试

#### 3.1 未认证访问受保护资源（会被拦截）
```bash
curl http://localhost:8000/auth
```

预期响应：
```json
{
  "code": "AUTHENTICATION_FAILED",
  "message": "缺少认证令牌",
  "success": false
}
```

#### 3.2 登录获取 Token
```bash
curl -X POST http://localhost:8000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "password"}'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "userId": "1001",
      "username": "admin",
      "email": "adm***@example.com",
      "mobile": "138****8888"
    },
    "loginTime": "2025-09-25T10:30:00"
  },
  "success": true
}
```

保存返回的 token，用于后续认证请求

#### 3.3 使用 Token 访问受保护资源
```bash
# 替换 YOUR_TOKEN 为实际的 token
curl http://localhost:8000/auth \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### 3.4 获取用户信息
```bash
curl http://localhost:8000/auth/profile \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### 3.5 权限检查
```bash
curl http://localhost:8000/auth/admin/users \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### 3.6 刷新 Token
```bash
curl -X POST http://localhost:8000/auth/refresh \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### 3.7 登出
```bash
curl -X POST http://localhost:8000/auth/logout \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 4. 性能监控接口

#### 4.1 应用性能指标
```bash
curl http://localhost:8000/performance/metrics
```

#### 4.2 系统指标
```bash
curl http://localhost:8000/performance/system
```

#### 4.3 综合状态
```bash
curl http://localhost:8000/performance/status
```

#### 4.4 重置性能指标
```bash
curl -X POST http://localhost:8000/performance/reset
```

### 5. API 文档

访问 Swagger UI：http://localhost:8000/swagger-ui.html

## 功能验证清单

###  认证系统
- [x] JWT Token 生成和验证
- [x] 认证拦截器工作正常
- [x] 忽略路径配置生效
- [x] 认证上下文设置
- [x] 权限检查功能

###  限流功能
- [x] IP 基础限流
- [x] 限流触发返回 429 状态
- [x] 限流信息响应头
- [x] 限流配置生效

###  响应缓存
- [x] GET 请求自动缓存
- [x] 缓存命中/未命中标识
- [x] TTL 过期管理
- [x] POST 请求不缓存

###  性能监控
- [x] 请求统计指标
- [x] 慢请求检测
- [x] 系统资源监控
- [x] JVM 指标收集

###  健康检查
- [x] 应用健康状态
- [x] 内存/磁盘检查
- [x] 自定义健康检查器
- [x] Kubernetes 探针

###  数据脱敏
- [x] 敏感数据自动脱敏
- [x] 多种脱敏策略
- [x] JSON 序列化集成
- [x] 注解驱动配置

###  请求日志
- [x] 请求响应日志记录
- [x] 敏感信息脱敏
- [x] 性能影响控制
- [x] 忽略路径配置

###  全局异常处理
- [x] 业务异常统一处理
- [x] 运行时异常捕获
- [x] 错误响应格式化
- [x] 日志记录

## 故障排查

### 1. 认证问题
- 检查 JWT secret 长度（至少 256 位）
- 确认路径是否在 ignore-paths 中
- 验证 Authorization header 格式

### 2. 限流不生效
- 确认 rate-limit.enabled=true
- 检查限流配置参数
- 查看控制台日志

### 3. 缓存不工作
- 只有 GET 请求会被缓存
- 检查响应状态码（必须是 2xx）
- 确认缓存配置启用

### 4. 性能监控异常
- 检查 performance.enabled=true
- 确认相关 Bean 正确注册
- 查看错误日志

## 开发建议

1. **开发环境配置**
   - 启用详细的健康检查信息
   - 降低限流阈值便于测试
   - 启用异常详情响应

2. **生产环境配置**
   - 使用环境变量配置 JWT secret
   - 关闭异常详情响应
   - 调整限流和缓存参数

3. **监控集成**
   - 配置 APM 工具
   - 设置告警规则
   - 定期查看性能指标

## 扩展功能

1. **自定义认证服务**
   - 实现 AuthService 接口
   - 集成第三方认证系统
   - 添加用户管理功能

2. **分布式缓存**
   - 集成 Redis 缓存
   - 实现 ResponseCache 接口
   - 配置缓存集群

3. **高级限流**
   - 实现分布式限流
   - 添加动态限流规则
   - 集成限流中心

4. **监控增强**
   - 集成 Micrometer
   - 配置 Prometheus
   - 添加业务指标

---

更多详细信息，请参考 [Nebula Web 使用指南](../../nebula/application/nebula-web/README.md)

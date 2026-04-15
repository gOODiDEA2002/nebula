# Nebula 服务发现功能测试指南

## 概述

这个指南详细介绍如何测试 Nebula 服务发现层的各种功能，包括服务注册服务发现服务订阅负载均衡等

## 环境准备

### 1. 启动 Nacos 服务器

```bash
# 使用 Docker 启动 Nacos（单机模式）
docker run -d \
  --name nacos \
  -e MODE=standalone \
  -p 8848:8848 \
  -p 9848:9848 \
  nacos/nacos-server:latest
```

或者使用项目提供的 docker-compose：

```bash
cd nebula-data
docker-compose up -d nacos
```

访问 Nacos 控制台：http://localhost:8848/nacos

- 用户名: `nacos`
- 密码: `nacos`

### 2. 配置应用

在 `application.yml` 中配置 Nacos 服务发现：

```yaml
nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
      namespace: nebula-dev
      group-name: DEFAULT_GROUP
      cluster-name: DEFAULT
      username: nacos
      password: nacos
```

### 3. 启动应用

```bash
cd nebula-example
mvn spring-boot:run
```

应用启动后，访问：http://localhost:8000

## API 接口测试

### 1. 注册服务实例

#### 1.1 基础服务注册

```bash
curl -X POST http://localhost:8000/discovery/services/register \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "user-service",
    "instanceId": "user-service-001",
    "ip": "192.168.1.100",
    "port": 8080,
    "weight": 1.0,
    "clusterName": "DEFAULT",
    "groupName": "DEFAULT_GROUP",
    "protocol": "http",
    "metadata": {
      "version": "1.0.0",
      "env": "dev"
    }
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "success": true,
    "address": "http://192.168.1.100:8080",
    "message": "服务注册成功"
  },
  "success": true
}
```

#### 1.2 注册多个实例（负载均衡测试）

```bash
# 实例 1
curl -X POST http://localhost:8000/discovery/services/register \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "order-service",
    "instanceId": "order-service-001",
    "ip": "192.168.1.101",
    "port": 8081,
    "weight": 1.0
  }'

# 实例 2
curl -X POST http://localhost:8000/discovery/services/register \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "order-service",
    "instanceId": "order-service-002",
    "ip": "192.168.1.102",
    "port": 8081,
    "weight": 2.0
  }'

# 实例 3
curl -X POST http://localhost:8000/discovery/services/register \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "order-service",
    "instanceId": "order-service-003",
    "ip": "192.168.1.103",
    "port": 8081,
    "weight": 1.5
  }'
```

#### 1.3 注册带元数据的服务

```bash
curl -X POST http://localhost:8000/discovery/services/register \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "payment-service",
    "instanceId": "payment-service-001",
    "ip": "192.168.1.104",
    "port": 8082,
    "weight": 1.0,
    "metadata": {
      "version": "2.1.0",
      "env": "production",
      "region": "cn-hangzhou",
      "zone": "zone-a",
      "team": "payment-team"
    }
  }'
```

### 2. 查询服务实例

#### 2.1 查询所有健康实例

```bash
curl -X POST http://localhost:8000/discovery/services/instances \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "order-service",
    "healthyOnly": true
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "查询服务实例成功",
  "data": {
    "instances": [
      {
        "serviceName": "order-service",
        "instanceId": "order-service-001",
        "ip": "192.168.1.101",
        "port": 8081,
        "weight": 1.0,
        "healthy": true,
        "enabled": true,
        "clusterName": "DEFAULT",
        "groupName": "DEFAULT_GROUP",
        "protocol": "http",
        "address": "http://192.168.1.101:8081",
        "available": true
      },
      {
        "serviceName": "order-service",
        "instanceId": "order-service-002",
        "ip": "192.168.1.102",
        "port": 8081,
        "weight": 2.0,
        "healthy": true,
        "enabled": true,
        "address": "http://192.168.1.102:8081",
        "available": true
      }
    ],
    "total": 2
  },
  "success": true
}
```

#### 2.2 查询所有实例（包括不健康实例）

```bash
curl -X POST http://localhost:8000/discovery/services/instances \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "order-service",
    "healthyOnly": false
  }'
```

#### 2.3 查询指定分组的实例

```bash
curl -X POST http://localhost:8000/discovery/services/instances \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "user-service",
    "groupName": "DEFAULT_GROUP",
    "healthyOnly": true
  }'
```

### 3. 获取所有服务列表

#### 3.1 获取全部服务

```bash
curl -X POST http://localhost:8000/discovery/services/all \
  -H "Content-Type: application/json" \
  -d '{}'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "查询所有服务成功",
  "data": {
    "services": [
      "user-service",
      "order-service",
      "payment-service"
    ],
    "total": 3
  },
  "success": true
}
```

#### 3.2 分页获取服务列表

```bash
curl -X POST http://localhost:8000/discovery/services/all \
  -H "Content-Type: application/json" \
  -d '{
    "pageNo": 1,
    "pageSize": 10
  }'
```

#### 3.3 获取指定分组的服务

```bash
curl -X POST http://localhost:8000/discovery/services/all \
  -H "Content-Type: application/json" \
  -d '{
    "groupName": "DEFAULT_GROUP"
  }'
```

### 4. 订阅服务变化

#### 4.1 订阅服务

```bash
curl -X POST http://localhost:8000/discovery/services/subscribe \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "order-service"
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "success": true,
    "message": "订阅服务变化成功"
  },
  "success": true
}
```

#### 4.2 订阅指定分组的服务

```bash
curl -X POST http://localhost:8000/discovery/services/subscribe \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "user-service",
    "groupName": "DEFAULT_GROUP"
  }'
```

#### 4.3 测试服务变化通知

订阅后，执行以下操作观察日志中的变化通知：

```bash
# 注册新实例
curl -X POST http://localhost:8000/discovery/services/register \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "order-service",
    "instanceId": "order-service-004",
    "ip": "192.168.1.105",
    "port": 8081
  }'
```

查看应用日志，应该能看到类似的输出：
```
服务变化通知: serviceName=order-service, instanceCount=4
```

### 5. 注销服务实例

#### 5.1 注销单个实例

```bash
curl -X POST http://localhost:8000/discovery/services/deregister \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "order-service",
    "instanceId": "order-service-001"
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "success": true,
    "message": "服务注销成功"
  },
  "success": true
}
```

#### 5.2 批量注销实例

```bash
# 注销实例1
curl -X POST http://localhost:8000/discovery/services/deregister \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "order-service",
    "instanceId": "order-service-002"
  }'

# 注销实例2
curl -X POST http://localhost:8000/discovery/services/deregister \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "order-service",
    "instanceId": "order-service-003"
  }'
```

### 6. 取消订阅服务变化

```bash
curl -X POST http://localhost:8000/discovery/services/unsubscribe \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "order-service"
  }'
```

## 功能验证清单

###  基础服务注册与发现
- [x] 注册单个服务实例 - 成功注册并返回地址
- [x] 注册多个服务实例 - 支持同一服务的多个实例
- [x] 查询服务实例 - 正确返回服务实例列表
- [x] 查询所有服务 - 返回注册中心的所有服务名称

###  服务健康检查
- [x] 健康实例过滤 - 只返回健康的实例
- [x] 包含不健康实例 - healthyOnly=false 时返回所有实例
- [x] 实例可用性判断 - 正确计算实例是否可用

###  服务元数据
- [x] 注册带元数据的服务 - 元数据正确存储
- [x] 查询包含元数据 - 查询结果包含元数据信息
- [x] 自定义元数据字段 - 支持任意键值对

###  服务订阅
- [x] 订阅服务变化 - 成功订阅服务
- [x] 接收变化通知 - 服务变化时触发监听器
- [x] 取消订阅 - 成功取消订阅

###  服务注销
- [x] 注销单个实例 - 成功注销并从注册中心移除
- [x] 注销后查询 - 注销的实例不再出现在查询结果中
- [x] 注销触发变化通知 - 订阅者收到服务变化通知

###  分组与集群
- [x] 指定分组查询 - 支持按分组查询服务实例
- [x] 指定集群注册 - 支持将实例注册到指定集群
- [x] 跨分组隔离 - 不同分组的服务互不影响

## 在 Nacos 控制台验证

### 1. 查看服务列表

访问：http://localhost:8848/nacos/#/serviceManagement

应该能看到注册的所有服务：
- user-service
- order-service
- payment-service

### 2. 查看服务详情

点击服务名称，查看服务实例详情：
- 实例IP和端口
- 健康状态
- 权重配置
- 元数据信息
- 集群信息

### 3. 管理服务实例

在控制台可以：
- 上线/下线实例
- 编辑实例信息
- 修改权重
- 查看实例健康检查详情

## 负载均衡测试

### 1. 轮询负载均衡

```bash
# 注册3个实例
for i in {1..3}; do
  curl -X POST http://localhost:8000/discovery/services/register \
    -H "Content-Type: application/json" \
    -d "{
      \"serviceName\": \"lb-test-service\",
      \"instanceId\": \"lb-test-00$i\",
      \"ip\": \"192.168.1.10$i\",
      \"port\": 8080,
      \"weight\": 1.0
    }"
done

# 查询实例
curl -X POST http://localhost:8000/discovery/services/instances \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "lb-test-service"
  }'
```

使用负载均衡器选择实例：
```java
// 在应用代码中
List<ServiceInstance> instances = serviceDiscovery.getInstances("lb-test-service");
LoadBalancer loadBalancer = LoadBalancerFactory.create(LoadBalanceStrategy.ROUND_ROBIN);

// 轮询选择实例
for (int i = 0; i < 10; i++) {
    ServiceInstance instance = loadBalancer.choose(instances);
    System.out.println("选中实例: " + instance.getInstanceId());
}
```

### 2. 加权随机负载均衡

```bash
# 注册不同权重的实例
curl -X POST http://localhost:8000/discovery/services/register \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "weighted-service",
    "instanceId": "weighted-001",
    "ip": "192.168.1.101",
    "port": 8080,
    "weight": 1.0
  }'

curl -X POST http://localhost:8000/discovery/services/register \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "weighted-service",
    "instanceId": "weighted-002",
    "ip": "192.168.1.102",
    "port": 8080,
    "weight": 3.0
  }'
```

理论上 weighted-002 被选中的概率应该是 weighted-001 的3倍

## 高可用测试

### 1. 实例下线测试

```bash
# 1. 注册服务实例
curl -X POST http://localhost:8000/discovery/services/register \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "ha-test-service",
    "instanceId": "ha-test-001",
    "ip": "192.168.1.101",
    "port": 8080
  }'

# 2. 订阅服务变化
curl -X POST http://localhost:8000/discovery/services/subscribe \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "ha-test-service"
  }'

# 3. 在 Nacos 控制台手动下线实例，观察应用日志

# 4. 查询服务实例，验证下线实例不再返回
curl -X POST http://localhost:8000/discovery/services/instances \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "ha-test-service"
  }'
```

### 2. 服务恢复测试

```bash
# 在 Nacos 控制台手动上线实例
# 订阅者应该收到变化通知
# 再次查询应该能看到实例
```

## 性能测试

### 1. 批量注册性能

```bash
# 测试批量注册100个实例的耗时
time for i in {1..100}; do
  curl -X POST http://localhost:8000/discovery/services/register \
    -H "Content-Type: application/json" \
    -d "{
      \"serviceName\": \"perf-test-service\",
      \"instanceId\": \"perf-test-$(printf %03d $i)\",
      \"ip\": \"192.168.1.$(( (i % 254) + 1 ))\",
      \"port\": $((8000 + i))
    }" > /dev/null 2>&1
done
```

### 2. 查询性能测试

```bash
# 测试查询大量实例的性能
time curl -X POST http://localhost:8000/discovery/services/instances \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "perf-test-service"
  }'
```

### 3. 订阅通知性能

观察订阅服务变化时的通知延迟

## 故障排查

### 常见问题

#### 1. 无法连接 Nacos
```bash
# 检查 Nacos 是否启动
docker ps | grep nacos

# 检查端口是否监听
netstat -an | grep 8848

# 查看 Nacos 日志
docker logs nacos
```

#### 2. 服务注册失败
- 检查服务名称是否合法（不能包含特殊字符）
- 确认 namespace 和 groupName 配置正确
- 查看应用日志中的错误信息

#### 3. 查询不到服务实例
- 确认服务已成功注册
- 检查 groupName 是否一致
- 验证 healthyOnly 参数设置
- 在 Nacos 控制台查看实例状态

#### 4. 订阅不生效
- 确认订阅方法调用成功
- 检查服务名称是否正确
- 查看应用日志中的订阅确认信息

### 开启调试日志

```yaml
logging:
  level:
    io.nebula.discovery: DEBUG
    io.nebula.example.modules.discovery: DEBUG
    com.alibaba.nacos: DEBUG
```

## 开发建议

### 1. 服务命名规范

```yaml
# 推荐的命名方式
serviceName: user-service       # 小写，连字符分隔
instanceId: user-service-001    # 服务名 + 实例编号
groupName: PRODUCT_GROUP        # 大写，下划线分隔
clusterName: BEIJING_CLUSTER    # 大写，下划线分隔
```

### 2. 元数据最佳实践

```json
{
  "version": "1.0.0",      // 服务版本
  "env": "production",     // 环境
  "region": "cn-hangzhou", // 地域
  "zone": "zone-a",        // 可用区
  "team": "order-team"     // 团队
}
```

### 3. 错误处理

```java
try {
    serviceDiscovery.register(instance);
} catch (ServiceDiscoveryException e) {
    log.error("服务注册失败", e);
    // 实现重试逻辑或告警
}
```

### 4. 优雅下线

```java
@PreDestroy
public void deregister() {
    try {
        serviceDiscovery.deregister(serviceName, instanceId);
        log.info("服务实例已注销");
    } catch (Exception e) {
        log.error("服务注销失败", e);
    }
}
```

## 更多功能

- [Nebula Discovery Nacos 使用指南](../../nebula/infrastructure/discovery/nebula-discovery-nacos/README.md)
- [Nebula Discovery Core 核心抽象](../../nebula/infrastructure/discovery/nebula-discovery-core/README.md)
- [完整示例项目](../../nebula-example)

---

**Nebula 服务发现 - 基于 Nacos 的企业级服务注册与发现解决方案**



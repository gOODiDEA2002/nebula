# Nebula 读写分离功能测试指南

## 概述

这个指南详细介绍如何测试 Nebula 数据持久层的读写分离功能，包括注解驱动的数据源切换编程式数据源控制事务内的数据源选择策略等

## 读写分离简介

Nebula 的读写分离功能提供了以下特性：
- **注解驱动**: 使用 `@ReadDataSource` 和 `@WriteDataSource` 注解自动切换数据源
- **编程式控制**: 使用 `DataSourceContextHolder` 手动控制数据源选择
- **事务支持**: 支持事务内的数据源选择策略
- **负载均衡**: 支持多个读库的负载均衡（轮询随机加权轮询）
- **故障转移**: 读库不可用时自动切换到写库

## 启动应用（读写分离模式）

### 1. 配置数据库

由于演示环境限制，我们使用同一个数据库模拟主从：

```bash
# 连接到 MySQL
mysql -u root -p

# 确保数据库存在
CREATE DATABASE IF NOT EXISTS `nebula_example` 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE `nebula_example`;

# 执行建表脚本
source sql/data-demo-tables.sql
```

### 2. 启动应用

```bash
cd nebula-example

# 使用读写分离配置启动
mvn spring-boot:run -Dspring.profiles.active=dev,readwrite
```

应用启动后，访问：http://localhost:8000

### 3. 验证读写分离配置

检查启动日志，确保看到类似信息：
```
INFO  i.n.d.p.readwrite.ReadWriteDataSourceManager - Read-write separation clusters configured: [default]
INFO  i.n.d.p.readwrite.autoconfigure.ReadWriteDataSourceAutoConfiguration - Creating DynamicDataSource for default cluster
INFO  i.n.d.p.readwrite.autoconfigure.ReadWriteDataSourceAutoConfiguration - Creating ReadWriteDataSourceAspect
```

## API 接口测试

### 1. 数据源状态检查

#### 1.1 检查数据源配置

```bash
curl http://localhost:8000/readwrite/status
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "数据源状态检查完成",
  "data": {
    "currentContext": "Thread: http-nio-8000-exec-1, DataSourceType: null",
    "readWriteSeparationEnabled": true,
    "defaultCluster": "default",
    "availableDataSources": ["primary", "slave1", "slave2"],
    "loadBalanceStrategy": "ROUND_ROBIN",
    "readTest": "READ_1703123456789",
    "writeTest": "WRITE_1703123456790",
    "timestamp": "2025-09-29T15:30:45"
  },
  "success": true
}
```

#### 1.2 负载均衡测试

```bash
curl "http://localhost:8000/readwrite/load-balance-test?rounds=5"
```

观察响应中每轮的数据源选择，验证负载均衡是否按预期工作

### 2. 注解驱动的读写分离

#### 2.1 写操作测试

```bash
curl -X POST http://localhost:8000/readwrite/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "读写分离测试产品",
    "description": "这个产品用于测试写数据源",
    "price": 1999.00,
    "category": "测试分类",
    "stockQuantity": 50,
    "status": "ACTIVE"
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "产品创建成功，使用写数据源",
  "data": {
    "productId": 67,
    "dataSourceContext": "Thread: http-nio-8000-exec-2, DataSourceType: WRITE",
    "operation": "WRITE",
    "timestamp": "2025-09-29T15:35:20"
  },
  "success": true
}
```

**验证点**：
- `dataSourceContext` 中显示 `DataSourceType: WRITE`
- 控制台日志显示使用写数据源

#### 2.2 读操作测试

```bash
curl "http://localhost:8000/readwrite/products/?id=1"
```

成功响应：
```json
{"success":true,"code":"SUCCESS","message":"获取产品详情成功","data":{"product":{"id":1,"name":"iPhone 15 Pro","category":"电子产品","price":8999.00,"stockQuantity":100,"status":"ACTIVE","description":"苹果最新旗舰手机，配备A17 Pro芯片"}},"timestamp":"2025-09-29T17:33:08.335519"}
```

**验证点**：
- `dataSourceContext` 中显示 `DataSourceType: READ`
- 控制台日志显示使用读数据源

### 3. 编程式数据源控制

#### 3.1 编程式读操作

```bash
curl "http://localhost:8000/readwrite/programmatic/read/products?page=1&size=3"
```

成功响应：
```json
{
    "success": true,
    "code": "SUCCESS",
    "message": "获取产品详情成功",
    "data":
    {
        "product":
        {
            "id": 1,
            "name": "iPhone 15 Pro",
            "category": "电子产品",
            "price": 8999.00,
            "stockQuantity": 100,
            "status": "ACTIVE",
            "description": "苹果最新旗舰手机，配备A17 Pro芯片"
        }
    },
    "timestamp": "2025-09-29T17:33:08.335519"
}
```

**验证点**：
- `switchMethod` 显示使用了 `DataSourceContextHolder.executeRead()`
- 数据源类型为 `READ`

#### 3.2 编程式写操作

```bash
curl -X POST http://localhost:8000/readwrite/programmatic/write/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "编程式创建产品",
    "description": "使用 DataSourceContextHolder.executeWrite() 创建",
    "price": 2999.00,
    "category": "编程式测试",
    "stockQuantity": 30,
    "status": "ACTIVE"
  }'
```

**验证点**：
- 响应中 `switchMethod` 为 `DataSourceContextHolder.executeWrite()`
- 数据源类型为 `WRITE`

### 4. 事务内读写分离测试

#### 4.1 事务内数据源选择策略

```bash
curl -X POST http://localhost:8000/readwrite/transaction/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "事务测试产品",
    "description": "测试事务内数据源选择策略",
    "price": 3999.00,
    "category": "事务测试",
    "stockQuantity": 20,
    "status": "ACTIVE"
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "事务内读写操作成功",
  "data": {
    "operation": "TRANSACTION_READ_WRITE",
    "timestamp": "2025-09-29T15:40:00",
    "createdProductId": 69,
    "queriedProduct": {...},
    "forceReadProduct": {...},
    "notes": "事务内默认使用写数据源，但可以通过 force=true 强制使用读数据源"
  },
  "success": true
}
```

**验证点**：
- 观察控制台日志中事务内不同操作的数据源选择
- `@ReadDataSource(force=false)` 在事务中使用写数据源
- `@ReadDataSource(force=true)` 在事务中强制使用读数据源

## 控制台日志分析

### 1. 数据源切换日志

启动应用后，观察控制台日志中的数据源切换信息：

```log
DEBUG i.n.d.p.readwrite.DataSourceContextHolder - Setting data source type to: WRITE for thread: http-nio-8000-exec-1
DEBUG i.n.d.p.readwrite.DynamicDataSource - Determining data source lookup key: default.write
DEBUG i.n.d.p.readwrite.DynamicDataSource - Selected WRITE data source for cluster: default
DEBUG i.n.d.p.readwrite.ReadWriteDataSourceAspect - Switched to WRITE data source for method: ReadWriteController.writeProduct
```

### 2. 负载均衡日志

观察读数据源的负载均衡：

```log
DEBUG i.n.d.p.readwrite.ReadWriteDataSourceManager - Using read data source: slave1 for cluster: default
DEBUG i.n.d.p.readwrite.ReadWriteDataSourceManager - Using read data source: slave2 for cluster: default
DEBUG i.n.d.p.readwrite.ReadWriteDataSourceManager - Using read data source: slave1 for cluster: default
```

### 3. 事务内数据源选择日志

```log
DEBUG i.n.d.p.readwrite.ReadWriteDataSourceAspect - In transaction, skipping read data source switch (force=false)
WARN  i.n.d.p.readwrite.ReadWriteDataSourceAspect - Forcing read data source in transaction, this may cause data consistency issues
```

## 功能验证清单

###  注解驱动读写分离
- [x] `@WriteDataSource` 注解强制使用写数据源
- [x] `@ReadDataSource` 注解强制使用读数据源  
- [x] 方法级别注解优先于类级别注解
- [x] 注解支持集群配置和优先级设置

###  编程式数据源控制
- [x] `DataSourceContextHolder.executeRead()` 手动切换到读数据源
- [x] `DataSourceContextHolder.executeWrite()` 手动切换到写数据源
- [x] 嵌套调用时正确恢复之前的数据源类型
- [x] 线程隔离，不同线程的数据源选择互不影响

###  事务内数据源策略
- [x] 事务内默认使用写数据源保证一致性
- [x] `@ReadDataSource(force=true)` 可强制在事务中使用读数据源
- [x] 事务提交/回滚后正确恢复数据源状态

###  负载均衡机制
- [x] 多个读数据源间的轮询负载均衡
- [x] 负载均衡状态在多次请求间正确维护
- [x] 数据源不可用时的故障转移

###  AOP 切面功能
- [x] 切面正确拦截带注解的方法
- [x] 切面执行顺序在事务切面之前
- [x] 异常情况下正确恢复数据源上下文

## 性能测试

### 1. 读写分离性能对比

```bash
# 测试读操作性能（使用读数据源）
time for i in {1..100}; do
  curl -s "http://localhost:8000/readwrite/read/products/1" > /dev/null
done

# 测试写操作性能（使用写数据源）
time for i in {1..100}; do
  curl -s -X POST http://localhost:8000/readwrite/write/products \
    -H "Content-Type: application/json" \
    -d '{
      "name": "性能测试产品'$i'",
      "price": 100.00,
      "category": "测试",
      "stockQuantity": 10,
      "status": "ACTIVE"
    }' > /dev/null
done
```

### 2. 负载均衡性能测试

```bash
# 测试负载均衡的性能影响
time curl "http://localhost:8000/readwrite/load-balance-test?rounds=100"
```

### 3. 大量并发读取测试

```bash
# 使用 Apache Bench 进行并发测试
ab -n 1000 -c 10 "http://localhost:8000/readwrite/read/products/1"
```

## 故障排查

### 1. 数据源配置问题

**症状**：应用启动失败或数据源切换不生效

**排查步骤**：
1. 检查 `application-readwrite.yml` 配置
2. 确认数据源连接信息正确
3. 验证读写分离是否启用：`nebula.data.read-write-separation.enabled=true`

**解决方案**：
```yaml
# 确保配置格式正确
nebula:
  data:
    read-write-separation:
      enabled: true
      clusters:
        default:
          master: primary
          slaves: [slave1, slave2]
```

### 2. 注解不生效问题

**症状**：使用了 `@ReadDataSource` 或 `@WriteDataSource` 但数据源没有切换

**排查步骤**：
1. 检查 AOP 是否启用
2. 确认方法是被 Spring 代理的（不能是 private 方法）
3. 验证切面配置：`nebula.data.read-write-separation.aspect-enabled=true`

**日志验证**：
```log
DEBUG i.n.d.p.readwrite.ReadWriteDataSourceAspect - Switched to READ data source for method: ...
```

### 3. 事务内数据源选择问题

**症状**：在事务内使用 `@ReadDataSource` 但仍然使用写数据源

**原因**：默认情况下，事务内的读操作使用写数据源保证数据一致性

**解决方案**：
```java
@ReadDataSource(force = true) // 强制使用读数据源
public Product getProductInTransaction(Long id) {
    // ...
}
```

### 4. 负载均衡不工作

**症状**：多次读请求都使用同一个数据源

**排查步骤**：
1. 检查是否配置了多个从数据源
2. 验证负载均衡策略配置
3. 确认数据源连接是否正常

**配置检查**：
```yaml
clusters:
  default:
    slaves: [slave1, slave2]  # 确保配置了多个从库
    load-balance-strategy: ROUND_ROBIN
```

### 5. 数据源连接失败

**症状**：切换到某个数据源后出现连接异常

**排查步骤**：
1. 检查数据源连接配置
2. 验证数据库服务状态
3. 测试网络连通性

**验证命令**：
```bash
# 测试数据库连接
mysql -h 192.168.2.130 -u root -p -e "SELECT 1"
```

## 监控和运维

### 1. 数据源状态监控

定期检查数据源状态：
```bash
curl http://localhost:8000/readwrite/status | jq '.data'
```

### 2. 性能指标监控

关注以下指标：
- 读写操作的响应时间
- 数据源切换的频率
- 负载均衡的分布情况
- 事务内数据源选择的比例

### 3. 日志监控

重要的日志关键词：
- `Switched to READ/WRITE data source`
- `In transaction, skipping read data source switch`
- `Forcing read data source in transaction`
- `Selected READ/WRITE data source for cluster`

## 开发建议

### 1. 注解使用原则

- **读操作**：查询统计报表等使用 `@ReadDataSource`
- **写操作**：增删改操作使用 `@WriteDataSource`
- **事务内读取**：考虑数据一致性要求，谨慎使用 `force=true`

### 2. 性能优化建议

- 合理配置连接池大小，读库连接池可以相对较小
- 使用连接池预热，避免首次连接延迟
- 监控数据源切换开销，避免过度切换

### 3. 数据一致性考虑

- 主从复制延迟可能导致读取到旧数据
- 关键业务操作后的立即查询应使用写数据源
- 考虑使用缓存减少对数据库的依赖

### 4. 故障处理策略

- 配置合理的连接超时和重试机制
- 实现读库故障时自动切换到写库
- 建立数据源状态监控和告警机制

---

更多详细信息，请参考 [Nebula Data Persistence 使用指南](../../nebula/infrastructure/data/nebula-data-persistence/README.md)


## 5. DTO 规范说明

### 5.1 项目 DTO 规范

本项目严格遵循 DTO 规范，每个接口都有对应的专用 DTO 文件：

**读写分离演示专用 DTO**:
- `CreateReadWriteProductDto` - 创建产品（读写分离演示）接口DTO
- `GetReadWriteProductDto` - 获取产品详情（读写分离演示）接口DTO  
- `UpdateReadWriteProductDto` - 更新产品（读写分离演示）接口DTO

### 5.2 Service 层规范

- **接口**: `ReadWriteDemoService` - 专用读写分离演示服务接口
- **实现**: `ReadWriteDemoServiceImpl` - 严格遵循 DTO 规范，使用专用 DTO
- **注解**: 在 Service 层使用 `@ReadDataSource` 和 `@WriteDataSource` 注解确保数据源路由

### 5.3 Controller 层规范

- **控制器**: `ReadWriteController` - 专用读写分离演示控制器
- **路径**: `/readwrite` - 专用演示路径，避免与基础数据访问功能混淆
- **DTO 映射**: 每个接口严格使用对应的专用 DTO，不复用其他功能的 DTO

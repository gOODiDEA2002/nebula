# Nebula 缓存功能测试指南

## 概述

这个指南详细介绍如何测试 Nebula 缓存模块的各种功能，包括基础缓存操作多级缓存Spring Cache注解性能统计等

## 启动应用

```bash
cd nebula-example
mvn spring-boot:run
```

应用启动后，访问：http://localhost:8000

## 环境准备

### 1. Redis服务器配置

确保Redis服务器正常运行：

```bash
# 检查Redis连接
redis-cli -h 192.168.111.130 -p 6379 -a lilishop ping

# 应该返回：PONG
```

### 2. 验证缓存模块启动

查看应用启动日志，确认缓存模块正常初始化：

```log
2025-01-01 12:00:00.000  INFO --- Configuring Multi-Level Cache Manager
2025-01-01 12:00:00.000  INFO --- MultiLevelCacheManager initialized with L1: LocalCache, L2: DefaultCache
```

## API 接口测试

### 1. 基础缓存操作

#### 1.1 设置缓存

```bash
curl -X POST http://localhost:8000/cache/set \
  -H "Content-Type: application/json" \
  -d '{
    "key": "user:123",
    "value": "张三",
    "ttlSeconds": 300
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "缓存设置成功",
  "data": {
    "success": true,
    "key": "user:123",
    "value": "张三",
    "ttlSeconds": 300
  }
}
```

#### 1.2 获取缓存

```bash
curl -X POST http://localhost:8000/cache/get \
  -H "Content-Type: application/json" \
  -d '{
    "key": "user:123",
    "valueType": "String"
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "缓存获取成功",
  "data": {
    "key": "user:123",
    "value": "张三",
    "exists": true,
    "remainingTtlSeconds": 280,
    "source": "L1"
  }
}
```

#### 1.3 设置复杂对象缓存

```bash
curl -X POST http://localhost:8000/cache/set \
  -H "Content-Type: application/json" \
  -d '{
    "key": "product:456",
    "value": {
      "id": 456,
      "name": "智能手机",
      "price": 3999.00,
      "category": "电子产品"
    },
    "ttlSeconds": 600
  }'
```

#### 1.4 获取对象缓存

```bash
curl -X POST http://localhost:8000/cache/get \
  -H "Content-Type: application/json" \
  -d '{
    "key": "product:456",
    "valueType": "Object"
  }'
```

### 2. 缓存统计信息

```bash
curl -X GET http://localhost:8000/cache/stats
```

响应示例：
```json
{
  "code": "SUCCESS",
  "message": "缓存统计获取成功",
  "data": {
    "cacheName": "MultiLevelCache[L1:LocalCache,L2:DefaultCache]",
    "hitCount": 15,
    "missCount": 5,
    "hitRate": 0.75,
    "size": 3,
    "evictionCount": 2,
    "available": true,
    "l1HitCount": 10,
    "l2HitCount": 5,
    "l1HitRate": 0.50,
    "l2HitRate": 0.25,
    "totalRequestCount": 20
  }
}
```

### 3. 查询缓存键

```bash
curl -X POST http://localhost:8000/cache/keys \
  -H "Content-Type: application/json" \
  -d '{
    "pattern": "user:*"
  }'
```

响应示例：
```json
{
  "code": "SUCCESS",
  "message": "缓存键查询成功",
  "data": {
    "pattern": "user:*",
    "keys": ["user:123", "user:456", "user:789"],
    "count": 3
  }
}
```

### 4. 删除缓存

```bash
curl -X DELETE http://localhost:8000/cache/delete \
  -H "Content-Type: application/json" \
  -d '{
    "keys": ["user:123", "product:456"]
  }'
```

响应示例：
```json
{
  "code": "SUCCESS",
  "message": "缓存删除成功",
  "data": {
    "deletedCount": 2,
    "requestedKeys": ["user:123", "product:456"],
    "deletedKeys": ["user:123", "product:456"]
  }
}
```

### 5. 清空所有缓存

```bash
curl -X POST http://localhost:8000/cache/clear
```

## Spring Cache 注解演示

### 1. 创建用户（@CachePut演示）

```bash
curl -X POST http://localhost:8000/cache/users/create \
  -H "Content-Type: application/json" \
  -d '{
    "username": "张三",
    "email": "zhangsan@example.com",
    "age": 25
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "用户创建成功",
  "data": {
    "userId": 1,
    "username": "张三",
    "email": "zhangsan@example.com",
    "age": 25,
    "source": "Database",
    "createTime": "2025-01-01 12:00:00",
    "updateTime": "2025-01-01 12:00:00"
  }
}
```

### 2. 获取用户（@Cacheable演示）

第一次调用（从数据库加载）：
```bash
curl -X POST http://localhost:8000/cache/users/get \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1
  }'
```

第二次调用（从缓存加载，速度更快）：
```bash
curl -X POST http://localhost:8000/cache/users/get \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1
  }'
```

### 3. 更新用户（@CachePut演示）

```bash
curl -X PUT http://localhost:8000/cache/users/update \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "username": "李四",
    "age": 30
  }'
```

### 4. 删除用户（@CacheEvict演示）

```bash
curl -X DELETE http://localhost:8000/cache/users/delete \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1
  }'
```

## 多级缓存验证

### 1. 验证缓存层级

```bash
# 1. 设置缓存
curl -X POST http://localhost:8000/cache/set \
  -H "Content-Type: application/json" \
  -d '{
    "key": "level:test",
    "value": "多级缓存测试",
    "ttlSeconds": 300
  }'

# 2. 第一次获取（应该从L1缓存命中）
curl -X POST http://localhost:8000/cache/get \
  -H "Content-Type: application/json" \
  -d '{
    "key": "level:test"
  }'

# 3. 等待L1缓存过期（120秒后）
sleep 125

# 4. 再次获取（应该从L2缓存命中并回写到L1）
curl -X POST http://localhost:8000/cache/get \
  -H "Content-Type: application/json" \
  -d '{
    "key": "level:test"
  }'
```

### 2. 性能对比测试

```bash
# 使用ab工具进行并发测试
ab -n 1000 -c 10 -p get_cache.json -T "application/json" \
  http://localhost:8000/cache/get

# get_cache.json 内容：
# {"key": "user:123", "valueType": "String"}
```

## 缓存性能监控

### 1. 实时统计监控

```bash
# 每5秒获取一次缓存统计
while true; do
  echo "=== $(date) ==="
  curl -s http://localhost:8000/cache/stats | jq '.data | {hitRate, l1HitRate, l2HitRate, size}'
  sleep 5
done
```

### 2. 监控多级缓存效果

```bash
# 创建监控脚本
cat > monitor_cache.sh << 'EOF'
#!/bin/bash
echo "时间,总命中率,L1命中率,L2命中率,缓存大小"
while true; do
  STATS=$(curl -s http://localhost:8000/cache/stats)
  TIME=$(date '+%H:%M:%S')
  HIT_RATE=$(echo $STATS | jq -r '.data.hitRate')
  L1_HIT_RATE=$(echo $STATS | jq -r '.data.l1HitRate')
  L2_HIT_RATE=$(echo $STATS | jq -r '.data.l2HitRate')
  SIZE=$(echo $STATS | jq -r '.data.size')
  echo "$TIME,$HIT_RATE,$L1_HIT_RATE,$L2_HIT_RATE,$SIZE"
  sleep 10
done
EOF

chmod +x monitor_cache.sh
./monitor_cache.sh
```

## 功能验证清单

###  基础缓存功能
- [x] 设置缓存（支持TTL）- 成功设置并返回确认
- [x] 获取缓存（多种数据类型）- 正确返回值和元信息
- [x] 删除缓存（单个和批量）- 正确删除并返回统计
- [x] 查询缓存键（模式匹配）- 支持*通配符查询
- [x] 清空所有缓存 - 完全清理缓存数据

###  Spring Cache注解
- [x] @Cacheable - 缓存查询结果，避免重复计算
- [x] @CachePut - 更新缓存，确保数据一致性
- [x] @CacheEvict - 清除缓存，及时失效过期数据
- [x] 缓存键生成 - 使用SpEL表达式生成复杂键

###  多级缓存架构
- [x] L1本地缓存 - 高速内存访问
- [x] L2远程缓存 - Redis分布式存储
- [x] 缓存回写 - L2命中时自动填充L1
- [x] 缓存同步 - 更新时同步所有层级

###  性能和监控
- [x] 缓存统计 - 命中率大小驱逐次数等
- [x] 多级统计 - L1/L2分别的性能指标
- [x] 可用性检查 - 缓存系统健康状态
- [x] 性能优化 - 智能缓存策略

###  错误处理
- [x] 连接失败处理 - Redis不可用时的降级
- [x] 序列化错误 - 复杂对象的安全处理
- [x] 内存溢出保护 - LRU驱逐策略
- [x] 并发安全 - 多线程环境下的数据一致性

## 性能基准测试

### 1. 单线程性能

```bash
# 测试本地缓存性能
time for i in {1..1000}; do
  curl -s -X POST http://localhost:8000/cache/get \
    -H "Content-Type: application/json" \
    -d '{"key": "perf:test", "valueType": "String"}' > /dev/null
done
```

### 2. 多线程并发测试

```bash
# 安装Apache Bench
sudo apt-get install apache2-utils  # Ubuntu/Debian
# 或
brew install apache2              # macOS

# 并发测试
ab -n 10000 -c 50 -p test_data.json -T "application/json" \
  http://localhost:8000/cache/get
```

## 故障排查

### 1. 缓存不生效

**问题**：设置的缓存无法获取
**排查步骤**：
```bash
# 1. 检查Redis连接
redis-cli -h 192.168.111.130 -p 6379 -a lilishop ping

# 2. 查看应用日志
tail -f logs/nebula-example.log | grep -i cache

# 3. 检查缓存统计
curl http://localhost:8000/cache/stats

# 4. 验证键是否存在
curl -X POST http://localhost:8000/cache/keys \
  -H "Content-Type: application/json" \
  -d '{"pattern": "*"}'
```

### 2. 性能问题

**问题**：缓存命中率过低
**解决方案**：
- 检查TTL设置是否过短
- 验证缓存键命名是否合理
- 分析访问模式是否适合缓存
- 考虑增加L1缓存大小

### 3. 内存使用过高

**问题**：应用内存占用过多
**解决方案**：
```yaml
# 调整本地缓存配置
nebula:
  data:
    cache:
      local:
        max-size: 5000           # 减少缓存条目数
        expire-after-write: 60s  # 缩短过期时间
```

## 最佳实践建议

### 1. 缓存键设计
- 使用层次化命名：`业务:类型:ID`
- 避免键名过长，影响内存使用
- 统一键命名规范，便于管理

### 2. TTL设置策略
- 热点数据：较长TTL（1-6小时）
- 实时数据：较短TTL（1-10分钟）
- 静态数据：更长TTL（1-7天）

### 3. 多级缓存使用
- L1缓存：存储最热点的小数据
- L2缓存：存储更大范围的数据
- 合理设置L1/L2的容量比例

### 4. 监控和告警
- 定期监控缓存命中率（建议>80%）
- 设置内存使用告警
- 监控Redis连接状态

---

更多详细信息，请参考 [Nebula Data Cache 使用指南](../../nebula/infrastructure/data/nebula-data-cache/README.md)

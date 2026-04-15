# Nebula 分库分表功能测试指南

## 概述

这个指南详细介绍如何测试 Nebula 框架数据访问层的分库分表功能通过集成 Apache ShardingSphere，实现订单表的分库分表操作，包括根据用户ID分库和根据订单ID分表的演示

## 1. 数据库准备

为了演示分库分表功能，你需要准备多个数据库实例和表此演示将创建2个数据库，每个数据库包含2个分表，形成 2x2 的分片架构

**分片规则**:
- **分库规则**: 根据 `user_id % 2` 决定路由到 `nebula_sharding_0` 或 `nebula_sharding_1`
- **分表规则**: 根据 `order_id % 2` 决定路由到 `t_order_0` 或 `t_order_1`
- **主键生成**: 使用 ShardingSphere 雪花算法生成全局唯一的 `order_id`

### 1.1 执行数据库脚本

执行 `nebula-example/sql/sharding-tables.sql` 脚本创建分片数据库和表：

```bash
cd nebula-example
mysql -u root -p < sql/sharding-tables.sql
```

**创建的数据库结构**:
```
nebula_sharding_0
 t_order_0  (user_id 偶数, order_id 偶数)
 t_order_1  (user_id 偶数, order_id 奇数)

nebula_sharding_1
 t_order_0  (user_id 奇数, order_id 偶数)
 t_order_1  (user_id 奇数, order_id 奇数)
```

## 2. 启动应用

使用 `sharding` profile 启动 `nebula-example` 应用这将加载 `application-sharding.yml` 中的分片配置

```bash
cd nebula-example
mvn spring-boot:run -Dspring-boot.run.profiles=dev,sharding
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev,sharding"
```

应用启动后，访问：http://localhost:8000/swagger-ui.html

## 3. API 接口测试

以下是 `ShardingController` 中提供的分库分表演示 API 接口示例：

### 3.1 创建订单 (分片路由)

**描述**: 创建新的订单，ShardingSphere 将根据分片规则自动路由到对应的数据库和表
**请求**: `POST /sharding/orders`
**请求体示例**:
```json
{
  "userId": 100,
  "productName": "分片测试产品A",
  "amount": 299.99,
  "status": "PENDING"
}
```
**Curl 示例**:
```bash
curl -X POST "http://localhost:8000/sharding/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 100,
    "productName": "分片测试产品A",
    "amount": 299.99,
    "status": "PENDING"
  }'
```
**预期响应**:
```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "orderId": 1851234567890123456 // ShardingSphere雪花算法生成的全局唯一ID
  },
  "success": true
}
```
**路由验证**: 
- `userId = 100` (偶数)  路由到 `nebula_sharding_0` 数据库
- `orderId` 生成后根据是否为奇偶数路由到 `t_order_0` 或 `t_order_1` 表

### 3.2 更多分片测试用例

#### 3.2.1 测试不同用户ID的分库路由

**用户ID为奇数** (路由到 ds1):
```bash
curl -X POST "http://localhost:8000/sharding/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 101,
    "productName": "分片测试产品B",
    "amount": 199.99,
    "status": "PAID"
  }'
```

**用户ID为偶数** (路由到 ds0):
```bash
curl -X POST "http://localhost:8000/sharding/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 102,
    "productName": "分片测试产品C",
    "amount": 399.99,
    "status": "SHIPPED"
  }'
```

### 3.3 获取订单详情 (精确路由)

**描述**: 根据订单ID和用户ID获取详细信息，ShardingSphere 会根据分片键精确路由到对应的库表
**请求**: `GET /sharding/orders/?userId={userId}&orderId={orderId}`
**Curl 示例**:
```bash
curl "http://localhost:8000/sharding/orders/?userId=100&orderId=1851234567890123456"
```
**预期响应**:
```json
{
  "code": "SUCCESS",
  "message": "获取订单详情成功",
  "data": {
    "order": {
      "orderId": 1851234567890123456,
      "userId": 100,
      "productName": "分片测试产品A",
      "amount": 299.99,
      "status": "PENDING",
      "createTime": "2024-01-15T10:30:00"
    }
  },
  "success": true
}
```

### 3.4 查询用户订单列表 (单库多表查询)

**描述**: 根据用户ID查询该用户的所有订单，查询会路由到对应的单个数据库，但会跨表查询
**请求**: `GET /sharding/orders/user/{userId}?page=1&size=10`
**Curl 示例**:
```bash
curl "http://localhost:8000/sharding/orders/user/100?page=1&size=10"
```
**预期响应**:
```json
{
  "code": "SUCCESS",
  "message": "查询用户订单列表成功",
  "data": {
    "orders": [
      {
        "orderId": 1851234567890123456,
        "userId": 100,
        "productName": "分片测试产品A",
        "amount": 299.99,
        "status": "PENDING",
        "createTime": "2024-01-15T10:30:00"
      }
      // ... 其他订单
    ],
    "total": 1,
    "page": 1,
    "size": 10
  },
  "success": true
}
```

## 4. 分片规则验证

### 4.1 数据路由验证表

创建多个订单来验证分片路由规则：

| 用户ID | 分库规则 (user_id % 2) | 目标数据库 | 订单ID示例 | 分表规则 (order_id % 2) | 目标表 |
|--------|------------------------|------------|------------|-------------------------|--------|
| 100    | 0 (偶数)               | ds0        | 偶数ID     | 0                       | t_order_0 |
| 100    | 0 (偶数)               | ds0        | 奇数ID     | 1                       | t_order_1 |
| 101    | 1 (奇数)               | ds1        | 偶数ID     | 0                       | t_order_0 |
| 101    | 1 (奇数)               | ds1        | 奇数ID     | 1                       | t_order_1 |

### 4.2 实际数据库验证

创建订单后，可以直接查询对应的数据库表来验证数据是否正确路由：

```bash
# 查询 ds0 的数据
mysql -u root -p nebula_sharding_0 -e "SELECT * FROM t_order_0 WHERE user_id = 100;"
mysql -u root -p nebula_sharding_0 -e "SELECT * FROM t_order_1 WHERE user_id = 100;"

# 查询 ds1 的数据
mysql -u root -p nebula_sharding_1 -e "SELECT * FROM t_order_0 WHERE user_id = 101;"
mysql -u root -p nebula_sharding_1 -e "SELECT * FROM t_order_1 WHERE user_id = 101;"
```

## 5. 日志分析

### 5.1 ShardingSphere 日志

启用了 `org.apache.shardingsphere: DEBUG` 日志级别，可以观察到分片路由的详细信息：

```
[DEBUG] ShardingSphere - Logic SQL: INSERT INTO t_order (user_id, product_name, amount, status) VALUES (?, ?, ?, ?)
[DEBUG] ShardingSphere - Actual SQL: ds0 ::: INSERT INTO t_order_0 (order_id, user_id, product_name, amount, status) VALUES (?, ?, ?, ?, ?)
```

### 5.2 关键日志信息

- **Logic SQL**: 应用程序执行的逻辑SQL
- **Actual SQL**: ShardingSphere 路由后的实际SQL，显示具体的数据源和表名
- **Route Result**: 路由结果，显示数据如何分布到不同的数据节点

## 6. 性能和特性验证

### 6.1 全局唯一主键验证

```bash
# 快速创建多个订单，验证主键唯一性
for i in {100..105}; do
  curl -X POST "http://localhost:8000/sharding/orders" \
    -H "Content-Type: application/json" \
    -d "{\"userId\": $i, \"productName\": \"批量测试产品\", \"amount\": 100.00, \"status\": \"PENDING\"}" \
    -s | grep orderId
done
```

### 6.2 跨分片查询性能

对于需要跨多个分片查询的场景（如统计所有用户的订单总数），ShardingSphere 会自动进行结果合并：

```bash
# 注意：这种查询会路由到所有分片，性能相对较低
curl "http://localhost:8000/sharding/orders/statistics"
```

## 7. 功能验证清单

###  分库分表
- [x] 应用能正常启动并加载分片配置
- [x] 根据 `user_id` 正确进行分库路由
- [x] 根据 `order_id` 正确进行分表路由
- [x] 雪花算法主键生成正常且全局唯一
- [x] 精确路由查询 (带分片键) 性能良好
- [x] 单库跨表查询功能正常
- [x] ShardingSphere 日志输出正常，显示路由信息
- [x] 事务在分片环境下正常工作

### ️ 注意事项
- **跨分片查询**: 避免不带分片键的全表查询，性能较差
- **事务一致性**: 跨分片的分布式事务需要特别注意
- **数据倾斜**: 确保分片键的分布相对均匀
- **扩容策略**: 分片数量确定后扩容需要数据重新分布

## 8. 故障排查

### 8.1 常见问题

1. **找不到表**: 检查分片规则配置是否正确
2. **主键冲突**: 确保使用 ShardingSphere 的主键生成策略
3. **路由失败**: 检查分片键是否在SQL中正确传入
4. **性能问题**: 避免不必要的跨分片查询

### 8.2 调试建议

```yaml
# 在 application-sharding.yml 中启用更详细的日志
logging:
  level:
    org.apache.shardingsphere.sql: DEBUG
    org.apache.shardingsphere.route: DEBUG
    org.apache.shardingsphere.rewrite: DEBUG
```

这些日志将帮助你理解 ShardingSphere 如何处理SQL的路由重写和执行过程
## 9. DTO 规范说明

### 9.1 项目 DTO 规范

本项目严格遵循 DTO 规范，每个接口都有对应的专用 DTO 文件：

**分片演示专用 DTO**:
- `CreateShardingOrderDto` - 创建订单（分片演示）接口DTO
- `GetShardingOrderDto` - 获取订单详情（分片演示）接口DTO  
- `GetShardingOrdersDto` - 获取订单列表（分片演示）接口DTO

### 9.2 DTO 结构示例

```java
/**
 * 创建订单（分片演示）接口DTO
 */
public class CreateShardingOrderDto {
    
    /**
     * 创建订单（分片演示）请求
     */
    @Data
    public static class Request {
        /** 用户ID (分库键) */
        @NotNull(message = "用户ID不能为空")
        private Long userId;
        
        /** 订单ID (可选, 不填则由ShardingSphere自动生成) */
        private Long orderId;
        
        /** 产品名称 */
        @NotBlank(message = "产品名称不能为空")
        private String productName;
        
        /** 订单金额 */
        @NotNull(message = "订单金额不能为空")
        @DecimalMin(value = "0.01", message = "订单金额必须大于0")
        private BigDecimal amount;
        
        /** 订单状态 */
        @NotBlank(message = "订单状态不能为空")
        private String status;
    }
    
    /**
     * 创建订单（分片演示）响应
     */
    @Data
    public static class Response {
        /** 创建的订单ID */
        private Long orderId;
    }
}
```

### 9.3 Service 层规范

- **接口**: `ShardingDemoService` - 专用分片演示服务接口
- **实现**: `ShardingDemoServiceImpl` - 严格遵循 DTO 规范，使用专用 DTO
- **分片逻辑**: 自动根据分片键（userIdorderId）路由到对应的数据库和表

### 9.4 Controller 层规范

- **控制器**: `ShardingController` - 专用分片演示控制器
- **路径**: `/sharding` - 专用演示路径，避免与其他功能混淆
- **DTO 映射**: 每个接口严格使用对应的专用 DTO，确保分片键传递正确

### 9.5 分片规则与 DTO 的关系

- **分库键**: `userId` 字段在 DTO 中必须正确传递，用于分库路由
- **分表键**: `orderId` 字段用于分表路由，可由 ShardingSphere 自动生成
- **精确路由**: 查询操作需要同时提供 `userId` 和 `orderId` 以实现精确路由

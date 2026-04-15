# Nebula 数据访问功能测试指南

## 概述

这个指南详细介绍如何测试 Nebula 数据访问层的各种功能，包括基础 CRUD 操作分页查询事务管理批量操作等

## 启动应用

```bash
cd nebula-example
mvn spring-boot:run
```

应用启动后，访问：http://localhost:8000

## 数据库准备

### 1. 创建数据库和表

执行 SQL 脚本创建测试数据：

```bash
# 连接到 MySQL
mysql -u root -p

# 执行建表脚本
source sql/data-demo-tables.sql
```

### 2. 验证数据

```sql
-- 查看产品总数
SELECT COUNT(*) FROM t_product WHERE deleted = 0;

-- 查看各分类统计
SELECT category, COUNT(*) as count, AVG(price) as avg_price 
FROM t_product 
WHERE deleted = 0 
GROUP BY category;
```

## API 接口测试

### 1. 创建产品

#### 1.1 基础创建

```bash
curl -X POST http://localhost:8000/data/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "新款智能手机",
    "description": "最新5G智能手机，拍照功能强大",
    "price": 3999.00,
    "category": "电子产品",
    "stockQuantity": 100,
    "status": "ACTIVE"
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "id": 65
  },
  "success": true
}
```

#### 1.2 数据验证测试

```bash
# 缺少必填字段
curl -X POST http://localhost:8000/data/products \
  -H "Content-Type: application/json" \
  -d '{
    "description": "缺少产品名称",
    "price": 100.00
  }'
```

预期响应：400 Bad Request，包含验证错误信息

#### 1.3 价格范围验证

```bash
# 价格过低
curl -X POST http://localhost:8000/data/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "免费产品",
    "price": 0.00,
    "category": "测试",
    "stockQuantity": 10,
    "status": "ACTIVE"
  }'
```

#### 1.4 状态枚举验证

```bash
# 无效状态
curl -X POST http://localhost:8000/data/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "测试产品",
    "price": 100.00,
    "category": "测试",
    "stockQuantity": 10,
    "status": "INVALID_STATUS"
  }'
```

### 2. 查询产品

#### 2.1 根据 ID 查询

```bash
curl "http://localhost:8000/data/products/?id=1"
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "获取产品详情成功",
  "data": {
    "product": {
      "id": 1,
      "name": "iPhone 15 Pro",
      "category": "电子产品",
      "price": 8999.00,
      "stockQuantity": 100,
      "status": "ACTIVE",
      "description": "苹果最新旗舰手机，配备A17 Pro芯片"
    }
  },
  "success": true
}
```

#### 2.2 查询不存在的产品

```bash
curl "http://localhost:8000/data/products/?id=99999"
```

#### 2.3 查询已删除的产品

```bash
curl "http://localhost:8000/data/products/?id=63"
```

### 3. 分页查询

#### 3.1 基础分页查询

```bash
curl -X POST http://localhost:8000/data/products/list \
  -H "Content-Type: application/json" \
  -d '{
    "page": 1,
    "size": 5
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "复杂条件查询成功",
  "data": {
    "products": {
      "records": [
        {
          "id": 1,
          "name": "iPhone 15 Pro",
          "category": "电子产品",
          "price": 8999.00,
          "stockQuantity": 100,
          "status": "ACTIVE",
          "description": "苹果最新旗舰手机，配备A17 Pro芯片"
        }
      ],
      "total": 61,
      "size": 5,
      "current": 1,
      "pages": 13
    }
  },
  "success": true
}
```

#### 3.2 按分类查询

```bash
curl -X POST http://localhost:8000/data/products/list \
  -H "Content-Type: application/json" \
  -d '{
    "page": 1,
    "size": 10,
    "category": "电子产品"
  }'
```

#### 3.3 按状态查询

```bash
curl -X POST http://localhost:8000/data/products/list \
  -H "Content-Type: application/json" \
  -d '{
    "page": 1,
    "size": 10,
    "status": "ACTIVE"
  }'
```

#### 3.4 价格范围查询

```bash
curl -X POST http://localhost:8000/data/products/list \
  -H "Content-Type: application/json" \
  -d '{
    "page": 1,
    "size": 10,
    "minPrice": 100.00,
    "maxPrice": 1000.00
  }'
```

#### 3.5 关键词搜索

```bash
curl -X POST http://localhost:8000/data/products/list \
  -H "Content-Type: application/json" \
  -d '{
    "page": 1,
    "size": 10,
    "keyword": "手机"
  }'
```

#### 3.6 复合条件查询

```bash
curl -X POST http://localhost:8000/data/products/list \
  -H "Content-Type: application/json" \
  -d '{
    "page": 1,
    "size": 10,
    "category": "电子产品",
    "status": "ACTIVE",
    "minPrice": 1000.00,
    "maxPrice": 10000.00,
    "keyword": "Pro"
  }'
```

### 4. 更新产品

#### 4.1 基础更新

```bash
curl -X PUT http://localhost:8000/data/products/ \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "name": "iPhone 15 Pro Max",
    "description": "苹果最新旗舰手机，配备A17 Pro芯片，更大屏幕",
    "price": 9999.00
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "产品更新成功",
  "data": {
    "product": {
      "id": 1,
      "name": "iPhone 15 Pro Max",
      "category": "电子产品",
      "price": 9999.00,
      "stockQuantity": 100,
      "status": "ACTIVE",
      "description": "苹果最新旗舰手机，配备A17 Pro芯片，更大屏幕"
    }
  },
  "success": true
}
```

#### 4.2 更新不存在的产品

```bash
curl -X PUT http://localhost:8000/data/products/ \
  -H "Content-Type: application/json" \
  -d '{
    "id": 99999,
    "name": "不存在的产品"
  }'
```

#### 4.3 部分字段更新

```bash
curl -X PUT http://localhost:8000/data/products/ \
  -H "Content-Type: application/json" \
  -d '{
    "id": 2,
    "price": 18999.00
  }'
```

### 5. 删除产品

#### 5.1 单个删除

```bash
curl -X DELETE http://localhost:8000/data/products/ \
  -H "Content-Type: application/json" \
  -d '{
    "ids": [63]
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "产品删除成功",
  "data": {
    "deletedCount": 1
  },
  "success": true
}
```

#### 5.2 批量删除

```bash
curl -X DELETE http://localhost:8000/data/products/ \
  -H "Content-Type: application/json" \
  -d '{
    "ids": [64, 65, 66]
  }'
```

#### 5.3 删除不存在的产品

```bash
curl -X DELETE http://localhost:8000/data/products/ \
  -H "Content-Type: application/json" \
  -d '{
    "ids": [99999]
  }'
```

#### 5.4 重复删除

```bash
# 尝试删除已经被删除的产品
curl -X DELETE http://localhost:8000/data/products/ \
  -H "Content-Type: application/json" \
  -d '{
    "ids": [63]
  }'
```

## 功能验证清单

###  基础 CRUD 操作
- [x] 创建产品 - 成功创建并返回 ID
- [x] 根据 ID 查询 - 正确返回产品信息
- [x] 更新产品 - 成功更新并返回最新数据
- [x] 删除产品 - 逻辑删除，设置 deleted = true

###  数据验证
- [x] 必填字段验证 - 缺少必填字段时返回 400 错误
- [x] 数据类型验证 - 价格库存等数值类型验证
- [x] 枚举值验证 - 状态字段只接受有效值
- [x] 长度限制验证 - 名称描述等字段长度限制

###  分页查询功能
- [x] 基础分页 - 正确返回分页信息和数据
- [x] 条件过滤 - 支持分类状态价格范围过滤
- [x] 关键词搜索 - 支持名称和描述的模糊搜索
- [x] 复合查询 - 多个条件组合查询

###  业务逻辑
- [x] 逻辑删除 - 删除操作不物理删除数据
- [x] 时间戳管理 - 自动设置创建时间和更新时间
- [x] 数据完整性 - 查询和更新操作过滤已删除数据

###  异常处理
- [x] 数据不存在 - 查询不存在的数据时正确处理
- [x] 重复操作 - 重复删除等操作的幂等性
- [x] 参数验证 - 无效参数时返回明确错误信息

## 性能测试

### 1. 批量操作性能

```bash
# 测试批量查询性能
time curl -X POST http://localhost:8000/data/products/list \
  -H "Content-Type: application/json" \
  -d '{
    "page": 1,
    "size": 100
  }'
```

### 2. 分页查询性能

```bash
# 测试大分页查询
for i in {1..10}; do
  echo "Page $i:"
  time curl -X POST http://localhost:8000/data/products/list \
    -H "Content-Type: application/json" \
    -d "{
      \"page\": $i,
      \"size\": 50
    }"
done
```

### 3. 复杂查询性能

```bash
# 测试复杂条件查询性能
time curl -X POST http://localhost:8000/data/products/list \
  -H "Content-Type: application/json" \
  -d '{
    "page": 1,
    "size": 20,
    "category": "电子产品",
    "status": "ACTIVE",
    "minPrice": 100.00,
    "maxPrice": 10000.00,
    "keyword": "手机"
  }'
```

## 数据库验证

### 1. 验证自动时间戳

```sql
-- 查看最近创建的产品的时间戳
SELECT id, name, create_time, update_time 
FROM t_product 
WHERE id = (SELECT MAX(id) FROM t_product);
```

### 2. 验证逻辑删除

```sql
-- 查看已删除的产品
SELECT id, name, deleted, update_time 
FROM t_product 
WHERE deleted = 1 
ORDER BY update_time DESC 
LIMIT 5;
```

### 3. 验证分页查询的 SQL

启用 MyBatis-Plus SQL 日志查看实际执行的 SQL：

```yaml
# application.yml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

## 故障排查

### 1. 数据库连接问题
- 检查数据库服务是否启动
- 验证连接配置信息
- 确认数据库权限设置

### 2. MyBatis-Plus 配置问题
- 检查实体类注解配置
- 验证 Mapper 扫描路径
- 确认分页插件配置

### 3. 数据验证失败
- 检查 DTO 类的验证注解
- 确认 Controller 使用了 @Valid 注解
- 验证全局异常处理配置

### 4. 逻辑删除不生效
- 确认实体类的 @TableLogic 注解
- 检查全局逻辑删除配置
- 验证查询条件是否包含逻辑删除字段

## 开发建议

1. **实体设计**
   - 统一使用 LocalDateTime 作为时间类型
   - 合理使用 @TableField 注解控制字段行为
   - 为重要字段添加索引注解说明

2. **查询优化**
   - 避免使用 SELECT *，明确指定需要的字段
   - 合理使用分页查询，避免一次性查询大量数据
   - 为常用查询条件创建数据库索引

3. **事务管理**
   - 在 Service 层使用 @Transactional 注解
   - 合理设置事务的传播行为和隔离级别
   - 避免在事务中执行耗时操作

4. **异常处理**
   - 使用统一的异常处理机制
   - 为业务异常定义明确的错误码
   - 记录详细的错误日志便于排查

---

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


更多详细信息，请参考 [Nebula Data Persistence 使用指南](../../nebula/infrastructure/data/nebula-data-persistence/README.md)

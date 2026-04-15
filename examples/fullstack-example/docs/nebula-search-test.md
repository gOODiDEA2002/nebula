# Nebula 搜索功能测试指南

## 概述

这个指南详细介绍如何测试 Nebula 框架的 Elasticsearch 搜索功能，包括索引管理文档操作全文搜索等功能

## 前置条件

### 1. 启动 Elasticsearch

确保 Elasticsearch 服务已启动并运行在 `localhost:9200`：

```bash
# 检查 Elasticsearch 状态
curl http://localhost:9200

# 预期响应包含版本信息
```

### 2. 启动应用

```bash
cd nebula-example
mvn clean install
mvn spring-boot:run
```

应用启动后，访问：http://localhost:8000

## 测试流程

### 一索引管理

#### 1.1 创建产品索引

首次使用前需要创建索引

**方式一:使用默认配置(不传参数)**

```bash
curl -X POST http://localhost:8000/search/index/create \
  -H "Content-Type: application/json"
```

**方式二:自定义索引配置(传入参数)**

```bash
curl -X POST http://localhost:8000/search/index/create \
  -H "Content-Type: application/json" \
  -d '{
    "indexName": "products",
    "shards": 1,
    "replicas": 0
  }'
```

**请求参数说明(都是可选的):**
- `indexName`: 索引名称(默认值:"products")
- `shards`: 分片数(默认值:1)
- `replicas`: 副本数(默认值:0,单节点环境建议为0)

成功响应：
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "索引创建成功",
  "data": {
    "success": true,
    "indexName": "nebula_example_products",
    "errorMessage": null
  },
  "timestamp": "2025-10-08T12:48:58.3377"
}
```

#### 1.2 检查索引是否存在

检查产品索引是否已创建

**方式一:检查默认索引(不传参数)**

```bash
curl -X POST http://localhost:8000/search/index/exists \
  -H "Content-Type: application/json"
```

**方式二:检查指定索引(传入参数)**

```bash
curl -X POST http://localhost:8000/search/index/exists \
  -H "Content-Type: application/json" \
  -d '{
    "indexName": "products"
  }'
```

**请求参数说明(可选):**
- `indexName`: 索引名称(默认值:"products")

成功响应：
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "exists": true,
    "indexName": "products",
    "errorMessage": null
  },
  "timestamp": "2025-10-08T12:49:00.123"
}
```

#### 1.3 删除索引(谨慎使用)

删除整个产品索引及所有文档

**方式一:删除默认索引(不传参数)**

```bash
curl -X DELETE http://localhost:8000/search/index/delete \
  -H "Content-Type: application/json"
```

**方式二:删除指定索引(传入参数)**

```bash
curl -X DELETE http://localhost:8000/search/index/delete \
  -H "Content-Type: application/json" \
  -d '{
    "indexName": "products"
  }'
```

**请求参数说明(可选):**
- `indexName`: 索引名称(默认值:"products")

成功响应：
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "索引删除成功",
  "data": {
    "success": true,
    "indexName": "products",
    "errorMessage": null
  },
  "timestamp": "2025-10-08T12:50:00.456"
}
```

### 二文档索引操作

#### 2.1 索引单个产品

将数据库中的产品数据索引到 Elasticsearch：

```bash
curl -X POST http://localhost:8000/search/products/index \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "索引成功",
  "data": {
    "success": true,
    "indexName": "nebula_example_products",
    "documentId": "1",
    "errorMessage": null
  },
  "success": true
}
```

#### 2.2 批量索引产品

批量索引多个产品以提高效率：

```bash
curl -X POST http://localhost:8000/search/products/bulk-index \
  -H "Content-Type: application/json" \
  -d '{
    "productIds": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
  }'
```

成功响应：
```json
{
  "code": "SUCCESS",
  "message": "批量索引完成: 成功10, 失败0",
  "data": {
    "totalCount": 10,
    "successCount": 10,
    "failureCount": 0,
    "errors": []
  },
  "success": true
}
```

#### 2.3 索引所有产品

建议一次性索引数据库中的所有产品：

```bash
# 假设有 60 个产品，分批索引
for i in {1..60}; do
  curl -X POST http://localhost:8000/search/products/index \
    -H "Content-Type: application/json" \
    -d "{\"productId\": $i}"
  sleep 0.1
done
```

或者使用批量接口：

```bash
curl -X POST http://localhost:8000/search/products/bulk-index \
  -H "Content-Type: application/json" \
  -d '{
    "productIds": [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60]
  }'
```

### 三搜索功能测试

#### 3.1 基础关键词搜索

搜索包含"手机"的产品：

```bash
curl -X POST http://localhost:8000/search/products/search \
  -H "Content-Type: application/json" \
  -d '{
    "keyword": "手机",
    "page": 1,
    "size": 10,
    "highlight": true
  }'
```

**请求参数说明:**
- `keyword`: 搜索关键词,将在产品名称和描述中进行全文搜索
- `page`: 页码,从1开始(默认值:1)
- `size`: 每页大小(默认值:10)
- `highlight`: 是否启用高亮显示(默认值:false)

成功响应：
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "搜索完成: 找到5条结果",
  "data": {
    "success": true,
    "totalHits": 5,
    "maxScore": 1.5,
    "took": 15,
    "timedOut": false,
    "products": [
      {
        "id": "1",
        "name": "iPhone 15 Pro",
        "description": "苹果最新旗舰手机，配备A17 Pro芯片",
        "category": "电子产品",
        "price": 8999.00,
        "stockQuantity": 100,
        "status": "ACTIVE",
        "brand": "Apple",
        "rating": 4.8,
        "salesCount": 1500,
        "score": 1.5,
        "highlight": {
          "name": ["iPhone 15 Pro <em>手机</em>"],
          "description": ["苹果最新旗舰<em>手机</em>"]
        }
      }
    ]
  },
  "timestamp": "2025-10-08T13:00:00.123"
}
```

#### 3.2 分类过滤搜索

搜索特定分类的产品：

```bash
curl -X POST http://localhost:8000/search/products/search \
  -H "Content-Type: application/json" \
  -d '{
    "category": "电子产品",
    "page": 1,
    "size": 20
  }'
```

#### 3.3 价格范围搜索

搜索指定价格区间的产品：

```bash
curl -X POST http://localhost:8000/search/products/search \
  -H "Content-Type: application/json" \
  -d '{
    "minPrice": 1000.00,
    "maxPrice": 5000.00,
    "page": 1,
    "size": 20
  }'
```

#### 3.4 组合条件搜索

结合多个条件进行搜索：

```bash
curl -X POST http://localhost:8000/search/products/search \
  -H "Content-Type: application/json" \
  -d '{
    "keyword": "Pro",
    "category": "电子产品",
    "minPrice": 5000.00,
    "maxPrice": 20000.00,
    "status": "ACTIVE",
    "brand": "Apple",
    "minRating": 4.0,
    "page": 1,
    "size": 20,
    "sortFields": ["price"],
    "sortOrder": "asc"
  }'
```

**可用的搜索条件:**
- `keyword`: 关键词(搜索名称和描述)
- `category`: 产品分类
- `status`: 产品状态
- `brand`: 品牌名称
- `tags`: 标签列表
- `minPrice` / `maxPrice`: 价格范围
- `minRating` / `maxRating`: 评分范围
- `minSalesCount` / `maxSalesCount`: 销量范围
- `sortFields`: 排序字段列表(如["price", "salesCount"])
- `sortOrder`: 排序方向(asc/desc)

#### 3.5 分页搜索

测试分页功能(page从1开始)：

```bash
# 第一页
curl -X POST http://localhost:8000/search/products/search \
  -H "Content-Type: application/json" \
  -d '{
    "page": 1,
    "size": 10
  }'

# 第二页
curl -X POST http://localhost:8000/search/products/search \
  -H "Content-Type: application/json" \
  -d '{
    "page": 2,
    "size": 10
  }'
```

#### 3.6 排序测试

```bash
# 按价格升序
curl -X POST http://localhost:8000/search/products/search \
  -H "Content-Type: application/json" \
  -d '{
    "page": 1,
    "size": 20,
    "sortFields": ["price"],
    "sortOrder": "asc"
  }'

# 按销量降序
curl -X POST http://localhost:8000/search/products/search \
  -H "Content-Type: application/json" \
  -d '{
    "page": 1,
    "size": 20,
    "sortFields": ["salesCount"],
    "sortOrder": "desc"
  }'

# 多字段排序:先按评分降序,再按价格升序
curl -X POST http://localhost:8000/search/products/search \
  -H "Content-Type: application/json" \
  -d '{
    "page": 1,
    "size": 20,
    "sortFields": ["rating", "price"],
    "sortOrder": "desc"
  }'
```

**可用的排序字段:**
- `_score`: 相关性得分(默认)
- `price`: 价格
- `salesCount`: 销量
- `rating`: 评分
- `createTime`: 创建时间
- `updateTime`: 更新时间

### 四搜索建议功能

#### 4.1 获取搜索建议

输入部分关键词,获取自动补全建议：

```bash
curl -X POST http://localhost:8000/search/products/suggest \
  -H "Content-Type: application/json" \
  -d '{
    "text": "iPh",
    "size": 5
  }'
```

**请求参数说明:**
- `text`: 要补全的文本(必填)
- `size`: 返回建议的最大数量(默认值:5)

成功响应：
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "找到3条建议",
  "data": {
    "success": true,
    "suggestions": [
      {
        "text": "iPhone 15 Pro",
        "score": 1.5
      },
      {
        "text": "iPhone 15",
        "score": 1.3
      },
      {
        "text": "iPhone 14 Pro Max",
        "score": 1.2
      }
    ]
  },
  "timestamp": "2025-10-08T13:05:00.789"
}
```

### 五索引维护

#### 5.1 删除单个产品索引

从Elasticsearch中删除指定产品的文档:

```bash
curl -X DELETE http://localhost:8000/search/products/index \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1
  }'
```

**请求参数:**
- `productId`: 要删除的产品ID(必填)

成功响应：
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "删除成功",
  "data": {
    "success": true,
    "documentId": "1"
  },
  "timestamp": "2025-10-08T13:10:00.456"
}
```

#### 5.2 重新索引产品

删除后可以重新索引:

```bash
curl -X POST http://localhost:8000/search/products/index \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1
  }'
```

成功响应:
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "索引成功",
  "data": {
    "success": true,
    "indexName": "nebula_example_products",
    "documentId": "1"
  },
  "timestamp": "2025-10-08T13:11:00.789"
}
```

## 性能测试

### 1. 批量索引性能

测试批量索引 60 个产品的耗时：

```bash
time curl -X POST http://localhost:8000/search/products/bulk-index \
  -H "Content-Type: application/json" \
  -d '{
    "productIds": [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60]
  }'
```

### 2. 搜索响应时间

测试搜索的响应时间(关注 `took` 字段，单位为毫秒)：

```bash
curl -X POST http://localhost:8000/search/products/search \
  -H "Content-Type: application/json" \
  -d '{
    "keyword": "Pro",
    "page": 1,
    "size": 20
  }' | jq '.data.took'
```

预期输出: `15`(表示15毫秒)

### 3. 并发搜索测试

使用 Apache Bench 进行并发测试：

```bash
# 安装 ab 工具
# macOS: brew install httpd
# Ubuntu: sudo apt-get install apache2-utils

# 准备请求数据
echo '{
  "keyword": "手机",
  "page": 1,
  "size": 10
}' > search_request.json

# 100 个并发请求
ab -n 100 -c 10 -p search_request.json -T application/json \
  http://localhost:8000/search/products/search
```

## 直接访问 Elasticsearch

### 查看索引信息

```bash
# 查看所有索引
curl http://localhost:9200/_cat/indices?v

# 查看产品索引详情
curl http://localhost:9200/nebula_example_products

# 查看索引映射
curl http://localhost:9200/nebula_example_products/_mapping
```

### 查看文档

```bash
# 查看指定文档
curl http://localhost:9200/nebula_example_products/_doc/1

# 搜索所有文档
curl -X POST http://localhost:9200/nebula_example_products/_search \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "match_all": {}
    }
  }'
```

### 查看索引统计

```bash
# 查看文档总数
curl http://localhost:9200/nebula_example_products/_count

# 查看索引统计信息
curl http://localhost:9200/nebula_example_products/_stats
```

## 功能验证清单

###  索引管理
- [x] 创建索引 - 成功创建并设置正确的映射
- [x] 检查索引存在 - 正确返回索引状态
- [x] 删除索引 - 成功删除索引

###  文档操作
- [x] 单个索引 - 成功索引单个产品
- [x] 批量索引 - 批量索引多个产品
- [x] 删除文档 - 成功删除产品索引

###  搜索功能
- [x] 关键词搜索 - 支持全文搜索
- [x] 分类过滤 - 按分类筛选结果
- [x] 价格范围 - 按价格区间筛选
- [x] 组合条件 - 多条件组合搜索
- [x] 分页查询 - 正确返回分页结果
- [x] 排序功能 - 支持多字段排序
- [x] 高亮显示 - 搜索结果高亮显示

###  搜索建议
- [x] 自动补全 - 根据输入提供建议

###  性能指标
- [x] 索引速度 - 批量索引性能良好
- [x] 搜索速度 - 搜索响应时间合理（< 100ms）
- [x] 并发能力 - 支持并发搜索请求

## 故障排查

### 1. 连接 Elasticsearch 失败

**症状**: 应用启动时报错 "Connection refused"

**解决方案**:
- 检查 Elasticsearch 是否启动: `curl http://localhost:9200`
- 检查配置文件中的 `uris` 设置
- 确认防火墙规则

### 2. 索引创建失败

**症状**: 创建索引时返回错误

**解决方案**:
- 检查 Elasticsearch 磁盘空间
- 确认索引名称符合规范（小写无特殊字符）
- 查看 Elasticsearch 日志

### 3. 搜索结果为空

**症状**: 搜索返回 0 条结果

**解决方案**:
- 确认数据已正确索引: `curl http://localhost:9200/nebula_example_products/_count`
- 检查搜索关键词是否正确
- 尝试使用 `match_all` 查询验证

### 4. 高亮不生效

**症状**: 搜索结果中没有高亮内容

**解决方案**:
- 确认 `highlight` 参数设置为 `true`
- 检查关键词是否匹配
- 验证字段类型是否为 `text`

## 开发建议

### 1. 索引设计

- **字段类型选择**:
  - 全文搜索字段使用 `text` 类型
  - 精确匹配字段使用 `keyword` 类型
  - 数值字段使用合适的数值类型

- **分词器配置**:
  - 中文内容使用 IK 分词器
  - 英文内容使用 Standard 分词器

### 2. 搜索优化

- 使用 `from + size` 进行浅分页
- 大量数据使用 `scroll` API
- 合理设置 `timeout` 避免长时间查询

### 3. 性能优化

- 批量操作优于单个操作
- 合理设置 `bulk_size`
- 使用异步索引提高性能
- 定期优化索引（force_merge）

### 4. 数据同步

- 实时同步: 数据库变更后立即索引
- 定时同步: 定期全量或增量同步
- 使用消息队列解耦索引操作

## 参考资料

- [Elasticsearch 官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Nebula Search 模块文档](../../nebula/infrastructure/search/nebula-search-elasticsearch/README.md)
- [Spring Data Elasticsearch](https://docs.spring.io/spring-data/elasticsearch/docs/current/reference/html/)

---

**提示**: 建议在开发和测试环境中充分测试搜索功能后再部署到生产环境


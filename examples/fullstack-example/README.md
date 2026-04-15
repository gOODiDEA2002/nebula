# Nebula Fullstack Example

> Nebula 框架全功能综合示例，展示所有模块的综合用法与最佳实践

## 项目概述

`fullstack-example` 是 Nebula 框架的全功能综合示例项目，提供了框架核心功能的完整演示，包括：

- 数据访问（基础 CRUD读写分离分库分表）
- 缓存管理（本地缓存Redis缓存多级缓存）
- 消息队列（RabbitMQ 生产消费）
- RPC 通信（HTTP RPCgRPC）
- 服务发现（Nacos 注册与发现）
- 对象存储（MinIO 文件管理）
- 全文搜索（Elasticsearch 集成）
- AI 能力（Spring AIRAG）
- Web 开发（健康检查性能监控限流数据脱敏）
- 任务调度（XXL-Job 定时任务）
- 支付集成（Mock支付宝微信支付）

## 项目结构

```
nebula-example/
 docs/                          # 功能测试文档
    nebula-ai-test.md          # AI 功能测试
    nebula-cache-test.md       # 缓存功能测试
    nebula-data-access-test.md # 数据访问测试
    nebula-discovery-test.md   # 服务发现测试
    nebula-grpc-test.md        # gRPC 测试
    nebula-messaging-rabbitmq-test.md  # 消息队列测试
    nebula-readwrite-splitting-test.md # 读写分离测试
    nebula-rpc-test.md         # RPC 测试
    nebula-search-test.md      # 搜索功能测试
    nebula-sharding-test.md    # 分库分表测试
    nebula-storage-test.md     # 对象存储测试
    nebula-web-api-test.md     # Web API 测试
 sql/                            # 数据库脚本
    data-demo-tables.sql       # 演示数据表
    sharding-tables.sql        # 分片表结构
 src/
    main/
        java/
           io/nebula/example/
               NebulaExampleApplication.java  # 启动类
               config/         # 配置类
               controller/     # 控制器
               service/        # 服务层
               domain/         # 领域模型
               dto/            # 数据传输对象
               mapper/         # 数据访问层
               vo/             # 视图对象
        resources/
            application.yml                     # 主配置文件
            application-simple.yml             # 简化配置
            application-combined.yml           # 组合配置
            application-docker.yml             # Docker配置
 pom.xml
```

## 快速开始

### 1. 环境要求

- JDK 21+
- Maven 3.6+
- Docker (可选，用于运行依赖服务)

### 2. 启动依赖服务

使用 Docker Compose 启动所有依赖服务：

```bash
cd nebula-data
docker-compose up -d

# 查看服务状态
docker-compose ps
```

启动的服务包括：
- **MySQL** (3306) - 关系型数据库
- **Redis** (6379) - 缓存服务
- **RabbitMQ** (5672/15672) - 消息队列
- **Nacos** (8848) - 服务注册与配置中心
- **MinIO** (9000/9001) - 对象存储
- **Elasticsearch** (9200) - 搜索引擎
- **Chroma** (8000) - 向量数据库
- **XXL-Job** (8080) - 任务调度平台

### 3. 初始化数据库

```bash
# 执行数据库脚本
mysql -h localhost -u root -p < sql/data-demo-tables.sql
mysql -h localhost -u root -p < sql/sharding-tables.sql
```

### 4. 启动应用

#### 方式一：使用简化配置（推荐初学者）

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=simple
```

简化配置只需要 H2 内存数据库，无需外部依赖

#### 方式二：使用完整配置

```bash
mvn spring-boot:run
```

使用完整配置需要所有依赖服务都正常运行

### 5. 访问应用

应用启动后，访问以下地址：

```bash
# 健康检查
curl http://localhost:1000/health

# API 文档 (Swagger UI)
open http://localhost:1000/swagger-ui.html

# 性能监控
curl http://localhost:1000/performance/metrics
```

## 核心功能演示

### 1. 数据访问

#### 基础 CRUD 操作

```bash
# 创建商品
curl -X POST http://localhost:1000/data/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Nebula 手机",
    "description": "高性能智能手机",
    "price": 3999.00,
    "category": "电子产品",
    "stockQuantity": 100,
    "status": "ACTIVE"
  }'

# 查询商品
curl http://localhost:1000/data/products/1

# 更新商品
curl -X PUT http://localhost:1000/data/products/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Nebula Pro 手机",
    "price": 4999.00
  }'

# 删除商品（逻辑删除）
curl -X DELETE http://localhost:1000/data/products/1

# 分页查询
curl "http://localhost:1000/data/products?page=1&size=10&category=电子产品"
```

#### 读写分离演示

详见：[读写分离功能测试指南](docs/nebula-readwrite-splitting-test.md)

```bash
# 写操作（路由到主库）
curl -X POST http://localhost:1000/readwrite/products \
  -H "Content-Type: application/json" \
  -d '{"name":"测试商品","price":99.99}'

# 读操作（路由到从库）
curl http://localhost:1000/readwrite/products/1
```

#### 分库分表演示

详见：[分库分表功能测试指南](docs/nebula-sharding-test.md)

```bash
# 创建订单（自动分片）
curl -X POST http://localhost:1000/sharding/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
    "productName": "Nebula 手机",
    "amount": 3999.00
  }'

# 查询用户订单（单库查询）
curl "http://localhost:1000/sharding/orders?userId=1001"
```

### 2. 缓存管理

详见：[缓存功能测试指南](docs/nebula-cache-test.md)

```bash
# 测试缓存命中
curl http://localhost:1000/cache/products/1

# 查看缓存统计
curl http://localhost:1000/cache/stats

# 清除缓存
curl -X DELETE http://localhost:1000/cache/products/1
```

### 3. 消息队列

详见：[消息队列功能测试指南](docs/nebula-messaging-rabbitmq-test.md)

```bash
# 发送消息
curl -X POST http://localhost:1000/messaging/send \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "order-events",
    "message": "订单创建成功"
  }'

# 查看消息消费情况
curl http://localhost:1000/messaging/stats
```

### 4. RPC 通信

详见：[RPC 功能测试指南](docs/nebula-rpc-test.md)

```bash
# HTTP RPC 调用
curl http://localhost:1000/rpc/users/1

# 查看 RPC 统计
curl http://localhost:1000/rpc/stats
```

### 5. 服务发现

详见：[服务发现功能测试指南](docs/nebula-discovery-test.md)

```bash
# 查看注册的服务
curl http://localhost:1000/discovery/services

# 查看服务实例
curl http://localhost:1000/discovery/services/nebula-example/instances
```

### 6. 对象存储

详见：[对象存储功能测试指南](docs/nebula-storage-test.md)

```bash
# 上传文件
curl -X POST http://localhost:1000/storage/upload \
  -F "file=@/path/to/file.jpg"

# 下载文件
curl http://localhost:1000/storage/download/file-id -o downloaded.jpg

# 列出文件
curl http://localhost:1000/storage/files
```

### 7. 全文搜索

详见：[搜索功能测试指南](docs/nebula-search-test.md)

```bash
# 索引文档
curl -X POST http://localhost:1000/search/products/index \
  -H "Content-Type: application/json" \
  -d '{
    "id": "1",
    "name": "Nebula 手机",
    "description": "高性能智能手机"
  }'

# 搜索文档
curl "http://localhost:1000/search/products?q=手机"
```

### 8. AI 功能

详见：[AI 功能测试指南](docs/nebula-ai-test.md)

```bash
# 聊天对话
curl -X POST http://localhost:1000/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "介绍一下 Nebula 框架"
  }'

# 文档问答（RAG）
curl -X POST http://localhost:1000/ai/qa \
  -H "Content-Type: application/json" \
  -d '{
    "question": "如何使用 Nebula 的缓存功能?"
  }'
```

### 9. Web 功能

#### 健康检查

```bash
# 综合健康检查
curl http://localhost:1000/health

# 存活检查
curl http://localhost:1000/health/liveness

# 就绪检查
curl http://localhost:1000/health/readiness
```

#### 性能监控

```bash
# 查看性能指标
curl http://localhost:1000/performance/metrics

# 查看系统指标
curl http://localhost:1000/performance/system

# 查看监控状态
curl http://localhost:1000/performance/status
```

#### 限流功能

```bash
# 测试限流（连续请求会被限流）
for i in {1..100}; do
  curl http://localhost:1000/api/limited-endpoint
done
```

#### 数据脱敏

```bash
# 查询包含敏感信息的数据（自动脱敏）
curl http://localhost:1000/api/users/1

# 响应示例：
{
  "name": "张**",
  "phone": "138****5678",
  "email": "user***@example.com",
  "idCard": "110***********1234"
}
```

### 10. 任务调度

```bash
# 查看任务列表
curl http://localhost:1000/tasks

# 手动触发任务
curl -X POST http://localhost:1000/tasks/trigger/task-id
```

## 配置说明

### 配置文件说明

#### application.yml (主配置)

包含所有功能模块的完整配置，需要所有外部服务支持

#### application-simple.yml (简化配置)

最小化配置，适合快速开始和学习：
- 使用 H2 内存数据库
- 禁用需要外部服务的功能
- 端口：1000

#### application-combined.yml (组合配置)

展示三种数据访问方式并存：
- 普通数据访问（用户表）
- 读写分离（产品表）
- 分库分表（订单表）

#### application-docker.yml (Docker 配置)

适用于 Docker 容器化部署的配置

### 核心配置项

```yaml
# 服务器配置
server:
  port: 1000
  
# 应用配置  
spring:
  application:
    name: nebula-example

# Nebula 配置
nebula:
  # 数据访问
  data:
    persistence:
      enabled: true
    cache:
      enabled: true
      type: multi-level
  
  # 消息队列
  messaging:
    rabbitmq:
      enabled: true
  
  # RPC
  rpc:
    http:
      enabled: true
    grpc:
      enabled: true
  
  # 服务发现
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
  
  # 对象存储
  storage:
    minio:
      enabled: true
  
  # 搜索引擎
  search:
    elasticsearch:
      enabled: true
  
  # AI 服务
  ai:
    enabled: true
```

## 测试指南

每个功能模块都有详细的测试文档，位于 `docs/` 目录：

1. [数据访问功能测试](docs/nebula-data-access-test.md)
2. [缓存功能测试](docs/nebula-cache-test.md)
3. [读写分离功能测试](docs/nebula-readwrite-splitting-test.md)
4. [分库分表功能测试](docs/nebula-sharding-test.md)
5. [消息队列功能测试](docs/nebula-messaging-rabbitmq-test.md)
6. [RPC 功能测试](docs/nebula-rpc-test.md)
7. [gRPC 功能测试](docs/nebula-grpc-test.md)
8. [服务发现功能测试](docs/nebula-discovery-test.md)
9. [对象存储功能测试](docs/nebula-storage-test.md)
10. [搜索功能测试](docs/nebula-search-test.md)
11. [AI 功能测试](docs/nebula-ai-test.md)
12. [Web API 功能测试](docs/nebula-web-api-test.md)

## 常见问题

### 1. 应用启动失败

**问题**：端口被占用
```bash
# 检查端口占用
lsof -i :1000

# 修改端口
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"
```

**问题**：依赖服务未启动
```bash
# 检查 Docker 服务状态
cd nebula-data
docker-compose ps

# 重启服务
docker-compose restart
```

### 2. 数据库连接失败

```bash
# 检查 MySQL 连接
mysql -h localhost -u root -p

# 检查数据库是否存在
show databases;
```

### 3. Nacos 注册失败

```bash
# 访问 Nacos 控制台
open http://localhost:8848/nacos

# 默认账号密码：nacos/nacos

# 检查服务列表
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=nebula-example
```

### 4. Redis 连接失败

```bash
# 测试 Redis 连接
redis-cli -h localhost -p 6379 ping

# 查看 Redis 信息
redis-cli info
```

## 最佳实践示例

### 1. 统一异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.error(e.getErrorCode(), e.getMessage());
    }
}
```

### 2. 统一响应格式

```java
@RestController
public class ProductController {
    
    @GetMapping("/products/{id}")
    public Result<Product> getProduct(@PathVariable Long id) {
        Product product = productService.getById(id);
        return Result.success(product);
    }
}
```

### 3. 缓存最佳实践

```java
@Service
public class ProductService {
    
    @Cacheable(value = "products", key = "#id")
    public Product getById(Long id) {
        return productRepository.findById(id);
    }
    
    @CacheEvict(value = "products", key = "#product.id")
    public Product update(Product product) {
        return productRepository.save(product);
    }
}
```

### 4. 事务管理

```java
@Service
public class OrderService {
    
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(Order order) {
        // 创建订单
        orderRepository.save(order);
        
        // 扣减库存
        inventoryService.decreaseStock(order.getProductId(), order.getQuantity());
        
        // 发送消息
        messageProducer.send("order-created", order);
    }
}
```

## 性能优化

### 1. 缓存策略

- 热点数据使用本地缓存
- 共享数据使用 Redis 缓存
- 大对象使用多级缓存

### 2. 数据库优化

- 读写分离，读操作路由到从库
- 大表分库分表
- 合理使用索引

### 3. 连接池配置

```yaml
# HikariCP 配置
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

## 监控与运维

### 1. 应用监控

```bash
# 健康检查
curl http://localhost:1000/actuator/health

# 性能指标
curl http://localhost:1000/actuator/metrics

# JVM 信息
curl http://localhost:1000/actuator/metrics/jvm.memory.used
```

### 2. 日志管理

日志文件位置：`logs/nebula-example.log`

```bash
# 查看日志
tail -f logs/nebula-example.log

# 搜索错误日志
grep "ERROR" logs/nebula-example.log
```

## 部署指南

### Docker 部署

```bash
# 构建镜像
docker build -t nebula-example:latest .

# 运行容器
docker run -d \
  -p 1000:1000 \
  --name nebula-example \
  nebula-example:latest
```

### Kubernetes 部署

```bash
# 应用部署文件
kubectl apply -f k8s/deployment.yaml

# 查看状态
kubectl get pods
kubectl logs -f nebula-example-xxx
```

## 相关资源

- [Nebula Examples 总览](../README.md)
- [Nebula 框架文档](../../docs/INDEX.md)
- [Nebula 框架使用指南](../../docs/Nebula框架使用指南.md)
- [自动配置文档](../../autoconfigure/nebula-autoconfigure/README.md)

## 技术栈

- **核心**: Java 21, Spring Boot 3.5.8
- **数据访问**: MyBatis-Plus, ShardingSphere
- **缓存**: Caffeine, Redis
- **消息队列**: RabbitMQ
- **服务发现**: Nacos
- **对象存储**: MinIO
- **搜索引擎**: Elasticsearch
- **AI**: Spring AI, OpenAI API
- **任务调度**: XXL-Job

## 许可证

Apache License 2.0

---

**Nebula Fullstack Example** - Nebula 框架全模块综合演示

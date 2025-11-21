# Nebula 框架术语表

> 本文档解释 Nebula 框架中使用的常用术语和概念

## A

### API (Application Programming Interface)
应用程序编程接口，模块对外提供的编程接口。

### Auto-Configuration (自动配置)
Spring Boot 的特性，根据类路径中的依赖自动配置应用。Nebula 框架充分利用此特性。

## B

### Bean
Spring 容器管理的对象实例。

### Batch Processing (批处理)
一次性处理多个数据项，提升性能。例如批量插入数据库记录。

## C

### Cache (缓存)
存储经常访问的数据副本，减少数据库访问，提升性能。

**Nebula 支持**：
- L1 缓存：Caffeine 本地缓存
- L2 缓存：Redis 分布式缓存

### Cache Aside Pattern (旁路缓存模式)
先查缓存，未命中再查数据库，然后将结果写入缓存。

### Cache Penetration (缓存穿透)
查询不存在的数据，缓存和数据库都没有，导致每次都查数据库。

**解决方案**：
- 缓存空值
- 布隆过滤器

### Cache Avalanche (缓存雪崩)
大量缓存同时失效，导致请求直接打到数据库。

**解决方案**：
- 随机过期时间
- 热点数据不过期

### Circuit Breaker (熔断器)
当服务出现故障时，自动切断请求，防止故障扩散。

### CRUD
Create（创建）、Read（读取）、Update（更新）、Delete（删除）的缩写。

## D

### DAO (Data Access Object)
数据访问对象，封装数据库访问逻辑。

### Distributed Lock (分布式锁)
在分布式系统中实现互斥访问的锁机制。

**Nebula 实现**：基于 Redis 的分布式锁

**典型场景**：
- 防止超卖
- 防止重复提交
- 定时任务唯一执行

### DTO (Data Transfer Object)
数据传输对象，用于不同层之间传递数据。

## E

### Entity (实体)
与数据库表对应的 Java 类。

### Elasticsearch
分布式搜索和分析引擎，用于全文搜索。

**Nebula 支持**：nebula-search-elasticsearch

## F

### Failover (故障转移)
当主服务失败时，自动切换到备用服务。

### Fair Lock (公平锁)
按照请求顺序获取锁。

## G

### gRPC
Google 开发的高性能 RPC 框架。

**特点**：
- 基于 HTTP/2
- 使用 Protocol Buffers
- 支持多语言

**Nebula 支持**：nebula-rpc-grpc

## H

### High Availability (高可用)
系统能够持续运行的能力，通常用百分比表示（如 99.99%）。

### Hot Key (热点 Key)
被频繁访问的缓存键。

**问题**：可能导致 Redis 单点压力过大

**解决方案**：
- 本地缓存
- 多级缓存
- 热点数据分散

## I

### Idempotent (幂等)
同一个操作执行多次的结果与执行一次相同。

**应用场景**：
- 支付接口
- 订单创建
- 消息消费

## J

### JWT (JSON Web Token)
基于 JSON 的开放标准令牌。

**结构**：
- Header（头部）
- Payload（载荷）
- Signature（签名）

**Nebula 支持**：nebula-security 模块

## L

### Load Balancing (负载均衡)
将请求分发到多个服务实例，避免单点过载。

**常见策略**：
- 轮询
- 随机
- 加权
- 最少连接

### Lua Script
Redis 支持的脚本语言，用于原子性操作。

**应用**：分布式锁的实现

## M

### Mapper
MyBatis 中的接口，定义数据库操作方法。

### Message Queue (消息队列)
异步消息传递中间件。

**Nebula 支持**：
- RabbitMQ（nebula-messaging-rabbitmq）

**典型场景**：
- 异步处理
- 系统解耦
- 流量削峰

### Microservice (微服务)
将应用拆分为多个小型、独立的服务。

**特点**：
- 独立部署
- 独立扩展
- 技术栈灵活

### MongoDB
面向文档的 NoSQL 数据库。

**Nebula 应用**：
- 操作日志
- 审计日志
- 非结构化数据

### Multi-Level Cache (多级缓存)
组合使用多个缓存层，提升性能。

**Nebula 架构**：
```
L1: Caffeine (本地)
  ↓
L2: Redis (分布式)
  ↓
L3: Database (数据源)
```

### MyBatis-Plus
MyBatis 的增强工具，简化 CRUD 操作。

**Nebula 集成**：nebula-data-persistence

## N

### Nacos
阿里开源的服务发现和配置管理平台。

**功能**：
- 服务注册发现
- 配置管理
- 健康检查

**Nebula 支持**：nebula-discovery-nacos

### Non-Blocking (非阻塞)
操作不会等待完成，立即返回。

## O

### Object Storage (对象存储)
存储非结构化数据的服务。

**Nebula 支持**：
- MinIO（nebula-storage-minio）
- 阿里云 OSS（nebula-storage-aliyun-oss）

### Optimistic Lock (乐观锁)
假设不会发生冲突，只在更新时检查版本号。

**实现**：通常使用版本号字段

### Oversell (超卖)
库存被卖出的数量超过实际库存。

**解决方案**：
- 分布式锁
- 数据库行锁
- 乐观锁

## P

### Pessimistic Lock (悲观锁)
假设会发生冲突，操作前先加锁。

**实现**：数据库的 `SELECT FOR UPDATE`

### Protocol Buffers (Protobuf)
Google 的数据序列化协议，gRPC 使用。

**特点**：
- 二进制格式
- 体积小
- 速度快

## Q

### QPS (Queries Per Second)
每秒查询数，衡量系统性能的指标。

### Query Wrapper
MyBatis-Plus 的查询构造器。

```java
QueryWrapper<User> query = new QueryWrapper<>();
query.eq("username", "john");
```

## R

### RBAC (Role-Based Access Control)
基于角色的访问控制。

**模型**：
```
User → Role → Permission → Resource
```

**Nebula 支持**：nebula-security

### Redis
开源的内存数据库。

**Nebula 应用**：
- 缓存
- 分布式锁
- 会话存储

### Redisson
Redis 的 Java 客户端，提供丰富的分布式数据结构。

**Nebula 使用**：实现分布式锁

### Refresh Token (刷新令牌)
用于获取新的访问令牌，延长会话。

### Resilience (弹性)
系统应对故障的能力。

**实现方式**：
- 熔断
- 降级
- 限流
- 重试

### RPC (Remote Procedure Call)
远程过程调用，像调用本地方法一样调用远程服务。

**Nebula 支持**：
- HTTP（nebula-rpc-http）
- gRPC（nebula-rpc-grpc）

## S

### Service Discovery (服务发现)
自动发现网络中的服务实例。

**Nebula 支持**：Nacos

### Service Mesh (服务网格)
处理服务间通信的基础设施层。

**代表**：Istio

### Sharding (分片)
将数据分散到多个数据库或表。

**类型**：
- 垂直分片：按功能划分
- 水平分片：按数据划分

### Spring Boot
简化 Spring 应用开发的框架。

**特点**：
- 自动配置
- 内嵌服务器
- 开箱即用

**Nebula 基础**：基于 Spring Boot 3.x

### Starter
Spring Boot 的依赖管理方式，一次引入相关依赖。

**Nebula Starters**：
- nebula-starter-minimal
- nebula-starter-web
- nebula-starter-service
- nebula-starter-ai
- nebula-starter-all

## T

### Thread-Safe (线程安全)
多线程环境下代码能正确执行。

### Throttling (限流)
限制请求速率，防止系统过载。

**算法**：
- 令牌桶
- 漏桶
- 固定窗口
- 滑动窗口

### Timeout (超时)
操作在规定时间内未完成。

**Nebula 配置**：各模块都有超时配置

### Transaction (事务)
一组操作要么全部成功，要么全部失败。

**ACID 特性**：
- Atomicity（原子性）
- Consistency（一致性）
- Isolation（隔离性）
- Durability（持久性）

## V

### VO (Value Object)
值对象，用于返回给前端的数据。

**区别**：
- DTO：内部传输
- VO：返回前端

## W

### Watch Dog (看门狗)
自动续期机制，防止锁意外释放。

**Nebula 实现**：分布式锁自动续期

## X

### XXL-Job
分布式任务调度平台。

**Nebula 支持**：nebula-task

**功能**：
- 定时任务
- 分片任务
- 失败重试
- 任务监控

## 票务系统专用术语

### Showtime (场次)
电影的放映时间和场地信息。

### Seat Lock (座位锁定)
用户选座后，临时锁定座位，防止其他用户选择。

**实现**：分布式锁 + 超时释放

### Oversell Prevention (防超卖)
防止票卖出数量超过实际可用座位数。

**核心技术**：
- 分布式锁
- 库存检查
- 事务管理

### E-Ticket (电子票)
在线购票后生成的电子凭证。

**包含信息**：
- 二维码
- 订单号
- 场次信息
- 座位信息

### Order Timeout (订单超时)
未在规定时间内完成支付的订单。

**处理**：
- 自动取消订单
- 释放座位
- 恢复库存

## 缩写对照

| 缩写 | 全称 | 中文 |
|-----|------|------|
| API | Application Programming Interface | 应用程序编程接口 |
| CRUD | Create, Read, Update, Delete | 增删改查 |
| DAO | Data Access Object | 数据访问对象 |
| DTO | Data Transfer Object | 数据传输对象 |
| JWT | JSON Web Token | JSON Web 令牌 |
| MQ | Message Queue | 消息队列 |
| ORM | Object-Relational Mapping | 对象关系映射 |
| OSS | Object Storage Service | 对象存储服务 |
| QPS | Queries Per Second | 每秒查询数 |
| RBAC | Role-Based Access Control | 基于角色的访问控制 |
| RPC | Remote Procedure Call | 远程过程调用 |
| TPS | Transactions Per Second | 每秒事务数 |
| VO | Value Object | 值对象 |

## 相关资源

- [框架概览](framework/OVERVIEW.md) - 了解核心概念
- [架构设计](framework/ARCHITECTURE.md) - 深入理解架构
- [模块文档](INDEX.md#按模块查找) - 详细模块说明

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0

如有遗漏或错误，欢迎[提交 Issue](https://github.com/nebula/nebula/issues)。


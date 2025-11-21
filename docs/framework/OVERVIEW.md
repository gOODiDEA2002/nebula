# Nebula 框架概览

> 轻量级、高性能的企业级 Java 微服务开发框架

## 框架介绍

### 什么是 Nebula 框架

Nebula 是一个基于 Spring Boot 3.x 和 Java 21 构建的现代化微服务开发框架，专为构建高并发、分布式的企业级应用而设计。框架提供了完整的基础设施支持和开箱即用的解决方案，让开发者能够专注于业务逻辑的实现。

**核心定位**：
- **轻量级**：精简的依赖，快速启动，低资源占用
- **高性能**：优化的组件实现，支持高并发场景
- **模块化**：清晰的模块划分，按需引入
- **企业级**：生产就绪，经过实战检验

### 为什么选择 Nebula

在实际项目中（如影院票务运营平台），我们面临诸多挑战：

**业务挑战**：
- ✓ 高并发抢票场景（10000+ QPS）
- ✓ 分布式事务一致性（订单-支付-库存）
- ✓ 复杂的业务流程（选座-下单-支付-检票）
- ✓ 多系统集成（支付、影院、短信等）

**技术挑战**：
- ✓ 微服务架构复杂度
- ✓ 数据一致性保证
- ✓ 缓存策略设计
- ✓ 性能优化要求

**Nebula 的解决方案**：
- ⚡ 提供开箱即用的分布式锁解决方案
- ⚡ 内置多级缓存支持，提升性能
- ⚡ 统一的消息队列抽象，简化异步处理
- ⚡ 完善的 RPC 支持，简化服务间通信
- ⚡ 集成服务发现，支持动态扩缩容

## 核心特性

### 1. 模块化设计

框架采用分层模块化设计，可根据需求按需引入：

```
核心层 (Core)
  ├─ foundation      # 基础工具和异常处理
  └─ security        # 安全认证和权限控制
  
基础设施层 (Infrastructure)
  ├─ data           # 数据访问（MySQL、MongoDB、Redis）
  ├─ messaging      # 消息队列（RabbitMQ）
  ├─ rpc            # 远程调用（HTTP、gRPC）
  ├─ discovery      # 服务发现（Nacos）
  ├─ storage        # 对象存储（MinIO、阿里云OSS）
  ├─ search         # 全文搜索（Elasticsearch）
  ├─ ai             # AI 集成（Spring AI）
  └─ lock           # 分布式锁（Redis）
  
应用层 (Application)
  ├─ web            # Web 框架支持
  └─ task           # 任务调度（XXL-Job）
  
集成层 (Integration)
  ├─ payment        # 支付集成
  └─ notification   # 通知集成
```

### 2. 多级缓存架构

针对高并发场景（如抢票），提供三级缓存策略：

```
L1: Caffeine 本地缓存 (ms级响应)
      ↓ 未命中
L2: Redis 分布式缓存 (ms级响应)
      ↓ 未命中
L3: 数据库查询 (数据源)
```

**适用场景**：
- 热门票务信息缓存
- 用户会话信息
- 座位状态实时查询

### 3. 分布式锁支持

基于 Redis 的高性能分布式锁实现：

**特性**：
- ✓ 支持自动续期
- ✓ 防止死锁
- ✓ 注解式使用
- ✓ 公平锁/非公平锁

**票务场景应用**：
```java
@DistributedLock(key = "'ticket:' + #showtimeId")
public Order createOrder(String showtimeId, List<String> seatIds) {
    // 防止超卖的订单创建逻辑
}
```

### 4. 统一消息抽象

提供统一的消息队列抽象，支持多种消息中间件：

**特性**：
- ✓ 统一的发送/接收接口
- ✓ 消息序列化/反序列化
- ✓ 消息重试机制
- ✓ 死信队列支持

**票务场景应用**：
- 订单创建后异步通知
- 支付成功后生成电子票
- 订单超时自动取消

### 5. 灵活的 RPC 支持

支持多种 RPC 协议，适应不同场景：

| 协议 | 适用场景 | 性能 | 复杂度 |
|-----|---------|------|--------|
| HTTP | 外部接口、跨语言 | 中 | 低 |
| gRPC | 内部服务、高性能 | 高 | 中 |

**票务场景**：
- 用户服务 ↔ 订单服务：gRPC（高性能）
- 订单服务 ↔ 支付服务：HTTP（集成简单）

### 6. 服务治理

完善的微服务治理能力：

**服务发现**：
- 基于 Nacos 的服务注册发现
- 健康检查和自动摘除
- 负载均衡策略

**流量控制**：
- 限流（防止系统过载）
- 熔断（快速失败）
- 降级（保障核心功能）

### 7. 数据访问

统一的数据访问层：

**支持的数据源**：
- **MySQL**：主要业务数据（订单、用户、票务）
- **MongoDB**：日志、审计数据
- **Redis**：缓存、会话、分布式锁
- **Elasticsearch**：全文搜索（票务搜索）

**特性**：
- ✓ 读写分离
- ✓ 分库分表
- ✓ 多数据源
- ✓ 事务管理

### 8. 安全认证

企业级的安全解决方案：

**认证方式**：
- JWT Token 认证
- OAuth 2.0 第三方登录
- 多因子认证（MFA）

**权限控制**：
- RBAC 基于角色的访问控制
- 细粒度资源权限
- 数据权限过滤

**票务场景**：
- 用户登录认证
- 管理员权限管理
- 影院员工权限控制

## 设计理念

### 1. 约定优于配置

遵循 Spring Boot 的理念，提供合理的默认配置：

```yaml
# 最简配置即可启动
nebula:
  data:
    persistence:
      enabled: true
```

### 2. 开箱即用

核心功能无需额外开发：

```xml
<!-- 引入 Starter 即可使用 -->
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-service</artifactId>
</dependency>
```

### 3. 面向扩展

提供丰富的扩展点，支持自定义：

- 自定义缓存策略
- 自定义序列化方式
- 自定义异常处理
- 自定义监控指标

### 4. 生产就绪

考虑生产环境的各种需求：

- 完善的监控指标
- 详细的日志记录
- 优雅的启动/关闭
- 故障自动恢复

## 适用场景

### 票务系统（主要场景）

**系统特点**：
- 高并发抢票
- 复杂业务流程
- 分布式事务
- 多系统集成

**使用的模块**：
```
nebula-starter-service          # 微服务基础
nebula-lock-redis              # 分布式锁（防超卖）
nebula-data-persistence        # 数据持久化
nebula-data-cache              # 多级缓存
nebula-messaging-rabbitmq      # 异步消息
nebula-rpc-grpc               # 服务间通信
nebula-discovery-nacos         # 服务发现
nebula-storage-minio           # 文件存储
nebula-search-elasticsearch    # 全文搜索
nebula-integration-payment     # 支付集成
nebula-integration-notification # 通知集成
nebula-task                    # 定时任务
```

**架构示例**：
```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│用户服务  │───│订单服务  │───│支付服务  │───│通知服务  │
└─────────┘    └─────────┘    └─────────┘    └─────────┘
     │             │               │               │
     └─────────────┴───────────────┴───────────────┘
                         │
              ┌──────────┴──────────┐
              │   Nebula Framework   │
              └─────────────────────┘
```

### 电商系统

**适用场景**：
- 商品管理
- 订单处理
- 库存管理
- 支付结算

**核心模块**：
- nebula-data-* （数据访问）
- nebula-lock-redis （秒杀场景）
- nebula-messaging-* （订单消息）
- nebula-integration-payment （支付）

### 内容管理系统

**适用场景**：
- 文章发布
- 评论互动
- 全文搜索
- 内容推荐

**核心模块**：
- nebula-data-* （数据存储）
- nebula-search-elasticsearch （内容搜索）
- nebula-storage-* （图片/视频存储）
- nebula-ai-spring （内容推荐）

### 社交平台

**适用场景**：
- 用户关系
- 消息推送
- 动态流
- 实时通知

**核心模块**：
- nebula-data-mongodb （动态数据）
- nebula-data-cache （关系缓存）
- nebula-messaging-* （消息推送）
- nebula-rpc-* （服务通信）

## 技术栈

### 核心依赖

| 组件 | 版本 | 说明 |
|-----|------|------|
| Java | 21 | LTS版本 |
| Spring Boot | 3.2.x | 核心框架 |
| Spring Cloud | 2023.x | 微服务组件 |
| MyBatis-Plus | 3.5.x | ORM框架 |

### 中间件支持

| 中间件 | 版本 | 说明 |
|-------|------|------|
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 7.0+ | 缓存和分布式锁 |
| RabbitMQ | 3.12+ | 消息队列 |
| Nacos | 2.3+ | 服务发现和配置中心 |
| Elasticsearch | 8.x | 全文搜索引擎 |
| MongoDB | 6.0+ | 文档数据库 |
| MinIO | RELEASE.2024 | 对象存储 |

## 性能指标

### 基准测试

基于票务系统的实际测试数据：

| 指标 | 目标值 | 实测值 |
|-----|-------|--------|
| API 响应时间 | < 200ms | 150ms |
| 数据库查询 | < 50ms | 30ms |
| 缓存命中率 | > 90% | 95% |
| 系统 QPS | > 10000 | 12000 |
| 系统可用性 | 99.9% | 99.95% |

### 资源占用

单个微服务实例：

- **内存占用**：512MB - 1GB
- **CPU 使用**：2 核心
- **启动时间**：< 30秒
- **连接数**：支持 1000+ 并发连接

## 快速对比

### vs Spring Boot

| 特性 | Spring Boot | Nebula |
|-----|-------------|--------|
| 定位 | 通用框架 | 企业微服务框架 |
| 开箱即用 | ✓ | ✓ 增强 |
| 分布式锁 | 需自行集成 | ✓ 内置 |
| 多级缓存 | 需自行实现 | ✓ 内置 |
| RPC 支持 | 部分支持 | ✓ 完整支持 |
| 服务治理 | 需Spring Cloud | ✓ 内置 |

### vs Spring Cloud

| 特性 | Spring Cloud | Nebula |
|-----|--------------|--------|
| 学习曲线 | 陡峭 | 平缓 |
| 配置复杂度 | 高 | 低 |
| 启动速度 | 较慢 | 快 |
| 内存占用 | 较高 | 低 |
| 功能完整性 | 完整 | 精简实用 |

## 开始使用

### 快速体验

1. **创建项目**：
```bash
mvn archetype:generate \
    -DgroupId=com.example \
    -DartifactId=ticket-system \
    -DarchetypeArtifactId=nebula-archetype-service
```

2. **添加依赖**：
```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-service</artifactId>
    <version>1.0.0</version>
</dependency>
```

3. **配置文件**：
```yaml
spring:
  application:
    name: ticket-service

nebula:
  data:
    persistence:
      enabled: true
  rpc:
    grpc:
      server:
        port: 9090
```

4. **启动应用**：
```java
@SpringBootApplication
@EnableNebulaService
public class TicketServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketServiceApplication.class, args);
    }
}
```

### 进一步学习

- [快速开始](QUICK_START.md) - 详细的快速开始指南
- [架构设计](ARCHITECTURE.md) - 深入了解架构设计
- [模块指南](MODULE_GUIDE.md) - 如何选择合适的模块
- [最佳实践](BEST_PRACTICES.md) - 开发最佳实践

## 社区和支持

### 获取帮助

- **文档**：[完整文档](../INDEX.md)
- **示例**：[示例项目](../../example/)
- **问题**：[GitHub Issues](https://github.com/nebula/nebula/issues)
- **讨论**：[GitHub Discussions](https://github.com/nebula/nebula/discussions)

### 参与贡献

欢迎贡献代码和改进建议！

- [贡献指南](../../CONTRIBUTING.md)
- [开发指南](DEVELOPMENT_GUIDE.md)
- [代码规范](CODE_STYLE.md)

## 许可证

Nebula 框架采用 Apache License 2.0 许可证。

---

**Nebula 团队**  
**最后更新**: 2025-11-20  
**文档版本**: v1.0


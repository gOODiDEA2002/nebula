# Nebula 框架文档索引

> Nebula 框架完整文档导航

## 📚 快速导航

### 🚀 新手入门

| 文档 | 说明 | 适合人群 |
|------|------|---------|
| [框架概览](framework/OVERVIEW.md) | 了解 Nebula 是什么 | 所有人 |
| [快速开始](framework/QUICK_START.md) | 5个场景快速上手 | 开发者 |
| [架构设计](framework/ARCHITECTURE.md) | 深入理解架构 | 架构师 |

### 📖 按角色查找

#### 架构师 / 技术负责人
- [框架概览](framework/OVERVIEW.md) - 整体了解
- [架构设计](framework/ARCHITECTURE.md) - 架构深度
- [配置说明](Nebula框架配置说明.md) - 配置体系
- [使用指南](Nebula框架使用指南.md) - 体系化使用

#### 开发者
- [快速开始](framework/QUICK_START.md) - 快速上手
- [模块文档](#-按模块查找) - 详细用法
- [Starter选择指南](STARTER_SELECTION_GUIDE.md) - 选型建议
- [常见问题](FAQ.md) - 问题解决

#### 测试工程师
- 各模块的 TESTING.md - 测试指南
- [测试索引](testing/INDEX.md) - 测试入口

#### 运维工程师
- 以 [常见问题](FAQ.md) 与 [使用指南](Nebula框架使用指南.md) 为主
- 运维专题文档待补齐

## 🎯 按场景查找

### 票务系统（主要场景）

- 票务场景仅在模块示例与快速开始中体现，不单独维护方案文档。
- 推荐阅读：[快速开始](framework/QUICK_START.md) 的票务章节。
- 关键模块入口：
  - [分布式锁](../infrastructure/lock/nebula-lock-redis/README.md)
  - [数据持久化](../infrastructure/data/nebula-data-persistence/README.md)
  - [缓存](../infrastructure/data/nebula-data-cache/README.md)
  - [消息队列](../infrastructure/messaging/nebula-messaging-rabbitmq/README.md)
  - [支付集成](../integration/nebula-integration-payment/README.md)

## 📦 按模块查找

### 核心层 (Core)

| 模块 | 说明 | 文档 |
|------|------|------|
| nebula-foundation | 基础工具和异常处理 | [README](../core/nebula-foundation/README.md) |
| nebula-security | 安全认证和权限控制 | [README](../core/nebula-security/README.md) |

### 基础设施层 (Infrastructure)

#### 数据访问 (Data)

| 模块 | 说明 | 文档 |
|------|------|------|
| nebula-data-persistence | MySQL + MyBatis-Plus | [README](../infrastructure/data/nebula-data-persistence/README.md) |
| nebula-data-mongodb | MongoDB 支持 | [README](../infrastructure/data/nebula-data-mongodb/README.md) |
| nebula-data-cache | 多级缓存（Redis + Caffeine） | [README](../infrastructure/data/nebula-data-cache/README.md) |

#### 消息传递 (Messaging)

| 模块 | 说明 | 文档 |
|------|------|------|
| nebula-messaging-core | 消息抽象层 | [README](../infrastructure/messaging/nebula-messaging-core/README.md) |
| nebula-messaging-rabbitmq | RabbitMQ 实现 | [README](../infrastructure/messaging/nebula-messaging-rabbitmq/README.md) |

#### RPC 通信 (RPC)

| 模块 | 说明 | 文档 |
|------|------|------|
| nebula-rpc-core | RPC 抽象层 | [README](../infrastructure/rpc/nebula-rpc-core/README.md) |
| nebula-rpc-http | HTTP RPC 实现 | [README](../infrastructure/rpc/nebula-rpc-http/README.md) |
| nebula-rpc-grpc | gRPC RPC 实现 | [README](../infrastructure/rpc/nebula-rpc-grpc/README.md) |

#### API 网关 (Gateway)

| 模块 | 说明 | 文档 |
|------|------|------|
| nebula-gateway-core | Gateway 核心组件 | [README](../infrastructure/gateway/nebula-gateway-core/README.md) |
| nebula-starter-gateway | Gateway 启动器 | [README](../starter/nebula-starter-gateway/README.md) |

#### MCP Server

| 模块 | 说明 | 文档 |
|------|------|------|
| nebula-starter-mcp | MCP 启动器（基于 Spring AI） | [README](../starter/nebula-starter-mcp/README.md) |

#### 服务发现 (Discovery)

| 模块 | 说明 | 文档 |
|------|------|------|
| nebula-discovery-core | 服务发现抽象 | [README](../infrastructure/discovery/nebula-discovery-core/README.md) |
| nebula-discovery-nacos | Nacos 实现 | [README](../infrastructure/discovery/nebula-discovery-nacos/README.md) |

#### 对象存储 (Storage)

| 模块 | 说明 | 文档 |
|------|------|------|
| nebula-storage-core | 存储抽象层 | [README](../infrastructure/storage/nebula-storage-core/README.md) |
| nebula-storage-minio | MinIO 实现 | [README](../infrastructure/storage/nebula-storage-minio/README.md) |
| nebula-storage-aliyun-oss | 阿里云 OSS 实现 | [README](../infrastructure/storage/nebula-storage-aliyun-oss/README.md) |

#### 全文搜索 (Search)

| 模块 | 说明 | 文档 |
|------|------|------|
| nebula-search-core | 搜索抽象层 | [README](../infrastructure/search/nebula-search-core/README.md) |
| nebula-search-elasticsearch | Elasticsearch 实现 | [README](../infrastructure/search/nebula-search-elasticsearch/README.md) |

#### AI 集成 (AI)

| 模块 | 说明 | 文档 |
|------|------|------|
| nebula-ai-core | AI 抽象层 | [README](../infrastructure/ai/nebula-ai-core/README.md) |
| nebula-ai-spring | Spring AI 集成 | [README](../infrastructure/ai/nebula-ai-spring/README.md) |

#### 分布式锁 (Lock)

| 模块 | 说明 | 文档 |
|------|------|------|
| nebula-lock-core | 锁抽象层 | [README](../infrastructure/lock/nebula-lock-core/README.md) |
| nebula-lock-redis | Redis 分布式锁 | [README](../infrastructure/lock/nebula-lock-redis/README.md) |

### 应用层 (Application)

| 模块 | 说明 | 文档 |
|------|------|------|
| nebula-web | Web 框架支持 | [README](../application/nebula-web/README.md) |
| nebula-task | 任务调度（XXL-Job） | [README](../application/nebula-task/README.md) |

### 集成层 (Integration)

| 模块 | 说明 | 文档 |
|------|------|------|
| nebula-integration-payment | 支付集成 | [README](../integration/nebula-integration-payment/README.md) |
| nebula-integration-notification | 通知集成 | [README](../integration/nebula-integration-notification/README.md) |

### Starter 模块

| Starter | 包含模块 | 适用场景 | 文档 |
|---------|---------|---------|------|
| nebula-starter-minimal | foundation | 最小化应用 | [README](../starter/nebula-starter-minimal/README.md) |
| nebula-starter-web | foundation + security + web | Web 应用 | [README](../starter/nebula-starter-web/README.md) |
| nebula-starter-service | foundation + data + messaging + rpc + discovery | 微服务 | [README](../starter/nebula-starter-service/README.md) |
| nebula-starter-ai | foundation + ai-spring | AI 应用 | [README](../starter/nebula-starter-ai/README.md) |
| nebula-starter-all | 所有模块 | 单体应用 | [README](../starter/nebula-starter-all/README.md) |

## 🔧 按功能查找

### 数据访问
- [MySQL 持久化](../infrastructure/data/nebula-data-persistence/)
- [MongoDB 文档存储](../infrastructure/data/nebula-data-mongodb/)
- [多级缓存](../infrastructure/data/nebula-data-cache/)

### 异步处理
- [消息队列](../infrastructure/messaging/nebula-messaging-rabbitmq/)
- [定时任务](../application/nebula-task/)

### 服务间通信
- [HTTP RPC](../infrastructure/rpc/nebula-rpc-http/)
- [gRPC](../infrastructure/rpc/nebula-rpc-grpc/)
- [服务发现](../infrastructure/discovery/nebula-discovery-nacos/)

### 并发控制
- [分布式锁](../infrastructure/lock/nebula-lock-redis/)

### 文件处理
- [对象存储](../infrastructure/storage/nebula-storage-minio/)

### 搜索功能
- [全文搜索](../infrastructure/search/nebula-search-elasticsearch/)

### AI 能力
- [AI 集成](../infrastructure/ai/nebula-ai-spring/)

### 第三方集成
- [支付集成](../integration/nebula-integration-payment/)
- [通知集成](../integration/nebula-integration-notification/)

## 📝 配置文档

### 环境配置
- [开发环境配置](configs/CONFIG_DEVELOPMENT.md)
- [生产环境配置](configs/CONFIG_PRODUCTION.md)

### 场景配置
- [票务系统配置](configs/CONFIG_TICKETING.md)
- [微服务配置](configs/CONFIG_MICROSERVICE.md)

## 🔗 集成指南

- [微服务架构指南](integration/MICROSERVICE_GUIDE.md)
- [单体架构指南](integration/MONOLITH_GUIDE.md)
- [集成模式](integration/INTEGRATION_PATTERNS.md)
- [服务通信指南](integration/SERVICE_COMMUNICATION.md)

## 🛠 运维文档

- 运维专题文档待补齐，建议先阅读 [使用指南](Nebula框架使用指南.md) 与 [常见问题](FAQ.md)

## 📚 开发文档

- [贡献指南](CONTRIBUTING.md)
- [常见问题](FAQ.md)
- [使用指南](Nebula框架使用指南.md)

## 🌟 示例项目

- [示例项目总览](../example/README.md) - 所有示例项目概述
- [nebula-example](../example/nebula-example/README.md) - 主示例项目
- [nebula-example-user-service](../example/nebula-example-user-service/README.md) - 用户服务实现
- [nebula-example-order-service](../example/nebula-example-order-service/README.md) - 订单服务实现

## 📖 术语表

- [术语表](GLOSSARY.md) - 常用术语和概念

## 🔍 搜索提示

### 如果您想了解...

| 需求 | 推荐文档 |
|------|---------|
| Nebula 是什么 | [框架概览](framework/OVERVIEW.md) |
| 如何快速开始 | [快速开始](framework/QUICK_START.md) |
| 如何防止超卖 | [分布式锁](../infrastructure/lock/nebula-lock-redis/README.md) + [票务章节](framework/QUICK_START.md#场景3票务系统快速开始) |
| 如何提升性能 | [缓存](../infrastructure/data/nebula-data-cache/README.md) |
| 如何集成支付 | [支付集成](../integration/nebula-integration-payment/README.md) |
| 如何实现搜索 | [Elasticsearch](../infrastructure/search/nebula-search-elasticsearch/README.md) |
| 如何做 AI 应用 | [AI 集成](../infrastructure/ai/nebula-ai-spring/README.md) |
| 微服务怎么做 | [快速开始场景2](framework/QUICK_START.md#场景2微服务应用) |
| 如何部署 | [使用指南](Nebula框架使用指南.md) |
| 遇到问题怎么办 | [FAQ](FAQ.md) |

## 📊 文档统计

- **框架级文档**: 6+ 篇
- **模块文档**: 30+ 个模块
- **场景文档**: 以模块示例为主
- **示例项目**: 5个核心项目

## 🔄 文档更新

**最后更新**: 2026-01-15  
**版本**: 2.0.1-SNAPSHOT

## 💬 反馈

如果您发现文档问题或有改进建议：
- [提交 Issue](https://github.com/nebula/nebula/issues)
- [发起讨论](https://github.com/nebula/nebula/discussions)

---

**Nebula 开发团队**  
让微服务开发更简单！

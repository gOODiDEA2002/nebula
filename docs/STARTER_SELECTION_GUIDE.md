# Nebula Starter 选择指南

## 概述

Nebula Framework 提供了 6 种不同的 Starter，以满足不同应用场景的需求。本指南帮助您选择最适合您项目的 Starter。

## Starter 对比表

| Starter | 依赖大小 | 启动时间 | 适用场景 | 推荐指数 |
|---------|---------|----------|---------|----------|
| nebula-starter-minimal | 最小 (~5MB) | 最快 (~2s) | 工具库、共享模块 | ⭐⭐⭐⭐⭐ |
| nebula-starter-api | 极小 (~3MB) | 极快 (~1s) | API 契约模块 | ⭐⭐⭐⭐⭐ |
| nebula-starter-web | 中等 (~30MB) | 较快 (~5s) | Web 应用、API 服务 | ⭐⭐⭐⭐⭐ |
| nebula-starter-service | 较大 (~50MB) | 中等 (~8s) | 微服务应用 | ⭐⭐⭐⭐ |
| nebula-starter-ai | 较大 (~60MB) | 较慢 (~10s) | AI/ML 应用 | ⭐⭐⭐⭐ |
| nebula-starter-all | 最大 (~100MB) | 最慢 (~15s) | 单体应用、快速原型 | ⭐⭐⭐ |

## 详细说明

### 1. nebula-starter-minimal

**包含模块**:
- `nebula-foundation` - 基础工具、异常处理、Result 封装

**何时使用**:
- ✅ 创建工具类库
- ✅ 开发共享模块（非应用入口）
- ✅ 需要精细控制依赖的项目
- ✅ 纯业务逻辑模块（不涉及 Spring Boot 应用）

**何时不使用**:
- ❌ 需要 Web 功能
- ❌ 需要数据库访问
- ❌ 需要任何基础设施组件

**示例项目**:
```xml
<!-- 工具类库 -->
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-minimal</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

---

### 2. nebula-starter-api

**包含模块**:
- `nebula-rpc-core` - RPC 接口定义、注解
- `spring-web` (provided) - HTTP 注解支持
- `jakarta.validation-api` - 参数校验
- `lombok` (provided) - 减少样板代码

**何时使用**:
- ✅ 定义 API 契约（接口、DTO、常量）
- ✅ 多模块项目的共享 API 模块
- ✅ RPC 接口定义
- ✅ 服务间通信的接口定义

**何时不使用**:
- ❌ 应用入口模块
- ❌ 需要实现业务逻辑

**示例项目**:
```
my-project/
├── my-api/         ← 使用 nebula-starter-api
│   ├── UserApi.java
│   ├── OrderApi.java
│   └── dto/
└── my-service/     ← 使用 nebula-starter-service
    └── UserServiceImpl.java
```

---

### 3. nebula-starter-web

**包含模块**:
- Foundation (基础工具)
- Web (REST API、异常处理、统一响应)
- Security (JWT 认证、RBAC)
- Data Persistence (MyBatis-Plus)
- Data Cache (多级缓存)
- RPC HTTP (HTTP 客户端)

**何时使用**:
- ✅ 传统 Web 应用
- ✅ RESTful API 服务
- ✅ 管理后台系统
- ✅ 单体应用（不需要服务发现）
- ✅ 对外提供 API 的服务

**何时不使用**:
- ❌ 微服务架构（使用 `nebula-starter-service`）
- ❌ 需要 AI 功能（使用 `nebula-starter-ai`）
- ❌ 无需 Web 功能的后台任务

**示例项目**:
- 企业管理系统
- 电商后台 API
- 内容管理系统 (CMS)

---

### 4. nebula-starter-service

**包含模块**:
- nebula-starter-web 的所有模块
- Discovery (服务注册与发现 - Nacos)
- Messaging (消息队列 - RabbitMQ)
- RPC gRPC (高性能 RPC)
- Lock Redis (分布式锁)

**何时使用**:
- ✅ 微服务架构
- ✅ 分布式系统
- ✅ 云原生应用
- ✅ 需要服务发现的应用
- ✅ 需要消息队列的应用
- ✅ 需要分布式锁的应用

**何时不使用**:
- ❌ 简单的单体应用（过度设计）
- ❌ 不需要分布式能力的应用

**示例项目**:
- 用户服务 (`user-service`)
- 订单服务 (`order-service`)
- 支付服务 (`payment-service`)

**典型架构**:
```
微服务集群
├── gateway              ← 使用 nebula-starter-web
├── user-service         ← 使用 nebula-starter-service
├── order-service        ← 使用 nebula-starter-service
├── payment-service      ← 使用 nebula-starter-service
└── user-api            ← 使用 nebula-starter-api
```

---

### 5. nebula-starter-ai

**包含模块**:
- Foundation (基础工具)
- Web (REST API)
- AI Core (AI 核心抽象)
- AI Spring (Spring AI 集成 - OpenAI、Ollama)
- Data Cache (缓存支持)
- Vector Store (Chroma, Pinecone)

**何时使用**:
- ✅ AI/ML 应用
- ✅ RAG (检索增强生成) 应用
- ✅ 智能对话系统
- ✅ 文档问答系统
- ✅ 向量搜索应用
- ✅ 集成 LLM (大语言模型) 的应用

**何时不使用**:
- ❌ 不涉及 AI 功能的应用

**示例项目**:
- 智能客服系统
- 文档问答助手
- 代码补全工具
- 个性化推荐系统

**典型依赖**:
```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-ai</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>

<!-- 如需特定向量数据库，可选添加 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-chromadb</artifactId>
</dependency>
```

---

### 6. nebula-starter-all

**包含模块**:
- 几乎所有 Nebula 模块
  - Foundation, Security, Web, Task
  - Data (Persistence, MongoDB, Cache)
  - Messaging (RabbitMQ)
  - RPC (HTTP, gRPC)
  - Discovery (Nacos)
  - Storage (MinIO, Aliyun OSS)
  - Search (Elasticsearch)
  - AI (Spring AI)
  - Lock (Redis)

**何时使用**:
- ✅ 单体应用（Monolithic Application）
- ✅ 快速原型开发
- ✅ 概念验证 (POC)
- ✅ 学习和实验
- ✅ 不确定需要哪些模块时

**何时不使用**:
- ❌ 生产环境（依赖过多，启动慢）
- ❌ 对启动速度有要求的应用
- ❌ 资源受限的环境
- ❌ 微服务架构（应使用更细粒度的 Starter）

**示例项目**:
- `nebula-example` - 框架示例项目
- 快速 MVP (最小可行产品)
- 教学演示项目

**迁移建议**:
如果您的项目最初使用 `nebula-starter-all`，随着项目成熟，建议迁移到更精确的 Starter：
- Web 应用 → `nebula-starter-web`
- 微服务 → `nebula-starter-service`
- AI 应用 → `nebula-starter-ai`

---

## 决策树

```
开始
│
├─ 是否是应用入口模块？
│  ├─ 否 → 是否是 API 契约模块？
│  │  ├─ 是 → nebula-starter-api
│  │  └─ 否 → nebula-starter-minimal
│  │
│  └─ 是 → 是否需要 AI 功能？
│     ├─ 是 → nebula-starter-ai
│     │
│     └─ 否 → 是否需要服务发现/消息队列？
│        ├─ 是 → nebula-starter-service
│        │
│        └─ 否 → 是否需要 Web 功能？
│           ├─ 是 → nebula-starter-web
│           │
│           └─ 否 → 是否需要所有功能（快速原型）？
│              ├─ 是 → nebula-starter-all
│              └─ 否 → nebula-starter-minimal
```

## 按场景推荐

### 企业级微服务架构
```
nebula-starter-service (主服务)
nebula-starter-api (共享接口)
nebula-starter-web (网关服务)
```

### 传统 Web 应用
```
nebula-starter-web (主应用)
nebula-starter-api (可选，如有 API 模块)
```

### AI 驱动应用
```
nebula-starter-ai (AI 服务)
nebula-starter-web (前端 API)
```

### 快速原型/POC
```
nebula-starter-all (一次性包含所有功能)
```

### 工具类库
```
nebula-starter-minimal (最小依赖)
```

## 组合使用

Starter 可以组合使用，例如：

### AI 微服务
```xml
<!-- 同时需要 AI 和服务发现 -->
<dependencies>
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-ai</artifactId>
    </dependency>
    
    <!-- 额外添加服务发现 -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-discovery-nacos</artifactId>
    </dependency>
</dependencies>
```

### Web + 搜索
```xml
<dependencies>
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-web</artifactId>
    </dependency>
    
    <!-- 额外添加搜索 -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-search-elasticsearch</artifactId>
    </dependency>
</dependencies>
```

## 迁移指南

### 从旧版 nebula-starter 迁移

如果您之前使用的是单一的 `nebula-starter`，请按照以下步骤迁移：

1. **分析您的依赖需求**
   ```bash
   mvn dependency:tree | grep nebula
   ```

2. **选择合适的 Starter**（参考上面的决策树）

3. **更新 pom.xml**
   ```xml
   <!-- 旧版 -->
   <dependency>
       <groupId>io.nebula</groupId>
       <artifactId>nebula-starter</artifactId>
   </dependency>
   
   <!-- 新版（示例：微服务） -->
   <dependency>
       <groupId>io.nebula</groupId>
       <artifactId>nebula-starter-service</artifactId>
   </dependency>
   ```

4. **验证编译和运行**
   ```bash
   mvn clean compile
   mvn spring-boot:run
   ```

5. **调整配置**（如有必要）

### 常见迁移场景

| 原依赖 | 推荐新 Starter | 说明 |
|--------|---------------|------|
| nebula-starter (Web 应用) | nebula-starter-web | 减少不必要的依赖 |
| nebula-starter (微服务) | nebula-starter-service | 包含服务发现和消息队列 |
| nebula-starter (AI 应用) | nebula-starter-ai | 优化 AI 相关依赖 |
| nebula-starter (API 模块) | nebula-starter-api | 极小依赖 |

## 常见问题 (FAQ)

### Q: 可以在一个项目中使用多个 Starter 吗？
A: 可以，但通常不推荐。Starter 设计为互斥的，选择一个最适合的即可。如需额外功能，直接添加相应的 Nebula 模块。

### Q: 如何知道 Starter 包含了哪些依赖？
A: 查看各 Starter 的 `pom.xml`，或运行 `mvn dependency:tree`。

### Q: Starter 会自动配置所有模块吗？
A: 会的。`nebula-autoconfigure` 会根据类路径自动配置可用的模块。不需要的功能可以通过 `spring.autoconfigure.exclude` 排除。

### Q: 为什么不建议生产环境使用 nebula-starter-all？
A: 它包含了所有模块，会增加启动时间、内存占用，并可能引入不需要的依赖冲突。

### Q: 如何添加 Starter 没有包含的模块？
A: 直接在 `pom.xml` 中添加相应的 Nebula 模块依赖即可：
```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-storage-minio</artifactId>
</dependency>
```

---

**文档版本**: 2.0.0-SNAPSHOT  
**最后更新**: 2025-11-14  
**维护者**: Nebula Team


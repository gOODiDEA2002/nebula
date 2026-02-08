# Nebula Starter 选择指南

本指南帮助你选择最适合项目需求的 Nebula Starter。

## 场景选择

### 我需要开发一个简单的 Web 应用

**推荐：** `nebula-starter-web`

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-web</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

适用于：
- REST API 服务
- 管理后台
- 单体应用

包含：Web 模块、缓存、基础自动配置

---

### 我需要开发微服务

**推荐：** `nebula-starter-service`

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-service</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

适用于：
- 微服务架构
- 需要服务注册发现
- 需要服务间 RPC 通信

包含：
- 服务发现（Nacos）
- HTTP RPC
- 分布式锁（Redis）
- 消息队列支持

---

### 我需要开发 API 契约模块

**推荐：** `nebula-starter-api`

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-api</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

适用于：
- RPC 接口定义模块
- DTO 定义模块
- 服务间共享的契约

包含：RPC 核心注解、验证 API

---

### 我需要开发 API 网关

**推荐：** `nebula-starter-gateway`

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-gateway</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

适用于：
- API 网关
- 路由转发
- 统一认证
- 限流熔断

包含：
- Spring Cloud Gateway
- 服务发现（Nacos）
- Resilience4j 熔断
- Redis 限流（可选）

---

### 我需要最小化依赖

**推荐：** `nebula-starter-minimal`

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-minimal</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

适用于：
- CLI 应用
- 批处理任务
- 工具类库

包含：基础模块、自动配置

---

### 我需要 AI 功能

**推荐：** `nebula-starter-ai`

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-ai</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

适用于：
- AI 应用
- 大语言模型集成
- RAG 应用

---

## 决策树

```
你的项目是什么类型？
│
├─ 微服务
│   │
│   ├─ 业务服务 → nebula-starter-service
│   │
│   ├─ API 契约模块 → nebula-starter-api
│   │
│   └─ API 网关 → nebula-starter-gateway
│
├─ 单体应用
│   │
│   └─ Web 应用 → nebula-starter-web
│
├─ 工具/批处理
│   │
│   └─ nebula-starter-minimal
│
└─ AI 应用
    │
    └─ nebula-starter-ai
```

## Starter 对比表

| Starter | 服务发现 | RPC | 分布式锁 | 消息队列 | 网关 | AI |
|---------|---------|-----|---------|---------|------|-----|
| nebula-starter-minimal | - | - | - | - | - | - |
| nebula-starter-web | - | - | - | - | - | - |
| nebula-starter-api | - | 核心 | - | - | - | - |
| nebula-starter-service | Nacos | HTTP | Redis | 核心 | - | - |
| nebula-starter-gateway | Nacos | - | - | - | 是 | - |
| nebula-starter-ai | - | - | - | - | - | 是 |

## 继承关系

```
nebula-starter-minimal
    │
    └── nebula-starter-web
            │
            └── nebula-starter-service
```

## 常见问题

### Q: 我应该选择 nebula-starter-web 还是 nebula-starter-service？

如果你的应用需要：
- 服务注册发现 → `nebula-starter-service`
- 服务间 RPC 调用 → `nebula-starter-service`
- 只是简单的 REST API → `nebula-starter-web`

### Q: 可以在运行时禁用某些功能吗？

是的，大多数功能都可以通过配置禁用：

```yaml
nebula:
  discovery:
    nacos:
      enabled: false  # 禁用 Nacos
  rpc:
    http:
      enabled: false  # 禁用 HTTP RPC
```

### Q: 如何排除不需要的传递依赖？

使用 Maven 的 exclusion：

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-service</artifactId>
    <exclusions>
        <exclusion>
            <groupId>io.nebula</groupId>
            <artifactId>nebula-lock-redis</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

## 相关文档

- [Nebula 框架使用指南](Nebula框架使用指南.md)
- [配置说明](Nebula框架配置说明.md)
- [排查指南](TROUBLESHOOTING.md)

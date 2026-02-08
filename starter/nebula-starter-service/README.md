# Nebula Starter Service

> 微服务开发专用 Starter，包含服务发现、RPC 通信、分布式锁等核心功能

## 功能特性

- 服务发现与注册（Nacos）
- HTTP RPC 通信
- 分布式锁（Redis）
- 消息队列集成（可选）
- 任务调度（可选）

## 快速开始

### 引入依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-service</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 最小配置

```yaml
server:
  port: 8080

spring:
  application:
    name: my-service

# Nebula 使用智能默认值，连接本地 Nacos (localhost:8848)
# 如需自定义 Nacos 地址：
# nebula:
#   discovery:
#     nacos:
#       server-addr: your-nacos:8848
```

## 包含的模块

| 模块 | 描述 | 默认包含 |
|------|------|----------|
| nebula-starter-web | Web 基础功能 | 是 |
| nebula-rpc-core | RPC 核心 | 是 |
| nebula-rpc-http | HTTP RPC 实现 | 是 |
| nebula-discovery-core | 服务发现核心 | 是 |
| nebula-discovery-nacos | Nacos 服务发现 | 是 |
| nebula-lock-core | 分布式锁核心 | 是 |
| nebula-lock-redis | Redis 锁实现 | 是 |
| nebula-messaging-core | 消息核心 | 是 |
| nebula-rpc-grpc | gRPC RPC 实现 | 否(optional) |
| nebula-messaging-rabbitmq | RabbitMQ 消息 | 否(optional) |
| nebula-task | 任务调度 | 否(optional) |

## 可选依赖

### 启用 gRPC

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-rpc-grpc</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 启用 RabbitMQ 消息队列

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-messaging-rabbitmq</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 启用任务调度

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-task</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

## 配置说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| nebula.discovery.nacos.server-addr | localhost:8848 | Nacos 服务器地址 |
| nebula.discovery.nacos.namespace | "" (public) | 命名空间 |
| nebula.discovery.nacos.username | nacos | 用户名 |
| nebula.discovery.nacos.password | nacos | 密码 |
| nebula.rpc.http.server.port | 8080 | RPC 服务端口 |
| nebula.rpc.http.server.context-path | /rpc | RPC 上下文路径 |
| nebula.rpc.discovery.enabled | true | 启用 RPC 服务发现 |

## 排除不需要的依赖

如果不需要 Nacos 服务发现（例如测试环境）：

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-service</artifactId>
    <version>${nebula.version}</version>
    <exclusions>
        <exclusion>
            <groupId>io.nebula</groupId>
            <artifactId>nebula-discovery-nacos</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

## 相关文档

- [Starter 选择指南](../../docs/STARTER_SELECTION_GUIDE.md)
- [配置说明](../../docs/Nebula框架配置说明.md)
- [RPC 架构说明](../../docs/rpc/ARCHITECTURE.md)
- [排查指南](../../docs/TROUBLESHOOTING.md)

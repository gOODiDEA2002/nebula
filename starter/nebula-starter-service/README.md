# Nebula Starter Service

微服务应用专用Starter，集成RPC、服务发现、消息队列等微服务能力。

## 适用场景

- 🔧 微服务应用
- 🌐 分布式系统
- 📡 RPC服务
- 🔄 事件驱动架构
- 📦 服务网格

## 包含模块

继承`nebula-starter-web`的所有功能，额外包含:

- `nebula-rpc-core` + `nebula-rpc-http` - RPC调用
- `nebula-rpc-grpc` - gRPC支持(可选)
- `nebula-discovery-core` + `nebula-discovery-nacos` - 服务发现(可选)
- `nebula-messaging-core` + `nebula-messaging-rabbitmq` - 消息队列(可选)
- `nebula-lock-redis` - 分布式锁
- `nebula-task` - 任务调度(可选)

## 功能特性

### 继承自Web
- ✅ 所有Web功能 (REST API, JWT, 缓存等)

### 微服务能力
- ✅ HTTP RPC客户端
- ✅ gRPC服务端/客户端
- ✅ Nacos服务注册发现
- ✅ RabbitMQ消息队列
- ✅ Redis分布式锁
- ✅ XXL-JOB任务调度

## 内存占用

**~800MB** (包含所有微服务组件)

## 快速开始

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-service</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

```yaml
nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
  
  rpc:
    http:
      enabled: true
    grpc:
      enabled: true
      port: 9090
  
  messaging:
    rabbitmq:
      enabled: true
      host: localhost
      port: 5672
```

详见: 
- [RPC模块文档](../../infrastructure/rpc/nebula-rpc-core/README.md)
- [服务发现文档](../../infrastructure/discovery/nebula-discovery-core/README.md)
- [消息队列文档](../../infrastructure/messaging/nebula-messaging-core/README.md)

---

**版本**: 2.0.0-SNAPSHOT

# Nebula Starter - 配置指南

> Nebula框架一站式启动器的配置说明

## 目录

- [Starter选择](#starter选择)
- [依赖配置](#依赖配置)
- [版本管理](#版本管理)
- [特性配置](#特性配置)

---

## Starter选择

### 选择决策树

```
你的应用是什么类型？
│
├─ CLI工具/批处理          → nebula-starter-minimal
├─ Web应用/REST API        → nebula-starter-web
├─ 微服务                  → nebula-starter-service
├─ AI应用                  → nebula-starter-ai
├─ RPC契约模块             → nebula-starter-api
└─ 单体应用/原型           → nebula-starter-all
```

---

## 依赖配置

### 1. nebula-starter-minimal

**适用于**：CLI工具、批处理、工具库

```xml
<dependencies>
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-minimal</artifactId>
        <version>${nebula.version}</version>
    </dependency>
</dependencies>
```

**包含模块**：
- nebula-foundation（核心工具）
- nebula-autoconfigure（自动配置）
- spring-boot-starter（Spring Boot基础）

### 2. nebula-starter-web

**适用于**：REST API、管理后台

```xml
<dependencies>
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-web</artifactId>
        <version>${nebula.version}</version>
    </dependency>
</dependencies>
```

**包含模块**：
- nebula-starter-minimal（基础依赖）
- nebula-web（Web框架）
- nebula-security（安全模块）
- nebula-data-cache（缓存）
- nebula-data-persistence（可选，数据持久化）

### 3. nebula-starter-service

**适用于**：微服务应用

```xml
<dependencies>
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-service</artifactId>
        <version>${nebula.version}</version>
    </dependency>
</dependencies>
```

**包含模块**：
- nebula-starter-web（Web依赖）
- nebula-rpc-core（RPC核心）
- nebula-rpc-http（HTTP RPC）
- nebula-discovery-core（服务发现核心）
- nebula-discovery-nacos（可选，Nacos实现）
- nebula-messaging-core（消息核心）
- nebula-messaging-rabbitmq（可选，RabbitMQ实现）
- nebula-lock-core（锁核心）
- nebula-lock-redis（Redis锁）
- nebula-task（可选，任务调度）

### 4. nebula-starter-ai

**适用于**：AI/ML应用

```xml
<dependencies>
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-ai</artifactId>
        <version>${nebula.version}</version>
    </dependency>
</dependencies>
```

**包含模块**：
- nebula-foundation（基础功能）
- nebula-ai-core（AI核心）
- nebula-ai-spring（Spring AI集成）
- nebula-ai-langchain4j（可选，LangChain4j集成）
- nebula-data-cache（缓存）
- spring-boot-starter-web（可选）

### 5. nebula-starter-api

**适用于**：RPC契约模块

```xml
<dependencies>
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-api</artifactId>
        <version>${nebula.version}</version>
    </dependency>
</dependencies>
```

**包含模块**：
- nebula-rpc-core（RPC核心）
- spring-web（provided，Web依赖）
- jakarta.validation-api（验证API）
- lombok（provided，Lombok）

### 6. nebula-starter-all

**适用于**：单体应用、原型开发

```xml
<dependencies>
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-all</artifactId>
        <version>${nebula.version}</version>
    </dependency>
</dependencies>
```

**包含所有Nebula模块**

---

## 版本管理

### BOM依赖

使用Nebula BOM统一管理版本：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.nebula</groupId>
            <artifactId>nebula-dependencies</artifactId>
            <version>${nebula.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 版本属性

```xml
<properties>
    <nebula.version>2.0.1-SNAPSHOT</nebula.version>
    <java.version>21</java.version>
    <spring-boot.version>3.2.0</spring-boot.version>
</properties>
```

---

## 特性配置

### 按需启用功能

所有功能模块都是可选的（`<optional>true</optional>`），通过配置启用：

```yaml
nebula:
  # 数据访问
  data:
    persistence:
      enabled: true  # 启用数据持久化
    cache:
      enabled: true  # 启用缓存
    mongodb:
      enabled: false  # 禁用MongoDB
  
  # 消息传递
  messaging:
    rabbitmq:
      enabled: true  # 启用RabbitMQ
  
  # RPC通信
  rpc:
    http:
      enabled: true  # 启用HTTP RPC
    grpc:
      enabled: false  # 禁用gRPC
  
  # 服务发现
  discovery:
    nacos:
      enabled: true  # 启用Nacos
  
  # 存储服务
  storage:
    minio:
      enabled: true  # 启用MinIO
  
  # 搜索服务
  search:
    elasticsearch:
      enabled: false  # 禁用Elasticsearch
  
  # AI服务
  ai:
    spring:
      enabled: false  # 禁用AI
```

### 排除不需要的依赖

如果项目中不需要某个依赖，可以排除：

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-service</artifactId>
    <version>${nebula.version}</version>
    <exclusions>
        <exclusion>
            <groupId>io.nebula</groupId>
            <artifactId>nebula-task</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

---

## 配置示例

### 最小化配置

```yaml
spring:
  application:
    name: my-app
```

### Web应用配置

```yaml
spring:
  application:
    name: web-app
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: password

nebula:
  data:
    persistence:
      enabled: true
    cache:
      enabled: true
  
  security:
    enabled: true
    jwt:
      secret: ${JWT_SECRET}
```

### 微服务配置

```yaml
spring:
  application:
    name: order-service
  datasource:
    url: jdbc:mysql://localhost:3306/order_db
    username: root
    password: password

nebula:
  data:
    persistence:
      enabled: true
    cache:
      enabled: true
  
  messaging:
    rabbitmq:
      enabled: true
      host: localhost
      port: 5672
  
  rpc:
    http:
      enabled: true
  
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
  
  lock:
    redis:
      enabled: true
```

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [EXAMPLE.md](./EXAMPLE.md) - 使用示例
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 发展路线图

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0


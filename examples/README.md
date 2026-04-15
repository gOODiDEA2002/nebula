# Nebula Framework Examples

本目录包含 Nebula 框架的所有示例项目，分为 **快速入门** 和 **进阶示例** 两大类。

## 目录结构

```
examples/
├── Quickstart (快速入门)
│   ├── starter-web-example/         Web 应用快速入门
│   ├── starter-service-example/     微服务快速入门
│   ├── starter-ai-example/          AI 应用快速入门
│   ├── starter-minimal-example/     最小化应用（无 Web）
│   ├── starter-all-example/         全功能单体应用
│   └── starter-api-example/         API 契约定义
│
├── Advanced (进阶示例)
│   ├── gateway-example/             API 网关（HTTP 反向代理 + 限流 + 认证）
│   ├── rpc-async-example/           异步 RPC 调用（多模块）
│   ├── microservice-example/        微服务拆分（多模块：API + Service）
│   ├── fullstack-example/           全功能综合示例（数据库/缓存/RPC/消息/搜索/AI）
│   └── oauth-example/              OAuth 2.0 认证（前后端分离）
```

## 快速开始

### 前置条件

```bash
# 确保框架已安装到本地仓库
cd /path/to/nebula
mvn install -DskipTests
```

### 运行 Starter 示例

```bash
# Web 应用（端口 8080）
mvn -q -f examples/starter-web-example spring-boot:run

# 微服务应用（端口 8082）
mvn -q -f examples/starter-service-example spring-boot:run

# AI 应用（端口 8083，需配置 API Key）
mvn -q -f examples/starter-ai-example spring-boot:run

# 全功能单体（端口 8084）
mvn -q -f examples/starter-all-example spring-boot:run

# 最小化（无 Web 端点，启动后退出）
mvn -q -f examples/starter-minimal-example spring-boot:run
```

### 运行进阶示例

```bash
# API 网关（端口 8090，需 Nacos + Redis）
mvn -q -f examples/gateway-example spring-boot:run

# 异步 RPC（需 Nacos）
mvn -q -f examples/rpc-async-example/service spring-boot:run  # 先启动服务端
mvn -q -f examples/rpc-async-example/client spring-boot:run   # 再启动客户端

# 微服务（需 Nacos）
mvn -q -f examples/microservice-example/user-service spring-boot:run   # User 服务
mvn -q -f examples/microservice-example/order-service spring-boot:run  # Order 服务

# 全功能示例（需 MySQL + Redis + RabbitMQ + Nacos）
mvn -q -f examples/fullstack-example spring-boot:run

# OAuth 示例（需 MySQL）
mvn -q -f examples/oauth-example/backend spring-boot:run   # 后端
cd examples/oauth-example/frontend && npm install && npm run dev  # 前端
```

## 示例对照表

| 示例 | Starter | 外部依赖 | 说明 |
|------|---------|----------|------|
| starter-web-example | `nebula-starter-web` | 无 | 最简 Web API |
| starter-service-example | `nebula-starter-service` | Nacos(可选) | RPC + 服务发现 |
| starter-ai-example | `nebula-starter-ai` | OpenAI API | AI 聊天 + 嵌入 |
| starter-minimal-example | `nebula-starter-minimal` | 无 | 非 Web 场景 |
| starter-all-example | `nebula-starter-all` | 多个 | 全功能单体 |
| starter-api-example | `nebula-starter-api` | 无 | RPC 契约定义 |
| gateway-example | `nebula-starter-gateway` | Nacos + Redis | HTTP 反向代理 |
| rpc-async-example | `nebula-starter-service` | Nacos | 异步 RPC |
| microservice-example | `nebula-starter-service` + `nebula-starter-api` | Nacos | 服务拆分 + RPC |
| fullstack-example | `nebula-starter-all` | MySQL + Redis + RabbitMQ + Nacos | 全模块综合 |
| oauth-example | `nebula-starter-web` | MySQL | OAuth 2.0 |

## 编译

```bash
# 编译所有示例
mvn compile -f examples/pom.xml

# 编译单个示例
mvn compile -f examples/starter-web-example/pom.xml
```

## 版本管理

所有示例使用 `${revision}` 引用框架版本，与框架主版本保持同步，无需手动管理版本号。

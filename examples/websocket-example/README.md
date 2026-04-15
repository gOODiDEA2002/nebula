# Nebula WebSocket Example

基于 `nebula-websocket-spring` 模块的实时通信示例，包含 **后端**（Spring Boot）和 **前端**（Vue 3）。

## 功能概览

| 功能 | 说明 |
|------|------|
| 实时聊天 | 客户端发送 `type=chat` 消息，服务端广播给所有在线用户 |
| 上下线通知 | 连接建立/断开时广播系统通知 |
| REST 推送 | 通过 HTTP API 向 WebSocket 客户端推送消息 |
| 在线状态 | 查询在线会话数、用户数、指定用户是否在线 |

## 架构

```
frontend (Vue 3 + Vite)       backend (Spring Boot)
        |                              |
        |--- WebSocket /ws ----------->| SpringWebSocketHandler
        |                              |   ChatMessageHandler
        |                              |   ConnectionEventHandler
        |--- HTTP /ws-api ------------>| WebSocketDemoController
```

## 技术要点

### 后端

- **`WebSocketMessageHandler<T>`** - 按消息类型路由，实现 `getType()` 返回类型标识
- **`WebSocketEventHandler`** - 监听连接生命周期（open/close/error）
- **`WebSocketMessageService`** - 消息发送 API（单发、群发、广播、按主题）
- **`WebSocketMessage<T>`** - 统一消息格式（type + payload + headers）
- **自动配置** - `nebula.websocket.enabled=true` 即可启用

### 前端

- Vue 3 Composition API
- 原生 WebSocket API
- Vite 开发服务器代理 WebSocket 请求

## 快速启动

### 1. 启动后端

```bash
# 安装框架到本地仓库
mvn install -DskipTests -f ../../pom.xml

# 启动后端
mvn spring-boot:run -f backend/pom.xml
```

后端启动在 `http://localhost:8086`，WebSocket 端点为 `ws://localhost:8086/ws`。

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端启动在 `http://localhost:3000`，已配置代理转发到后端。

### 3. 使用方式

1. 打开浏览器访问 `http://localhost:3000`
2. 输入昵称后点击 **Connect**
3. 多个浏览器标签页可同时连接，体验实时聊天

## REST API

| 端点 | 方法 | 说明 |
|------|------|------|
| `/ws-api/status` | GET | 获取在线状态 |
| `/ws-api/broadcast` | POST | 广播消息 |
| `/ws-api/send-to-user` | POST | 向指定用户发送 |
| `/ws-api/send-to-session` | POST | 向指定会话发送 |
| `/ws-api/online/{userId}` | GET | 检查用户是否在线 |

### 示例请求

```bash
# 广播通知
curl -X POST http://localhost:8086/ws-api/broadcast \
  -H "Content-Type: application/json" \
  -d '{"type": "notification", "content": "Hello from REST!"}'

# 查看在线状态
curl http://localhost:8086/ws-api/status
```

## 配置说明

```yaml
nebula:
  websocket:
    enabled: true              # 启用 WebSocket
    endpoint: /ws              # WebSocket 路径
    allowed-origins: ["*"]     # CORS 来源
    sock-js-enabled: false     # 是否启用 SockJS
    heartbeat:
      enabled: true            # 心跳检测
      interval-seconds: 30     # 心跳间隔
      timeout-seconds: 60      # 超时时间
    cluster:
      enabled: false           # 集群模式（需 Redis）
```

## 相关文档

- [Nebula 框架使用指南](../../docs/Nebula框架使用指南.md)
- [Nebula 框架审查报告](../../docs/nebula-framework-review.md)

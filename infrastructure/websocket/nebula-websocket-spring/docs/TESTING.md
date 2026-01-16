# Nebula WebSocket Spring - 测试指南

## 测试前提

- 启动 WebSocket 服务
- 客户端可连接 `/ws`

## 建议测试

1. 建立连接并发送消息，验证 handler 是否被触发。
2. 断线重连与心跳超时逻辑。
3. 大消息体与缓冲区限制校验。

## 集成测试示例

使用 WebSocket 客户端连接并发送测试消息：

```java
WebSocketClient client = new StandardWebSocketClient();
client.doHandshake(new TextWebSocketHandler() {}, "ws://localhost:8080/ws");
```

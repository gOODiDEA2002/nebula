# Nebula WebSocket Netty - 测试指南

## 测试前提

- Netty WebSocket 服务已启动
- 端口与路径配置正确

## 建议测试

1. 使用 WebSocket 客户端连接 `ws://host:port/ws`。
2. 验证消息收发、心跳与空闲断开机制。
3. 压测连接数与消息吞吐。

## 简单连接示例

```bash
websocat ws://localhost:9000/ws
```

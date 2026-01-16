# Nebula WebSocket Core - 测试指南

核心模块仅包含抽象接口，测试重点应放在实现模块。

## 建议

1. 对 `WebSocketMessageHandler` 的业务逻辑做单元测试。
2. 使用实现模块（Spring/Netty）进行集成测试，验证连接与消息收发。
3. 对消息序列化与反序列化路径进行边界测试。

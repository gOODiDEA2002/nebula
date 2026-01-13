# Nebula WebSocket Core

Nebula 框架的 WebSocket 核心抽象模块，定义了 WebSocket 通信的核心接口和模型。

## 特性

- 统一的 WebSocket 会话抽象
- 标准化的消息格式
- 可扩展的消息处理器
- 会话注册表管理
- 集群消息代理接口

## 核心接口

### WebSocketSession

WebSocket 会话抽象，提供：
- 会话 ID 和用户绑定
- 会话属性管理
- 消息发送（文本/二进制/对象）
- 连接状态管理

### WebSocketMessage

统一的消息格式：

```java
@Data
public class WebSocketMessage<T> {
    private String id;          // 消息 ID
    private String type;        // 消息类型
    private String topic;       // 主题（可选）
    private T payload;          // 消息载荷
    private Map<String, String> headers;  // 消息头
    private String senderId;    // 发送者
    private String receiverId;  // 接收者（可选）
    private LocalDateTime createTime;
}
```

### SessionRegistry

会话注册表，管理所有在线会话：
- 注册/注销会话
- 按会话 ID/用户 ID 查找
- 会话统计

### WebSocketMessageHandler

消息处理器接口：

```java
public interface WebSocketMessageHandler<T> {
    void handle(WebSocketSession session, WebSocketMessage<T> message);
    String getType();
    Class<T> getPayloadType();
}
```

### ClusterMessageBroker

集群消息代理接口，用于多实例部署时的消息同步。

## 模块结构

```
nebula-websocket-core/
├── session/
│   ├── WebSocketSession.java      # 会话接口
│   ├── SessionRegistry.java       # 会话注册表接口
│   └── DefaultSessionRegistry.java
├── message/
│   ├── WebSocketMessage.java      # 消息对象
│   └── MessageType.java           # 消息类型常量
├── handler/
│   ├── WebSocketMessageHandler.java   # 消息处理器
│   └── WebSocketEventHandler.java     # 事件处理器
├── cluster/
│   └── ClusterMessageBroker.java  # 集群消息代理
├── exception/
│   ├── WebSocketException.java
│   ├── SessionNotFoundException.java
│   └── MessageSendException.java
└── WebSocketMessageService.java   # 消息服务接口
```

## 许可证

Apache License 2.0


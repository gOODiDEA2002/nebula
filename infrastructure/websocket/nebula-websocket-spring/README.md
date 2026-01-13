# Nebula WebSocket Spring

Nebula 框架基于 Spring WebSocket 的实现模块。

## 特性

- 基于 Spring WebSocket 实现
- 自动配置，开箱即用
- 支持 Redis 集群消息同步
- 可选 SockJS 支持

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-websocket-spring</artifactId>
    <version>${nebula.version}</version>
</dependency>

<!-- 集群模式需要添加 Redis 消息模块 -->
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-messaging-redis</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 2. 配置

```yaml
nebula:
  websocket:
    enabled: true
    endpoint: /ws
    allowed-origins:
      - "*"
    sock-js-enabled: false
    heartbeat:
      enabled: true
      interval-seconds: 30
      timeout-seconds: 60
    cluster:
      enabled: false
      channel-prefix: "websocket:cluster:"
    buffer:
      send-buffer-size-limit: 524288
      send-time-limit: 10000
      message-size-limit: 65536
```

### 3. 发送消息

```java
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final WebSocketMessageService messageService;
    
    // 发送给指定用户
    public void sendToUser(String userId, Object data) {
        WebSocketMessage<Object> message = WebSocketMessage.of("notification", data);
        messageService.sendToUser(userId, message);
    }
    
    // 广播给所有用户
    public void broadcast(Object data) {
        WebSocketMessage<Object> message = WebSocketMessage.of("broadcast", data);
        messageService.broadcast(message);
    }
}
```

### 4. 处理消息

```java
@Component
public class DataMessageHandler implements WebSocketMessageHandler<DataRequest> {
    
    @Override
    public void handle(WebSocketSession session, WebSocketMessage<DataRequest> message) {
        // 处理客户端发送的数据请求
        DataRequest request = message.getPayload();
        // ... 处理逻辑
        
        // 返回响应
        session.send(WebSocketMessage.of("data", response));
    }
    
    @Override
    public String getType() {
        return "data_request";
    }
    
    @Override
    public Class<DataRequest> getPayloadType() {
        return DataRequest.class;
    }
}
```

### 5. 处理连接事件

```java
@Component
public class ConnectionEventHandler implements WebSocketEventHandler {
    
    @Override
    public void onOpen(WebSocketSession session) {
        // 连接建立时处理
        log.info("用户连接: sessionId={}", session.getId());
    }
    
    @Override
    public void onClose(WebSocketSession session, int code, String reason) {
        // 连接关闭时处理
        log.info("用户断开: sessionId={}, reason={}", session.getId(), reason);
    }
}
```

## 集群模式

启用集群模式后，消息会通过 Redis 同步到所有实例：

```yaml
nebula:
  websocket:
    cluster:
      enabled: true
      channel-prefix: "websocket:cluster:"
  messaging:
    redis:
      enabled: true
```

## 客户端连接

### JavaScript 示例

```javascript
const ws = new WebSocket('ws://localhost:8080/ws');

ws.onopen = function() {
    console.log('Connected');
    
    // 发送心跳
    setInterval(() => {
        ws.send(JSON.stringify({ type: 'heartbeat' }));
    }, 30000);
};

ws.onmessage = function(event) {
    const message = JSON.parse(event.data);
    console.log('Received:', message);
    
    switch (message.type) {
        case 'connected':
            console.log('Session ID:', message.payload.sessionId);
            break;
        case 'notification':
            // 处理通知
            break;
        case 'data':
            // 处理数据
            break;
    }
};

ws.onclose = function(event) {
    console.log('Disconnected:', event.code, event.reason);
};

// 发送消息
ws.send(JSON.stringify({
    type: 'data_request',
    payload: { /* 请求数据 */ }
}));
```

## 模块结构

```
nebula-websocket-spring/
├── session/
│   └── SpringWebSocketSession.java    # Spring 会话实现
├── handler/
│   └── SpringWebSocketHandler.java    # 消息处理器
├── cluster/
│   └── RedisClusterMessageBroker.java # Redis 集群代理
├── config/
│   ├── WebSocketProperties.java       # 配置属性
│   └── WebSocketAutoConfiguration.java# 自动配置类
└── SpringWebSocketMessageService.java # 消息服务实现
```

> **注意**: 自动配置由 `nebula-autoconfigure` 模块统一管理，注册在其 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件中。

## 许可证

Apache License 2.0


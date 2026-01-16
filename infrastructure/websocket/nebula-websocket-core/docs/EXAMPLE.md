# Nebula WebSocket Core - 使用示例

`nebula-websocket-core` 提供核心抽象接口，实际使用请结合 Spring 或 Netty 实现。

## 示例 1：自定义消息处理器

```java
@Component
public class TicketMessageHandler implements WebSocketMessageHandler<TicketEvent> {

    @Override
    public void handle(WebSocketSession session, WebSocketMessage<TicketEvent> message) {
        TicketEvent event = message.getPayload();
        // 处理业务消息
    }

    @Override
    public String getType() {
        return "ticket_event";
    }

    @Override
    public Class<TicketEvent> getPayloadType() {
        return TicketEvent.class;
    }
}
```

## 示例 2：构建统一消息对象

```java
WebSocketMessage<String> message = WebSocketMessage.of("notice", "hello");
message.setSenderId("system");
```

## 说明

核心模块不负责连接管理与网络实现，需使用 `nebula-websocket-spring` 或 `nebula-websocket-netty` 提供的具体实现。

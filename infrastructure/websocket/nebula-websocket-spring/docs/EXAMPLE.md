# Nebula WebSocket Spring - 使用示例

## 示例 1：发送消息

```java
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final WebSocketMessageService messageService;

    public void sendToUser(String userId, Object data) {
        WebSocketMessage<Object> message = WebSocketMessage.of("notification", data);
        messageService.sendToUser(userId, message);
    }

    public void broadcast(Object data) {
        WebSocketMessage<Object> message = WebSocketMessage.of("broadcast", data);
        messageService.broadcast(message);
    }
}
```

## 示例 2：处理客户端消息

```java
@Component
public class TicketMessageHandler implements WebSocketMessageHandler<TicketEvent> {

    @Override
    public void handle(WebSocketSession session, WebSocketMessage<TicketEvent> message) {
        TicketEvent event = message.getPayload();
        // 处理票务事件
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

## 示例 3：票务场景（模块示例）

在订单支付完成后推送电子票消息：

```java
public void onOrderPaid(String userId, TicketPayload payload) {
    WebSocketMessage<TicketPayload> message = WebSocketMessage.of("ticket_ready", payload);
    messageService.sendToUser(userId, message);
}
```

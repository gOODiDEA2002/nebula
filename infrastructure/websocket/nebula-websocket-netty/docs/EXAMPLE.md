# Nebula WebSocket Netty - 使用示例

## 示例 1：启动配置

```yaml
nebula:
  websocket:
    netty:
      enabled: true
      port: 9000
      path: /ws
```

## 示例 2：发送消息

```java
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final WebSocketMessageService messageService;

    public void sendToUser(String userId, Object data) {
        WebSocketMessage<Object> message = WebSocketMessage.of("notification", data);
        messageService.sendToUser(userId, message);
    }
}
```

## 示例 3：票务场景（模块示例）

票务系统在出票后推送消息：

```java
public void onTicketIssued(String userId, TicketPayload payload) {
    WebSocketMessage<TicketPayload> message = WebSocketMessage.of("ticket_issued", payload);
    messageService.sendToUser(userId, message);
}
```

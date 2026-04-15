package io.nebula.example.websocket.handler;

import io.nebula.websocket.core.WebSocketMessageService;
import io.nebula.websocket.core.handler.WebSocketMessageHandler;
import io.nebula.websocket.core.message.WebSocketMessage;
import io.nebula.websocket.core.session.WebSocketSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 聊天消息处理器
 * 处理 type="chat" 的消息，广播给所有在线用户
 */
@Slf4j
@Component
@SuppressWarnings("unchecked")
public class ChatMessageHandler implements WebSocketMessageHandler<Map<String, Object>> {

    private final WebSocketMessageService messageService;

    public ChatMessageHandler(@Lazy WebSocketMessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public String getType() {
        return "chat";
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class getPayloadType() {
        return Map.class;
    }

    @Override
    public void handle(WebSocketSession session, WebSocketMessage<Map<String, Object>> message) {
        String userId = session.getUserId() != null ? session.getUserId() : session.getId();
        log.info("[聊天] 收到消息: from={}, payload={}", userId, message.getPayload());

        Object content = message.getPayload() != null
                ? message.getPayload().getOrDefault("content", "")
                : "";

        WebSocketMessage<Map<String, Object>> broadcastMsg = WebSocketMessage.of("chat", Map.of(
                "from", userId,
                "content", content,
                "sessionId", session.getId()
        ));

        int sent = messageService.broadcast(broadcastMsg);
        log.info("[聊天] 广播消息到 {} 个会话", sent);
    }
}

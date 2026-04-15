package io.nebula.example.websocket.handler;

import io.nebula.websocket.core.WebSocketMessageService;
import io.nebula.websocket.core.handler.WebSocketEventHandler;
import io.nebula.websocket.core.message.WebSocketMessage;
import io.nebula.websocket.core.session.WebSocketSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 连接事件处理器
 * 监听连接建立/断开事件，广播上下线通知
 */
@Slf4j
@Component
public class ConnectionEventHandler implements WebSocketEventHandler {

    private final WebSocketMessageService messageService;

    public ConnectionEventHandler(@Lazy WebSocketMessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void onOpen(WebSocketSession session) {
        log.info("[WebSocket] 新连接: sessionId={}, remoteAddr={}",
                session.getId(), session.getRemoteAddress());

        WebSocketMessage<Map<String, Object>> notification = WebSocketMessage.of("system", Map.of(
                "event", "user_joined",
                "sessionId", session.getId(),
                "onlineCount", messageService.getOnlineSessionCount()
        ));
        messageService.broadcast(notification);
    }

    @Override
    public void onClose(WebSocketSession session, int code, String reason) {
        log.info("[WebSocket] 连接关闭: sessionId={}, code={}, reason={}",
                session.getId(), code, reason);

        WebSocketMessage<Map<String, Object>> notification = WebSocketMessage.of("system", Map.of(
                "event", "user_left",
                "sessionId", session.getId(),
                "onlineCount", messageService.getOnlineSessionCount()
        ));
        messageService.broadcast(notification);
    }

    @Override
    public void onError(WebSocketSession session, Throwable throwable) {
        log.error("[WebSocket] 连接错误: sessionId={}", session.getId(), throwable);
    }
}

package io.nebula.websocket.spring.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.websocket.core.handler.WebSocketEventHandler;
import io.nebula.websocket.core.handler.WebSocketMessageHandler;
import io.nebula.websocket.core.message.MessageType;
import io.nebula.websocket.core.message.WebSocketMessage;
import io.nebula.websocket.core.session.SessionRegistry;
import io.nebula.websocket.core.session.WebSocketSession;
import io.nebula.websocket.spring.session.SpringWebSocketSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring WebSocket 处理器
 * <p>
 * 处理 WebSocket 连接的生命周期事件和消息。
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class SpringWebSocketHandler extends AbstractWebSocketHandler {

    private final SessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;
    private final List<WebSocketEventHandler> eventHandlers;
    private final Map<String, WebSocketMessageHandler<?>> messageHandlers;

    /**
     * 会话映射: Spring Session ID -> Nebula Session
     */
    private final Map<String, SpringWebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(org.springframework.web.socket.WebSocketSession session) throws Exception {
        SpringWebSocketSession nebulaSession = new SpringWebSocketSession(session, objectMapper);
        sessions.put(session.getId(), nebulaSession);
        sessionRegistry.register(nebulaSession);

        log.info("WebSocket 连接建立: sessionId={}, remoteAddress={}", 
                session.getId(), nebulaSession.getRemoteAddress());

        // 触发连接事件
        for (WebSocketEventHandler handler : eventHandlers) {
            try {
                handler.onOpen(nebulaSession);
            } catch (Exception e) {
                log.error("连接事件处理失败: {}", e.getMessage(), e);
            }
        }

        // 发送连接成功消息
        nebulaSession.send(WebSocketMessage.of(MessageType.CONNECTED, Map.of(
                "sessionId", session.getId(),
                "message", "连接成功"
        )));
    }

    @Override
    public void afterConnectionClosed(org.springframework.web.socket.WebSocketSession session, 
                                       CloseStatus status) throws Exception {
        SpringWebSocketSession nebulaSession = sessions.remove(session.getId());
        if (nebulaSession != null) {
            sessionRegistry.unregister(session.getId());

            log.info("WebSocket 连接关闭: sessionId={}, code={}, reason={}", 
                    session.getId(), status.getCode(), status.getReason());

            // 触发关闭事件
            for (WebSocketEventHandler handler : eventHandlers) {
                try {
                    handler.onClose(nebulaSession, status.getCode(), status.getReason());
                } catch (Exception e) {
                    log.error("关闭事件处理失败: {}", e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void handleTransportError(org.springframework.web.socket.WebSocketSession session, 
                                      Throwable exception) throws Exception {
        SpringWebSocketSession nebulaSession = sessions.get(session.getId());
        if (nebulaSession != null) {
            log.error("WebSocket 传输错误: sessionId={}, error={}", 
                    session.getId(), exception.getMessage(), exception);

            // 触发错误事件
            for (WebSocketEventHandler handler : eventHandlers) {
                try {
                    handler.onError(nebulaSession, exception);
                } catch (Exception e) {
                    log.error("错误事件处理失败: {}", e.getMessage(), e);
                }
            }
        }
    }

    @Override
    protected void handleTextMessage(org.springframework.web.socket.WebSocketSession session, 
                                      TextMessage message) throws Exception {
        SpringWebSocketSession nebulaSession = sessions.get(session.getId());
        if (nebulaSession == null) {
            log.warn("收到未知会话的消息: sessionId={}", session.getId());
            return;
        }

        nebulaSession.updateLastActiveTime();
        String payload = message.getPayload();

        log.debug("收到文本消息: sessionId={}, length={}", session.getId(), payload.length());

        // 触发文本消息事件
        for (WebSocketEventHandler handler : eventHandlers) {
            try {
                handler.onTextMessage(nebulaSession, payload);
            } catch (Exception e) {
                log.error("文本消息事件处理失败: {}", e.getMessage(), e);
            }
        }

        // 尝试解析为 WebSocketMessage 并路由
        try {
            WebSocketMessage<?> wsMessage = objectMapper.readValue(payload, WebSocketMessage.class);
            routeMessage(nebulaSession, wsMessage);
        } catch (Exception e) {
            log.debug("消息不是标准 WebSocketMessage 格式，跳过路由");
        }
    }

    @Override
    protected void handleBinaryMessage(org.springframework.web.socket.WebSocketSession session, 
                                        BinaryMessage message) throws Exception {
        SpringWebSocketSession nebulaSession = sessions.get(session.getId());
        if (nebulaSession == null) {
            return;
        }

        nebulaSession.updateLastActiveTime();
        byte[] data = message.getPayload().array();

        log.debug("收到二进制消息: sessionId={}, length={}", session.getId(), data.length);

        // 触发二进制消息事件
        for (WebSocketEventHandler handler : eventHandlers) {
            try {
                handler.onBinaryMessage(nebulaSession, data);
            } catch (Exception e) {
                log.error("二进制消息事件处理失败: {}", e.getMessage(), e);
            }
        }
    }

    @Override
    protected void handlePongMessage(org.springframework.web.socket.WebSocketSession session, 
                                      PongMessage message) throws Exception {
        SpringWebSocketSession nebulaSession = sessions.get(session.getId());
        if (nebulaSession != null) {
            nebulaSession.updateLastActiveTime();
            for (WebSocketEventHandler handler : eventHandlers) {
                try {
                    handler.onPong(nebulaSession);
                } catch (Exception e) {
                    log.error("Pong 事件处理失败: {}", e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 路由消息到对应的处理器
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void routeMessage(WebSocketSession session, WebSocketMessage<?> message) {
        String type = message.getType();
        if (type == null) {
            log.debug("消息类型为空，跳过路由");
            return;
        }

        // 处理心跳消息
        if (MessageType.HEARTBEAT.equals(type)) {
            session.send(WebSocketMessage.heartbeat());
            return;
        }

        // 查找消息处理器
        WebSocketMessageHandler handler = messageHandlers.get(type);
        if (handler != null) {
            try {
                handler.handle(session, message);
            } catch (Exception e) {
                log.error("消息处理失败: type={}, error={}", type, e.getMessage(), e);
                session.send(WebSocketMessage.error("HANDLER_ERROR", e.getMessage()));
            }
        } else {
            log.debug("未找到消息处理器: type={}", type);
        }
    }

    /**
     * 获取所有会话
     */
    public Map<String, SpringWebSocketSession> getSessions() {
        return sessions;
    }
}


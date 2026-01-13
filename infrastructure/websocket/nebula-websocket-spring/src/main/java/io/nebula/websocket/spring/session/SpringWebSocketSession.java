package io.nebula.websocket.spring.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.websocket.core.exception.MessageSendException;
import io.nebula.websocket.core.message.WebSocketMessage;
import io.nebula.websocket.core.session.WebSocketSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.BinaryMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring WebSocket 会话实现
 * <p>
 * 包装 Spring WebSocketSession，提供统一的会话接口。
 * </p>
 */
@Slf4j
public class SpringWebSocketSession implements WebSocketSession {

    private final org.springframework.web.socket.WebSocketSession nativeSession;
    private final ObjectMapper objectMapper;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private final LocalDateTime connectTime;

    private String userId;
    private volatile LocalDateTime lastActiveTime;

    public SpringWebSocketSession(org.springframework.web.socket.WebSocketSession nativeSession,
                                   ObjectMapper objectMapper) {
        this.nativeSession = nativeSession;
        this.objectMapper = objectMapper;
        this.connectTime = LocalDateTime.now();
        this.lastActiveTime = this.connectTime;
    }

    @Override
    public String getId() {
        return nativeSession.getId();
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    @Override
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public boolean isOpen() {
        return nativeSession.isOpen();
    }

    @Override
    public void sendText(String text) {
        if (!isOpen()) {
            log.warn("会话已关闭，无法发送消息: sessionId={}", getId());
            return;
        }
        try {
            synchronized (nativeSession) {
                nativeSession.sendMessage(new TextMessage(text));
            }
            updateLastActiveTime();
        } catch (IOException e) {
            throw new MessageSendException(getId(), "发送文本消息失败", e);
        }
    }

    @Override
    public void sendBinary(byte[] data) {
        if (!isOpen()) {
            log.warn("会话已关闭，无法发送消息: sessionId={}", getId());
            return;
        }
        try {
            synchronized (nativeSession) {
                nativeSession.sendMessage(new BinaryMessage(ByteBuffer.wrap(data)));
            }
            updateLastActiveTime();
        } catch (IOException e) {
            throw new MessageSendException(getId(), "发送二进制消息失败", e);
        }
    }

    @Override
    public <T> void send(WebSocketMessage<T> message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            sendText(json);
        } catch (JsonProcessingException e) {
            throw new MessageSendException(getId(), "消息序列化失败", e);
        }
    }

    @Override
    public <T> CompletableFuture<Void> sendAsync(WebSocketMessage<T> message) {
        return CompletableFuture.runAsync(() -> send(message));
    }

    @Override
    public void close() {
        close(1000, "Normal closure");
    }

    @Override
    public void close(int code, String reason) {
        try {
            if (isOpen()) {
                nativeSession.close(new org.springframework.web.socket.CloseStatus(code, reason));
            }
        } catch (IOException e) {
            log.error("关闭会话失败: sessionId={}, error={}", getId(), e.getMessage(), e);
        }
    }

    @Override
    public String getRemoteAddress() {
        InetSocketAddress address = nativeSession.getRemoteAddress();
        if (address != null) {
            return address.getHostString() + ":" + address.getPort();
        }
        return null;
    }

    @Override
    public LocalDateTime getConnectTime() {
        return connectTime;
    }

    @Override
    public LocalDateTime getLastActiveTime() {
        return lastActiveTime;
    }

    @Override
    public void updateLastActiveTime() {
        this.lastActiveTime = LocalDateTime.now();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getNativeSession() {
        return (T) nativeSession;
    }
}


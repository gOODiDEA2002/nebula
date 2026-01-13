package io.nebula.websocket.netty.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.websocket.core.exception.MessageSendException;
import io.nebula.websocket.core.message.WebSocketMessage;
import io.nebula.websocket.core.session.WebSocketSession;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Netty WebSocket 会话实现
 * <p>
 * 高性能 WebSocket 会话，适用于海量连接场景。
 * </p>
 */
@Slf4j
public class NettyWebSocketSession implements WebSocketSession {

    private final Channel channel;
    private final ObjectMapper objectMapper;
    private final String sessionId;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private final LocalDateTime connectTime;

    private String userId;
    private volatile LocalDateTime lastActiveTime;

    public NettyWebSocketSession(Channel channel, ObjectMapper objectMapper) {
        this.channel = channel;
        this.objectMapper = objectMapper;
        this.sessionId = channel.id().asLongText();
        this.connectTime = LocalDateTime.now();
        this.lastActiveTime = this.connectTime;
    }

    @Override
    public String getId() {
        return sessionId;
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
        return channel.isActive();
    }

    @Override
    public void sendText(String text) {
        if (!isOpen()) {
            log.warn("会话已关闭，无法发送消息: sessionId={}", sessionId);
            return;
        }
        channel.writeAndFlush(new TextWebSocketFrame(text));
        updateLastActiveTime();
    }

    @Override
    public void sendBinary(byte[] data) {
        if (!isOpen()) {
            log.warn("会话已关闭，无法发送消息: sessionId={}", sessionId);
            return;
        }
        channel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(data)));
        updateLastActiveTime();
    }

    @Override
    public <T> void send(WebSocketMessage<T> message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            sendText(json);
        } catch (JsonProcessingException e) {
            throw new MessageSendException(sessionId, "消息序列化失败", e);
        }
    }

    @Override
    public <T> CompletableFuture<Void> sendAsync(WebSocketMessage<T> message) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            String json = objectMapper.writeValueAsString(message);
            if (!isOpen()) {
                future.completeExceptionally(new MessageSendException(sessionId, "会话已关闭"));
                return future;
            }
            ChannelFuture channelFuture = channel.writeAndFlush(new TextWebSocketFrame(json));
            channelFuture.addListener(f -> {
                if (f.isSuccess()) {
                    updateLastActiveTime();
                    future.complete(null);
                } else {
                    future.completeExceptionally(f.cause());
                }
            });
        } catch (JsonProcessingException e) {
            future.completeExceptionally(new MessageSendException(sessionId, "消息序列化失败", e));
        }
        return future;
    }

    @Override
    public void close() {
        close(1000, "Normal closure");
    }

    @Override
    public void close(int code, String reason) {
        if (isOpen()) {
            channel.writeAndFlush(new CloseWebSocketFrame(code, reason))
                    .addListener(f -> channel.close());
        }
    }

    @Override
    public String getRemoteAddress() {
        InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
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
        return (T) channel;
    }

    /**
     * 获取 Netty Channel
     */
    public Channel getChannel() {
        return channel;
    }
}


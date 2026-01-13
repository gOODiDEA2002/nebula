package io.nebula.websocket.netty.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.websocket.core.handler.WebSocketEventHandler;
import io.nebula.websocket.core.handler.WebSocketMessageHandler;
import io.nebula.websocket.core.message.MessageType;
import io.nebula.websocket.core.message.WebSocketMessage;
import io.nebula.websocket.core.session.SessionRegistry;
import io.nebula.websocket.netty.session.NettyWebSocketSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * WebSocket 帧处理器
 * <p>
 * 处理 WebSocket 消息帧，包括文本、二进制、Ping/Pong、关闭等。
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final SessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;
    private final List<WebSocketEventHandler> eventHandlers;
    private final Map<String, WebSocketMessageHandler<?>> messageHandlers;

    /**
     * Channel 属性键：会话对象
     */
    public static final String SESSION_ATTRIBUTE = "websocket.session";

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 创建会话
        NettyWebSocketSession session = new NettyWebSocketSession(ctx.channel(), objectMapper);
        ctx.channel().attr(io.netty.util.AttributeKey.<NettyWebSocketSession>valueOf(SESSION_ATTRIBUTE)).set(session);
        sessionRegistry.register(session);

        log.info("WebSocket 连接建立: sessionId={}, remoteAddress={}", 
                session.getId(), session.getRemoteAddress());

        // 触发连接事件
        for (WebSocketEventHandler handler : eventHandlers) {
            try {
                handler.onOpen(session);
            } catch (Exception e) {
                log.error("连接事件处理失败: {}", e.getMessage(), e);
            }
        }

        // 发送连接成功消息
        session.send(WebSocketMessage.of(MessageType.CONNECTED, Map.of(
                "sessionId", session.getId(),
                "message", "连接成功"
        )));

        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyWebSocketSession session = getSession(ctx);
        if (session != null) {
            sessionRegistry.unregister(session.getId());

            log.info("WebSocket 连接关闭: sessionId={}", session.getId());

            // 触发关闭事件
            for (WebSocketEventHandler handler : eventHandlers) {
                try {
                    handler.onClose(session, 1000, "Connection closed");
                } catch (Exception e) {
                    log.error("关闭事件处理失败: {}", e.getMessage(), e);
                }
            }
        }

        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        NettyWebSocketSession session = getSession(ctx);
        if (session == null) {
            log.warn("收到未知会话的消息");
            return;
        }

        session.updateLastActiveTime();

        if (frame instanceof TextWebSocketFrame) {
            handleTextFrame(session, (TextWebSocketFrame) frame);
        } else if (frame instanceof BinaryWebSocketFrame) {
            handleBinaryFrame(session, (BinaryWebSocketFrame) frame);
        } else if (frame instanceof PingWebSocketFrame) {
            handlePingFrame(ctx, session);
        } else if (frame instanceof PongWebSocketFrame) {
            handlePongFrame(session);
        } else if (frame instanceof CloseWebSocketFrame) {
            handleCloseFrame(ctx, session, (CloseWebSocketFrame) frame);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        NettyWebSocketSession session = getSession(ctx);
        if (session != null) {
            log.error("WebSocket 异常: sessionId={}, error={}", 
                    session.getId(), cause.getMessage(), cause);

            // 触发错误事件
            for (WebSocketEventHandler handler : eventHandlers) {
                try {
                    handler.onError(session, cause);
                } catch (Exception e) {
                    log.error("错误事件处理失败: {}", e.getMessage(), e);
                }
            }
        }
        ctx.close();
    }

    /**
     * 处理文本帧
     */
    private void handleTextFrame(NettyWebSocketSession session, TextWebSocketFrame frame) {
        String text = frame.text();
        log.debug("收到文本消息: sessionId={}, length={}", session.getId(), text.length());

        // 触发文本消息事件
        for (WebSocketEventHandler handler : eventHandlers) {
            try {
                handler.onTextMessage(session, text);
            } catch (Exception e) {
                log.error("文本消息事件处理失败: {}", e.getMessage(), e);
            }
        }

        // 尝试解析为 WebSocketMessage 并路由
        try {
            WebSocketMessage<?> message = objectMapper.readValue(text, WebSocketMessage.class);
            routeMessage(session, message);
        } catch (Exception e) {
            log.debug("消息不是标准 WebSocketMessage 格式，跳过路由");
        }
    }

    /**
     * 处理二进制帧
     */
    private void handleBinaryFrame(NettyWebSocketSession session, BinaryWebSocketFrame frame) {
        byte[] data = new byte[frame.content().readableBytes()];
        frame.content().readBytes(data);

        log.debug("收到二进制消息: sessionId={}, length={}", session.getId(), data.length);

        // 触发二进制消息事件
        for (WebSocketEventHandler handler : eventHandlers) {
            try {
                handler.onBinaryMessage(session, data);
            } catch (Exception e) {
                log.error("二进制消息事件处理失败: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 处理 Ping 帧
     */
    private void handlePingFrame(ChannelHandlerContext ctx, NettyWebSocketSession session) {
        ctx.writeAndFlush(new PongWebSocketFrame());
        for (WebSocketEventHandler handler : eventHandlers) {
            try {
                handler.onPing(session);
            } catch (Exception e) {
                log.error("Ping 事件处理失败: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 处理 Pong 帧
     */
    private void handlePongFrame(NettyWebSocketSession session) {
        for (WebSocketEventHandler handler : eventHandlers) {
            try {
                handler.onPong(session);
            } catch (Exception e) {
                log.error("Pong 事件处理失败: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 处理关闭帧
     */
    private void handleCloseFrame(ChannelHandlerContext ctx, NettyWebSocketSession session, 
                                   CloseWebSocketFrame frame) {
        int code = frame.statusCode();
        String reason = frame.reasonText();
        log.info("收到关闭帧: sessionId={}, code={}, reason={}", session.getId(), code, reason);
        ctx.close();
    }

    /**
     * 路由消息到对应的处理器
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void routeMessage(NettyWebSocketSession session, WebSocketMessage<?> message) {
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
     * 获取会话对象
     */
    private NettyWebSocketSession getSession(ChannelHandlerContext ctx) {
        return ctx.channel().attr(
                io.netty.util.AttributeKey.<NettyWebSocketSession>valueOf(SESSION_ATTRIBUTE)
        ).get();
    }
}


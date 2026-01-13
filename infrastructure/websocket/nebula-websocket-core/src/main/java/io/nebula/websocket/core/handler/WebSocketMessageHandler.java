package io.nebula.websocket.core.handler;

import io.nebula.websocket.core.message.WebSocketMessage;
import io.nebula.websocket.core.session.WebSocketSession;

/**
 * WebSocket 消息处理器接口
 * <p>
 * 处理客户端发送的消息。
 * </p>
 *
 * @param <T> 消息载荷类型
 */
public interface WebSocketMessageHandler<T> {

    /**
     * 处理消息
     *
     * @param session 会话对象
     * @param message 消息对象
     */
    void handle(WebSocketSession session, WebSocketMessage<T> message);

    /**
     * 获取支持的消息类型
     * <p>
     * 返回该处理器支持的消息类型，用于消息路由。
     * </p>
     *
     * @return 消息类型
     */
    String getType();

    /**
     * 获取消息载荷类型
     *
     * @return 载荷类型
     */
    Class<T> getPayloadType();
}


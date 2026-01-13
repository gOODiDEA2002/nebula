package io.nebula.websocket.core.handler;

import io.nebula.websocket.core.session.WebSocketSession;

/**
 * WebSocket 事件处理器接口
 * <p>
 * 处理连接生命周期事件。
 * </p>
 */
public interface WebSocketEventHandler {

    /**
     * 连接建立时调用
     *
     * @param session 会话对象
     */
    default void onOpen(WebSocketSession session) {
    }

    /**
     * 连接关闭时调用
     *
     * @param session 会话对象
     * @param code    关闭代码
     * @param reason  关闭原因
     */
    default void onClose(WebSocketSession session, int code, String reason) {
    }

    /**
     * 发生错误时调用
     *
     * @param session   会话对象
     * @param throwable 异常对象
     */
    default void onError(WebSocketSession session, Throwable throwable) {
    }

    /**
     * 收到文本消息时调用
     *
     * @param session 会话对象
     * @param text    文本内容
     */
    default void onTextMessage(WebSocketSession session, String text) {
    }

    /**
     * 收到二进制消息时调用
     *
     * @param session 会话对象
     * @param data    二进制数据
     */
    default void onBinaryMessage(WebSocketSession session, byte[] data) {
    }

    /**
     * 收到心跳 Ping 时调用
     *
     * @param session 会话对象
     */
    default void onPing(WebSocketSession session) {
    }

    /**
     * 收到心跳 Pong 时调用
     *
     * @param session 会话对象
     */
    default void onPong(WebSocketSession session) {
    }
}


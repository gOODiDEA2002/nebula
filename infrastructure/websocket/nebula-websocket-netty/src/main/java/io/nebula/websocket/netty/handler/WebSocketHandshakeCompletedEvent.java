package io.nebula.websocket.netty.handler;

/**
 * WebSocket 握手完成事件（MW-5）
 *
 * <p>由 {@link HttpRequestHandler} 在握手响应写出成功后沿 pipeline 传播,
 * {@link WebSocketFrameHandler} 收到后才注册会话。此前会话在 channelActive
 * (TCP 建连)时注册, 任何 HTTP 请求(健康检查/端口扫描)都会产生幽灵会话,
 * 且 CONNECTED 帧在握手完成前发出(协议违规)。</p>
 *
 * @param requestUri 握手请求 URI（含查询参数, 供鉴权/路由扩展使用）
 * @author Nebula Framework
 * @since 2.0.1
 */
public record WebSocketHandshakeCompletedEvent(String requestUri) {
}

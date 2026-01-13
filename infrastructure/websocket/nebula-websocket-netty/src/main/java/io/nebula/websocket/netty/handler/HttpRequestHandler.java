package io.nebula.websocket.netty.handler;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * HTTP 请求处理器
 * <p>
 * 处理 WebSocket 握手请求和普通 HTTP 请求。
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final String websocketPath;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        // 处理 HTTP 请求
        if (!request.decoderResult().isSuccess()) {
            sendHttpResponse(ctx, request, new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        // 只接受 GET 请求
        if (!HttpMethod.GET.equals(request.method())) {
            sendHttpResponse(ctx, request, new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED));
            return;
        }

        String uri = request.uri();

        // 检查是否是 WebSocket 握手请求
        if (uri.equals(websocketPath) || uri.startsWith(websocketPath + "?")) {
            // WebSocket 握手
            handleWebSocketHandshake(ctx, request);
        } else if ("/health".equals(uri)) {
            // 健康检查
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
            response.content().writeBytes("OK".getBytes());
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            ctx.writeAndFlush(response);
        } else {
            // 其他请求返回 404
            sendHttpResponse(ctx, request, new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND));
        }
    }

    /**
     * 处理 WebSocket 握手
     */
    private void handleWebSocketHandshake(ChannelHandlerContext ctx, FullHttpRequest request) {
        String webSocketLocation = getWebSocketLocation(request);
        
        WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(
                webSocketLocation, null, true, 65536);
        
        WebSocketServerHandshaker handshaker = factory.newHandshaker(request);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), request);
            log.debug("WebSocket 握手成功: {}", ctx.channel().remoteAddress());
        }
    }

    /**
     * 获取 WebSocket URL
     */
    private String getWebSocketLocation(FullHttpRequest request) {
        String location = request.headers().get(HttpHeaderNames.HOST) + websocketPath;
        // 根据是否使用 SSL 决定协议
        return "ws://" + location;
    }

    /**
     * 发送 HTTP 响应
     */
    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest request, 
                                   FullHttpResponse response) {
        // 返回应答给客户端
        if (response.status().code() != 200) {
            response.content().writeBytes(response.status().toString().getBytes());
            HttpUtil.setContentLength(response, response.content().readableBytes());
        }

        // 如果是非 Keep-Alive，关闭连接
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        if (!keepAlive) {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(response);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("HTTP 处理异常: {}", cause.getMessage(), cause);
        ctx.close();
    }
}


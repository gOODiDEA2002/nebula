package io.nebula.websocket.netty.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.nebula.websocket.core.session.SessionRegistry;
import io.nebula.websocket.core.session.WebSocketSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Netty WebSocket 握手时机与空闲清理测试（MW-5）
 *
 * <p>此前会话在 channelActive(TCP 建连)注册: 任何 HTTP 请求都产生幽灵会话,
 * CONNECTED 帧在握手前发出; IdleStateEvent 无人消费, 半死连接永不清理。</p>
 */
class WebSocketFrameHandlerTest {

    private SessionRegistry sessionRegistry;
    private EmbeddedChannel channel;

    @BeforeEach
    void setUp() {
        sessionRegistry = mock(SessionRegistry.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        channel = new EmbeddedChannel(
                new HttpServerCodec(),
                new HttpObjectAggregator(65536),
                new HttpRequestHandler("/ws"),
                new WebSocketFrameHandler(sessionRegistry, objectMapper, List.of(), Map.of()));
    }

    @Test
    @DisplayName("TCP 建连本身不再注册会话(修复幽灵会话)")
    void tcpConnectDoesNotRegisterSession() {
        // EmbeddedChannel 构造即 active, channelActive 已触发
        assertThat(channel.isActive()).isTrue();
        verify(sessionRegistry, never()).register(any());
    }

    @Test
    @DisplayName("普通 HTTP 请求(健康检查)不注册会话")
    void plainHttpRequestDoesNotRegisterSession() {
        channel.writeInbound(httpBytes(
                "GET /health HTTP/1.1\r\nHost: localhost\r\n\r\n"));

        verify(sessionRegistry, never()).register(any());
        // 响应正常返回
        assertThat((Object) channel.readOutbound()).isNotNull();
    }

    @Test
    @DisplayName("WebSocket 握手完成后才注册会话并发送 CONNECTED 帧")
    void handshakeRegistersSessionAndSendsConnected() {
        channel.writeInbound(httpBytes(
                "GET /ws HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n" +
                "Sec-WebSocket-Version: 13\r\n\r\n"));

        verify(sessionRegistry).register(any(WebSocketSession.class));

        // 第一段出站字节: 101 握手响应; 其后: CONNECTED 帧
        ByteBuf first = channel.readOutbound();
        String response = first.toString(StandardCharsets.US_ASCII);
        first.release();
        assertThat(response).contains("101 Switching Protocols");
        assertThat((Object) channel.readOutbound()).as("CONNECTED 帧应在握手响应之后发出").isNotNull();
    }

    @Test
    @DisplayName("空闲事件触发连接关闭(半死连接清理)")
    void idleStateEventClosesConnection() {
        assertThat(channel.isOpen()).isTrue();

        channel.pipeline().fireUserEventTriggered(IdleStateEvent.READER_IDLE_STATE_EVENT);

        assertThat(channel.isOpen()).isFalse();
    }

    private ByteBuf httpBytes(String raw) {
        return Unpooled.copiedBuffer(raw, StandardCharsets.US_ASCII);
    }
}

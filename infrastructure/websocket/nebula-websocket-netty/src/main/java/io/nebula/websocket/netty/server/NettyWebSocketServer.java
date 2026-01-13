package io.nebula.websocket.netty.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.websocket.core.handler.WebSocketEventHandler;
import io.nebula.websocket.core.handler.WebSocketMessageHandler;
import io.nebula.websocket.core.session.SessionRegistry;
import io.nebula.websocket.netty.config.NettyWebSocketProperties;
import io.nebula.websocket.netty.handler.HttpRequestHandler;
import io.nebula.websocket.netty.handler.WebSocketFrameHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Netty WebSocket 服务器
 * <p>
 * 高性能 WebSocket 服务器，支持百万级连接。
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class NettyWebSocketServer {

    private final NettyWebSocketProperties properties;
    private final SessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;
    private final List<WebSocketEventHandler> eventHandlers;
    private final Map<String, WebSocketMessageHandler<?>> messageHandlers;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    @PostConstruct
    public void start() {
        int bossThreads = properties.getBossThreads();
        int workerThreads = properties.getWorkerThreads();
        int port = properties.getPort();
        String path = properties.getPath();

        bossGroup = new NioEventLoopGroup(bossThreads);
        workerGroup = new NioEventLoopGroup(workerThreads);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();

                            // HTTP 编解码
                            pipeline.addLast(new HttpServerCodec());
                            // 分块写
                            pipeline.addLast(new ChunkedWriteHandler());
                            // HTTP 消息聚合
                            pipeline.addLast(new HttpObjectAggregator(properties.getMaxContentLength()));
                            // 空闲检测
                            pipeline.addLast(new IdleStateHandler(
                                    properties.getReaderIdleTime(),
                                    properties.getWriterIdleTime(),
                                    properties.getAllIdleTime(),
                                    TimeUnit.SECONDS
                            ));
                            // HTTP 请求处理（WebSocket 握手）
                            pipeline.addLast(new HttpRequestHandler(path));
                            // WebSocket 帧处理
                            pipeline.addLast(new WebSocketFrameHandler(
                                    sessionRegistry, objectMapper, eventHandlers, messageHandlers
                            ));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, properties.getBacklog())
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();

            log.info("Netty WebSocket 服务器已启动: port={}, path={}, bossThreads={}, workerThreads={}",
                    port, path, bossThreads, workerThreads);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Netty WebSocket 服务器启动失败: {}", e.getMessage(), e);
            throw new RuntimeException("WebSocket 服务器启动失败", e);
        }
    }

    @PreDestroy
    public void stop() {
        log.info("正在关闭 Netty WebSocket 服务器...");

        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

        log.info("Netty WebSocket 服务器已关闭");
    }

    /**
     * 获取服务器端口
     */
    public int getPort() {
        return properties.getPort();
    }

    /**
     * 检查服务器是否运行
     */
    public boolean isRunning() {
        return serverChannel != null && serverChannel.isActive();
    }
}


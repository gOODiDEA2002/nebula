package io.nebula.websocket.netty.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.websocket.core.WebSocketMessageService;
import io.nebula.websocket.core.cluster.ClusterMessageBroker;
import io.nebula.websocket.core.handler.WebSocketEventHandler;
import io.nebula.websocket.core.handler.WebSocketMessageHandler;
import io.nebula.websocket.core.session.DefaultSessionRegistry;
import io.nebula.websocket.core.session.SessionRegistry;
import io.nebula.websocket.netty.NettyWebSocketMessageService;
import io.nebula.websocket.netty.server.NettyWebSocketServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Netty WebSocket 自动配置
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "nebula.websocket.netty", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(NettyWebSocketProperties.class)
@RequiredArgsConstructor
public class NettyWebSocketAutoConfiguration {

    private final NettyWebSocketProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 会话注册表
     */
    @Bean
    @ConditionalOnMissingBean
    public SessionRegistry sessionRegistry() {
        log.info("初始化 Netty WebSocket 会话注册表");
        return new DefaultSessionRegistry();
    }

    /**
     * Netty WebSocket 服务器
     */
    @Bean
    @ConditionalOnMissingBean
    public NettyWebSocketServer nettyWebSocketServer(
            SessionRegistry sessionRegistry,
            List<WebSocketEventHandler> eventHandlers,
            List<WebSocketMessageHandler<?>> messageHandlers) {

        Map<String, WebSocketMessageHandler<?>> handlerMap = messageHandlers.stream()
                .collect(Collectors.toMap(WebSocketMessageHandler::getType, h -> h));

        log.info("初始化 Netty WebSocket 服务器, port={}, path={}, handlers={}",
                properties.getPort(), properties.getPath(), handlerMap.keySet());

        return new NettyWebSocketServer(
                properties,
                sessionRegistry,
                objectMapper,
                eventHandlers != null ? eventHandlers : Collections.emptyList(),
                handlerMap
        );
    }

    /**
     * 默认集群消息代理（空实现）
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "nebula.websocket.netty.cluster", name = "enabled", havingValue = "false", matchIfMissing = true)
    public ClusterMessageBroker noopClusterMessageBroker() {
        log.info("集群模式未启用，使用空实现");
        return new NoopClusterMessageBroker();
    }

    /**
     * WebSocket 消息服务
     */
    @Bean
    @ConditionalOnMissingBean
    public WebSocketMessageService webSocketMessageService(SessionRegistry sessionRegistry,
                                                            ClusterMessageBroker clusterMessageBroker) {
        log.info("初始化 Netty WebSocket 消息服务");
        return new NettyWebSocketMessageService(sessionRegistry, clusterMessageBroker);
    }

    /**
     * 空集群消息代理
     */
    private static class NoopClusterMessageBroker implements ClusterMessageBroker {
        @Override
        public <T> void publish(String channel, io.nebula.websocket.core.message.WebSocketMessage<T> message) {}

        @Override
        public <T> void subscribe(String channel, java.util.function.Consumer<io.nebula.websocket.core.message.WebSocketMessage<T>> handler) {}

        @Override
        public void unsubscribe(String channel) {}

        @Override
        public <T> void publishToUser(String userId, io.nebula.websocket.core.message.WebSocketMessage<T> message) {}

        @Override
        public <T> void publishBroadcast(io.nebula.websocket.core.message.WebSocketMessage<T> message) {}

        @Override
        public <T> void publishToTopic(String topic, io.nebula.websocket.core.message.WebSocketMessage<T> message) {}

        @Override
        public void start() {}

        @Override
        public void stop() {}

        @Override
        public boolean isAvailable() {
            return false;
        }
    }
}


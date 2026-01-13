package io.nebula.websocket.spring.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.websocket.core.WebSocketMessageService;
import io.nebula.websocket.core.cluster.ClusterMessageBroker;
import io.nebula.websocket.core.handler.WebSocketEventHandler;
import io.nebula.websocket.core.handler.WebSocketMessageHandler;
import io.nebula.websocket.core.session.DefaultSessionRegistry;
import io.nebula.websocket.core.session.SessionRegistry;
import io.nebula.websocket.spring.SpringWebSocketMessageService;
import io.nebula.websocket.spring.cluster.RedisClusterMessageBroker;
import io.nebula.websocket.spring.handler.SpringWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * WebSocket 自动配置
 */
@Slf4j
@AutoConfiguration
@EnableWebSocket
@ConditionalOnProperty(prefix = "nebula.websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(WebSocketProperties.class)
@RequiredArgsConstructor
public class WebSocketAutoConfiguration implements WebSocketConfigurer {

    private final WebSocketProperties properties;
    private final ObjectMapper objectMapper;
    private final List<WebSocketEventHandler> eventHandlers;
    private final List<WebSocketMessageHandler<?>> messageHandlers;

    /**
     * 会话注册表
     */
    @Bean
    @ConditionalOnMissingBean
    public SessionRegistry sessionRegistry() {
        log.info("初始化 WebSocket 会话注册表");
        return new DefaultSessionRegistry();
    }

    /**
     * WebSocket 处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public SpringWebSocketHandler springWebSocketHandler(SessionRegistry sessionRegistry) {
        Map<String, WebSocketMessageHandler<?>> handlerMap = messageHandlers.stream()
                .collect(Collectors.toMap(WebSocketMessageHandler::getType, h -> h));
        log.info("初始化 WebSocket 处理器, 注册消息处理器: {}", handlerMap.keySet());
        return new SpringWebSocketHandler(
                sessionRegistry, 
                objectMapper, 
                eventHandlers != null ? eventHandlers : Collections.emptyList(),
                handlerMap
        );
    }

    /**
     * WebSocket 消息服务
     */
    @Bean
    @ConditionalOnMissingBean
    public WebSocketMessageService webSocketMessageService(SessionRegistry sessionRegistry,
                                                            ClusterMessageBroker clusterMessageBroker) {
        log.info("初始化 WebSocket 消息服务");
        return new SpringWebSocketMessageService(sessionRegistry, clusterMessageBroker);
    }

    /**
     * 默认集群消息代理（空实现）
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "nebula.websocket.cluster", name = "enabled", havingValue = "false", matchIfMissing = true)
    public ClusterMessageBroker noopClusterMessageBroker() {
        log.info("集群模式未启用，使用空实现");
        return new NoopClusterMessageBroker();
    }

    /**
     * Servlet 容器配置
     */
    @Bean
    @ConditionalOnMissingBean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(properties.getBuffer().getMessageSizeLimit());
        container.setMaxBinaryMessageBufferSize(properties.getBuffer().getMessageSizeLimit());
        container.setMaxSessionIdleTimeout((long) properties.getHeartbeat().getTimeoutSeconds() * 1000);
        return container;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        SpringWebSocketHandler handler = springWebSocketHandler(sessionRegistry());

        var registration = registry
                .addHandler(handler, properties.getEndpoint())
                .setAllowedOrigins(properties.getAllowedOrigins());

        if (properties.isSockJsEnabled()) {
            registration.withSockJS();
        }

        log.info("注册 WebSocket 端点: {}", properties.getEndpoint());
    }

    // ========== 内部类 ==========

    /**
     * 空集群消息代理（单机模式使用）
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

    // ========== Redis 集群配置 ==========

    @AutoConfiguration
    @ConditionalOnProperty(prefix = "nebula.websocket.cluster", name = "enabled", havingValue = "true")
    @ConditionalOnClass(name = "io.nebula.messaging.redis.RedisMessageManager")
    public static class RedisClusterAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnBean(name = "redisMessageManager")
        public ClusterMessageBroker redisClusterMessageBroker(
                Object redisMessageManager,
                WebSocketProperties properties) {
            log.info("初始化 Redis 集群消息代理");
            return new RedisClusterMessageBroker(
                    (io.nebula.messaging.redis.RedisMessageManager) redisMessageManager,
                    properties.getCluster().getChannelPrefix()
            );
        }
    }
}


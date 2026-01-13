package io.nebula.websocket.spring.cluster;

import io.nebula.messaging.redis.RedisMessageManager;
import io.nebula.websocket.core.cluster.ClusterMessageBroker;
import io.nebula.websocket.core.message.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Redis 集群消息代理
 * <p>
 * 基于 Redis Pub/Sub 实现的集群消息同步。
 * 用于在多实例部署时同步 WebSocket 消息。
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class RedisClusterMessageBroker implements ClusterMessageBroker {

    private final RedisMessageManager redisMessageManager;
    private final String channelPrefix;

    private final Map<String, Boolean> subscriptions = new ConcurrentHashMap<>();
    private volatile boolean running = false;

    /**
     * 用户消息频道
     */
    private static final String USER_CHANNEL = "user";

    /**
     * 广播消息频道
     */
    private static final String BROADCAST_CHANNEL = "broadcast";

    /**
     * 主题消息频道前缀
     */
    private static final String TOPIC_CHANNEL_PREFIX = "topic:";

    @PostConstruct
    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;
        log.info("Redis 集群消息代理已启动, channelPrefix={}", channelPrefix);
    }

    @PreDestroy
    @Override
    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        // 取消所有订阅
        subscriptions.keySet().forEach(this::unsubscribe);
        subscriptions.clear();
        log.info("Redis 集群消息代理已停止");
    }

    @Override
    public <T> void publish(String channel, WebSocketMessage<T> message) {
        if (!running) {
            log.warn("集群消息代理未运行，消息未发送");
            return;
        }
        String fullChannel = buildChannel(channel);
        redisMessageManager.publish(fullChannel, message);
        log.debug("发布集群消息: channel={}", fullChannel);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void subscribe(String channel, Consumer<WebSocketMessage<T>> handler) {
        if (!running) {
            log.warn("集群消息代理未运行，订阅未生效");
            return;
        }
        String fullChannel = buildChannel(channel);
        redisMessageManager.subscribe(fullChannel, msg -> {
            // Redis 消息载荷就是 WebSocketMessage
            Object payload = msg.getPayload();
            if (payload instanceof WebSocketMessage) {
                handler.accept((WebSocketMessage<T>) payload);
            } else {
                log.warn("收到非 WebSocketMessage 类型的消息: {}", payload.getClass());
            }
        });
        subscriptions.put(channel, true);
        log.info("订阅集群频道: {}", fullChannel);
    }

    @Override
    public void unsubscribe(String channel) {
        String fullChannel = buildChannel(channel);
        redisMessageManager.unsubscribe(fullChannel);
        subscriptions.remove(channel);
        log.info("取消订阅集群频道: {}", fullChannel);
    }

    @Override
    public <T> void publishToUser(String userId, WebSocketMessage<T> message) {
        if (!running) {
            return;
        }
        String channel = USER_CHANNEL + ":" + userId;
        publish(channel, message);
    }

    @Override
    public <T> void publishBroadcast(WebSocketMessage<T> message) {
        if (!running) {
            return;
        }
        publish(BROADCAST_CHANNEL, message);
    }

    @Override
    public <T> void publishToTopic(String topic, WebSocketMessage<T> message) {
        if (!running) {
            return;
        }
        String channel = TOPIC_CHANNEL_PREFIX + topic;
        publish(channel, message);
    }

    @Override
    public boolean isAvailable() {
        return running && redisMessageManager.isPubSubAvailable();
    }

    /**
     * 构建完整频道名称
     */
    private String buildChannel(String channel) {
        return channelPrefix + channel;
    }
}


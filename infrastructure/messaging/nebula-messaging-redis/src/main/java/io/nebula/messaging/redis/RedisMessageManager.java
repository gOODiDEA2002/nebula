package io.nebula.messaging.redis;

import io.nebula.messaging.core.consumer.MessageHandler;
import io.nebula.messaging.core.message.Message;
import io.nebula.messaging.core.producer.MessageProducer.SendResult;
import io.nebula.messaging.redis.config.RedisMessagingProperties;
import io.nebula.messaging.redis.consumer.RedisMessageConsumer;
import io.nebula.messaging.redis.producer.RedisMessageProducer;
import io.nebula.messaging.redis.stream.RedisStreamConsumer;
import io.nebula.messaging.redis.stream.RedisStreamProducer;
import io.nebula.messaging.redis.support.RedisMessageSerializer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Redis 消息管理器
 * <p>
 * 统一管理 Redis Pub/Sub 和 Stream 消息的发送与订阅。
 * 提供简化的 API，隐藏底层实现细节。
 * </p>
 * 
 * <pre>
 * 使用示例:
 * 
 * // 发送 Pub/Sub 消息
 * redisMessageManager.publish("user:notification", notification);
 * 
 * // 发送 Stream 消息（可靠）
 * redisMessageManager.send("order:events", orderEvent);
 * 
 * // 订阅 Pub/Sub 消息
 * redisMessageManager.subscribe("user:notification", message -> {
 *     // 处理消息
 * });
 * 
 * // 订阅 Stream 消息
 * redisMessageManager.subscribeStream("order:events", message -> {
 *     // 处理消息
 * });
 * </pre>
 */
@Slf4j
public class RedisMessageManager {

    @Getter
    private final RedisMessagingProperties properties;

    @Getter
    private final RedisMessageSerializer serializer;

    @Getter
    private final RedisMessageProducer<Object> pubsubProducer;

    @Getter
    private final RedisMessageConsumer<Object> pubsubConsumer;

    private RedisStreamProducer<Object> streamProducer;
    private RedisStreamConsumer<Object> streamConsumer;

    public RedisMessageManager(RedisMessagingProperties properties,
                               RedisMessageSerializer serializer,
                               RedisMessageProducer<Object> pubsubProducer,
                               RedisMessageConsumer<Object> pubsubConsumer) {
        this.properties = properties;
        this.serializer = serializer;
        this.pubsubProducer = pubsubProducer;
        this.pubsubConsumer = pubsubConsumer;
    }

    /**
     * 设置 Stream 生产者（可选）
     */
    public void setStreamProducer(RedisStreamProducer<Object> streamProducer) {
        this.streamProducer = streamProducer;
    }

    /**
     * 设置 Stream 消费者（可选）
     */
    public void setStreamConsumer(RedisStreamConsumer<Object> streamConsumer) {
        this.streamConsumer = streamConsumer;
    }

    @PostConstruct
    public void init() {
        pubsubProducer.start();
        pubsubConsumer.start();
        if (streamProducer != null) {
            streamProducer.start();
        }
        if (streamConsumer != null) {
            streamConsumer.start();
        }
        log.info("Redis 消息管理器已启动");
    }

    @PreDestroy
    public void destroy() {
        pubsubProducer.stop();
        pubsubConsumer.stop();
        if (streamProducer != null) {
            streamProducer.stop();
        }
        if (streamConsumer != null) {
            streamConsumer.stop();
        }
        log.info("Redis 消息管理器已停止");
    }

    // ========== Pub/Sub 操作 ==========

    /**
     * 发布消息到频道（Pub/Sub 模式）
     *
     * @param channel 频道名称
     * @param payload 消息载荷
     * @param <T>     载荷类型
     * @return 发送结果
     */
    @SuppressWarnings("unchecked")
    public <T> SendResult publish(String channel, T payload) {
        return ((RedisMessageProducer<T>) (RedisMessageProducer<?>) pubsubProducer)
                .send(channel, payload);
    }

    /**
     * 发布消息到频道（带 Headers）
     *
     * @param channel 频道名称
     * @param payload 消息载荷
     * @param headers 消息头
     * @param <T>     载荷类型
     * @return 发送结果
     */
    @SuppressWarnings("unchecked")
    public <T> SendResult publish(String channel, T payload, Map<String, String> headers) {
        return ((RedisMessageProducer<T>) (RedisMessageProducer<?>) pubsubProducer)
                .send(channel, payload, headers);
    }

    /**
     * 异步发布消息到频道
     *
     * @param channel 频道名称
     * @param payload 消息载荷
     * @param <T>     载荷类型
     * @return 发送结果 Future
     */
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<SendResult> publishAsync(String channel, T payload) {
        return ((RedisMessageProducer<T>) (RedisMessageProducer<?>) pubsubProducer)
                .sendAsync(channel, payload);
    }

    /**
     * 订阅频道（Pub/Sub 模式）
     *
     * @param channel 频道名称
     * @param handler 消息处理器
     * @param <T>     载荷类型
     */
    @SuppressWarnings("unchecked")
    public <T> void subscribe(String channel, MessageHandler<T> handler) {
        ((RedisMessageConsumer<T>) (RedisMessageConsumer<?>) pubsubConsumer)
                .subscribe(channel, handler);
    }

    /**
     * 订阅频道（简化版）
     *
     * @param channel  频道名称
     * @param consumer 消息消费函数
     * @param <T>      载荷类型
     */
    public <T> void subscribe(String channel, java.util.function.Consumer<Message<T>> consumer) {
        subscribe(channel, new MessageHandler<T>() {
            @Override
            public void handle(Message<T> message) {
                consumer.accept(message);
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<T> getMessageType() {
                return (Class<T>) Object.class;
            }
        });
    }

    /**
     * 模式订阅（支持通配符）
     *
     * @param pattern 模式（支持 * 和 ?）
     * @param handler 消息处理器
     * @param <T>     载荷类型
     */
    @SuppressWarnings("unchecked")
    public <T> void subscribePattern(String pattern, MessageHandler<T> handler) {
        ((RedisMessageConsumer<T>) (RedisMessageConsumer<?>) pubsubConsumer)
                .subscribePattern(pattern, handler);
    }

    /**
     * 取消订阅
     *
     * @param channel 频道名称
     */
    public void unsubscribe(String channel) {
        pubsubConsumer.unsubscribe(channel);
    }

    // ========== Stream 操作 ==========

    /**
     * 发送消息到 Stream（可靠模式）
     *
     * @param streamKey Stream 键名
     * @param payload   消息载荷
     * @param <T>       载荷类型
     * @return 发送结果
     */
    @SuppressWarnings("unchecked")
    public <T> SendResult send(String streamKey, T payload) {
        if (streamProducer == null) {
            throw new IllegalStateException("Stream 模式未启用，请配置 nebula.messaging.redis.stream.enabled=true");
        }
        return ((RedisStreamProducer<T>) (RedisStreamProducer<?>) streamProducer)
                .send(streamKey, payload);
    }

    /**
     * 发送消息到 Stream（带 Headers）
     *
     * @param streamKey Stream 键名
     * @param payload   消息载荷
     * @param headers   消息头
     * @param <T>       载荷类型
     * @return 发送结果
     */
    @SuppressWarnings("unchecked")
    public <T> SendResult send(String streamKey, T payload, Map<String, String> headers) {
        if (streamProducer == null) {
            throw new IllegalStateException("Stream 模式未启用，请配置 nebula.messaging.redis.stream.enabled=true");
        }
        return ((RedisStreamProducer<T>) (RedisStreamProducer<?>) streamProducer)
                .send(streamKey, payload, headers);
    }

    /**
     * 异步发送消息到 Stream
     *
     * @param streamKey Stream 键名
     * @param payload   消息载荷
     * @param <T>       载荷类型
     * @return 发送结果 Future
     */
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<SendResult> sendAsync(String streamKey, T payload) {
        if (streamProducer == null) {
            throw new IllegalStateException("Stream 模式未启用，请配置 nebula.messaging.redis.stream.enabled=true");
        }
        return ((RedisStreamProducer<T>) (RedisStreamProducer<?>) streamProducer)
                .sendAsync(streamKey, payload);
    }

    /**
     * 订阅 Stream（可靠模式）
     *
     * @param streamKey Stream 键名
     * @param handler   消息处理器
     * @param <T>       载荷类型
     */
    @SuppressWarnings("unchecked")
    public <T> void subscribeStream(String streamKey, MessageHandler<T> handler) {
        if (streamConsumer == null) {
            throw new IllegalStateException("Stream 模式未启用，请配置 nebula.messaging.redis.stream.enabled=true");
        }
        ((RedisStreamConsumer<T>) (RedisStreamConsumer<?>) streamConsumer)
                .subscribe(streamKey, handler);
    }

    /**
     * 订阅 Stream（简化版）
     *
     * @param streamKey Stream 键名
     * @param consumer  消息消费函数
     * @param <T>       载荷类型
     */
    public <T> void subscribeStream(String streamKey, java.util.function.Consumer<Message<T>> consumer) {
        subscribeStream(streamKey, new MessageHandler<T>() {
            @Override
            public void handle(Message<T> message) {
                consumer.accept(message);
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<T> getMessageType() {
                return (Class<T>) Object.class;
            }
        });
    }

    /**
     * 取消 Stream 订阅
     *
     * @param streamKey Stream 键名
     */
    public void unsubscribeStream(String streamKey) {
        if (streamConsumer != null) {
            streamConsumer.unsubscribe(streamKey);
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 创建消息对象
     *
     * @param topic   主题
     * @param payload 载荷
     * @param <T>     载荷类型
     * @return 消息对象
     */
    public <T> Message<T> createMessage(String topic, T payload) {
        return Message.<T>builder()
                .topic(topic)
                .payload(payload)
                .createTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建消息对象（带 Headers）
     *
     * @param topic   主题
     * @param payload 载荷
     * @param headers 消息头
     * @param <T>     载荷类型
     * @return 消息对象
     */
    public <T> Message<T> createMessage(String topic, T payload, Map<String, String> headers) {
        return Message.<T>builder()
                .topic(topic)
                .payload(payload)
                .headers(headers)
                .createTime(LocalDateTime.now())
                .build();
    }

    /**
     * 检查 Pub/Sub 是否可用
     *
     * @return 是否可用
     */
    public boolean isPubSubAvailable() {
        return pubsubProducer.isAvailable();
    }

    /**
     * 检查 Stream 是否启用
     *
     * @return 是否启用
     */
    public boolean isStreamEnabled() {
        return streamProducer != null && streamConsumer != null;
    }
}


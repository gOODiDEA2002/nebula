package io.nebula.messaging.redis.stream;

import io.nebula.messaging.core.consumer.MessageConsumer;
import io.nebula.messaging.core.consumer.MessageHandler;
import io.nebula.messaging.core.message.Message;
import io.nebula.messaging.redis.config.RedisMessagingProperties;
import io.nebula.messaging.redis.support.RedisMessageSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis Stream 消息消费者
 * <p>
 * 基于 Redis Stream 实现的可靠消息消费者。
 * 支持消费者组、消息确认、拉取模式等特性。
 * </p>
 */
@Slf4j
public class RedisStreamConsumer<T> implements MessageConsumer<T> {

    private final StringRedisTemplate redisTemplate;
    private final RedisMessagingProperties properties;
    private final RedisMessageSerializer serializer;
    private final StreamMessageListenerContainer<String, ObjectRecord<String, String>> listenerContainer;
    private final Executor executor;

    private final Map<String, StreamSubscriptionInfo> subscriptions = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final DefaultConsumerStats stats = new DefaultConsumerStats();
    private ConsumerConfig config = new DefaultConsumerConfig();

    private final String consumerGroup;
    private final String consumerName;

    public RedisStreamConsumer(StringRedisTemplate redisTemplate,
                               RedisMessagingProperties properties,
                               RedisMessageSerializer serializer,
                               StreamMessageListenerContainer<String, ObjectRecord<String, String>> listenerContainer,
                               Executor executor) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.serializer = serializer;
        this.listenerContainer = listenerContainer;
        this.executor = executor;
        this.consumerGroup = properties.getStream().getConsumerGroup();
        this.consumerName = properties.getStream().getConsumerNamePrefix() + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public void subscribe(String topic, MessageHandler<T> handler) {
        String streamKey = buildStreamKey(topic);
        log.info("订阅 Stream: {}, consumerGroup={}, consumerName={}", streamKey, consumerGroup, consumerName);

        // 确保消费者组存在
        ensureConsumerGroup(streamKey);

        // 创建 Stream 监听器
        StreamListener<String, ObjectRecord<String, String>> listener = createStreamListener(topic, handler);

        // 订阅 Stream
        Subscription subscription = listenerContainer.receive(
                Consumer.from(consumerGroup, consumerName),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
                listener
        );

        subscriptions.put(topic, new StreamSubscriptionInfo(topic, streamKey, subscription, handler));
        log.info("Stream 订阅成功: {}", streamKey);
    }

    @Override
    public void subscribe(String topic, String queue, MessageHandler<T> handler) {
        String combinedTopic = topic + ":" + queue;
        subscribe(combinedTopic, handler);
    }

    @Override
    public void subscribeWithTag(String topic, String tag, MessageHandler<T> handler) {
        // Stream 不原生支持标签过滤，订阅后在消费时过滤
        subscribe(topic, new MessageHandler<T>() {
            @Override
            public void handle(Message<T> message) {
                String msgTag = message.getHeaders().get("tag");
                if (tag.equals(msgTag) || "*".equals(tag)) {
                    handler.handle(message);
                }
            }

            @Override
            public Class<T> getMessageType() {
                return handler.getMessageType();
            }
        });
    }

    @Override
    public void unsubscribe(String topic) {
        StreamSubscriptionInfo info = subscriptions.remove(topic);
        if (info != null && info.subscription != null) {
            info.subscription.cancel();
            log.info("取消 Stream 订阅: {}", topic);
        }
    }

    @Override
    public void unsubscribe(String topic, String queue) {
        String combinedTopic = topic + ":" + queue;
        unsubscribe(combinedTopic);
    }

    @Override
    public List<Message<T>> pull(String topic, int maxCount, Duration timeout) {
        String streamKey = buildStreamKey(topic);
        List<Message<T>> messages = new ArrayList<>();

        try {
            // 确保消费者组存在
            ensureConsumerGroup(streamKey);

            // 拉取消息
            List<ObjectRecord<String, String>> records = redisTemplate.opsForStream().read(
                    String.class,
                    Consumer.from(consumerGroup, consumerName),
                    StreamReadOptions.empty()
                            .count(maxCount)
                            .block(timeout),
                    StreamOffset.create(streamKey, ReadOffset.lastConsumed())
            );

            if (records != null) {
                for (ObjectRecord<String, String> record : records) {
                    try {
                        Message<T> message = serializer.deserialize(record.getValue());
                        // 保存 Record ID 用于后续确认
                        message.getHeaders().put("_recordId", record.getId().getValue());
                        message.getHeaders().put("_streamKey", streamKey);
                        messages.add(message);
                    } catch (Exception e) {
                        log.error("消息反序列化失败: {}", e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("拉取 Stream 消息失败: topic={}, error={}", topic, e.getMessage(), e);
        }

        return messages;
    }

    @Override
    public Message<T> pullOne(String topic, Duration timeout) {
        List<Message<T>> messages = pull(topic, 1, timeout);
        return messages.isEmpty() ? null : messages.get(0);
    }

    @Override
    public CompletableFuture<List<Message<T>>> pullAsync(String topic, int maxCount, Duration timeout) {
        return CompletableFuture.supplyAsync(() -> pull(topic, maxCount, timeout), executor);
    }

    @Override
    public boolean ack(Message<T> message) {
        String recordId = message.getHeaders().get("_recordId");
        String streamKey = message.getHeaders().get("_streamKey");

        if (recordId == null || streamKey == null) {
            log.warn("无法确认消息，缺少 recordId 或 streamKey");
            return false;
        }

        try {
            Long acknowledged = redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, recordId);
            return acknowledged != null && acknowledged > 0;
        } catch (Exception e) {
            log.error("消息确认失败: recordId={}, error={}", recordId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean nack(Message<T> message, boolean requeue) {
        // Stream 不需要显式 nack，未确认的消息会保留在 pending 列表中
        // 可以通过 XCLAIM 命令重新分配
        if (requeue) {
            log.debug("Stream 消息将自动重新投递（未确认消息）");
        }
        return true;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            if (!listenerContainer.isRunning()) {
                listenerContainer.start();
            }
            log.info("Redis Stream 消息消费者已启动, consumerGroup={}, consumerName={}", consumerGroup, consumerName);
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            // 取消所有订阅
            subscriptions.values().forEach(info -> {
                if (info.subscription != null) {
                    info.subscription.cancel();
                }
            });
            subscriptions.clear();
            log.info("Redis Stream 消息消费者已停止");
        }
    }

    @Override
    public void pause() {
        paused.set(true);
        log.info("Redis Stream 消息消费者已暂停");
    }

    @Override
    public void resume() {
        paused.set(false);
        log.info("Redis Stream 消息消费者已恢复");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isPaused() {
        return paused.get();
    }

    @Override
    public void setConfig(ConsumerConfig config) {
        this.config = config;
    }

    @Override
    public ConsumerConfig getConfig() {
        return config;
    }

    @Override
    public ConsumerStats getStats() {
        return stats;
    }

    /**
     * 获取消费者名称
     */
    public String getConsumerName() {
        return consumerName;
    }

    /**
     * 获取消费者组名称
     */
    public String getConsumerGroup() {
        return consumerGroup;
    }

    /**
     * 确保消费者组存在
     */
    private void ensureConsumerGroup(String streamKey) {
        try {
            // 尝试创建消费者组
            redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from("0"), consumerGroup);
            log.debug("创建消费者组: streamKey={}, group={}", streamKey, consumerGroup);
        } catch (Exception e) {
            // 如果组已存在，忽略错误
            if (!e.getMessage().contains("BUSYGROUP")) {
                // Stream 不存在时，先创建 Stream
                if (e.getMessage().contains("no such key")) {
                    try {
                        // 创建空 Stream
                        redisTemplate.opsForStream().add(streamKey, Map.of("_init", "1"));
                        redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from("0"), consumerGroup);
                        log.debug("创建 Stream 和消费者组: streamKey={}, group={}", streamKey, consumerGroup);
                    } catch (Exception ex) {
                        if (!ex.getMessage().contains("BUSYGROUP")) {
                            log.warn("创建消费者组失败: {}", ex.getMessage());
                        }
                    }
                } else {
                    log.warn("创建消费者组失败: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 创建 Stream 监听器
     */
    private StreamListener<String, ObjectRecord<String, String>> createStreamListener(String topic, MessageHandler<T> handler) {
        return record -> {
            if (paused.get()) {
                log.debug("消费者已暂停，忽略消息");
                return;
            }

            stats.incrementProcessing();
            long startTime = System.currentTimeMillis();

            try {
                String json = record.getValue();
                Message<T> message = serializer.deserialize(json);
                message.getHeaders().put("_recordId", record.getId().getValue());
                message.getHeaders().put("_streamKey", record.getStream());

                log.debug("收到 Stream 消息: topic={}, messageId={}", topic, message.getId());
                handler.handle(message);

                // 自动确认
                if (config.isAutoAck()) {
                    redisTemplate.opsForStream().acknowledge(
                            record.getStream(), consumerGroup, record.getId()
                    );
                }

                long elapsed = System.currentTimeMillis() - startTime;
                stats.recordSuccess(elapsed);
            } catch (Exception e) {
                stats.recordFailure();
                log.error("Stream 消息处理失败: topic={}, error={}", topic, e.getMessage(), e);
            } finally {
                stats.decrementProcessing();
            }
        };
    }

    /**
     * 构建 Stream 键名
     */
    private String buildStreamKey(String topic) {
        String prefix = properties.getChannelPrefix();
        if (prefix != null && !prefix.isEmpty()) {
            return prefix + "stream:" + topic;
        }
        return "stream:" + topic;
    }

    // ========== 内部类 ==========

    private static class StreamSubscriptionInfo {
        final String topicName;
        final String streamKey;
        final Subscription subscription;
        final MessageHandler<?> handler;

        StreamSubscriptionInfo(String topicName, String streamKey, Subscription subscription, MessageHandler<?> handler) {
            this.topicName = topicName;
            this.streamKey = streamKey;
            this.subscription = subscription;
            this.handler = handler;
        }
    }

    private static class DefaultConsumerConfig implements ConsumerConfig {
        private String consumerGroup = "default";
        private int concurrency = 1;
        private int batchSize = 10;
        private Duration consumeTimeout = Duration.ofSeconds(30);
        private boolean autoAck = true;
        private int maxRetries = 3;

        @Override public String getConsumerGroup() { return consumerGroup; }
        @Override public void setConsumerGroup(String consumerGroup) { this.consumerGroup = consumerGroup; }
        @Override public int getConcurrency() { return concurrency; }
        @Override public void setConcurrency(int concurrency) { this.concurrency = concurrency; }
        @Override public int getBatchSize() { return batchSize; }
        @Override public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        @Override public Duration getConsumeTimeout() { return consumeTimeout; }
        @Override public void setConsumeTimeout(Duration timeout) { this.consumeTimeout = timeout; }
        @Override public boolean isAutoAck() { return autoAck; }
        @Override public void setAutoAck(boolean autoAck) { this.autoAck = autoAck; }
        @Override public int getMaxRetries() { return maxRetries; }
        @Override public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    }

    private static class DefaultConsumerStats implements ConsumerStats {
        private final AtomicLong consumedCount = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failedCount = new AtomicLong(0);
        private final AtomicLong totalElapsedTime = new AtomicLong(0);
        private final AtomicInteger processingCount = new AtomicInteger(0);
        private final long startTime = System.currentTimeMillis();

        void incrementProcessing() { processingCount.incrementAndGet(); }
        void decrementProcessing() { processingCount.decrementAndGet(); }
        void recordSuccess(long elapsedTime) {
            consumedCount.incrementAndGet();
            successCount.incrementAndGet();
            totalElapsedTime.addAndGet(elapsedTime);
        }
        void recordFailure() {
            consumedCount.incrementAndGet();
            failedCount.incrementAndGet();
        }

        @Override public long getConsumedCount() { return consumedCount.get(); }
        @Override public long getSuccessCount() { return successCount.get(); }
        @Override public long getFailedCount() { return failedCount.get(); }
        @Override public double getSuccessRate() {
            long total = consumedCount.get();
            return total == 0 ? 1.0 : (double) successCount.get() / total;
        }
        @Override public double getAverageElapsedTime() {
            long success = successCount.get();
            return success == 0 ? 0 : (double) totalElapsedTime.get() / success;
        }
        @Override public int getProcessingCount() { return processingCount.get(); }
        @Override public long getStartTime() { return startTime; }
        @Override public void reset() {
            consumedCount.set(0);
            successCount.set(0);
            failedCount.set(0);
            totalElapsedTime.set(0);
        }
    }
}


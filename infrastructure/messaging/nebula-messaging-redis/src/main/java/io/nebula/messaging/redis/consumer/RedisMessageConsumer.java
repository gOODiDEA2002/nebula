package io.nebula.messaging.redis.consumer;

import io.nebula.messaging.core.consumer.MessageConsumer;
import io.nebula.messaging.core.consumer.MessageHandler;
import io.nebula.messaging.core.message.Message;
import io.nebula.messaging.redis.config.RedisMessagingProperties;
import io.nebula.messaging.redis.support.RedisMessageSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis Pub/Sub 消息消费者
 * <p>
 * 基于 Redis Pub/Sub 实现的消息消费者。
 * 注意：Redis Pub/Sub 不支持消息确认、拉取模式等高级特性。
 * </p>
 */
@Slf4j
public class RedisMessageConsumer<T> implements MessageConsumer<T> {

    private final RedisMessageListenerContainer listenerContainer;
    private final RedisMessagingProperties properties;
    private final RedisMessageSerializer serializer;

    private final Map<String, SubscriptionInfo> subscriptions = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final DefaultConsumerStats stats = new DefaultConsumerStats();
    private ConsumerConfig config = new DefaultConsumerConfig();

    public RedisMessageConsumer(RedisMessageListenerContainer listenerContainer,
                                RedisMessagingProperties properties,
                                RedisMessageSerializer serializer) {
        this.listenerContainer = listenerContainer;
        this.properties = properties;
        this.serializer = serializer;
    }

    @Override
    public void subscribe(String topic, MessageHandler<T> handler) {
        String channel = buildChannel(topic);
        log.info("订阅频道: {}", channel);

        MessageListener listener = createMessageListener(topic, handler);
        Topic redisTopic = new ChannelTopic(channel);

        listenerContainer.addMessageListener(listener, redisTopic);
        subscriptions.put(topic, new SubscriptionInfo(topic, redisTopic, listener, handler));

        log.info("频道订阅成功: {}", channel);
    }

    @Override
    public void subscribe(String topic, String queue, MessageHandler<T> handler) {
        // Redis Pub/Sub 不支持队列概念，使用 topic:queue 作为频道名
        String combinedTopic = topic + ":" + queue;
        subscribe(combinedTopic, handler);
    }

    @Override
    public void subscribeWithTag(String topic, String tag, MessageHandler<T> handler) {
        // Redis Pub/Sub 不原生支持标签过滤，使用模式订阅模拟
        String pattern = topic + ":" + tag;
        subscribePattern(pattern, handler);
    }

    /**
     * 模式订阅（支持通配符）
     *
     * @param pattern 模式（支持 * 和 ? 通配符）
     * @param handler 消息处理器
     */
    public void subscribePattern(String pattern, MessageHandler<T> handler) {
        String channel = buildChannel(pattern);
        log.info("模式订阅: {}", channel);

        MessageListener listener = createMessageListener(pattern, handler);
        Topic redisTopic = new PatternTopic(channel);

        listenerContainer.addMessageListener(listener, redisTopic);
        subscriptions.put(pattern, new SubscriptionInfo(pattern, redisTopic, listener, handler));

        log.info("模式订阅成功: {}", channel);
    }

    @Override
    public void unsubscribe(String topic) {
        SubscriptionInfo info = subscriptions.remove(topic);
        if (info != null) {
            listenerContainer.removeMessageListener(info.listener, info.topic);
            log.info("取消订阅: {}", topic);
        }
    }

    @Override
    public void unsubscribe(String topic, String queue) {
        String combinedTopic = topic + ":" + queue;
        unsubscribe(combinedTopic);
    }

    @Override
    public List<Message<T>> pull(String topic, int maxCount, Duration timeout) {
        throw new UnsupportedOperationException("Redis Pub/Sub 不支持拉取模式，请使用 Redis Stream");
    }

    @Override
    public Message<T> pullOne(String topic, Duration timeout) {
        throw new UnsupportedOperationException("Redis Pub/Sub 不支持拉取模式，请使用 Redis Stream");
    }

    @Override
    public java.util.concurrent.CompletableFuture<List<Message<T>>> pullAsync(String topic, int maxCount, Duration timeout) {
        throw new UnsupportedOperationException("Redis Pub/Sub 不支持拉取模式，请使用 Redis Stream");
    }

    @Override
    public boolean ack(Message<T> message) {
        // Redis Pub/Sub 不需要确认
        log.debug("Redis Pub/Sub 不需要消息确认");
        return true;
    }

    @Override
    public boolean nack(Message<T> message, boolean requeue) {
        // Redis Pub/Sub 不支持 nack
        log.warn("Redis Pub/Sub 不支持消息拒绝/重新入队");
        return false;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            if (!listenerContainer.isRunning()) {
                listenerContainer.start();
            }
            log.info("Redis 消息消费者已启动");
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            // 不停止 listenerContainer，因为可能有其他消费者使用
            log.info("Redis 消息消费者已停止");
        }
    }

    @Override
    public void pause() {
        paused.set(true);
        log.info("Redis 消息消费者已暂停");
    }

    @Override
    public void resume() {
        paused.set(false);
        log.info("Redis 消息消费者已恢复");
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
     * 获取所有订阅
     *
     * @return 订阅信息映射
     */
    public Map<String, SubscriptionInfo> getSubscriptions() {
        return subscriptions;
    }

    /**
     * 创建消息监听器
     */
    private MessageListener createMessageListener(String topic, MessageHandler<T> handler) {
        return (message, pattern) -> {
            if (paused.get()) {
                log.debug("消费者已暂停，忽略消息");
                return;
            }

            stats.incrementProcessing();
            long startTime = System.currentTimeMillis();

            try {
                String json = new String(message.getBody());
                Message<T> msg = serializer.deserialize(json);

                log.debug("收到消息: topic={}, messageId={}", topic, msg.getId());
                handler.handle(msg);

                long elapsed = System.currentTimeMillis() - startTime;
                stats.recordSuccess(elapsed);
            } catch (Exception e) {
                stats.recordFailure();
                log.error("消息处理失败: topic={}, error={}", topic, e.getMessage(), e);
            } finally {
                stats.decrementProcessing();
            }
        };
    }

    /**
     * 构建 Redis 频道名称
     */
    private String buildChannel(String topic) {
        String prefix = properties.getChannelPrefix();
        if (prefix != null && !prefix.isEmpty()) {
            return prefix + topic;
        }
        return topic;
    }

    // ========== 内部类 ==========

    /**
     * 订阅信息
     */
    public static class SubscriptionInfo {
        final String topicName;
        final Topic topic;
        final MessageListener listener;
        final MessageHandler<?> handler;

        SubscriptionInfo(String topicName, Topic topic, MessageListener listener, MessageHandler<?> handler) {
            this.topicName = topicName;
            this.topic = topic;
            this.listener = listener;
            this.handler = handler;
        }
    }

    /**
     * 默认消费者配置
     */
    private static class DefaultConsumerConfig implements ConsumerConfig {
        private String consumerGroup = "default";
        private int concurrency = 1;
        private int batchSize = 1;
        private Duration consumeTimeout = Duration.ofSeconds(30);
        private boolean autoAck = true;
        private int maxRetries = 3;

        @Override
        public String getConsumerGroup() {
            return consumerGroup;
        }

        @Override
        public void setConsumerGroup(String consumerGroup) {
            this.consumerGroup = consumerGroup;
        }

        @Override
        public int getConcurrency() {
            return concurrency;
        }

        @Override
        public void setConcurrency(int concurrency) {
            this.concurrency = concurrency;
        }

        @Override
        public int getBatchSize() {
            return batchSize;
        }

        @Override
        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        @Override
        public Duration getConsumeTimeout() {
            return consumeTimeout;
        }

        @Override
        public void setConsumeTimeout(Duration timeout) {
            this.consumeTimeout = timeout;
        }

        @Override
        public boolean isAutoAck() {
            return autoAck;
        }

        @Override
        public void setAutoAck(boolean autoAck) {
            this.autoAck = autoAck;
        }

        @Override
        public int getMaxRetries() {
            return maxRetries;
        }

        @Override
        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }
    }

    /**
     * 默认消费者统计
     */
    private static class DefaultConsumerStats implements ConsumerStats {
        private final AtomicLong consumedCount = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failedCount = new AtomicLong(0);
        private final AtomicLong totalElapsedTime = new AtomicLong(0);
        private final AtomicInteger processingCount = new AtomicInteger(0);
        private final long startTime = System.currentTimeMillis();

        void incrementProcessing() {
            processingCount.incrementAndGet();
        }

        void decrementProcessing() {
            processingCount.decrementAndGet();
        }

        void recordSuccess(long elapsedTime) {
            consumedCount.incrementAndGet();
            successCount.incrementAndGet();
            totalElapsedTime.addAndGet(elapsedTime);
        }

        void recordFailure() {
            consumedCount.incrementAndGet();
            failedCount.incrementAndGet();
        }

        @Override
        public long getConsumedCount() {
            return consumedCount.get();
        }

        @Override
        public long getSuccessCount() {
            return successCount.get();
        }

        @Override
        public long getFailedCount() {
            return failedCount.get();
        }

        @Override
        public double getSuccessRate() {
            long total = consumedCount.get();
            return total == 0 ? 1.0 : (double) successCount.get() / total;
        }

        @Override
        public double getAverageElapsedTime() {
            long success = successCount.get();
            return success == 0 ? 0 : (double) totalElapsedTime.get() / success;
        }

        @Override
        public int getProcessingCount() {
            return processingCount.get();
        }

        @Override
        public long getStartTime() {
            return startTime;
        }

        @Override
        public void reset() {
            consumedCount.set(0);
            successCount.set(0);
            failedCount.set(0);
            totalElapsedTime.set(0);
        }
    }
}


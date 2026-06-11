package io.nebula.messaging.rocketmq.consumer;

import io.nebula.messaging.core.consumer.MessageConsumer;
import io.nebula.messaging.core.consumer.MessageHandler;
import io.nebula.messaging.core.exception.MessageReceiveException;
import io.nebula.messaging.core.message.Message;
import io.nebula.messaging.core.serializer.MessageSerializer;
import io.nebula.messaging.rocketmq.config.RocketMQProperties;
import org.apache.rocketmq.client.consumer.DefaultLitePullConsumer;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RocketMQ 消息消费者实现
 *
 * <p>实现说明：</p>
 * <ul>
 *   <li>每个订阅创建独立的 DefaultMQPushConsumer，消费组名为 {基础组名}-{topic}[-{tag}]，
 *       避免 RocketMQ 同组订阅关系不一致导致消息丢失</li>
 *   <li>queue 参数映射为 Tag 过滤（queue 为空或与 topic 同名时按 "*" 订阅全部消息）</li>
 *   <li>ack/nack 由 Push 模式监听器返回值驱动：处理器正常返回即 ACK，抛异常即 RECONSUME_LATER，
 *       重试超过 maxReconsumeTimes 后进入死信队列</li>
 *   <li>pull 系列方法基于 DefaultLitePullConsumer 实现，使用独立消费组，不影响 Push 订阅位点</li>
 * </ul>
 *
 * @author nebula
 */
public class RocketMQMessageConsumer<T> implements MessageConsumer<T> {

    private static final Logger logger = LoggerFactory.getLogger(RocketMQMessageConsumer.class);

    private final MessageSerializer messageSerializer;
    private final RocketMQProperties properties;

    /**
     * 订阅表：订阅键（topic 或 topic-tag） → Push 消费者实例
     */
    private final Map<String, DefaultMQPushConsumer> subscriptions = new ConcurrentHashMap<>();

    private volatile boolean running = false;
    private volatile boolean paused = false;
    private ConsumerConfig config;

    private final RocketMQConsumerStats stats = new RocketMQConsumerStats();

    public RocketMQMessageConsumer(MessageSerializer messageSerializer, RocketMQProperties properties) {
        this.messageSerializer = messageSerializer;
        this.properties = properties;
        this.config = new RocketMQConsumerConfig(properties);
        this.running = true;
    }

    @Override
    public void subscribe(String topic, MessageHandler<T> handler) {
        doSubscribe(topic, "*", handler);
    }

    @Override
    public void subscribe(String topic, String queue, MessageHandler<T> handler) {
        // queue 为空或与 topic 同名（注解默认值）时订阅全部消息，否则映射为 Tag 过滤
        String tag = (queue == null || queue.isEmpty() || queue.equals(topic)) ? "*" : queue;
        doSubscribe(topic, tag, handler);
    }

    @Override
    public void subscribeWithTag(String topic, String tag, MessageHandler<T> handler) {
        doSubscribe(topic, tag, handler);
    }

    @Override
    public void unsubscribe(String topic) {
        // 移除该 topic 下的全部订阅（含带 tag 的订阅）
        subscriptions.entrySet().removeIf(entry -> {
            if (entry.getKey().equals(topic) || entry.getKey().startsWith(topic + "|")) {
                entry.getValue().shutdown();
                logger.info("取消订阅: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }

    @Override
    public void unsubscribe(String topic, String queue) {
        String tag = (queue == null || queue.isEmpty() || queue.equals(topic)) ? "*" : queue;
        DefaultMQPushConsumer consumer = subscriptions.remove(subscriptionKey(topic, tag));
        if (consumer != null) {
            consumer.shutdown();
            logger.info("取消订阅: topic={}, tag={}", topic, tag);
        }
    }

    @Override
    public List<Message<T>> pull(String topic, int maxCount, Duration timeout) {
        DefaultLitePullConsumer pullConsumer = null;
        try {
            pullConsumer = createLitePullConsumer(topic);
            pullConsumer.start();

            List<Message<T>> messages = new ArrayList<>(maxCount);
            long deadline = System.currentTimeMillis() + timeout.toMillis();
            while (messages.size() < maxCount && System.currentTimeMillis() < deadline) {
                long remaining = Math.max(deadline - System.currentTimeMillis(), 1);
                List<MessageExt> batch = pullConsumer.poll(remaining);
                if (batch.isEmpty()) {
                    break;
                }
                for (MessageExt ext : batch) {
                    if (messages.size() >= maxCount) {
                        break;
                    }
                    messages.add(convertMessage(ext, null));
                }
            }
            return messages;
        } catch (Exception e) {
            throw new MessageReceiveException("拉取消息失败: topic=" + topic, e);
        } finally {
            if (pullConsumer != null) {
                pullConsumer.shutdown();
            }
        }
    }

    @Override
    public Message<T> pullOne(String topic, Duration timeout) {
        List<Message<T>> messages = pull(topic, 1, timeout);
        return messages.isEmpty() ? null : messages.get(0);
    }

    @Override
    public CompletableFuture<List<Message<T>>> pullAsync(String topic, int maxCount, Duration timeout) {
        return CompletableFuture.supplyAsync(() -> pull(topic, maxCount, timeout));
    }

    @Override
    public boolean ack(Message<T> message) {
        // Push 模式下由监听器返回 CONSUME_SUCCESS 自动确认，此处无需额外操作
        logger.debug("RocketMQ Push 模式自动确认消息: {}", message.getId());
        return true;
    }

    @Override
    public boolean nack(Message<T> message, boolean requeue) {
        // Push 模式下由监听器抛出异常触发 RECONSUME_LATER 重新投递
        logger.debug("RocketMQ Push 模式通过监听器异常触发重投: {}, requeue={}", message.getId(), requeue);
        return true;
    }

    @Override
    public void start() {
        running = true;
        subscriptions.values().forEach(consumer -> {
            try {
                consumer.resume();
            } catch (Exception e) {
                logger.warn("恢复消费者失败", e);
            }
        });
    }

    @Override
    public void stop() {
        running = false;
        subscriptions.values().forEach(DefaultMQPushConsumer::shutdown);
        subscriptions.clear();
    }

    @Override
    public void pause() {
        paused = true;
        subscriptions.values().forEach(DefaultMQPushConsumer::suspend);
        logger.info("RocketMQ 消费者已暂停");
    }

    @Override
    public void resume() {
        paused = false;
        subscriptions.values().forEach(DefaultMQPushConsumer::resume);
        logger.info("RocketMQ 消费者已恢复");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isPaused() {
        return paused;
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

    // ==================== 内部实现 ====================

    /**
     * 执行订阅：为每个 topic+tag 创建独立 Push 消费者
     */
    private void doSubscribe(String topic, String tag, MessageHandler<T> handler) {
        String key = subscriptionKey(topic, tag);
        if (subscriptions.containsKey(key)) {
            logger.warn("重复订阅，忽略: topic={}, tag={}", topic, tag);
            return;
        }

        try {
            DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroup(topic, tag));
            consumer.setNamesrvAddr(properties.getNameServer());
            consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
            consumer.setMessageModel(properties.getConsumer().isBroadcasting()
                    ? MessageModel.BROADCASTING : MessageModel.CLUSTERING);
            consumer.setConsumeThreadMin(properties.getConsumer().getConsumeThreadMin());
            consumer.setConsumeThreadMax(properties.getConsumer().getConsumeThreadMax());
            consumer.setMaxReconsumeTimes(properties.getConsumer().getMaxReconsumeTimes());
            consumer.setConsumeTimeout(properties.getConsumer().getConsumeTimeoutMinutes());
            consumer.setConsumeMessageBatchMaxSize(properties.getConsumer().getConsumeMessageBatchMaxSize());
            consumer.subscribe(topic, tag);
            consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
                if (paused) {
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
                for (MessageExt ext : msgs) {
                    if (!handleMessage(ext, handler)) {
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
            consumer.start();

            subscriptions.put(key, consumer);
            logger.info("订阅成功: topic={}, tag={}, group={}", topic, tag, consumer.getConsumerGroup());
        } catch (Exception e) {
            throw new MessageReceiveException("订阅失败: topic=" + topic + ", tag=" + tag, e);
        }
    }

    /**
     * 处理单条消息：反序列化后交给处理器
     *
     * @return 是否处理成功
     */
    private boolean handleMessage(MessageExt ext, MessageHandler<T> handler) {
        long startTime = System.currentTimeMillis();
        stats.processingCount.incrementAndGet();
        try {
            Message<T> message = convertMessage(ext, handler.getMessageType());
            handler.handle(message);
            stats.record(true, System.currentTimeMillis() - startTime);
            return true;
        } catch (Exception e) {
            stats.record(false, System.currentTimeMillis() - startTime);
            logger.error("消息处理失败，等待重投: topic={}, msgId={}, reconsumeTimes={}",
                    ext.getTopic(), ext.getMsgId(), ext.getReconsumeTimes(), e);
            return false;
        } finally {
            stats.processingCount.decrementAndGet();
        }
    }

    /**
     * RocketMQ 原生消息转换为统一消息对象
     */
    @SuppressWarnings("unchecked")
    private Message<T> convertMessage(MessageExt ext, Class<T> payloadType) throws Exception {
        T payload;
        if (payloadType == null || payloadType == Object.class) {
            payload = (T) messageSerializer.deserialize(ext.getBody(), Map.class);
        } else {
            payload = messageSerializer.deserialize(ext.getBody(), payloadType);
        }

        Map<String, String> headers = new HashMap<>();
        if (ext.getProperties() != null) {
            headers.putAll(ext.getProperties());
        }

        return Message.<T>builder()
                .id(ext.getMsgId())
                .topic(ext.getTopic())
                .tag(ext.getTags())
                .payload(payload)
                .headers(headers)
                .retryCount(ext.getReconsumeTimes())
                .createTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建一次性 Lite Pull 消费者（独立消费组，不影响 Push 订阅位点）
     */
    private DefaultLitePullConsumer createLitePullConsumer(String topic) throws Exception {
        DefaultLitePullConsumer pullConsumer =
                new DefaultLitePullConsumer(properties.getConsumer().getGroup() + "-pull");
        pullConsumer.setNamesrvAddr(properties.getNameServer());
        pullConsumer.setAutoCommit(true);
        pullConsumer.subscribe(topic, "*");
        return pullConsumer;
    }

    private String subscriptionKey(String topic, String tag) {
        return "*".equals(tag) ? topic : topic + "|" + tag;
    }

    /**
     * 消费组命名：基础组名-topic[-tag]，保证每个订阅关系独立
     */
    private String consumerGroup(String topic, String tag) {
        String base = properties.getConsumer().getGroup() + "-" + topic;
        return "*".equals(tag) ? base : base + "-" + tag;
    }

    /**
     * 消费者配置实现
     */
    private static class RocketMQConsumerConfig implements ConsumerConfig {

        private String consumerGroup;
        private int concurrency;
        private int batchSize;
        private Duration consumeTimeout;
        private boolean autoAck = true;
        private int maxRetries;

        RocketMQConsumerConfig(RocketMQProperties properties) {
            this.consumerGroup = properties.getConsumer().getGroup();
            this.concurrency = properties.getConsumer().getConsumeThreadMax();
            this.batchSize = properties.getConsumer().getConsumeMessageBatchMaxSize();
            this.consumeTimeout = Duration.ofMinutes(properties.getConsumer().getConsumeTimeoutMinutes());
            this.maxRetries = properties.getConsumer().getMaxReconsumeTimes();
        }

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
     * 消费者统计信息实现
     */
    private static class RocketMQConsumerStats implements ConsumerStats {

        private final AtomicLong consumedCount = new AtomicLong();
        private final AtomicLong successCount = new AtomicLong();
        private final AtomicLong failedCount = new AtomicLong();
        private final AtomicLong totalElapsedTime = new AtomicLong();
        private final AtomicInteger processingCount = new AtomicInteger();
        private volatile long startTime = System.currentTimeMillis();

        void record(boolean success, long elapsed) {
            consumedCount.incrementAndGet();
            if (success) {
                successCount.incrementAndGet();
            } else {
                failedCount.incrementAndGet();
            }
            totalElapsedTime.addAndGet(elapsed);
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
            return total == 0 ? 0.0 : (double) successCount.get() / total;
        }

        @Override
        public double getAverageElapsedTime() {
            long total = consumedCount.get();
            return total == 0 ? 0.0 : (double) totalElapsedTime.get() / total;
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
            startTime = System.currentTimeMillis();
        }
    }
}

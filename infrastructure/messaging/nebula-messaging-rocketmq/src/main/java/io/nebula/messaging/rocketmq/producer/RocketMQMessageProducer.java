package io.nebula.messaging.rocketmq.producer;

import io.nebula.messaging.core.message.Message;
import io.nebula.messaging.core.producer.MessageProducer;
import io.nebula.messaging.core.serializer.MessageSerializer;
import io.nebula.messaging.rocketmq.config.RocketMQProperties;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RocketMQ 消息生产者实现
 *
 * <p>核心语义映射（messaging-core 抽象 → RocketMQ）：</p>
 * <ul>
 *   <li>topic → Topic</li>
 *   <li>queue/tag → Tag（tag 优先；queue 与 topic 同名时视为默认订阅，不设置 Tag）</li>
 *   <li>延迟消息 → 延迟等级（RocketMQ 4.x 固定 18 级，向上取最近等级）</li>
 *   <li>顺序消息 → MessageQueueSelector 按 shardKey 哈希选队列</li>
 *   <li>广播消息 → 由消费端 MessageModel.BROADCASTING 决定，生产端等同普通发送</li>
 * </ul>
 *
 * @author nebula
 */
public class RocketMQMessageProducer<T> implements MessageProducer<T> {

    private static final Logger logger = LoggerFactory.getLogger(RocketMQMessageProducer.class);

    /**
     * RocketMQ 4.x 默认延迟等级对应的秒数（messageDelayLevel: 1s 5s 10s 30s 1m 2m ... 2h）
     */
    private static final long[] DELAY_LEVEL_SECONDS = {
            1, 5, 10, 30, 60, 120, 180, 240, 300, 360,
            420, 480, 540, 600, 1200, 1800, 3600, 7200
    };

    private final DefaultMQProducer producer;
    private final MessageSerializer messageSerializer;
    private final RocketMQProperties properties;

    /**
     * 事务生产者（懒加载，仅在发送事务消息时创建）
     */
    private volatile TransactionMQProducer transactionProducer;

    /**
     * 事务回调登记表：transactionId → 本地事务执行结果（供回查使用）
     */
    private final ConcurrentHashMap<String, LocalTransactionState> transactionStates = new ConcurrentHashMap<>();

    private Duration timeout = Duration.ofSeconds(10);
    private volatile boolean started = false;

    private final RocketMQProducerStats stats = new RocketMQProducerStats();

    public RocketMQMessageProducer(DefaultMQProducer producer,
                                   MessageSerializer messageSerializer,
                                   RocketMQProperties properties) {
        this.producer = producer;
        this.messageSerializer = messageSerializer;
        this.properties = properties;
        this.timeout = Duration.ofMillis(properties.getProducer().getSendTimeout());
        this.started = true;
    }

    @Override
    public SendResult send(Message<T> message) {
        String tag = resolveTag(message.getTag(), message.getQueue(), message.getTopic());
        return doSend(message.getTopic(), tag, message.getPayload(), message.getHeaders());
    }

    @Override
    public SendResult send(String topic, T payload) {
        return doSend(topic, null, payload, null);
    }

    @Override
    public SendResult send(String topic, String queue, T payload) {
        return doSend(topic, resolveTag(null, queue, topic), payload, null);
    }

    @Override
    public SendResult send(String topic, T payload, Map<String, String> headers) {
        return doSend(topic, null, payload, headers);
    }

    @Override
    public SendResult send(String topic, String queue, T payload, Map<String, String> headers) {
        return doSend(topic, resolveTag(null, queue, topic), payload, headers);
    }

    @Override
    public CompletableFuture<SendResult> sendAsync(Message<T> message) {
        return CompletableFuture.supplyAsync(() -> send(message));
    }

    @Override
    public CompletableFuture<SendResult> sendAsync(String topic, T payload) {
        return CompletableFuture.supplyAsync(() -> send(topic, payload));
    }

    @Override
    public CompletableFuture<SendResult> sendAsync(String topic, String queue, T payload) {
        return CompletableFuture.supplyAsync(() -> send(topic, queue, payload));
    }

    @Override
    public SendResult sendDelayMessage(String topic, T payload, Duration delay) {
        return sendDelayMessage(topic, null, payload, delay);
    }

    @Override
    public SendResult sendDelayMessage(String topic, String queue, T payload, Duration delay) {
        long startTime = System.currentTimeMillis();
        String tag = resolveTag(null, queue, topic);
        try {
            org.apache.rocketmq.common.message.Message mqMessage = buildMessage(topic, tag, payload, null);
            int delayLevel = mapDelayLevel(delay);
            mqMessage.setDelayTimeLevel(delayLevel);

            org.apache.rocketmq.client.producer.SendResult result =
                    producer.send(mqMessage, timeout.toMillis());

            long elapsed = System.currentTimeMillis() - startTime;
            stats.record(true, elapsed);
            logger.debug("延迟消息发送成功: topic={}, tag={}, delayLevel={}, msgId={}",
                    topic, tag, delayLevel, result.getMsgId());
            return new RocketMQSendResult(true, result.getMsgId(), topic, tag, startTime, elapsed, null, null);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            stats.record(false, elapsed);
            logger.error("延迟消息发送失败: topic={}, tag={}", topic, tag, e);
            return new RocketMQSendResult(false, null, topic, tag, startTime, elapsed,
                    "延迟消息发送失败: " + e.getMessage(), e);
        }
    }

    @Override
    public SendResult sendOrderedMessage(String topic, T payload, String shardKey) {
        return sendOrderedMessage(topic, null, payload, shardKey);
    }

    @Override
    public SendResult sendOrderedMessage(String topic, String queue, T payload, String shardKey) {
        long startTime = System.currentTimeMillis();
        String tag = resolveTag(null, queue, topic);
        try {
            org.apache.rocketmq.common.message.Message mqMessage = buildMessage(topic, tag, payload, null);

            // 按分片键哈希选择固定队列，保证同一分片键的消息严格有序
            org.apache.rocketmq.client.producer.SendResult result = producer.send(mqMessage,
                    (mqs, msg, arg) -> {
                        int index = Math.abs(arg.hashCode() % mqs.size());
                        return mqs.get(index);
                    }, shardKey, timeout.toMillis());

            long elapsed = System.currentTimeMillis() - startTime;
            stats.record(true, elapsed);
            return new RocketMQSendResult(true, result.getMsgId(), topic, tag, startTime, elapsed, null, null);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            stats.record(false, elapsed);
            logger.error("顺序消息发送失败: topic={}, shardKey={}", topic, shardKey, e);
            return new RocketMQSendResult(false, null, topic, tag, startTime, elapsed,
                    "顺序消息发送失败: " + e.getMessage(), e);
        }
    }

    @Override
    public SendResult sendTransactionMessage(String topic, T payload, TransactionCallback callback) {
        long startTime = System.currentTimeMillis();
        try {
            TransactionMQProducer txProducer = getOrCreateTransactionProducer();
            org.apache.rocketmq.common.message.Message mqMessage = buildMessage(topic, null, payload, null);

            Message<T> coreMessage = Message.of(topic, payload);
            org.apache.rocketmq.client.producer.TransactionSendResult result =
                    txProducer.sendMessageInTransaction(mqMessage, new TransactionInvocation(callback, coreMessage));

            boolean success = result.getLocalTransactionState() == LocalTransactionState.COMMIT_MESSAGE;
            long elapsed = System.currentTimeMillis() - startTime;
            stats.record(success, elapsed);
            return new RocketMQSendResult(success, result.getMsgId(), topic, null, startTime, elapsed,
                    success ? null : "本地事务未提交: " + result.getLocalTransactionState(), null);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            stats.record(false, elapsed);
            logger.error("事务消息发送失败: topic={}", topic, e);
            return new RocketMQSendResult(false, null, topic, null, startTime, elapsed,
                    "事务消息发送失败: " + e.getMessage(), e);
        }
    }

    @Override
    public BatchSendResult sendBatch(List<Message<T>> messages) {
        long startTime = System.currentTimeMillis();
        List<SendResult> results = new ArrayList<>(messages.size());
        for (Message<T> message : messages) {
            results.add(send(message));
        }
        return new RocketMQBatchSendResult(results, System.currentTimeMillis() - startTime);
    }

    @Override
    public BatchSendResult sendBatch(String topic, List<T> payloads) {
        long startTime = System.currentTimeMillis();
        List<SendResult> results = new ArrayList<>(payloads.size());
        try {
            // RocketMQ 原生批量发送要求同一 Topic，且总大小不超过 4MB
            List<org.apache.rocketmq.common.message.Message> mqMessages = new ArrayList<>(payloads.size());
            for (T payload : payloads) {
                mqMessages.add(buildMessage(topic, null, payload, null));
            }
            org.apache.rocketmq.client.producer.SendResult result = producer.send(mqMessages, timeout.toMillis());

            long elapsed = System.currentTimeMillis() - startTime;
            for (int i = 0; i < payloads.size(); i++) {
                stats.record(true, elapsed / payloads.size());
                results.add(new RocketMQSendResult(true, result.getMsgId(), topic, null, startTime, elapsed, null, null));
            }
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            logger.error("批量消息发送失败: topic={}, count={}", topic, payloads.size(), e);
            for (int i = 0; i < payloads.size(); i++) {
                stats.record(false, elapsed / Math.max(payloads.size(), 1));
                results.add(new RocketMQSendResult(false, null, topic, null, startTime, elapsed,
                        "批量消息发送失败: " + e.getMessage(), e));
            }
        }
        return new RocketMQBatchSendResult(results, System.currentTimeMillis() - startTime);
    }

    @Override
    public CompletableFuture<BatchSendResult> sendBatchAsync(List<Message<T>> messages) {
        return CompletableFuture.supplyAsync(() -> sendBatch(messages));
    }

    @Override
    public SendResult sendBroadcast(String topic, T payload) {
        // RocketMQ 广播由消费端 MessageModel.BROADCASTING 决定，生产端等同普通发送
        return send(topic, payload);
    }

    @Override
    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    @Override
    public Duration getTimeout() {
        return timeout;
    }

    @Override
    public boolean isAvailable() {
        return started;
    }

    @Override
    public void start() {
        // DefaultMQProducer 由自动配置负责 start，这里仅维护状态位
        started = true;
    }

    @Override
    public void stop() {
        started = false;
        if (transactionProducer != null) {
            transactionProducer.shutdown();
            transactionProducer = null;
        }
    }

    @Override
    public ProducerStats getStats() {
        return stats;
    }

    // ==================== 内部实现 ====================

    /**
     * 实际发送逻辑
     */
    private SendResult doSend(String topic, String tag, T payload, Map<String, String> headers) {
        long startTime = System.currentTimeMillis();
        try {
            org.apache.rocketmq.common.message.Message mqMessage = buildMessage(topic, tag, payload, headers);
            org.apache.rocketmq.client.producer.SendResult result = producer.send(mqMessage, timeout.toMillis());

            long elapsed = System.currentTimeMillis() - startTime;
            stats.record(true, elapsed);
            logger.debug("消息发送成功: topic={}, tag={}, msgId={}, elapsed={}ms",
                    topic, tag, result.getMsgId(), elapsed);
            return new RocketMQSendResult(true, result.getMsgId(), topic, tag, startTime, elapsed, null, null);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            stats.record(false, elapsed);
            logger.error("消息发送失败: topic={}, tag={}", topic, tag, e);
            return new RocketMQSendResult(false, null, topic, tag, startTime, elapsed,
                    "消息发送失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建 RocketMQ 原生消息
     */
    private org.apache.rocketmq.common.message.Message buildMessage(
            String topic, String tag, T payload, Map<String, String> headers) throws Exception {
        byte[] body = messageSerializer.serialize(payload);
        org.apache.rocketmq.common.message.Message mqMessage =
                new org.apache.rocketmq.common.message.Message(topic, tag, body);
        mqMessage.setKeys(UUID.randomUUID().toString().replace("-", ""));
        if (headers != null) {
            headers.forEach(mqMessage::putUserProperty);
        }
        return mqMessage;
    }

    /**
     * queue → tag 语义映射：queue 为空或与 topic 同名（注解默认值）时不设置 Tag
     */
    private String resolveTag(String tag, String queue, String topic) {
        if (tag != null && !tag.isEmpty()) {
            return tag;
        }
        if (queue == null || queue.isEmpty() || queue.equals(topic)) {
            return null;
        }
        return queue;
    }

    /**
     * 将延迟时长映射为 RocketMQ 4.x 延迟等级（向上取最近等级，超过 2h 取最大等级 18）
     */
    static int mapDelayLevel(Duration delay) {
        long seconds = Math.max(delay.getSeconds(), 1);
        for (int i = 0; i < DELAY_LEVEL_SECONDS.length; i++) {
            if (seconds <= DELAY_LEVEL_SECONDS[i]) {
                return i + 1;
            }
        }
        return DELAY_LEVEL_SECONDS.length;
    }

    /**
     * 懒加载事务生产者
     */
    private TransactionMQProducer getOrCreateTransactionProducer() throws Exception {
        if (transactionProducer == null) {
            synchronized (this) {
                if (transactionProducer == null) {
                    TransactionMQProducer txProducer =
                            new TransactionMQProducer(properties.getProducer().getGroup() + "-tx");
                    txProducer.setNamesrvAddr(properties.getNameServer());
                    txProducer.setSendMsgTimeout(properties.getProducer().getSendTimeout());
                    txProducer.setTransactionListener(new DelegatingTransactionListener());
                    txProducer.start();
                    transactionProducer = txProducer;
                }
            }
        }
        return transactionProducer;
    }

    /**
     * 事务调用上下文：携带回调与核心消息对象
     */
    private record TransactionInvocation(TransactionCallback callback, Message<?> message) {
    }

    /**
     * 事务监听器：执行阶段委托给调用方回调，回查阶段返回登记的执行结果
     */
    private class DelegatingTransactionListener implements TransactionListener {

        @Override
        public LocalTransactionState executeLocalTransaction(
                org.apache.rocketmq.common.message.Message msg, Object arg) {
            TransactionInvocation invocation = (TransactionInvocation) arg;
            LocalTransactionState state;
            try {
                TransactionResult result = invocation.callback().executeLocalTransaction(invocation.message());
                state = switch (result) {
                    case COMMIT -> LocalTransactionState.COMMIT_MESSAGE;
                    case ROLLBACK -> LocalTransactionState.ROLLBACK_MESSAGE;
                    case UNKNOWN -> LocalTransactionState.UNKNOW;
                };
            } catch (Exception e) {
                logger.error("本地事务执行异常，回滚事务消息: topic={}", msg.getTopic(), e);
                state = LocalTransactionState.ROLLBACK_MESSAGE;
            }
            if (msg.getTransactionId() != null) {
                transactionStates.put(msg.getTransactionId(), state);
            }
            return state;
        }

        @Override
        public LocalTransactionState checkLocalTransaction(MessageExt msg) {
            LocalTransactionState state = transactionStates.get(msg.getTransactionId());
            return state != null ? state : LocalTransactionState.UNKNOW;
        }
    }

    /**
     * 发送结果实现
     */
    private record RocketMQSendResult(boolean success, String messageId, String topic, String queue,
                                      long timestamp, long elapsedTime,
                                      String errorMessage, Throwable exception) implements SendResult {

        @Override
        public boolean isSuccess() {
            return success;
        }

        @Override
        public String getMessageId() {
            return messageId;
        }

        @Override
        public String getTopic() {
            return topic;
        }

        @Override
        public String getQueue() {
            return queue;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public Throwable getException() {
            return exception;
        }

        @Override
        public long getElapsedTime() {
            return elapsedTime;
        }
    }

    /**
     * 批量发送结果实现
     */
    private record RocketMQBatchSendResult(List<SendResult> results, long elapsedTime) implements BatchSendResult {

        @Override
        public boolean isAllSuccess() {
            return results.stream().allMatch(SendResult::isSuccess);
        }

        @Override
        public int getSuccessCount() {
            return (int) results.stream().filter(SendResult::isSuccess).count();
        }

        @Override
        public int getFailedCount() {
            return getTotalCount() - getSuccessCount();
        }

        @Override
        public int getTotalCount() {
            return results.size();
        }

        @Override
        public List<SendResult> getResults() {
            return results;
        }

        @Override
        public List<SendResult> getFailedResults() {
            return results.stream().filter(r -> !r.isSuccess()).toList();
        }

        @Override
        public long getElapsedTime() {
            return elapsedTime;
        }
    }

    /**
     * 生产者统计信息实现
     */
    private static class RocketMQProducerStats implements ProducerStats {

        private final AtomicLong sentCount = new AtomicLong();
        private final AtomicLong successCount = new AtomicLong();
        private final AtomicLong failedCount = new AtomicLong();
        private final AtomicLong totalElapsedTime = new AtomicLong();
        private volatile long startTime = System.currentTimeMillis();

        void record(boolean success, long elapsed) {
            sentCount.incrementAndGet();
            if (success) {
                successCount.incrementAndGet();
            } else {
                failedCount.incrementAndGet();
            }
            totalElapsedTime.addAndGet(elapsed);
        }

        @Override
        public long getSentCount() {
            return sentCount.get();
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
            long total = sentCount.get();
            return total == 0 ? 0.0 : (double) successCount.get() / total;
        }

        @Override
        public double getAverageElapsedTime() {
            long total = sentCount.get();
            return total == 0 ? 0.0 : (double) totalElapsedTime.get() / total;
        }

        @Override
        public long getStartTime() {
            return startTime;
        }

        @Override
        public void reset() {
            sentCount.set(0);
            successCount.set(0);
            failedCount.set(0);
            totalElapsedTime.set(0);
            startTime = System.currentTimeMillis();
        }
    }
}

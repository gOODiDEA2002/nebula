package io.nebula.messaging.redis.stream;

import io.nebula.messaging.core.message.Message;
import io.nebula.messaging.core.producer.MessageProducer;
import io.nebula.messaging.redis.config.RedisMessagingProperties;
import io.nebula.messaging.redis.support.RedisMessageSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis Stream 消息生产者
 * <p>
 * 基于 Redis Stream 实现的可靠消息生产者。
 * 支持消息持久化、消费者组、消息确认等特性。
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class RedisStreamProducer<T> implements MessageProducer<T> {

    private final StringRedisTemplate redisTemplate;
    private final RedisMessagingProperties properties;
    private final RedisMessageSerializer serializer;

    private Duration timeout = Duration.ofSeconds(5);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final DefaultProducerStats stats = new DefaultProducerStats();

    @Override
    public SendResult send(Message<T> message) {
        long startTime = System.currentTimeMillis();
        try {
            String streamKey = buildStreamKey(message.getTopic());
            String messageId = generateMessageId();
            message.setId(messageId);
            message.setSendTime(LocalDateTime.now());

            // 序列化消息
            String json = serializer.serialize(message);

            // 创建 Stream Record
            ObjectRecord<String, String> record = StreamRecords.newRecord()
                    .in(streamKey)
                    .ofObject(json);

            // 发送到 Stream
            RecordId recordId = redisTemplate.opsForStream().add(record);

            // 如果设置了最大长度，进行裁剪
            long maxLen = properties.getStream().getMaxLen();
            if (maxLen > 0) {
                redisTemplate.opsForStream().trim(streamKey, maxLen);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            stats.recordSuccess(elapsed);

            String fullMessageId = recordId != null ? recordId.getValue() : messageId;
            log.debug("Stream 消息发送成功: streamKey={}, messageId={}", streamKey, fullMessageId);
            
            return new DefaultSendResult(true, fullMessageId, message.getTopic(), message.getQueue(), elapsed, null, null);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            stats.recordFailure();
            log.error("Stream 消息发送失败: topic={}, error={}", message.getTopic(), e.getMessage(), e);
            return new DefaultSendResult(false, null, message.getTopic(), message.getQueue(), elapsed, e.getMessage(), e);
        }
    }

    @Override
    public SendResult send(String topic, T payload) {
        Message<T> message = Message.<T>builder()
                .topic(topic)
                .payload(payload)
                .createTime(LocalDateTime.now())
                .build();
        return send(message);
    }

    @Override
    public SendResult send(String topic, String queue, T payload) {
        Message<T> message = Message.<T>builder()
                .topic(topic)
                .queue(queue)
                .payload(payload)
                .createTime(LocalDateTime.now())
                .build();
        return send(message);
    }

    @Override
    public SendResult send(String topic, T payload, Map<String, String> headers) {
        Message<T> message = Message.<T>builder()
                .topic(topic)
                .payload(payload)
                .headers(headers)
                .createTime(LocalDateTime.now())
                .build();
        return send(message);
    }

    @Override
    public SendResult send(String topic, String queue, T payload, Map<String, String> headers) {
        Message<T> message = Message.<T>builder()
                .topic(topic)
                .queue(queue)
                .payload(payload)
                .headers(headers)
                .createTime(LocalDateTime.now())
                .build();
        return send(message);
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
        // Redis Stream 不原生支持延迟消息，但可以通过在消息中添加延迟时间来模拟
        // 消费者需要检查延迟时间
        log.warn("Redis Stream 不原生支持延迟消息，将在消息中添加延迟时间信息");
        
        Message<T> message = Message.<T>builder()
                .topic(topic)
                .payload(payload)
                .createTime(LocalDateTime.now())
                .build();
        message.getHeaders().put("delay_until", LocalDateTime.now().plus(delay).toString());
        
        return send(message);
    }

    @Override
    public SendResult sendDelayMessage(String topic, String queue, T payload, Duration delay) {
        Message<T> message = Message.<T>builder()
                .topic(topic)
                .queue(queue)
                .payload(payload)
                .createTime(LocalDateTime.now())
                .build();
        message.getHeaders().put("delay_until", LocalDateTime.now().plus(delay).toString());
        
        return send(message);
    }

    @Override
    public SendResult sendOrderedMessage(String topic, T payload, String shardKey) {
        // Redis Stream 保证同一个 Stream 内的消息顺序
        log.debug("Redis Stream 保证消息顺序，shardKey={} 将被记录到消息头", shardKey);
        
        Message<T> message = Message.<T>builder()
                .topic(topic)
                .payload(payload)
                .createTime(LocalDateTime.now())
                .build();
        message.getHeaders().put("shard_key", shardKey);
        
        return send(message);
    }

    @Override
    public SendResult sendOrderedMessage(String topic, String queue, T payload, String shardKey) {
        Message<T> message = Message.<T>builder()
                .topic(topic)
                .queue(queue)
                .payload(payload)
                .createTime(LocalDateTime.now())
                .build();
        message.getHeaders().put("shard_key", shardKey);
        
        return send(message);
    }

    @Override
    public SendResult sendTransactionMessage(String topic, T payload, TransactionCallback callback) {
        throw new UnsupportedOperationException("Redis Stream 不支持事务消息");
    }

    @Override
    public BatchSendResult sendBatch(List<Message<T>> messages) {
        long startTime = System.currentTimeMillis();
        DefaultBatchSendResult batchResult = new DefaultBatchSendResult();

        for (Message<T> message : messages) {
            SendResult result = send(message);
            batchResult.addResult(result);
        }

        batchResult.setElapsedTime(System.currentTimeMillis() - startTime);
        return batchResult;
    }

    @Override
    public BatchSendResult sendBatch(String topic, List<T> payloads) {
        long startTime = System.currentTimeMillis();
        DefaultBatchSendResult batchResult = new DefaultBatchSendResult();

        for (T payload : payloads) {
            SendResult result = send(topic, payload);
            batchResult.addResult(result);
        }

        batchResult.setElapsedTime(System.currentTimeMillis() - startTime);
        return batchResult;
    }

    @Override
    public CompletableFuture<BatchSendResult> sendBatchAsync(List<Message<T>> messages) {
        return CompletableFuture.supplyAsync(() -> sendBatch(messages));
    }

    @Override
    public SendResult sendBroadcast(String topic, T payload) {
        // Stream 模式下的广播可以通过多个消费者组实现
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
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void start() {
        running.set(true);
        log.info("Redis Stream 消息生产者已启动");
    }

    @Override
    public void stop() {
        running.set(false);
        log.info("Redis Stream 消息生产者已停止");
    }

    @Override
    public ProducerStats getStats() {
        return stats;
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

    /**
     * 生成消息ID
     */
    private String generateMessageId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // ========== 内部类 ==========

    @RequiredArgsConstructor
    private static class DefaultSendResult implements SendResult {
        private final boolean success;
        private final String messageId;
        private final String topic;
        private final String queue;
        private final long elapsedTime;
        private final String errorMessage;
        private final Throwable exception;
        private final long timestamp = System.currentTimeMillis();

        @Override
        public boolean isSuccess() { return success; }

        @Override
        public String getMessageId() { return messageId; }

        @Override
        public String getTopic() { return topic; }

        @Override
        public String getQueue() { return queue; }

        @Override
        public long getTimestamp() { return timestamp; }

        @Override
        public String getErrorMessage() { return errorMessage; }

        @Override
        public Throwable getException() { return exception; }

        @Override
        public long getElapsedTime() { return elapsedTime; }
    }

    private static class DefaultBatchSendResult implements BatchSendResult {
        private final java.util.List<SendResult> results = new java.util.ArrayList<>();
        private long elapsedTime;

        void addResult(SendResult result) { results.add(result); }
        void setElapsedTime(long elapsedTime) { this.elapsedTime = elapsedTime; }

        @Override
        public boolean isAllSuccess() { return results.stream().allMatch(SendResult::isSuccess); }

        @Override
        public int getSuccessCount() { return (int) results.stream().filter(SendResult::isSuccess).count(); }

        @Override
        public int getFailedCount() { return (int) results.stream().filter(r -> !r.isSuccess()).count(); }

        @Override
        public int getTotalCount() { return results.size(); }

        @Override
        public List<SendResult> getResults() { return results; }

        @Override
        public List<SendResult> getFailedResults() { return results.stream().filter(r -> !r.isSuccess()).toList(); }

        @Override
        public long getElapsedTime() { return elapsedTime; }
    }

    private static class DefaultProducerStats implements ProducerStats {
        private final AtomicLong sentCount = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failedCount = new AtomicLong(0);
        private final AtomicLong totalElapsedTime = new AtomicLong(0);
        private final long startTime = System.currentTimeMillis();

        void recordSuccess(long elapsedTime) {
            sentCount.incrementAndGet();
            successCount.incrementAndGet();
            totalElapsedTime.addAndGet(elapsedTime);
        }

        void recordFailure() {
            sentCount.incrementAndGet();
            failedCount.incrementAndGet();
        }

        @Override
        public long getSentCount() { return sentCount.get(); }

        @Override
        public long getSuccessCount() { return successCount.get(); }

        @Override
        public long getFailedCount() { return failedCount.get(); }

        @Override
        public double getSuccessRate() {
            long total = sentCount.get();
            return total == 0 ? 1.0 : (double) successCount.get() / total;
        }

        @Override
        public double getAverageElapsedTime() {
            long success = successCount.get();
            return success == 0 ? 0 : (double) totalElapsedTime.get() / success;
        }

        @Override
        public long getStartTime() { return startTime; }

        @Override
        public void reset() {
            sentCount.set(0);
            successCount.set(0);
            failedCount.set(0);
            totalElapsedTime.set(0);
        }
    }
}


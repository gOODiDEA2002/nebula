package io.nebula.messaging.rabbitmq.producer;

import io.nebula.messaging.core.producer.MessageProducer;
import io.nebula.messaging.core.message.Message;
import io.nebula.messaging.core.serializer.MessageSerializer;
import io.nebula.messaging.core.exception.MessageSendException;
import io.nebula.messaging.rabbitmq.delay.DelayMessage;
import io.nebula.messaging.rabbitmq.delay.DelayMessageProducer;
import io.nebula.messaging.rabbitmq.delay.DelayMessageResult;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.AMQP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RabbitMQ 消息生产者实现
 *
 * @author nebula
 */
public class RabbitMQMessageProducer<T> implements MessageProducer<T> {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQMessageProducer.class);

    private final Connection connection;
    private final MessageSerializer messageSerializer;
    private final DelayMessageProducer delayMessageProducer;
    private Duration timeout = Duration.ofSeconds(30);
    private volatile boolean started = false;
    private final AtomicLong sentCount = new AtomicLong();
    private final AtomicLong successCount = new AtomicLong();
    private final AtomicLong failedCount = new AtomicLong();
    private final AtomicLong totalElapsedTime = new AtomicLong();
    private final AtomicLong statsStartTime = new AtomicLong(System.currentTimeMillis());

    public RabbitMQMessageProducer(Connection connection, MessageSerializer messageSerializer, 
                                   DelayMessageProducer delayMessageProducer) {
        this.connection = connection;
        this.messageSerializer = messageSerializer;
        this.delayMessageProducer = delayMessageProducer;
    }

    @Override
    public SendResult send(Message<T> message) {
        // tag 非空时路由键用 tag, 匹配 subscribeWithTag 的 queueBind(queue, topic, tag) 绑定;
        // 此前 tag 被丢弃(路由键恒为 topic), tagged 队列永远收不到消息
        if (message.getTag() != null && !message.getTag().isEmpty()) {
            return doSend(message.getTopic(), null, message.getPayload(), message.getHeaders(), message.getTag());
        }
        return send(message.getTopic(), message.getQueue(), message.getPayload(), message.getHeaders());
    }

    @Override
    public SendResult send(String topic, T payload) {
        return send(topic, null, payload, null);
    }

    @Override
    public SendResult send(String topic, String queue, T payload) {
        return send(topic, queue, payload, null);
    }

    @Override
    public SendResult send(String topic, T payload, Map<String, String> headers) {
        return send(topic, null, payload, headers);
    }

    @Override
    public SendResult send(String topic, String queue, T payload, Map<String, String> headers) {
        // topic 交换机下消费者以 queueBind(queue, topic, topic) 绑定(路由键=topic)，
        // 故普通发送路由键统一用 topic；此前用 queue/"" 与消费端绑定不匹配，消息会被静默丢弃。
        return doSend(topic, queue, payload, headers, topic);
    }

    /**
     * 统一发送实现
     *
     * @param routingKey 路由键: 普通消息=topic(匹配 subscribe 绑定), 带 tag 消息=tag(匹配 subscribeWithTag 绑定)
     */
    private SendResult doSend(String topic, String queue, T payload, Map<String, String> headers, String routingKey) {
        long startTime = System.currentTimeMillis();
        String messageId = generateMessageId();
        
        try (Channel channel = connection.createChannel()) {
            // 确保交换机存在
            declareExchangeIfNotExists(channel, topic);
            
            // 如果指定了队列，确保队列存在并绑定到交换机(队列的声明绑定属消费侧职责, 此处仅兜底)
            if (queue != null) {
                declareQueueIfNotExists(channel, queue);
                bindQueueToExchange(channel, queue, topic);
            }
            
            // 序列化消息
            byte[] messageBody = messageSerializer.serialize(payload);
            
            // 构建消息属性
            AMQP.BasicProperties properties = buildMessageProperties(messageId, headers);
            
            channel.basicPublish(topic, routingKey, properties, messageBody);
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            recordSend(true, elapsedTime);
            logger.debug("Message sent successfully: topic={}, queue={}, routingKey={}, messageId={}, elapsed={}ms", 
                topic, queue, routingKey, messageId, elapsedTime);
            
            return new RabbitMQSendResult(true, messageId, topic, queue, startTime, elapsedTime, null, null);
            
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            recordSend(false, elapsedTime);
            logger.error("Failed to send message: topic={}, queue={}, messageId={}", topic, queue, messageId, e);
            return new RabbitMQSendResult(false, messageId, topic, queue, startTime, elapsedTime, 
                "Failed to send message: " + e.getMessage(), e);
        }
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
        if (delayMessageProducer == null) {
            logger.warn("DelayMessageProducer not available, sending immediately");
            return send(topic, payload);
        }
        
        DelayMessageResult result = delayMessageProducer.send(topic, payload, delay);
        return convertDelayResult(result);
    }

    @Override
    public SendResult sendDelayMessage(String topic, String queue, T payload, Duration delay) {
        if (delayMessageProducer == null) {
            logger.warn("DelayMessageProducer not available, sending immediately");
            return send(topic, queue, payload);
        }
        
        DelayMessageResult result = delayMessageProducer.send(topic, queue, payload, delay);
        return convertDelayResult(result);
    }
    
    /**
     * 转换延时消息结果为标准发送结果
     */
    private SendResult convertDelayResult(DelayMessageResult delayResult) {
        recordSend(delayResult.isSuccess(), delayResult.getElapsedTime());
        return new RabbitMQSendResult(
                delayResult.isSuccess(),
                delayResult.getMessageId(),
                null, // topic
                null, // queue
                delayResult.getTimestamp(),
                delayResult.getElapsedTime(),
                delayResult.getErrorMessage(),
                delayResult.getException()
        );
    }

    @Override
    public SendResult sendOrderedMessage(String topic, T payload, String shardKey) {
        // RabbitMQ 的顺序消息可以通过单一消费者或分区队列实现
        // 这里提供基础实现
        return send(topic, shardKey, payload);
    }

    @Override
    public SendResult sendOrderedMessage(String topic, String queue, T payload, String shardKey) {
        return send(topic, queue, payload);
    }

    @Override
    public SendResult sendTransactionMessage(String topic, T payload, TransactionCallback callback) {
        // RabbitMQ 的事务消息实现相对复杂，这里提供基础版本
        logger.warn("Transaction message not fully implemented for RabbitMQ");
        return send(topic, payload);
    }

    @Override
    public BatchSendResult sendBatch(List<Message<T>> messages) {
        // TODO: 实现批量发送
        throw new UnsupportedOperationException("Batch send not implemented yet");
    }

    @Override
    public BatchSendResult sendBatch(String topic, List<T> payloads) {
        // TODO: 实现批量发送
        throw new UnsupportedOperationException("Batch send not implemented yet");
    }

    @Override
    public CompletableFuture<BatchSendResult> sendBatchAsync(List<Message<T>> messages) {
        return CompletableFuture.supplyAsync(() -> sendBatch(messages));
    }

    @Override
    public SendResult sendBroadcast(String topic, T payload) {
        // 广播消息通过fanout类型的交换机实现
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
        return connection != null && connection.isOpen() && started;
    }

    @Override
    public void start() {
        started = true;
        logger.info("RabbitMQ Message Producer started");
    }

    @Override
    public void stop() {
        started = false;
        logger.info("RabbitMQ Message Producer stopped");
    }

    /**
     * 关闭生产者
     */
    public void close() {
        stop();
    }

    @Override
    public ProducerStats getStats() {
        return new RabbitMQProducerStats();
    }

    private void recordSend(boolean success, long elapsedTime) {
        sentCount.incrementAndGet();
        totalElapsedTime.addAndGet(elapsedTime);
        if (success) {
            successCount.incrementAndGet();
        } else {
            failedCount.incrementAndGet();
        }
    }

    // 私有方法

    private String generateMessageId() {
        return "MSG_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
    }

    private void declareExchangeIfNotExists(Channel channel, String exchangeName) throws IOException {
        channel.exchangeDeclare(exchangeName, "topic", true, false, null);
    }

    private void declareQueueIfNotExists(Channel channel, String queueName) throws IOException {
        channel.queueDeclare(queueName, true, false, false, null);
    }

    private void bindQueueToExchange(Channel channel, String queueName, String exchangeName) throws IOException {
        // 路由键统一用交换机名(=topic)，与消费端 queueBind(queue, topic, topic) 及发送路由键保持一致
        channel.queueBind(queueName, exchangeName, exchangeName);
    }

    private AMQP.BasicProperties buildMessageProperties(String messageId, Map<String, String> headers) {
        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder()
            .messageId(messageId)
            .timestamp(new java.util.Date())
            .deliveryMode(2); // 持久化消息

        if (headers != null && !headers.isEmpty()) {
            builder.headers(new ConcurrentHashMap<>(headers));
        }

        return builder.build();
    }

    // 内部类

    private static class RabbitMQSendResult implements SendResult {
        private final boolean success;
        private final String messageId;
        private final String topic;
        private final String queue;
        private final long timestamp;
        private final long elapsedTime;
        private final String errorMessage;
        private final Throwable exception;

        public RabbitMQSendResult(boolean success, String messageId, String topic, String queue, 
                                 long timestamp, long elapsedTime, String errorMessage, Throwable exception) {
            this.success = success;
            this.messageId = messageId;
            this.topic = topic;
            this.queue = queue;
            this.timestamp = timestamp;
            this.elapsedTime = elapsedTime;
            this.errorMessage = errorMessage;
            this.exception = exception;
        }

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

    private class RabbitMQProducerStats implements ProducerStats {
        @Override
        public long getSentCount() { return sentCount.get(); }
        
        @Override
        public long getSuccessCount() { return successCount.get(); }
        
        @Override
        public long getFailedCount() { return failedCount.get(); }
        
        @Override
        public double getSuccessRate() {
            long sent = sentCount.get();
            return sent == 0 ? 0.0 : (double) successCount.get() / sent;
        }
        
        @Override
        public double getAverageElapsedTime() {
            long sent = sentCount.get();
            return sent == 0 ? 0.0 : (double) totalElapsedTime.get() / sent;
        }
        
        @Override
        public long getStartTime() { return statsStartTime.get(); }
        
        @Override
        public void reset() {
            sentCount.set(0);
            successCount.set(0);
            failedCount.set(0);
            totalElapsedTime.set(0);
            statsStartTime.set(System.currentTimeMillis());
        }
    }
}

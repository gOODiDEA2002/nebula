package io.nebula.messaging.rabbitmq.producer;

import io.nebula.messaging.core.producer.MessageProducer;
import io.nebula.messaging.core.message.Message;
import io.nebula.messaging.core.serializer.MessageSerializer;
import io.nebula.messaging.core.exception.MessageSendException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.AMQP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RabbitMQ 消息生产者实现
 *
 * @author nebula
 */
@Component
public class RabbitMQMessageProducer<T> implements MessageProducer<T> {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQMessageProducer.class);

    private final Connection connection;
    private final MessageSerializer messageSerializer;
    private Duration timeout = Duration.ofSeconds(30);
    private volatile boolean started = false;

    public RabbitMQMessageProducer(Connection connection, MessageSerializer messageSerializer) {
        this.connection = connection;
        this.messageSerializer = messageSerializer;
    }

    @Override
    public SendResult send(Message<T> message) {
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
        long startTime = System.currentTimeMillis();
        String messageId = generateMessageId();
        
        try (Channel channel = connection.createChannel()) {
            // 确保交换机存在
            declareExchangeIfNotExists(channel, topic);
            
            // 如果指定了队列，确保队列存在并绑定到交换机
            if (queue != null) {
                declareQueueIfNotExists(channel, queue);
                bindQueueToExchange(channel, queue, topic);
            }
            
            // 序列化消息
            byte[] messageBody = messageSerializer.serialize(payload);
            
            // 构建消息属性
            AMQP.BasicProperties properties = buildMessageProperties(messageId, headers);
            
            // 发送消息
            String routingKey = queue != null ? queue : "";
            channel.basicPublish(topic, routingKey, properties, messageBody);
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.debug("Message sent successfully: topic={}, queue={}, messageId={}, elapsed={}ms", 
                topic, queue, messageId, elapsedTime);
            
            return new RabbitMQSendResult(true, messageId, topic, queue, startTime, elapsedTime, null, null);
            
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
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
        // RabbitMQ 的延迟消息需要使用插件或通过TTL+DLX实现
        // 这里提供基础实现，生产环境中应该使用专门的延迟队列
        logger.warn("Delay message not fully implemented for RabbitMQ, sending immediately");
        return send(topic, payload);
    }

    @Override
    public SendResult sendDelayMessage(String topic, String queue, T payload, Duration delay) {
        logger.warn("Delay message not fully implemented for RabbitMQ, sending immediately");
        return send(topic, queue, payload);
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
        // TODO: 实现统计信息
        return new RabbitMQProducerStats();
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
        channel.queueBind(queueName, exchangeName, queueName);
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

    private static class RabbitMQProducerStats implements ProducerStats {
        // TODO: 实现统计功能
        @Override
        public long getSentCount() { return 0; }
        
        @Override
        public long getSuccessCount() { return 0; }
        
        @Override
        public long getFailedCount() { return 0; }
        
        @Override
        public double getSuccessRate() { return 0.0; }
        
        @Override
        public double getAverageElapsedTime() { return 0.0; }
        
        @Override
        public long getStartTime() { return System.currentTimeMillis(); }
        
        @Override
        public void reset() { }
    }
}
package io.nebula.messaging.rabbitmq.delay;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import io.nebula.messaging.core.exception.MessageSendException;
import io.nebula.messaging.core.serializer.MessageSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 延时消息生产者
 * 
 * 基于RabbitMQ的TTL+DLX机制实现延时消息功能
 * 
 * 工作原理：
 * 1. 创建延时交换机和延时队列（设置TTL）
 * 2. 将延时队列的DLX（死信交换机）指向目标交换机
 * 3. 消息发送到延时队列，TTL到期后自动路由到目标队列
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@Component
public class DelayMessageProducer {
    
    private static final String DELAY_EXCHANGE_PREFIX = "nebula.delay.exchange.";
    private static final String DELAY_QUEUE_PREFIX = "nebula.delay.queue.";
    private static final String DLX_HEADER = "x-dead-letter-exchange";
    private static final String DLX_ROUTING_KEY_HEADER = "x-dead-letter-routing-key";
    
    private final Connection connection;
    private final MessageSerializer messageSerializer;
    private final RabbitDelayMessageProperties properties;
    private final Map<String, Boolean> declaredExchanges = new ConcurrentHashMap<>();
    
    public DelayMessageProducer(Connection connection, MessageSerializer messageSerializer) {
        this(connection, messageSerializer, null);
    }
    
    public DelayMessageProducer(Connection connection, MessageSerializer messageSerializer,
                               RabbitDelayMessageProperties properties) {
        this.connection = connection;
        this.messageSerializer = messageSerializer;
        this.properties = properties != null ? properties : createDefaultProperties();
    }
    
    private RabbitDelayMessageProperties createDefaultProperties() {
        RabbitDelayMessageProperties defaultProps = new RabbitDelayMessageProperties();
        return defaultProps;
    }
    
    /**
     * 发送延时消息
     * 
     * @param message 延时消息对象
     * @param <T>     消息载荷类型
     * @return 发送结果
     */
    public <T> DelayMessageResult send(DelayMessage<T> message) {
        if (message == null) {
            throw new IllegalArgumentException("Delay message cannot be null");
        }
        
        if (message.getDelay() == null || message.getDelay().isNegative() || message.getDelay().isZero()) {
            throw new IllegalArgumentException("Delay duration must be positive");
        }
        
        // 验证延时时间范围
        long delayMillis = message.getDelay().toMillis();
        if (delayMillis < properties.getMinDelayMillis()) {
            throw new IllegalArgumentException(
                String.format("Delay duration %dms is less than minimum %dms", 
                    delayMillis, properties.getMinDelayMillis()));
        }
        if (delayMillis > properties.getMaxDelayMillis()) {
            throw new IllegalArgumentException(
                String.format("Delay duration %dms exceeds maximum %dms", 
                    delayMillis, properties.getMaxDelayMillis()));
        }
        
        if (message.getMessageId() == null) {
            message.setMessageId(generateMessageId());
        }
        
        message.calculateExpectedTime();
        
        long startTime = System.currentTimeMillis();
        
        try (Channel channel = connection.createChannel()) {
            // 确保目标交换机和队列存在
            ensureTargetExists(channel, message.getTopic(), message.getQueue());
            
            // 创建延时交换机和队列
            String delayExchange = createDelayExchange(channel, message);
            String delayQueue = createDelayQueue(channel, message);
            
            // 绑定延时队列到延时交换机
            channel.queueBind(delayQueue, delayExchange, delayQueue);
            
            // 序列化消息载荷
            byte[] messageBody = messageSerializer.serialize(message.getPayload());
            
            // 构建消息属性
            AMQP.BasicProperties properties = buildMessageProperties(message);
            
            // 发送消息到延时队列
            channel.basicPublish(delayExchange, delayQueue, properties, messageBody);
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            
            log.debug("Delay message sent successfully: messageId={}, topic={}, queue={}, delay={}ms, elapsed={}ms",
                    message.getMessageId(), message.getTopic(), message.getQueue(), 
                    message.getDelay().toMillis(), elapsedTime);
            
            return DelayMessageResult.success(message.getMessageId(), startTime, elapsedTime);
            
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("Failed to send delay message: messageId={}, topic={}, queue={}", 
                    message.getMessageId(), message.getTopic(), message.getQueue(), e);
            return DelayMessageResult.failure(message.getMessageId(), startTime, elapsedTime, e);
        }
    }
    
    /**
     * 发送简单延时消息
     * 
     * @param topic   目标主题
     * @param payload 消息载荷
     * @param delay   延时时间
     * @param <T>     消息载荷类型
     * @return 发送结果
     */
    public <T> DelayMessageResult send(String topic, T payload, Duration delay) {
        DelayMessage<T> message = DelayMessage.<T>builder()
                .topic(topic)
                .payload(payload)
                .delay(delay)
                .build();
        return send(message);
    }
    
    /**
     * 发送延时消息到指定队列
     * 
     * @param topic   目标主题
     * @param queue   目标队列
     * @param payload 消息载荷
     * @param delay   延时时间
     * @param <T>     消息载荷类型
     * @return 发送结果
     */
    public <T> DelayMessageResult send(String topic, String queue, T payload, Duration delay) {
        DelayMessage<T> message = DelayMessage.<T>builder()
                .topic(topic)
                .queue(queue)
                .payload(payload)
                .delay(delay)
                .build();
        return send(message);
    }
    
    /**
     * 批量发送延时消息
     * 
     * @param messages 延时消息列表
     * @param <T>      消息载荷类型
     * @return 批量发送结果
     */
    public <T> BatchDelayMessageResult sendBatch(List<DelayMessage<T>> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("Messages cannot be null or empty");
        }
        
        long startTime = System.currentTimeMillis();
        
        List<DelayMessageResult> results = messages.stream()
                .map(this::send)
                .collect(Collectors.toList());
        
        long elapsedTime = System.currentTimeMillis() - startTime;
        
        return new BatchDelayMessageResult(results, elapsedTime);
    }
    
    /**
     * 确保目标交换机和队列存在
     */
    private void ensureTargetExists(Channel channel, String topic, String queue) throws IOException {
        // 声明目标交换机
        if (topic != null && !declaredExchanges.containsKey(topic)) {
            channel.exchangeDeclare(topic, "topic", true, false, null);
            declaredExchanges.put(topic, true);
        }
        
        // 声明目标队列并绑定
        if (queue != null) {
            channel.queueDeclare(queue, true, false, false, null);
            if (topic != null) {
                channel.queueBind(queue, topic, queue);
            }
        }
    }
    
    /**
     * 创建延时交换机
     */
    private <T> String createDelayExchange(Channel channel, DelayMessage<T> message) throws IOException {
        String delayExchange = DELAY_EXCHANGE_PREFIX + message.getTopic();
        
        if (!declaredExchanges.containsKey(delayExchange)) {
            // Direct类型，精准匹配路由键
            channel.exchangeDeclare(delayExchange, "direct", true, false, null);
            declaredExchanges.put(delayExchange, true);
            log.debug("Created delay exchange: {}", delayExchange);
        }
        
        return delayExchange;
    }
    
    /**
     * 创建延时队列
     * 
     * 为每个延时时间创建独立的队列，以支持不同延时时间的消息
     */
    private <T> String createDelayQueue(Channel channel, DelayMessage<T> message) throws IOException {
        long delayMillis = message.getDelay().toMillis();
        String targetQueue = message.getQueue() != null ? message.getQueue() : message.getTopic();
        String delayQueue = DELAY_QUEUE_PREFIX + targetQueue + "." + delayMillis + "ms";
        
        Map<String, Object> args = new HashMap<>();
        // 设置消息TTL
        args.put("x-message-ttl", delayMillis);
        // 设置死信交换机
        args.put(DLX_HEADER, message.getTopic());
        // 设置死信路由键
        args.put(DLX_ROUTING_KEY_HEADER, targetQueue);
        
        // 声明延时队列
        channel.queueDeclare(delayQueue, true, false, false, args);
        
        log.debug("Created delay queue: {}, ttl={}ms, dlx={}, dlx-routing-key={}", 
                delayQueue, delayMillis, message.getTopic(), targetQueue);
        
        return delayQueue;
    }
    
    /**
     * 构建消息属性
     */
    private <T> AMQP.BasicProperties buildMessageProperties(DelayMessage<T> message) {
        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder()
                .messageId(message.getMessageId())
                .timestamp(new java.util.Date(message.getCreateTime()))
                .priority(message.getPriority());
        
        // 持久化消息
        if (message.isPersistent()) {
            builder.deliveryMode(2);
        }
        
        // 添加自定义headers
        Map<String, Object> headers = new HashMap<>();
        if (message.getHeaders() != null) {
            headers.putAll(message.getHeaders());
        }
        // 添加延时消息元数据
        headers.put("x-delay-original-topic", message.getTopic());
        headers.put("x-delay-original-queue", message.getQueue());
        headers.put("x-delay-millis", message.getDelay().toMillis());
        headers.put("x-delay-expected-time", message.getExpectedTime());
        headers.put("x-delay-max-retries", message.getMaxRetries());
        headers.put("x-delay-current-retry", message.getCurrentRetry());
        
        builder.headers(headers);
        
        return builder.build();
    }
    
    /**
     * 生成消息ID
     */
    private String generateMessageId() {
        return "DELAY_" + UUID.randomUUID().toString();
    }
    
    /**
     * 检查生产者是否可用
     */
    public boolean isAvailable() {
        return connection != null && connection.isOpen();
    }
}


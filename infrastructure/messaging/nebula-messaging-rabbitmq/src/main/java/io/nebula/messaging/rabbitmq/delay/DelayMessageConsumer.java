package io.nebula.messaging.rabbitmq.delay;

import com.rabbitmq.client.*;
import com.rabbitmq.client.LongString;
import io.nebula.messaging.core.serializer.MessageSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 延时消息消费者
 * 
 * 负责消费从延时队列过期后转发到目标队列的消息，
 * 支持自动重试和死信处理
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@Component
public class DelayMessageConsumer {
    
    private static final String DEAD_LETTER_EXCHANGE = "nebula.dlx.exchange";
    private static final String DEAD_LETTER_QUEUE = "nebula.dlx.queue";
    
    private final Connection connection;
    private final MessageSerializer messageSerializer;
    private final RabbitDelayMessageProperties properties;
    private final Map<String, String> consumerTags = new ConcurrentHashMap<>();
    
    public DelayMessageConsumer(Connection connection, MessageSerializer messageSerializer) {
        this(connection, messageSerializer, null);
    }
    
    public DelayMessageConsumer(Connection connection, MessageSerializer messageSerializer, 
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
     * 订阅延时消息
     * 
     * @param queue   目标队列名称
     * @param handler 消息处理器
     * @param <T>     消息载荷类型
     * @throws IOException IO异常
     */
    public <T> void subscribe(String queue, Class<T> messageType, DelayMessageHandler<T> handler) throws IOException {
        Channel channel = connection.createChannel();
        
        // 确保队列存在
        channel.queueDeclare(queue, true, false, false, null);
        
        // 设置预取数量
        channel.basicQos(1);
        
        // 创建消费者
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            try {
                handleMessage(channel, delivery, messageType, handler);
            } catch (Exception e) {
                log.error("Failed to handle delay message: queue={}, deliveryTag={}", 
                        queue, delivery.getEnvelope().getDeliveryTag(), e);
                // 拒绝消息，不重新入队，进入死信队列
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            }
        };
        
        CancelCallback cancelCallback = consumerTag -> {
            log.warn("Consumer cancelled: queue={}, consumerTag={}", queue, consumerTag);
            consumerTags.remove(queue);
        };
        
        // 开始消费，手动确认模式
        String consumerTag = channel.basicConsume(queue, false, deliverCallback, cancelCallback);
        consumerTags.put(queue, consumerTag);
        
        log.info("Started consuming delay messages: queue={}, consumerTag={}", queue, consumerTag);
    }
    
    /**
     * 处理消息
     */
    private <T> void handleMessage(Channel channel, Delivery delivery, Class<T> messageType, 
                                   DelayMessageHandler<T> handler) throws IOException {
        long deliveryTag = delivery.getEnvelope().getDeliveryTag();
        String messageId = delivery.getProperties().getMessageId();
        
        try {
            // 反序列化消息
            byte[] body = delivery.getBody();
            T payload = messageSerializer.deserialize(body, messageType);
            
            // 提取延时消息元数据
            Map<String, Object> headers = delivery.getProperties().getHeaders();
            DelayMessageContext context = extractContext(delivery.getProperties(), headers);
            
            log.debug("Processing delay message: messageId={}, queue={}, deliveryTag={}", 
                    messageId, context.getOriginalQueue(), deliveryTag);
            
            // 调用业务处理器
            handler.handle(payload, context);
            
            // 确认消息
            channel.basicAck(deliveryTag, false);
            
            log.debug("Delay message processed successfully: messageId={}, deliveryTag={}", 
                    messageId, deliveryTag);
            
        } catch (Exception e) {
            log.error("Failed to process delay message: messageId={}, deliveryTag={}", 
                    messageId, deliveryTag, e);
            
            // 检查是否可以重试
            Map<String, Object> headers = delivery.getProperties().getHeaders();
            int currentRetry = headers != null && headers.containsKey("x-delay-current-retry") 
                    ? (int) headers.get("x-delay-current-retry") : 0;
            int maxRetries = headers != null && headers.containsKey("x-delay-max-retries") 
                    ? (int) headers.get("x-delay-max-retries") : properties.getDefaultMaxRetries();
            
            if (currentRetry < maxRetries) {
                // 重新发送到延时队列进行重试
                retryMessage(channel, delivery, currentRetry + 1);
                // 确认原消息
                channel.basicAck(deliveryTag, false);
            } else {
                // 超过最大重试次数，发送到死信队列
                sendToDeadLetterQueue(channel, delivery, e);
                // 确认原消息
                channel.basicAck(deliveryTag, false);
            }
        }
    }
    
    /**
     * 重试消息
     */
    private void retryMessage(Channel channel, Delivery delivery, int retryCount) throws IOException {
        Map<String, Object> headers = new HashMap<>(delivery.getProperties().getHeaders());
        headers.put("x-delay-current-retry", retryCount);
        
        // 获取重试间隔（使用配置的默认值）
        long retryInterval = headers.containsKey("x-delay-retry-interval") 
                ? (long) headers.get("x-delay-retry-interval") : properties.getDefaultRetryInterval().toMillis();
        
        // 创建新的属性，增加重试次数
        AMQP.BasicProperties newProperties = new AMQP.BasicProperties.Builder()
                .messageId(delivery.getProperties().getMessageId())
                .timestamp(new java.util.Date())
                .deliveryMode(2)
                .headers(headers)
                .build();
        
        // 重新发送到延时队列
        String originalTopic = getStringFromHeader(headers, "x-delay-original-topic");
        String originalQueue = getStringFromHeader(headers, "x-delay-original-queue");
        
        log.info("Retrying delay message: messageId={}, retry={}/{}, delay={}ms",
                delivery.getProperties().getMessageId(), retryCount, 
                headers.get("x-delay-max-retries"), retryInterval);
        
        // 这里应该发送到延时队列，但为了简化，直接发送到原队列
        // 生产环境应该使用DelayMessageProducer重新发送
        channel.basicPublish(originalTopic, originalQueue, newProperties, delivery.getBody());
    }
    
    /**
     * 发送到死信队列
     */
    private void sendToDeadLetterQueue(Channel channel, Delivery delivery, Exception exception) throws IOException {
        // 如果禁用了死信队列，直接拒绝消息
        if (!properties.isEnableDeadLetterQueue()) {
            log.warn("Dead letter queue is disabled, rejecting message: messageId={}", 
                delivery.getProperties().getMessageId());
            return;
        }
        
        // 使用配置的死信队列名称
        String dlxExchange = properties.getDeadLetterQueue().getExchange();
        String dlxQueue = properties.getDeadLetterQueue().getQueue();
        boolean durable = properties.getDeadLetterQueue().isDurable();
        boolean autoDelete = properties.getDeadLetterQueue().isAutoDelete();
        
        // 确保死信交换机和队列存在
        channel.exchangeDeclare(dlxExchange, "direct", durable, autoDelete, null);
        channel.queueDeclare(dlxQueue, durable, false, autoDelete, null);
        channel.queueBind(dlxQueue, dlxExchange, dlxQueue);
        
        // 添加异常信息到headers
        Map<String, Object> headers = new HashMap<>(delivery.getProperties().getHeaders());
        headers.put("x-exception-message", exception.getMessage());
        headers.put("x-exception-class", exception.getClass().getName());
        headers.put("x-failed-time", System.currentTimeMillis());
        headers.put("x-original-exchange", delivery.getEnvelope().getExchange());
        headers.put("x-original-routing-key", delivery.getEnvelope().getRoutingKey());
        
        AMQP.BasicProperties messageProperties = new AMQP.BasicProperties.Builder()
                .messageId(delivery.getProperties().getMessageId())
                .timestamp(new java.util.Date())
                .deliveryMode(2)
                .headers(headers)
                .build();
        
        channel.basicPublish(dlxExchange, dlxQueue, messageProperties, delivery.getBody());
        
        log.error("Sent message to dead letter queue: messageId={}, queue={}, reason={}",
                delivery.getProperties().getMessageId(), dlxQueue, exception.getMessage());
    }
    
    /**
     * 提取延时消息上下文
     */
    private DelayMessageContext extractContext(AMQP.BasicProperties properties, Map<String, Object> headers) {
        DelayMessageContext context = new DelayMessageContext();
        context.setMessageId(properties.getMessageId());
        context.setTimestamp(properties.getTimestamp() != null ? properties.getTimestamp().getTime() : 0);
        
        if (headers != null) {
            context.setOriginalTopic(getStringFromHeader(headers, "x-delay-original-topic"));
            context.setOriginalQueue(getStringFromHeader(headers, "x-delay-original-queue"));
            context.setDelayMillis(headers.containsKey("x-delay-millis") 
                    ? ((Number) headers.get("x-delay-millis")).longValue() : 0);
            context.setExpectedTime(headers.containsKey("x-delay-expected-time") 
                    ? ((Number) headers.get("x-delay-expected-time")).longValue() : 0);
            context.setMaxRetries(headers.containsKey("x-delay-max-retries") 
                    ? ((Number) headers.get("x-delay-max-retries")).intValue() : 3);
            context.setCurrentRetry(headers.containsKey("x-delay-current-retry") 
                    ? ((Number) headers.get("x-delay-current-retry")).intValue() : 0);
        }
        
        return context;
    }
    
    /**
     * 取消订阅
     */
    public void unsubscribe(String queue) throws IOException {
        String consumerTag = consumerTags.remove(queue);
        if (consumerTag != null) {
            Channel channel = connection.createChannel();
            channel.basicCancel(consumerTag);
            log.info("Unsubscribed from delay messages: queue={}, consumerTag={}", queue, consumerTag);
        }
    }
    
    /**
     * 检查消费者是否可用
     */
    public boolean isAvailable() {
        return connection != null && connection.isOpen();
    }
    
    /**
     * 从 header 中安全地获取字符串值
     * RabbitMQ headers 中的字符串可能是 LongString 类型
     */
    private String getStringFromHeader(Map<String, Object> headers, String key) {
        if (headers == null || !headers.containsKey(key)) {
            return null;
        }
        
        Object value = headers.get(key);
        if (value == null) {
            return null;
        }
        
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof LongString) {
            return value.toString();
        } else {
            return value.toString();
        }
    }
    
    /**
     * 延时消息处理器接口
     */
    @FunctionalInterface
    public interface DelayMessageHandler<T> {
        /**
         * 处理延时消息
         * 
         * @param payload 消息载荷
         * @param context 消息上下文
         * @throws Exception 处理异常
         */
        void handle(T payload, DelayMessageContext context) throws Exception;
    }
}


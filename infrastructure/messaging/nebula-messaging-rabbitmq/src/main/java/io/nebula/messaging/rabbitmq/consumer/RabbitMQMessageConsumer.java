package io.nebula.messaging.rabbitmq.consumer;

import io.nebula.messaging.core.consumer.MessageConsumer;
import io.nebula.messaging.core.consumer.MessageHandler;
import io.nebula.messaging.core.message.Message;
import io.nebula.messaging.core.serializer.MessageSerializer;
import io.nebula.messaging.core.router.MessageRouter;
import io.nebula.messaging.core.exception.MessageReceiveException;
import io.nebula.messaging.core.exception.MessageConnectionException;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;

/**
 * RabbitMQ消息消费者实现
 */
@Component
public class RabbitMQMessageConsumer implements MessageConsumer<Object> {
    
    private static final Logger log = LoggerFactory.getLogger(RabbitMQMessageConsumer.class);
    
    private final Connection connection;
    private final MessageSerializer messageSerializer;
    private final MessageRouter messageRouter;
    private final Map<String, Channel> subscriptions = new ConcurrentHashMap<>();
    private final Map<String, MessageHandler<Object>> handlers = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    
    // 配置信息
    private volatile RabbitMQConsumerConfig config = new RabbitMQConsumerConfig();
    
    // 统计信息
    private final AtomicLong consumedCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failedCount = new AtomicLong(0);
    private final long startTime = System.currentTimeMillis();
    
    public RabbitMQMessageConsumer(Connection connection, 
                                 MessageSerializer messageSerializer,
                                 MessageRouter messageRouter) {
        this.connection = connection;
        this.messageSerializer = messageSerializer;
        this.messageRouter = messageRouter;
    }
    
    @Override
    public void subscribe(String topic, MessageHandler<Object> handler) {
        subscribe(topic, topic, handler); // 默认队列名等于主题名
    }
    
    @Override
    public void subscribe(String topic, String queue, MessageHandler<Object> handler) {
        try {
            if (subscriptions.containsKey(topic + ":" + queue)) {
                log.warn("主题 {} 队列 {} 已订阅，将替换原有处理器", topic, queue);
                unsubscribe(topic, queue);
            }
            
            handlers.put(topic + ":" + queue, handler);
            
            Channel channel = connection.createChannel();
            
            // 声明交换机和队列
            channel.exchangeDeclare(topic, "topic", true, false, null);
            channel.queueDeclare(queue, true, false, false, null);
            channel.queueBind(queue, topic, topic);
            
            // 设置消息消费者
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                if (paused.get()) {
                    return; // 如果暂停，不处理消息
                }
                
                try {
                    consumedCount.incrementAndGet();
                    
                    // 反序列化消息
                    Object payload = messageSerializer.deserialize(delivery.getBody(), Object.class);
                    
                    // 创建消息对象
                    Message<Object> message = Message.<Object>builder()
                        .id(delivery.getProperties().getMessageId())
                        .topic(topic)
                        .payload(payload)
                        .createTime(java.time.LocalDateTime.now())
                        .build();
                    
                    // 处理消息
                    handler.handle(message);
                    
                    // 确认消息（如果不是自动确认）
                    if (!config.isAutoAck()) {
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    }
                    successCount.incrementAndGet();
                    
                    log.debug("消息处理成功: topic={}, queue={}, messageId={}", topic, queue, message.getId());
                    
                } catch (Exception e) {
                    failedCount.incrementAndGet();
                    log.error("消息处理失败: topic={}, queue={}", topic, queue, e);
                    
                    try {
                        // 拒绝消息并重新入队
                        channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
                    } catch (IOException ioException) {
                        log.error("消息拒绝失败", ioException);
                    }
                }
            };
            
            CancelCallback cancelCallback = consumerTag -> {
                log.warn("消费者被取消: topic={}, queue={}, consumerTag={}", topic, queue, consumerTag);
            };
            
            // 开始消费
            channel.basicConsume(queue, config.isAutoAck(), deliverCallback, cancelCallback);
            subscriptions.put(topic + ":" + queue, channel);
            
            log.info("订阅主题队列成功: topic={}, queue={}", topic, queue);
            
        } catch (Exception e) {
            log.error("订阅主题队列失败: topic={}, queue={}", topic, queue, e);
            throw new MessageConnectionException("Failed to subscribe to topic: " + topic + ", queue: " + queue, e);
        }
    }
    
    @Override
    public void subscribeWithTag(String topic, String tag, MessageHandler<Object> handler) {
        // RabbitMQ使用routing key实现标签过滤
        try {
            String queue = topic + "_" + tag;
            handlers.put(topic + ":" + queue, handler);
            
            Channel channel = connection.createChannel();
            
            // 声明交换机和队列
            channel.exchangeDeclare(topic, "topic", true, false, null);
            channel.queueDeclare(queue, true, false, false, null);
            channel.queueBind(queue, topic, tag); // 使用tag作为routing key
            
            // 设置消息消费者（与subscribe方法类似）
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                if (paused.get()) return;
                
                try {
                    consumedCount.incrementAndGet();
                    
                    Object payload = messageSerializer.deserialize(delivery.getBody(), Object.class);
                    
                    Message<Object> message = Message.<Object>builder()
                        .id(delivery.getProperties().getMessageId())
                        .topic(topic)
                        .payload(payload)
                        .createTime(java.time.LocalDateTime.now())
                        .build();
                    
                    handler.handle(message);
                    
                    if (!config.isAutoAck()) {
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    }
                    successCount.incrementAndGet();
                    
                } catch (Exception e) {
                    failedCount.incrementAndGet();
                    log.error("消息处理失败: topic={}, tag={}", topic, tag, e);
                    
                    try {
                        channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
                    } catch (IOException ioException) {
                        log.error("消息拒绝失败", ioException);
                    }
                }
            };
            
            channel.basicConsume(queue, config.isAutoAck(), deliverCallback, consumerTag -> {
                log.warn("消费者被取消: topic={}, tag={}", topic, tag);
            });
            subscriptions.put(topic + ":" + queue, channel);
            
            log.info("订阅主题标签成功: topic={}, tag={}", topic, tag);
            
        } catch (Exception e) {
            log.error("订阅主题标签失败: topic={}, tag={}", topic, tag, e);
            throw new MessageConnectionException("Failed to subscribe to topic: " + topic + " with tag: " + tag, e);
        }
    }
    
    @Override
    public void unsubscribe(String topic) {
        unsubscribe(topic, topic); // 默认队列名等于主题名
    }
    
    @Override
    public void unsubscribe(String topic, String queue) {
        try {
            String key = topic + ":" + queue;
            Channel channel = subscriptions.remove(key);
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            handlers.remove(key);
            log.info("取消订阅主题队列: topic={}, queue={}", topic, queue);
        } catch (Exception e) {
            log.error("取消订阅失败: topic={}, queue={}", topic, queue, e);
            throw new MessageConnectionException("Failed to unsubscribe from topic: " + topic + ", queue: " + queue, e);
        }
    }
    
    @Override
    public List<Message<Object>> pull(String topic, int maxCount, Duration timeout) {
        List<Message<Object>> messages = new ArrayList<>();
        
        try (Channel channel = connection.createChannel()) {
            // 声明队列
            channel.queueDeclare(topic, true, false, false, null);
            
            long startTime = System.currentTimeMillis();
            long timeoutMillis = timeout.toMillis();
            
            while (messages.size() < maxCount && (System.currentTimeMillis() - startTime) < timeoutMillis) {
                GetResponse response = channel.basicGet(topic, true);
                if (response == null) {
                    Thread.sleep(100); // 短暂等待
                    continue;
                }
                
                Object payload = messageSerializer.deserialize(response.getBody(), Object.class);
                
                Message<Object> message = Message.<Object>builder()
                    .id(response.getProps().getMessageId())
                    .topic(topic)
                    .payload(payload)
                    .createTime(java.time.LocalDateTime.now())
                    .build();
                
                messages.add(message);
            }
            
        } catch (Exception e) {
            log.error("拉取消息失败: topic={}", topic, e);
            throw new MessageReceiveException("Failed to pull messages from topic: " + topic, e);
        }
        
        return messages;
    }
    
    @Override
    public Message<Object> pullOne(String topic, Duration timeout) {
        List<Message<Object>> messages = pull(topic, 1, timeout);
        return messages.isEmpty() ? null : messages.get(0);
    }
    
    @Override
    public CompletableFuture<List<Message<Object>>> pullAsync(String topic, int maxCount, Duration timeout) {
        return CompletableFuture.supplyAsync(() -> pull(topic, maxCount, timeout));
    }
    
    @Override
    public boolean ack(Message<Object> message) {
        // RabbitMQ中确认操作通常在消息处理时自动完成
        // 这里返回true表示支持该操作
        return true;
    }
    
    @Override
    public boolean nack(Message<Object> message, boolean requeue) {
        // RabbitMQ中拒绝操作通常在消息处理时自动完成
        // 这里返回true表示支持该操作
        return true;
    }
    
    @Override
    public void start() {
        running.set(true);
        log.info("RabbitMQ Message Consumer started");
    }
    
    @Override
    public void stop() {
        running.set(false);
        // 关闭所有订阅
        subscriptions.forEach((key, channel) -> {
            try {
                if (channel.isOpen()) {
                    channel.close();
                }
            } catch (Exception e) {
                log.error("关闭订阅通道失败: key={}", key, e);
            }
        });
        subscriptions.clear();
        handlers.clear();
        log.info("RabbitMQ Message Consumer stopped");
    }
    
    @Override
    public void pause() {
        paused.set(true);
        log.info("RabbitMQ Message Consumer paused");
    }
    
    @Override
    public void resume() {
        paused.set(false);
        log.info("RabbitMQ Message Consumer resumed");
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
        if (config instanceof RabbitMQConsumerConfig) {
            this.config = (RabbitMQConsumerConfig) config;
        } else {
            // 如果传入的不是RabbitMQConsumerConfig，创建一个新的并复制属性
            RabbitMQConsumerConfig newConfig = new RabbitMQConsumerConfig();
            newConfig.setConsumerGroup(config.getConsumerGroup());
            newConfig.setConcurrency(config.getConcurrency());
            newConfig.setBatchSize(config.getBatchSize());
            newConfig.setConsumeTimeout(config.getConsumeTimeout());
            newConfig.setAutoAck(config.isAutoAck());
            newConfig.setMaxRetries(config.getMaxRetries());
            this.config = newConfig;
        }
    }
    
    @Override
    public ConsumerConfig getConfig() {
        return config;
    }
    
    @Override
    public ConsumerStats getStats() {
        return new RabbitMQConsumerStats();
    }
    
    /**
     * RabbitMQ消费者统计信息实现
     */
    private class RabbitMQConsumerStats implements ConsumerStats {
        
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
            return total > 0 ? (double) successCount.get() / total : 0.0;
        }
        
        @Override
        public double getAverageElapsedTime() {
            // TODO: 实现平均处理耗时统计
            return 0.0;
        }
        
        @Override
        public int getProcessingCount() {
            // TODO: 实现正在处理的消息数量统计
            return 0;
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
        }
    }
    
    /**
     * RabbitMQ消费者配置实现
     */
    public static class RabbitMQConsumerConfig implements ConsumerConfig {
        
        private String consumerGroup = "default";
        private int concurrency = 1;
        private int batchSize = 1;
        private Duration consumeTimeout = Duration.ofSeconds(30);
        private boolean autoAck = false;
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
}
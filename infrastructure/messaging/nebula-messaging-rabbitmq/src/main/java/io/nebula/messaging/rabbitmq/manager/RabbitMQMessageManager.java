package io.nebula.messaging.rabbitmq.manager;

import io.nebula.messaging.core.manager.MessageManager;
import io.nebula.messaging.core.producer.MessageProducer;
import io.nebula.messaging.core.consumer.MessageConsumer;
import io.nebula.messaging.core.consumer.MessageHandler;
import io.nebula.messaging.rabbitmq.producer.RabbitMQMessageProducer;
import io.nebula.messaging.rabbitmq.consumer.RabbitMQMessageConsumer;
import io.nebula.messaging.rabbitmq.exchange.RabbitMQExchangeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ消息管理器实现
 * 统一管理生产者、消费者和Exchange
 */
@Component
public class RabbitMQMessageManager implements MessageManager {
    
    private static final Logger log = LoggerFactory.getLogger(RabbitMQMessageManager.class);
    
    private final RabbitMQMessageProducer producer;
    private final RabbitMQMessageConsumer consumer;
    private final RabbitMQExchangeManager exchangeManager;
    private boolean running = false;
    
    @Autowired
    public RabbitMQMessageManager(RabbitMQMessageProducer producer,
                                RabbitMQMessageConsumer consumer,
                                RabbitMQExchangeManager exchangeManager) {
        this.producer = producer;
        this.consumer = consumer;
        this.exchangeManager = exchangeManager;
    }
    
    @Override
    public MessageProducer getProducer() {
        return producer;
    }
    
    @Override
    public MessageConsumer getConsumer() {
        return consumer;
    }
    
    @Override
    public void start() throws Exception {
        if (!running) {
            consumer.start();
            running = true;
            log.info("RabbitMQ消息管理器启动成功");
        }
    }
    
    @Override
    public void stop() throws Exception {
        if (running) {
            consumer.stop();
            producer.stop();
            running = false;
            log.info("RabbitMQ消息管理器停止成功");
        }
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
    
    @Override
    public void registerHandler(String topic, MessageHandler<?> handler) {
        // 确保Exchange存在
        if (!exchangeManager.isExchangeDeclared(topic)) {
            exchangeManager.declareTopicExchange(topic);
        }
        
        @SuppressWarnings("unchecked")
        MessageHandler<Object> objectHandler = (MessageHandler<Object>) handler;
        consumer.subscribe(topic, objectHandler);
        log.info("注册消息处理器: topic={}", topic);
    }
    
    @Override
    public void unregisterHandler(String topic) {
        consumer.unsubscribe(topic);
        log.info("取消注册消息处理器: topic={}", topic);
    }
    
    @Override
    public void createTopic(String topic) {
        exchangeManager.declareTopicExchange(topic);
        log.info("创建主题: topic={}", topic);
    }
    
    @Override
    public void deleteTopic(String topic) {
        // 取消该主题的所有订阅
        consumer.unsubscribe(topic);
        
        // 删除Exchange
        exchangeManager.deleteExchange(topic, true);
        log.info("删除主题: topic={}", topic);
    }
    
    @Override
    public boolean topicExists(String topic) {
        return exchangeManager.isExchangeDeclared(topic);
    }
    
    /**
     * 获取Exchange管理器
     * 
     * @return Exchange管理器
     */
    public RabbitMQExchangeManager getExchangeManager() {
        return exchangeManager;
    }
}

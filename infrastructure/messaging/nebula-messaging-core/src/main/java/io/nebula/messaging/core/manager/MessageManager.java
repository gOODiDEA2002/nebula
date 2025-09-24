package io.nebula.messaging.core.manager;

import io.nebula.messaging.core.consumer.MessageConsumer;
import io.nebula.messaging.core.producer.MessageProducer;
import io.nebula.messaging.core.consumer.MessageHandler;

/**
 * 消息管理器接口
 * 提供统一的消息生产者和消费者管理功能
 */
public interface MessageManager {
    
    /**
     * 获取消息生产者
     * 
     * @return 消息生产者
     */
    MessageProducer getProducer();
    
    /**
     * 获取消息消费者
     * 
     * @return 消息消费者
     */
    MessageConsumer getConsumer();
    
    /**
     * 启动消息管理器
     * 
     * @throws Exception 启动异常
     */
    void start() throws Exception;
    
    /**
     * 停止消息管理器
     * 
     * @throws Exception 停止异常
     */
    void stop() throws Exception;
    
    /**
     * 判断消息管理器是否正在运行
     * 
     * @return 是否运行中
     */
    boolean isRunning();
    
    /**
     * 注册消息处理器
     * 
     * @param topic 主题
     * @param handler 消息处理器
     */
    void registerHandler(String topic, MessageHandler<?> handler);
    
    /**
     * 取消注册消息处理器
     * 
     * @param topic 主题
     */
    void unregisterHandler(String topic);
    
    /**
     * 创建主题
     * 
     * @param topic 主题名称
     */
    void createTopic(String topic);
    
    /**
     * 删除主题
     * 
     * @param topic 主题名称
     */
    void deleteTopic(String topic);
    
    /**
     * 判断主题是否存在
     * 
     * @param topic 主题名称
     * @return 是否存在
     */
    boolean topicExists(String topic);
}

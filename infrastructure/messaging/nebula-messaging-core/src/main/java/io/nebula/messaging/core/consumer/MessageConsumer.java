package io.nebula.messaging.core.consumer;

import io.nebula.messaging.core.consumer.MessageHandler;
import io.nebula.messaging.core.message.Message;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 消息消费者接口
 * 
 * @param <T> 消息载荷类型
 */
public interface MessageConsumer<T> {
    
    /**
     * 订阅主题
     * 
     * @param topic   主题
     * @param handler 消息处理器
     */
    void subscribe(String topic, MessageHandler<T> handler);
    
    /**
     * 订阅主题和队列
     * 
     * @param topic   主题
     * @param queue   队列
     * @param handler 消息处理器
     */
    void subscribe(String topic, String queue, MessageHandler<T> handler);
    
    /**
     * 订阅主题（带标签过滤）
     * 
     * @param topic   主题
     * @param tag     标签
     * @param handler 消息处理器
     */
    void subscribeWithTag(String topic, String tag, MessageHandler<T> handler);
    
    /**
     * 取消订阅
     * 
     * @param topic 主题
     */
    void unsubscribe(String topic);
    
    /**
     * 取消订阅（指定队列）
     * 
     * @param topic 主题
     * @param queue 队列
     */
    void unsubscribe(String topic, String queue);
    
    /**
     * 手动拉取消息
     * 
     * @param topic    主题
     * @param maxCount 最大数量
     * @param timeout  超时时间
     * @return 消息列表
     */
    List<Message<T>> pull(String topic, int maxCount, Duration timeout);
    
    /**
     * 手动拉取单个消息
     * 
     * @param topic   主题
     * @param timeout 超时时间
     * @return 消息，如果没有则返回null
     */
    Message<T> pullOne(String topic, Duration timeout);
    
    /**
     * 异步拉取消息
     * 
     * @param topic    主题
     * @param maxCount 最大数量
     * @param timeout  超时时间
     * @return CompletableFuture包装的消息列表
     */
    CompletableFuture<List<Message<T>>> pullAsync(String topic, int maxCount, Duration timeout);
    
    /**
     * 手动确认消息
     * 
     * @param message 消息
     * @return 是否确认成功
     */
    boolean ack(Message<T> message);
    
    /**
     * 手动拒绝消息
     * 
     * @param message 消息
     * @param requeue 是否重新入队
     * @return 是否拒绝成功
     */
    boolean nack(Message<T> message, boolean requeue);
    
    /**
     * 启动消费者
     */
    void start();
    
    /**
     * 停止消费者
     */
    void stop();
    
    /**
     * 暂停消费
     */
    void pause();
    
    /**
     * 恢复消费
     */
    void resume();
    
    /**
     * 检查消费者是否正在运行
     * 
     * @return 是否运行中
     */
    boolean isRunning();
    
    /**
     * 检查消费者是否暂停
     * 
     * @return 是否暂停
     */
    boolean isPaused();
    
    /**
     * 设置消费者配置
     * 
     * @param config 消费者配置
     */
    void setConfig(ConsumerConfig config);
    
    /**
     * 获取消费者配置
     * 
     * @return 消费者配置
     */
    ConsumerConfig getConfig();
    
    /**
     * 获取消费者统计信息
     * 
     * @return 统计信息
     */
    ConsumerStats getStats();
    
    /**
     * 消费者配置
     */
    interface ConsumerConfig {
        /**
         * 获取消费者组
         * 
         * @return 消费者组
         */
        String getConsumerGroup();
        
        /**
         * 设置消费者组
         * 
         * @param consumerGroup 消费者组
         */
        void setConsumerGroup(String consumerGroup);
        
        /**
         * 获取并发消费线程数
         * 
         * @return 线程数
         */
        int getConcurrency();
        
        /**
         * 设置并发消费线程数
         * 
         * @param concurrency 线程数
         */
        void setConcurrency(int concurrency);
        
        /**
         * 获取批量消费大小
         * 
         * @return 批量大小
         */
        int getBatchSize();
        
        /**
         * 设置批量消费大小
         * 
         * @param batchSize 批量大小
         */
        void setBatchSize(int batchSize);
        
        /**
         * 获取消费超时时间
         * 
         * @return 超时时间
         */
        Duration getConsumeTimeout();
        
        /**
         * 设置消费超时时间
         * 
         * @param timeout 超时时间
         */
        void setConsumeTimeout(Duration timeout);
        
        /**
         * 是否自动确认
         * 
         * @return 是否自动确认
         */
        boolean isAutoAck();
        
        /**
         * 设置是否自动确认
         * 
         * @param autoAck 是否自动确认
         */
        void setAutoAck(boolean autoAck);
        
        /**
         * 获取最大重试次数
         * 
         * @return 最大重试次数
         */
        int getMaxRetries();
        
        /**
         * 设置最大重试次数
         * 
         * @param maxRetries 最大重试次数
         */
        void setMaxRetries(int maxRetries);
    }
    
    /**
     * 消费者统计信息
     */
    interface ConsumerStats {
        /**
         * 获取已消费消息数量
         * 
         * @return 消息数量
         */
        long getConsumedCount();
        
        /**
         * 获取消费成功数量
         * 
         * @return 成功数量
         */
        long getSuccessCount();
        
        /**
         * 获取消费失败数量
         * 
         * @return 失败数量
         */
        long getFailedCount();
        
        /**
         * 获取成功率
         * 
         * @return 成功率（0.0-1.0）
         */
        double getSuccessRate();
        
        /**
         * 获取平均消费耗时（毫秒）
         * 
         * @return 平均耗时
         */
        double getAverageElapsedTime();
        
        /**
         * 获取当前正在处理的消息数量
         * 
         * @return 处理中的消息数量
         */
        int getProcessingCount();
        
        /**
         * 获取统计开始时间
         * 
         * @return 开始时间戳
         */
        long getStartTime();
        
        /**
         * 重置统计信息
         */
        void reset();
    }
}

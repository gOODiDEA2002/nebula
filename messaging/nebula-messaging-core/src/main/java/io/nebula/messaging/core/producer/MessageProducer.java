package io.nebula.messaging.core.producer;

import io.nebula.messaging.core.message.Message;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 消息生产者接口
 * 
 * @param <T> 消息载荷类型
 */
public interface MessageProducer<T> {
    
    /**
     * 发送消息
     * 
     * @param message 消息对象
     * @return 发送结果
     */
    SendResult send(Message<T> message);
    
    /**
     * 发送消息到指定主题
     * 
     * @param topic   主题
     * @param payload 消息载荷
     * @return 发送结果
     */
    SendResult send(String topic, T payload);
    
    /**
     * 发送消息到指定主题和队列
     * 
     * @param topic   主题
     * @param queue   队列
     * @param payload 消息载荷
     * @return 发送结果
     */
    SendResult send(String topic, String queue, T payload);
    
    /**
     * 发送消息（带头部信息）
     * 
     * @param topic   主题
     * @param payload 消息载荷
     * @param headers 头部信息
     * @return 发送结果
     */
    SendResult send(String topic, T payload, Map<String, String> headers);
    
    /**
     * 发送消息到指定主题和队列（带头部信息）
     * 
     * @param topic   主题
     * @param queue   队列
     * @param payload 消息载荷
     * @param headers 头部信息
     * @return 发送结果
     */
    SendResult send(String topic, String queue, T payload, Map<String, String> headers);
    
    /**
     * 异步发送消息
     * 
     * @param message 消息对象
     * @return CompletableFuture包装的发送结果
     */
    CompletableFuture<SendResult> sendAsync(Message<T> message);
    
    /**
     * 异步发送消息到指定主题
     * 
     * @param topic   主题
     * @param payload 消息载荷
     * @return CompletableFuture包装的发送结果
     */
    CompletableFuture<SendResult> sendAsync(String topic, T payload);
    
    /**
     * 异步发送消息到指定主题和队列
     * 
     * @param topic   主题
     * @param queue   队列
     * @param payload 消息载荷
     * @return CompletableFuture包装的发送结果
     */
    CompletableFuture<SendResult> sendAsync(String topic, String queue, T payload);
    
    /**
     * 发送延迟消息
     * 
     * @param topic   主题
     * @param payload 消息载荷
     * @param delay   延迟时间
     * @return 发送结果
     */
    SendResult sendDelayMessage(String topic, T payload, Duration delay);
    
    /**
     * 发送延迟消息到指定队列
     * 
     * @param topic   主题
     * @param queue   队列
     * @param payload 消息载荷
     * @param delay   延迟时间
     * @return 发送结果
     */
    SendResult sendDelayMessage(String topic, String queue, T payload, Duration delay);
    
    /**
     * 发送顺序消息
     * 
     * @param topic     主题
     * @param payload   消息载荷
     * @param shardKey  分片键（保证相同分片键的消息有序）
     * @return 发送结果
     */
    SendResult sendOrderedMessage(String topic, T payload, String shardKey);
    
    /**
     * 发送顺序消息到指定队列
     * 
     * @param topic     主题
     * @param queue     队列
     * @param payload   消息载荷
     * @param shardKey  分片键
     * @return 发送结果
     */
    SendResult sendOrderedMessage(String topic, String queue, T payload, String shardKey);
    
    /**
     * 发送事务消息
     * 
     * @param topic    主题
     * @param payload  消息载荷
     * @param callback 事务回调
     * @return 发送结果
     */
    SendResult sendTransactionMessage(String topic, T payload, TransactionCallback callback);
    
    /**
     * 批量发送消息
     * 
     * @param messages 消息列表
     * @return 批量发送结果
     */
    BatchSendResult sendBatch(List<Message<T>> messages);
    
    /**
     * 批量发送消息到指定主题
     * 
     * @param topic    主题
     * @param payloads 消息载荷列表
     * @return 批量发送结果
     */
    BatchSendResult sendBatch(String topic, List<T> payloads);
    
    /**
     * 异步批量发送消息
     * 
     * @param messages 消息列表
     * @return CompletableFuture包装的批量发送结果
     */
    CompletableFuture<BatchSendResult> sendBatchAsync(List<Message<T>> messages);
    
    /**
     * 发送广播消息
     * 
     * @param topic   主题
     * @param payload 消息载荷
     * @return 发送结果
     */
    SendResult sendBroadcast(String topic, T payload);
    
    /**
     * 设置默认超时时间
     * 
     * @param timeout 超时时间
     */
    void setTimeout(Duration timeout);
    
    /**
     * 获取默认超时时间
     * 
     * @return 超时时间
     */
    Duration getTimeout();
    
    /**
     * 检查生产者是否可用
     * 
     * @return 是否可用
     */
    boolean isAvailable();
    
    /**
     * 启动生产者
     */
    void start();
    
    /**
     * 停止生产者
     */
    void stop();
    
    /**
     * 获取生产者统计信息
     * 
     * @return 统计信息
     */
    ProducerStats getStats();
    
    /**
     * 发送结果
     */
    interface SendResult {
        /**
         * 是否发送成功
         * 
         * @return 是否成功
         */
        boolean isSuccess();
        
        /**
         * 获取消息ID
         * 
         * @return 消息ID
         */
        String getMessageId();
        
        /**
         * 获取主题
         * 
         * @return 主题
         */
        String getTopic();
        
        /**
         * 获取队列
         * 
         * @return 队列
         */
        String getQueue();
        
        /**
         * 获取发送时间戳
         * 
         * @return 时间戳
         */
        long getTimestamp();
        
        /**
         * 获取错误消息
         * 
         * @return 错误消息
         */
        String getErrorMessage();
        
        /**
         * 获取异常
         * 
         * @return 异常
         */
        Throwable getException();
        
        /**
         * 获取发送耗时（毫秒）
         * 
         * @return 耗时
         */
        long getElapsedTime();
    }
    
    /**
     * 批量发送结果
     */
    interface BatchSendResult {
        /**
         * 是否全部发送成功
         * 
         * @return 是否全部成功
         */
        boolean isAllSuccess();
        
        /**
         * 获取成功数量
         * 
         * @return 成功数量
         */
        int getSuccessCount();
        
        /**
         * 获取失败数量
         * 
         * @return 失败数量
         */
        int getFailedCount();
        
        /**
         * 获取总数量
         * 
         * @return 总数量
         */
        int getTotalCount();
        
        /**
         * 获取所有发送结果
         * 
         * @return 发送结果列表
         */
        List<SendResult> getResults();
        
        /**
         * 获取失败的发送结果
         * 
         * @return 失败的发送结果列表
         */
        List<SendResult> getFailedResults();
        
        /**
         * 获取发送耗时（毫秒）
         * 
         * @return 耗时
         */
        long getElapsedTime();
    }
    
    /**
     * 事务回调接口
     */
    @FunctionalInterface
    interface TransactionCallback {
        /**
         * 执行本地事务
         * 
         * @param message 消息对象
         * @return 事务执行结果
         */
        TransactionResult executeLocalTransaction(Message<?> message);
    }
    
    /**
     * 事务执行结果
     */
    enum TransactionResult {
        /**
         * 提交事务
         */
        COMMIT,
        
        /**
         * 回滚事务
         */
        ROLLBACK,
        
        /**
         * 未知状态，需要回查
         */
        UNKNOWN
    }
    
    /**
     * 生产者统计信息
     */
    interface ProducerStats {
        /**
         * 获取已发送消息数量
         * 
         * @return 消息数量
         */
        long getSentCount();
        
        /**
         * 获取发送成功数量
         * 
         * @return 成功数量
         */
        long getSuccessCount();
        
        /**
         * 获取发送失败数量
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
         * 获取平均发送耗时（毫秒）
         * 
         * @return 平均耗时
         */
        double getAverageElapsedTime();
        
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

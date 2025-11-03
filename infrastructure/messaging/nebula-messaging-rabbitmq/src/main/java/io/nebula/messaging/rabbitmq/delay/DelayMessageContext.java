package io.nebula.messaging.rabbitmq.delay;

import lombok.Data;

/**
 * 延时消息上下文
 * 
 * 封装了延时消息在消费时的上下文信息
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
public class DelayMessageContext {
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 原始主题
     */
    private String originalTopic;
    
    /**
     * 原始队列
     */
    private String originalQueue;
    
    /**
     * 延时时间（毫秒）
     */
    private long delayMillis;
    
    /**
     * 预期处理时间戳
     */
    private long expectedTime;
    
    /**
     * 实际处理时间戳
     */
    private long actualTime;
    
    /**
     * 最大重试次数
     */
    private int maxRetries;
    
    /**
     * 当前重试次数
     */
    private int currentRetry;
    
    /**
     * 消息创建时间戳
     */
    private long timestamp;
    
    /**
     * 计算延时误差（毫秒）
     * 
     * @return 延时误差，正数表示延迟，负数表示提前
     */
    public long getDelayError() {
        this.actualTime = System.currentTimeMillis();
        return this.actualTime - this.expectedTime;
    }
    
    /**
     * 是否需要重试
     */
    public boolean needsRetry() {
        return currentRetry < maxRetries;
    }
    
    /**
     * 获取总延时时间（毫秒）
     * 从消息创建到实际处理的时间
     */
    public long getTotalDelay() {
        this.actualTime = System.currentTimeMillis();
        return this.actualTime - this.timestamp;
    }
}


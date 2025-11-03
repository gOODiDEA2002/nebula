package io.nebula.messaging.rabbitmq.delay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.Map;

/**
 * 延时消息模型
 * 
 * 封装了延时消息的所有关键信息，包括消息ID、消息体、延时时间、
 * 重试配置和自定义headers
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelayMessage<T> {
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 目标主题
     */
    private String topic;
    
    /**
     * 目标队列
     */
    private String queue;
    
    /**
     * 消息载荷
     */
    private T payload;
    
    /**
     * 延时时间
     */
    private Duration delay;
    
    /**
     * 最大重试次数（默认3次）
     */
    @Builder.Default
    private int maxRetries = 3;
    
    /**
     * 当前重试次数
     */
    @Builder.Default
    private int currentRetry = 0;
    
    /**
     * 重试间隔（默认1秒）
     */
    @Builder.Default
    private Duration retryInterval = Duration.ofSeconds(1);
    
    /**
     * 消息headers
     */
    private Map<String, String> headers;
    
    /**
     * 创建时间戳
     */
    @Builder.Default
    private long createTime = System.currentTimeMillis();
    
    /**
     * 预期处理时间戳（创建时间 + 延时时间）
     */
    private long expectedTime;
    
    /**
     * 是否持久化（默认true）
     */
    @Builder.Default
    private boolean persistent = true;
    
    /**
     * 消息优先级（0-9，数字越大优先级越高）
     */
    @Builder.Default
    private int priority = 5;
    
    /**
     * 判断是否可以重试
     * 
     * @return 是否可以重试
     */
    public boolean canRetry() {
        return currentRetry < maxRetries;
    }
    
    /**
     * 增加重试次数
     */
    public void incrementRetry() {
        this.currentRetry++;
    }
    
    /**
     * 计算预期处理时间
     */
    public void calculateExpectedTime() {
        this.expectedTime = this.createTime + (this.delay != null ? this.delay.toMillis() : 0);
    }
    
    /**
     * 获取剩余延时时间（毫秒）
     * 
     * @return 剩余延时时间，如果已到期返回0
     */
    public long getRemainingDelayMillis() {
        long remaining = this.expectedTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
    
    /**
     * 判断是否已到期
     * 
     * @return 是否已到期
     */
    public boolean isExpired() {
        return System.currentTimeMillis() >= this.expectedTime;
    }
}


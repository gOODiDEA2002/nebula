package io.nebula.messaging.rabbitmq.delay;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 延时消息发送结果
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@AllArgsConstructor
public class DelayMessageResult {
    
    /**
     * 是否发送成功
     */
    private boolean success;
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 发送时间戳
     */
    private long timestamp;
    
    /**
     * 发送耗时（毫秒）
     */
    private long elapsedTime;
    
    /**
     * 错误消息
     */
    private String errorMessage;
    
    /**
     * 异常信息
     */
    private Throwable exception;
    
    /**
     * 创建成功结果
     */
    public static DelayMessageResult success(String messageId, long timestamp, long elapsedTime) {
        return new DelayMessageResult(true, messageId, timestamp, elapsedTime, null, null);
    }
    
    /**
     * 创建失败结果
     */
    public static DelayMessageResult failure(String messageId, long timestamp, long elapsedTime, Throwable exception) {
        String errorMessage = exception != null ? exception.getMessage() : "Unknown error";
        return new DelayMessageResult(false, messageId, timestamp, elapsedTime, errorMessage, exception);
    }
}


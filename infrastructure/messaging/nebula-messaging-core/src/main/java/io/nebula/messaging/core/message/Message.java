package io.nebula.messaging.core.message;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 统一消息对象
 * 
 * @param <T> 消息载荷类型
 */
@Data
@Builder
public class Message<T> {
    
    /**
     * 消息ID（唯一标识）
     */
    private String id;
    
    /**
     * 消息主题/交换机
     */
    private String topic;
    
    /**
     * 消息队列/路由键
     */
    private String queue;
    
    /**
     * 消息标签（用于过滤）
     */
    private String tag;
    
    /**
     * 消息载荷
     */
    private T payload;
    
    /**
     * 消息头部信息
     */
    private Map<String, String> headers;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 发送时间
     */
    private LocalDateTime sendTime;
    
    /**
     * 消息优先级 (0-9, 数字越大优先级越高)
     */
    @Builder.Default
    private int priority = 5;
    
    /**
     * 重试次数
     */
    @Builder.Default
    private int retryCount = 0;
    
    /**
     * 最大重试次数
     */
    @Builder.Default
    private int maxRetryCount = 3;
    
    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
    
    /**
     * 延迟发送时间
     */
    private LocalDateTime delayTime;
    
    /**
     * 消息来源
     */
    private String source;
    
    /**
     * 消息目标
     */
    private String target;
    
    /**
     * 是否需要确认
     */
    @Builder.Default
    private boolean requireAck = false;
    
    /**
     * 是否持久化
     */
    @Builder.Default
    private boolean persistent = true;
    
    /**
     * 消息类型
     */
    private String messageType;
    
    /**
     * 消息状态
     */
    private String status;
    
    /**
     * 检查消息是否过期
     * 
     * @return true if expired
     */
    public boolean isExpired() {
        return expireTime != null && LocalDateTime.now().isAfter(expireTime);
    }
    
    /**
     * 检查是否可以重试
     * 
     * @return true if can retry
     */
    public boolean canRetry() {
        return retryCount < maxRetryCount;
    }
    
    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }
    
    /**
     * 获取消息大小（估算）
     * 
     * @return 字节数
     */
    public int getEstimatedSize() {
        if (payload == null) {
            return 0;
        }
        return payload.toString().getBytes().length;
    }
    
    /**
     * 静态工厂方法：创建简单消息
     * 
     * @param topic   主题
     * @param payload 载荷
     * @param <T>     载荷类型
     * @return 消息实例
     */
    public static <T> Message<T> of(String topic, T payload) {
        return Message.<T>builder()
                .topic(topic)
                .payload(payload)
                .createTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 静态工厂方法：创建带队列的消息
     * 
     * @param topic   主题
     * @param queue   队列
     * @param payload 载荷
     * @param <T>     载荷类型
     * @return 消息实例
     */
    public static <T> Message<T> of(String topic, String queue, T payload) {
        return Message.<T>builder()
                .topic(topic)
                .queue(queue)
                .payload(payload)
                .createTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 静态工厂方法：创建带标签的消息
     * 
     * @param topic   主题
     * @param queue   队列
     * @param tag     标签
     * @param payload 载荷
     * @param <T>     载荷类型
     * @return 消息实例
     */
    public static <T> Message<T> of(String topic, String queue, String tag, T payload) {
        return Message.<T>builder()
                .topic(topic)
                .queue(queue)
                .tag(tag)
                .payload(payload)
                .createTime(LocalDateTime.now())
                .build();
    }
}
package io.nebula.messaging.rabbitmq.delay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * RabbitMQ延时消息配置属性
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@ConfigurationProperties(prefix = "nebula.messaging.rabbitmq.delay-message")
public class RabbitDelayMessageProperties {
    
    /**
     * 是否启用延时消息功能
     * 默认启用
     */
    private boolean enabled = true;
    
    /**
     * 默认最大重试次数
     */
    private int defaultMaxRetries = 3;
    
    /**
     * 默认重试间隔
     */
    private Duration defaultRetryInterval = Duration.ofSeconds(1);
    
    /**
     * 延时队列最大TTL（毫秒）
     * 防止延时时间过长，默认7天
     */
    private long maxDelayMillis = Duration.ofDays(7).toMillis();
    
    /**
     * 延时队列最小TTL（毫秒）
     * 防止延时时间过短，默认1秒
     */
    private long minDelayMillis = Duration.ofSeconds(1).toMillis();
    
    /**
     * 是否自动创建延时交换机和队列
     * 默认true
     */
    private boolean autoCreateResources = true;
    
    /**
     * 是否启用死信队列
     * 默认true
     */
    private boolean enableDeadLetterQueue = true;
    
    /**
     * 死信队列配置
     */
    private DeadLetterQueue deadLetterQueue = new DeadLetterQueue();
    
    /**
     * 死信队列配置
     */
    @Data
    public static class DeadLetterQueue {
        
        /**
         * 死信交换机名称
         */
        private String exchange = "nebula.dlx.exchange";
        
        /**
         * 死信队列名称
         */
        private String queue = "nebula.dlx.queue";
        
        /**
         * 是否持久化
         */
        private boolean durable = true;
        
        /**
         * 是否自动删除
         */
        private boolean autoDelete = false;
    }
}


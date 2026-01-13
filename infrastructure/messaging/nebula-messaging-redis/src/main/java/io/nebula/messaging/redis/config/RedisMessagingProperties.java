package io.nebula.messaging.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 消息配置属性
 */
@Data
@ConfigurationProperties(prefix = "nebula.messaging.redis")
public class RedisMessagingProperties {

    /**
     * 是否启用 Redis 消息
     */
    private boolean enabled = true;

    /**
     * 频道前缀
     */
    private String channelPrefix = "nebula:";

    /**
     * 序列化方式: json, jdk
     */
    private String serializer = "json";

    /**
     * Pub/Sub 配置
     */
    private PubSubConfig pubsub = new PubSubConfig();

    /**
     * Stream 配置
     */
    private StreamConfig stream = new StreamConfig();

    /**
     * Pub/Sub 配置
     */
    @Data
    public static class PubSubConfig {
        /**
         * 监听线程池大小
         */
        private int listenerThreadPoolSize = 4;

        /**
         * 是否启用模式订阅
         */
        private boolean patternSubscriptionEnabled = true;
    }

    /**
     * Stream 配置
     */
    @Data
    public static class StreamConfig {
        /**
         * 是否启用 Stream
         */
        private boolean enabled = false;

        /**
         * 消费者组名称
         */
        private String consumerGroup = "nebula-consumer-group";

        /**
         * 消费者名称前缀
         */
        private String consumerNamePrefix = "consumer-";

        /**
         * 每次拉取的消息数量
         */
        private int batchSize = 10;

        /**
         * 拉取超时时间（毫秒）
         */
        private long pollTimeout = 1000;

        /**
         * 消息保留时间（毫秒），0表示不限制
         */
        private long maxLen = 0;
    }
}


package io.nebula.messaging.rocketmq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RocketMQ 配置属性
 *
 * <p>前缀: nebula.messaging.rocketmq</p>
 *
 * @author nebula
 */
@Data
@ConfigurationProperties(prefix = "nebula.messaging.rocketmq")
public class RocketMQProperties {

    /**
     * 是否启用 RocketMQ
     */
    private boolean enabled = false;

    /**
     * NameServer 地址，多个用分号分隔，如 127.0.0.1:9876;127.0.0.2:9876
     */
    private String nameServer = "127.0.0.1:9876";

    /**
     * ACL 访问密钥（可选，开启 ACL 的集群需要配置）
     */
    private String accessKey;

    /**
     * ACL 访问密钥 Secret（可选）
     */
    private String secretKey;

    /**
     * 生产者配置
     */
    private Producer producer = new Producer();

    /**
     * 消费者配置
     */
    private Consumer consumer = new Consumer();

    /**
     * 生产者配置
     */
    @Data
    public static class Producer {

        /**
         * 生产者组名
         */
        private String group = "nebula-producer-group";

        /**
         * 发送超时时间（毫秒）
         */
        private int sendTimeout = 10000;

        /**
         * 同步发送失败重试次数
         */
        private int retryTimesWhenSendFailed = 2;

        /**
         * 异步发送失败重试次数
         */
        private int retryTimesWhenSendAsyncFailed = 2;

        /**
         * 消息体最大大小（字节），默认 4MB
         */
        private int maxMessageSize = 4 * 1024 * 1024;
    }

    /**
     * 消费者配置
     */
    @Data
    public static class Consumer {

        /**
         * 消费者组名基础前缀（实际组名为 {group}-{topic}[-{tag}]，避免同组订阅关系不一致）
         */
        private String group = "nebula-consumer-group";

        /**
         * 最小消费线程数
         */
        private int consumeThreadMin = 4;

        /**
         * 最大消费线程数
         */
        private int consumeThreadMax = 16;

        /**
         * 最大重试消费次数（超过后进入死信队列 %DLQ%{group}）
         */
        private int maxReconsumeTimes = 3;

        /**
         * 单条消息消费超时时间（分钟），RocketMQ 客户端粒度为分钟
         */
        private long consumeTimeoutMinutes = 15;

        /**
         * 单次批量消费最大消息数
         */
        private int consumeMessageBatchMaxSize = 1;

        /**
         * 是否广播消费模式（默认集群模式）
         */
        private boolean broadcasting = false;
    }
}

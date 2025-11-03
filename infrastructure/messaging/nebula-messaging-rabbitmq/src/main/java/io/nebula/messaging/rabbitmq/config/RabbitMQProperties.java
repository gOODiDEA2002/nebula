package io.nebula.messaging.rabbitmq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RabbitMQ配置属性
 */
@ConfigurationProperties(prefix = "nebula.messaging.rabbitmq")
public class RabbitMQProperties {
    
    /**
     * 是否启用RabbitMQ
     */
    private boolean enabled = true;
    
    /**
     * RabbitMQ服务器地址
     */
    private String host = "localhost";
    
    /**
     * RabbitMQ服务器端口
     */
    private int port = 5672;
    
    /**
     * 用户名
     */
    private String username = "guest";
    
    /**
     * 密码
     */
    private String password = "guest";
    
    /**
     * 虚拟主机
     */
    private String virtualHost = "/";
    
    /**
     * 连接超时时间（毫秒）
     */
    private int connectionTimeout = 60000;
    
    /**
     * 心跳间隔（秒）
     */
    private int heartbeat = 60;
    
    /**
     * 是否启用自动恢复
     */
    private boolean automaticRecovery = true;
    
    /**
     * 网络恢复间隔（毫秒）
     */
    private long networkRecoveryInterval = 5000;
    
    /**
     * 消费者配置
     */
    private Consumer consumer = new Consumer();
    
    /**
     * 生产者配置
     */
    private Producer producer = new Producer();
    
    /**
     * Exchange配置
     */
    private Exchange exchange = new Exchange();
    
    /**
     * 延时消息配置
     */
    private DelayMessage delayMessage = new DelayMessage();
    
    // Getter and Setter methods
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getVirtualHost() {
        return virtualHost;
    }
    
    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }
    
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    public int getHeartbeat() {
        return heartbeat;
    }
    
    public void setHeartbeat(int heartbeat) {
        this.heartbeat = heartbeat;
    }
    
    public boolean isAutomaticRecovery() {
        return automaticRecovery;
    }
    
    public void setAutomaticRecovery(boolean automaticRecovery) {
        this.automaticRecovery = automaticRecovery;
    }
    
    public long getNetworkRecoveryInterval() {
        return networkRecoveryInterval;
    }
    
    public void setNetworkRecoveryInterval(long networkRecoveryInterval) {
        this.networkRecoveryInterval = networkRecoveryInterval;
    }
    
    public Consumer getConsumer() {
        return consumer;
    }
    
    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }
    
    public Producer getProducer() {
        return producer;
    }
    
    public void setProducer(Producer producer) {
        this.producer = producer;
    }
    
    public Exchange getExchange() {
        return exchange;
    }
    
    public void setExchange(Exchange exchange) {
        this.exchange = exchange;
    }
    
    public DelayMessage getDelayMessage() {
        return delayMessage;
    }
    
    public void setDelayMessage(DelayMessage delayMessage) {
        this.delayMessage = delayMessage;
    }
    
    /**
     * 消费者配置
     */
    public static class Consumer {
        /**
         * 预取数量
         */
        private int prefetchCount = 1;
        
        /**
         * 是否自动确认
         */
        private boolean autoAck = false;
        
        /**
         * 重试次数
         */
        private int retryCount = 3;
        
        /**
         * 重试间隔（毫秒）
         */
        private long retryInterval = 1000;
        
        public int getPrefetchCount() {
            return prefetchCount;
        }
        
        public void setPrefetchCount(int prefetchCount) {
            this.prefetchCount = prefetchCount;
        }
        
        public boolean isAutoAck() {
            return autoAck;
        }
        
        public void setAutoAck(boolean autoAck) {
            this.autoAck = autoAck;
        }
        
        public int getRetryCount() {
            return retryCount;
        }
        
        public void setRetryCount(int retryCount) {
            this.retryCount = retryCount;
        }
        
        public long getRetryInterval() {
            return retryInterval;
        }
        
        public void setRetryInterval(long retryInterval) {
            this.retryInterval = retryInterval;
        }
    }
    
    /**
     * 生产者配置
     */
    public static class Producer {
        /**
         * 是否启用发送确认
         */
        private boolean publisherConfirms = true;
        
        /**
         * 确认超时时间（毫秒）
         */
        private long confirmTimeout = 5000;
        
        /**
         * 是否启用发送回调
         */
        private boolean publisherReturns = true;
        
        public boolean isPublisherConfirms() {
            return publisherConfirms;
        }
        
        public void setPublisherConfirms(boolean publisherConfirms) {
            this.publisherConfirms = publisherConfirms;
        }
        
        public long getConfirmTimeout() {
            return confirmTimeout;
        }
        
        public void setConfirmTimeout(long confirmTimeout) {
            this.confirmTimeout = confirmTimeout;
        }
        
        public boolean isPublisherReturns() {
            return publisherReturns;
        }
        
        public void setPublisherReturns(boolean publisherReturns) {
            this.publisherReturns = publisherReturns;
        }
    }
    
    /**
     * Exchange配置
     */
    public static class Exchange {
        /**
         * 默认Exchange类型
         */
        private String defaultType = "topic";
        
        /**
         * 是否持久化
         */
        private boolean durable = true;
        
        /**
         * 是否自动删除
         */
        private boolean autoDelete = false;
        
        public String getDefaultType() {
            return defaultType;
        }
        
        public void setDefaultType(String defaultType) {
            this.defaultType = defaultType;
        }
        
        public boolean isDurable() {
            return durable;
        }
        
        public void setDurable(boolean durable) {
            this.durable = durable;
        }
        
        public boolean isAutoDelete() {
            return autoDelete;
        }
        
        public void setAutoDelete(boolean autoDelete) {
            this.autoDelete = autoDelete;
        }
    }
    
    /**
     * 延时消息配置
     */
    public static class DelayMessage {
        /**
         * 是否启用延时消息功能
         */
        private boolean enabled = true;
        
        /**
         * 默认最大重试次数
         */
        private int defaultMaxRetries = 3;
        
        /**
         * 默认重试间隔(毫秒)
         */
        private long defaultRetryInterval = 1000;
        
        /**
         * 延时队列最大TTL(毫秒), 默认7天
         */
        private long maxDelayMillis = 7 * 24 * 60 * 60 * 1000L;
        
        /**
         * 延时队列最小TTL(毫秒), 默认1秒
         */
        private long minDelayMillis = 1000;
        
        /**
         * 是否自动创建延时交换机和队列
         */
        private boolean autoCreateResources = true;
        
        /**
         * 是否启用死信队列
         */
        private boolean enableDeadLetterQueue = true;
        
        /**
         * 死信交换机名称
         */
        private String deadLetterExchange = "nebula.dlx.exchange";
        
        /**
         * 死信队列名称
         */
        private String deadLetterQueue = "nebula.dlx.queue";
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public int getDefaultMaxRetries() {
            return defaultMaxRetries;
        }
        
        public void setDefaultMaxRetries(int defaultMaxRetries) {
            this.defaultMaxRetries = defaultMaxRetries;
        }
        
        public long getDefaultRetryInterval() {
            return defaultRetryInterval;
        }
        
        public void setDefaultRetryInterval(long defaultRetryInterval) {
            this.defaultRetryInterval = defaultRetryInterval;
        }
        
        public long getMaxDelayMillis() {
            return maxDelayMillis;
        }
        
        public void setMaxDelayMillis(long maxDelayMillis) {
            this.maxDelayMillis = maxDelayMillis;
        }
        
        public long getMinDelayMillis() {
            return minDelayMillis;
        }
        
        public void setMinDelayMillis(long minDelayMillis) {
            this.minDelayMillis = minDelayMillis;
        }
        
        public boolean isAutoCreateResources() {
            return autoCreateResources;
        }
        
        public void setAutoCreateResources(boolean autoCreateResources) {
            this.autoCreateResources = autoCreateResources;
        }
        
        public boolean isEnableDeadLetterQueue() {
            return enableDeadLetterQueue;
        }
        
        public void setEnableDeadLetterQueue(boolean enableDeadLetterQueue) {
            this.enableDeadLetterQueue = enableDeadLetterQueue;
        }
        
        public String getDeadLetterExchange() {
            return deadLetterExchange;
        }
        
        public void setDeadLetterExchange(String deadLetterExchange) {
            this.deadLetterExchange = deadLetterExchange;
        }
        
        public String getDeadLetterQueue() {
            return deadLetterQueue;
        }
        
        public void setDeadLetterQueue(String deadLetterQueue) {
            this.deadLetterQueue = deadLetterQueue;
        }
    }
}


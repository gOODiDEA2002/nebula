package io.nebula.messaging.redis.config;

import io.nebula.messaging.redis.RedisMessageManager;
import io.nebula.messaging.redis.annotation.RedisMessageHandlerProcessor;
import io.nebula.messaging.redis.consumer.RedisMessageConsumer;
import io.nebula.messaging.redis.producer.RedisMessageProducer;
import io.nebula.messaging.redis.stream.RedisStreamConsumer;
import io.nebula.messaging.redis.stream.RedisStreamProducer;
import io.nebula.messaging.redis.support.RedisMessageSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * Redis 消息自动配置
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({StringRedisTemplate.class, RedisConnectionFactory.class})
@ConditionalOnProperty(prefix = "nebula.messaging.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(RedisMessagingProperties.class)
@RequiredArgsConstructor
public class RedisMessagingAutoConfiguration {

    private final RedisMessagingProperties properties;

    /**
     * Redis 消息序列化器
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisMessageSerializer redisMessageSerializer() {
        return new RedisMessageSerializer();
    }

    /**
     * Redis 消息监听容器（Pub/Sub）
     */
    @Bean
    @ConditionalOnMissingBean(name = "redisMessageListenerContainer")
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 配置线程池
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getPubsub().getListenerThreadPoolSize());
        executor.setMaxPoolSize(properties.getPubsub().getListenerThreadPoolSize() * 2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("redis-msg-listener-");
        executor.initialize();
        container.setTaskExecutor(executor);

        return container;
    }

    /**
     * Redis Pub/Sub 消息生产者
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisMessageProducer<Object> redisMessageProducer(StringRedisTemplate redisTemplate,
                                                              RedisMessageSerializer serializer) {
        log.info("初始化 Redis Pub/Sub 消息生产者");
        return new RedisMessageProducer<>(redisTemplate, properties, serializer);
    }

    /**
     * Redis Pub/Sub 消息消费者
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisMessageConsumer<Object> redisMessageConsumer(RedisMessageListenerContainer listenerContainer,
                                                              RedisMessageSerializer serializer) {
        log.info("初始化 Redis Pub/Sub 消息消费者");
        return new RedisMessageConsumer<>(listenerContainer, properties, serializer);
    }

    /**
     * 异步执行器（用于消息处理）
     */
    @Bean(name = "redisMessageExecutor")
    @ConditionalOnMissingBean(name = "redisMessageExecutor")
    public Executor redisMessageExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("redis-msg-handler-");
        executor.initialize();
        return executor;
    }

    /**
     * Redis 消息处理器注解处理器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(RedisMessageConsumer.class)
    public RedisMessageHandlerProcessor redisMessageHandlerProcessor(RedisMessageConsumer<?> messageConsumer,
                                                                      Executor redisMessageExecutor) {
        log.info("初始化 Redis 消息处理器注解处理器");
        return new RedisMessageHandlerProcessor(messageConsumer, redisMessageExecutor);
    }

    /**
     * Redis 消息管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisMessageManager redisMessageManager(RedisMessageSerializer serializer,
                                                    RedisMessageProducer<Object> pubsubProducer,
                                                    RedisMessageConsumer<Object> pubsubConsumer) {
        log.info("初始化 Redis 消息管理器");
        return new RedisMessageManager(properties, serializer, pubsubProducer, pubsubConsumer);
    }

    // ========== Stream 配置 ==========

    /**
     * Redis Stream 配置
     */
    @AutoConfiguration
    @ConditionalOnProperty(prefix = "nebula.messaging.redis.stream", name = "enabled", havingValue = "true")
    public class RedisStreamAutoConfiguration {

        /**
         * Stream 消息监听容器
         */
        @Bean
        @ConditionalOnMissingBean
        public StreamMessageListenerContainer<String, ObjectRecord<String, String>> streamMessageListenerContainer(
                RedisConnectionFactory connectionFactory) {

            StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, String>> options =
                    StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                            .batchSize(properties.getStream().getBatchSize())
                            .pollTimeout(Duration.ofMillis(properties.getStream().getPollTimeout()))
                            .targetType(String.class)
                            .build();

            StreamMessageListenerContainer<String, ObjectRecord<String, String>> container =
                    StreamMessageListenerContainer.create(connectionFactory, options);

            return container;
        }

        /**
         * Redis Stream 消息生产者
         */
        @Bean
        @ConditionalOnMissingBean
        public RedisStreamProducer<Object> redisStreamProducer(StringRedisTemplate redisTemplate,
                                                                RedisMessageSerializer serializer) {
            log.info("初始化 Redis Stream 消息生产者");
            return new RedisStreamProducer<>(redisTemplate, properties, serializer);
        }

        /**
         * Redis Stream 消息消费者
         */
        @Bean
        @ConditionalOnMissingBean
        public RedisStreamConsumer<Object> redisStreamConsumer(StringRedisTemplate redisTemplate,
                                                                RedisMessageSerializer serializer,
                                                                StreamMessageListenerContainer<String, ObjectRecord<String, String>> listenerContainer,
                                                                Executor redisMessageExecutor) {
            log.info("初始化 Redis Stream 消息消费者, consumerGroup={}", properties.getStream().getConsumerGroup());
            return new RedisStreamConsumer<>(redisTemplate, properties, serializer, listenerContainer, redisMessageExecutor);
        }

        /**
         * 配置 Stream 到消息管理器
         */
        @Bean
        @ConditionalOnBean({RedisMessageManager.class, RedisStreamProducer.class, RedisStreamConsumer.class})
        public Object configureStreamToManager(RedisMessageManager manager,
                                               RedisStreamProducer<Object> streamProducer,
                                               RedisStreamConsumer<Object> streamConsumer) {
            manager.setStreamProducer(streamProducer);
            manager.setStreamConsumer(streamConsumer);
            log.info("Stream 已配置到消息管理器");
            return new Object(); // 返回一个占位 Bean
        }
    }
}


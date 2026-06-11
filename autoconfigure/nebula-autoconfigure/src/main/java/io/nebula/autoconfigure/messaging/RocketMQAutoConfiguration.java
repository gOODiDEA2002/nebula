package io.nebula.autoconfigure.messaging;

import io.nebula.core.common.diagnostic.NebulaComponentSummary;
import io.nebula.core.common.diagnostic.SimpleComponentSummary;
import io.nebula.messaging.core.annotation.MessageHandlerProcessor;
import io.nebula.messaging.core.router.DefaultMessageRouter;
import io.nebula.messaging.core.router.MessageRouter;
import io.nebula.messaging.core.serializer.JsonMessageSerializer;
import io.nebula.messaging.core.serializer.MessageSerializer;
import io.nebula.messaging.rocketmq.config.RocketMQProperties;
import io.nebula.messaging.rocketmq.consumer.RocketMQMessageConsumer;
import io.nebula.messaging.rocketmq.manager.RocketMQMessageManager;
import io.nebula.messaging.rocketmq.producer.RocketMQMessageProducer;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

/**
 * RocketMQ自动配置
 */
@Configuration
@ConditionalOnClass(DefaultMQProducer.class)
@ConditionalOnProperty(prefix = "nebula.messaging.rocketmq", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(RocketMQProperties.class)
public class RocketMQAutoConfiguration {

    private final RocketMQProperties properties;

    public RocketMQAutoConfiguration(RocketMQProperties properties) {
        this.properties = properties;
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public DefaultMQProducer rocketMQNativeProducer() throws Exception {
        DefaultMQProducer producer;
        if (StringUtils.hasText(properties.getAccessKey()) && StringUtils.hasText(properties.getSecretKey())) {
            producer = new DefaultMQProducer(properties.getProducer().getGroup(),
                    new AclClientRPCHook(new SessionCredentials(properties.getAccessKey(), properties.getSecretKey())));
        } else {
            producer = new DefaultMQProducer(properties.getProducer().getGroup());
        }
        producer.setNamesrvAddr(properties.getNameServer());
        producer.setSendMsgTimeout(properties.getProducer().getSendTimeout());
        producer.setRetryTimesWhenSendFailed(properties.getProducer().getRetryTimesWhenSendFailed());
        producer.setRetryTimesWhenSendAsyncFailed(properties.getProducer().getRetryTimesWhenSendAsyncFailed());
        producer.setMaxMessageSize(properties.getProducer().getMaxMessageSize());
        producer.start();
        return producer;
    }

    @Bean
    @ConditionalOnMissingBean
    @Primary
    public MessageSerializer messageSerializer() {
        return new JsonMessageSerializer();
    }

    @Bean
    @ConditionalOnMissingBean
    @Primary
    public MessageRouter messageRouter() {
        return new DefaultMessageRouter();
    }

    @Bean
    @ConditionalOnMissingBean
    public RocketMQMessageProducer<?> rocketMQMessageProducer(DefaultMQProducer rocketMQNativeProducer,
            MessageSerializer messageSerializer) {
        return new RocketMQMessageProducer<>(rocketMQNativeProducer, messageSerializer, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public RocketMQMessageConsumer<?> rocketMQMessageConsumer(MessageSerializer messageSerializer) {
        return new RocketMQMessageConsumer<>(messageSerializer, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @Primary
    public RocketMQMessageManager rocketMQMessageManager(RocketMQMessageProducer<?> producer,
            RocketMQMessageConsumer<?> consumer,
            DefaultMQProducer rocketMQNativeProducer) throws Exception {
        RocketMQMessageManager manager = new RocketMQMessageManager(producer, consumer, rocketMQNativeProducer);
        manager.start();
        return manager;
    }

    @Bean
    @ConditionalOnMissingBean
    public static MessageHandlerProcessor messageHandlerProcessor(@Lazy RocketMQMessageManager messageManager) {
        return new MessageHandlerProcessor(messageManager);
    }

    /**
     * 组件摘要: RocketMQ
     */
    @Bean
    NebulaComponentSummary rocketMqSummary() {
        var details = new java.util.LinkedHashMap<String, String>();
        details.put("NameServer", properties.getNameServer());
        details.put("Producer Group", properties.getProducer().getGroup());
        details.put("Consumer Group", properties.getConsumer().getGroup());
        details.put("Send Timeout", properties.getProducer().getSendTimeout() + "ms");
        details.put("Max Reconsume Times", String.valueOf(properties.getConsumer().getMaxReconsumeTimes()));
        details.put("ACL", StringUtils.hasText(properties.getAccessKey()) ? "ENABLED" : "DISABLED");

        return new SimpleComponentSummary("Messaging", "RocketMQ", true, 500, details);
    }
}

package io.nebula.messaging.rabbitmq.delay;

import com.rabbitmq.client.Connection;
import io.nebula.messaging.core.serializer.MessageSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ延时消息配置
 * 
 * 配置延时消息所需的交换机、队列、绑定关系等
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@Configuration
@ConditionalOnClass({Connection.class, RabbitTemplate.class})
@ConditionalOnProperty(prefix = "nebula.messaging.rabbitmq.delay-message", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitDelayMessageConfig {
    
    /**
     * 配置延时消息生产者
     */
    @Bean
    @ConditionalOnClass(Connection.class)
    public DelayMessageProducer delayMessageProducer(
            Connection connection,
            MessageSerializer messageSerializer) {
        log.info("Initializing DelayMessageProducer");
        return new DelayMessageProducer(connection, messageSerializer);
    }
    
    /**
     * 配置延时消息消费者
     */
    @Bean
    @ConditionalOnClass(Connection.class)
    public DelayMessageConsumer delayMessageConsumer(
            Connection connection,
            MessageSerializer messageSerializer) {
        log.info("Initializing DelayMessageConsumer");
        return new DelayMessageConsumer(connection, messageSerializer);
    }
    
    /**
     * 配置消息转换器
     * 使用Jackson2进行JSON序列化
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    /**
     * 配置RabbitTemplate
     * 启用发布者确认和返回
     */
    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        
        // 启用发布者确认
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("Message sent successfully: correlationData={}", correlationData);
            } else {
                log.error("Message send failed: correlationData={}, cause={}", correlationData, cause);
            }
        });
        
        // 启用消息返回（当消息无法路由时触发）
        rabbitTemplate.setReturnsCallback(returnedMessage -> {
            log.error("Message returned: exchange={}, routingKey={}, replyCode={}, replyText={}",
                    returnedMessage.getExchange(),
                    returnedMessage.getRoutingKey(),
                    returnedMessage.getReplyCode(),
                    returnedMessage.getReplyText());
        });
        
        // 启用强制模式，确保消息返回回调生效
        rabbitTemplate.setMandatory(true);
        
        log.info("RabbitTemplate configured with delay message support");
        return rabbitTemplate;
    }
}


package io.nebula.autoconfigure.messaging;

import io.nebula.messaging.core.serializer.MessageSerializer;
import io.nebula.messaging.core.serializer.JsonMessageSerializer;
import io.nebula.messaging.core.router.MessageRouter;
import io.nebula.messaging.core.router.DefaultMessageRouter;
import io.nebula.messaging.core.annotation.MessageHandlerProcessor;
import io.nebula.messaging.rabbitmq.config.RabbitMQProperties;
import io.nebula.messaging.rabbitmq.producer.RabbitMQMessageProducer;
import io.nebula.messaging.rabbitmq.consumer.RabbitMQMessageConsumer;
import io.nebula.messaging.rabbitmq.exchange.RabbitMQExchangeManager;
import io.nebula.messaging.rabbitmq.manager.RabbitMQMessageManager;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

/**
 * RabbitMQ自动配置
 */
@Configuration
@ConditionalOnClass({ConnectionFactory.class, Connection.class})
@ConditionalOnProperty(prefix = "nebula.messaging.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(RabbitMQProperties.class)
public class RabbitMQAutoConfiguration {
    
    private final RabbitMQProperties properties;
    
    public RabbitMQAutoConfiguration(RabbitMQProperties properties) {
        this.properties = properties;
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ConnectionFactory rabbitMQConnectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(properties.getHost());
        factory.setPort(properties.getPort());
        factory.setUsername(properties.getUsername());
        factory.setPassword(properties.getPassword());
        factory.setVirtualHost(properties.getVirtualHost());
        
        // 连接池配置
        factory.setConnectionTimeout(properties.getConnectionTimeout());
        factory.setRequestedHeartbeat(properties.getHeartbeat());
        factory.setAutomaticRecoveryEnabled(properties.isAutomaticRecovery());
        factory.setNetworkRecoveryInterval(properties.getNetworkRecoveryInterval());
        
        return factory;
    }
    
    @Bean
    @ConditionalOnMissingBean
    public Connection rabbitMQConnection(ConnectionFactory connectionFactory) throws Exception {
        return connectionFactory.newConnection();
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
    public RabbitMQExchangeManager rabbitMQExchangeManager(Connection connection) {
        return new RabbitMQExchangeManager(connection);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public RabbitMQMessageProducer rabbitMQMessageProducer(Connection connection, MessageSerializer messageSerializer) {
        return new RabbitMQMessageProducer(connection, messageSerializer);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public RabbitMQMessageConsumer rabbitMQMessageConsumer(Connection connection, 
                                                          MessageSerializer messageSerializer,
                                                          MessageRouter messageRouter) {
        return new RabbitMQMessageConsumer(connection, messageSerializer, messageRouter);
    }
    
    
    @Bean
    @ConditionalOnMissingBean
    @Primary
    public RabbitMQMessageManager rabbitMQMessageManager(RabbitMQMessageProducer producer,
                                                        RabbitMQMessageConsumer consumer,
                                                        RabbitMQExchangeManager exchangeManager) {
        return new RabbitMQMessageManager(producer, consumer, exchangeManager);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public static MessageHandlerProcessor messageHandlerProcessor(@Lazy RabbitMQMessageManager messageManager) {
        return new MessageHandlerProcessor(messageManager);
    }
}

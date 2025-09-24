package io.nebula.messaging.rabbitmq.exchange;

import io.nebula.messaging.core.exception.MessagingException;
import io.nebula.messaging.core.exception.MessageConnectionException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RabbitMQ Exchange管理器
 * 负责Exchange的创建、删除和管理
 */
@Component
public class RabbitMQExchangeManager {
    
    private static final Logger log = LoggerFactory.getLogger(RabbitMQExchangeManager.class);
    
    private final Connection connection;
    private final Set<String> declaredExchanges = ConcurrentHashMap.newKeySet();
    
    @Autowired
    public RabbitMQExchangeManager(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * 声明Exchange
     * 
     * @param exchangeName Exchange名称
     * @param exchangeType Exchange类型 (direct, topic, fanout, headers)
     * @param durable 是否持久化
     * @param autoDelete 是否自动删除
     * @param arguments 额外参数
     */
    public void declareExchange(String exchangeName, String exchangeType, 
                              boolean durable, boolean autoDelete, Map<String, Object> arguments) {
        try (Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(exchangeName, exchangeType, durable, autoDelete, arguments);
            declaredExchanges.add(exchangeName);
            
            log.info("声明Exchange成功: name={}, type={}, durable={}, autoDelete={}", 
                    exchangeName, exchangeType, durable, autoDelete);
            
        } catch (Exception e) {
            log.error("声明Exchange失败: name={}, type={}", exchangeName, exchangeType, e);
            throw new MessageConnectionException("声明Exchange失败: " + exchangeName, e);
        }
    }
    
    /**
     * 声明Topic Exchange
     * 
     * @param exchangeName Exchange名称
     */
    public void declareTopicExchange(String exchangeName) {
        declareExchange(exchangeName, "topic", true, false, null);
    }
    
    /**
     * 声明Direct Exchange
     * 
     * @param exchangeName Exchange名称
     */
    public void declareDirectExchange(String exchangeName) {
        declareExchange(exchangeName, "direct", true, false, null);
    }
    
    /**
     * 声明Fanout Exchange
     * 
     * @param exchangeName Exchange名称
     */
    public void declareFanoutExchange(String exchangeName) {
        declareExchange(exchangeName, "fanout", true, false, null);
    }
    
    /**
     * 声明Headers Exchange
     * 
     * @param exchangeName Exchange名称
     */
    public void declareHeadersExchange(String exchangeName) {
        declareExchange(exchangeName, "headers", true, false, null);
    }
    
    /**
     * 删除Exchange
     * 
     * @param exchangeName Exchange名称
     * @param ifUnused 仅在未使用时删除
     */
    public void deleteExchange(String exchangeName, boolean ifUnused) {
        try (Channel channel = connection.createChannel()) {
            channel.exchangeDelete(exchangeName, ifUnused);
            declaredExchanges.remove(exchangeName);
            
            log.info("删除Exchange成功: name={}, ifUnused={}", exchangeName, ifUnused);
            
        } catch (Exception e) {
            log.error("删除Exchange失败: name={}", exchangeName, e);
            throw new MessageConnectionException("删除Exchange失败: " + exchangeName, e);
        }
    }
    
    /**
     * 绑定Exchange
     * 
     * @param destination 目标Exchange
     * @param source 源Exchange
     * @param routingKey 路由键
     * @param arguments 绑定参数
     */
    public void bindExchange(String destination, String source, String routingKey, Map<String, Object> arguments) {
        try (Channel channel = connection.createChannel()) {
            channel.exchangeBind(destination, source, routingKey, arguments);
            
            log.info("绑定Exchange成功: destination={}, source={}, routingKey={}", 
                    destination, source, routingKey);
            
        } catch (Exception e) {
            log.error("绑定Exchange失败: destination={}, source={}, routingKey={}", 
                    destination, source, routingKey, e);
            throw new MessageConnectionException(
                    String.format("绑定Exchange失败: %s -> %s", source, destination), e);
        }
    }
    
    /**
     * 解绑Exchange
     * 
     * @param destination 目标Exchange
     * @param source 源Exchange
     * @param routingKey 路由键
     * @param arguments 绑定参数
     */
    public void unbindExchange(String destination, String source, String routingKey, Map<String, Object> arguments) {
        try (Channel channel = connection.createChannel()) {
            channel.exchangeUnbind(destination, source, routingKey, arguments);
            
            log.info("解绑Exchange成功: destination={}, source={}, routingKey={}", 
                    destination, source, routingKey);
            
        } catch (Exception e) {
            log.error("解绑Exchange失败: destination={}, source={}, routingKey={}", 
                    destination, source, routingKey, e);
            throw new MessageConnectionException(
                    String.format("解绑Exchange失败: %s -> %s", source, destination), e);
        }
    }
    
    /**
     * 声明队列
     * 
     * @param queueName 队列名称
     * @param durable 是否持久化
     * @param exclusive 是否独占
     * @param autoDelete 是否自动删除
     * @param arguments 额外参数
     * @return 实际的队列名称
     */
    public String declareQueue(String queueName, boolean durable, boolean exclusive, 
                             boolean autoDelete, Map<String, Object> arguments) {
        try (Channel channel = connection.createChannel()) {
            String actualQueueName = channel.queueDeclare(queueName, durable, exclusive, autoDelete, arguments)
                    .getQueue();
            
            log.info("声明队列成功: name={}, actualName={}, durable={}, exclusive={}, autoDelete={}", 
                    queueName, actualQueueName, durable, exclusive, autoDelete);
            
            return actualQueueName;
            
        } catch (Exception e) {
            log.error("声明队列失败: name={}", queueName, e);
            throw new MessageConnectionException("声明队列失败: " + queueName, e);
        }
    }
    
    /**
     * 绑定队列到Exchange
     * 
     * @param queueName 队列名称
     * @param exchangeName Exchange名称
     * @param routingKey 路由键
     * @param arguments 绑定参数
     */
    public void bindQueue(String queueName, String exchangeName, String routingKey, Map<String, Object> arguments) {
        try (Channel channel = connection.createChannel()) {
            channel.queueBind(queueName, exchangeName, routingKey, arguments);
            
            log.info("绑定队列成功: queue={}, exchange={}, routingKey={}", 
                    queueName, exchangeName, routingKey);
            
        } catch (Exception e) {
            log.error("绑定队列失败: queue={}, exchange={}, routingKey={}", 
                    queueName, exchangeName, routingKey, e);
            throw new MessageConnectionException(
                    String.format("绑定队列失败: %s -> %s", queueName, exchangeName), e);
        }
    }
    
    /**
     * 解绑队列
     * 
     * @param queueName 队列名称
     * @param exchangeName Exchange名称
     * @param routingKey 路由键
     * @param arguments 绑定参数
     */
    public void unbindQueue(String queueName, String exchangeName, String routingKey, Map<String, Object> arguments) {
        try (Channel channel = connection.createChannel()) {
            channel.queueUnbind(queueName, exchangeName, routingKey, arguments);
            
            log.info("解绑队列成功: queue={}, exchange={}, routingKey={}", 
                    queueName, exchangeName, routingKey);
            
        } catch (Exception e) {
            log.error("解绑队列失败: queue={}, exchange={}, routingKey={}", 
                    queueName, exchangeName, routingKey, e);
            throw new MessageConnectionException(
                    String.format("解绑队列失败: %s -> %s", queueName, exchangeName), e);
        }
    }
    
    /**
     * 删除队列
     * 
     * @param queueName 队列名称
     * @param ifUnused 仅在未使用时删除
     * @param ifEmpty 仅在为空时删除
     */
    public void deleteQueue(String queueName, boolean ifUnused, boolean ifEmpty) {
        try (Channel channel = connection.createChannel()) {
            channel.queueDelete(queueName, ifUnused, ifEmpty);
            
            log.info("删除队列成功: name={}, ifUnused={}, ifEmpty={}", queueName, ifUnused, ifEmpty);
            
        } catch (Exception e) {
            log.error("删除队列失败: name={}", queueName, e);
            throw new MessageConnectionException("删除队列失败: " + queueName, e);
        }
    }
    
    /**
     * 检查Exchange是否已声明
     * 
     * @param exchangeName Exchange名称
     * @return 是否已声明
     */
    public boolean isExchangeDeclared(String exchangeName) {
        return declaredExchanges.contains(exchangeName);
    }
    
    /**
     * 获取所有已声明的Exchange
     * 
     * @return Exchange名称集合
     */
    public Set<String> getDeclaredExchanges() {
        return Set.copyOf(declaredExchanges);
    }
}

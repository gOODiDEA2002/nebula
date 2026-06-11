package io.nebula.messaging.rocketmq.manager;

import io.nebula.messaging.core.consumer.MessageConsumer;
import io.nebula.messaging.core.consumer.MessageHandler;
import io.nebula.messaging.core.manager.MessageManager;
import io.nebula.messaging.core.producer.MessageProducer;
import io.nebula.messaging.rocketmq.consumer.RocketMQMessageConsumer;
import io.nebula.messaging.rocketmq.producer.RocketMQMessageProducer;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RocketMQ 消息管理器实现
 * 统一管理生产者和消费者
 *
 * <p>主题管理说明：RocketMQ 的 Topic 属于运维资源，生产环境通常关闭 autoCreateTopicEnable，
 * 由控制台或 mqadmin 工具创建。createTopic 依赖 Broker 开启自动建题，deleteTopic 客户端不支持。</p>
 *
 * @author nebula
 */
public class RocketMQMessageManager implements MessageManager {

    private static final Logger log = LoggerFactory.getLogger(RocketMQMessageManager.class);

    private final RocketMQMessageProducer<?> producer;
    private final RocketMQMessageConsumer<?> consumer;
    private final DefaultMQProducer nativeProducer;
    private boolean running = false;

    public RocketMQMessageManager(RocketMQMessageProducer<?> producer,
                                  RocketMQMessageConsumer<?> consumer,
                                  DefaultMQProducer nativeProducer) {
        this.producer = producer;
        this.consumer = consumer;
        this.nativeProducer = nativeProducer;
    }

    @Override
    public MessageProducer getProducer() {
        return producer;
    }

    @Override
    public MessageConsumer getConsumer() {
        return consumer;
    }

    @Override
    public void start() throws Exception {
        if (!running) {
            producer.start();
            consumer.start();
            running = true;
            log.info("RocketMQ消息管理器启动成功");
        }
    }

    @Override
    public void stop() throws Exception {
        if (running) {
            consumer.stop();
            producer.stop();
            running = false;
            log.info("RocketMQ消息管理器停止成功");
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void registerHandler(String topic, MessageHandler<?> handler) {
        @SuppressWarnings("unchecked")
        MessageHandler<Object> objectHandler = (MessageHandler<Object>) handler;
        @SuppressWarnings("unchecked")
        MessageConsumer<Object> objectConsumer = (MessageConsumer<Object>) consumer;
        objectConsumer.subscribe(topic, objectHandler);
        log.info("注册消息处理器: topic={}", topic);
    }

    @Override
    public void unregisterHandler(String topic) {
        consumer.unsubscribe(topic);
        log.info("取消注册消息处理器: topic={}", topic);
    }

    @Override
    public void createTopic(String topic) {
        try {
            // 依赖 Broker 开启 autoCreateTopicEnable；生产环境建议通过控制台预先创建
            nativeProducer.createTopic(nativeProducer.getCreateTopicKey(), topic, 8);
            log.info("创建主题: topic={}", topic);
        } catch (Exception e) {
            throw new IllegalStateException("创建主题失败（生产环境请通过控制台/mqadmin 创建）: " + topic, e);
        }
    }

    @Override
    public void deleteTopic(String topic) {
        throw new UnsupportedOperationException(
                "RocketMQ 客户端不支持删除 Topic，请通过控制台或 mqadmin deleteTopic 操作: " + topic);
    }

    @Override
    public boolean topicExists(String topic) {
        try {
            return !nativeProducer.fetchPublishMessageQueues(topic).isEmpty();
        } catch (Exception e) {
            log.debug("查询主题路由失败，视为不存在: topic={}, reason={}", topic, e.getMessage());
            return false;
        }
    }
}

package io.nebula.messaging.rocketmq.consumer;

import io.nebula.messaging.core.consumer.MessageConsumer;
import io.nebula.messaging.core.message.Message;
import io.nebula.messaging.core.serializer.MessageSerializer;
import io.nebula.messaging.rocketmq.config.RocketMQProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RocketMQMessageConsumer单元测试
 *
 * <p>订阅/拉取依赖真实 Broker，由集成测试覆盖；此处验证配置映射与状态管理。</p>
 */
@ExtendWith(MockitoExtension.class)
class RocketMQMessageConsumerTest {

    @Mock
    private MessageSerializer messageSerializer;

    private RocketMQProperties properties;
    private RocketMQMessageConsumer<String> consumer;

    @BeforeEach
    void setUp() {
        properties = new RocketMQProperties();
        properties.setNameServer("127.0.0.1:9876");
        properties.getConsumer().setGroup("test-group");
        properties.getConsumer().setConsumeThreadMax(8);
        properties.getConsumer().setMaxReconsumeTimes(5);
        properties.getConsumer().setConsumeTimeoutMinutes(20);

        consumer = new RocketMQMessageConsumer<>(messageSerializer, properties);
    }

    @Test
    void testConfigMappedFromProperties() {
        MessageConsumer.ConsumerConfig config = consumer.getConfig();

        assertThat(config.getConsumerGroup()).isEqualTo("test-group");
        assertThat(config.getConcurrency()).isEqualTo(8);
        assertThat(config.getMaxRetries()).isEqualTo(5);
        assertThat(config.getConsumeTimeout()).isEqualTo(Duration.ofMinutes(20));
        assertThat(config.isAutoAck()).isTrue();
    }

    @Test
    void testConfigSetters() {
        MessageConsumer.ConsumerConfig config = consumer.getConfig();

        config.setConsumerGroup("new-group");
        config.setConcurrency(32);
        config.setMaxRetries(10);
        config.setConsumeTimeout(Duration.ofMinutes(30));
        config.setAutoAck(false);

        assertThat(config.getConsumerGroup()).isEqualTo("new-group");
        assertThat(config.getConcurrency()).isEqualTo(32);
        assertThat(config.getMaxRetries()).isEqualTo(10);
        assertThat(config.getConsumeTimeout()).isEqualTo(Duration.ofMinutes(30));
        assertThat(config.isAutoAck()).isFalse();
    }

    @Test
    void testLifecycleFlags() {
        assertThat(consumer.isRunning()).isTrue();
        assertThat(consumer.isPaused()).isFalse();

        consumer.pause();
        assertThat(consumer.isPaused()).isTrue();

        consumer.resume();
        assertThat(consumer.isPaused()).isFalse();

        consumer.stop();
        assertThat(consumer.isRunning()).isFalse();

        consumer.start();
        assertThat(consumer.isRunning()).isTrue();
    }

    @Test
    void testAckAndNackAreNoOpInPushMode() {
        Message<String> message = Message.of("test-topic", "payload");
        message.setId("MSG-001");

        assertThat(consumer.ack(message)).isTrue();
        assertThat(consumer.nack(message, true)).isTrue();
    }

    @Test
    void testStatsInitialState() {
        MessageConsumer.ConsumerStats stats = consumer.getStats();

        assertThat(stats.getConsumedCount()).isZero();
        assertThat(stats.getSuccessCount()).isZero();
        assertThat(stats.getFailedCount()).isZero();
        assertThat(stats.getSuccessRate()).isZero();
        assertThat(stats.getProcessingCount()).isZero();
    }
}

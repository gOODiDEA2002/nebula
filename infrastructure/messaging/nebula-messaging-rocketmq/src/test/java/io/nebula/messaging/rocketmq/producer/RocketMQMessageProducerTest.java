package io.nebula.messaging.rocketmq.producer;

import io.nebula.messaging.core.message.Message;
import io.nebula.messaging.core.producer.MessageProducer;
import io.nebula.messaging.core.serializer.MessageSerializer;
import io.nebula.messaging.rocketmq.config.RocketMQProperties;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RocketMQMessageProducer单元测试
 */
@ExtendWith(MockitoExtension.class)
class RocketMQMessageProducerTest {

    @Mock
    private DefaultMQProducer mqProducer;

    @Mock
    private MessageSerializer messageSerializer;

    private RocketMQMessageProducer<String> producer;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(messageSerializer.serialize(any())).thenReturn("test-message".getBytes());

        RocketMQProperties properties = new RocketMQProperties();
        properties.setNameServer("127.0.0.1:9876");
        producer = new RocketMQMessageProducer<>(mqProducer, messageSerializer, properties);
    }

    private SendResult okResult() {
        SendResult result = new SendResult();
        result.setSendStatus(SendStatus.SEND_OK);
        result.setMsgId("MSG-001");
        return result;
    }

    @Test
    void testSendWithTopicAndPayload() throws Exception {
        when(mqProducer.send(any(org.apache.rocketmq.common.message.Message.class), anyLong()))
                .thenReturn(okResult());

        MessageProducer.SendResult result = producer.send("test-topic", "test message");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTopic()).isEqualTo("test-topic");
        assertThat(result.getMessageId()).isEqualTo("MSG-001");
        verify(messageSerializer).serialize("test message");
    }

    @Test
    void testSendWithQueueMapsToTag() throws Exception {
        when(mqProducer.send(any(org.apache.rocketmq.common.message.Message.class), anyLong()))
                .thenReturn(okResult());

        producer.send("test-topic", "biz-tag", "payload");

        ArgumentCaptor<org.apache.rocketmq.common.message.Message> captor =
                ArgumentCaptor.forClass(org.apache.rocketmq.common.message.Message.class);
        verify(mqProducer).send(captor.capture(), anyLong());
        assertThat(captor.getValue().getTopic()).isEqualTo("test-topic");
        assertThat(captor.getValue().getTags()).isEqualTo("biz-tag");
    }

    @Test
    void testSendWithQueueSameAsTopicHasNoTag() throws Exception {
        when(mqProducer.send(any(org.apache.rocketmq.common.message.Message.class), anyLong()))
                .thenReturn(okResult());

        producer.send("test-topic", "test-topic", "payload");

        ArgumentCaptor<org.apache.rocketmq.common.message.Message> captor =
                ArgumentCaptor.forClass(org.apache.rocketmq.common.message.Message.class);
        verify(mqProducer).send(captor.capture(), anyLong());
        assertThat(captor.getValue().getTags()).isNull();
    }

    @Test
    void testSendWithMessageObject() throws Exception {
        when(mqProducer.send(any(org.apache.rocketmq.common.message.Message.class), anyLong()))
                .thenReturn(okResult());

        Message<String> message = Message.of("test-topic", "test-topic", "order-created", "payload");
        MessageProducer.SendResult result = producer.send(message);

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<org.apache.rocketmq.common.message.Message> captor =
                ArgumentCaptor.forClass(org.apache.rocketmq.common.message.Message.class);
        verify(mqProducer).send(captor.capture(), anyLong());
        assertThat(captor.getValue().getTags()).isEqualTo("order-created");
    }

    @Test
    void testSendFailure() throws Exception {
        when(mqProducer.send(any(org.apache.rocketmq.common.message.Message.class), anyLong()))
                .thenThrow(new RuntimeException("broker unavailable"));

        MessageProducer.SendResult result = producer.send("test-topic", "payload");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("broker unavailable");
        assertThat(result.getException()).isNotNull();
    }

    @Test
    void testSendDelayMessageSetsDelayLevel() throws Exception {
        when(mqProducer.send(any(org.apache.rocketmq.common.message.Message.class), anyLong()))
                .thenReturn(okResult());

        MessageProducer.SendResult result =
                producer.sendDelayMessage("test-topic", "payload", Duration.ofMinutes(5));

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<org.apache.rocketmq.common.message.Message> captor =
                ArgumentCaptor.forClass(org.apache.rocketmq.common.message.Message.class);
        verify(mqProducer).send(captor.capture(), anyLong());
        // 5 分钟对应延迟等级 9（1s 5s 10s 30s 1m 2m 3m 4m 5m）
        assertThat(captor.getValue().getDelayTimeLevel()).isEqualTo(9);
    }

    @Test
    void testMapDelayLevel() {
        assertThat(RocketMQMessageProducer.mapDelayLevel(Duration.ofSeconds(1))).isEqualTo(1);
        assertThat(RocketMQMessageProducer.mapDelayLevel(Duration.ofSeconds(3))).isEqualTo(2);
        assertThat(RocketMQMessageProducer.mapDelayLevel(Duration.ofSeconds(30))).isEqualTo(4);
        assertThat(RocketMQMessageProducer.mapDelayLevel(Duration.ofMinutes(10))).isEqualTo(14);
        assertThat(RocketMQMessageProducer.mapDelayLevel(Duration.ofHours(2))).isEqualTo(18);
        // 超过最大等级（2h）取最大等级 18
        assertThat(RocketMQMessageProducer.mapDelayLevel(Duration.ofHours(24))).isEqualTo(18);
    }

    @Test
    void testSendBatchWithSameTopic() throws Exception {
        when(mqProducer.send(any(java.util.Collection.class), anyLong())).thenReturn(okResult());

        MessageProducer.BatchSendResult result =
                producer.sendBatch("test-topic", List.of("m1", "m2", "m3"));

        assertThat(result.isAllSuccess()).isTrue();
        assertThat(result.getTotalCount()).isEqualTo(3);
        assertThat(result.getSuccessCount()).isEqualTo(3);
    }

    @Test
    void testStats() throws Exception {
        when(mqProducer.send(any(org.apache.rocketmq.common.message.Message.class), anyLong()))
                .thenReturn(okResult())
                .thenThrow(new RuntimeException("fail"));

        producer.send("test-topic", "ok");
        producer.send("test-topic", "fail");

        MessageProducer.ProducerStats stats = producer.getStats();
        assertThat(stats.getSentCount()).isEqualTo(2);
        assertThat(stats.getSuccessCount()).isEqualTo(1);
        assertThat(stats.getFailedCount()).isEqualTo(1);
        assertThat(stats.getSuccessRate()).isEqualTo(0.5);

        stats.reset();
        assertThat(stats.getSentCount()).isZero();
    }

    @Test
    void testBroadcastFallsBackToNormalSend() throws Exception {
        when(mqProducer.send(any(org.apache.rocketmq.common.message.Message.class), anyLong()))
                .thenReturn(okResult());

        MessageProducer.SendResult result = producer.sendBroadcast("test-topic", "payload");

        assertThat(result.isSuccess()).isTrue();
    }
}

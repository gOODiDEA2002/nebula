package io.nebula.messaging.redis.support;

import io.nebula.messaging.core.message.Message;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedisMessageSerializerTest {

    private final RedisMessageSerializer serializer = new RedisMessageSerializer();

    @Test
    void typedPayloadRoundTripsWithoutDegradingToMap() {
        Message<TestPayload> message = Message.of("orders", new TestPayload("order-1", 3));

        Message<TestPayload> restored = serializer.deserialize(
                serializer.serialize(message), TestPayload.class);

        assertThat(restored.getPayload()).isInstanceOf(TestPayload.class);
        assertThat(restored.getPayload().getId()).isEqualTo("order-1");
        assertThat(restored.getPayload().getQuantity()).isEqualTo(3);
    }

    @Test
    void publicObjectHelpersRemainCompatible() {
        TestPayload restored = serializer.deserializeObject(
                serializer.serializeObject(new TestPayload("order-2", 5)), TestPayload.class);

        assertThat(restored.getId()).isEqualTo("order-2");
        assertThat(restored.getQuantity()).isEqualTo(5);
    }

    static class TestPayload {
        private String id;
        private int quantity;

        TestPayload() {
        }

        TestPayload(String id, int quantity) {
            this.id = id;
            this.quantity = quantity;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }
}

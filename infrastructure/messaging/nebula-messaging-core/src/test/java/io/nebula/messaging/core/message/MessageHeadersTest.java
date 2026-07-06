package io.nebula.messaging.core.message;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 验证 {@link Message#getHeaders()} 在任何构造路径下都空安全，
 * 修复消费端(尤其无头 Redis Stream 消息) {@code getHeaders().put/get} 的 NPE。
 */
class MessageHeadersTest {

    @Test
    void noArgConstructorHeadersNonNull() {
        Message<String> message = new Message<>();
        assertThat(message.getHeaders()).isNotNull();
        assertThatCode(() -> message.getHeaders().put("k", "v")).doesNotThrowAnyException();
        assertThat(message.getHeaders()).containsEntry("k", "v");
    }

    @Test
    void builderHeadersDefaultNonNull() {
        Message<String> message = Message.<String>builder().topic("t").build();
        assertThat(message.getHeaders()).isNotNull();
        assertThatCode(() -> message.getHeaders().put("k", "v")).doesNotThrowAnyException();
    }

    @Test
    void explicitNullHeadersStillSafe() {
        Message<String> message = new Message<>();
        message.setHeaders(null);
        assertThat(message.getHeaders()).isNotNull();
        assertThatCode(() -> message.getHeaders().put("k", "v")).doesNotThrowAnyException();
    }
}

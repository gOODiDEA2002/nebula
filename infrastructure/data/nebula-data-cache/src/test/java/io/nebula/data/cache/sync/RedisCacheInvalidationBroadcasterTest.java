package io.nebula.data.cache.sync;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * 缓存失效广播消息协议测试: 自发消息跳过、EVICT/CLEAR 分发、含"|"的 key、畸形消息容错。
 */
@ExtendWith(MockitoExtension.class)
class RedisCacheInvalidationBroadcasterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private final List<String> evicted = new ArrayList<>();
    private final AtomicInteger clears = new AtomicInteger();
    private RedisCacheInvalidationBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        broadcaster = new RedisCacheInvalidationBroadcaster(
                redisTemplate, "nebula:cache:invalidation", evicted::add, clears::incrementAndGet);
    }

    @Test
    @DisplayName("publishEvict 按协议格式发布消息")
    void publishEvictSendsProtocolMessage() {
        broadcaster.publishEvict("orders:1");

        verify(redisTemplate).convertAndSend("nebula:cache:invalidation",
                broadcaster.getNodeId() + "|EVICT|orders:1");
    }

    @Test
    @DisplayName("自己发出的消息被跳过(本地 L1 已在写路径处理)")
    void ownMessagesAreSkipped() {
        broadcaster.handlePayload(broadcaster.getNodeId() + "|EVICT|k1");
        broadcaster.handlePayload(broadcaster.getNodeId() + "|CLEAR");

        assertThat(evicted).isEmpty();
        assertThat(clears.get()).isZero();
    }

    @Test
    @DisplayName("其他节点的 EVICT/CLEAR 触发本地回调, key 中的 | 不破坏解析")
    void remoteMessagesDispatchToHandlers() {
        broadcaster.handlePayload("other-node|EVICT|k1");
        broadcaster.handlePayload("other-node|EVICT|user|profile|1");
        broadcaster.handlePayload("other-node|CLEAR");

        assertThat(evicted).containsExactly("k1", "user|profile|1");
        assertThat(clears.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("畸形消息与发布异常不抛出(尽力而为语义)")
    void malformedMessagesAndPublishFailuresAreTolerated() {
        assertThatCode(() -> {
            broadcaster.handlePayload("");
            broadcaster.handlePayload("no-separator");
            broadcaster.handlePayload("other|UNKNOWN|x");
        }).doesNotThrowAnyException();
        assertThat(evicted).isEmpty();

        doThrow(new IllegalStateException("redis down")).when(redisTemplate).convertAndSend(anyString(), anyString());
        assertThatCode(() -> broadcaster.publishEvict("k")).doesNotThrowAnyException();
        assertThatCode(() -> broadcaster.publishClear()).doesNotThrowAnyException();
    }
}

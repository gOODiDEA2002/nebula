package io.nebula.data.cache.sync;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 基于 Redis pub/sub 的缓存失效广播实现（CD-10）
 *
 * <p>消息格式: {@code nodeId|EVICT|key} 或 {@code nodeId|CLEAR}。
 * nodeId 为本实例随机生成的标识, 用于跳过自己发出的消息(本地 L1 已在写路径同步处理)。
 * key 中允许出现 "|"(按 limit=3 切分, 尾段整体作为 key)。</p>
 *
 * <p>发布失败仅记录告警, 不影响本地缓存操作——广播是尽力而为的加速,
 * L1 短 TTL 仍是最终一致性兜底。订阅侧由 {@code RedisMessageListenerContainer}
 * 驱动(自动配置装配), 本类实现 {@link MessageListener} 处理入站消息。</p>
 *
 * @author Nebula Framework
 * @since 2.0.1
 */
@Slf4j
public class RedisCacheInvalidationBroadcaster implements CacheInvalidationBroadcaster, MessageListener {

    private static final String OP_EVICT = "EVICT";
    private static final String OP_CLEAR = "CLEAR";

    private final String nodeId = UUID.randomUUID().toString();
    private final StringRedisTemplate redisTemplate;
    private final String topic;
    private final Consumer<String> evictHandler;
    private final Runnable clearHandler;

    /**
     * @param redisTemplate 用于发布的字符串模板（与订阅容器共用连接工厂）
     * @param topic         广播频道
     * @param evictHandler  收到其他节点 EVICT 消息时的本地 L1 驱逐回调
     * @param clearHandler  收到其他节点 CLEAR 消息时的本地 L1 清空回调
     */
    public RedisCacheInvalidationBroadcaster(StringRedisTemplate redisTemplate, String topic,
                                             Consumer<String> evictHandler, Runnable clearHandler) {
        this.redisTemplate = redisTemplate;
        this.topic = topic;
        this.evictHandler = evictHandler;
        this.clearHandler = clearHandler;
    }

    @Override
    public void publishEvict(String key) {
        publish(nodeId + "|" + OP_EVICT + "|" + key);
    }

    @Override
    public void publishClear() {
        publish(nodeId + "|" + OP_CLEAR);
    }

    private void publish(String payload) {
        try {
            redisTemplate.convertAndSend(topic, payload);
        } catch (Exception e) {
            log.warn("缓存失效广播发送失败(不影响本地缓存操作, 其他节点靠 L1 TTL 兜底): {}", e.getMessage());
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        handlePayload(new String(message.getBody(), StandardCharsets.UTF_8));
    }

    /**
     * 处理入站广播消息（拆出便于测试, 不依赖 Redis 环境）
     */
    void handlePayload(String payload) {
        try {
            String[] parts = payload.split("\\|", 3);
            if (parts.length < 2) {
                return;
            }
            if (nodeId.equals(parts[0])) {
                // 本节点发出的消息, 本地 L1 已在写路径处理过
                return;
            }
            switch (parts[1]) {
                case OP_EVICT -> {
                    if (parts.length == 3) {
                        log.debug("收到跨节点缓存失效: key={}", parts[2]);
                        evictHandler.accept(parts[2]);
                    }
                }
                case OP_CLEAR -> {
                    log.debug("收到跨节点缓存清空广播");
                    clearHandler.run();
                }
                default -> log.debug("忽略未知缓存广播操作: {}", parts[1]);
            }
        } catch (Exception e) {
            log.warn("处理缓存失效广播失败: payload={}", payload, e);
        }
    }

    String getNodeId() {
        return nodeId;
    }
}

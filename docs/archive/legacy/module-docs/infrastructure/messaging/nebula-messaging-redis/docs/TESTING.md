# Nebula Messaging Redis - 测试指南

本文档提供 `nebula-messaging-redis` 的测试建议与示例。

## 测试前提

- 本地 Redis 服务可用，或使用 Testcontainers 启动 Redis。
- 启用 `nebula.messaging.redis.enabled=true`。

## 单元测试建议

### 1. 消息发送验证

```java
@SpringBootTest
class RedisMessageProducerTest {

    @Autowired
    private RedisMessageManager messageManager;

    @Test
    void shouldPublishMessage() {
        messageManager.publish("test:topic", "hello");
    }
}
```

### 2. 注解处理器注册验证

```java
@SpringBootTest
class RedisMessageHandlerTest {

    @Autowired
    private TestHandler testHandler;

    @Test
    void shouldReceiveMessage() {
        // 触发 publish 并断言 handler 被调用
    }

    @Component
    static class TestHandler {
        @RedisMessageHandler(channel = "test:topic")
        public void handle(Message<String> message) {
            // 断言逻辑
        }
    }
}
```

## 集成测试建议

1. 使用 Testcontainers 启动 Redis：
   - 镜像：`redis:7-alpine`
2. 验证 Stream 消息的消费与 ACK 逻辑。
3. 验证 `channel-prefix` 与模式订阅是否生效。

## 常见问题

- 没有收到消息：检查 `channel-prefix` 与订阅频道是否匹配。
- Stream 无消费：确认 `stream.enabled=true` 且 `consumer-group` 配置正确。

package io.nebula.messaging.redis.support;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import io.nebula.messaging.core.exception.MessageSerializationException;
import io.nebula.messaging.core.message.Message;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis 消息序列化器
 * <p>
 * 使用 Jackson 进行 JSON 序列化/反序列化
 * </p>
 */
@Slf4j
public class RedisMessageSerializer {

    private final ObjectMapper objectMapper;

    public RedisMessageSerializer() {
        this.objectMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .build();
    }

    public RedisMessageSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 序列化消息为 JSON 字符串
     *
     * @param message 消息对象
     * @return JSON 字符串
     */
    public String serialize(Message<?> message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JacksonException e) {
            throw new MessageSerializationException("消息序列化失败", e);
        }
    }

    /**
     * 反序列化 JSON 字符串为消息对象
     *
     * @param json JSON 字符串
     * @param <T>  载荷类型
     * @return 消息对象
     */
    @SuppressWarnings("unchecked")
    public <T> Message<T> deserialize(String json) {
        try {
            return objectMapper.readValue(json, Message.class);
        } catch (JacksonException e) {
            throw new MessageSerializationException("消息反序列化失败", e);
        }
    }

    /**
     * 反序列化 JSON 字符串为指定类型的消息对象
     *
     * @param json        JSON 字符串
     * @param payloadType 载荷类型
     * @param <T>         载荷泛型
     * @return 消息对象
     */
    public <T> Message<T> deserialize(String json, Class<T> payloadType) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructParametricType(Message.class, payloadType));
        } catch (JacksonException e) {
            throw new MessageSerializationException("消息反序列化失败: payloadType=" + payloadType.getName(), e);
        }
    }

    /**
     * 序列化普通对象。
     */
    public String serializeObject(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JacksonException e) {
            throw new MessageSerializationException("对象序列化失败", e);
        }
    }

    /**
     * 反序列化普通对象。
     */
    public <T> T deserializeObject(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JacksonException e) {
            throw new MessageSerializationException("对象反序列化失败", e);
        }
    }

    /**
     * 获取内部 ObjectMapper
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}

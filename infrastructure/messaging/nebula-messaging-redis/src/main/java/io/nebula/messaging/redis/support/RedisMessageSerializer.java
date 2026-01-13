package io.nebula.messaging.redis.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
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
        } catch (JsonProcessingException e) {
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
        } catch (JsonProcessingException e) {
            throw new MessageSerializationException("消息反序列化失败", e);
        }
    }

    /**
     * 反序列化 JSON 字符串为指定类型的消息对象
     *
     * @param json        JSON 字符串
     * @param payloadType 载荷类型
     * @param <T>         载荷类型
     * @return 消息对象
     */
    public <T> Message<T> deserialize(String json, Class<T> payloadType) {
        try {
            Message<T> message = objectMapper.readValue(json, 
                objectMapper.getTypeFactory().constructParametricType(Message.class, payloadType));
            return message;
        } catch (JsonProcessingException e) {
            throw new MessageSerializationException("消息反序列化失败", e);
        }
    }

    /**
     * 序列化对象为 JSON 字符串
     *
     * @param obj 对象
     * @return JSON 字符串
     */
    public String serializeObject(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new MessageSerializationException("对象序列化失败", e);
        }
    }

    /**
     * 反序列化 JSON 字符串为对象
     *
     * @param json JSON 字符串
     * @param type 目标类型
     * @param <T>  目标类型
     * @return 对象
     */
    public <T> T deserializeObject(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new MessageSerializationException("对象反序列化失败", e);
        }
    }

    /**
     * 获取 ObjectMapper
     *
     * @return ObjectMapper
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}


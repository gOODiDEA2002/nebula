package io.nebula.messaging.core.serializer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 消息序列化器接口
 * 负责消息对象与字节数组之间的转换
 */
public interface MessageSerializer {
    
    /**
     * 序列化对象为字节数组
     * 
     * @param obj 要序列化的对象
     * @return 序列化后的字节数组
     * @throws SerializationException 序列化异常
     */
    byte[] serialize(Object obj) throws SerializationException;
    
    /**
     * 反序列化字节数组为对象
     * 
     * @param data 字节数组
     * @param clazz 目标类型
     * @param <T> 目标类型泛型
     * @return 反序列化后的对象
     * @throws SerializationException 反序列化异常
     */
    <T> T deserialize(byte[] data, Class<T> clazz) throws SerializationException;
    
    /**
     * 反序列化字节数组为对象（带类型信息）
     * 
     * @param data 字节数组
     * @param typeReference 类型引用
     * @param <T> 目标类型泛型
     * @return 反序列化后的对象
     * @throws SerializationException 反序列化异常
     */
    <T> T deserialize(byte[] data, TypeReference<T> typeReference) throws SerializationException;
    
    /**
     * 序列化对象为字符串
     * 
     * @param obj 要序列化的对象
     * @return 序列化后的字符串
     * @throws SerializationException 序列化异常
     */
    default String serializeToString(Object obj) throws SerializationException {
        return new String(serialize(obj), getCharset());
    }
    
    /**
     * 反序列化字符串为对象
     * 
     * @param data 字符串数据
     * @param clazz 目标类型
     * @param <T> 目标类型泛型
     * @return 反序列化后的对象
     * @throws SerializationException 反序列化异常
     */
    default <T> T deserializeFromString(String data, Class<T> clazz) throws SerializationException {
        return deserialize(data.getBytes(getCharset()), clazz);
    }
    
    /**
     * 获取内容类型
     * 
     * @return 内容类型 (如 application/json, application/x-java-serialized-object)
     */
    String getContentType();
    
    /**
     * 获取序列化器名称
     * 
     * @return 序列化器名称
     */
    String getName();
    
    /**
     * 获取字符编码
     * 
     * @return 字符编码
     */
    default Charset getCharset() {
        return StandardCharsets.UTF_8;
    }
    
    /**
     * 检查是否支持指定类型
     * 
     * @param clazz 类型
     * @return 是否支持
     */
    boolean supports(Class<?> clazz);
    
    /**
     * 获取序列化后的大小估算
     * 
     * @param obj 对象
     * @return 预估大小（字节）
     */
    default int estimateSize(Object obj) {
        if (obj == null) {
            return 0;
        }
        try {
            return serialize(obj).length;
        } catch (Exception e) {
            return obj.toString().getBytes(getCharset()).length;
        }
    }
    
    /**
     * 检查序列化器是否可用
     * 
     * @return 是否可用
     */
    default boolean isAvailable() {
        return true;
    }
    
    /**
     * 类型引用接口，用于泛型类型反序列化
     * 
     * @param <T> 目标类型
     */
    interface TypeReference<T> {
        /**
         * 获取实际的类型
         * 
         * @return 类型
         */
        Class<T> getType();
        
        /**
         * 获取泛型信息
         * 
         * @return 泛型类型
         */
        default java.lang.reflect.Type getGenericType() {
            return getType();
        }
    }
    
    /**
     * 序列化异常
     */
    class SerializationException extends Exception {
        
        private static final long serialVersionUID = 1L;
        
        public SerializationException(String message) {
            super(message);
        }
        
        public SerializationException(String message, Throwable cause) {
            super(message, cause);
        }
        
        public SerializationException(Throwable cause) {
            super(cause);
        }
    }
    
    /**
     * 序列化器类型枚举
     */
    enum SerializerType {
        /**
         * JSON 序列化器
         */
        JSON("application/json"),
        
        /**
         * Java 原生序列化器
         */
        JAVA("application/x-java-serialized-object"),
        
        /**
         * Protobuf 序列化器
         */
        PROTOBUF("application/x-protobuf"),
        
        /**
         * Avro 序列化器
         */
        AVRO("application/avro"),
        
        /**
         * MessagePack 序列化器
         */
        MESSAGEPACK("application/x-msgpack"),
        
        /**
         * Kryo 序列化器
         */
        KRYO("application/x-kryo"),
        
        /**
         * XML 序列化器
         */
        XML("application/xml"),
        
        /**
         * 字符串序列化器
         */
        STRING("text/plain");
        
        private final String contentType;
        
        SerializerType(String contentType) {
            this.contentType = contentType;
        }
        
        public String getContentType() {
            return contentType;
        }
    }
}

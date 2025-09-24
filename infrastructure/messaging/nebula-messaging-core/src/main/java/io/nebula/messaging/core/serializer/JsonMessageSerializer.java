package io.nebula.messaging.core.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * JSON 消息序列化器实现
 * 基于 Jackson 的 JSON 序列化器
 */
@Slf4j
public class JsonMessageSerializer implements MessageSerializer {
    
    private final ObjectMapper objectMapper;
    private final Set<Class<?>> supportedTypes;
    
    public JsonMessageSerializer() {
        this.objectMapper = createObjectMapper();
        this.supportedTypes = createSupportedTypes();
        log.info("JSON 消息序列化器初始化完成");
    }
    
    public JsonMessageSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : createObjectMapper();
        this.supportedTypes = createSupportedTypes();
        log.info("JSON 消息序列化器初始化完成（自定义 ObjectMapper）");
    }
    
    @Override
    public byte[] serialize(Object obj) throws SerializationException {
        if (obj == null) {
            return new byte[0];
        }
        
        try {
            byte[] result = objectMapper.writeValueAsBytes(obj);
            log.debug("对象序列化成功: type={}, size={} bytes", 
                    obj.getClass().getSimpleName(), result.length);
            return result;
        } catch (JsonProcessingException e) {
            String errorMsg = String.format("JSON 序列化失败: type=%s, error=%s", 
                    obj.getClass().getName(), e.getMessage());
            log.error(errorMsg, e);
            throw new SerializationException(errorMsg, e);
        }
    }
    
    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) throws SerializationException {
        if (data == null || data.length == 0) {
            return null;
        }
        
        if (clazz == null) {
            throw new SerializationException("目标类型不能为空");
        }
        
        try {
            T result = objectMapper.readValue(data, clazz);
            log.debug("数据反序列化成功: type={}, size={} bytes", 
                    clazz.getSimpleName(), data.length);
            return result;
        } catch (IOException e) {
            String errorMsg = String.format("JSON 反序列化失败: type=%s, dataSize=%d, error=%s", 
                    clazz.getName(), data.length, e.getMessage());
            log.error(errorMsg, e);
            throw new SerializationException(errorMsg, e);
        }
    }
    
    @Override
    public <T> T deserialize(byte[] data, TypeReference<T> typeReference) throws SerializationException {
        if (data == null || data.length == 0) {
            return null;
        }
        
        if (typeReference == null) {
            throw new SerializationException("类型引用不能为空");
        }
        
        try {
            com.fasterxml.jackson.core.type.TypeReference<T> jacksonTypeRef = 
                    new com.fasterxml.jackson.core.type.TypeReference<T>() {
                        @Override
                        public java.lang.reflect.Type getType() {
                            return typeReference.getGenericType();
                        }
                    };
            
            T result = objectMapper.readValue(data, jacksonTypeRef);
            log.debug("数据反序列化成功（泛型）: type={}, size={} bytes", 
                    typeReference.getType().getSimpleName(), data.length);
            return result;
        } catch (IOException e) {
            String errorMsg = String.format("JSON 反序列化失败（泛型）: type=%s, dataSize=%d, error=%s", 
                    typeReference.getType().getName(), data.length, e.getMessage());
            log.error(errorMsg, e);
            throw new SerializationException(errorMsg, e);
        }
    }
    
    @Override
    public String getContentType() {
        return SerializerType.JSON.getContentType();
    }
    
    @Override
    public String getName() {
        return "JSON";
    }
    
    @Override
    public boolean supports(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        
        // 基本类型和包装类型
        if (clazz.isPrimitive() || supportedTypes.contains(clazz)) {
            return true;
        }
        
        // 数组类型
        if (clazz.isArray()) {
            return supports(clazz.getComponentType());
        }
        
        // 枚举类型
        if (clazz.isEnum()) {
            return true;
        }
        
        // Java Bean 类型（有无参构造函数的类）
        try {
            clazz.getDeclaredConstructor();
            return true;
        } catch (NoSuchMethodException e) {
            log.debug("类型不支持 JSON 序列化（无无参构造函数）: {}", clazz.getName());
            return false;
        }
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // 测试 ObjectMapper 是否可用
            objectMapper.writeValueAsString("test");
            return true;
        } catch (Exception e) {
            log.warn("JSON 序列化器不可用: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取 ObjectMapper 实例
     * 
     * @return ObjectMapper
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
    
    /**
     * 创建 ObjectMapper
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // 注册 Java 8 时间模块
        mapper.registerModule(new JavaTimeModule());
        
        // 配置序列化特性
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        
        // 配置反序列化特性
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        
        return mapper;
    }
    
    /**
     * 创建支持的类型集合
     */
    private Set<Class<?>> createSupportedTypes() {
        return new HashSet<>(Arrays.asList(
                // 基本包装类型
                Boolean.class, Byte.class, Character.class, Short.class,
                Integer.class, Long.class, Float.class, Double.class,
                
                // 字符串和日期
                String.class, java.util.Date.class,
                java.time.LocalDate.class, java.time.LocalTime.class,
                java.time.LocalDateTime.class, java.time.ZonedDateTime.class,
                java.time.Instant.class,
                
                // 集合类型
                java.util.List.class, java.util.Set.class, java.util.Map.class,
                java.util.Collection.class, java.util.ArrayList.class,
                java.util.LinkedList.class, java.util.HashSet.class,
                java.util.LinkedHashSet.class, java.util.HashMap.class,
                java.util.LinkedHashMap.class, java.util.TreeMap.class,
                
                // 其他常用类型
                java.math.BigDecimal.class, java.math.BigInteger.class,
                java.util.UUID.class, java.net.URL.class, java.net.URI.class
        ));
    }
}

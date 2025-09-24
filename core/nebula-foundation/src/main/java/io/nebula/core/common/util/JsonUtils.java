package io.nebula.core.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * JSON工具类
 * 提供JSON序列化、反序列化和操作功能
 */
public final class JsonUtils {
    
    /**
     * 默认ObjectMapper实例
     */
    private static final ObjectMapper DEFAULT_MAPPER = createDefaultMapper();
    
    /**
     * 美化输出的ObjectMapper实例
     */
    private static final ObjectMapper PRETTY_MAPPER = createPrettyMapper();
    
    /**
     * 私有构造函数，防止实例化
     */
    private JsonUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * 创建默认配置的ObjectMapper
     * 
     * @return ObjectMapper实例
     */
    private static ObjectMapper createDefaultMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 注册Java 8时间模块
        mapper.registerModule(new JavaTimeModule());
        // 禁用写入日期为时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 忽略未知属性
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 允许空Bean序列化
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }
    
    /**
     * 创建美化输出的ObjectMapper
     * 
     * @return ObjectMapper实例
     */
    private static ObjectMapper createPrettyMapper() {
        ObjectMapper mapper = createDefaultMapper();
        // 启用美化输出
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }
    
    /**
     * 获取默认ObjectMapper实例
     * 
     * @return ObjectMapper实例
     */
    public static ObjectMapper getMapper() {
        return DEFAULT_MAPPER;
    }
    
    /**
     * 获取美化输出的ObjectMapper实例
     * 
     * @return ObjectMapper实例
     */
    public static ObjectMapper getPrettyMapper() {
        return PRETTY_MAPPER;
    }
    
    // ====================
    // 序列化方法
    // ====================
    
    /**
     * 将对象序列化为JSON字符串
     * 
     * @param obj 要序列化的对象
     * @return JSON字符串，序列化失败返回null
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return DEFAULT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    /**
     * 将对象序列化为美化的JSON字符串
     * 
     * @param obj 要序列化的对象
     * @return 美化的JSON字符串，序列化失败返回null
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return PRETTY_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    /**
     * 将对象序列化为JSON字节数组
     * 
     * @param obj 要序列化的对象
     * @return JSON字节数组，序列化失败返回null
     */
    public static byte[] toJsonBytes(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return DEFAULT_MAPPER.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    // ====================
    // 反序列化方法
    // ====================
    
    /**
     * 将JSON字符串反序列化为指定类型对象
     * 
     * @param json  JSON字符串
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 反序列化对象，失败返回null
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (Strings.isBlank(json) || clazz == null) {
            return null;
        }
        try {
            return DEFAULT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    /**
     * 将JSON字符串反序列化为指定类型对象
     * 
     * @param json        JSON字符串
     * @param typeRef 类型引用
     * @param <T>         泛型类型
     * @return 反序列化对象，失败返回null
     */
    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (Strings.isBlank(json) || typeRef == null) {
            return null;
        }
        try {
            return DEFAULT_MAPPER.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    /**
     * 将JSON字节数组反序列化为指定类型对象
     * 
     * @param jsonBytes JSON字节数组
     * @param clazz     目标类型
     * @param <T>       泛型类型
     * @return 反序列化对象，失败返回null
     */
    public static <T> T fromJsonBytes(byte[] jsonBytes, Class<T> clazz) {
        if (jsonBytes == null || jsonBytes.length == 0 || clazz == null) {
            return null;
        }
        try {
            return DEFAULT_MAPPER.readValue(jsonBytes, clazz);
        } catch (IOException e) {
            return null;
        }
    }
    
    /**
     * 从输入流读取JSON并反序列化为指定类型对象
     * 
     * @param inputStream 输入流
     * @param clazz       目标类型
     * @param <T>         泛型类型
     * @return 反序列化对象，失败返回null
     */
    public static <T> T fromJson(InputStream inputStream, Class<T> clazz) {
        if (inputStream == null || clazz == null) {
            return null;
        }
        try {
            return DEFAULT_MAPPER.readValue(inputStream, clazz);
        } catch (IOException e) {
            return null;
        }
    }
    
    // ====================
    // 特殊类型转换
    // ====================
    
    /**
     * 将JSON字符串转换为Map
     * 
     * @param json JSON字符串
     * @return Map对象，失败返回null
     */
    public static Map<String, Object> toMap(String json) {
        return fromJson(json, new TypeReference<Map<String, Object>>() {});
    }
    
    /**
     * 将JSON字符串转换为List
     * 
     * @param json  JSON字符串
     * @param clazz 列表元素类型
     * @param <T>   泛型类型
     * @return List对象，失败返回null
     */
    public static <T> List<T> toList(String json, Class<T> clazz) {
        if (Strings.isBlank(json) || clazz == null) {
            return null;
        }
        try {
            return DEFAULT_MAPPER.readValue(json, 
                    DEFAULT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    /**
     * 将对象转换为Map
     * 
     * @param obj 要转换的对象
     * @return Map对象，失败返回null
     */
    public static Map<String, Object> objectToMap(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return DEFAULT_MAPPER.convertValue(obj, new TypeReference<Map<String, Object>>() {});
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * 将Map转换为指定类型对象
     * 
     * @param map   Map对象
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 转换后的对象，失败返回null
     */
    public static <T> T mapToObject(Map<String, Object> map, Class<T> clazz) {
        if (map == null || clazz == null) {
            return null;
        }
        try {
            return DEFAULT_MAPPER.convertValue(map, clazz);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    // ====================
    // JSON节点操作
    // ====================
    
    /**
     * 解析JSON字符串为JsonNode
     * 
     * @param json JSON字符串
     * @return JsonNode对象，失败返回null
     */
    public static JsonNode parseJson(String json) {
        if (Strings.isBlank(json)) {
            return null;
        }
        try {
            return DEFAULT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    /**
     * 从JsonNode获取指定路径的值
     * 
     * @param jsonNode JsonNode对象
     * @param path     路径（用.分隔，如"user.name"）
     * @return 值字符串，不存在返回null
     */
    public static String getValue(JsonNode jsonNode, String path) {
        if (jsonNode == null || Strings.isBlank(path)) {
            return null;
        }
        
        JsonNode currentNode = jsonNode;
        String[] parts = path.split("\\.");
        
        for (String part : parts) {
            if (currentNode == null || !currentNode.has(part)) {
                return null;
            }
            currentNode = currentNode.get(part);
        }
        
        return currentNode != null ? currentNode.asText() : null;
    }
    
    /**
     * 从JsonNode获取指定路径的值（指定类型）
     * 
     * @param jsonNode JsonNode对象
     * @param path     路径（用.分隔，如"user.age"）
     * @param clazz    目标类型
     * @param <T>      泛型类型
     * @return 指定类型的值，不存在或转换失败返回null
     */
    public static <T> T getValue(JsonNode jsonNode, String path, Class<T> clazz) {
        if (jsonNode == null || Strings.isBlank(path) || clazz == null) {
            return null;
        }
        
        JsonNode currentNode = jsonNode;
        String[] parts = path.split("\\.");
        
        for (String part : parts) {
            if (currentNode == null || !currentNode.has(part)) {
                return null;
            }
            currentNode = currentNode.get(part);
        }
        
        if (currentNode == null) {
            return null;
        }
        
        try {
            return DEFAULT_MAPPER.treeToValue(currentNode, clazz);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    // ====================
    // 验证和判断
    // ====================
    
    /**
     * 检查字符串是否为有效的JSON
     * 
     * @param json 要检查的字符串
     * @return 是否为有效JSON
     */
    public static boolean isValidJson(String json) {
        if (Strings.isBlank(json)) {
            return false;
        }
        try {
            DEFAULT_MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }
    
    /**
     * 检查字符串是否为JSON对象格式
     * 
     * @param json 要检查的字符串
     * @return 是否为JSON对象
     */
    public static boolean isJsonObject(String json) {
        JsonNode node = parseJson(json);
        return node != null && node.isObject();
    }
    
    /**
     * 检查字符串是否为JSON数组格式
     * 
     * @param json 要检查的字符串
     * @return 是否为JSON数组
     */
    public static boolean isJsonArray(String json) {
        JsonNode node = parseJson(json);
        return node != null && node.isArray();
    }
    
    // ====================
    // JSON合并和操作
    // ====================
    
    /**
     * 合并两个JSON字符串
     * 
     * @param json1 第一个JSON字符串
     * @param json2 第二个JSON字符串（会覆盖第一个中的相同字段）
     * @return 合并后的JSON字符串，失败返回null
     */
    public static String mergeJson(String json1, String json2) {
        JsonNode node1 = parseJson(json1);
        JsonNode node2 = parseJson(json2);
        
        if (node1 == null && node2 == null) {
            return null;
        }
        if (node1 == null) {
            return json2;
        }
        if (node2 == null) {
            return json1;
        }
        
        try {
            JsonNode merged = merge(node1, node2);
            return DEFAULT_MAPPER.writeValueAsString(merged);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    /**
     * 递归合并两个JsonNode
     * 
     * @param mainNode 主节点
     * @param updateNode 更新节点
     * @return 合并后的节点
     */
    private static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {
        if (updateNode.isObject() && mainNode.isObject()) {
            updateNode.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                JsonNode updateValue = entry.getValue();
                
                if (mainNode.has(fieldName)) {
                    JsonNode mainValue = mainNode.get(fieldName);
                    if (mainValue.isObject() && updateValue.isObject()) {
                        merge(mainValue, updateValue);
                    } else {
                        ((com.fasterxml.jackson.databind.node.ObjectNode) mainNode).set(fieldName, updateValue);
                    }
                } else {
                    ((com.fasterxml.jackson.databind.node.ObjectNode) mainNode).set(fieldName, updateValue);
                }
            });
            return mainNode;
        }
        return updateNode;
    }
    
    /**
     * 深拷贝对象（通过JSON序列化/反序列化）
     * 
     * @param obj   要拷贝的对象
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 拷贝后的对象，失败返回null
     */
    public static <T> T deepCopy(Object obj, Class<T> clazz) {
        if (obj == null || clazz == null) {
            return null;
        }
        String json = toJson(obj);
        return fromJson(json, clazz);
    }
    
    /**
     * 格式化JSON字符串（美化输出）
     * 
     * @param json 原JSON字符串
     * @return 格式化后的JSON字符串，失败返回原字符串
     */
    public static String formatJson(String json) {
        JsonNode node = parseJson(json);
        if (node == null) {
            return json;
        }
        return toPrettyJson(node);
    }
}

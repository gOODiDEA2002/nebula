package io.nebula.core.common.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Bean工具类
 * 提供常用的Bean操作方法
 */
public final class Beans {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    static {
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    /**
     * 私有构造函数，防止实例化
     */
    private Beans() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * 复制属性（浅拷贝）
     * 
     * @param source      源对象
     * @param targetClass 目标类型
     * @param <T>         目标类型
     * @return 目标对象
     */
    public static <T> T copyProperties(Object source, Class<T> targetClass) {
        if (source == null || targetClass == null) {
            return null;
        }
        
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy properties", e);
        }
    }
    
    /**
     * 复制属性（浅拷贝，忽略null值）
     * 
     * @param source      源对象
     * @param targetClass 目标类型
     * @param <T>         目标类型
     * @return 目标对象
     */
    public static <T> T copyPropertiesIgnoreNull(Object source, Class<T> targetClass) {
        if (source == null || targetClass == null) {
            return null;
        }
        
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target, getNullPropertyNames(source));
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy properties", e);
        }
    }
    
    /**
     * 复制属性到现有对象
     * 
     * @param source 源对象
     * @param target 目标对象
     */
    public static void copyProperties(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        BeanUtils.copyProperties(source, target);
    }
    
    /**
     * 复制属性到现有对象（忽略null值）
     * 
     * @param source 源对象
     * @param target 目标对象
     */
    public static void copyPropertiesIgnoreNull(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        BeanUtils.copyProperties(source, target, getNullPropertyNames(source));
    }
    
    /**
     * 复制属性（排除指定属性）
     * 
     * @param source           源对象
     * @param target           目标对象
     * @param ignoreProperties 要忽略的属性名
     */
    public static void copyProperties(Object source, Object target, String... ignoreProperties) {
        if (source == null || target == null) {
            return;
        }
        BeanUtils.copyProperties(source, target, ignoreProperties);
    }
    
    /**
     * Bean转Map
     * 
     * @param bean Bean对象
     * @return Map对象
     */
    public static Map<String, Object> toMap(Object bean) {
        if (bean == null) {
            return new HashMap<>();
        }
        
        try {
            return OBJECT_MAPPER.convertValue(bean, Map.class);
        } catch (Exception e) {
            // 如果转换失败，使用反射方式
            return toMapByReflection(bean);
        }
    }
    
    /**
     * Bean转Map（忽略null值）
     * 
     * @param bean Bean对象
     * @return Map对象
     */
    public static Map<String, Object> toMapIgnoreNull(Object bean) {
        Map<String, Object> map = toMap(bean);
        map.entrySet().removeIf(entry -> entry.getValue() == null);
        return map;
    }
    
    /**
     * Map转Bean
     * 
     * @param map         Map对象
     * @param targetClass 目标类型
     * @param <T>         目标类型
     * @return Bean对象
     */
    public static <T> T fromMap(Map<String, Object> map, Class<T> targetClass) {
        if (map == null || targetClass == null) {
            return null;
        }
        
        try {
            return OBJECT_MAPPER.convertValue(map, targetClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert map to bean", e);
        }
    }
    
    /**
     * 检查Bean的所有属性是否都为null
     * 
     * @param bean Bean对象
     * @return 是否所有属性都为null
     */
    public static boolean isAllFieldsNull(Object bean) {
        if (bean == null) {
            return true;
        }
        
        BeanWrapper beanWrapper = new BeanWrapperImpl(bean);
        PropertyDescriptor[] propertyDescriptors = beanWrapper.getPropertyDescriptors();
        
        for (PropertyDescriptor pd : propertyDescriptors) {
            if (!"class".equals(pd.getName()) && beanWrapper.getPropertyValue(pd.getName()) != null) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检查Bean是否有任何非null属性
     * 
     * @param bean Bean对象
     * @return 是否有非null属性
     */
    public static boolean hasNonNullField(Object bean) {
        return !isAllFieldsNull(bean);
    }
    
    /**
     * 获取Bean的所有null属性名
     * 
     * @param bean Bean对象
     * @return null属性名数组
     */
    public static String[] getNullPropertyNames(Object bean) {
        if (bean == null) {
            return new String[0];
        }
        
        BeanWrapper beanWrapper = new BeanWrapperImpl(bean);
        PropertyDescriptor[] propertyDescriptors = beanWrapper.getPropertyDescriptors();
        
        List<String> nullPropertyNames = new ArrayList<>();
        for (PropertyDescriptor pd : propertyDescriptors) {
            if (!"class".equals(pd.getName()) && beanWrapper.getPropertyValue(pd.getName()) == null) {
                nullPropertyNames.add(pd.getName());
            }
        }
        
        return nullPropertyNames.toArray(new String[0]);
    }
    
    /**
     * 获取Bean的所有非null属性名
     * 
     * @param bean Bean对象
     * @return 非null属性名数组
     */
    public static String[] getNonNullPropertyNames(Object bean) {
        if (bean == null) {
            return new String[0];
        }
        
        BeanWrapper beanWrapper = new BeanWrapperImpl(bean);
        PropertyDescriptor[] propertyDescriptors = beanWrapper.getPropertyDescriptors();
        
        List<String> nonNullPropertyNames = new ArrayList<>();
        for (PropertyDescriptor pd : propertyDescriptors) {
            if (!"class".equals(pd.getName()) && beanWrapper.getPropertyValue(pd.getName()) != null) {
                nonNullPropertyNames.add(pd.getName());
            }
        }
        
        return nonNullPropertyNames.toArray(new String[0]);
    }
    
    /**
     * 获取Bean属性值
     * 
     * @param bean         Bean对象
     * @param propertyName 属性名
     * @return 属性值
     */
    public static Object getProperty(Object bean, String propertyName) {
        if (bean == null || Strings.isBlank(propertyName)) {
            return null;
        }
        
        try {
            BeanWrapper beanWrapper = new BeanWrapperImpl(bean);
            return beanWrapper.getPropertyValue(propertyName);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 设置Bean属性值
     * 
     * @param bean         Bean对象
     * @param propertyName 属性名
     * @param value        属性值
     */
    public static void setProperty(Object bean, String propertyName, Object value) {
        if (bean == null || Strings.isBlank(propertyName)) {
            return;
        }
        
        try {
            BeanWrapper beanWrapper = new BeanWrapperImpl(bean);
            beanWrapper.setPropertyValue(propertyName, value);
        } catch (Exception e) {
            // 忽略设置失败的情况
        }
    }
    
    /**
     * 检查Bean是否具有指定属性
     * 
     * @param bean         Bean对象
     * @param propertyName 属性名
     * @return 是否具有该属性
     */
    public static boolean hasProperty(Object bean, String propertyName) {
        if (bean == null || Strings.isBlank(propertyName)) {
            return false;
        }
        
        try {
            BeanWrapper beanWrapper = new BeanWrapperImpl(bean);
            return beanWrapper.isReadableProperty(propertyName);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取Bean的所有属性名
     * 
     * @param bean Bean对象
     * @return 属性名列表
     */
    public static List<String> getPropertyNames(Object bean) {
        if (bean == null) {
            return new ArrayList<>();
        }
        
        BeanWrapper beanWrapper = new BeanWrapperImpl(bean);
        PropertyDescriptor[] propertyDescriptors = beanWrapper.getPropertyDescriptors();
        
        List<String> propertyNames = new ArrayList<>();
        for (PropertyDescriptor pd : propertyDescriptors) {
            if (!"class".equals(pd.getName())) {
                propertyNames.add(pd.getName());
            }
        }
        
        return propertyNames;
    }
    
    /**
     * 深度复制对象（使用JSON序列化/反序列化）
     * 
     * @param source      源对象
     * @param targetClass 目标类型
     * @param <T>         目标类型
     * @return 深度复制后的对象
     */
    public static <T> T deepCopy(Object source, Class<T> targetClass) {
        if (source == null || targetClass == null) {
            return null;
        }
        
        try {
            String json = OBJECT_MAPPER.writeValueAsString(source);
            return OBJECT_MAPPER.readValue(json, targetClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deep copy object", e);
        }
    }
    
    /**
     * 比较两个Bean的属性值是否相等
     * 
     * @param bean1 Bean1
     * @param bean2 Bean2
     * @return 是否相等
     */
    public static boolean equals(Object bean1, Object bean2) {
        if (bean1 == bean2) {
            return true;
        }
        if (bean1 == null || bean2 == null) {
            return false;
        }
        if (!bean1.getClass().equals(bean2.getClass())) {
            return false;
        }
        
        Map<String, Object> map1 = toMap(bean1);
        Map<String, Object> map2 = toMap(bean2);
        return Objects.equals(map1, map2);
    }
    
    /**
     * 使用反射方式将Bean转为Map
     * 
     * @param bean Bean对象
     * @return Map对象
     */
    private static Map<String, Object> toMapByReflection(Object bean) {
        Map<String, Object> map = new HashMap<>();
        
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                Object value = field.get(bean);
                map.put(field.getName(), value);
            } catch (Exception e) {
                // 忽略无法访问的字段
            }
        }
        
        return map;
    }
}

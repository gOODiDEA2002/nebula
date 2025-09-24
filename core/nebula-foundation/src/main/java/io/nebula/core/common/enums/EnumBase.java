package io.nebula.core.common.enums;

/**
 * 枚举基础接口
 * 为系统中的枚举类型提供统一的标准
 */
public interface EnumBase<T> {
    
    /**
     * 获取枚举编码
     * 
     * @return 编码值
     */
    T getCode();
    
    /**
     * 获取枚举描述
     * 
     * @return 描述信息
     */
    String getDescription();
    
    /**
     * 枚举工具类
     */
    final class EnumUtils {
        
        /**
         * 私有构造函数，防止实例化
         */
        private EnumUtils() {
            throw new UnsupportedOperationException("Utility class cannot be instantiated");
        }
        
        /**
         * 根据编码查找枚举
         * 
         * @param enumClass 枚举类
         * @param code      编码值
         * @param <E>       枚举类型
         * @param <T>       编码类型
         * @return 匹配的枚举，未找到返回null
         */
        public static <E extends Enum<E> & EnumBase<T>, T> E getByCode(Class<E> enumClass, T code) {
            if (enumClass == null || code == null) {
                return null;
            }
            
            for (E enumConstant : enumClass.getEnumConstants()) {
                if (code.equals(enumConstant.getCode())) {
                    return enumConstant;
                }
            }
            return null;
        }
        
        /**
         * 根据描述查找枚举
         * 
         * @param enumClass   枚举类
         * @param description 描述信息
         * @param <E>         枚举类型
         * @param <T>         编码类型
         * @return 匹配的枚举，未找到返回null
         */
        public static <E extends Enum<E> & EnumBase<T>, T> E getByDescription(Class<E> enumClass, String description) {
            if (enumClass == null || description == null) {
                return null;
            }
            
            for (E enumConstant : enumClass.getEnumConstants()) {
                if (description.equals(enumConstant.getDescription())) {
                    return enumConstant;
                }
            }
            return null;
        }
        
        /**
         * 检查编码是否存在
         * 
         * @param enumClass 枚举类
         * @param code      编码值
         * @param <E>       枚举类型
         * @param <T>       编码类型
         * @return 是否存在
         */
        public static <E extends Enum<E> & EnumBase<T>, T> boolean hasCode(Class<E> enumClass, T code) {
            return getByCode(enumClass, code) != null;
        }
        
        /**
         * 检查描述是否存在
         * 
         * @param enumClass   枚举类
         * @param description 描述信息
         * @param <E>         枚举类型
         * @param <T>         编码类型
         * @return 是否存在
         */
        public static <E extends Enum<E> & EnumBase<T>, T> boolean hasDescription(Class<E> enumClass, String description) {
            return getByDescription(enumClass, description) != null;
        }
        
        /**
         * 获取所有编码
         * 
         * @param enumClass 枚举类
         * @param <E>       枚举类型
         * @param <T>       编码类型
         * @return 编码数组
         */
        @SuppressWarnings("unchecked")
        public static <E extends Enum<E> & EnumBase<T>, T> T[] getAllCodes(Class<E> enumClass) {
            if (enumClass == null) {
                return null;
            }
            
            E[] enumConstants = enumClass.getEnumConstants();
            if (enumConstants.length == 0) {
                return null;
            }
            
            // 获取第一个枚举的编码类型
            T firstCode = enumConstants[0].getCode();
            if (firstCode == null) {
                return null;
            }
            
            Class<T> codeClass = (Class<T>) firstCode.getClass();
            T[] codes = (T[]) java.lang.reflect.Array.newInstance(codeClass, enumConstants.length);
            
            for (int i = 0; i < enumConstants.length; i++) {
                codes[i] = enumConstants[i].getCode();
            }
            
            return codes;
        }
        
        /**
         * 获取所有描述
         * 
         * @param enumClass 枚举类
         * @param <E>       枚举类型
         * @param <T>       编码类型
         * @return 描述数组
         */
        public static <E extends Enum<E> & EnumBase<T>, T> String[] getAllDescriptions(Class<E> enumClass) {
            if (enumClass == null) {
                return null;
            }
            
            E[] enumConstants = enumClass.getEnumConstants();
            String[] descriptions = new String[enumConstants.length];
            
            for (int i = 0; i < enumConstants.length; i++) {
                descriptions[i] = enumConstants[i].getDescription();
            }
            
            return descriptions;
        }
    }
}

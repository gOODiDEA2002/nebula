package io.nebula.core.common.util;

import java.util.Collection;
import java.util.List;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * 字符串工具类
 * 提供常用的字符串操作方法
 */
public final class Strings {
    
    private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("([a-z])([A-Z])");
    private static final Pattern SNAKE_CASE_PATTERN = Pattern.compile("_([a-z])");
    
    /**
     * 私有构造函数，防止实例化
     */
    private Strings() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * 检查字符串是否为空（null 或空字符串）
     * 
     * @param str 字符串
     * @return 是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
    
    /**
     * 检查字符串是否为空白（null、空字符串或只包含空白字符）
     * 
     * @param str 字符串
     * @return 是否为空白
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 检查字符串是否不为空
     * 
     * @param str 字符串
     * @return 是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * 检查字符串是否不为空白
     * 
     * @param str 字符串
     * @return 是否不为空白
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
    
    /**
     * 安全地修剪字符串
     * 
     * @param str 字符串
     * @return 修剪后的字符串，如果输入为null则返回null
     */
    public static String trim(String str) {
        return str == null ? null : str.trim();
    }
    
    /**
     * 安全地修剪字符串，如果为空则返回null
     * 
     * @param str 字符串
     * @return 修剪后的字符串，如果为空则返回null
     */
    public static String trimToNull(String str) {
        String trimmed = trim(str);
        return isEmpty(trimmed) ? null : trimmed;
    }
    
    /**
     * 安全地修剪字符串，如果为空则返回空字符串
     * 
     * @param str 字符串
     * @return 修剪后的字符串，如果为null则返回空字符串
     */
    public static String trimToEmpty(String str) {
        return str == null ? "" : str.trim();
    }
    
    /**
     * 如果字符串为null，则返回默认值
     * 
     * @param str          字符串
     * @param defaultValue 默认值
     * @return 字符串或默认值
     */
    public static String defaultIfNull(String str, String defaultValue) {
        return str == null ? defaultValue : str;
    }
    
    /**
     * 如果字符串为空，则返回默认值
     * 
     * @param str          字符串
     * @param defaultValue 默认值
     * @return 字符串或默认值
     */
    public static String defaultIfEmpty(String str, String defaultValue) {
        return isEmpty(str) ? defaultValue : str;
    }
    
    /**
     * 如果字符串为空白，则返回默认值
     * 
     * @param str          字符串
     * @param defaultValue 默认值
     * @return 字符串或默认值
     */
    public static String defaultIfBlank(String str, String defaultValue) {
        return isBlank(str) ? defaultValue : str;
    }
    
    /**
     * 格式化字符串（使用占位符）
     * 
     * @param template 模板字符串，使用{}作为占位符
     * @param args     参数
     * @return 格式化后的字符串
     */
    public static String format(String template, Object... args) {
        if (template == null || args == null || args.length == 0) {
            return template;
        }
        
        String result = template;
        for (Object arg : args) {
            result = result.replaceFirst("\\{}", String.valueOf(arg));
        }
        return result;
    }
    
    /**
     * 驼峰命名转下划线命名
     * 
     * @param camelCase 驼峰命名字符串
     * @return 下划线命名字符串
     */
    public static String camelToSnake(String camelCase) {
        if (isEmpty(camelCase)) {
            return camelCase;
        }
        return CAMEL_CASE_PATTERN.matcher(camelCase)
                .replaceAll("$1_$2")
                .toLowerCase();
    }
    
    /**
     * 下划线命名转驼峰命名
     * 
     * @param snakeCase 下划线命名字符串
     * @return 驼峰命名字符串
     */
    public static String snakeToCamel(String snakeCase) {
        if (isEmpty(snakeCase)) {
            return snakeCase;
        }
        return SNAKE_CASE_PATTERN.matcher(snakeCase.toLowerCase())
                .replaceAll(matchResult -> matchResult.group(1).toUpperCase());
    }
    
    /**
     * 首字母大写
     * 
     * @param str 字符串
     * @return 首字母大写的字符串
     */
    public static String capitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
    
    /**
     * 首字母小写
     * 
     * @param str 字符串
     * @return 首字母小写的字符串
     */
    public static String uncapitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
    
    /**
     * 重复字符串
     * 
     * @param str   字符串
     * @param count 重复次数
     * @return 重复后的字符串
     */
    public static String repeat(String str, int count) {
        if (str == null || count <= 0) {
            return "";
        }
        return str.repeat(count);
    }
    
    /**
     * 连接字符串数组
     * 
     * @param delimiter 分隔符
     * @param elements  字符串数组
     * @return 连接后的字符串
     */
    public static String join(String delimiter, String... elements) {
        if (elements == null || elements.length == 0) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(delimiter);
        for (String element : elements) {
            if (element != null) {
                joiner.add(element);
            }
        }
        return joiner.toString();
    }
    
    /**
     * 连接字符串集合
     * 
     * @param delimiter 分隔符
     * @param elements  字符串集合
     * @return 连接后的字符串
     */
    public static String join(String delimiter, Collection<String> elements) {
        if (elements == null || elements.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(delimiter);
        for (String element : elements) {
            if (element != null) {
                joiner.add(element);
            }
        }
        return joiner.toString();
    }
    
    /**
     * 截断字符串
     * 
     * @param str       字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串
     */
    public static String truncate(String str, int maxLength) {
        return truncate(str, maxLength, "...");
    }
    
    /**
     * 截断字符串（带后缀）
     * 
     * @param str       字符串
     * @param maxLength 最大长度
     * @param suffix    后缀
     * @return 截断后的字符串
     */
    public static String truncate(String str, int maxLength, String suffix) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        String safeSuffix = defaultIfNull(suffix, "");
        int truncateAt = Math.max(0, maxLength - safeSuffix.length());
        return str.substring(0, truncateAt) + safeSuffix;
    }
    
    /**
     * 移除字符串前缀
     * 
     * @param str    字符串
     * @param prefix 前缀
     * @return 移除前缀后的字符串
     */
    public static String removePrefix(String str, String prefix) {
        if (str == null || prefix == null || !str.startsWith(prefix)) {
            return str;
        }
        return str.substring(prefix.length());
    }
    
    /**
     * 移除字符串后缀
     * 
     * @param str    字符串
     * @param suffix 后缀
     * @return 移除后缀后的字符串
     */
    public static String removeSuffix(String str, String suffix) {
        if (str == null || suffix == null || !str.endsWith(suffix)) {
            return str;
        }
        return str.substring(0, str.length() - suffix.length());
    }
    
    /**
     * 检查字符串是否包含任意一个子字符串
     * 
     * @param str        字符串
     * @param substrings 子字符串数组
     * @return 是否包含
     */
    public static boolean containsAny(String str, String... substrings) {
        if (str == null || substrings == null) {
            return false;
        }
        for (String substring : substrings) {
            if (substring != null && str.contains(substring)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 生成随机字符串
     * 
     * @param length 长度
     * @return 随机字符串
     */
    public static String random(int length) {
        return random(length, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
    }
    
    /**
     * 生成随机字符串
     * 
     * @param length     长度
     * @param characters 字符集
     * @return 随机字符串
     */
    public static String random(int length, String characters) {
        if (length <= 0 || isEmpty(characters)) {
            return "";
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * characters.length());
            sb.append(characters.charAt(index));
        }
        return sb.toString();
    }
    
    /**
     * 安全地分割字符串
     * 
     * @param str       字符串
     * @param delimiter 分隔符
     * @return 分割后的字符串列表，如果输入为null或空则返回空列表
     */
    public static List<String> split(String str, String delimiter) {
        if (isEmpty(str)) {
            return List.of();
        }
        if (delimiter == null) {
            return List.of(str);
        }
        return Arrays.asList(str.split(Pattern.quote(delimiter), -1));
    }
    
    /**
     * 安全地比较两个字符串是否相等
     * 
     * @param str1 字符串1
     * @param str2 字符串2
     * @return 是否相等
     */
    public static boolean safeEquals(String str1, String str2) {
        if (str1 == null && str2 == null) {
            return true;
        }
        if (str1 == null || str2 == null) {
            return false;
        }
        return str1.equals(str2);
    }
    
    /**
     * 忽略大小写检查字符串是否包含子字符串
     * 
     * @param str       字符串
     * @param substring 子字符串
     * @return 是否包含
     */
    public static boolean containsIgnoreCase(String str, String substring) {
        if (str == null || substring == null) {
            return false;
        }
        return str.toLowerCase().contains(substring.toLowerCase());
    }
}

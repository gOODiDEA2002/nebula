package io.nebula.core.common.util;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 集合工具类
 * 提供常用的集合操作方法
 */
public final class Collections {
    
    /**
     * 私有构造函数，防止实例化
     */
    private Collections() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * 检查集合是否为空
     * 
     * @param collection 集合
     * @return 是否为空
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
    
    /**
     * 检查集合是否不为空
     * 
     * @param collection 集合
     * @return 是否不为空
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }
    
    /**
     * 检查Map是否为空
     * 
     * @param map Map对象
     * @return 是否为空
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }
    
    /**
     * 检查Map是否不为空
     * 
     * @param map Map对象
     * @return 是否不为空
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }
    
    /**
     * 安全获取集合大小
     * 
     * @param collection 集合
     * @return 集合大小，如果为null则返回0
     */
    public static int size(Collection<?> collection) {
        return collection == null ? 0 : collection.size();
    }
    
    /**
     * 安全获取Map大小
     * 
     * @param map Map对象
     * @return Map大小，如果为null则返回0
     */
    public static int size(Map<?, ?> map) {
        return map == null ? 0 : map.size();
    }
    
    /**
     * 如果集合为null，则返回空集合
     * 
     * @param collection 集合
     * @param <T>        元素类型
     * @return 集合或空集合
     */
    public static <T> Collection<T> emptyIfNull(Collection<T> collection) {
        return collection == null ? java.util.Collections.emptyList() : collection;
    }
    
    /**
     * 如果列表为null，则返回空列表
     * 
     * @param list 列表
     * @param <T>  元素类型
     * @return 列表或空列表
     */
    public static <T> List<T> emptyIfNull(List<T> list) {
        return list == null ? java.util.Collections.emptyList() : list;
    }
    
    /**
     * 如果Set为null，则返回空Set
     * 
     * @param set Set
     * @param <T> 元素类型
     * @return Set或空Set
     */
    public static <T> Set<T> emptyIfNull(Set<T> set) {
        return set == null ? java.util.Collections.emptySet() : set;
    }
    
    /**
     * 如果Map为null，则返回空Map
     * 
     * @param map Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return Map或空Map
     */
    public static <K, V> Map<K, V> emptyIfNull(Map<K, V> map) {
        return map == null ? java.util.Collections.emptyMap() : map;
    }
    
    /**
     * 安全地获取列表中的元素
     * 
     * @param list  列表
     * @param index 索引
     * @param <T>   元素类型
     * @return 元素或null
     */
    public static <T> T get(List<T> list, int index) {
        if (list == null || index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }
    
    /**
     * 安全地获取列表中的元素，如果不存在则返回默认值
     * 
     * @param list         列表
     * @param index        索引
     * @param defaultValue 默认值
     * @param <T>          元素类型
     * @return 元素或默认值
     */
    public static <T> T get(List<T> list, int index, T defaultValue) {
        T value = get(list, index);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 获取集合的第一个元素
     * 
     * @param collection 集合
     * @param <T>        元素类型
     * @return 第一个元素或null
     */
    public static <T> T first(Collection<T> collection) {
        if (isEmpty(collection)) {
            return null;
        }
        return collection.iterator().next();
    }
    
    /**
     * 获取集合的第一个元素，如果不存在则返回默认值
     * 
     * @param collection   集合
     * @param defaultValue 默认值
     * @param <T>          元素类型
     * @return 第一个元素或默认值
     */
    public static <T> T first(Collection<T> collection, T defaultValue) {
        T value = first(collection);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 获取列表的最后一个元素
     * 
     * @param list 列表
     * @param <T>  元素类型
     * @return 最后一个元素或null
     */
    public static <T> T last(List<T> list) {
        if (isEmpty(list)) {
            return null;
        }
        return list.get(list.size() - 1);
    }
    
    /**
     * 获取列表的最后一个元素，如果不存在则返回默认值
     * 
     * @param list         列表
     * @param defaultValue 默认值
     * @param <T>          元素类型
     * @return 最后一个元素或默认值
     */
    public static <T> T last(List<T> list, T defaultValue) {
        T value = last(list);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 创建ArrayList
     * 
     * @param elements 元素
     * @param <T>      元素类型
     * @return ArrayList
     */
    @SafeVarargs
    public static <T> List<T> listOf(T... elements) {
        if (elements == null || elements.length == 0) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(elements));
    }
    
    /**
     * 创建HashSet
     * 
     * @param elements 元素
     * @param <T>      元素类型
     * @return HashSet
     */
    @SafeVarargs
    public static <T> Set<T> setOf(T... elements) {
        if (elements == null || elements.length == 0) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(elements));
    }
    
    /**
     * 创建HashMap
     * 
     * @param <K> 键类型
     * @param <V> 值类型
     * @return HashMap
     */
    public static <K, V> Map<K, V> mapOf() {
        return new HashMap<>();
    }
    
    /**
     * 创建HashMap并添加键值对
     * 
     * @param key   键
     * @param value 值
     * @param <K>   键类型
     * @param <V>   值类型
     * @return HashMap
     */
    public static <K, V> Map<K, V> mapOf(K key, V value) {
        Map<K, V> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
    
    /**
     * 过滤集合
     * 
     * @param collection 原集合
     * @param predicate  过滤条件
     * @param <T>        元素类型
     * @return 过滤后的列表
     */
    public static <T> List<T> filter(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection) || predicate == null) {
            return new ArrayList<>();
        }
        return collection.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
    
    /**
     * 映射集合
     * 
     * @param collection 原集合
     * @param mapper     映射函数
     * @param <T>        原元素类型
     * @param <R>        映射后元素类型
     * @return 映射后的列表
     */
    public static <T, R> List<R> map(Collection<T> collection, Function<T, R> mapper) {
        if (isEmpty(collection) || mapper == null) {
            return new ArrayList<>();
        }
        return collection.stream()
                .map(mapper)
                .collect(Collectors.toList());
    }
    
    /**
     * 分组
     * 
     * @param collection 原集合
     * @param classifier 分类函数
     * @param <T>        元素类型
     * @param <K>        分组键类型
     * @return 分组后的Map
     */
    public static <T, K> Map<K, List<T>> groupBy(Collection<T> collection, Function<T, K> classifier) {
        if (isEmpty(collection) || classifier == null) {
            return new HashMap<>();
        }
        return collection.stream()
                .collect(Collectors.groupingBy(classifier));
    }
    
    /**
     * 转换为Map
     * 
     * @param collection    原集合
     * @param keyMapper     键映射函数
     * @param valueMapper   值映射函数
     * @param <T>           元素类型
     * @param <K>           键类型
     * @param <V>           值类型
     * @return Map
     */
    public static <T, K, V> Map<K, V> toMap(Collection<T> collection, 
                                           Function<T, K> keyMapper, 
                                           Function<T, V> valueMapper) {
        if (isEmpty(collection) || keyMapper == null || valueMapper == null) {
            return new HashMap<>();
        }
        return collection.stream()
                .collect(Collectors.toMap(keyMapper, valueMapper));
    }
    
    /**
     * 检查集合是否包含任意一个元素
     * 
     * @param collection 集合
     * @param elements   元素数组
     * @param <T>        元素类型
     * @return 是否包含
     */
    @SafeVarargs
    public static <T> boolean containsAny(Collection<T> collection, T... elements) {
        if (isEmpty(collection) || elements == null || elements.length == 0) {
            return false;
        }
        for (T element : elements) {
            if (collection.contains(element)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查集合是否包含所有元素
     * 
     * @param collection 集合
     * @param elements   元素数组
     * @param <T>        元素类型
     * @return 是否包含所有元素
     */
    @SafeVarargs
    public static <T> boolean containsAll(Collection<T> collection, T... elements) {
        if (isEmpty(collection)) {
            return elements == null || elements.length == 0;
        }
        if (elements == null || elements.length == 0) {
            return true;
        }
        for (T element : elements) {
            if (!collection.contains(element)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 求两个集合的交集
     * 
     * @param collection1 集合1
     * @param collection2 集合2
     * @param <T>         元素类型
     * @return 交集
     */
    public static <T> Set<T> intersection(Collection<T> collection1, Collection<T> collection2) {
        if (isEmpty(collection1) || isEmpty(collection2)) {
            return new HashSet<>();
        }
        Set<T> result = new HashSet<>(collection1);
        result.retainAll(collection2);
        return result;
    }
    
    /**
     * 求两个集合的并集
     * 
     * @param collection1 集合1
     * @param collection2 集合2
     * @param <T>         元素类型
     * @return 并集
     */
    public static <T> Set<T> union(Collection<T> collection1, Collection<T> collection2) {
        Set<T> result = new HashSet<>();
        if (isNotEmpty(collection1)) {
            result.addAll(collection1);
        }
        if (isNotEmpty(collection2)) {
            result.addAll(collection2);
        }
        return result;
    }
    
    /**
     * 求两个集合的差集（在collection1中但不在collection2中的元素）
     * 
     * @param collection1 集合1
     * @param collection2 集合2
     * @param <T>         元素类型
     * @return 差集
     */
    public static <T> Set<T> difference(Collection<T> collection1, Collection<T> collection2) {
        if (isEmpty(collection1)) {
            return new HashSet<>();
        }
        Set<T> result = new HashSet<>(collection1);
        if (isNotEmpty(collection2)) {
            result.removeAll(collection2);
        }
        return result;
    }
}

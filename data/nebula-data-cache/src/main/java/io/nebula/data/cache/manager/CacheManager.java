package io.nebula.data.cache.manager;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 统一缓存管理器接口
 * 提供各种缓存操作的抽象
 */
public interface CacheManager {
    
    // ========== 基础字符串操作 ==========
    
    /**
     * 设置缓存值
     * 
     * @param key   键
     * @param value 值
     */
    void set(String key, Object value);
    
    /**
     * 设置缓存值并指定过期时间
     * 
     * @param key      键
     * @param value    值
     * @param duration 过期时间
     */
    void set(String key, Object value, Duration duration);
    
    /**
     * 获取缓存值
     * 
     * @param key  键
     * @param type 值类型
     * @param <T>  类型参数
     * @return 值，如果不存在则返回Optional.empty()
     */
    <T> Optional<T> get(String key, Class<T> type);
    
    /**
     * 获取缓存值，如果不存在则使用默认值
     * 
     * @param key          键
     * @param type         值类型
     * @param defaultValue 默认值
     * @param <T>          类型参数
     * @return 值或默认值
     */
    <T> T get(String key, Class<T> type, T defaultValue);
    
    /**
     * 获取缓存值，如果不存在则通过Supplier获取并缓存
     * 
     * @param key      键
     * @param type     值类型
     * @param supplier 值供应器
     * @param <T>      类型参数
     * @return 值
     */
    <T> T getOrSet(String key, Class<T> type, Supplier<T> supplier);
    
    /**
     * 获取缓存值，如果不存在则通过Supplier获取并缓存（指定过期时间）
     * 
     * @param key      键
     * @param type     值类型
     * @param supplier 值供应器
     * @param duration 过期时间
     * @param <T>      类型参数
     * @return 值
     */
    <T> T getOrSet(String key, Class<T> type, Supplier<T> supplier, Duration duration);
    
    /**
     * 删除缓存
     * 
     * @param key 键
     * @return 是否成功删除
     */
    boolean delete(String key);
    
    /**
     * 批量删除缓存
     * 
     * @param keys 键集合
     * @return 实际删除的数量
     */
    long delete(Collection<String> keys);
    
    /**
     * 检查键是否存在
     * 
     * @param key 键
     * @return 是否存在
     */
    boolean exists(String key);
    
    /**
     * 设置过期时间
     * 
     * @param key      键
     * @param duration 过期时间
     * @return 是否设置成功
     */
    boolean expire(String key, Duration duration);
    
    /**
     * 获取剩余过期时间
     * 
     * @param key 键
     * @return 剩余过期时间，如果没有过期时间则返回Duration.ofSeconds(-1)
     */
    Duration getExpire(String key);
    
    /**
     * 移除过期时间（设置为永不过期）
     * 
     * @param key 键
     * @return 是否成功
     */
    boolean persist(String key);
    
    // ========== 数值操作 ==========
    
    /**
     * 递增
     * 
     * @param key 键
     * @return 递增后的值
     */
    long increment(String key);
    
    /**
     * 递增指定值
     * 
     * @param key   键
     * @param delta 递增值
     * @return 递增后的值
     */
    long increment(String key, long delta);
    
    /**
     * 递减
     * 
     * @param key 键
     * @return 递减后的值
     */
    long decrement(String key);
    
    /**
     * 递减指定值
     * 
     * @param key   键
     * @param delta 递减值
     * @return 递减后的值
     */
    long decrement(String key, long delta);
    
    // ========== Hash操作 ==========
    
    /**
     * 设置Hash字段值
     * 
     * @param key   键
     * @param field 字段
     * @param value 值
     */
    void hSet(String key, String field, Object value);
    
    /**
     * 批量设置Hash字段值
     * 
     * @param key    键
     * @param fields 字段映射
     */
    void hMSet(String key, Map<String, Object> fields);
    
    /**
     * 获取Hash字段值
     * 
     * @param key   键
     * @param field 字段
     * @param type  值类型
     * @param <T>   类型参数
     * @return 字段值
     */
    <T> Optional<T> hGet(String key, String field, Class<T> type);
    
    /**
     * 获取Hash所有字段值
     * 
     * @param key 键
     * @return 字段映射
     */
    Map<String, Object> hGetAll(String key);
    
    /**
     * 删除Hash字段
     * 
     * @param key    键
     * @param fields 字段列表
     * @return 实际删除的字段数量
     */
    long hDelete(String key, String... fields);
    
    /**
     * 检查Hash字段是否存在
     * 
     * @param key   键
     * @param field 字段
     * @return 是否存在
     */
    boolean hExists(String key, String field);
    
    /**
     * 获取Hash字段数量
     * 
     * @param key 键
     * @return 字段数量
     */
    long hLen(String key);
    
    /**
     * 获取Hash所有字段名
     * 
     * @param key 键
     * @return 字段名集合
     */
    Set<String> hKeys(String key);
    
    /**
     * Hash字段递增
     * 
     * @param key   键
     * @param field 字段
     * @param delta 递增值
     * @return 递增后的值
     */
    long hIncrement(String key, String field, long delta);
    
    // ========== List操作 ==========
    
    /**
     * 从左侧推入元素
     * 
     * @param key    键
     * @param values 元素列表
     * @return 列表长度
     */
    long lPush(String key, Object... values);
    
    /**
     * 从右侧推入元素
     * 
     * @param key    键
     * @param values 元素列表
     * @return 列表长度
     */
    long rPush(String key, Object... values);
    
    /**
     * 从左侧弹出元素
     * 
     * @param key  键
     * @param type 元素类型
     * @param <T>  类型参数
     * @return 弹出的元素
     */
    <T> Optional<T> lPop(String key, Class<T> type);
    
    /**
     * 从右侧弹出元素
     * 
     * @param key  键
     * @param type 元素类型
     * @param <T>  类型参数
     * @return 弹出的元素
     */
    <T> Optional<T> rPop(String key, Class<T> type);
    
    /**
     * 获取列表指定范围的元素
     * 
     * @param key   键
     * @param start 开始索引
     * @param end   结束索引
     * @param type  元素类型
     * @param <T>   类型参数
     * @return 元素列表
     */
    <T> List<T> lRange(String key, long start, long end, Class<T> type);
    
    /**
     * 获取列表长度
     * 
     * @param key 键
     * @return 列表长度
     */
    long lLen(String key);
    
    // ========== Set操作 ==========
    
    /**
     * 添加元素到Set
     * 
     * @param key    键
     * @param values 元素列表
     * @return 实际添加的元素数量
     */
    long sAdd(String key, Object... values);
    
    /**
     * 从Set中移除元素
     * 
     * @param key    键
     * @param values 元素列表
     * @return 实际移除的元素数量
     */
    long sRem(String key, Object... values);
    
    /**
     * 检查元素是否在Set中
     * 
     * @param key   键
     * @param value 元素
     * @return 是否存在
     */
    boolean sIsMember(String key, Object value);
    
    /**
     * 获取Set所有元素
     * 
     * @param key  键
     * @param type 元素类型
     * @param <T>  类型参数
     * @return 元素集合
     */
    <T> Set<T> sMembers(String key, Class<T> type);
    
    /**
     * 获取Set元素数量
     * 
     * @param key 键
     * @return 元素数量
     */
    long sCard(String key);
    
    // ========== ZSet操作 ==========
    
    /**
     * 添加元素到有序Set
     * 
     * @param key   键
     * @param value 元素
     * @param score 分数
     * @return 是否添加成功
     */
    boolean zAdd(String key, Object value, double score);
    
    /**
     * 批量添加元素到有序Set
     * 
     * @param key    键
     * @param values 元素和分数的映射
     * @return 实际添加的元素数量
     */
    long zAdd(String key, Map<Object, Double> values);
    
    /**
     * 获取有序Set指定范围的元素（按分数升序）
     * 
     * @param key   键
     * @param start 开始索引
     * @param end   结束索引
     * @param type  元素类型
     * @param <T>   类型参数
     * @return 元素列表
     */
    <T> List<T> zRange(String key, long start, long end, Class<T> type);
    
    /**
     * 获取有序Set指定分数范围的元素
     * 
     * @param key      键
     * @param minScore 最小分数
     * @param maxScore 最大分数
     * @param type     元素类型
     * @param <T>      类型参数
     * @return 元素列表
     */
    <T> List<T> zRangeByScore(String key, double minScore, double maxScore, Class<T> type);
    
    /**
     * 获取元素在有序Set中的排名
     * 
     * @param key   键
     * @param value 元素
     * @return 排名（从0开始），如果元素不存在则返回null
     */
    Long zRank(String key, Object value);
    
    /**
     * 获取元素的分数
     * 
     * @param key   键
     * @param value 元素
     * @return 分数，如果元素不存在则返回null
     */
    Double zScore(String key, Object value);
    
    /**
     * 获取有序Set元素数量
     * 
     * @param key 键
     * @return 元素数量
     */
    long zCard(String key);
    
    // ========== 模式匹配 ==========
    
    /**
     * 根据模式获取键列表
     * 
     * @param pattern 模式（支持*、?等通配符）
     * @return 匹配的键列表
     */
    Set<String> keys(String pattern);
    
    /**
     * 扫描键（推荐用于生产环境，避免阻塞）
     * 
     * @param pattern 模式
     * @param count   每次扫描的数量
     * @return 匹配的键集合
     */
    Set<String> scan(String pattern, long count);
    
    // ========== 异步操作 ==========
    
    /**
     * 异步设置缓存值
     * 
     * @param key   键
     * @param value 值
     * @return CompletableFuture
     */
    CompletableFuture<Void> setAsync(String key, Object value);
    
    /**
     * 异步获取缓存值
     * 
     * @param key  键
     * @param type 值类型
     * @param <T>  类型参数
     * @return CompletableFuture包装的值
     */
    <T> CompletableFuture<Optional<T>> getAsync(String key, Class<T> type);
    
    /**
     * 异步删除缓存
     * 
     * @param key 键
     * @return CompletableFuture包装的删除结果
     */
    CompletableFuture<Boolean> deleteAsync(String key);
    
    // ========== 管理操作 ==========
    
    /**
     * 清空所有缓存
     */
    void clear();
    
    /**
     * 获取缓存统计信息
     * 
     * @return 统计信息
     */
    CacheStats getStats();
    
    /**
     * 获取缓存名称
     * 
     * @return 缓存名称
     */
    String getName();
    
    /**
     * 检查缓存是否可用
     * 
     * @return 是否可用
     */
    boolean isAvailable();
    
    /**
     * 缓存统计信息
     */
    interface CacheStats {
        /**
         * 获取命中次数
         * 
         * @return 命中次数
         */
        long getHitCount();
        
        /**
         * 获取未命中次数
         * 
         * @return 未命中次数
         */
        long getMissCount();
        
        /**
         * 获取命中率
         * 
         * @return 命中率（0.0-1.0）
         */
        double getHitRate();
        
        /**
         * 获取缓存数量
         * 
         * @return 缓存数量
         */
        long getSize();
        
        /**
         * 获取驱逐次数
         * 
         * @return 驱逐次数
         */
        long getEvictionCount();
    }
}

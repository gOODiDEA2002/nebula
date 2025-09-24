package io.nebula.web.cache;

/**
 * 响应缓存接口
 */
public interface ResponseCache {
    
    /**
     * 获取缓存的响应
     * 
     * @param key 缓存键
     * @return 缓存的响应数据，如果不存在返回null
     */
    CachedResponse get(String key);
    
    /**
     * 存储响应到缓存
     * 
     * @param key 缓存键
     * @param response 响应数据
     * @param ttlSeconds 过期时间（秒）
     */
    void put(String key, CachedResponse response, int ttlSeconds);
    
    /**
     * 删除缓存
     * 
     * @param key 缓存键
     */
    void remove(String key);
    
    /**
     * 清空所有缓存
     */
    void clear();
    
    /**
     * 检查缓存是否存在
     * 
     * @param key 缓存键
     * @return 是否存在
     */
    boolean exists(String key);
    
    /**
     * 获取缓存大小
     * 
     * @return 缓存条目数量
     */
    int size();
}

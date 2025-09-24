package io.nebula.web.ratelimit;

/**
 * 限流器接口
 */
public interface RateLimiter {
    
    /**
     * 尝试获取许可
     * 
     * @param key 限流键
     * @param permits 需要的许可数量
     * @return 是否获取成功
     */
    boolean tryAcquire(String key, int permits);
    
    /**
     * 尝试获取许可（默认获取1个许可）
     * 
     * @param key 限流键
     * @return 是否获取成功
     */
    default boolean tryAcquire(String key) {
        return tryAcquire(key, 1);
    }
    
    /**
     * 获取当前剩余许可数
     * 
     * @param key 限流键
     * @return 剩余许可数
     */
    int getAvailablePermits(String key);
    
    /**
     * 重置限流器状态
     * 
     * @param key 限流键
     */
    void reset(String key);
}

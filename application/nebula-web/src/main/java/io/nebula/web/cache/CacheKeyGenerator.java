package io.nebula.web.cache;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 缓存键生成器接口
 */
public interface CacheKeyGenerator {
    
    /**
     * 生成缓存键
     * 
     * @param request HTTP请求
     * @return 缓存键
     */
    String generateKey(HttpServletRequest request);
}

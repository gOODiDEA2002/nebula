package io.nebula.web.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 限流键生成器接口
 */
public interface RateLimitKeyGenerator {
    
    /**
     * 生成限流键
     * 
     * @param request HTTP请求
     * @return 限流键
     */
    String generateKey(HttpServletRequest request);
}

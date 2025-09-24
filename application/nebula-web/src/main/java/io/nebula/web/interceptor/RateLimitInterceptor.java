package io.nebula.web.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.core.common.result.Result;
import io.nebula.web.autoconfigure.WebProperties;
import io.nebula.web.ratelimit.RateLimiter;
import io.nebula.web.ratelimit.RateLimitKeyGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 限流拦截器
 * 基于配置的策略对请求进行限流
 */
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);
    
    private final RateLimiter rateLimiter;
    private final RateLimitKeyGenerator keyGenerator;
    private final WebProperties.RateLimit config;
    private final ObjectMapper objectMapper;
    
    public RateLimitInterceptor(RateLimiter rateLimiter, 
                               RateLimitKeyGenerator keyGenerator,
                               WebProperties.RateLimit config,
                               ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.keyGenerator = keyGenerator;
        this.config = config;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        if (!config.isEnabled()) {
            return true;
        }
        
        String key = keyGenerator.generateKey(request);
        boolean acquired = rateLimiter.tryAcquire(key);
        
        if (!acquired) {
            handleRateLimitExceeded(request, response, key);
            return false;
        }
        
        // 添加限流信息到响应头
        addRateLimitHeaders(response, key);
        return true;
    }
    
    /**
     * 处理限流超出情况
     */
    private void handleRateLimitExceeded(HttpServletRequest request, HttpServletResponse response, String key) 
            throws Exception {
        
        logger.warn("Rate limit exceeded for key: {} from request: {} {}", 
            key, request.getMethod(), request.getRequestURI());
        
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        
        // 创建错误响应
        Result<Void> errorResult = Result.error("RATE_LIMIT_EXCEEDED", config.getLimitExceededMessage());
        
        // 添加限流信息到响应头
        addRateLimitHeaders(response, key);
        
        // 写入响应
        String responseBody = objectMapper.writeValueAsString(errorResult);
        response.getWriter().write(responseBody);
        response.getWriter().flush();
    }
    
    /**
     * 添加限流相关的响应头
     */
    private void addRateLimitHeaders(HttpServletResponse response, String key) {
        try {
            int availablePermits = rateLimiter.getAvailablePermits(key);
            response.setHeader("X-RateLimit-Limit", String.valueOf(config.getDefaultRequestsPerSecond()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(availablePermits));
            response.setHeader("X-RateLimit-Window", String.valueOf(config.getTimeWindow()));
        } catch (Exception e) {
            logger.warn("Failed to add rate limit headers: {}", e.getMessage());
        }
    }
}

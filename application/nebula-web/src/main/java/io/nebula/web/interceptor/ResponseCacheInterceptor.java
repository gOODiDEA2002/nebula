package io.nebula.web.interceptor;

import io.nebula.web.autoconfigure.WebProperties;
import io.nebula.web.cache.CacheKeyGenerator;
import io.nebula.web.cache.CachedResponse;
import io.nebula.web.cache.ResponseCache;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.util.HashMap;
import java.util.Map;

/**
 * 响应缓存拦截器
 * 支持GET请求的响应缓存
 */
public class ResponseCacheInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(ResponseCacheInterceptor.class);
    private static final String CACHE_KEY_ATTRIBUTE = "CACHE_KEY";
    private static final String CACHE_ENABLED_ATTRIBUTE = "CACHE_ENABLED";
    
    private final ResponseCache responseCache;
    private final CacheKeyGenerator keyGenerator;
    private final WebProperties.Cache config;
    
    public ResponseCacheInterceptor(ResponseCache responseCache, 
                                  CacheKeyGenerator keyGenerator,
                                  WebProperties.Cache config) {
        this.responseCache = responseCache;
        this.keyGenerator = keyGenerator;
        this.config = config;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        if (!config.isEnabled() || !isCacheable(request)) {
            return true;
        }
        
        String cacheKey = keyGenerator.generateKey(request);
        request.setAttribute(CACHE_KEY_ATTRIBUTE, cacheKey);
        request.setAttribute(CACHE_ENABLED_ATTRIBUTE, true);
        
        // 尝试从缓存获取响应
        CachedResponse cachedResponse = responseCache.get(cacheKey);
        if (cachedResponse != null) {
            writeCachedResponse(response, cachedResponse);
            logger.debug("Cache hit: {}", cacheKey);
            return false; // 阻止继续处理，直接返回缓存的响应
        }
        
        logger.debug("Cache miss: {}", cacheKey);
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                              Object handler, Exception ex) {
        
        Boolean cacheEnabled = (Boolean) request.getAttribute(CACHE_ENABLED_ATTRIBUTE);
        if (cacheEnabled == null || !cacheEnabled || ex != null) {
            return;
        }
        
        String cacheKey = (String) request.getAttribute(CACHE_KEY_ATTRIBUTE);
        if (cacheKey == null) {
            return;
        }
        
        // 只缓存成功的响应
        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            cacheResponse(cacheKey, response);
        }
    }
    
    /**
     * 判断请求是否可缓存
     */
    private boolean isCacheable(HttpServletRequest request) {
        // 只缓存GET请求
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        
        // 检查是否有No-Cache头
        String cacheControl = request.getHeader("Cache-Control");
        if (cacheControl != null && 
            (cacheControl.contains("no-cache") || cacheControl.contains("no-store"))) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 写入缓存的响应
     */
    private void writeCachedResponse(HttpServletResponse response, CachedResponse cachedResponse) 
            throws Exception {
        
        // 设置状态码
        response.setStatus(cachedResponse.getStatus());
        
        // 设置响应头
        if (cachedResponse.getHeaders() != null) {
            cachedResponse.getHeaders().forEach(response::setHeader);
        }
        
        // 设置内容类型和编码
        if (cachedResponse.getContentType() != null) {
            response.setContentType(cachedResponse.getContentType());
        }
        if (cachedResponse.getCharacterEncoding() != null) {
            response.setCharacterEncoding(cachedResponse.getCharacterEncoding());
        }
        
        // 添加缓存相关头
        response.setHeader("X-Cache", "HIT");
        response.setHeader("X-Cache-Time", String.valueOf(cachedResponse.getCreatedTime()));
        
        // 写入响应体
        if (cachedResponse.getBody() != null && cachedResponse.getBody().length > 0) {
            response.getOutputStream().write(cachedResponse.getBody());
            response.getOutputStream().flush();
        }
    }
    
    /**
     * 缓存响应
     */
    private void cacheResponse(String cacheKey, HttpServletResponse response) {
        try {
            if (!(response instanceof ContentCachingResponseWrapper)) {
                logger.debug("Response is not wrapped for caching: {}", cacheKey);
                return;
            }
            
            ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) response;
            
            // 收集响应头
            Map<String, String> headers = new HashMap<>();
            response.getHeaderNames().forEach(name -> {
                String value = response.getHeader(name);
                if (value != null && !isExcludedHeader(name)) {
                    headers.put(name, value);
                }
            });
            
            // 获取响应体
            byte[] content = wrapper.getContentAsByteArray();
            
            // 创建缓存响应
            CachedResponse cachedResponse = new CachedResponse(
                response.getStatus(),
                headers,
                content,
                response.getContentType(),
                response.getCharacterEncoding()
            );
            
            // 存储到缓存
            responseCache.put(cacheKey, cachedResponse, config.getDefaultTtl());
            
            // 添加缓存标识头
            response.setHeader("X-Cache", "MISS");
            
            logger.debug("Response cached: {} (size: {} bytes)", cacheKey, content.length);
            
        } catch (Exception e) {
            logger.warn("Failed to cache response for key: {}, error: {}", cacheKey, e.getMessage());
        }
    }
    
    /**
     * 判断是否为需要排除的响应头
     */
    private boolean isExcludedHeader(String headerName) {
        String lowerName = headerName.toLowerCase();
        return lowerName.equals("transfer-encoding") ||
               lowerName.equals("content-encoding") ||
               lowerName.startsWith("x-cache");
    }
}

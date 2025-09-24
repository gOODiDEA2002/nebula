package io.nebula.web.filter;

import io.nebula.web.autoconfigure.WebProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * 响应缓存过滤器
 * 用于包装响应，以支持响应内容的缓存
 */
public class ResponseCacheFilter implements Filter {
    
    private final WebProperties.Cache config;
    
    public ResponseCacheFilter(WebProperties.Cache config) {
        this.config = config;
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        if (!config.isEnabled() || !(request instanceof HttpServletRequest) || 
            !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 只对GET请求进行响应包装
        if ("GET".equalsIgnoreCase(httpRequest.getMethod())) {
            // 包装响应以支持内容缓存
            ContentCachingResponseWrapper wrappedResponse = 
                new ContentCachingResponseWrapper(httpResponse);
            
            try {
                chain.doFilter(request, wrappedResponse);
            } finally {
                // 将缓存的响应内容写回原始响应
                wrappedResponse.copyBodyToResponse();
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}

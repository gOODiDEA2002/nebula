package io.nebula.web.filter;

import io.nebula.web.autoconfigure.WebProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * 请求日志过滤器
 * 用于包装请求和响应，以支持多次读取内容
 */
public class RequestLoggingFilter implements Filter {
    
    private final WebProperties.RequestLogging config;
    
    public RequestLoggingFilter(WebProperties.RequestLogging config) {
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
        
        // 包装请求和响应以支持内容缓存
        ContentCachingRequestWrapper wrappedRequest = 
            new ContentCachingRequestWrapper(httpRequest, config.getMaxRequestBodyLength());
        ContentCachingResponseWrapper wrappedResponse = 
            new ContentCachingResponseWrapper(httpResponse);
        
        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            // 将缓存的响应内容写回原始响应
            wrappedResponse.copyBodyToResponse();
        }
    }
}

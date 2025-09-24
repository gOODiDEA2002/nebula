package io.nebula.web.interceptor;

import io.nebula.web.performance.PerformanceMonitor;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 性能监控拦截器
 * 
 * @author nebula
 */
public class PerformanceMonitorInterceptor implements HandlerInterceptor {
    
    private static final String START_TIME_ATTRIBUTE = "performance.startTime";
    
    private final PerformanceMonitor performanceMonitor;
    private final String[] ignorePaths;
    
    public PerformanceMonitorInterceptor(PerformanceMonitor performanceMonitor, String[] ignorePaths) {
        this.performanceMonitor = performanceMonitor;
        this.ignorePaths = ignorePaths != null ? ignorePaths : new String[0];
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (shouldIgnore(request)) {
            return true;
        }
        
        long startTime = performanceMonitor.recordRequestStart(request);
        request.setAttribute(START_TIME_ATTRIBUTE, startTime);
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                              Object handler, Exception ex) {
        if (shouldIgnore(request)) {
            return;
        }
        
        Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
        if (startTime != null) {
            if (ex != null) {
                performanceMonitor.recordRequestError(request, ex, startTime);
            } else {
                performanceMonitor.recordRequestComplete(request, response, startTime);
            }
        }
    }
    
    private boolean shouldIgnore(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        
        for (String ignorePath : ignorePaths) {
            if (StringUtils.hasText(ignorePath)) {
                // 简单的通配符匹配
                if (ignorePath.endsWith("/**")) {
                    String prefix = ignorePath.substring(0, ignorePath.length() - 3);
                    if (requestUri.startsWith(prefix)) {
                        return true;
                    }
                } else if (ignorePath.endsWith("/*")) {
                    String prefix = ignorePath.substring(0, ignorePath.length() - 2);
                    if (requestUri.startsWith(prefix)) {
                        String remaining = requestUri.substring(prefix.length());
                        if (!remaining.contains("/")) {
                            return true;
                        }
                    }
                } else if (requestUri.equals(ignorePath)) {
                    return true;
                }
            }
        }
        
        return false;
    }
}

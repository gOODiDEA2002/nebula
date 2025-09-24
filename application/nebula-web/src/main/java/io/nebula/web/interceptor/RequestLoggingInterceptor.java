package io.nebula.web.interceptor;

import io.nebula.web.autoconfigure.WebProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 请求日志拦截器
 * 记录 HTTP 请求和响应的详细信息
 */
public class RequestLoggingInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String START_TIME_ATTRIBUTE = "REQUEST_START_TIME";
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    private final WebProperties.RequestLogging config;
    
    public RequestLoggingInterceptor(WebProperties.RequestLogging config) {
        this.config = config;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!config.isEnabled() || shouldIgnore(request.getRequestURI())) {
            return true;
        }
        
        // 记录开始时间
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        
        // 包装请求以支持多次读取
        if (!(request instanceof ContentCachingRequestWrapper)) {
            request = new ContentCachingRequestWrapper(request, config.getMaxRequestBodyLength());
        }
        
        // 包装响应以支持内容缓存
        if (!(response instanceof ContentCachingResponseWrapper)) {
            response = new ContentCachingResponseWrapper(response);
        }
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                              Object handler, Exception ex) {
        if (!config.isEnabled() || shouldIgnore(request.getRequestURI())) {
            return;
        }
        
        Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
        if (startTime == null) {
            return;
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        try {
            logRequest(request, response, duration, ex);
        } catch (Exception e) {
            logger.warn("Failed to log request: {}", e.getMessage());
        }
    }
    
    /**
     * 记录请求信息
     */
    private void logRequest(HttpServletRequest request, HttpServletResponse response, 
                           long duration, Exception ex) {
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n=== HTTP Request Log ===\n");
        
        // 基本信息
        logMessage.append(String.format("Method: %s\n", request.getMethod()));
        logMessage.append(String.format("URI: %s\n", request.getRequestURI()));
        logMessage.append(String.format("Query: %s\n", request.getQueryString()));
        logMessage.append(String.format("Remote IP: %s\n", getClientIpAddress(request)));
        logMessage.append(String.format("User Agent: %s\n", request.getHeader("User-Agent")));
        logMessage.append(String.format("Duration: %d ms\n", duration));
        logMessage.append(String.format("Status: %d\n", response.getStatus()));
        
        // 请求头
        if (config.isIncludeHeaders()) {
            logMessage.append("Request Headers:\n");
            Map<String, String> headers = getRequestHeaders(request);
            headers.forEach((name, value) -> 
                logMessage.append(String.format("  %s: %s\n", name, value)));
        }
        
        // 请求体
        if (config.isIncludeRequestBody()) {
            String requestBody = getRequestBody(request);
            if (StringUtils.hasText(requestBody)) {
                logMessage.append("Request Body:\n");
                logMessage.append(requestBody).append("\n");
            }
        }
        
        // 响应体
        if (config.isIncludeResponseBody()) {
            String responseBody = getResponseBody(response);
            if (StringUtils.hasText(responseBody)) {
                logMessage.append("Response Body:\n");
                logMessage.append(responseBody).append("\n");
            }
        }
        
        // 异常信息
        if (ex != null) {
            logMessage.append(String.format("Exception: %s\n", ex.getMessage()));
        }
        
        logMessage.append("=== End HTTP Request Log ===");
        
        // 根据状态码和异常情况选择日志级别
        if (ex != null || response.getStatus() >= 500) {
            logger.error(logMessage.toString());
        } else if (response.getStatus() >= 400) {
            logger.warn(logMessage.toString());
        } else if (duration > 1000) {
            logger.warn("[SLOW REQUEST] {}", logMessage.toString());
        } else {
            logger.info(logMessage.toString());
        }
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor) && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0];
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp) && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 获取请求头
     */
    private Map<String, String> getRequestHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            
            // 敏感信息脱敏
            if (isSensitiveHeader(headerName)) {
                headerValue = maskSensitiveValue(headerValue);
            }
            
            headers.put(headerName, headerValue);
        }
        
        return headers;
    }
    
    /**
     * 获取请求体内容
     */
    private String getRequestBody(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper) {
            ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;
            byte[] content = wrapper.getContentAsByteArray();
            if (content.length > 0) {
                try {
                    String body = new String(content, wrapper.getCharacterEncoding());
                    return truncateContent(body, config.getMaxRequestBodyLength());
                } catch (UnsupportedEncodingException e) {
                    return "[Error reading request body: " + e.getMessage() + "]";
                }
            }
        }
        return null;
    }
    
    /**
     * 获取响应体内容
     */
    private String getResponseBody(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper) {
            ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) response;
            byte[] content = wrapper.getContentAsByteArray();
            if (content.length > 0) {
                try {
                    String body = new String(content, wrapper.getCharacterEncoding());
                    return truncateContent(body, config.getMaxResponseBodyLength());
                } catch (UnsupportedEncodingException e) {
                    return "[Error reading response body: " + e.getMessage() + "]";
                }
            }
        }
        return null;
    }
    
    /**
     * 判断是否应该忽略该路径
     */
    private boolean shouldIgnore(String requestURI) {
        if (config.getIgnorePaths() == null) {
            return false;
        }
        
        for (String pattern : config.getIgnorePaths()) {
            if (pathMatcher.match(pattern, requestURI)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 判断是否为敏感请求头
     */
    private boolean isSensitiveHeader(String headerName) {
        String lowerName = headerName.toLowerCase();
        return lowerName.contains("authorization") || 
               lowerName.contains("cookie") || 
               lowerName.contains("token") ||
               lowerName.contains("password");
    }
    
    /**
     * 脱敏敏感值
     */
    private String maskSensitiveValue(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        
        if (value.length() <= 4) {
            return "****";
        }
        
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
    
    /**
     * 截取内容到指定长度
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        
        return content.substring(0, maxLength) + "... [truncated]";
    }
}

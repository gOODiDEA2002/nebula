package io.nebula.web.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * 默认限流键生成器
 * 根据策略生成不同的限流键
 */
public class DefaultRateLimitKeyGenerator implements RateLimitKeyGenerator {
    
    private final String strategy;
    
    public DefaultRateLimitKeyGenerator(String strategy) {
        this.strategy = strategy != null ? strategy.toUpperCase() : "IP";
    }
    
    @Override
    public String generateKey(HttpServletRequest request) {
        switch (strategy) {
            case "IP":
                return "ip:" + getClientIpAddress(request);
            case "USER":
                return "user:" + getUserIdentifier(request);
            case "API":
                return "api:" + request.getMethod() + ":" + request.getRequestURI();
            case "IP_API":
                return "ip_api:" + getClientIpAddress(request) + ":" + 
                       request.getMethod() + ":" + request.getRequestURI();
            case "USER_API":
                return "user_api:" + getUserIdentifier(request) + ":" + 
                       request.getMethod() + ":" + request.getRequestURI();
            default:
                return "global";
        }
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor) && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp) && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        String remoteAddr = request.getRemoteAddr();
        return StringUtils.hasText(remoteAddr) ? remoteAddr : "unknown";
    }
    
    /**
     * 获取用户标识
     */
    private String getUserIdentifier(HttpServletRequest request) {
        // 尝试从认证头获取用户信息
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader)) {
            // 这里可以解析JWT或其他认证信息获取用户ID
            // 为了简化，我们使用认证头的hash值
            return String.valueOf(authHeader.hashCode());
        }
        
        // 尝试从Session获取用户信息
        if (request.getSession(false) != null) {
            Object userId = request.getSession().getAttribute("userId");
            if (userId != null) {
                return userId.toString();
            }
        }
        
        // 如果无法获取用户信息，回退到IP地址
        return getClientIpAddress(request);
    }
}

package io.nebula.web.ratelimit;

import io.nebula.web.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * 默认限流键生成器
 * 根据策略生成不同的限流键
 *
 * <p>IP 解析委托 {@link ClientIpResolver}: 默认不信任转发头(客户端伪造 XFF 即可绕过
 * IP 限流), 仅当请求来自 nebula.web.trusted-proxies 配置的可信代理时才解析 XFF。</p>
 */
public class DefaultRateLimitKeyGenerator implements RateLimitKeyGenerator {
    
    private final String strategy;
    private final ClientIpResolver clientIpResolver;
    
    public DefaultRateLimitKeyGenerator(String strategy, ClientIpResolver clientIpResolver) {
        this.strategy = strategy != null ? strategy.toUpperCase() : "IP";
        this.clientIpResolver = clientIpResolver;
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
     * 获取客户端IP地址（可信代理感知，防 XFF 伪造绕过限流）
     */
    private String getClientIpAddress(HttpServletRequest request) {
        return clientIpResolver.resolve(request);
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

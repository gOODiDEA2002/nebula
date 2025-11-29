package io.nebula.gateway.config;

import io.nebula.gateway.config.GatewayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * 限流Key解析器配置
 * <p>
 * 框架层提供两种通用限流策略：
 * - ip: 基于客户端IP限流（默认）
 * - path: 基于请求路径限流
 * <p>
 * 注意：如需基于用户或其他业务维度限流，请在应用层自定义 KeyResolver
 */
@Configuration
@ConditionalOnClass(KeyResolver.class)
@ConditionalOnProperty(prefix = "nebula.gateway.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class RateLimitKeyResolverConfig {
    
    private final GatewayProperties gatewayProperties;
    
    /**
     * 基于策略配置的统一限流Key解析器
     * 根据配置的strategy自动选择IP或Path策略
     */
    @Bean
    @Primary
    public KeyResolver defaultKeyResolver() {
        return exchange -> {
            String strategy = gatewayProperties.getRateLimit().getStrategy();
            
            switch (strategy) {
                case "path":
                    return Mono.just("path:" + exchange.getRequest().getPath().value());
                    
                case "ip":
                default:
                    return Mono.just("ip:" + getClientIp(exchange));
            }
        };
    }
    
    /**
     * 获取客户端真实IP
     */
    private String getClientIp(org.springframework.web.server.ServerWebExchange exchange) {
        // 从X-Forwarded-For获取
        String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        
        // 从X-Real-IP获取
        String xri = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xri != null && !xri.isEmpty()) {
            return xri;
        }
        
        // 从远程地址获取
        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        
        return "unknown";
    }
}


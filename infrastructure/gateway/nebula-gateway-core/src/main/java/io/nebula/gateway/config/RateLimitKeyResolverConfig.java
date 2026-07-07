package io.nebula.gateway.config;

import io.nebula.gateway.util.ReactiveClientIpResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * 限流 Key 解析器配置
 * <p>
 * 框架层提供两种通用限流策略：
 * - ip: 基于客户端 IP 限流（默认）
 * - path: 基于请求路径限流
 * <p>
 * IP 解析遵循可信代理策略：默认不信任 XFF，
 * 需在 nebula.gateway.logging.trusted-proxies 配置可信代理后才解析。
 */
@Configuration
@ConditionalOnClass(KeyResolver.class)
@ConditionalOnProperty(prefix = "nebula.gateway.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class RateLimitKeyResolverConfig {
    
    private final GatewayProperties gatewayProperties;
    private final ReactiveClientIpResolver clientIpResolver;
    
    @Bean
    @Primary
    public KeyResolver defaultKeyResolver() {
        return exchange -> {
            String strategy = gatewayProperties.getRateLimit().getStrategy();
            
            return switch (strategy) {
                case "path" -> Mono.just("path:" + exchange.getRequest().getPath().value());
                default -> Mono.just("ip:" + clientIpResolver.resolve(exchange.getRequest()));
            };
        };
    }
}


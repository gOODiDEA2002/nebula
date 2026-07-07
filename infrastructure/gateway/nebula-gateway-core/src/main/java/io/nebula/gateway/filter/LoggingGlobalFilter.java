package io.nebula.gateway.filter;

import io.nebula.gateway.config.GatewayProperties;
import io.nebula.gateway.util.ReactiveClientIpResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 全局日志过滤器
 * <p>
 * 功能：
 * - 为每个请求生成唯一的RequestId
 * - 记录请求开始和结束日志
 * - 统计请求耗时
 * - 标记慢请求
 */
@Slf4j
@RequiredArgsConstructor
public class LoggingGlobalFilter implements GlobalFilter, Ordered {
    
    private final GatewayProperties gatewayProperties;
    private final ReactiveClientIpResolver clientIpResolver;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        GatewayProperties.LoggingConfig loggingConfig = gatewayProperties.getLogging();
        
        if (!loggingConfig.isEnabled()) {
            return chain.filter(exchange);
        }
        
        ServerHttpRequest request = exchange.getRequest();
        
        String requestId = generateRequestId();
        long startTime = System.currentTimeMillis();
        
        log.info("[{}] >>> {} {} from {}", 
                requestId,
                request.getMethod(),
                request.getPath(),
                clientIpResolver.resolve(request));
        
        ServerHttpRequest modifiedRequest = request.mutate()
                .header(loggingConfig.getRequestIdHeader(), requestId)
                .build();
        
        return chain.filter(exchange.mutate().request(modifiedRequest).build())
                .then(Mono.fromRunnable(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    int statusCode = exchange.getResponse().getStatusCode() != null 
                            ? exchange.getResponse().getStatusCode().value() 
                            : 0;
                    
                    String slowMark = duration > loggingConfig.getSlowRequestThreshold() ? " [SLOW]" : "";
                    
                    log.info("[{}] <<< {} {} - {} ({}ms){}", 
                            requestId,
                            request.getMethod(),
                            request.getPath(),
                            statusCode,
                            duration,
                            slowMark);
                }));
    }
    
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
    
    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}


package io.nebula.gateway.filter;

import io.nebula.gateway.config.GatewayProperties;
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
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        GatewayProperties.LoggingConfig loggingConfig = gatewayProperties.getLogging();
        
        // 检查是否启用日志
        if (!loggingConfig.isEnabled()) {
            return chain.filter(exchange);
        }
        
        ServerHttpRequest request = exchange.getRequest();
        
        // 生成请求ID
        String requestId = generateRequestId();
        long startTime = System.currentTimeMillis();
        
        // 记录请求开始
        log.info("[{}] >>> {} {} from {}", 
                requestId,
                request.getMethod(),
                request.getPath(),
                getClientIp(request));
        
        // 将请求ID添加到请求头
        ServerHttpRequest modifiedRequest = request.mutate()
                .header(loggingConfig.getRequestIdHeader(), requestId)
                .build();
        
        return chain.filter(exchange.mutate().request(modifiedRequest).build())
                .then(Mono.fromRunnable(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    int statusCode = exchange.getResponse().getStatusCode() != null 
                            ? exchange.getResponse().getStatusCode().value() 
                            : 0;
                    
                    // 判断是否为慢请求
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
        // 最高优先级，确保最先执行
        return Ordered.HIGHEST_PRECEDENCE;
    }
    
    /**
     * 生成请求ID
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * 获取客户端IP
     */
    private String getClientIp(ServerHttpRequest request) {
        // 优先从X-Forwarded-For获取
        String xff = request.getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            // 取第一个IP（真实客户端IP）
            return xff.split(",")[0].trim();
        }
        
        // 从X-Real-IP获取
        String xri = request.getHeaders().getFirst("X-Real-IP");
        if (xri != null && !xri.isEmpty()) {
            return xri;
        }
        
        // 从远程地址获取
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        
        return "unknown";
    }
}


package io.nebula.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.nebula.gateway.config.GatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT认证网关过滤器工厂
 * <p>
 * 在Spring Cloud Gateway中使用：
 * <pre>
 * spring:
 *   cloud:
 *     gateway:
 *       routes:
 *         - id: my-route
 *           uri: lb://my-service
 *           filters:
 *             - JwtAuth
 * </pre>
 */
@Slf4j
public class JwtAuthGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthGatewayFilterFactory.Config> {
    
    private final GatewayProperties gatewayProperties;
    private final PathMatcher pathMatcher = new AntPathMatcher();
    
    public JwtAuthGatewayFilterFactory(GatewayProperties gatewayProperties) {
        super(Config.class);
        this.gatewayProperties = gatewayProperties;
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        GatewayProperties.JwtConfig jwtConfig = gatewayProperties.getJwt();
        
        return (exchange, chain) -> {
            // 检查是否启用JWT认证
            if (!jwtConfig.isEnabled()) {
                return chain.filter(exchange);
            }
            
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();
            
            // 检查白名单
            if (isWhitelisted(path, jwtConfig.getWhitelist())) {
                return chain.filter(exchange);
            }
            
            // 获取Token
            String token = extractToken(request, jwtConfig);
            if (!StringUtils.hasText(token)) {
                log.warn("缺少认证Token: path={}", path);
                return unauthorized(exchange, "缺少认证Token");
            }
            
            // 验证Token
            try {
                Claims claims = validateToken(token, jwtConfig.getSecret());
                
                // 将用户信息添加到请求头
                ServerHttpRequest.Builder requestBuilder = request.mutate();
                
                // 添加用户ID
                String subject = claims.getSubject();
                if (StringUtils.hasText(subject)) {
                    requestBuilder.header(jwtConfig.getUserIdHeader(), subject);
                }
                
                // 添加自定义Claims映射
                for (String mapping : jwtConfig.getClaimHeaders()) {
                    String[] parts = mapping.split(":");
                    if (parts.length == 2) {
                        String claimName = parts[0].trim();
                        String headerName = parts[1].trim();
                        Object claimValue = claims.get(claimName);
                        if (claimValue != null) {
                            requestBuilder.header(headerName, claimValue.toString());
                        }
                    }
                }
                
                ServerHttpRequest modifiedRequest = requestBuilder.build();
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
                
            } catch (Exception e) {
                log.warn("Token验证失败: path={}, error={}", path, e.getMessage());
                return unauthorized(exchange, "Token无效或已过期");
            }
        };
    }
    
    @Override
    public String name() {
        return "JwtAuth";
    }
    
    /**
     * 检查是否在白名单
     */
    private boolean isWhitelisted(String path, List<String> whitelist) {
        if (whitelist == null || whitelist.isEmpty()) {
            return false;
        }
        return whitelist.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
    
    /**
     * 从请求中提取Token
     */
    private String extractToken(ServerHttpRequest request, GatewayProperties.JwtConfig jwtConfig) {
        String header = request.getHeaders().getFirst(jwtConfig.getHeader());
        String prefix = jwtConfig.getPrefix();
        if (StringUtils.hasText(header) && header.startsWith(prefix)) {
            return header.substring(prefix.length());
        }
        return null;
    }
    
    /**
     * 验证Token
     */
    private Claims validateToken(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * 返回未授权响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        
        String body = String.format("{\"success\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"%s\"}", message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8))));
    }
    
    /**
     * 过滤器配置
     */
    public static class Config {
        // 可通过路由配置覆盖的属性（暂无）
    }
}


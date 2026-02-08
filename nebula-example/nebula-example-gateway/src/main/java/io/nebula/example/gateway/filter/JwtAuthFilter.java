package io.nebula.example.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 认证过滤器
 * 
 * 功能：
 * - 验证 JWT Token
 * - 将用户信息注入请求头传递给后端服务
 * - 白名单路径放行
 * 
 * @author Nebula Framework Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${nebula.gateway.auth.jwt.secret}")
    private String jwtSecret;

    @Value("${nebula.gateway.auth.jwt.header:Authorization}")
    private String headerName;

    @Value("${nebula.gateway.auth.jwt.prefix:Bearer }")
    private String tokenPrefix;

    @Value("${nebula.gateway.auth.jwt.whitelist}")
    private List<String> whitelist;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 1. 检查白名单
        if (isWhitelisted(path)) {
            log.debug("路径 {} 在白名单中，跳过认证", path);
            return chain.filter(exchange);
        }

        // 2. 提取 Token
        String token = extractToken(exchange.getRequest());
        if (token == null) {
            log.warn("请求 {} 缺少认证 Token", path);
            return unauthorized(exchange, "Missing authentication token");
        }

        // 3. 验证 Token
        try {
            Claims claims = validateToken(token);
            String userId = claims.getSubject();
            String username = claims.get("username", String.class);

            log.debug("认证成功: userId={}, username={}", userId, username);

            // 4. 将用户信息注入请求头
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Name", username != null ? username : "")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.warn("Token 验证失败: {}", e.getMessage());
            return unauthorized(exchange, "Invalid or expired token");
        }
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhitelisted(String path) {
        if (whitelist == null || whitelist.isEmpty()) {
            return false;
        }
        return whitelist.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 从请求头提取 Token
     */
    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(headerName);
        if (authHeader != null && authHeader.startsWith(tokenPrefix)) {
            return authHeader.substring(tokenPrefix.length()).trim();
        }
        return null;
    }

    /**
     * 验证 Token 并返回 Claims
     */
    private Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 返回 401 未授权响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        
        String body = String.format(
                "{\"code\":\"UNAUTHORIZED\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                message, java.time.LocalDateTime.now());
        
        return response.writeWith(Mono.just(
                response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8))));
    }

    @Override
    public int getOrder() {
        // 最先执行
        return -100;
    }
}

package io.nebula.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.nebula.security.config.SecurityProperties;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT服务默认实现
 * 
 * 基于 JJWT 库实现JWT令牌的生成、验证和解析
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class DefaultJwtService implements JwtService {
    
    /**
     * Token类型声明键
     */
    private static final String CLAIM_TYPE = "type";
    
    /**
     * 访问Token类型值
     */
    private static final String TYPE_ACCESS = "access";
    
    /**
     * 刷新Token类型值
     */
    private static final String TYPE_REFRESH = "refresh";
    
    private final SecurityProperties properties;
    private final SecretKey signingKey;
    
    public DefaultJwtService(SecurityProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(
            properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8)
        );
        log.info("JWT服务初始化完成");
    }
    
    @Override
    public String generateAccessToken(String subject) {
        return generateAccessToken(subject, new HashMap<>());
    }
    
    @Override
    public String generateAccessToken(String subject, Map<String, Object> claims) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + properties.getJwt().getExpiration().toMillis());
        
        JwtBuilder builder = Jwts.builder()
                .subject(subject)
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey);
        
        // 添加额外声明
        if (claims != null && !claims.isEmpty()) {
            claims.forEach(builder::claim);
        }
        
        return builder.compact();
    }
    
    @Override
    public String generateRefreshToken(String subject) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + properties.getJwt().getRefreshExpiration().toMillis());
        
        return Jwts.builder()
                .subject(subject)
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }
    
    @Override
    public String validateAccessToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        
        // 检查Token类型
        String type = claims.get(CLAIM_TYPE, String.class);
        if (!TYPE_ACCESS.equals(type)) {
            log.warn("Token类型错误: expected={}, actual={}", TYPE_ACCESS, type);
            return null;
        }
        
        return claims.getSubject();
    }
    
    @Override
    public String validateRefreshToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        
        // 检查Token类型
        String type = claims.get(CLAIM_TYPE, String.class);
        if (!TYPE_REFRESH.equals(type)) {
            log.warn("Token类型错误: expected={}, actual={}", TYPE_REFRESH, type);
            return null;
        }
        
        return claims.getSubject();
    }
    
    @Override
    public <T> T getClaim(String token, String key, Class<T> type) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return claims.get(key, type);
    }
    
    @Override
    public Long getExpiration(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        Date expiration = claims.getExpiration();
        return expiration != null ? expiration.getTime() : null;
    }
    
    @Override
    public boolean isTokenExpiringSoon(String token, long thresholdSeconds) {
        Long expiration = getExpiration(token);
        if (expiration == null) {
            return true;
        }
        long remainingMillis = expiration - System.currentTimeMillis();
        return remainingMillis < thresholdSeconds * 1000;
    }
    
    /**
     * 解析Token获取Claims
     *
     * @param token JWT Token
     * @return Claims对象，解析失败返回null
     */
    private Claims parseToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("Token已过期");
            return null;
        } catch (MalformedJwtException e) {
            log.warn("Token格式错误: {}", e.getMessage());
            return null;
        } catch (SecurityException e) {
            log.warn("Token签名验证失败: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("Token解析失败: {}", e.getMessage());
            return null;
        }
    }
}


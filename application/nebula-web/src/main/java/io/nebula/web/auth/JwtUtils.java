package io.nebula.web.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 工具类
 */
@Slf4j
@RequiredArgsConstructor
public class JwtUtils {
    
    private final SecretKey secretKey;
    private final int expiration;
    private final ObjectMapper objectMapper;

    public JwtUtils(String secret, int expiration, ObjectMapper objectMapper) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.objectMapper = objectMapper;
    }
        
    /**
     * 生成JWT令牌
     * 
     * @param user 用户信息
     * @return JWT令牌
     */
    public String generateToken(AuthUser user) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expiration * 1000L);
            
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getUserId());
            claims.put("username", user.getUsername());
            
            if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                claims.put("roles", user.getRoles());
            }
            
            if (user.getPermissions() != null && !user.getPermissions().isEmpty()) {
                claims.put("permissions", user.getPermissions());
            }
            
            if (user.getExtra() != null) {
                claims.put("extra", user.getExtra());
            }
            
            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(user.getUserId())
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(secretKey)
                    .compact();
                    
        } catch (Exception e) {
            log.error("Failed to generate JWT token for user: {}", user.getUserId(), e);
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }
    
    /**
     * 从JWT令牌解析用户信息
     * 
     * @param token JWT令牌
     * @return 用户信息
     */
    public AuthUser parseToken(String token) {
        try {
            Claims claims = parseTokenClaims(token);
            if (claims == null) {
                return null;
            }
            
            AuthUser user = new AuthUser();
            user.setUserId(claims.getSubject());
            user.setUsername(claims.get("username", String.class));
            
            // 解析角色
            Object rolesObj = claims.get("roles");
            if (rolesObj != null) {
                Set<String> roles = objectMapper.convertValue(rolesObj, new TypeReference<Set<String>>() {});
                user.setRoles(roles);
            }
            
            // 解析权限
            Object permissionsObj = claims.get("permissions");
            if (permissionsObj != null) {
                Set<String> permissions = objectMapper.convertValue(permissionsObj, new TypeReference<Set<String>>() {});
                user.setPermissions(permissions);
            }
            
            // 解析扩展属性
            Object extraObj = claims.get("extra");
            if (extraObj != null) {
                user.setExtra(extraObj);
            }
            
            return user;
            
        } catch (Exception e) {
            log.warn("Failed to parse JWT token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 验证JWT令牌
     * 
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            return parseTokenClaims(token) != null;
        } catch (Exception e) {
            log.debug("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取令牌剩余有效时间（秒）
     * 
     * @param token JWT令牌
     * @return 剩余有效时间，-1表示已过期或无效
     */
    public long getTokenRemainingTime(String token) {
        try {
            Claims claims = parseTokenClaims(token);
            if (claims == null) {
                return -1;
            }
            
            Date expiration = claims.getExpiration();
            if (expiration == null) {
                return -1;
            }
            
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return remaining > 0 ? remaining / 1000 : -1;

        } catch (Exception e) {
            log.debug("Failed to get token remaining time: {}", e.getMessage());
            return -1;
        }
    }
    
    /**
     * 从令牌中获取用户ID
     * 
     * @param token JWT令牌
     * @return 用户ID
     */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = parseTokenClaims(token);
            return claims != null ? claims.getSubject() : null;
        } catch (Exception e) {
            log.debug("Failed to get user ID from token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 解析JWT令牌的Claims
     * 
     * @param token JWT令牌
     * @return Claims对象
     */
    private Claims parseTokenClaims(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
                    
        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.debug("JWT token unsupported: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.debug("JWT token malformed: {}", e.getMessage());
            throw e;
        } catch (SecurityException e) {
            log.debug("JWT token security error: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.debug("JWT token illegal argument: {}", e.getMessage());
            throw e;
        }
    }
}

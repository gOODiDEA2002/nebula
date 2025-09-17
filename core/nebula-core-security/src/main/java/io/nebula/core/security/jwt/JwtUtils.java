package io.nebula.core.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Builder;
import lombok.Data;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类
 * 提供JWT token的生成、解析和验证功能
 */
public final class JwtUtils {
    
    private static final String DEFAULT_ISSUER = "nebula";
    private static final Duration DEFAULT_EXPIRATION = Duration.ofHours(24);
    
    /**
     * 私有构造函数，防止实例化
     */
    private JwtUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * 生成密钥
     * 
     * @return 密钥
     */
    public static SecretKey generateKey() {
        return Keys.secretKeyFor(SignatureAlgorithm.HS256);
    }
    
    /**
     * 从Base64字符串创建密钥
     * 
     * @param base64Key Base64编码的密钥
     * @return 密钥
     */
    public static SecretKey keyFromBase64(String base64Key) {
        byte[] keyBytes = java.util.Base64.getDecoder().decode(base64Key);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * 将密钥转换为Base64字符串
     * 
     * @param key 密钥
     * @return Base64编码的密钥
     */
    public static String keyToBase64(SecretKey key) {
        return java.util.Base64.getEncoder().encodeToString(key.getEncoded());
    }
    
    /**
     * 生成JWT token
     * 
     * @param subject    主题（通常是用户ID）
     * @param claims     自定义声明
     * @param expiration 过期时间
     * @param key        签名密钥
     * @return JWT token
     */
    public static String generateToken(String subject, Map<String, Object> claims, Duration expiration, SecretKey key) {
        return generateToken(subject, claims, expiration, key, DEFAULT_ISSUER);
    }
    
    /**
     * 生成JWT token
     * 
     * @param subject    主题（通常是用户ID）
     * @param claims     自定义声明
     * @param expiration 过期时间
     * @param key        签名密钥
     * @param issuer     签发者
     * @return JWT token
     */
    public static String generateToken(String subject, Map<String, Object> claims, Duration expiration, SecretKey key, String issuer) {
        Instant now = Instant.now();
        Instant expiryTime = now.plus(expiration != null ? expiration : DEFAULT_EXPIRATION);
        
        JwtBuilder builder = Jwts.builder()
                .subject(subject)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryTime))
                .signWith(key);
        
        if (claims != null && !claims.isEmpty()) {
            builder.claims(claims);
        }
        
        return builder.compact();
    }
    
    /**
     * 生成JWT token（使用默认过期时间）
     * 
     * @param subject 主题（通常是用户ID）
     * @param claims  自定义声明
     * @param key     签名密钥
     * @return JWT token
     */
    public static String generateToken(String subject, Map<String, Object> claims, SecretKey key) {
        return generateToken(subject, claims, DEFAULT_EXPIRATION, key);
    }
    
    /**
     * 生成简单的JWT token
     * 
     * @param subject 主题（通常是用户ID）
     * @param key     签名密钥
     * @return JWT token
     */
    public static String generateToken(String subject, SecretKey key) {
        return generateToken(subject, null, DEFAULT_EXPIRATION, key);
    }
    
    /**
     * 解析JWT token
     * 
     * @param token JWT token
     * @param key   验证密钥
     * @return 解析结果
     */
    public static JwtParseResult parseToken(String token, SecretKey key) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return JwtParseResult.builder()
                    .valid(true)
                    .subject(claims.getSubject())
                    .issuer(claims.getIssuer())
                    .issuedAt(claims.getIssuedAt())
                    .expiration(claims.getExpiration())
                    .claims(claims)
                    .build();
                    
        } catch (ExpiredJwtException e) {
            return JwtParseResult.builder()
                    .valid(false)
                    .expired(true)
                    .errorMessage("Token已过期")
                    .exception(e)
                    .build();
                    
        } catch (UnsupportedJwtException e) {
            return JwtParseResult.builder()
                    .valid(false)
                    .errorMessage("不支持的JWT格式")
                    .exception(e)
                    .build();
                    
        } catch (MalformedJwtException e) {
            return JwtParseResult.builder()
                    .valid(false)
                    .errorMessage("JWT格式错误")
                    .exception(e)
                    .build();
                    
        } catch (SecurityException | SignatureException e) {
            return JwtParseResult.builder()
                    .valid(false)
                    .errorMessage("JWT签名验证失败")
                    .exception(e)
                    .build();
                    
        } catch (IllegalArgumentException e) {
            return JwtParseResult.builder()
                    .valid(false)
                    .errorMessage("JWT参数无效")
                    .exception(e)
                    .build();
                    
        } catch (Exception e) {
            return JwtParseResult.builder()
                    .valid(false)
                    .errorMessage("JWT解析失败: " + e.getMessage())
                    .exception(e)
                    .build();
        }
    }
    
    /**
     * 验证JWT token是否有效
     * 
     * @param token JWT token
     * @param key   验证密钥
     * @return 是否有效
     */
    public static boolean isTokenValid(String token, SecretKey key) {
        JwtParseResult result = parseToken(token, key);
        return result.isValid();
    }
    
    /**
     * 获取JWT token的主题
     * 
     * @param token JWT token
     * @param key   验证密钥
     * @return 主题，如果token无效则返回null
     */
    public static String getSubject(String token, SecretKey key) {
        JwtParseResult result = parseToken(token, key);
        return result.isValid() ? result.getSubject() : null;
    }
    
    /**
     * 获取JWT token的声明
     * 
     * @param token JWT token
     * @param key   验证密钥
     * @param name  声明名称
     * @param type  声明类型
     * @param <T>   声明类型
     * @return 声明值，如果token无效或声明不存在则返回null
     */
    public static <T> T getClaim(String token, SecretKey key, String name, Class<T> type) {
        JwtParseResult result = parseToken(token, key);
        if (!result.isValid() || result.getClaims() == null) {
            return null;
        }
        return result.getClaims().get(name, type);
    }
    
    /**
     * 检查JWT token是否即将过期
     * 
     * @param token           JWT token
     * @param key             验证密钥
     * @param thresholdBefore 过期前多长时间认为即将过期
     * @return 是否即将过期
     */
    public static boolean isTokenExpiringSoon(String token, SecretKey key, Duration thresholdBefore) {
        JwtParseResult result = parseToken(token, key);
        if (!result.isValid() || result.getExpiration() == null) {
            return false;
        }
        
        Instant expiryTime = result.getExpiration().toInstant();
        Instant thresholdTime = Instant.now().plus(thresholdBefore);
        
        return expiryTime.isBefore(thresholdTime);
    }
    
    /**
     * 刷新JWT token（生成新的token，保持原有的声明）
     * 
     * @param token      原JWT token
     * @param key        密钥
     * @param expiration 新的过期时间
     * @return 新的JWT token，如果原token无效则返回null
     */
    public static String refreshToken(String token, SecretKey key, Duration expiration) {
        JwtParseResult result = parseToken(token, key);
        if (!result.isValid()) {
            return null;
        }
        
        // 移除系统声明，保留自定义声明
        Claims claims = result.getClaims();
        claims.remove(Claims.SUBJECT);
        claims.remove(Claims.ISSUER);
        claims.remove(Claims.ISSUED_AT);
        claims.remove(Claims.EXPIRATION);
        
        return generateToken(result.getSubject(), claims, expiration, key, result.getIssuer());
    }
    
    /**
     * JWT解析结果
     */
    @Data
    @Builder
    public static class JwtParseResult {
        /**
         * 是否有效
         */
        private boolean valid;
        
        /**
         * 是否过期
         */
        private boolean expired;
        
        /**
         * 主题
         */
        private String subject;
        
        /**
         * 签发者
         */
        private String issuer;
        
        /**
         * 签发时间
         */
        private Date issuedAt;
        
        /**
         * 过期时间
         */
        private Date expiration;
        
        /**
         * 所有声明
         */
        private Claims claims;
        
        /**
         * 错误消息
         */
        private String errorMessage;
        
        /**
         * 异常
         */
        private Exception exception;
        
        /**
         * 获取自定义声明
         * 
         * @param name 声明名称
         * @param type 声明类型
         * @param <T>  声明类型
         * @return 声明值
         */
        public <T> T getClaim(String name, Class<T> type) {
            return claims != null ? claims.get(name, type) : null;
        }
        
        /**
         * 获取剩余有效时间
         * 
         * @return 剩余有效时间，如果已过期或无过期时间则返回null
         */
        public Duration getRemainingTime() {
            if (expiration == null) {
                return null;
            }
            
            Instant now = Instant.now();
            Instant expiryTime = expiration.toInstant();
            
            if (expiryTime.isBefore(now)) {
                return Duration.ZERO;
            }
            
            return Duration.between(now, expiryTime);
        }
    }
}

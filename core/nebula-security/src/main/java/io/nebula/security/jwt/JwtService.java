package io.nebula.security.jwt;

import java.util.Map;

/**
 * JWT服务接口
 * 
 * 提供JWT令牌的生成、验证和解析功能
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public interface JwtService {
    
    /**
     * 生成访问Token
     *
     * @param subject 主题（通常是用户ID）
     * @return 访问Token
     */
    String generateAccessToken(String subject);
    
    /**
     * 生成访问Token（带额外声明）
     *
     * @param subject 主题（通常是用户ID）
     * @param claims  额外声明
     * @return 访问Token
     */
    String generateAccessToken(String subject, Map<String, Object> claims);
    
    /**
     * 生成刷新Token
     *
     * @param subject 主题（通常是用户ID）
     * @return 刷新Token
     */
    String generateRefreshToken(String subject);
    
    /**
     * 验证访问Token
     *
     * @param token 访问Token
     * @return 主题（通常是用户ID），无效返回null
     */
    String validateAccessToken(String token);
    
    /**
     * 验证刷新Token
     *
     * @param token 刷新Token
     * @return 主题（通常是用户ID），无效返回null
     */
    String validateRefreshToken(String token);
    
    /**
     * 验证Token并获取用户ID（兼容旧接口）
     *
     * @param token 访问Token
     * @return 用户ID，无效返回null
     */
    default Long validateToken(String token) {
        String subject = validateAccessToken(token);
        if (subject == null) {
            return null;
        }
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 从Token中获取声明
     *
     * @param token Token
     * @param key   声明键
     * @param type  值类型
     * @param <T>   类型参数
     * @return 声明值，不存在返回null
     */
    <T> T getClaim(String token, String key, Class<T> type);
    
    /**
     * 获取Token过期时间（毫秒时间戳）
     *
     * @param token Token
     * @return 过期时间戳，解析失败返回null
     */
    Long getExpiration(String token);
    
    /**
     * 检查Token是否即将过期
     *
     * @param token               Token
     * @param thresholdSeconds    阈值秒数
     * @return 是否即将过期
     */
    boolean isTokenExpiringSoon(String token, long thresholdSeconds);
}


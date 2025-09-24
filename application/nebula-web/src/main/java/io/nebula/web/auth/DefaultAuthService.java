package io.nebula.web.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认认证服务实现
 * 基于JWT和内存黑名单的简单实现
 */
public class DefaultAuthService implements AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultAuthService.class);
    
    private final JwtUtils jwtUtils;
    
    /**
     * 令牌黑名单（已注销的令牌）
     */
    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();
    
    public DefaultAuthService(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }
    
    @Override
    public AuthUser getUser(String token) {
        if (isTokenBlacklisted(token)) {
            logger.debug("Token is blacklisted: {}", maskToken(token));
            return null;
        }
        
        try {
            return jwtUtils.parseToken(token);
        } catch (Exception e) {
            logger.debug("Failed to parse user from token: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    public boolean validateToken(String token) {
        if (isTokenBlacklisted(token)) {
            return false;
        }
        
        try {
            return jwtUtils.validateToken(token);
        } catch (Exception e) {
            logger.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public String generateToken(AuthUser user) {
        try {
            return jwtUtils.generateToken(user);
        } catch (Exception e) {
            logger.error("Failed to generate token for user: {}", user.getUserId(), e);
            throw new RuntimeException("Failed to generate token", e);
        }
    }
    
    @Override
    public String refreshToken(String token) {
        // 验证原令牌
        AuthUser user = getUser(token);
        if (user == null) {
            throw new IllegalArgumentException("Invalid token for refresh");
        }
        
        // 将原令牌加入黑名单
        blacklist.add(token);
        
        // 生成新令牌
        String newToken = generateToken(user);
        
        logger.debug("Token refreshed for user: {}", user.getUserId());
        return newToken;
    }
    
    @Override
    public void logout(String token) {
        if (token != null) {
            blacklist.add(token);
            
            // 清理过期的黑名单令牌（可选优化）
            cleanupBlacklist();
            
            logger.debug("Token logged out: {}", maskToken(token));
        }
    }
    
    /**
     * 检查令牌是否在黑名单中
     */
    private boolean isTokenBlacklisted(String token) {
        return token != null && blacklist.contains(token);
    }
    
    /**
     * 清理黑名单中的过期令牌
     */
    private void cleanupBlacklist() {
        // 为了避免内存泄漏，定期清理已过期的令牌
        // 这里采用简单的大小限制策略
        if (blacklist.size() > 10000) {
            // 当黑名单过大时，清理一半
            blacklist.clear();
            logger.info("Blacklist cleared due to size limit");
        }
    }
    
    /**
     * 掩码令牌用于日志输出
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 6) + "***" + token.substring(token.length() - 4);
    }
    
    /**
     * 获取黑名单大小（用于监控）
     */
    public int getBlacklistSize() {
        return blacklist.size();
    }
}

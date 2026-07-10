package io.nebula.web.auth;

import io.nebula.security.jwt.JwtService;
import lombok.extern.slf4j.Slf4j;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认认证服务实现
 * 基于JWT和内存黑名单的简单实现
 */
@Slf4j
public class DefaultAuthService implements AuthService {

    private final TokenAdapter tokenAdapter;

    /**
     * 兼容旧版直接实例化方式。
     *
     * @deprecated 新代码应注入 {@link JwtService}，确保发放和校验使用同一套 token 规则。
     */
    @Deprecated(since = "2.1.0", forRemoval = true)
    public DefaultAuthService(JwtUtils jwtUtils) {
        this.tokenAdapter = new LegacyJwtAdapter(Objects.requireNonNull(jwtUtils, "jwtUtils 不能为空"));
    }

    public DefaultAuthService(JwtService jwtService) {
        this.tokenAdapter = new SecurityJwtAdapter(Objects.requireNonNull(jwtService, "jwtService 不能为空"));
    }
    
    /**
     * 令牌黑名单（已注销的令牌）
     */
    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();
    
    @Override
    public AuthUser getUser(String token) {
        if (isTokenBlacklisted(token)) {
            log.debug("Token is blacklisted: {}", maskToken(token));
            return null;
        }
        
        try {
            return tokenAdapter.getUser(token);
        } catch (Exception e) {
            log.debug("Failed to parse user from token: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    public boolean validateToken(String token) {
        if (isTokenBlacklisted(token)) {
            return false;
        }
        
        try {
            return tokenAdapter.validateToken(token);
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public String generateToken(AuthUser user) {
        try {
            return tokenAdapter.generateToken(user);
        } catch (Exception e) {
            log.error("Failed to generate token for user: {}", user.getUserId(), e);
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
        
        log.debug("Token refreshed for user: {}", user.getUserId());
        return newToken;
    }
    
    @Override
    public void logout(String token) {
        if (token != null) {
            blacklist.add(token);
            
            // 清理过期的黑名单令牌（可选优化）
            cleanupBlacklist();
            
            log.debug("Token logged out: {}", maskToken(token));
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
            log.info("Blacklist cleared due to size limit");
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

    private interface TokenAdapter {

        AuthUser getUser(String token);

        boolean validateToken(String token);

        String generateToken(AuthUser user);
    }

    private record LegacyJwtAdapter(JwtUtils jwtUtils) implements TokenAdapter {

        @Override
        public AuthUser getUser(String token) {
            return jwtUtils.parseToken(token);
        }

        @Override
        public boolean validateToken(String token) {
            return jwtUtils.validateToken(token);
        }

        @Override
        public String generateToken(AuthUser user) {
            return jwtUtils.generateToken(user);
        }
    }

    private record SecurityJwtAdapter(JwtService jwtService) implements TokenAdapter {

        @Override
        public AuthUser getUser(String token) {
            String userId = jwtService.validateAccessToken(token);
            if (userId == null) {
                return null;
            }

            AuthUser user = new AuthUser(userId, getClaim(token, "username", String.class));
            user.setRoles(toStringSet(getClaim(token, "roles", Object.class)));
            user.setPermissions(toStringSet(getClaim(token, "permissions", Object.class)));
            user.setExtra(getClaim(token, "extra", Object.class));
            return user;
        }

        @Override
        public boolean validateToken(String token) {
            return jwtService.validateAccessToken(token) != null;
        }

        @Override
        public String generateToken(AuthUser user) {
            Map<String, Object> claims = new LinkedHashMap<>();
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
            return jwtService.generateAccessToken(user.getUserId(), claims);
        }

        private <T> T getClaim(String token, String key, Class<T> type) {
            try {
                return jwtService.getClaim(token, key, type);
            } catch (Exception e) {
                log.debug("读取 JWT claim 失败: key={}, error={}", key, e.getMessage());
                return null;
            }
        }
    }

    private static Set<String> toStringSet(Object value) {
        if (value == null) {
            return Set.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (value instanceof Collection<?> collection) {
            collection.stream().filter(java.util.Objects::nonNull)
                    .map(Object::toString).forEach(result::add);
        } else {
            for (String item : value.toString().split(",")) {
                if (!item.isBlank()) {
                    result.add(item.trim());
                }
            }
        }
        return result;
    }
}

package io.nebula.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 安全配置属性
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@ConfigurationProperties(prefix = "nebula.security")
public class SecurityProperties {
    
    /**
     * 是否启用安全功能
     */
    private boolean enabled = true;
    
    /**
     * JWT配置
     */
    private Jwt jwt = new Jwt();
    
    /**
     * RBAC配置
     */
    private Rbac rbac = new Rbac();
    
    /**
     * 匿名访问URL列表
     */
    private List<String> anonymousUrls = new ArrayList<>();
    
    /**
     * JWT配置
     */
    @Data
    public static class Jwt {
        /**
         * 是否启用JWT
         */
        private boolean enabled = true;
        
        /**
         * JWT密钥
         */
        private String secret = "nebula-default-secret-key-please-change-in-production";
        
        /**
         * Token过期时间
         */
        private Duration expiration = Duration.ofHours(24);
        
        /**
         * Refresh Token过期时间
         */
        private Duration refreshExpiration = Duration.ofDays(7);
        
        /**
         * Token header名称
         */
        private String headerName = "Authorization";
        
        /**
         * Token前缀
         */
        private String tokenPrefix = "Bearer ";
    }
    
    /**
     * RBAC配置
     */
    @Data
    public static class Rbac {
        /**
         * 是否启用RBAC
         */
        private boolean enabled = true;
        
        /**
         * 是否启用权限缓存
         */
        private boolean enableCache = true;
        
        /**
         * 权限缓存过期时间
         */
        private Duration cacheExpiration = Duration.ofMinutes(30);
        
        /**
         * 超级管理员角色
         */
        private String superAdminRole = "SUPER_ADMIN";
    }
}


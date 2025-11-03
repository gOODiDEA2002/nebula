package io.nebula.security.authentication;

/**
 * 授予的权限接口
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public interface GrantedAuthority {
    
    /**
     * 获取权限字符串
     * 
     * @return 权限标识
     */
    String getAuthority();
}


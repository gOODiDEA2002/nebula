package io.nebula.security.user;

import io.nebula.security.authentication.GrantedAuthority;

import java.util.Collection;
import java.util.Set;

/**
 * 用户详情接口
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public interface UserDetails {
    
    /**
     * 获取用户ID
     */
    Long getUserId();
    
    /**
     * 获取用户名
     */
    String getUsername();
    
    /**
     * 获取密码
     */
    String getPassword();
    
    /**
     * 获取角色列表
     */
    Set<String> getRoles();
    
    /**
     * 获取权限列表
     */
    Collection<? extends GrantedAuthority> getAuthorities();
    
    /**
     * 账户是否未过期
     */
    boolean isAccountNonExpired();
    
    /**
     * 账户是否未锁定
     */
    boolean isAccountNonLocked();
    
    /**
     * 凭证是否未过期
     */
    boolean isCredentialsNonExpired();
    
    /**
     * 账户是否启用
     */
    boolean isEnabled();
}


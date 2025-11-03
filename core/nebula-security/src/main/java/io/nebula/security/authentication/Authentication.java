package io.nebula.security.authentication;

import java.util.Collection;
import java.util.Map;

/**
 * 认证信息接口
 * 
 * 表示已认证的用户信息
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public interface Authentication {
    
    /**
     * 获取主体(通常是用户名或用户ID)
     * 
     * @return 主体信息
     */
    Object getPrincipal();
    
    /**
     * 获取凭证(通常是密码或Token)
     * 
     * @return 凭证信息
     */
    Object getCredentials();
    
    /**
     * 获取权限列表
     * 
     * @return 权限集合
     */
    Collection<? extends GrantedAuthority> getAuthorities();
    
    /**
     * 获取详细信息
     * 
     * @return 详细信息(如IP、设备等)
     */
    Object getDetails();
    
    /**
     * 是否已认证
     * 
     * @return 是否已认证
     */
    boolean isAuthenticated();
    
    /**
     * 设置认证状态
     * 
     * @param authenticated 是否已认证
     */
    void setAuthenticated(boolean authenticated);
    
    /**
     * 获取额外属性
     * 
     * @return 属性映射
     */
    Map<String, Object> getAttributes();
    
    /**
     * 设置属性
     * 
     * @param key 属性键
     * @param value 属性值
     */
    void setAttribute(String key, Object value);
    
    /**
     * 获取属性
     * 
     * @param key 属性键
     * @return 属性值
     */
    Object getAttribute(String key);
}


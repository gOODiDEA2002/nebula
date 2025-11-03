package io.nebula.security.authentication;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 简单权限实现
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@AllArgsConstructor
public class SimpleGrantedAuthority implements GrantedAuthority {
    
    private String authority;
    
    @Override
    public String getAuthority() {
        return authority;
    }
}


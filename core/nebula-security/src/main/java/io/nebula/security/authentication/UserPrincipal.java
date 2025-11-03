package io.nebula.security.authentication;

import io.nebula.security.user.UserDetails;
import lombok.Data;

import java.util.Collection;
import java.util.Set;

/**
 * 用户主体
 * 
 * 实现UserDetails和Principal
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
public class UserPrincipal implements UserDetails {
    
    private Long userId;
    private String username;
    private String password;
    private Set<String> roles;
    private Collection<? extends GrantedAuthority> authorities;
    private boolean accountNonExpired = true;
    private boolean accountNonLocked = true;
    private boolean credentialsNonExpired = true;
    private boolean enabled = true;
    
    public UserPrincipal(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }
    
    public UserPrincipal(Long userId, String username, Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.authorities = authorities;
    }
}


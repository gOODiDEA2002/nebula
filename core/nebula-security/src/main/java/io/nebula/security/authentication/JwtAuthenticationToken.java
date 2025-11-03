package io.nebula.security.authentication;

import lombok.Data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT认证Token
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
public class JwtAuthenticationToken implements Authentication {
    
    private String token;
    private Object principal;
    private Object credentials;
    private Collection<? extends GrantedAuthority> authorities;
    private Object details;
    private boolean authenticated;
    private Map<String, Object> attributes = new HashMap<>();
    
    public JwtAuthenticationToken(String token) {
        this.token = token;
        this.credentials = token;
        this.authenticated = false;
    }
    
    public JwtAuthenticationToken(
            String token,
            Object principal,
            Collection<? extends GrantedAuthority> authorities) {
        this.token = token;
        this.principal = principal;
        this.credentials = token;
        this.authorities = authorities;
        this.authenticated = true;
    }
    
    @Override
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    @Override
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
}


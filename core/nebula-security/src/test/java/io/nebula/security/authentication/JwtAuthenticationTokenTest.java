package io.nebula.security.authentication;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * JwtAuthenticationToken单元测试
 */
class JwtAuthenticationTokenTest {
    
    @Test
    void testTokenCreationUnauth() {
        String token = "test.jwt.token";
        
        JwtAuthenticationToken authToken = new JwtAuthenticationToken(token);
        
        assertThat(authToken.getToken()).isEqualTo(token);
        assertThat(authToken.getCredentials()).isEqualTo(token);
        assertThat(authToken.isAuthenticated()).isFalse();
        assertThat(authToken.getPrincipal()).isNull();
    }
    
    @Test
    void testTokenCreationAuthenticated() {
        String token = "test.jwt.token";
        UserPrincipal principal = new UserPrincipal(1L, "testuser");
        List<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("user:read")
        );
        
        JwtAuthenticationToken authToken = new JwtAuthenticationToken(token, principal, authorities);
        
        assertThat(authToken.getToken()).isEqualTo(token);
        assertThat(authToken.getPrincipal()).isEqualTo(principal);
        assertThat(authToken.getAuthorities()).hasSize(2);
        assertThat(authToken.isAuthenticated()).isTrue();
        assertThat(authToken.getCredentials()).isEqualTo(token);
    }
    
    @Test
    void testGetPrincipal() {
        UserPrincipal principal = new UserPrincipal(1L, "testuser");
        JwtAuthenticationToken authToken = new JwtAuthenticationToken("token", principal, Collections.emptyList());
        
        Object result = authToken.getPrincipal();
        
        assertThat(result).isInstanceOf(UserPrincipal.class);
        assertThat(((UserPrincipal) result).getUserId()).isEqualTo(1L);
        assertThat(((UserPrincipal) result).getUsername()).isEqualTo("testuser");
    }
    
    @Test
    void testGetAuthorities() {
        List<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("admin:write")
        );
        
        JwtAuthenticationToken authToken = new JwtAuthenticationToken("token", null, authorities);
        
        Collection<? extends GrantedAuthority> result = authToken.getAuthorities();
        
        assertThat(result).hasSize(2);
        assertThat(result).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN", "admin:write");
    }
    
    @Test
    void testIsAuthenticated() {
        JwtAuthenticationToken unauthToken = new JwtAuthenticationToken("token");
        assertThat(unauthToken.isAuthenticated()).isFalse();
        
        JwtAuthenticationToken authToken = new JwtAuthenticationToken("token", new UserPrincipal(1L, "user"), Collections.emptyList());
        assertThat(authToken.isAuthenticated()).isTrue();
    }
    
    @Test
    void testGetToken() {
        String token = "my.jwt.token";
        JwtAuthenticationToken authToken = new JwtAuthenticationToken(token);
        
        assertThat(authToken.getToken()).isEqualTo(token);
    }
    
    @Test
    void testSetAndGetAttribute() {
        JwtAuthenticationToken authToken = new JwtAuthenticationToken("token");
        
        authToken.setAttribute("key1", "value1");
        authToken.setAttribute("key2", 123);
        
        assertThat(authToken.getAttribute("key1")).isEqualTo("value1");
        assertThat(authToken.getAttribute("key2")).isEqualTo(123);
        assertThat(authToken.getAttribute("nonexistent")).isNull();
    }
    
    @Test
    void testSetDetails() {
        JwtAuthenticationToken authToken = new JwtAuthenticationToken("token");
        Map<String, Object> details = Map.of("ip", "192.168.1.1", "device", "mobile");
        
        authToken.setDetails(details);
        
        assertThat(authToken.getDetails()).isEqualTo(details);
    }
}


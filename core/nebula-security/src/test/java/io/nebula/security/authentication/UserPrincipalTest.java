package io.nebula.security.authentication;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * UserPrincipal单元测试
 */
class UserPrincipalTest {
    
    @Test
    void testPrincipalCreation() {
        Long userId = 123L;
        String username = "testuser";
        
        UserPrincipal principal = new UserPrincipal(userId, username);
        
        assertThat(principal.getUserId()).isEqualTo(userId);
        assertThat(principal.getUsername()).isEqualTo(username);
    }
    
    @Test
    void testPrincipalCreationWithAuthorities() {
        Long userId = 456L;
        String username = "admin";
        Collection<? extends GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("admin:write")
        );
        
        UserPrincipal principal = new UserPrincipal(userId, username, authorities);
        
        assertThat(principal.getUserId()).isEqualTo(userId);
        assertThat(principal.getUsername()).isEqualTo(username);
        assertThat(principal.getAuthorities()).hasSize(2);
        assertThat(principal.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN", "admin:write");
    }
    
    @Test
    void testGetUserId() {
        UserPrincipal principal = new UserPrincipal(789L, "user");
        
        assertThat(principal.getUserId()).isEqualTo(789L);
    }
    
    @Test
    void testGetUsername() {
        UserPrincipal principal = new UserPrincipal(1L, "myusername");
        
        assertThat(principal.getUsername()).isEqualTo("myusername");
    }
    
    @Test
    void testGetAuthorities() {
        Collection<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("user:read"),
                new SimpleGrantedAuthority("user:write")
        );
        
        UserPrincipal principal = new UserPrincipal(1L, "user", authorities);
        
        assertThat(principal.getAuthorities()).isNotNull();
        assertThat(principal.getAuthorities()).hasSize(3);
    }
    
    @Test
    void testSetPassword() {
        UserPrincipal principal = new UserPrincipal(1L, "user");
        
        principal.setPassword("encrypted_password");
        
        assertThat(principal.getPassword()).isEqualTo("encrypted_password");
    }
    
    @Test
    void testSetRoles() {
        UserPrincipal principal = new UserPrincipal(1L, "user");
        Set<String> roles = Set.of("ADMIN", "USER");
        
        principal.setRoles(roles);
        
        assertThat(principal.getRoles()).containsExactlyInAnyOrder("ADMIN", "USER");
    }
    
    @Test
    void testAccountStatus() {
        UserPrincipal principal = new UserPrincipal(1L, "user");
        
        // 默认状态
        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isAccountNonLocked()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
        assertThat(principal.isEnabled()).isTrue();
    }
    
    @Test
    void testSetAccountStatus() {
        UserPrincipal principal = new UserPrincipal(1L, "user");
        
        principal.setAccountNonExpired(false);
        principal.setAccountNonLocked(false);
        principal.setCredentialsNonExpired(false);
        principal.setEnabled(false);
        
        assertThat(principal.isAccountNonExpired()).isFalse();
        assertThat(principal.isAccountNonLocked()).isFalse();
        assertThat(principal.isCredentialsNonExpired()).isFalse();
        assertThat(principal.isEnabled()).isFalse();
    }
}


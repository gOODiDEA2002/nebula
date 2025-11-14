package io.nebula.web.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DefaultAuthService单元测试
 */
@ExtendWith(MockitoExtension.class)
class DefaultAuthServiceTest {
    
    @Mock
    private JwtUtils jwtUtils;
    
    private DefaultAuthService authService;
    
    @BeforeEach
    void setUp() {
        authService = new DefaultAuthService(jwtUtils);
    }
    
    @Test
    void testGenerateToken() {
        AuthUser user = new AuthUser("user-123", "testuser");
        Set<String> roles = new HashSet<>(Arrays.asList("USER"));
        user.setRoles(roles);
        
        String expectedToken = "generated.jwt.token";
        when(jwtUtils.generateToken(user)).thenReturn(expectedToken);
        
        String token = authService.generateToken(user);
        
        assertThat(token).isEqualTo(expectedToken);
        verify(jwtUtils).generateToken(user);
    }
    
    @Test
    void testValidateToken() {
        String token = "valid.jwt.token";
        when(jwtUtils.validateToken(token)).thenReturn(true);
        
        boolean isValid = authService.validateToken(token);
        
        assertThat(isValid).isTrue();
        verify(jwtUtils).validateToken(token);
    }
    
    @Test
    void testValidateInvalidToken() {
        String token = "invalid.token";
        when(jwtUtils.validateToken(token)).thenReturn(false);
        
        boolean isValid = authService.validateToken(token);
        
        assertThat(isValid).isFalse();
    }
    
    @Test
    void testGetUser() {
        String token = "valid.jwt.token";
        AuthUser expectedUser = new AuthUser("user-123", "testuser");
        
        when(jwtUtils.parseToken(token)).thenReturn(expectedUser);
        
        AuthUser user = authService.getUser(token);
        
        assertThat(user).isNotNull();
        assertThat(user.getUserId()).isEqualTo("user-123");
        assertThat(user.getUsername()).isEqualTo("testuser");
    }
    
    @Test
    void testGetUserWithInvalidToken() {
        String token = "invalid.token";
        when(jwtUtils.parseToken(token)).thenThrow(new RuntimeException("Invalid token"));
        
        AuthUser user = authService.getUser(token);
        
        assertThat(user).isNull();
    }
    
    @Test
    void testLogout() {
        String token = "user.token";
        
        authService.logout(token);
        
        // 验证token被加入黑名单
        assertThat(authService.validateToken(token)).isFalse();
    }
    
    @Test
    void testBlacklistedToken() {
        String token = "blacklisted.token";
        
        // 首先logout使token进入黑名单
        authService.logout(token);
        
        // 验证黑名单中的token被拒绝（不会调用jwtUtils，直接返回false/null）
        assertThat(authService.validateToken(token)).isFalse();
        assertThat(authService.getUser(token)).isNull();
    }
}


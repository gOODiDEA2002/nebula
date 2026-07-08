package io.nebula.web.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.nebula.security.authentication.Authentication;
import io.nebula.security.authentication.GrantedAuthority;
import io.nebula.security.authentication.JwtAuthenticationToken;
import io.nebula.security.authentication.SecurityContext;
import io.nebula.security.authentication.SimpleGrantedAuthority;
import io.nebula.security.authentication.UserPrincipal;
import io.nebula.web.autoconfigure.WebProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 认证拦截器测试（收敛后：AuthInterceptor 读取 SecurityContext，不再自行解析 JWT）
 */
@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    private AuthInterceptor authInterceptor;
    private WebProperties.Auth authConfig;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        authConfig = new WebProperties.Auth();
        authConfig.setEnabled(true);
        authConfig.setAuthHeader("Authorization");
        authConfig.setAuthHeaderPrefix("Bearer ");
        authConfig.setIgnorePaths(new String[]{"/public/**", "/health", "/login"});
        
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        authInterceptor = new AuthInterceptor(authConfig, objectMapper);
    }
    
    @AfterEach
    void tearDown() {
        SecurityContext.clearAuthentication();
    }
    
    @Test
    void testAuthSuccess() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users");
        
        // 模拟 Filter 层已填充 SecurityContext
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("user:read"));
        UserPrincipal principal = new UserPrincipal(123L, "testuser", authorities);
        principal.setRoles(Set.of("USER"));
        Authentication authentication = new JwtAuthenticationToken("token", principal, authorities);
        SecurityContext.setAuthentication(authentication);
        
        boolean result = authInterceptor.preHandle(request, response, null);
        
        assertThat(result).isTrue();
        verify(request).setAttribute(eq("currentUser"), any());
        verify(request).setAttribute("currentUserId", "123");
        verify(request).setAttribute("currentUsername", "testuser");
    }
    
    @Test
    void testNoAuthentication_returns401() throws Exception {
        StringWriter responseWriter = new StringWriter();
        when(request.getRequestURI()).thenReturn("/api/users");
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        
        // SecurityContext 无认证信息
        boolean result = authInterceptor.preHandle(request, response, null);
        
        assertThat(result).isFalse();
        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(response).setHeader("WWW-Authenticate", "Bearer");
    }
    
    @Test
    void testIgnorePath() throws Exception {
        when(request.getRequestURI()).thenReturn("/public/info");
        
        boolean result = authInterceptor.preHandle(request, response, null);
        
        assertThat(result).isTrue();
    }
    
    @Test
    void testAuthDisabled() throws Exception {
        authConfig.setEnabled(false);
        authInterceptor = new AuthInterceptor(authConfig, objectMapper);
        
        lenient().when(request.getRequestURI()).thenReturn("/api/users");
        
        boolean result = authInterceptor.preHandle(request, response, null);
        
        assertThat(result).isTrue();
    }

    @Test
    void testCorsPreflightAllowed() throws Exception {
        when(request.getMethod()).thenReturn("OPTIONS");
        when(request.getHeader("Origin")).thenReturn("http://example.com");
        when(request.getHeader("Access-Control-Request-Method")).thenReturn("POST");

        boolean result = authInterceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
    }

    @Test
    void testPlainOptionsNotBypassed() throws Exception {
        StringWriter responseWriter = new StringWriter();
        when(request.getMethod()).thenReturn("OPTIONS");
        when(request.getRequestURI()).thenReturn("/api/users");
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        boolean result = authInterceptor.preHandle(request, response, null);

        assertThat(result).isFalse();
        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
    }
    
    @Test
    void testAuthContext_populated_from_securityContext() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/data");
        
        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("data:read"),
                new SimpleGrantedAuthority("data:write"));
        UserPrincipal principal = new UserPrincipal(456L, "datauser", authorities);
        principal.setRoles(Set.of("ADMIN", "USER"));
        Authentication authentication = new JwtAuthenticationToken("token", principal, authorities);
        SecurityContext.setAuthentication(authentication);
        
        boolean result = authInterceptor.preHandle(request, response, null);
        
        assertThat(result).isTrue();
        
        // 验证 AuthContext 已正确填充
        var authUser = io.nebula.web.auth.AuthContext.getCurrentUser();
        assertThat(authUser).isNotNull();
        assertThat(authUser.getUserId()).isEqualTo("456");
        assertThat(authUser.getUsername()).isEqualTo("datauser");
        assertThat(authUser.getRoles()).containsExactlyInAnyOrder("ADMIN", "USER");
        assertThat(authUser.getPermissions()).containsExactlyInAnyOrder("data:read", "data:write");
    }
    
    @Test
    void testAfterCompletion_clearsAuthContext() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/data");
        
        UserPrincipal principal = new UserPrincipal(1L, "user");
        Authentication authentication = new JwtAuthenticationToken("t", principal, List.of());
        SecurityContext.setAuthentication(authentication);
        
        authInterceptor.preHandle(request, response, null);
        assertThat(io.nebula.web.auth.AuthContext.getCurrentUser()).isNotNull();
        
        authInterceptor.afterCompletion(request, response, null, null);
        assertThat(io.nebula.web.auth.AuthContext.getCurrentUser()).isNull();
    }
}

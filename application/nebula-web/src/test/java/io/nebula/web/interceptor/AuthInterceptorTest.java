package io.nebula.web.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.nebula.web.auth.AuthService;
import io.nebula.web.auth.AuthUser;
import io.nebula.web.autoconfigure.WebProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 认证拦截器测试
 */
@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {
    
    @Mock
    private AuthService authService;
    
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
        
        authInterceptor = new AuthInterceptor(authService, authConfig, objectMapper);
    }
    
    @Test
    void testAuthSuccess() throws Exception {
        // 准备测试数据
        String token = "valid-token";
        AuthUser user = new AuthUser("user-123", "testuser");
        user.setRoles(Set.of("USER"));
        
        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(authService.getUser(token)).thenReturn(user);
        
        // 执行认证
        boolean result = authInterceptor.preHandle(request, response, null);
        
        // 验证认证成功
        assertThat(result).isTrue();
        verify(request).setAttribute("currentUser", user);
        verify(request).setAttribute("currentUserId", "user-123");
        verify(request).setAttribute("currentUsername", "testuser");
    }
    
    @Test
    void testAuthFailure() throws Exception {
        // 准备测试数据
        String token = "invalid-token";
        StringWriter responseWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(responseWriter);
        
        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(authService.getUser(token)).thenReturn(null);
        when(response.getWriter()).thenReturn(printWriter);
        
        // 执行认证
        boolean result = authInterceptor.preHandle(request, response, null);
        
        // 验证认证失败
        assertThat(result).isFalse();
        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(response).setHeader("WWW-Authenticate", "Bearer");
    }
    
    @Test
    void testIgnorePath() throws Exception {
        // 测试忽略路径
        when(request.getRequestURI()).thenReturn("/public/info");
        
        boolean result = authInterceptor.preHandle(request, response, null);
        
        // 验证忽略路径不需要认证
        assertThat(result).isTrue();
        verify(authService, never()).getUser(anyString());
    }
    
    @Test
    void testMissingToken() throws Exception {
        // 准备测试数据
        StringWriter responseWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(responseWriter);
        
        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getParameter("token")).thenReturn(null);
        when(response.getWriter()).thenReturn(printWriter);
        
        // 执行认证
        boolean result = authInterceptor.preHandle(request, response, null);
        
        // 验证缺少Token返回401
        assertThat(result).isFalse();
        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(authService, never()).getUser(anyString());
    }
    
    @Test
    void testTokenFromQueryParameter() throws Exception {
        // 测试从查询参数获取Token
        String token = "query-token";
        AuthUser user = new AuthUser("user-456", "queryuser");
        
        when(request.getRequestURI()).thenReturn("/api/data");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getParameter("token")).thenReturn(token);
        when(authService.getUser(token)).thenReturn(user);
        
        boolean result = authInterceptor.preHandle(request, response, null);
        
        assertThat(result).isTrue();
        verify(authService).getUser(token);
    }
    
    @Test
    void testAuthDisabled() throws Exception {
        // 禁用认证
        authConfig.setEnabled(false);
        authInterceptor = new AuthInterceptor(authService, authConfig, objectMapper);
        
        lenient().when(request.getRequestURI()).thenReturn("/api/users");
        
        boolean result = authInterceptor.preHandle(request, response, null);
        
        // 验证禁用认证后所有请求都通过
        assertThat(result).isTrue();
        verify(authService, never()).getUser(anyString());
    }
}


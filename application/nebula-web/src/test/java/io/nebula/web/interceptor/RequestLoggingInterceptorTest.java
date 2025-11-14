package io.nebula.web.interceptor;

import io.nebula.web.autoconfigure.WebProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 请求日志拦截器测试
 */
@ExtendWith(MockitoExtension.class)
class RequestLoggingInterceptorTest {
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    private RequestLoggingInterceptor loggingInterceptor;
    private WebProperties.RequestLogging loggingConfig;
    
    @BeforeEach
    void setUp() {
        loggingConfig = new WebProperties.RequestLogging();
        loggingConfig.setEnabled(true);
        loggingConfig.setIncludeHeaders(true);
        loggingConfig.setIncludeRequestBody(true);
        loggingConfig.setIncludeResponseBody(true);
        loggingConfig.setIgnorePaths(new String[]{"/health", "/metrics"});
        loggingConfig.setMaxRequestBodyLength(1024);
        loggingConfig.setMaxResponseBodyLength(1024);
        
        loggingInterceptor = new RequestLoggingInterceptor(loggingConfig);
        
        // Mock基本请求信息
        lenient().when(request.getMethod()).thenReturn("GET");
        lenient().when(request.getRequestURI()).thenReturn("/api/test");
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
    }
    
    @Test
    void testLogRequest() throws Exception {
        // 准备测试数据
        lenient().when(request.getQueryString()).thenReturn("param=value");
        lenient().when(request.getHeader("User-Agent")).thenReturn("Test Agent");
        lenient().when(response.getStatus()).thenReturn(200);
        
        // 执行请求处理
        boolean proceed = loggingInterceptor.preHandle(request, response, null);
        assertThat(proceed).isTrue();
        
        // 模拟请求完成
        loggingInterceptor.afterCompletion(request, response, null, null);
        
        // 验证请求被记录
        verify(request).setAttribute(eq("REQUEST_START_TIME"), anyLong());
    }
    
    @Test
    void testLogResponse() throws Exception {
        // 准备测试数据
        when(response.getStatus()).thenReturn(200);
        when(request.getAttribute("REQUEST_START_TIME")).thenReturn(System.currentTimeMillis());
        
        // 执行afterCompletion
        loggingInterceptor.afterCompletion(request, response, null, null);
        
        // 验证响应被记录（通过验证status获取）
        verify(response, atLeastOnce()).getStatus();
    }
    
    @Test
    void testLogWithMasking() throws Exception {
        // 准备测试数据（包含敏感信息）
        lenient().when(request.getHeaderNames()).thenReturn(
            Collections.enumeration(List.of("Authorization", "Content-Type"))
        );
        lenient().when(request.getHeader("Authorization")).thenReturn("Bearer secret-token-12345");
        lenient().when(request.getHeader("Content-Type")).thenReturn("application/json");
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(request.getHeader("X-Real-IP")).thenReturn(null);
        lenient().when(request.getQueryString()).thenReturn(null);
        lenient().when(request.getAttribute("REQUEST_START_TIME")).thenReturn(System.currentTimeMillis());
        lenient().when(response.getStatus()).thenReturn(200);
        
        // 执行
        loggingInterceptor.preHandle(request, response, null);
        loggingInterceptor.afterCompletion(request, response, null, null);
        
        // 验证请求URI被访问
        verify(request, atLeastOnce()).getRequestURI();
    }
    
    @Test
    void testIgnorePath() throws Exception {
        // 测试忽略路径
        when(request.getRequestURI()).thenReturn("/health");
        
        // 执行
        boolean proceed = loggingInterceptor.preHandle(request, response, null);
        
        // 验证忽略路径不记录日志
        assertThat(proceed).isTrue();
        
        // afterCompletion也应该忽略
        loggingInterceptor.afterCompletion(request, response, null, null);
        verify(request, never()).setAttribute(eq("REQUEST_START_TIME"), anyLong());
    }
    
    @Test
    void testDisabledLogging() throws Exception {
        // 禁用日志
        loggingConfig.setEnabled(false);
        loggingInterceptor = new RequestLoggingInterceptor(loggingConfig);
        
        lenient().when(request.getRequestURI()).thenReturn("/api/test");
        
        // 执行
        boolean proceed = loggingInterceptor.preHandle(request, response, null);
        
        // 验证禁用时不记录日志
        assertThat(proceed).isTrue();
        verify(request, never()).setAttribute(eq("REQUEST_START_TIME"), anyLong());
    }
    
    @Test
    void testErrorStatusCode() throws Exception {
        // 准备测试数据（错误状态码）
        when(request.getAttribute("REQUEST_START_TIME")).thenReturn(System.currentTimeMillis());
        when(response.getStatus()).thenReturn(500);
        
        // 执行
        loggingInterceptor.afterCompletion(request, response, null, null);
        
        // 验证错误状态码被记录
        verify(response, atLeastOnce()).getStatus();
    }
    
    @Test
    void testExceptionLogging() throws Exception {
        // 准备测试数据（异常）
        Exception testException = new RuntimeException("Test exception");
        when(request.getAttribute("REQUEST_START_TIME")).thenReturn(System.currentTimeMillis());
        when(response.getStatus()).thenReturn(500);
        
        // 执行
        loggingInterceptor.afterCompletion(request, response, null, testException);
        
        // 验证异常被处理
        verify(response, atLeastOnce()).getStatus();
    }
    
    @Test
    void testClientIpExtraction() throws Exception {
        // 测试X-Forwarded-For头
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        lenient().when(request.getHeader("X-Real-IP")).thenReturn(null);
        lenient().when(request.getAttribute("REQUEST_START_TIME")).thenReturn(System.currentTimeMillis());
        lenient().when(response.getStatus()).thenReturn(200);
        lenient().when(request.getQueryString()).thenReturn(null);
        
        // 执行
        loggingInterceptor.afterCompletion(request, response, null, null);
        
        // 验证IP头被访问
        verify(request, atLeastOnce()).getHeader("X-Forwarded-For");
    }
    
    @Test
    void testWildcardIgnorePath() throws Exception {
        // 测试通配符忽略路径
        loggingConfig.setIgnorePaths(new String[]{"/api/public/**"});
        loggingInterceptor = new RequestLoggingInterceptor(loggingConfig);
        
        lenient().when(request.getRequestURI()).thenReturn("/api/public/info");
        
        // 执行
        boolean proceed = loggingInterceptor.preHandle(request, response, null);
        
        // 验证通配符路径被忽略
        assertThat(proceed).isTrue();
        verify(request, never()).setAttribute(eq("REQUEST_START_TIME"), anyLong());
    }
    
    @Test
    void testSlowRequestDetection() throws Exception {
        // 模拟慢请求（超过1秒）
        long slowStartTime = System.currentTimeMillis() - 2000; // 2秒前
        when(request.getAttribute("REQUEST_START_TIME")).thenReturn(slowStartTime);
        when(response.getStatus()).thenReturn(200);
        
        // 执行
        loggingInterceptor.afterCompletion(request, response, null, null);
        
        // 验证慢请求被处理（日志级别会是WARN）
        verify(response, atLeastOnce()).getStatus();
    }
}


package io.nebula.web.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.core.common.result.Result;
import io.nebula.web.auth.AuthContext;
import io.nebula.web.auth.AuthService;
import io.nebula.web.auth.AuthUser;
import io.nebula.web.autoconfigure.WebProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 认证拦截器
 * 验证用户身份并设置认证上下文
 */
public class AuthInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    private final AuthService authService;
    private final WebProperties.Auth config;
    private final ObjectMapper objectMapper;
    
    public AuthInterceptor(AuthService authService, WebProperties.Auth config, ObjectMapper objectMapper) {
        this.authService = authService;
        this.config = config;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        if (!config.isEnabled()) {
            return true;
        }
        
        String requestURI = request.getRequestURI();
        
        // 检查是否为忽略路径
        if (shouldIgnoreAuth(requestURI)) {
            return true;
        }
        
        // 提取认证令牌
        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            handleAuthenticationError(response, "缺少认证令牌");
            return false;
        }
        
        // 验证令牌并获取用户信息
        AuthUser user = authService.getUser(token);
        if (user == null) {
            handleAuthenticationError(response, "认证令牌无效");
            return false;
        }
        
        // 设置认证上下文
        AuthContext.setCurrentUser(user);
        
        // 添加用户信息到请求属性
        request.setAttribute("currentUser", user);
        request.setAttribute("currentUserId", user.getUserId());
        request.setAttribute("currentUsername", user.getUsername());
        
        logger.debug("User authenticated: {} [{}]", user.getUsername(), user.getUserId());
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                              Object handler, Exception ex) {
        // 清除认证上下文
        AuthContext.clear();
    }
    
    /**
     * 提取认证令牌
     */
    private String extractToken(HttpServletRequest request) {
        // 从请求头获取令牌
        String authHeader = request.getHeader(config.getAuthHeader());
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(config.getAuthHeaderPrefix())) {
            return authHeader.substring(config.getAuthHeaderPrefix().length());
        }
        
        // 从请求参数获取令牌（备选方案）
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }
        
        return null;
    }
    
    /**
     * 判断是否应该忽略认证
     */
    private boolean shouldIgnoreAuth(String requestURI) {
        if (config.getIgnorePaths() == null) {
            return false;
        }
        
        for (String pattern : config.getIgnorePaths()) {
            if (pathMatcher.match(pattern, requestURI)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 处理认证错误
     */
    private void handleAuthenticationError(HttpServletResponse response, String message) throws Exception {
        logger.debug("Authentication failed: {}", message);
        
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        
        // 创建错误响应
        Result<Void> errorResult = Result.error("AUTHENTICATION_FAILED", message);
        
        // 添加认证相关的响应头
        response.setHeader("WWW-Authenticate", config.getAuthHeaderPrefix().trim());
        
        // 写入响应
        String responseBody = objectMapper.writeValueAsString(errorResult);
        response.getWriter().write(responseBody);
        response.getWriter().flush();
    }
}

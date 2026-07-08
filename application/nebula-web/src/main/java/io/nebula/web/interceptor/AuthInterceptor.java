package io.nebula.web.interceptor;

import tools.jackson.databind.ObjectMapper;
import io.nebula.core.common.result.Result;
import io.nebula.security.authentication.Authentication;
import io.nebula.security.authentication.SecurityContext;
import io.nebula.security.authentication.UserPrincipal;
import io.nebula.web.auth.AuthContext;
import io.nebula.web.auth.AuthUser;
import io.nebula.web.autoconfigure.WebProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 认证拦截器
 * <p>
 * 从 {@link SecurityContext} 读取认证信息（由 JwtAuthenticationFilter 在 Filter 层填充），
 * 桥接填充 {@link AuthContext} 以保持 web 层 API 兼容。
 * 自身不再解析 JWT，收敛为单一解析点。
 */
public class AuthInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    private final WebProperties.Auth config;
    private final ObjectMapper objectMapper;
    
    public AuthInterceptor(WebProperties.Auth config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        if (!config.isEnabled()) {
            return true;
        }
        
        // 仅放行真正的 CORS 预检请求(OPTIONS + Origin + Access-Control-Request-Method 三者齐全)
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        
        String requestURI = request.getRequestURI();
        
        if (shouldIgnoreAuth(requestURI)) {
            return true;
        }
        
        // 从 SecurityContext 读取认证信息（由 JwtAuthenticationFilter 在 Filter 层预填充）
        Authentication authentication = SecurityContext.getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            handleAuthenticationError(response, "缺少认证令牌");
            return false;
        }
        
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            handleAuthenticationError(response, "认证令牌无效");
            return false;
        }
        
        // 从 security 层的 UserPrincipal 构建 web 层的 AuthUser，保持 API 兼容
        AuthUser user = buildAuthUser(userPrincipal);
        
        // 设置认证上下文
        AuthContext.setCurrentUser(user);
        
        // 添加用户信息到请求属性（兼容行为）
        request.setAttribute("currentUser", user);
        request.setAttribute("currentUserId", user.getUserId());
        request.setAttribute("currentUsername", user.getUsername());
        
        logger.debug("User authenticated: {} [{}]", user.getUsername(), user.getUserId());
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                              Object handler, Exception ex) {
        AuthContext.clear();
    }
    
    /**
     * 从 security 层的 UserPrincipal 构建 web 层的 AuthUser
     */
    private AuthUser buildAuthUser(UserPrincipal principal) {
        String userId = principal.getUserId() != null ? principal.getUserId().toString() : null;
        AuthUser user = new AuthUser(userId, principal.getUsername());
        
        if (principal.getRoles() != null) {
            user.setRoles(new LinkedHashSet<>(principal.getRoles()));
        }
        
        if (principal.getAuthorities() != null) {
            Set<String> permissions = principal.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            user.setPermissions(permissions);
        }
        
        return user;
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
        
        Result<Void> errorResult = Result.error("AUTHENTICATION_FAILED", message);
        
        response.setHeader("WWW-Authenticate", config.getAuthHeaderPrefix().trim());
        
        String responseBody = objectMapper.writeValueAsString(errorResult);
        response.getWriter().write(responseBody);
        response.getWriter().flush();
    }
}

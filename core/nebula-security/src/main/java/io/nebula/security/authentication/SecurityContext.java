package io.nebula.security.authentication;

/**
 * 安全上下文
 * 
 * 存储当前线程的认证信息
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public class SecurityContext {
    
    private static final ThreadLocal<Authentication> CONTEXT = new ThreadLocal<>();
    
    /**
     * 设置认证信息
     */
    public static void setAuthentication(Authentication authentication) {
        CONTEXT.set(authentication);
    }
    
    /**
     * 获取认证信息
     */
    public static Authentication getAuthentication() {
        return CONTEXT.get();
    }
    
    /**
     * 清除认证信息
     */
    public static void clearAuthentication() {
        CONTEXT.remove();
    }
    
    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        Authentication auth = getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) auth.getPrincipal()).getUserId();
        }
        return null;
    }
    
    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        Authentication auth = getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) auth.getPrincipal()).getUsername();
        }
        return null;
    }
}


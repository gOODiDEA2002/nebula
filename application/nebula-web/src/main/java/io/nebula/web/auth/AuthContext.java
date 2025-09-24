package io.nebula.web.auth;

/**
 * 认证上下文
 * 用于在请求处理过程中存储当前用户信息
 */
public class AuthContext {
    
    private static final ThreadLocal<AuthUser> USER_CONTEXT = new ThreadLocal<>();
    
    /**
     * 设置当前用户
     * 
     * @param user 用户信息
     */
    public static void setCurrentUser(AuthUser user) {
        USER_CONTEXT.set(user);
    }
    
    /**
     * 获取当前用户
     * 
     * @return 当前用户信息，如果未认证返回null
     */
    public static AuthUser getCurrentUser() {
        return USER_CONTEXT.get();
    }
    
    /**
     * 获取当前用户ID
     * 
     * @return 当前用户ID，如果未认证返回null
     */
    public static String getCurrentUserId() {
        AuthUser user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }
    
    /**
     * 获取当前用户名
     * 
     * @return 当前用户名，如果未认证返回null
     */
    public static String getCurrentUsername() {
        AuthUser user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }
    
    /**
     * 检查当前用户是否已认证
     * 
     * @return 是否已认证
     */
    public static boolean isAuthenticated() {
        return getCurrentUser() != null;
    }
    
    /**
     * 检查当前用户是否有指定角色
     * 
     * @param role 角色名称
     * @return 是否有指定角色
     */
    public static boolean hasRole(String role) {
        AuthUser user = getCurrentUser();
        return user != null && user.hasRole(role);
    }
    
    /**
     * 检查当前用户是否有指定权限
     * 
     * @param permission 权限名称
     * @return 是否有指定权限
     */
    public static boolean hasPermission(String permission) {
        AuthUser user = getCurrentUser();
        return user != null && user.hasPermission(permission);
    }
    
    /**
     * 检查当前用户是否有任意一个角色
     * 
     * @param roles 角色列表
     * @return 是否有任意一个角色
     */
    public static boolean hasAnyRole(String... roles) {
        AuthUser user = getCurrentUser();
        return user != null && user.hasAnyRole(roles);
    }
    
    /**
     * 检查当前用户是否有任意一个权限
     * 
     * @param permissions 权限列表
     * @return 是否有任意一个权限
     */
    public static boolean hasAnyPermission(String... permissions) {
        AuthUser user = getCurrentUser();
        return user != null && user.hasAnyPermission(permissions);
    }
    
    /**
     * 清除当前用户上下文
     */
    public static void clear() {
        USER_CONTEXT.remove();
    }
}

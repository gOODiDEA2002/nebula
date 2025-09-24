package io.nebula.web.auth;

/**
 * 认证服务接口
 */
public interface AuthService {
    
    /**
     * 根据令牌获取用户信息
     * 
     * @param token 认证令牌
     * @return 用户信息，如果令牌无效返回null
     */
    AuthUser getUser(String token);
    
    /**
     * 验证令牌是否有效
     * 
     * @param token 认证令牌
     * @return 是否有效
     */
    boolean validateToken(String token);
    
    /**
     * 生成访问令牌
     * 
     * @param user 用户信息
     * @return 访问令牌
     */
    String generateToken(AuthUser user);
    
    /**
     * 刷新令牌
     * 
     * @param token 原令牌
     * @return 新令牌
     */
    String refreshToken(String token);
    
    /**
     * 注销令牌
     * 
     * @param token 令牌
     */
    void logout(String token);
}

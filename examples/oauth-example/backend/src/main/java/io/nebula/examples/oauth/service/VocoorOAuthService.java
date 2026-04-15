package io.nebula.examples.oauth.service;

import io.nebula.examples.oauth.entity.dto.VocoorOAuthDto;
import io.nebula.examples.oauth.entity.vo.AuthResultVo;

/**
 * Vocoor OAuth 服务接口
 */
public interface VocoorOAuthService {

    /**
     * 生成授权 URL
     *
     * @param scope 权限范围
     * @return 授权 URL
     */
    String generateAuthUrl(String scope);

    /**
     * 处理授权回调
     *
     * @param code  授权码
     * @param state 状态参数
     * @return 授权结果
     */
    AuthResultVo handleCallback(String code, String state);

    /**
     * 使用授权码换取 Token
     *
     * @param code 授权码
     * @return Token 响应
     */
    VocoorOAuthDto.TokenResult exchangeToken(String code);

    /**
     * 获取 Vocoor 用户信息
     *
     * @param accessToken 访问令牌
     * @return 用户信息
     */
    VocoorOAuthDto.VocoorUser getUserInfo(String accessToken);

    /**
     * 刷新令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新的 Token 响应
     */
    VocoorOAuthDto.TokenResult refreshToken(String refreshToken);
}



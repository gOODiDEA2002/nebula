package io.nebula.examples.oauth.entity.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 授权结果视图对象
 */
@Data
@Builder
public class AuthResultVo {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 本地用户ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 访问令牌（本地生成的JWT）
     */
    private String accessToken;

    /**
     * 令牌过期时间（秒）
     */
    private Integer expiresIn;

    /**
     * 错误信息
     */
    private String errorMessage;

    public static AuthResultVo success(Long userId, String nickname, String avatar, String token) {
        return AuthResultVo.builder()
                .success(true)
                .userId(userId)
                .nickname(nickname)
                .avatar(avatar)
                .accessToken(token)
                .expiresIn(7200)
                .build();
    }

    public static AuthResultVo fail(String errorMessage) {
        return AuthResultVo.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}



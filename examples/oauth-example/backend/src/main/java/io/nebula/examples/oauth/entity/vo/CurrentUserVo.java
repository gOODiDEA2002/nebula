package io.nebula.examples.oauth.entity.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 当前登录用户视图对象
 */
@Data
@Builder
public class CurrentUserVo {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 企业名称
     */
    private String companyName;
}



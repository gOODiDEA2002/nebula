package io.nebula.examples.oauth.service;

import io.nebula.examples.oauth.entity.dos.LocalUser;
import io.nebula.examples.oauth.entity.dto.VocoorOAuthDto;

/**
 * 用户绑定服务接口
 */
public interface UserBindingService {

    /**
     * 通过 Vocoor 用户信息获取或创建本地用户
     *
     * @param vocoorUser Vocoor 用户信息
     * @return 本地用户
     */
    LocalUser getOrCreateLocalUser(VocoorOAuthDto.VocoorUser vocoorUser);

    /**
     * 根据 OpenID 查找本地用户
     *
     * @param openId Vocoor 用户 OpenID
     * @return 本地用户，如果未找到返回 null
     */
    LocalUser findByOpenId(String openId);

    /**
     * 根据用户ID获取用户
     *
     * @param userId 用户ID
     * @return 本地用户
     */
    LocalUser getUserById(Long userId);

    /**
     * 更新用户信息
     *
     * @param userId     用户ID
     * @param vocoorUser Vocoor 用户信息
     */
    void updateUserInfo(Long userId, VocoorOAuthDto.VocoorUser vocoorUser);
}



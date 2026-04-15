package io.nebula.examples.oauth.service.impl;

import io.nebula.examples.oauth.entity.dos.LocalUser;
import io.nebula.examples.oauth.entity.dos.UserBinding;
import io.nebula.examples.oauth.entity.dto.VocoorOAuthDto;
import io.nebula.examples.oauth.mapper.LocalUserMapper;
import io.nebula.examples.oauth.mapper.UserBindingMapper;
import io.nebula.examples.oauth.service.UserBindingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户绑定服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserBindingServiceImpl implements UserBindingService {

    private final UserBindingMapper userBindingMapper;
    private final LocalUserMapper localUserMapper;

    @Override
    @Transactional
    public LocalUser getOrCreateLocalUser(VocoorOAuthDto.VocoorUser vocoorUser) {
        log.info("获取或创建本地用户，OpenID: {}", vocoorUser.getOpenid());

        // 1. 通过 OpenID 查找绑定关系
        UserBinding binding = userBindingMapper.findByOpenId(vocoorUser.getOpenid());

        if (binding != null) {
            // 2. 已绑定，获取本地用户并更新信息
            LocalUser existingUser = localUserMapper.selectById(binding.getUserId());
            if (existingUser != null) {
                updateUserInfo(existingUser.getId(), vocoorUser);
                // 重新查询获取更新后的用户
                return localUserMapper.selectById(existingUser.getId());
            }
        }

        // 3. 未绑定，创建新用户
        LocalUser newUser = new LocalUser();
        newUser.setUsername("vocoor_" + vocoorUser.getOpenid().substring(0, 8));
        newUser.setNickname(vocoorUser.getNickname());
        newUser.setAvatar(vocoorUser.getAvatar());
        newUser.setMobile(vocoorUser.getMobile());
        newUser.setEmail(vocoorUser.getEmail());
        if (vocoorUser.getCompany() != null) {
            newUser.setCompanyName(vocoorUser.getCompany().getName());
        }
        newUser.setStatus(1);
        newUser.setCreateTime(LocalDateTime.now());
        newUser.setUpdateTime(LocalDateTime.now());

        localUserMapper.insert(newUser);
        log.info("创建新用户，ID: {}", newUser.getId());

        // 4. 创建绑定关系
        UserBinding newBinding = new UserBinding();
        newBinding.setUserId(newUser.getId());
        newBinding.setOpenId(vocoorUser.getOpenid());
        newBinding.setSource("vocoor");
        newBinding.setNickname(vocoorUser.getNickname());
        newBinding.setAvatar(vocoorUser.getAvatar());
        newBinding.setCreateTime(LocalDateTime.now());
        newBinding.setUpdateTime(LocalDateTime.now());

        userBindingMapper.insert(newBinding);
        log.info("创建用户绑定，用户ID: {}, OpenID: {}", newUser.getId(), vocoorUser.getOpenid());

        return newUser;
    }

    @Override
    public LocalUser findByOpenId(String openId) {
        UserBinding binding = userBindingMapper.findByOpenId(openId);
        if (binding != null) {
            return localUserMapper.selectById(binding.getUserId());
        }
        return null;
    }

    @Override
    public LocalUser getUserById(Long userId) {
        return localUserMapper.selectById(userId);
    }

    @Override
    @Transactional
    public void updateUserInfo(Long userId, VocoorOAuthDto.VocoorUser vocoorUser) {
        LocalUser user = localUserMapper.selectById(userId);
        if (user != null) {
            user.setNickname(vocoorUser.getNickname());
            user.setAvatar(vocoorUser.getAvatar());
            user.setMobile(vocoorUser.getMobile());
            user.setEmail(vocoorUser.getEmail());
            if (vocoorUser.getCompany() != null) {
                user.setCompanyName(vocoorUser.getCompany().getName());
            }
            user.setUpdateTime(LocalDateTime.now());

            localUserMapper.updateById(user);
            log.info("更新用户信息，用户ID: {}", userId);

            // 同时更新绑定表中的冗余字段
            UserBinding binding = userBindingMapper.findByUserId(userId);
            if (binding != null) {
                binding.setNickname(vocoorUser.getNickname());
                binding.setAvatar(vocoorUser.getAvatar());
                binding.setUpdateTime(LocalDateTime.now());
                userBindingMapper.updateById(binding);
            }
        }
    }
}



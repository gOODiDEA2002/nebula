package io.nebula.examples.oauth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.nebula.examples.oauth.entity.dos.UserBinding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户绑定 Mapper 接口
 */
@Mapper
public interface UserBindingMapper extends BaseMapper<UserBinding> {

    /**
     * 根据 OpenID 查询绑定记录
     *
     * @param openId 用户 OpenID
     * @return 绑定记录
     */
    @Select("SELECT * FROM user_binding WHERE open_id = #{openId}")
    UserBinding findByOpenId(@Param("openId") String openId);

    /**
     * 根据用户ID查询绑定记录
     *
     * @param userId 本地用户ID
     * @return 绑定记录
     */
    @Select("SELECT * FROM user_binding WHERE user_id = #{userId}")
    UserBinding findByUserId(@Param("userId") Long userId);
}



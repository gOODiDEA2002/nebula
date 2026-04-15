package io.nebula.examples.oauth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.nebula.examples.oauth.entity.dos.LocalUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 本地用户 Mapper 接口
 */
@Mapper
public interface LocalUserMapper extends BaseMapper<LocalUser> {
}



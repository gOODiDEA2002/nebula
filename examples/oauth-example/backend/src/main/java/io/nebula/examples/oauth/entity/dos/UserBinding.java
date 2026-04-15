package io.nebula.examples.oauth.entity.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户第三方绑定实体
 * 存储本地用户与 Vocoor 用户的绑定关系
 */
@Data
@TableName("user_binding")
public class UserBinding {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 本地用户ID
     */
    private Long userId;

    /**
     * Vocoor 用户唯一标识（OpenID）
     */
    private String openId;

    /**
     * 来源：vocoor
     */
    private String source;

    /**
     * 用户昵称（冗余存储）
     */
    private String nickname;

    /**
     * 用户头像（冗余存储）
     */
    private String avatar;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}



package io.nebula.example.user.api.vo;

import lombok.Data;

/**
 * 用户视图对象（用于RPC演示）
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
public class UserVo {
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 姓名
     */
    private String name;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 电话
     */
    private String phone;
    
    /**
     * 状态
     */
    private String status;
}


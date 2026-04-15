package io.nebula.example.user.api.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体（用于RPC演示）
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
public class User {
    
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
     * 状态：ACTIVE, INACTIVE, LOCKED
     */
    private String status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}


package io.nebula.example.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户响应DTO
 */
@Data
@Builder
public class UserResponse {
    
    private Long id;
    private String username;
    private String email;
    private String realName;
    private String phone;
    private Integer status;
    private String role;
    private String avatarUrl;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
}

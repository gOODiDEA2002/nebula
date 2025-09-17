package io.nebula.example.dto;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 用户更新请求DTO
 */
@Data
public class UserUpdateRequest {
    
    @Size(min = 6, message = "密码长度不能少于6位")
    private String password;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Size(max = 100, message = "真实姓名长度不能超过100字符")
    private String realName;
    
    @Size(max = 20, message = "手机号长度不能超过20字符")
    private String phone;
    
    private Integer status;
    
    @Size(max = 50, message = "角色长度不能超过50字符")
    private String role;
    
    @Size(max = 255, message = "头像URL长度不能超过255字符")
    private String avatarUrl;
}

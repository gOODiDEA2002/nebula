package io.nebula.example.dto;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户创建请求DTO
 */
@Data
public class UserCreateRequest {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50字符之间")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能少于6位")
    private String password;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Size(max = 100, message = "真实姓名长度不能超过100字符")
    private String realName;
    
    @Size(max = 20, message = "手机号长度不能超过20字符")
    private String phone;
    
    @Size(max = 50, message = "角色长度不能超过50字符")
    private String role;
}

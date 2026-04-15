package io.nebula.example.user.api.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建用户接口DTO
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class CreateUserDto {

    /**
     * 创建用户请求
     */
    @Data
    public static class Request {
        /**
         * 用户名
         */
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 20, message = "用户名长度必须在3-20个字符之间")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
        private String username;
        
        /**
         * 姓名
         */
        @NotBlank(message = "姓名不能为空")
        @Size(max = 50, message = "姓名长度不能超过50个字符")
        private String name;
        
        /**
         * 邮箱
         */
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;
        
        /**
         * 电话
         */
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "电话格式不正确")
        private String phone;
        
        /**
         * 状态
         */
        @Pattern(regexp = "^(ACTIVE|INACTIVE|LOCKED)$", message = "状态只能是ACTIVE、INACTIVE或LOCKED")
        private String status;
    }

    /**
     * 创建用户响应
     */
    @Data
    public static class Response {
        /**
         * 创建的用户ID
         */
        private Long id;
    }
}


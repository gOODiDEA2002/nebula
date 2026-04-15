package io.nebula.example.user.api.dto;

import io.nebula.example.user.api.vo.UserVo;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 更新用户接口DTO
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class UpdateUserDto {

    /**
     * 更新用户请求
     */
    @Data
    public static class Request {
        /**
         * 用户ID
         */
        @NotNull(message = "用户ID不能为空")
        private Long id;
        
        /**
         * 姓名
         */
        @Size(max = 50, message = "姓名长度不能超过50个字符")
        private String name;
        
        /**
         * 邮箱
         */
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
     * 更新用户响应
     */
    @Data
    public static class Response {
        /**
         * 更新后的用户信息
         */
        private UserVo user;
    }
}


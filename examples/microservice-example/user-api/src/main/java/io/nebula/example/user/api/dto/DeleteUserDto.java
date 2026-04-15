package io.nebula.example.user.api.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 删除用户接口DTO
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class DeleteUserDto {

    /**
     * 删除用户请求
     */
    @Data
    public static class Request {
        /**
         * 用户ID
         */
        @NotNull(message = "用户ID不能为空")
        private Long id;
    }

    /**
     * 删除用户响应
     */
    @Data
    public static class Response {
        /**
         * 是否成功
         */
        private Boolean success;
    }
}


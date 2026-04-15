package io.nebula.example.user.api.dto;

import io.nebula.example.user.api.vo.UserVo;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 获取用户详情接口DTO
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class GetUserDto {

    /**
     * 获取用户详情请求
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
     * 获取用户详情响应
     */
    @Data
    public static class Response {
        /**
         * 用户信息
         */
        private UserVo user;
    }
}


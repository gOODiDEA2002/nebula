package io.nebula.example.user.api.dto;

import io.nebula.example.user.api.vo.UserVo;
import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.util.List;

/**
 * 获取用户列表接口DTO
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class GetUsersDto {

    /**
     * 获取用户列表请求
     */
    @Data
    public static class Request {
        /**
         * 用户名（关键字搜索）
         */
        private String username;
        
        /**
         * 姓名（关键字搜索）
         */
        private String name;
        
        /**
         * 状态筛选
         */
        private String status;
        
        /**
         * 页码
         */
        @Min(value = 1, message = "页码必须大于0")
        private Integer page = 1;
        
        /**
         * 每页大小
         */
        @Min(value = 1, message = "每页大小必须大于0")
        @Max(value = 100, message = "每页大小不能超过100")
        private Integer size = 10;
    }

    /**
     * 获取用户列表响应
     */
    @Data
    public static class Response {
        /**
         * 用户列表
         */
        private List<UserVo> users;
        
        /**
         * 总记录数
         */
        private Long total;
        
        /**
         * 当前页
         */
        private Integer page;
        
        /**
         * 每页大小
         */
        private Integer size;
    }
}


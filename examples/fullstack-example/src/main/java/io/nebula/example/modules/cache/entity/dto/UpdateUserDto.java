package io.nebula.example.modules.cache.entity.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 更新用户接口DTO
 */
public class UpdateUserDto {
    
    /**
     * 更新用户请求
     */
    @Data
    @Schema(description = "更新用户请求")
    public static class Request {
        
        @Schema(description = "用户ID", example = "123")
        @NotNull(message = "用户ID不能为空")
        @Min(value = 1, message = "用户ID必须大于0")
        private Long userId;
        
        @Schema(description = "用户名", example = "李四")
        private String username;
        
        @Schema(description = "邮箱", example = "lisi@example.com")
        private String email;
        
        @Schema(description = "年龄", example = "30")
        @Min(value = 1, message = "年龄必须大于0")
        private Integer age;
    }
    
    /**
     * 更新用户响应
     */
    @Schema(description = "更新用户响应")
    public static class Response extends GetUserDto.Response {
    }
}

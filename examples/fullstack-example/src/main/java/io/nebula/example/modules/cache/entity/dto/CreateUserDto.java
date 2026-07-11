package io.nebula.example.modules.cache.entity.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 创建用户接口DTO
 */
public class CreateUserDto {
    
    /**
     * 创建用户请求
     */
    @Data
    @Schema(description = "创建用户请求")
    public static class Request {
        
        @Schema(description = "用户名", example = "张三")
        @NotBlank(message = "用户名不能为空")
        private String username;
        
        @Schema(description = "邮箱", example = "zhangsan@example.com")
        @NotBlank(message = "邮箱不能为空")
        private String email;
        
        @Schema(description = "年龄", example = "25")
        @Min(value = 1, message = "年龄必须大于0")
        private Integer age;
    }
    
    /**
     * 创建用户响应
     */
    @Schema(description = "创建用户响应")
    public static class Response extends GetUserDto.Response {
    }
}

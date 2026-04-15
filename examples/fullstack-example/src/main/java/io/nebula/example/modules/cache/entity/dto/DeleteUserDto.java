package io.nebula.example.modules.cache.entity.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 删除用户接口DTO
 */
public class DeleteUserDto {
    
    /**
     * 删除用户请求
     */
    @Data
    @Schema(description = "删除用户请求")
    public static class Request {
        
        @Schema(description = "用户ID", example = "123")
        @NotNull(message = "用户ID不能为空")
        @Min(value = 1, message = "用户ID必须大于0")
        private Long userId;
    }
    
    /**
     * 删除用户响应
     */
    @Data
    @Schema(description = "删除用户响应")
    public static class Response {
        
        @Schema(description = "是否删除成功", example = "true")
        private Boolean success;
        
        @Schema(description = "用户ID", example = "123")
        private Long userId;
    }
}

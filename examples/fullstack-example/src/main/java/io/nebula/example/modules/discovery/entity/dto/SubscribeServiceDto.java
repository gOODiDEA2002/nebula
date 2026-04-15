package io.nebula.example.modules.discovery.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 订阅服务变化接口DTO
 */
public class SubscribeServiceDto {
    
    /**
     * 订阅服务变化请求
     */
    @Data
    public static class Request {
        
        /** 服务名称 */
        @NotBlank(message = "服务名称不能为空")
        private String serviceName;
        
        /** 分组名称 */
        private String groupName;
    }
    
    /**
     * 订阅服务变化响应
     */
    @Data
    public static class Response {
        
        /** 是否成功 */
        private Boolean success;
        
        /** 消息 */
        private String message;
    }
}



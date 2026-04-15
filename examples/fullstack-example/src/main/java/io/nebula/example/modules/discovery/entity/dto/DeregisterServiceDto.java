package io.nebula.example.modules.discovery.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 注销服务接口DTO
 */
public class DeregisterServiceDto {
    
    /**
     * 注销服务请求
     */
    @Data
    public static class Request {
        
        /** 服务名称 */
        @NotBlank(message = "服务名称不能为空")
        private String serviceName;
        
        /** 实例ID */
        @NotBlank(message = "实例ID不能为空")
        private String instanceId;
    }
    
    /**
     * 注销服务响应
     */
    @Data
    public static class Response {
        
        /** 是否成功 */
        private Boolean success;
        
        /** 消息 */
        private String message;
    }
}



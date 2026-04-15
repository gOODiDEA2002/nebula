package io.nebula.example.modules.discovery.entity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 注册服务接口DTO
 */
public class RegisterServiceDto {
    
    /**
     * 注册服务请求
     */
    @Data
    public static class Request {
        
        /** 服务名称 */
        @NotBlank(message = "服务名称不能为空")
        private String serviceName;
        
        /** 实例ID */
        @NotBlank(message = "实例ID不能为空")
        private String instanceId;
        
        /** IP地址 */
        @NotBlank(message = "IP地址不能为空")
        private String ip;
        
        /** 端口号 */
        @NotNull(message = "端口号不能为空")
        @Min(value = 1, message = "端口号必须大于0")
        private Integer port;
        
        /** 权重 */
        private Double weight = 1.0;
        
        /** 集群名称 */
        private String clusterName;
        
        /** 分组名称 */
        private String groupName;
        
        /** 协议 */
        private String protocol = "http";
        
        /** 元数据 */
        private Map<String, String> metadata;
    }
    
    /**
     * 注册服务响应
     */
    @Data
    public static class Response {
        
        /** 是否成功 */
        private Boolean success;
        
        /** 服务地址 */
        private String address;
        
        /** 消息 */
        private String message;
    }
}



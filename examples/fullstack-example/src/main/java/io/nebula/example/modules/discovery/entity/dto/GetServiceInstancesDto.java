package io.nebula.example.modules.discovery.entity.dto;

import io.nebula.example.modules.discovery.entity.vo.ServiceInstanceVo;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 获取服务实例接口DTO
 */
public class GetServiceInstancesDto {
    
    /**
     * 获取服务实例请求
     */
    @Data
    public static class Request {
        
        /** 服务名称 */
        @NotBlank(message = "服务名称不能为空")
        private String serviceName;
        
        /** 分组名称 */
        private String groupName;
        
        /** 是否只获取健康实例 */
        private Boolean healthyOnly = true;
    }
    
    /**
     * 获取服务实例响应
     */
    @Data
    public static class Response {
        
        /** 服务实例列表 */
        private List<ServiceInstanceVo> instances;
        
        /** 实例总数 */
        private Integer total;
    }
}



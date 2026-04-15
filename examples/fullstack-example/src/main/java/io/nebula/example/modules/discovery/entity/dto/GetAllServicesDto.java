package io.nebula.example.modules.discovery.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * 获取所有服务接口DTO
 */
public class GetAllServicesDto {
    
    /**
     * 获取所有服务请求
     */
    @Data
    public static class Request {
        
        /** 分组名称 */
        private String groupName;
        
        /** 页码 */
        private Integer pageNo = 1;
        
        /** 每页大小 */
        private Integer pageSize = 100;
    }
    
    /**
     * 获取所有服务响应
     */
    @Data
    public static class Response {
        
        /** 服务名称列表 */
        private List<String> services;
        
        /** 服务总数 */
        private Integer total;
    }
}



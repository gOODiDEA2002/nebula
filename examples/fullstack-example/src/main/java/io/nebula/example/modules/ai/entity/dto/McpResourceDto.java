package io.nebula.example.modules.ai.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * MCP资源DTO
 */
public class McpResourceDto {
    
    /**
     * 获取MCP资源列表请求
     */
    @Data
    public static class ListRequest {
        // 无参数
    }
    
    /**
     * 获取MCP资源列表响应
     */
    @Data
    public static class ListResponse {
        /**
         * 资源列表
         */
        private List<ResourceInfo> resources;
    }
    
    /**
     * 读取MCP资源请求
     */
    @Data
    public static class ReadRequest {
        /**
         * 资源URI
         */
        @NotBlank(message = "资源URI不能为空")
        private String resourceUri;
    }
    
    /**
     * 读取MCP资源响应
     */
    @Data
    public static class ReadResponse {
        /**
         * 资源内容
         */
        private String content;
        
        /**
         * MIME类型
         */
        private String mimeType;
    }
    
    /**
     * 资源信息
     */
    @Data
    public static class ResourceInfo {
        /**
         * 资源URI
         */
        private String uri;
        
        /**
         * 资源名称
         */
        private String name;
        
        /**
         * 资源描述
         */
        private String description;
        
        /**
         * MIME类型
         */
        private String mimeType;
    }
}


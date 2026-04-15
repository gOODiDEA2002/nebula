package io.nebula.example.modules.ai.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * MCP工具DTO
 */
public class McpToolDto {
    
    /**
     * 获取MCP工具列表请求
     */
    @Data
    public static class ListRequest {
        // 无参数
    }
    
    /**
     * 获取MCP工具列表响应
     */
    @Data
    public static class ListResponse {
        /**
         * 工具列表
         */
        private List<ToolInfo> tools;
    }
    
    /**
     * 调用MCP工具请求
     */
    @Data
    public static class CallRequest {
        /**
         * 工具名称
         */
        @NotBlank(message = "工具名称不能为空")
        private String toolName;
        
        /**
         * 工具参数(JSON格式)
         */
        @NotBlank(message = "工具参数不能为空")
        private String arguments;
    }
    
    /**
     * 调用MCP工具响应
     */
    @Data
    public static class CallResponse {
        /**
         * 执行结果
         */
        private String result;
    }
    
    /**
     * 工具信息
     */
    @Data
    public static class ToolInfo {
        /**
         * 工具名称
         */
        private String name;
        
        /**
         * 工具描述
         */
        private String description;
        
        /**
         * 输入schema
         */
        private Map<String, Object> inputSchema;
    }
}


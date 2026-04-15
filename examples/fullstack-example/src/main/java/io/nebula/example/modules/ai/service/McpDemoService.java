package io.nebula.example.modules.ai.service;

import io.nebula.example.modules.ai.entity.dto.McpResourceDto;
import io.nebula.example.modules.ai.entity.dto.McpToolDto;

/**
 * MCP演示服务接口
 */
public interface McpDemoService {
    
    /**
     * 获取所有注册的MCP工具
     * @param request 请求参数
     * @return 工具列表
     */
    McpToolDto.ListResponse listTools(McpToolDto.ListRequest request);
    
    /**
     * 调用MCP工具
     * @param request 请求参数
     * @return 执行结果
     */
    McpToolDto.CallResponse callTool(McpToolDto.CallRequest request);
    
    /**
     * 获取所有注册的MCP资源
     * @param request 请求参数
     * @return 资源列表
     */
    McpResourceDto.ListResponse listResources(McpResourceDto.ListRequest request);
    
    /**
     * 读取MCP资源
     * @param request 请求参数
     * @return 资源内容
     */
    McpResourceDto.ReadResponse readResource(McpResourceDto.ReadRequest request);
}


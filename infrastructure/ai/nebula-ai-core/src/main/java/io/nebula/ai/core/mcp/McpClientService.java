package io.nebula.ai.core.mcp;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * MCP客户端服务接口
 * 提供连接到MCP服务器并使用其工具和资源的能力
 */
public interface McpClientService {
    
    /**
     * 列出可用的工具
     * @return 工具列表
     */
    List<McpTool> listTools();
    
    /**
     * 调用远程工具
     * @param toolName 工具名称
     * @param arguments 工具参数(JSON格式)
     * @return 执行结果
     */
    String callTool(String toolName, String arguments);
    
    /**
     * 异步调用远程工具
     * @param toolName 工具名称
     * @param arguments 工具参数(JSON格式)
     * @return 异步执行结果
     */
    CompletableFuture<String> callToolAsync(String toolName, String arguments);
    
    /**
     * 列出可用的资源
     * @return 资源列表
     */
    List<McpResource> listResources();
    
    /**
     * 读取远程资源
     * @param resourceUri 资源URI
     * @return 资源内容
     */
    String readResource(String resourceUri);
    
    /**
     * 异步读取远程资源
     * @param resourceUri 资源URI
     * @return 异步资源内容
     */
    CompletableFuture<String> readResourceAsync(String resourceUri);
    
    /**
     * 检查客户端是否已连接
     * @return 是否已连接
     */
    boolean isConnected();
    
    /**
     * 关闭客户端连接
     */
    void close();
}


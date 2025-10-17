package io.nebula.ai.core.mcp;

import java.util.Map;

/**
 * MCP工具定义
 * 表示一个Model Context Protocol工具
 */
public interface McpTool {
    
    /**
     * 获取工具名称
     * @return 工具名称
     */
    String getName();
    
    /**
     * 获取工具描述
     * @return 工具描述
     */
    String getDescription();
    
    /**
     * 获取工具输入schema
     * @return JSON Schema定义
     */
    Map<String, Object> getInputSchema();
    
    /**
     * 执行工具
     * @param arguments 工具参数(JSON格式)
     * @return 执行结果
     */
    String execute(String arguments);
}


package io.nebula.ai.core.mcp;

/**
 * MCP资源定义
 * 表示一个Model Context Protocol资源
 */
public interface McpResource {
    
    /**
     * 获取资源URI
     * @return 资源URI
     */
    String getUri();
    
    /**
     * 获取资源名称
     * @return 资源名称
     */
    String getName();
    
    /**
     * 获取资源描述
     * @return 资源描述
     */
    String getDescription();
    
    /**
     * 获取资源MIME类型
     * @return MIME类型
     */
    String getMimeType();
    
    /**
     * 读取资源内容
     * @return 资源内容
     */
    String getContent();
}


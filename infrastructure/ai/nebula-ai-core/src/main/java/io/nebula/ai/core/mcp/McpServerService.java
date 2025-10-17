package io.nebula.ai.core.mcp;

import java.util.List;

/**
 * MCP服务器服务接口
 * 提供MCP服务器的核心功能,将工具和资源暴露给MCP客户端
 */
public interface McpServerService {
    
    /**
     * 注册MCP工具
     * @param tool 工具实例
     */
    void registerTool(McpTool tool);
    
    /**
     * 注销MCP工具
     * @param toolName 工具名称
     */
    void unregisterTool(String toolName);
    
    /**
     * 获取所有注册的工具
     * @return 工具列表
     */
    List<McpTool> getTools();
    
    /**
     * 注册MCP资源
     * @param resource 资源实例
     */
    void registerResource(McpResource resource);
    
    /**
     * 注销MCP资源
     * @param resourceUri 资源URI
     */
    void unregisterResource(String resourceUri);
    
    /**
     * 获取所有注册的资源
     * @return 资源列表
     */
    List<McpResource> getResources();
    
    /**
     * 获取服务器信息
     * @return 服务器信息
     */
    ServerInfo getServerInfo();
    
    /**
     * 服务器信息类
     */
    class ServerInfo {
        private final String name;
        private final String version;
        private final boolean enabled;
        
        public ServerInfo(String name, String version, boolean enabled) {
            this.name = name;
            this.version = version;
            this.enabled = enabled;
        }
        
        public String getName() {
            return name;
        }
        
        public String getVersion() {
            return version;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
    }
}


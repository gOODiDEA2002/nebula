package io.nebula.ai.spring.mcp;

import io.nebula.ai.core.mcp.McpResource;
import io.nebula.ai.core.mcp.McpServerService;
import io.nebula.ai.core.mcp.McpTool;
import io.nebula.ai.spring.config.McpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring AI MCP服务器服务实现
 * 基于Spring AI的MCP Server功能
 */
public class SpringAIMcpServerService implements McpServerService {
    
    private static final Logger logger = LoggerFactory.getLogger(SpringAIMcpServerService.class);
    
    private final McpProperties.Server serverProperties;
    private final Map<String, McpTool> tools = new ConcurrentHashMap<>();
    private final Map<String, McpResource> resources = new ConcurrentHashMap<>();
    
    public SpringAIMcpServerService(McpProperties.Server serverProperties) {
        this.serverProperties = serverProperties;
        logger.info("Initialized Spring AI MCP Server: {} v{}", 
                   serverProperties.getName(), 
                   serverProperties.getVersion());
    }
    
    @Override
    public void registerTool(McpTool tool) {
        if (tool == null || tool.getName() == null) {
            throw new IllegalArgumentException("Tool or tool name cannot be null");
        }
        tools.put(tool.getName(), tool);
        logger.info("Registered MCP tool: {}", tool.getName());
    }
    
    @Override
    public void unregisterTool(String toolName) {
        if (toolName == null) {
            throw new IllegalArgumentException("Tool name cannot be null");
        }
        McpTool removed = tools.remove(toolName);
        if (removed != null) {
            logger.info("Unregistered MCP tool: {}", toolName);
        }
    }
    
    @Override
    public List<McpTool> getTools() {
        return new ArrayList<>(tools.values());
    }
    
    @Override
    public void registerResource(McpResource resource) {
        if (resource == null || resource.getUri() == null) {
            throw new IllegalArgumentException("Resource or resource URI cannot be null");
        }
        resources.put(resource.getUri(), resource);
        logger.info("Registered MCP resource: {}", resource.getUri());
    }
    
    @Override
    public void unregisterResource(String resourceUri) {
        if (resourceUri == null) {
            throw new IllegalArgumentException("Resource URI cannot be null");
        }
        McpResource removed = resources.remove(resourceUri);
        if (removed != null) {
            logger.info("Unregistered MCP resource: {}", resourceUri);
        }
    }
    
    @Override
    public List<McpResource> getResources() {
        return new ArrayList<>(resources.values());
    }
    
    @Override
    public ServerInfo getServerInfo() {
        return new ServerInfo(
            serverProperties.getName(),
            serverProperties.getVersion(),
            serverProperties.isEnabled()
        );
    }
}


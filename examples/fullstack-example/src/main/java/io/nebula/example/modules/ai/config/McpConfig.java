package io.nebula.example.modules.ai.config;

import io.nebula.ai.core.mcp.McpResource;
import io.nebula.ai.core.mcp.McpServerService;
import io.nebula.ai.core.mcp.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.util.List;

/**
 * MCP配置类
 * 自动注册所有MCP工具和资源
 */
@Configuration
@ConditionalOnBean(McpServerService.class)
public class McpConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(McpConfig.class);
    
    private final McpServerService mcpServerService;
    private final List<McpTool> mcpTools;
    private final List<McpResource> mcpResources;
    
    @Autowired
    public McpConfig(McpServerService mcpServerService,
                     List<McpTool> mcpTools,
                     List<McpResource> mcpResources) {
        this.mcpServerService = mcpServerService;
        this.mcpTools = mcpTools;
        this.mcpResources = mcpResources;
    }
    
    /**
     * 应用启动后自动注册所有MCP工具和资源
     */
    @EventListener(ContextRefreshedEvent.class)
    public void registerMcpToolsAndResources() {
        logger.info("开始注册MCP工具和资源");
        
        // 注册工具
        for (McpTool tool : mcpTools) {
            mcpServerService.registerTool(tool);
            logger.info("已注册MCP工具: {}", tool.getName());
        }
        
        // 注册资源
        for (McpResource resource : mcpResources) {
            mcpServerService.registerResource(resource);
            logger.info("已注册MCP资源: {}", resource.getUri());
        }
        
        logger.info("MCP工具和资源注册完成,共注册{}个工具,{}个资源", 
                   mcpTools.size(), mcpResources.size());
    }
}


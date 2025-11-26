package io.nebula.autoconfigure.ai;

import io.nebula.ai.spring.config.McpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Nebula MCP Server 自动配置
 * 
 * 将 nebula.ai.mcp 配置桥接到 Spring AI 的 spring.ai.mcp.server 配置
 * 
 * 这样用户只需配置 nebula.ai.mcp，框架会自动设置对应的 Spring AI 配置
 * 
 * @author Nebula Framework
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.ai.mcp.server.McpServer")
@ConditionalOnProperty(prefix = "nebula.ai.mcp.server", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(McpProperties.class)
public class NebulaMcpServerAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NebulaMcpServerAutoConfiguration.class);

    private final McpProperties mcpProperties;
    private final ConfigurableEnvironment environment;

    public NebulaMcpServerAutoConfiguration(McpProperties mcpProperties, ConfigurableEnvironment environment) {
        this.mcpProperties = mcpProperties;
        this.environment = environment;
    }

    @PostConstruct
    public void bridgeToSpringAi() {
        McpProperties.Server server = mcpProperties.getServer();
        
        // 将 nebula.ai.mcp.server 配置映射到 spring.ai.mcp.server
        Map<String, Object> springAiProps = new HashMap<>();
        
        // 基本配置
        springAiProps.put("spring.ai.mcp.server.enabled", server.isEnabled());
        springAiProps.put("spring.ai.mcp.server.protocol", server.getProtocol());
        springAiProps.put("spring.ai.mcp.server.name", server.getName());
        springAiProps.put("spring.ai.mcp.server.version", server.getVersion());
        springAiProps.put("spring.ai.mcp.server.type", server.getType());
        springAiProps.put("spring.ai.mcp.server.instructions", server.getInstructions());
        springAiProps.put("spring.ai.mcp.server.tool-callback-converter", server.isToolCallbackConverter());
        
        // 能力配置
        McpProperties.Capabilities cap = server.getCapabilities();
        springAiProps.put("spring.ai.mcp.server.capabilities.tool", cap.isTool());
        springAiProps.put("spring.ai.mcp.server.capabilities.resource", cap.isResource());
        springAiProps.put("spring.ai.mcp.server.capabilities.prompt", cap.isPrompt());
        springAiProps.put("spring.ai.mcp.server.capabilities.completion", cap.isCompletion());
        
        // 变更通知
        springAiProps.put("spring.ai.mcp.server.tool-change-notification", server.isToolChangeNotification());
        springAiProps.put("spring.ai.mcp.server.resource-change-notification", server.isResourceChangeNotification());
        
        // Streamable HTTP 配置
        McpProperties.StreamableHttp http = server.getStreamableHttp();
        springAiProps.put("spring.ai.mcp.server.streamable-http.mcp-endpoint", http.getMcpEndpoint());
        springAiProps.put("spring.ai.mcp.server.streamable-http.keep-alive-interval", http.getKeepAliveInterval());
        
        // 添加到 Spring Environment（低优先级，允许显式配置覆盖）
        MapPropertySource propertySource = new MapPropertySource("nebulaMcpBridge", springAiProps);
        environment.getPropertySources().addLast(propertySource);
        
        log.info("Nebula MCP Server 配置桥接完成: name={}, protocol={}, endpoint={}",
                server.getName(), server.getProtocol(), http.getMcpEndpoint());
    }
}




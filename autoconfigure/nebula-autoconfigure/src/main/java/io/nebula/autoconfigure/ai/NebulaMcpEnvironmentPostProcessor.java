package io.nebula.autoconfigure.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * 环境后处理器：在应用启动早期阶段将 nebula.ai.mcp 配置桥接到 spring.ai.mcp.server
 * 
 * 这个处理器在 Spring 环境准备阶段运行，确保 Spring AI MCP Server 能够
 * 正确读取配置并注册 MCP 端点
 * 
 * 工作原理:
 * 1. 在 Spring 环境准备阶段（所有 Bean 创建之前）执行
 * 2. 读取 nebula.ai.mcp.server.* 配置
 * 3. 将配置映射到 spring.ai.mcp.server.* 
 * 4. 添加到 Spring Environment 中，优先级最高
 * 
 * 这样 Spring AI 的 MCP Server 自动配置就能读取到正确的配置，
 * 从而正确注册 MCP 端点（如 /mcp）
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class NebulaMcpEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    
    private static final Logger log = LoggerFactory.getLogger(NebulaMcpEnvironmentPostProcessor.class);
    
    private static final String NEBULA_PREFIX = "nebula.ai.mcp.server.";
    private static final String SPRING_AI_PREFIX = "spring.ai.mcp.server.";
    
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // 检查是否启用了 Nebula MCP Server
        String enabled = environment.getProperty(NEBULA_PREFIX + "enabled");
        if (!"true".equalsIgnoreCase(enabled)) {
            return;
        }
        
        Map<String, Object> springAiProps = new HashMap<>();
        
        // 基本配置映射
        mapProperty(environment, springAiProps, "enabled");
        mapProperty(environment, springAiProps, "protocol");
        mapProperty(environment, springAiProps, "name");
        mapProperty(environment, springAiProps, "version");
        mapProperty(environment, springAiProps, "type");
        mapProperty(environment, springAiProps, "instructions");
        mapProperty(environment, springAiProps, "tool-callback-converter");
        
        // 能力配置映射
        mapProperty(environment, springAiProps, "capabilities.tool");
        mapProperty(environment, springAiProps, "capabilities.resource");
        mapProperty(environment, springAiProps, "capabilities.prompt");
        mapProperty(environment, springAiProps, "capabilities.completion");
        
        // 变更通知配置映射
        mapProperty(environment, springAiProps, "tool-change-notification");
        mapProperty(environment, springAiProps, "resource-change-notification");
        
        // Streamable HTTP 配置映射
        mapProperty(environment, springAiProps, "streamable-http.mcp-endpoint");
        mapProperty(environment, springAiProps, "streamable-http.keep-alive-interval");
        
        if (!springAiProps.isEmpty()) {
            // 添加到 Spring Environment（高优先级，确保覆盖默认配置）
            MapPropertySource propertySource = new MapPropertySource("nebulaMcpBridge", springAiProps);
            environment.getPropertySources().addFirst(propertySource);
            
            log.info("Nebula MCP 配置桥接完成: {} 个属性已映射到 spring.ai.mcp.server.*", springAiProps.size());
        }
    }
    
    /**
     * 将 nebula.ai.mcp.server.xxx 属性映射到 spring.ai.mcp.server.xxx
     */
    private void mapProperty(ConfigurableEnvironment environment, 
                             Map<String, Object> targetProps, 
                             String propertyName) {
        String value = environment.getProperty(NEBULA_PREFIX + propertyName);
        if (value != null) {
            targetProps.put(SPRING_AI_PREFIX + propertyName, value);
        }
    }
    
    @Override
    public int getOrder() {
        // 确保在 ConfigDataEnvironmentPostProcessor 之后运行
        // ConfigDataEnvironmentPostProcessor.ORDER = Ordered.HIGHEST_PRECEDENCE + 10
        // 我们设置为 Ordered.HIGHEST_PRECEDENCE + 15，确保 application.yml 已加载
        return Ordered.HIGHEST_PRECEDENCE + 15;
    }
}


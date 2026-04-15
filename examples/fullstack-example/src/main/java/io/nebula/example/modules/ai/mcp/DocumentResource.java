package io.nebula.example.modules.ai.mcp;

import io.nebula.ai.core.mcp.McpResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 文档资源示例
 * 演示如何实现一个MCP资源
 */
@Component
@ConditionalOnProperty(prefix = "nebula.ai", name = "enabled", havingValue = "true")
public class DocumentResource implements McpResource {
    
    @Override
    public String getUri() {
        return "file:///docs/readme.md";
    }
    
    @Override
    public String getName() {
        return "项目文档";
    }
    
    @Override
    public String getDescription() {
        return "Nebula框架的项目说明文档";
    }
    
    @Override
    public String getMimeType() {
        return "text/markdown";
    }
    
    @Override
    public String getContent() {
        return """
                # Nebula框架
                
                Nebula是一个基于Spring Boot 3.x和Java 21的企业级后端框架。
                
                ## 特性
                
                - 分层DDD架构
                - AI集成(聊天、向量嵌入、向量存储)
                - MCP(Model Context Protocol)支持
                - 数据持久化(MyBatis-Plus)
                - 缓存支持(Redis)
                - 消息队列(RabbitMQ)
                - 服务发现(Nacos)
                - 搜索功能(Elasticsearch)
                
                ## 快速开始
                
                1. 克隆项目
                2. 配置数据库和中间件
                3. 运行 mvn clean install
                4. 启动示例应用
                """;
    }
}


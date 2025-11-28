package io.nebula.mcp.core.config;

import io.nebula.mcp.core.search.DocumentSearchService;
import io.nebula.mcp.core.search.DefaultDocumentSearchService;
import io.nebula.mcp.core.indexer.DocumentIndexer;
import io.nebula.mcp.core.indexer.DefaultDocumentIndexer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * MCP Server 自动配置
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(McpProperties.class)
@ConditionalOnProperty(prefix = "nebula.mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
public class McpAutoConfiguration {
    
    /**
     * 文档搜索服务
     */
    @Bean
    @ConditionalOnMissingBean
    public DocumentSearchService documentSearchService(McpProperties properties) {
        log.info("创建 MCP 文档搜索服务");
        return new DefaultDocumentSearchService(properties);
    }
    
    /**
     * 文档索引器
     */
    @Bean
    @ConditionalOnMissingBean
    public DocumentIndexer documentIndexer(McpProperties properties) {
        log.info("创建 MCP 文档索引器");
        return new DefaultDocumentIndexer(properties);
    }
}


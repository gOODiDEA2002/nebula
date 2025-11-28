package io.nebula.mcp.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP Server 配置属性
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@ConfigurationProperties(prefix = "nebula.mcp")
public class McpProperties {
    
    /**
     * 是否启用 MCP Server
     */
    private boolean enabled = true;
    
    /**
     * MCP Server 名称
     */
    private String name = "nebula-mcp-server";
    
    /**
     * MCP Server 版本
     */
    private String version = "1.0.0";
    
    /**
     * 工具扫描包路径
     */
    private List<String> toolPackages = new ArrayList<>();
    
    /**
     * 搜索配置
     */
    private SearchConfig search = new SearchConfig();
    
    /**
     * 索引配置
     */
    private IndexingConfig indexing = new IndexingConfig();
    
    /**
     * 搜索配置
     */
    @Data
    public static class SearchConfig {
        /**
         * 是否启用缓存
         */
        private boolean cacheEnabled = true;
        
        /**
         * 缓存过期时间（分钟）
         */
        private int cacheTtlMinutes = 60;
        
        /**
         * 默认返回结果数量
         */
        private int defaultTopK = 5;
        
        /**
         * 相似度阈值
         */
        private float similarityThreshold = 0.5f;
    }
    
    /**
     * 索引配置
     */
    @Data
    public static class IndexingConfig {
        /**
         * 是否启用启动时自动索引
         */
        private boolean autoIndex = false;
        
        /**
         * 文档目录路径
         */
        private String docsPath;
        
        /**
         * 分块策略
         */
        private ChunkStrategy chunkStrategy = ChunkStrategy.HYBRID;
        
        /**
         * 最大分块大小
         */
        private int maxChunkSize = 1000;
        
        /**
         * 批处理大小
         */
        private int batchSize = 100;
    }
    
    /**
     * 文档分块策略
     */
    public enum ChunkStrategy {
        /**
         * 按段落分块
         */
        PARAGRAPH,
        
        /**
         * 按固定大小分块
         */
        FIXED_SIZE,
        
        /**
         * 混合策略（按标题 + 段落）
         */
        HYBRID
    }
}


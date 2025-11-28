package io.nebula.ai.spring.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG（检索增强生成）配置属性
 * 
 * 用于配置文档索引和检索的相关参数
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "nebula.ai.rag")
public class RagProperties {
    
    /**
     * 是否启用 RAG 功能
     */
    private boolean enabled = true;
    
    /**
     * 文档切片配置
     */
    private ChunkingConfig chunking = new ChunkingConfig();
    
    /**
     * 索引配置
     */
    private IndexingConfig indexing = new IndexingConfig();
    
    /**
     * 搜索配置
     */
    private SearchConfig search = new SearchConfig();
    
    // Getters and Setters
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public ChunkingConfig getChunking() {
        return chunking;
    }
    
    public void setChunking(ChunkingConfig chunking) {
        this.chunking = chunking;
    }
    
    public IndexingConfig getIndexing() {
        return indexing;
    }
    
    public void setIndexing(IndexingConfig indexing) {
        this.indexing = indexing;
    }
    
    public SearchConfig getSearch() {
        return search;
    }
    
    public void setSearch(SearchConfig search) {
        this.search = search;
    }
    
    /**
     * 文档切片配置
     */
    public static class ChunkingConfig {
        
        /**
         * 分块策略：HYBRID（混合）、SECTION（按章节）、FIXED（固定大小）
         */
        private String strategy = "HYBRID";
        
        /**
         * 最大块大小（字符数）
         * 
         * 说明：此值应根据 embedding 模型的上下文长度调整
         * - nomic-embed-text: 2048 tokens ≈ 8192 字符，建议 500-1000 字符
         * - bge-m3: 8192 tokens，建议 2000-4000 字符
         * - text-embedding-ada-002: 8191 tokens ≈ 32K 字符，建议 1000-2000 字符
         */
        private int maxChunkSize = 3000;
        
        /**
         * 块间重叠大小（字符数）
         * 
         * 说明：重叠可以避免重要信息在块边界丢失
         * 建议值为 maxChunkSize 的 10-20%
         */
        private int overlapSize = 200;
        
        // Getters and Setters
        
        public String getStrategy() {
            return strategy;
        }
        
        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }
        
        public int getMaxChunkSize() {
            return maxChunkSize;
        }
        
        public void setMaxChunkSize(int maxChunkSize) {
            this.maxChunkSize = maxChunkSize;
        }
        
        public int getOverlapSize() {
            return overlapSize;
        }
        
        public void setOverlapSize(int overlapSize) {
            this.overlapSize = overlapSize;
        }
    }
    
    /**
     * 索引配置
     */
    public static class IndexingConfig {
        
        /**
         * 批处理大小
         * 
         * 说明：每次向向量库添加的文档块数量
         */
        private int batchSize = 10;
        
        /**
         * 最大重试次数
         */
        private int maxRetryAttempts = 3;
        
        /**
         * 重试延迟（毫秒）
         */
        private long retryDelayMs = 1000;
        
        /**
         * 启动时自动索引文档
         */
        private boolean autoIndexOnStartup = false;
        
        // Getters and Setters
        
        public int getBatchSize() {
            return batchSize;
        }
        
        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
        
        public int getMaxRetryAttempts() {
            return maxRetryAttempts;
        }
        
        public void setMaxRetryAttempts(int maxRetryAttempts) {
            this.maxRetryAttempts = maxRetryAttempts;
        }
        
        public long getRetryDelayMs() {
            return retryDelayMs;
        }
        
        public void setRetryDelayMs(long retryDelayMs) {
            this.retryDelayMs = retryDelayMs;
        }
        
        public boolean isAutoIndexOnStartup() {
            return autoIndexOnStartup;
        }
        
        public void setAutoIndexOnStartup(boolean autoIndexOnStartup) {
            this.autoIndexOnStartup = autoIndexOnStartup;
        }
    }
    
    /**
     * 搜索配置
     */
    public static class SearchConfig {
        
        /**
         * 默认返回结果数量
         */
        private int defaultTopK = 5;
        
        /**
         * 最大返回结果数量
         */
        private int maxTopK = 20;
        
        /**
         * 相似度分数阈值（0.0-1.0）
         * 低于此阈值的搜索结果将被过滤
         */
        private double scoreThreshold = 0.4;
        
        /**
         * 是否按相似度分数排序
         */
        private boolean sortByScore = true;
        
        // Getters and Setters
        
        public int getDefaultTopK() {
            return defaultTopK;
        }
        
        public void setDefaultTopK(int defaultTopK) {
            this.defaultTopK = defaultTopK;
        }
        
        public int getMaxTopK() {
            return maxTopK;
        }
        
        public void setMaxTopK(int maxTopK) {
            this.maxTopK = maxTopK;
        }
        
        public double getScoreThreshold() {
            return scoreThreshold;
        }
        
        public void setScoreThreshold(double scoreThreshold) {
            this.scoreThreshold = scoreThreshold;
        }
        
        public boolean isSortByScore() {
            return sortByScore;
        }
        
        public void setSortByScore(boolean sortByScore) {
            this.sortByScore = sortByScore;
        }
    }
}




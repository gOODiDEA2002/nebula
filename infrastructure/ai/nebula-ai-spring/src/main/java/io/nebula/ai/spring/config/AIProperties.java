package io.nebula.ai.spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * AI模块配置属性
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "nebula.ai")
public class AIProperties {

    /**
     * 是否启用AI功能
     */
    private boolean enabled = true;

    /**
     * 聊天配置
     */
    private ChatProperties chat = new ChatProperties();

    /**
     * 嵌入配置
     */
    private EmbeddingProperties embedding = new EmbeddingProperties();

    /**
     * 向量存储配置
     */
    private VectorStoreProperties vectorStore = new VectorStoreProperties();

    /**
     * MCP配置
     */
    private McpProperties mcp = new McpProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ChatProperties getChat() {
        return chat;
    }

    public void setChat(ChatProperties chat) {
        this.chat = chat;
    }

    public EmbeddingProperties getEmbedding() {
        return embedding;
    }

    public void setEmbedding(EmbeddingProperties embedding) {
        this.embedding = embedding;
    }

    public VectorStoreProperties getVectorStore() {
        return vectorStore;
    }

    public void setVectorStore(VectorStoreProperties vectorStore) {
        this.vectorStore = vectorStore;
    }

    public McpProperties getMcp() {
        return mcp;
    }

    public void setMcp(McpProperties mcp) {
        this.mcp = mcp;
    }

    /**
     * 聊天配置
     */
    public static class ChatProperties {
        /**
         * 默认提供商
         */
        private String defaultProvider = "openai";

        /**
         * 提供商配置
         */
        private Map<String, ProviderProperties> providers = new HashMap<>();

        public String getDefaultProvider() {
            return defaultProvider;
        }

        public void setDefaultProvider(String defaultProvider) {
            this.defaultProvider = defaultProvider;
        }

        public Map<String, ProviderProperties> getProviders() {
            return providers;
        }

        public void setProviders(Map<String, ProviderProperties> providers) {
            this.providers = providers;
        }
    }

    /**
     * 嵌入配置
     */
    public static class EmbeddingProperties {
        /**
         * 默认提供商
         */
        private String defaultProvider = "openai";

        /**
         * 提供商配置
         */
        private Map<String, ProviderProperties> providers = new HashMap<>();

        public String getDefaultProvider() {
            return defaultProvider;
        }

        public void setDefaultProvider(String defaultProvider) {
            this.defaultProvider = defaultProvider;
        }

        public Map<String, ProviderProperties> getProviders() {
            return providers;
        }

        public void setProviders(Map<String, ProviderProperties> providers) {
            this.providers = providers;
        }
    }

    /**
     * 向量存储配置
     */
    public static class VectorStoreProperties {
        /**
         * 默认提供商
         */
        private String defaultProvider = "chroma";

        /**
         * 提供商配置
         */
        private Map<String, ProviderProperties> providers = new HashMap<>();

        public String getDefaultProvider() {
            return defaultProvider;
        }

        public void setDefaultProvider(String defaultProvider) {
            this.defaultProvider = defaultProvider;
        }

        public Map<String, ProviderProperties> getProviders() {
            return providers;
        }

        public void setProviders(Map<String, ProviderProperties> providers) {
            this.providers = providers;
        }
    }

    /**
     * 提供商配置
     */
    public static class ProviderProperties {
        /**
         * API密钥
         */
        private String apiKey;

        /**
         * 基础URL
         */
        private String baseUrl;

        /**
         * 默认模型
         */
        private String model;

        /**
         * 模型配置
         */
        private Map<String, String> models = new HashMap<>();

        /**
         * 选项配置
         */
        private Map<String, Object> options = new HashMap<>();

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Map<String, String> getModels() {
            return models;
        }

        public void setModels(Map<String, String> models) {
            this.models = models;
        }

        public Map<String, Object> getOptions() {
            return options;
        }

        public void setOptions(Map<String, Object> options) {
            this.options = options;
        }
    }
}


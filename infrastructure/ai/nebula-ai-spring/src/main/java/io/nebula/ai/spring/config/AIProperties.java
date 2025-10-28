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
    
    /**
     * OpenAI配置
     */
    private OpenAIProperties openai = new OpenAIProperties();

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
    
    public OpenAIProperties getOpenai() {
        return openai;
    }
    
    public void setOpenai(OpenAIProperties openai) {
        this.openai = openai;
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
        
        /**
         * Chroma 向量存储配置
         */
        private ChromaProperties chroma = new ChromaProperties();

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
        
        public ChromaProperties getChroma() {
            return chroma;
        }
        
        public void setChroma(ChromaProperties chroma) {
            this.chroma = chroma;
        }
    }
    
    /**
     * Chroma 向量存储特定配置
     */
    public static class ChromaProperties {
        /**
         * Chroma 服务器主机
         */
        private String host = "localhost";
        
        /**
         * Chroma 服务器端口
         */
        private int port = 8000;
        
        /**
         * 集合名称
         */
        private String collectionName = "nebula-collection";
        
        /**
         * 是否自动初始化 Schema
         */
        private boolean initializeSchema = true;
        
        /**
         * API 密钥（可选）
         */
        private String apiKey;
        
        public String getHost() {
            return host;
        }
        
        public void setHost(String host) {
            this.host = host;
        }
        
        public int getPort() {
            return port;
        }
        
        public void setPort(int port) {
            this.port = port;
        }
        
        public String getCollectionName() {
            return collectionName;
        }
        
        public void setCollectionName(String collectionName) {
            this.collectionName = collectionName;
        }
        
        public boolean isInitializeSchema() {
            return initializeSchema;
        }
        
        public void setInitializeSchema(boolean initializeSchema) {
            this.initializeSchema = initializeSchema;
        }
        
        public String getApiKey() {
            return apiKey;
        }
        
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
        
        /**
         * 获取完整的 Chroma URL
         */
        public String getUrl() {
            return String.format("http://%s:%d", host, port);
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
    
    /**
     * OpenAI 特定配置
     */
    public static class OpenAIProperties {
        /**
         * API 密钥
         */
        private String apiKey;
        
        /**
         * 基础 URL
         */
        private String baseUrl = "https://api.openai.com";
        
        /**
         * 聊天配置
         */
        private OpenAIChatProperties chat = new OpenAIChatProperties();
        
        /**
         * 嵌入配置
         */
        private OpenAIEmbeddingProperties embedding = new OpenAIEmbeddingProperties();
        
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
        
        public OpenAIChatProperties getChat() {
            return chat;
        }
        
        public void setChat(OpenAIChatProperties chat) {
            this.chat = chat;
        }
        
        public OpenAIEmbeddingProperties getEmbedding() {
            return embedding;
        }
        
        public void setEmbedding(OpenAIEmbeddingProperties embedding) {
            this.embedding = embedding;
        }
    }
    
    /**
     * OpenAI 聊天配置
     */
    public static class OpenAIChatProperties {
        /**
         * 是否启用
         */
        private boolean enabled = true;
        
        /**
         * 聊天选项
         */
        private OpenAIChatOptions options = new OpenAIChatOptions();
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public OpenAIChatOptions getOptions() {
            return options;
        }
        
        public void setOptions(OpenAIChatOptions options) {
            this.options = options;
        }
    }
    
    /**
     * OpenAI 聊天选项
     */
    public static class OpenAIChatOptions {
        /**
         * 模型名称
         */
        private String model = "gpt-3.5-turbo";
        
        /**
         * 温度参数
         */
        private Double temperature = 0.7;
        
        /**
         * 最大令牌数
         */
        private Integer maxTokens = 1000;
        
        public String getModel() {
            return model;
        }
        
        public void setModel(String model) {
            this.model = model;
        }
        
        public Double getTemperature() {
            return temperature;
        }
        
        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }
        
        public Integer getMaxTokens() {
            return maxTokens;
        }
        
        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }
    }
    
    /**
     * OpenAI 嵌入配置
     */
    public static class OpenAIEmbeddingProperties {
        /**
         * 是否启用
         */
        private boolean enabled = true;
        
        /**
         * 嵌入选项
         */
        private OpenAIEmbeddingOptions options = new OpenAIEmbeddingOptions();
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public OpenAIEmbeddingOptions getOptions() {
            return options;
        }
        
        public void setOptions(OpenAIEmbeddingOptions options) {
            this.options = options;
        }
    }
    
    /**
     * OpenAI 嵌入选项
     */
    public static class OpenAIEmbeddingOptions {
        /**
         * 模型名称
         */
        private String model = "text-embedding-ada-002";
        
        public String getModel() {
            return model;
        }
        
        public void setModel(String model) {
            this.model = model;
        }
    }
}


package io.nebula.autoconfigure.ai;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.core.embedding.EmbeddingService;
import io.nebula.ai.core.mcp.McpServerService;
import io.nebula.ai.core.mcp.McpClientService;
import io.nebula.ai.core.vectorstore.VectorStoreService;
import io.nebula.ai.spring.chat.SpringAIChatService;
import io.nebula.ai.spring.config.AIProperties;
import io.nebula.ai.spring.config.McpProperties;
import io.nebula.ai.spring.embedding.SpringAIEmbeddingService;
import io.nebula.ai.spring.mcp.SpringAIMcpServerService;
import io.nebula.ai.spring.mcp.SpringAIMcpClientService;
import io.nebula.ai.spring.vectorstore.SpringAIVectorStoreService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.retry.RetryUtils;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Nebula AI 自动配置类
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ChatClient.class, ChatModel.class})
@ConditionalOnProperty(prefix = "nebula.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({AIProperties.class, io.nebula.ai.spring.config.VectorStoreProperties.class})
public class AIAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AIAutoConfiguration.class);

    public AIAutoConfiguration() {
        log.info("Nebula AI 模块自动配置已启用");
    }
    
    /**
     * 配置 RestClient.Builder
     * 为 ChromaApi 和 OllamaApi 提供 RestClient.Builder Bean
     * 添加 HTTP 日志拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    public RestClient.Builder builder() {
        log.info("配置 RestClient.Builder (带HTTP日志拦截器)");
        
        // 添加 HTTP 日志拦截器
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        
        // 不使用 BufferingClientHttpRequestFactory，直接使用默认的 SimpleClientHttpRequestFactory
        // BufferingClientHttpRequestFactory 可能导致与某些服务器的兼容性问题
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(java.time.Duration.ofSeconds(10));
        requestFactory.setReadTimeout(java.time.Duration.ofSeconds(120));
        
        return RestClient.builder()
                .requestFactory(requestFactory)
                .requestInterceptor(loggingInterceptor);
    }
    
    /**
     * 配置 OpenAiApi
     * 基于 Nebula 配置创建 OpenAiApi Bean
     */
    @Bean("nebulaOpenAiApi")
    @Primary
    @ConditionalOnClass(OpenAiApi.class)
    @ConditionalOnMissingBean(name = "nebulaOpenAiApi")
    @ConditionalOnProperty(prefix = "nebula.ai.openai", name = "api-key")
    public OpenAiApi nebulaOpenAiApi(AIProperties aiProperties) {
        AIProperties.OpenAIProperties openAIConfig = aiProperties.getOpenai();
        
        log.info("配置 OpenAiApi, Base URL: {}", openAIConfig.getBaseUrl());
        
        return OpenAiApi.builder()
                .apiKey(openAIConfig.getApiKey())
                .baseUrl(openAIConfig.getBaseUrl())
                .build();
    }
    
    /**
     * 配置 OpenAI ChatModel
     * 基于 Nebula 配置创建 ChatModel Bean
     */
    @Bean("nebulaOpenAiChatModel")
    @Primary
    @ConditionalOnClass(OpenAiChatModel.class)
    @ConditionalOnMissingBean(name = "nebulaOpenAiChatModel")
    @ConditionalOnProperty(prefix = "nebula.ai.openai.chat", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ChatModel nebulaOpenAiChatModel(OpenAiApi nebulaOpenAiApi, AIProperties aiProperties) {
        AIProperties.OpenAIProperties openAIConfig = aiProperties.getOpenai();
        AIProperties.OpenAIChatOptions chatOptions = openAIConfig.getChat().getOptions();
        
        log.info("配置 OpenAI ChatModel, Model: {}, Temperature: {}, MaxTokens: {}", 
                chatOptions.getModel(), chatOptions.getTemperature(), chatOptions.getMaxTokens());
        
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(chatOptions.getModel())
                .temperature(chatOptions.getTemperature())
                .maxTokens(chatOptions.getMaxTokens())
                .build();
        
        return OpenAiChatModel.builder()
                .openAiApi(nebulaOpenAiApi)
                .defaultOptions(options)
                .build();
    }
    
    /**
     * 配置 OpenAI EmbeddingModel
     * 基于 Nebula 配置创建 EmbeddingModel Bean
     */
    @Bean("nebulaOpenAiEmbeddingModel")
    @ConditionalOnClass(OpenAiEmbeddingModel.class)
    @ConditionalOnMissingBean(name = "nebulaOpenAiEmbeddingModel")
    @ConditionalOnProperty(prefix = "nebula.ai.openai.embedding", name = "enabled", havingValue = "true", matchIfMissing = false)
    public EmbeddingModel nebulaOpenAiEmbeddingModel(OpenAiApi nebulaOpenAiApi, AIProperties aiProperties) {
        AIProperties.OpenAIProperties openAIConfig = aiProperties.getOpenai();
        AIProperties.OpenAIEmbeddingOptions embeddingOptions = openAIConfig.getEmbedding().getOptions();
        
        log.info("配置 OpenAI EmbeddingModel, Model: {}", embeddingOptions.getModel());
        
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(embeddingOptions.getModel())
                .build();
        
        return new OpenAiEmbeddingModel(nebulaOpenAiApi, MetadataMode.EMBED, options, RetryUtils.DEFAULT_RETRY_TEMPLATE);
    }
    
    /**
     * 配置 OllamaApi
     * 基于 Nebula 配置创建 OllamaApi Bean
     */
    @Bean("nebulaOllamaApi")
    @ConditionalOnClass(OllamaApi.class)
    @ConditionalOnMissingBean(name = "nebulaOllamaApi")
    @ConditionalOnProperty(prefix = "nebula.ai.ollama", name = "base-url")
    public OllamaApi nebulaOllamaApi(AIProperties aiProperties, RestClient.Builder restClientBuilder) {
        AIProperties.OllamaProperties ollamaConfig = aiProperties.getOllama();
        
        log.info("配置 OllamaApi, Base URL: {}", ollamaConfig.getBaseUrl());
        log.info("- 读取超时: {}", ollamaConfig.getTimeout().getRead());
        log.info("- 连接超时: {}", ollamaConfig.getTimeout().getConnect());
        
        // 使用自定义的RestClient.Builder（带HTTP日志拦截器）
        // Spring AI 1.0.3的OllamaApi.builder()支持restClientBuilder()方法
        return OllamaApi.builder()
                .baseUrl(ollamaConfig.getBaseUrl())
                .restClientBuilder(restClientBuilder)
                .build();
    }
    
    /**
     * 配置 Ollama EmbeddingModel
     * 基于 Nebula 配置创建 EmbeddingModel Bean
     */
    @Bean("nebulaOllamaEmbeddingModel")
    @Primary
    @ConditionalOnClass(OllamaEmbeddingModel.class)
    @ConditionalOnMissingBean(name = "nebulaOllamaEmbeddingModel")
    @ConditionalOnProperty(prefix = "nebula.ai.ollama.embedding", name = "enabled", havingValue = "true", matchIfMissing = true)
    public EmbeddingModel nebulaOllamaEmbeddingModel(
            OllamaApi nebulaOllamaApi, 
            AIProperties aiProperties) {
        AIProperties.OllamaProperties ollamaConfig = aiProperties.getOllama();
        AIProperties.OllamaEmbeddingOptions embeddingOptions = ollamaConfig.getEmbedding().getOptions();
        
        log.info("配置 Ollama EmbeddingModel, Model: {}", embeddingOptions.getModel());
        
        // Spring AI 1.1.0 使用 OllamaEmbeddingOptions
        OllamaEmbeddingOptions options = OllamaEmbeddingOptions.builder()
                .model(embeddingOptions.getModel())
                .build();
        
        // Spring AI 1.1.0 使用 Builder 模式
        return OllamaEmbeddingModel.builder()
                .ollamaApi(nebulaOllamaApi)
                .defaultOptions(options)
                .build();
    }

    
    /**
     * 配置 ChromaApi
     * 基于 Nebula 配置创建 ChromaApi Bean
     */
    @Bean("nebulaChromaApi")
    @Primary
    @ConditionalOnClass(ChromaApi.class)
    @ConditionalOnMissingBean(name = "nebulaChromaApi")
    public ChromaApi nebulaChromaApi(RestClient.Builder builder, ObjectMapper objectMapper, AIProperties aiProperties) {
        AIProperties.ChromaProperties chromaConfig = aiProperties.getVectorStore().getChroma();
        String chromaUrl = chromaConfig.getUrl();
        
        log.info("配置 ChromaApi, URL: {}, Collection: {}, InitializeSchema: {}", 
                chromaUrl, chromaConfig.getCollectionName(), chromaConfig.isInitializeSchema());
        
        return new ChromaApi(chromaUrl, builder, objectMapper);
    }
    
    /**
     * 配置 ChromaVectorStore
     * 基于 Nebula 配置创建 ChromaVectorStore Bean
     */
    @Bean("nebulaChromaVectorStore")
    @Primary
    @ConditionalOnClass({ChromaVectorStore.class, ChromaApi.class})
    @ConditionalOnMissingBean(name = "nebulaChromaVectorStore")
    public VectorStore nebulaChromaVectorStore(ChromaApi nebulaChromaApi, EmbeddingModel embeddingModel, AIProperties aiProperties) {
        AIProperties.ChromaProperties chromaConfig = aiProperties.getVectorStore().getChroma();
        
        log.info("配置 CustomChromaVectorStore, Collection: {}, InitializeSchema: {}", 
                chromaConfig.getCollectionName(), chromaConfig.isInitializeSchema());
        log.info("注入的 EmbeddingModel 类型: {}", embeddingModel.getClass().getName());
        
        // 使用自定义实现绕过JSON解析兼容性问题
        return new CustomChromaVectorStore(
                nebulaChromaApi,
                embeddingModel,
                chromaConfig.getCollectionName(),
                chromaConfig.isInitializeSchema()
        );
    }

    /**
     * 配置聊天服务
     */
    @Bean
    @ConditionalOnClass(ChatModel.class)
    @ConditionalOnMissingBean(ChatService.class)
    public ChatService chatService(ChatClient.Builder chatClientBuilder, ChatModel chatModel) {
        log.info("配置 Nebula ChatService");
        return new SpringAIChatService(chatClientBuilder, chatModel);
    }

    /**
     * 配置嵌入服务
     */
    @Bean
    @ConditionalOnClass(EmbeddingModel.class)
    @ConditionalOnMissingBean(EmbeddingService.class)
    public EmbeddingService embeddingService(EmbeddingModel embeddingModel) {
        log.info("配置 Nebula EmbeddingService");
        return new SpringAIEmbeddingService(embeddingModel);
    }

    /**
     * 配置向量存储服务
     */
    @Bean
    @ConditionalOnClass(VectorStore.class)
    @ConditionalOnMissingBean(VectorStoreService.class)
    public VectorStoreService vectorStoreService(
            VectorStore vectorStore, 
            EmbeddingService embeddingService,
            io.nebula.ai.spring.config.VectorStoreProperties properties) {
        log.info("配置 Nebula VectorStoreService - 批处理: {}, 重试: {}", 
                properties.isBatchingEnabled(), properties.isRetryEnabled());
        return new SpringAIVectorStoreService(vectorStore, embeddingService, properties);
    }

    /**
     * 配置MCP服务器服务
     */
    @Bean
    @ConditionalOnProperty(prefix = "nebula.ai.mcp.server", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(McpServerService.class)
    public McpServerService mcpServerService(AIProperties aiProperties) {
        log.info("配置 Nebula McpServerService");
        return new SpringAIMcpServerService(aiProperties.getMcp().getServer());
    }

    /**
     * 配置MCP客户端服务
     */
    @Bean
    @ConditionalOnProperty(prefix = "nebula.ai.mcp.client", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(McpClientService.class)
    public McpClientService mcpClientService(AIProperties aiProperties) {
        log.info("配置 Nebula McpClientService");
        return new SpringAIMcpClientService(aiProperties.getMcp().getClient());
    }
}


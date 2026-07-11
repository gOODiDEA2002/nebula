package io.nebula.autoconfigure.ai;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.core.common.diagnostic.NebulaComponentSummary;
import io.nebula.core.common.diagnostic.SimpleComponentSummary;
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
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

/**
 * Nebula AI 自动配置类
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ ChatClient.class, ChatModel.class })
@ConditionalOnProperty(prefix = "nebula.ai", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties({ AIProperties.class, io.nebula.ai.spring.config.VectorStoreProperties.class })
public class AIAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AIAutoConfiguration.class);

    public AIAutoConfiguration() {
        log.info("Nebula AI 模块自动配置已启用");
    }

    /**
     * 配置 OpenAI ChatModel
     *
     * Spring AI 2.0: OpenAiApi 已移除，apiKey/baseUrl 通过 OpenAiChatOptions 传入，
     * 模型内部自动构建 OpenAIClient (基于官方 SDK)
     */
    @Bean("nebulaOpenAiChatModel")
    @Primary
    @ConditionalOnClass(OpenAiChatModel.class)
    @ConditionalOnMissingBean(name = "nebulaOpenAiChatModel")
    @ConditionalOnProperty(prefix = "nebula.ai.openai", name = "api-key")
    public ChatModel nebulaOpenAiChatModel(AIProperties aiProperties) {
        AIProperties.OpenAIProperties openAIConfig = aiProperties.getOpenai();
        AIProperties.OpenAIChatOptions chatOpts = openAIConfig.getChat().getOptions();

        log.info("配置 OpenAI ChatModel, Base URL: {}, Model: {}, Temperature: {}, MaxTokens: {}",
                openAIConfig.getBaseUrl(), chatOpts.getModel(), chatOpts.getTemperature(), chatOpts.getMaxTokens());

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .apiKey(openAIConfig.getApiKey())
                .baseUrl(openAIConfig.getBaseUrl())
                .model(chatOpts.getModel())
                .temperature(chatOpts.getTemperature())
                .maxTokens(chatOpts.getMaxTokens())
                .maxRetries(chatOpts.getMaxRetries())
                .build();

        return OpenAiChatModel.builder()
                .options(options)
                .build();
    }

    /**
     * 配置 OpenAI EmbeddingModel
     *
     * Spring AI 2.0: 同 ChatModel，apiKey/baseUrl 通过 Options 传入
     */
    @Bean("nebulaOpenAiEmbeddingModel")
    @Primary
    @ConditionalOnClass(OpenAiEmbeddingModel.class)
    @ConditionalOnMissingBean(name = "nebulaOpenAiEmbeddingModel")
    @ConditionalOnProperty(prefix = "nebula.ai.openai", name = "api-key")
    public EmbeddingModel nebulaOpenAiEmbeddingModel(AIProperties aiProperties) {
        AIProperties.OpenAIProperties openAIConfig = aiProperties.getOpenai();
        AIProperties.OpenAIEmbeddingOptions embOpts = openAIConfig.getEmbedding().getOptions();

        log.info("配置 OpenAI EmbeddingModel, Model: {}", embOpts.getModel());

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .apiKey(openAIConfig.getApiKey())
                .baseUrl(openAIConfig.getBaseUrl())
                .model(embOpts.getModel())
                .encodingFormat(OpenAiEmbeddingOptions.EncodingFormat.FLOAT)
                .maxRetries(embOpts.getMaxRetries())
                .build();

        return OpenAiEmbeddingModel.builder()
                .options(options)
                .build();
    }

    /**
     * 配置 ChromaApi
     *
     * Spring AI 2.0: 使用 builder 模式，JsonMapper 由框架自动提供
     */
    @Bean("nebulaChromaApi")
    @Primary
    @ConditionalOnClass(ChromaApi.class)
    @ConditionalOnMissingBean(name = "nebulaChromaApi")
    public ChromaApi nebulaChromaApi(AIProperties aiProperties) {
        AIProperties.ChromaProperties chromaConfig = aiProperties.getVectorStore().getChroma();
        String chromaUrl = chromaConfig.getUrl();

        log.info("配置 ChromaApi, URL: {}, Collection: {}, InitializeSchema: {}",
                chromaUrl, chromaConfig.getCollectionName(), chromaConfig.isInitializeSchema());

        return ChromaApi.builder()
                .baseUrl(chromaUrl)
                .build();
    }

    /**
     * 配置 ChromaVectorStore
     *
     * Spring AI 2.0: 使用官方 ChromaVectorStore.builder 替代自定义实现
     */
    @Bean("nebulaChromaVectorStore")
    @Primary
    @ConditionalOnClass({ ChromaVectorStore.class, ChromaApi.class })
    @ConditionalOnMissingBean(name = "nebulaChromaVectorStore")
    public VectorStore nebulaChromaVectorStore(ChromaApi nebulaChromaApi, EmbeddingModel embeddingModel,
            AIProperties aiProperties) {
        AIProperties.ChromaProperties chromaConfig = aiProperties.getVectorStore().getChroma();

        log.info("配置 ChromaVectorStore, Collection: {}, InitializeSchema: {}",
                chromaConfig.getCollectionName(), chromaConfig.isInitializeSchema());

        return ChromaVectorStore.builder(nebulaChromaApi, embeddingModel)
                .collectionName(chromaConfig.getCollectionName())
                .initializeSchema(chromaConfig.isInitializeSchema())
                .build();
    }

    /**
     * 配置聊天服务
     */
    @Bean
    @ConditionalOnClass(ChatModel.class)
    @ConditionalOnMissingBean(ChatService.class)
    public ChatService chatService(ChatClient.Builder chatClientBuilder, ChatModel chatModel,
                                   AIProperties aiProperties) {
        log.info("配置 Nebula ChatService");
        return new SpringAIChatService(chatClientBuilder, chatModel, aiProperties);
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

    /**
     * 组件摘要: AI
     */
    @Bean
    NebulaComponentSummary aiSummary(AIProperties aiProperties) {
        var details = new java.util.LinkedHashMap<String, String>();

        // Chat
        details.put("Chat Provider", aiProperties.getChat().getDefaultProvider());

        // OpenAI Specific
        var openai = aiProperties.getOpenai();
        if (openai != null) {
            if (openai.getBaseUrl() != null) {
                details.put("OpenAI URL", openai.getBaseUrl());
            }
            if (openai.getChat() != null && openai.getChat().getOptions() != null) {
                details.put("Chat Model", openai.getChat().getOptions().getModel());
            }
            if (openai.getEmbedding() != null && openai.getEmbedding().getOptions() != null) {
                details.put("Embedding Model", openai.getEmbedding().getOptions().getModel());
            }
        }

        // Vector Store
        details.put("Vector Store", aiProperties.getVectorStore().getDefaultProvider());
        if ("chroma".equalsIgnoreCase(aiProperties.getVectorStore().getDefaultProvider())) {
            var chroma = aiProperties.getVectorStore().getChroma();
            details.put("Chroma Host", chroma.getHost() + ":" + chroma.getPort());
            details.put("Collection", chroma.getCollectionName());
        }

        return new SimpleComponentSummary("AI", "Spring AI", true, 900, details);
    }
}

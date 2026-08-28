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
import io.nebula.ai.spring.vectorstore.QdrantIdMappingVectorStore;
import io.nebula.ai.spring.vectorstore.QdrantPointIdMapper;
import io.nebula.ai.spring.vectorstore.SpringAIVectorStoreService;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
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

import java.time.Duration;

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
     *
     * 2.1.1 起补加 provider 条件（默认 chroma，行为不变）：多后端共存时同类型
     * @Primary Bean 只能有一个，切到 qdrant 后 Chroma 这两个 Bean 必须让位。
     */
    @Bean("nebulaChromaApi")
    @Primary
    @ConditionalOnClass(ChromaApi.class)
    @ConditionalOnMissingBean(name = "nebulaChromaApi")
    @ConditionalOnProperty(prefix = "nebula.ai.vector-store", name = "default-provider",
            havingValue = "chroma", matchIfMissing = true)
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
    @ConditionalOnProperty(prefix = "nebula.ai.vector-store", name = "default-provider",
            havingValue = "chroma", matchIfMissing = true)
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
     * 配置 QdrantClient
     *
     * 声明 destroyMethod：gRPC 通道属于长期资源，随 Spring 上下文关闭才不会泄漏。
     */
    @Bean(name = "nebulaQdrantClient", destroyMethod = "close")
    @ConditionalOnClass(QdrantVectorStore.class)
    @ConditionalOnMissingBean(name = "nebulaQdrantClient")
    @ConditionalOnProperty(prefix = "nebula.ai.vector-store", name = "default-provider",
            havingValue = "qdrant")
    public QdrantClient nebulaQdrantClient(AIProperties aiProperties) {
        AIProperties.QdrantProperties qdrantConfig = aiProperties.getVectorStore().getQdrant();

        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(
                qdrantConfig.getHost(), qdrantConfig.getPort(), qdrantConfig.isUseTls());
        builder.withTimeout(Duration.ofSeconds(qdrantConfig.getTimeoutSeconds()));

        if (qdrantConfig.getApiKey() != null && !qdrantConfig.getApiKey().isBlank()) {
            builder.withApiKey(qdrantConfig.getApiKey());
            log.info("配置 QdrantClient, endpoint={}:{}, tls={}, 已启用 api-key 鉴权",
                    qdrantConfig.getHost(), qdrantConfig.getPort(), qdrantConfig.isUseTls());
        } else {
            log.warn("配置 QdrantClient, endpoint={}:{}, tls={}, 未配置 api-key: "
                            + "若 Qdrant 开启鉴权将导致检索与写入全部失败",
                    qdrantConfig.getHost(), qdrantConfig.getPort(), qdrantConfig.isUseTls());
        }
        return new QdrantClient(builder.build());
    }

    /**
     * 配置 QdrantVectorStore
     *
     * 开启 id-mapping 时对外的是 {@link QdrantIdMappingVectorStore} 装饰器而不是裸的
     * QdrantVectorStore：Qdrant 点 ID 只收 UUID 或无符号整数，命名空间字符串形态的
     * docId 裸接会在写入时逐条抛 Invalid UUID string。
     */
    @Bean("nebulaQdrantVectorStore")
    @Primary
    @ConditionalOnClass(QdrantVectorStore.class)
    @ConditionalOnMissingBean(name = "nebulaQdrantVectorStore")
    @ConditionalOnProperty(prefix = "nebula.ai.vector-store", name = "default-provider",
            havingValue = "qdrant")
    public VectorStore nebulaQdrantVectorStore(QdrantClient nebulaQdrantClient,
            EmbeddingModel embeddingModel, AIProperties aiProperties) {
        AIProperties.QdrantProperties qdrantConfig = aiProperties.getVectorStore().getQdrant();

        log.info("配置 QdrantVectorStore, Collection: {}, InitializeSchema: {}",
                qdrantConfig.getCollectionName(), qdrantConfig.isInitializeSchema());

        VectorStore qdrantVectorStore = QdrantVectorStore.builder(nebulaQdrantClient, embeddingModel)
                .collectionName(qdrantConfig.getCollectionName())
                .initializeSchema(qdrantConfig.isInitializeSchema())
                .build();

        AIProperties.QdrantProperties.IdMapping idMapping = qdrantConfig.getIdMapping();
        if (!idMapping.isEnabled()) {
            return qdrantVectorStore;
        }

        // 命名空间缺失时直接启动失败：错配会产生一个全新的命名空间，
        // 表现为写入成功、全库检索永远为空，比启动不来难查得多
        if (idMapping.getNamespaceName() == null || idMapping.getNamespaceName().isBlank()) {
            throw new IllegalStateException(
                    "nebula.ai.vector-store.qdrant.id-mapping.enabled=true 时必须配置 "
                            + "nebula.ai.vector-store.qdrant.id-mapping.namespace-name; "
                            + "命名空间决定全库点 ID, 缺失或改动会让已灌入的数据全部检索不到");
        }
        QdrantPointIdMapper pointIdMapper = new QdrantPointIdMapper(idMapping.getNamespaceName());
        log.info("Qdrant 点 ID 映射已启用, namespaceName={}, namespace={}, payload 字段={}",
                pointIdMapper.getNamespaceName(), pointIdMapper.getNamespace(),
                idMapping.getOriginalDocIdField());
        return new QdrantIdMappingVectorStore(qdrantVectorStore, pointIdMapper,
                idMapping.getOriginalDocIdField());
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
        } else if ("qdrant".equalsIgnoreCase(aiProperties.getVectorStore().getDefaultProvider())) {
            var qdrant = aiProperties.getVectorStore().getQdrant();
            details.put("Qdrant Host", qdrant.getHost() + ":" + qdrant.getPort());
            details.put("Collection", qdrant.getCollectionName());
            details.put("Id Mapping", String.valueOf(qdrant.getIdMapping().isEnabled()));
            if (qdrant.getIdMapping().isEnabled() && qdrant.getIdMapping().getNamespaceName() != null) {
                details.put("Id Mapping Namespace", qdrant.getIdMapping().getNamespaceName());
            }
        }

        return new SimpleComponentSummary("AI", "Spring AI", true, 900, details);
    }
}

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
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Nebula AI 自动配置类
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ChatClient.class, ChatModel.class})
@ConditionalOnProperty(prefix = "nebula.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AIProperties.class)
public class AIAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AIAutoConfiguration.class);

    public AIAutoConfiguration() {
        log.info("Nebula AI 模块自动配置已启用");
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
    public VectorStoreService vectorStoreService(VectorStore vectorStore, 
                                                EmbeddingService embeddingService) {
        log.info("配置 Nebula VectorStoreService");
        return new SpringAIVectorStoreService(vectorStore, embeddingService);
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


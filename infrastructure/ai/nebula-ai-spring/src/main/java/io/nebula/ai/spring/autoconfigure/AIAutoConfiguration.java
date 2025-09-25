package io.nebula.ai.spring.autoconfigure;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.core.embedding.EmbeddingService;
import io.nebula.ai.core.vectorstore.VectorStoreService;
import io.nebula.ai.spring.chat.SpringAIChatService;
import io.nebula.ai.spring.embedding.SpringAIEmbeddingService;
import io.nebula.ai.spring.vectorstore.SpringAIVectorStoreService;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AI模块自动配置
 */
@AutoConfiguration
@ConditionalOnClass({ChatModel.class, EmbeddingModel.class, VectorStore.class})
@ConditionalOnProperty(prefix = "nebula.ai", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(AIProperties.class)
public class AIAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AIAutoConfiguration.class);

    private final AIProperties properties;

    public AIAutoConfiguration(AIProperties properties) {
        this.properties = properties;
        log.info("Nebula AI 模块自动配置已启用");
    }

    /**
     * 聊天服务Bean
     */
    @Bean
    @ConditionalOnMissingBean(ChatService.class)
    @ConditionalOnClass({ChatClient.class, ChatModel.class})
    public SpringAIChatService nebulaAIChatService(ChatClient.Builder chatClientBuilder, ChatModel chatModel) {
        log.info("配置 Nebula AI 聊天服务");
        return new SpringAIChatService(chatClientBuilder, chatModel);
    }

    /**
     * 嵌入服务Bean
     */
    @Bean
    @ConditionalOnMissingBean(EmbeddingService.class)
    @ConditionalOnClass(EmbeddingModel.class)
    public SpringAIEmbeddingService nebulaAIEmbeddingService(EmbeddingModel embeddingModel) {
        log.info("配置 Nebula AI 嵌入服务");
        return new SpringAIEmbeddingService(embeddingModel);
    }

    /**
     * 向量存储服务Bean
     */
    @Bean
    @ConditionalOnMissingBean(VectorStoreService.class)
    @ConditionalOnClass(VectorStore.class)
    public SpringAIVectorStoreService nebulaAIVectorStoreService(VectorStore vectorStore, EmbeddingService embeddingService) {
        log.info("配置 Nebula AI 向量存储服务");
        return new SpringAIVectorStoreService(vectorStore, embeddingService);
    }
}

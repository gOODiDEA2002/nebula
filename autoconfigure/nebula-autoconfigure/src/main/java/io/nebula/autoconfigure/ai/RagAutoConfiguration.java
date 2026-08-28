package io.nebula.autoconfigure.ai;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.core.vectorstore.VectorStoreService;
import io.nebula.ai.rag.chunking.TextChunker;
import io.nebula.ai.rag.config.RagProperties;
import io.nebula.ai.rag.fusion.FusionStrategy;
import io.nebula.ai.rag.fusion.RrfFusionStrategy;
import io.nebula.ai.rag.pipeline.AnswerGenerator;
import io.nebula.ai.rag.pipeline.ChatServiceAnswerGenerator;
import io.nebula.ai.rag.pipeline.ContextAssembler;
import io.nebula.ai.rag.pipeline.DefaultRagPipeline;
import io.nebula.ai.rag.pipeline.DefaultRagPromptRenderer;
import io.nebula.ai.rag.pipeline.HybridRetrievalEngine;
import io.nebula.ai.rag.pipeline.RagPipeline;
import io.nebula.ai.rag.pipeline.RagPromptRenderer;
import io.nebula.ai.rag.rerank.NoopReranker;
import io.nebula.ai.rag.rerank.Reranker;
import io.nebula.ai.rag.retriever.Retriever;
import io.nebula.ai.rag.retriever.VectorStoreRetriever;
import io.nebula.core.common.diagnostic.NebulaComponentSummary;
import io.nebula.core.common.diagnostic.SimpleComponentSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Nebula RAG 自动配置类
 * <p>
 * Level 3 默认关闭（{@code nebula.ai.rag.enabled=true} 才启动）：RAG 依赖向量库等
 * 外部服务，引入 JAR 不该造成任何远程连接。starter-ai 也不预置开启。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
@AutoConfiguration(after = AIAutoConfiguration.class)
@ConditionalOnClass({ RagPipeline.class, Retriever.class })
@ConditionalOnProperty(prefix = "nebula.ai.rag", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(RagProperties.class)
public class RagAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RagAutoConfiguration.class);

    public RagAutoConfiguration() {
        log.info("Nebula AI RAG 模块自动配置已启用");
    }

    /**
     * 基于向量存储的默认检索器
     * <p>
     * 权重固定为 1.0：权重是检索器自身的特性，多路检索场景下由应用提供
     * 各自带权重的 Retriever Bean 覆盖或补充，框架不为此额外开一组配置项。
     */
    @Bean
    @ConditionalOnBean(VectorStoreService.class)
    @ConditionalOnMissingBean(VectorStoreRetriever.class)
    public VectorStoreRetriever vectorStoreRetriever(VectorStoreService vectorStoreService) {
        log.info("配置 Nebula VectorStoreRetriever");
        return new VectorStoreRetriever(vectorStoreService, 1.0, 0.0);
    }

    /**
     * RRF 融合策略
     */
    @Bean
    @ConditionalOnMissingBean(FusionStrategy.class)
    public FusionStrategy rrfFusionStrategy(RagProperties properties) {
        RagProperties.Fusion fusion = properties.getFusion();
        log.info("配置 Nebula RrfFusionStrategy - rrfK: {}, sourcePriority: {}",
                fusion.getRrfK(), fusion.getSourcePriority());
        return new RrfFusionStrategy(fusion.getRrfK(), fusion.getSourcePriority());
    }

    /**
     * 多路并行检索引擎
     * <p>
     * 容器里一个 {@link Retriever} 都没有时直接启动失败：这种情况下管线只会永远返回
     * 「没找到相关内容」，是配置事故而不是可用状态，藏到运行期才暴露代价更大。
     */
    @Bean
    @ConditionalOnMissingBean(HybridRetrievalEngine.class)
    public HybridRetrievalEngine hybridRetrievalEngine(ObjectProvider<Retriever> retrievers,
                                                       FusionStrategy fusionStrategy,
                                                       RagProperties properties) {
        List<Retriever> ordered = retrievers.orderedStream().toList();
        if (ordered.isEmpty()) {
            throw new IllegalStateException(
                    "nebula.ai.rag.enabled=true 但容器内没有任何 Retriever Bean; "
                            + "请提供至少一个 io.nebula.ai.rag.retriever.Retriever 实现, "
                            + "或启用 VectorStoreService 以装配默认的 VectorStoreRetriever");
        }
        RagProperties.Retrieval retrieval = properties.getRetrieval();
        log.info("配置 Nebula HybridRetrievalEngine - 检索器: {}, 候选放大: {}x, 默认超时: {}s",
                ordered.stream().map(Retriever::getName).toList(),
                retrieval.getCandidateMultiplier(), retrieval.getTimeoutSeconds());
        return new HybridRetrievalEngine(ordered, fusionStrategy,
                retrieval.getCandidateMultiplier(), retrieval.getTimeoutSeconds() * 1000L);
    }

    /**
     * 默认重排序器：只截断不排序
     */
    @Bean
    @ConditionalOnMissingBean(Reranker.class)
    public Reranker noopReranker() {
        log.info("配置 Nebula NoopReranker (未提供 Reranker 实现, 仅按融合顺序截断)");
        return new NoopReranker();
    }

    /**
     * 上下文拼接器
     */
    @Bean
    @ConditionalOnMissingBean(ContextAssembler.class)
    public ContextAssembler contextAssembler(RagProperties properties) {
        RagProperties.Context context = properties.getContext();
        log.info("配置 Nebula ContextAssembler - 上下文预算: {} 字符", context.getMaxLength());
        return new ContextAssembler(context.getMaxLength(), context.getDocumentTemplate());
    }

    /**
     * 默认提示词渲染器
     */
    @Bean
    @ConditionalOnMissingBean(RagPromptRenderer.class)
    public RagPromptRenderer ragPromptRenderer() {
        return new DefaultRagPromptRenderer();
    }

    /**
     * 默认答案生成器：走 ChatService
     */
    @Bean
    @ConditionalOnBean(ChatService.class)
    @ConditionalOnMissingBean(AnswerGenerator.class)
    public AnswerGenerator defaultAnswerGenerator(ChatService chatService) {
        log.info("配置 Nebula ChatServiceAnswerGenerator");
        return new ChatServiceAnswerGenerator(chatService);
    }

    /**
     * RAG 管线
     */
    @Bean
    @ConditionalOnBean({ HybridRetrievalEngine.class, AnswerGenerator.class })
    @ConditionalOnMissingBean(RagPipeline.class)
    public RagPipeline ragPipeline(HybridRetrievalEngine hybridRetrievalEngine,
                                   Reranker reranker,
                                   ContextAssembler contextAssembler,
                                   RagPromptRenderer ragPromptRenderer,
                                   AnswerGenerator answerGenerator,
                                   RagProperties properties) {
        log.info("配置 Nebula RagPipeline - 重排: {}, 生成超时: {}ms",
                properties.getRerank().isEnabled(), properties.getGeneration().getTimeoutMs());
        return new DefaultRagPipeline(hybridRetrievalEngine, reranker, contextAssembler,
                ragPromptRenderer, answerGenerator, properties);
    }

    /**
     * 文本分块器
     */
    @Bean
    @ConditionalOnMissingBean(TextChunker.class)
    public TextChunker textChunker(RagProperties properties) {
        RagProperties.Chunking chunking = properties.getChunking();
        return new TextChunker(chunking.getSize(), chunking.getOverlap());
    }

    /**
     * 组件摘要: RAG
     */
    @Bean
    NebulaComponentSummary ragSummary(RagProperties properties,
                                      ObjectProvider<Retriever> retrievers) {
        var details = new java.util.LinkedHashMap<String, String>();
        details.put("Retrievers", retrievers.orderedStream().map(Retriever::getName).toList().toString());
        details.put("Top K", String.valueOf(properties.getRetrieval().getTopK()));
        details.put("Candidate Multiplier", String.valueOf(properties.getRetrieval().getCandidateMultiplier()));
        details.put("RRF K", String.valueOf(properties.getFusion().getRrfK()));
        details.put("Source Priority", properties.getFusion().getSourcePriority().toString());
        details.put("Rerank", properties.getRerank().isEnabled()
                + " (topK=" + properties.getRerank().getTopK() + ")");
        details.put("Context Max Length", String.valueOf(properties.getContext().getMaxLength()));
        details.put("Generation Timeout", properties.getGeneration().getTimeoutMs() + "ms");
        return new SimpleComponentSummary("AI", "RAG", true, 901, details);
    }
}

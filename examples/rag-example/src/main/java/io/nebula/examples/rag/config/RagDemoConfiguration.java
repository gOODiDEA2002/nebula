package io.nebula.examples.rag.config;

import io.nebula.ai.core.vectorstore.VectorStoreService;
import io.nebula.ai.rag.config.RagProperties;
import io.nebula.ai.rag.retriever.VectorStoreRetriever;
import io.nebula.examples.rag.index.ClasspathDocumentSource;
import io.nebula.examples.rag.index.FileIndexStateRepository;
import io.nebula.examples.rag.retriever.InMemoryKeywordIndex;
import io.nebula.examples.rag.retriever.InMemoryKeywordRetriever;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * 示例显式声明的 Bean。整类只在 {@code nebula.ai.rag.enabled=true} 时装配。
 * <p>
 * 装配分工（详见 README「示例写了什么、框架给了什么」）：
 * <ul>
 *   <li>{@link VectorStoreRetriever}：框架已有默认 Bean（权重固定 1.0、{@code @Order(10)}）。
 *       示例显式声明只为把权重接到 {@code VECTOR_WEIGHT}；框架默认 Bean 带
 *       {@code @ConditionalOnMissingBean} 会自动退避。</li>
 *   <li>{@link InMemoryKeywordRetriever}：{@code @Order(20)}，实现 {@code Retriever} 即被收集。</li>
 *   <li>{@link InMemoryKeywordIndex}：实现 {@code IndexSink} 即成为第二写目标。</li>
 *   <li>{@link ClasspathDocumentSource} / {@link FileIndexStateRepository}：索引治理的两个必写扩展点。</li>
 * </ul>
 * 其余（融合、重排、上下文装配、生成、清洗、引用后处理、流式、指标、管线、规划器、
 * 四个解析器、向量写目标、索引管线）全部由框架自动配置提供，示例一行不写。
 *
 * @author Nebula Framework
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "nebula.ai.rag", name = "enabled", havingValue = "true")
public class RagDemoConfiguration {

    /**
     * 向量检索器，权重接 {@code VECTOR_WEIGHT}。
     * <p>
     * 直接以 {@link VectorStoreService} 作为硬依赖：本类仅在 RAG 启用（即 AI 启用、
     * 向量库就绪）时装配，此时该 Bean 必然存在。相似度阈值取 0.0（不过滤，交由 RRF 融合排序）。
     */
    @Bean
    @Order(10)
    public VectorStoreRetriever vectorStoreRetriever(VectorStoreService vectorStoreService,
                                                     RagDemoProperties demoProperties) {
        return new VectorStoreRetriever(vectorStoreService, "vector",
                demoProperties.getVectorWeight(), 0.0);
    }

    @Bean
    @Order(20)
    public InMemoryKeywordRetriever inMemoryKeywordRetriever(InMemoryKeywordIndex index,
                                                             RagDemoProperties demoProperties) {
        return new InMemoryKeywordRetriever(index, demoProperties.getKeywordWeight());
    }

    @Bean
    public InMemoryKeywordIndex inMemoryKeywordIndex() {
        return new InMemoryKeywordIndex();
    }

    @Bean
    public ClasspathDocumentSource classpathDocumentSource(RagProperties ragProperties) {
        return new ClasspathDocumentSource(ragProperties);
    }

    @Bean
    public FileIndexStateRepository fileIndexStateRepository(RagDemoProperties demoProperties) {
        return new FileIndexStateRepository(demoProperties.getStateFile());
    }
}

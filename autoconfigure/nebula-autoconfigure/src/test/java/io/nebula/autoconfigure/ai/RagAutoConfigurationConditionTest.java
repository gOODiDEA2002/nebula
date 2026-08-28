package io.nebula.autoconfigure.ai;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.core.model.SearchRequest;
import io.nebula.ai.core.model.SearchResult;
import io.nebula.ai.core.vectorstore.VectorStoreService;
import io.nebula.ai.rag.fusion.FusionStrategy;
import io.nebula.ai.rag.fusion.RrfFusionStrategy;
import io.nebula.ai.rag.pipeline.AnswerGenerator;
import io.nebula.ai.rag.pipeline.ContextAssembler;
import io.nebula.ai.rag.pipeline.HybridRetrievalEngine;
import io.nebula.ai.rag.pipeline.RagPipeline;
import io.nebula.ai.rag.pipeline.RagPromptRenderer;
import io.nebula.ai.rag.rerank.Reranker;
import io.nebula.ai.rag.retriever.RetrievalResult;
import io.nebula.ai.rag.retriever.Retriever;
import io.nebula.ai.rag.retriever.VectorStoreRetriever;
import io.nebula.core.common.diagnostic.NebulaComponentSummary;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * RagAutoConfiguration 四类条件测试：默认关闭 / 显式启用 / 显式关闭 / 缺类
 * <p>
 * 另外覆盖两条硬约束：所有公共 Bean 可被应用替换；容器内没有任何 Retriever 时启动即失败。
 */
class RagAutoConfigurationConditionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RagAutoConfiguration.class))
            .withUserConfiguration(MockAiServices.class);

    @Test
    void defaultDisabled_registersNothing() {
        runner.run(ctx -> {
            assertThat(ctx).doesNotHaveBean(RagPipeline.class);
            assertThat(ctx).doesNotHaveBean(HybridRetrievalEngine.class);
            assertThat(ctx).doesNotHaveBean(FusionStrategy.class);
        });
    }

    @Test
    void explicitlyDisabled_registersNothing() {
        runner.withPropertyValues("nebula.ai.rag.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(RagPipeline.class));
    }

    @Test
    void enabled_registersFullPipeline() {
        runner.withPropertyValues("nebula.ai.rag.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(RagPipeline.class);
                    assertThat(ctx).hasSingleBean(HybridRetrievalEngine.class);
                    assertThat(ctx).hasSingleBean(FusionStrategy.class);
                    assertThat(ctx).hasSingleBean(Reranker.class);
                    assertThat(ctx).hasSingleBean(ContextAssembler.class);
                    assertThat(ctx).hasSingleBean(RagPromptRenderer.class);
                    assertThat(ctx).hasSingleBean(AnswerGenerator.class);
                    assertThat(ctx).hasSingleBean(VectorStoreRetriever.class);
                });
    }

    @Test
    void missingClass_doesNotLoadConfiguration() {
        runner.withPropertyValues("nebula.ai.rag.enabled=true")
                .withClassLoader(new FilteredClassLoader(RagPipeline.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(RagAutoConfiguration.class));
    }

    @Test
    void configuredValues_reachTheBeans() {
        runner.withPropertyValues(
                        "nebula.ai.rag.enabled=true",
                        "nebula.ai.rag.fusion.rrf-k=30",
                        "nebula.ai.rag.fusion.source-priority=graph",
                        "nebula.ai.rag.context.max-length=1234")
                .run(ctx -> {
                    RrfFusionStrategy fusion = (RrfFusionStrategy) ctx.getBean(FusionStrategy.class);
                    assertThat(fusion.getRrfK()).isEqualTo(30);
                    assertThat(fusion.getSourcePriority()).containsExactly("graph");
                    assertThat(ctx.getBean(ContextAssembler.class).getMaxLength()).isEqualTo(1234);
                });
    }

    /**
     * 没有任何 Retriever 时必须启动失败：这种配置下管线只会永远返回「没找到」，
     * 让它安静地起来等于把一次配置错误推迟到线上才暴露
     */
    @Test
    void noRetriever_failsFastWithActionableMessage() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RagAutoConfiguration.class))
                .withUserConfiguration(ChatServiceOnly.class)
                .withPropertyValues("nebula.ai.rag.enabled=true")
                .run(ctx -> assertThat(ctx).hasFailed()
                        .getFailure()
                        .rootCause()
                        .hasMessageContaining("Retriever"));
    }

    @Test
    void applicationBeans_overrideFrameworkDefaults() {
        runner.withPropertyValues("nebula.ai.rag.enabled=true")
                .withUserConfiguration(CustomRagBeans.class)
                .run(ctx -> {
                    assertThat(ctx.getBean(Reranker.class).getName()).isEqualTo("custom");
                    assertThat(ctx.getBean(AnswerGenerator.class).generate("p", 1))
                            .isEqualTo("custom-answer");
                    assertThat(ctx.getBean(RagPromptRenderer.class).render("q", "c"))
                            .isEqualTo("custom-prompt");
                    // 默认实现让位，容器里只剩应用自己的那一个
                    assertThat(ctx).hasSingleBean(Reranker.class);
                    assertThat(ctx).hasSingleBean(AnswerGenerator.class);
                });
    }

    @Test
    void customRetriever_participatesAlongsideDefaultVectorRetriever() {
        runner.withPropertyValues("nebula.ai.rag.enabled=true")
                .withUserConfiguration(CustomRetrieverConfig.class)
                .run(ctx -> assertThat(ctx.getBean(HybridRetrievalEngine.class).getRetrievers())
                        .extracting(Retriever::getName)
                        .contains("custom-retriever"));
    }

    @Test
    void enabled_exposesComponentSummary() {
        runner.withPropertyValues("nebula.ai.rag.enabled=true")
                .run(ctx -> {
                    NebulaComponentSummary summary = ctx.getBean("ragSummary", NebulaComponentSummary.class);
                    assertThat(summary.group()).isEqualTo("AI");
                    assertThat(summary.name()).isEqualTo("RAG");
                    assertThat(summary.isEnabled()).isTrue();
                    assertThat(summary.configDetails()).containsKeys("Retrievers", "Top K", "RRF K");
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class MockAiServices {

        @Bean
        VectorStoreService vectorStoreService() {
            return new EmptyVectorStoreService();
        }

        @Bean
        ChatService chatService() {
            return mock(ChatService.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ChatServiceOnly {

        @Bean
        ChatService chatService() {
            return mock(ChatService.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomRagBeans {

        @Bean
        Reranker customReranker() {
            return new Reranker() {
                @Override
                public List<RetrievalResult> rerank(String query, List<RetrievalResult> results, int topK) {
                    return results;
                }

                @Override
                public String getName() {
                    return "custom";
                }
            };
        }

        @Bean
        AnswerGenerator customAnswerGenerator() {
            return (prompt, timeoutMillis) -> "custom-answer";
        }

        @Bean
        RagPromptRenderer customPromptRenderer() {
            return (query, context) -> "custom-prompt";
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomRetrieverConfig {

        @Bean
        Retriever customRetriever() {
            return new Retriever() {
                @Override
                public List<RetrievalResult> retrieve(String query, int topK, Map<String, Object> filter) {
                    return List.of();
                }

                @Override
                public String getName() {
                    return "custom-retriever";
                }

                @Override
                public double getWeight() {
                    return 1.0;
                }
            };
        }
    }

    /**
     * 恒返回空结果的向量存储替身
     * <p>
     * 只实现装配路径真正会碰到的方法，其余一律抛
     * {@link UnsupportedOperationException}：静默返回默认值会让「装配把不该调的方法调了」
     * 这种问题在测试里看不出来。
     */
    private static class EmptyVectorStoreService implements VectorStoreService {

        @Override
        public SearchResult search(SearchRequest request) {
            return SearchResult.empty();
        }

        @Override
        public boolean add(io.nebula.ai.core.model.Document document) {
            throw unsupported();
        }

        @Override
        public int addAll(List<io.nebula.ai.core.model.Document> documents) {
            throw unsupported();
        }

        @Override
        public java.util.concurrent.CompletableFuture<Boolean> addAsync(
                io.nebula.ai.core.model.Document document) {
            throw unsupported();
        }

        @Override
        public java.util.concurrent.CompletableFuture<Integer> addAllAsync(
                List<io.nebula.ai.core.model.Document> documents) {
            throw unsupported();
        }

        @Override
        public io.nebula.ai.core.model.Document get(String id) {
            throw unsupported();
        }

        @Override
        public List<io.nebula.ai.core.model.Document> getAll(List<String> ids) {
            throw unsupported();
        }

        @Override
        public boolean delete(String id) {
            throw unsupported();
        }

        @Override
        public int deleteAll(List<String> ids) {
            throw unsupported();
        }

        @Override
        public int deleteByFilter(Map<String, Object> filter) {
            throw unsupported();
        }

        @Override
        public boolean update(io.nebula.ai.core.model.Document document) {
            throw unsupported();
        }

        @Override
        public SearchResult search(String query, int topK) {
            throw unsupported();
        }

        @Override
        public SearchResult search(String query, int topK, double similarityThreshold) {
            throw unsupported();
        }

        @Override
        public SearchResult search(String query, int topK, Map<String, Object> filter) {
            throw unsupported();
        }

        @Override
        public java.util.concurrent.CompletableFuture<SearchResult> searchAsync(String query, int topK) {
            throw unsupported();
        }

        @Override
        public java.util.concurrent.CompletableFuture<SearchResult> searchAsync(SearchRequest request) {
            throw unsupported();
        }

        @Override
        public SearchResult searchByVector(List<Double> vector, int topK) {
            throw unsupported();
        }

        @Override
        public SearchResult searchByVector(List<Double> vector, int topK, Map<String, Object> filter) {
            throw unsupported();
        }

        @Override
        public boolean exists(String id) {
            throw unsupported();
        }

        @Override
        public long count() {
            throw unsupported();
        }

        @Override
        public long count(Map<String, Object> filter) {
            throw unsupported();
        }

        @Override
        public boolean clear() {
            throw unsupported();
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public String getCollectionName() {
            return "test-collection";
        }

        @Override
        public boolean createCollection(int dimension) {
            throw unsupported();
        }

        @Override
        public boolean deleteCollection() {
            throw unsupported();
        }

        @Override
        public boolean collectionExists() {
            throw unsupported();
        }

        private static UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("装配测试不应调用该方法");
        }
    }
}

package io.nebula.autoconfigure.ai;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.core.model.SearchRequest;
import io.nebula.ai.core.model.SearchResult;
import io.nebula.ai.core.vectorstore.VectorStoreService;
import io.nebula.ai.rag.index.DocIndexState;
import io.nebula.ai.rag.index.DocumentSource;
import io.nebula.ai.rag.index.IndexPlanner;
import io.nebula.ai.rag.index.IndexSink;
import io.nebula.ai.rag.index.IndexStateRepository;
import io.nebula.ai.rag.index.IndexingPipeline;
import io.nebula.ai.rag.index.SourceDocument;
import io.nebula.ai.rag.index.SearchServiceIndexSink;
import io.nebula.ai.rag.index.VectorStoreIndexSink;
import io.nebula.search.core.SearchService;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * 索引治理装配条件矩阵（P2，详细设计 §2.4、§7）
 * <p>
 * 覆盖：默认关 / 显式开但缺 DocumentSource+IndexStateRepository（不装配 IndexingPipeline）/
 * 齐备时装配 / InMemoryIndexStateRepository 不进自动装配。
 */
class RagIndexingConditionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RagAutoConfiguration.class))
            .withUserConfiguration(MockAiServices.class)
            .withPropertyValues("nebula.ai.rag.enabled=true");

    @Test
    void indexingDisabledByDefault_registersNothing() {
        runner.run(ctx -> {
            assertThat(ctx).doesNotHaveBean(IndexingPipeline.class);
            assertThat(ctx).doesNotHaveBean(IndexPlanner.class);
            assertThat(ctx).doesNotHaveBean(VectorStoreIndexSink.class);
        });
    }

    @Test
    void indexingEnabledButNoSourceOrStateRepo_doesNotBuildPipeline() {
        runner.withPropertyValues("nebula.ai.rag.indexing.enabled=true")
                .run(ctx -> {
                    // planner 与 sink 可以有，但缺 DocumentSource + IndexStateRepository → 无 Pipeline
                    assertThat(ctx).doesNotHaveBean(IndexingPipeline.class);
                    assertThat(ctx).hasSingleBean(IndexPlanner.class);
                    assertThat(ctx).hasSingleBean(VectorStoreIndexSink.class);
                });
    }

    @Test
    void indexingEnabledWithSourceAndStateRepo_buildsPipeline() {
        runner.withPropertyValues("nebula.ai.rag.indexing.enabled=true")
                .withUserConfiguration(IndexingBeans.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(IndexingPipeline.class);
                    assertThat(ctx).hasSingleBean(VectorStoreIndexSink.class);
                });
    }

    /**
     * 回归守护：nebula-search-core 为可选依赖，消费方缺 SearchService 类时
     * IndexingConfiguration 仍须可内省、可装配（rag-example 首次 E2E 暴露的缺陷）。
     */
    @Test
    void indexingEnabledWithoutSearchCoreOnClasspath_stillBuildsPipeline() {
        runner.withClassLoader(new FilteredClassLoader(SearchService.class))
                .withPropertyValues("nebula.ai.rag.indexing.enabled=true")
                .withUserConfiguration(IndexingBeans.class)
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasSingleBean(IndexingPipeline.class);
                    assertThat(ctx).hasSingleBean(VectorStoreIndexSink.class);
                    assertThat(ctx).doesNotHaveBean(SearchServiceIndexSink.class);
                });
    }

    /**
     * F-5：写目标固定顺序——向量写目标（@Order(10)）先于关键词写目标（@Order(20)）。
     * {@code orderedStream()} 会读取 @Bean 方法上的 @Order。
     */
    @Test
    void indexSinks_orderedVectorThenSearch() {
        runner.withPropertyValues(
                        "nebula.ai.rag.indexing.enabled=true",
                        "nebula.ai.rag.indexing.search-index-name=rag_docs")
                .withUserConfiguration(SearchServiceBean.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(VectorStoreIndexSink.class);
                    assertThat(ctx).hasSingleBean(SearchServiceIndexSink.class);
                    List<Class<?>> order = ctx.getBeanProvider(IndexSink.class)
                            .orderedStream()
                            .map(Object::getClass)
                            .collect(java.util.stream.Collectors.toList());
                    assertThat(order)
                            .containsExactly(VectorStoreIndexSink.class, SearchServiceIndexSink.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class SearchServiceBean {

        @Bean
        SearchService searchService() {
            return mock(SearchService.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class MockAiServices {

        @Bean
        VectorStoreService vectorStoreService() {
            VectorStoreService mock = mock(VectorStoreService.class);
            lenient().when(mock.search(org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                    .thenReturn(SearchResult.empty());
            return mock;
        }

        @Bean
        ChatService chatService() {
            return mock(ChatService.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class IndexingBeans {

        @Bean
        DocumentSource documentSource() {
            return new DocumentSource() {
                @Override
                public String name() {
                    return "test-source";
                }

                @Override
                public List<SourceDocument> snapshot() {
                    return List.of();
                }
            };
        }

        @Bean
        IndexStateRepository indexStateRepository() {
            return new IndexStateRepository() {
                @Override
                public Map<String, DocIndexState> load(String sourceName) {
                    return Map.of();
                }

                @Override
                public void save(String sourceName, DocIndexState state) {
                }

                @Override
                public void remove(String sourceName, String docId) {
                }
            };
        }
    }
}

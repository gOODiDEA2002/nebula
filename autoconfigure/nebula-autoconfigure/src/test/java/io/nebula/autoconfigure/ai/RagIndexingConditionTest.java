package io.nebula.autoconfigure.ai;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.core.model.SearchRequest;
import io.nebula.ai.core.model.SearchResult;
import io.nebula.ai.core.vectorstore.VectorStoreService;
import io.nebula.ai.rag.index.DocIndexState;
import io.nebula.ai.rag.index.DocumentSource;
import io.nebula.ai.rag.index.IndexPlanner;
import io.nebula.ai.rag.index.IndexStateRepository;
import io.nebula.ai.rag.index.IndexingPipeline;
import io.nebula.ai.rag.index.SourceDocument;
import io.nebula.ai.rag.index.VectorStoreIndexSink;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
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

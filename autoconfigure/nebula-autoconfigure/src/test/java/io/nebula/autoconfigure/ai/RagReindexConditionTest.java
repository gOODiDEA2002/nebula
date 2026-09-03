package io.nebula.autoconfigure.ai;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.core.embedding.EmbeddingService;
import io.nebula.ai.core.model.SearchRequest;
import io.nebula.ai.core.model.SearchResult;
import io.nebula.ai.core.vectorstore.VectorStoreService;
import io.nebula.ai.rag.index.DocIndexState;
import io.nebula.ai.rag.index.DocumentSource;
import io.nebula.ai.rag.index.IndexStateRepository;
import io.nebula.ai.rag.index.ReindexPipeline;
import io.nebula.ai.rag.index.ReindexTarget;
import io.nebula.ai.rag.index.SourceDocument;
import io.nebula.search.core.SearchService;
import io.qdrant.client.QdrantClient;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
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
 * 版本化重灌装配条件矩阵（R3 §7、§8）
 * <p>
 * reindex.enabled 开关 × 两个别名键空/非空 × 缺 Qdrant 类 × 缺 SearchService 类。
 */
class RagReindexConditionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RagAutoConfiguration.class))
            .withUserConfiguration(MockServices.class)
            .withPropertyValues("nebula.ai.rag.enabled=true");

    @Test
    void reindexDisabledByDefault_registersNothing() {
        runner.withUserConfiguration(SourceAndState.class).run(ctx -> {
            assertThat(ctx).doesNotHaveBean(ReindexPipeline.class);
            assertThat(ctx).doesNotHaveBean(ReindexTarget.class);
        });
    }

    @Test
    void reindexEnabledButNoAlias_registersNothing() {
        runner.withUserConfiguration(SourceAndState.class)
                .withPropertyValues("nebula.ai.rag.indexing.reindex.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).doesNotHaveBean(ReindexPipeline.class);
                    assertThat(ctx).doesNotHaveBean(ReindexTarget.class);
                });
    }

    @Test
    void searchAliasSet_buildsSearchTargetAndPipeline() {
        runner.withUserConfiguration(SourceAndState.class, SearchServiceBean.class)
                .withPropertyValues(
                        "nebula.ai.rag.indexing.reindex.enabled=true",
                        "nebula.ai.rag.indexing.reindex.search-alias=chunks")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ReindexPipeline.class);
                    assertThat(ctx.getBeanNamesForType(ReindexTarget.class)).hasSize(1);
                });
    }

    @Test
    void vectorAliasSet_buildsVectorTargetAndPipeline() {
        runner.withUserConfiguration(SourceAndState.class, QdrantBeans.class)
                .withPropertyValues(
                        "nebula.ai.rag.indexing.reindex.enabled=true",
                        "nebula.ai.rag.indexing.reindex.vector-alias=vec",
                        "nebula.ai.rag.indexing.reindex.vector-dimension=768")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ReindexPipeline.class);
                    assertThat(ctx.getBeanNamesForType(ReindexTarget.class)).hasSize(1);
                });
    }

    @Test
    void bothAliasesSet_missingQdrantClass_buildsSearchTargetOnly() {
        runner.withUserConfiguration(SourceAndState.class, SearchServiceBean.class, QdrantBeans.class)
                .withClassLoader(new FilteredClassLoader(QdrantVectorStore.class))
                .withPropertyValues(
                        "nebula.ai.rag.indexing.reindex.enabled=true",
                        "nebula.ai.rag.indexing.reindex.vector-alias=vec",
                        "nebula.ai.rag.indexing.reindex.vector-dimension=768",
                        "nebula.ai.rag.indexing.reindex.search-alias=chunks")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ReindexPipeline.class);
                    // 缺 Qdrant 类 → 仅 BM25 目标
                    assertThat(ctx.getBeanNamesForType(ReindexTarget.class)).hasSize(1);
                });
    }

    @Test
    void searchAliasSet_missingSearchServiceClass_failsFastNoTarget() {
        runner.withUserConfiguration(SourceAndState.class)
                .withClassLoader(new FilteredClassLoader(SearchService.class))
                .withPropertyValues(
                        "nebula.ai.rag.indexing.reindex.enabled=true",
                        "nebula.ai.rag.indexing.reindex.search-alias=chunks")
                .run(ctx -> {
                    // 配了 search-alias 却缺 SearchService 类 → 无可用目标 → 快速失败
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure().getMessage())
                            .contains("没有可用的重灌切换目标");
                });
    }

    // ------------------------------------------------------------------

    @Configuration(proxyBeanMethods = false)
    static class MockServices {

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
    static class SourceAndState {

        @Bean
        DocumentSource documentSource() {
            return new DocumentSource() {
                @Override
                public String name() {
                    return "src";
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

    @Configuration(proxyBeanMethods = false)
    static class SearchServiceBean {

        @Bean
        SearchService searchService() {
            return mock(SearchService.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class QdrantBeans {

        @Bean("nebulaQdrantClient")
        QdrantClient nebulaQdrantClient() {
            return mock(QdrantClient.class);
        }

        @Bean
        EmbeddingModel embeddingModel() {
            return mock(EmbeddingModel.class);
        }

        @Bean
        EmbeddingService embeddingService() {
            return mock(EmbeddingService.class);
        }
    }
}

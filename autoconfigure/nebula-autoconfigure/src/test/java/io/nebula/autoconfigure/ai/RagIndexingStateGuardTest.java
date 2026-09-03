package io.nebula.autoconfigure.ai;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.core.model.SearchRequest;
import io.nebula.ai.core.model.SearchResult;
import io.nebula.ai.core.vectorstore.VectorStoreService;
import io.nebula.ai.rag.index.DocIndexState;
import io.nebula.ai.rag.index.DocumentSource;
import io.nebula.ai.rag.index.InMemoryIndexStateRepository;
import io.nebula.ai.rag.index.IndexStateRepository;
import io.nebula.ai.rag.index.SourceDocument;

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
 * 状态库缺席快速失败守卫的四态矩阵（R3 §7）
 * <p>
 * 覆盖：默认关（不启用索引治理）/ 开且有状态库（放行）/ 开且缺状态库（应启动失败）/
 * 开且缺状态库但检查关闭（放行）；另加 InMemoryIndexStateRepository 明示选择不失败。
 */
class RagIndexingStateGuardTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RagAutoConfiguration.class))
            .withUserConfiguration(MockAiServices.class)
            .withPropertyValues("nebula.ai.rag.enabled=true");

    @Test
    void indexingDisabledByDefault_startsWithoutGuard() {
        runner.withUserConfiguration(DocumentSourceOnly.class).run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).doesNotHaveBean(RagAutoConfiguration.IndexStateRepositoryGuard.class);
        });
    }

    @Test
    void enabledWithStateRepository_startsAndBuildsGuard() {
        runner.withPropertyValues("nebula.ai.rag.indexing.enabled=true")
                .withUserConfiguration(DocumentSourceOnly.class, PersistentStateRepo.class)
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasSingleBean(RagAutoConfiguration.IndexStateRepositoryGuard.class);
                });
    }

    @Test
    void enabledMissingStateRepository_failsFastByDefault() {
        runner.withPropertyValues("nebula.ai.rag.indexing.enabled=true")
                .withUserConfiguration(DocumentSourceOnly.class)
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class);
                    assertThat(ctx.getStartupFailure().getMessage())
                            .contains("IndexStateRepository");
                });
    }

    @Test
    void enabledMissingStateRepositoryButCheckDisabled_starts() {
        runner.withPropertyValues(
                        "nebula.ai.rag.indexing.enabled=true",
                        "nebula.ai.rag.indexing.fail-fast-without-state-repository=false")
                .withUserConfiguration(DocumentSourceOnly.class)
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasSingleBean(RagAutoConfiguration.IndexStateRepositoryGuard.class);
                });
    }

    @Test
    void enabledWithInMemoryStateRepository_startsWithoutFailure() {
        // 用户明示选择内存状态库(一次性任务), 不触发快速失败
        runner.withPropertyValues("nebula.ai.rag.indexing.enabled=true")
                .withUserConfiguration(DocumentSourceOnly.class, InMemoryStateRepo.class)
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasSingleBean(RagAutoConfiguration.IndexStateRepositoryGuard.class);
                });
    }

    @Test
    void enabledWithoutDocumentSource_startsWithoutGuard() {
        // 没有 DocumentSource 即没有增量任务, 守卫不构造
        runner.withPropertyValues("nebula.ai.rag.indexing.enabled=true").run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).doesNotHaveBean(RagAutoConfiguration.IndexStateRepositoryGuard.class);
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
    static class DocumentSourceOnly {

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
    }

    @Configuration(proxyBeanMethods = false)
    static class PersistentStateRepo {

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
    static class InMemoryStateRepo {

        @Bean
        IndexStateRepository indexStateRepository() {
            return new InMemoryIndexStateRepository();
        }
    }
}

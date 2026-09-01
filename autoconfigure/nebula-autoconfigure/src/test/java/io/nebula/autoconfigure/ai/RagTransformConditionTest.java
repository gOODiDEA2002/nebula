package io.nebula.autoconfigure.ai;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.core.model.SearchRequest;
import io.nebula.ai.core.model.SearchResult;
import io.nebula.ai.core.vectorstore.VectorStoreService;
import io.nebula.ai.rag.transform.QueryTransformer;
import io.nebula.ai.rag.transform.QueryVariant;
import io.nebula.ai.rag.transform.TrimQueryTransformer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * 查询改写装配三态（P5，详细设计 §4.2、§7）
 * <p>
 * mode=none 默认装 {@link TrimQueryTransformer}；显式配 rewrite 但没有任何改写器 Bean（即
 * nebula-ai-spring 适配器不可用）时，默认 Bean 直接快速失败，不静默退回 trim；应用自带
 * QueryTransformer 时框架让位。
 */
class RagTransformConditionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RagAutoConfiguration.class))
            .withUserConfiguration(MockAiServices.class)
            .withPropertyValues("nebula.ai.rag.enabled=true");

    @Test
    void modeNone_registersTrimTransformer() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(QueryTransformer.class);
            assertThat(ctx.getBean(QueryTransformer.class)).isInstanceOf(TrimQueryTransformer.class);
        });
    }

    @Test
    void modeRewriteWithoutAdapter_failsFast() {
        runner.withPropertyValues("nebula.ai.rag.transform.mode=rewrite")
                .run(ctx -> assertThat(ctx).hasFailed()
                        .getFailure()
                        .rootCause()
                        .hasMessageContaining("transform.mode=rewrite"));
    }

    @Test
    void modeMultiQueryWithoutAdapter_failsFast() {
        runner.withPropertyValues("nebula.ai.rag.transform.mode=multi-query")
                .run(ctx -> assertThat(ctx).hasFailed()
                        .getFailure()
                        .rootCause()
                        .hasMessageContaining("multi-query"));
    }

    @Test
    void applicationTransformer_overridesFrameworkDefault() {
        runner.withPropertyValues("nebula.ai.rag.transform.mode=rewrite")
                .withUserConfiguration(CustomTransformer.class)
                .run(ctx -> {
                    // 应用提供了 QueryTransformer，框架默认让位，rewrite 模式也不再快速失败
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx.getBean(QueryTransformer.class).transform("q"))
                            .extracting(QueryVariant::getText).containsExactly("custom");
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
    static class CustomTransformer {

        @Bean
        QueryTransformer customTransformer() {
            return rawQuery -> List.of(new QueryVariant("custom", 1.0));
        }
    }
}

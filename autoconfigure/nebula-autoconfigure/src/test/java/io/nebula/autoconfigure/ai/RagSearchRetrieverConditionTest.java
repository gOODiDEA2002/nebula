package io.nebula.autoconfigure.ai;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.core.model.SearchRequest;
import io.nebula.ai.core.model.SearchResult;
import io.nebula.ai.core.vectorstore.VectorStoreService;
import io.nebula.ai.rag.pipeline.HybridRetrievalEngine;
import io.nebula.ai.rag.retriever.Retriever;
import io.nebula.ai.rag.retriever.SearchServiceRetriever;
import io.nebula.ai.rag.retriever.VectorStoreRetriever;
import io.nebula.search.core.SearchService;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * BM25 关键词检索路的装配条件矩阵（P4b，详细设计 §3.3、§7）
 * <p>
 * 覆盖：默认关（index-name 空）/ 显式开（index-name 非空）/ 缺 SearchService 类
 * （FilteredClassLoader）/ 缺 SearchService Bean；以及检索器列表顺序断言
 * （向量 {@code @Order(10)} 在前、关键词 order=20 在后）。
 */
class RagSearchRetrieverConditionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RagAutoConfiguration.class))
            .withUserConfiguration(MockServices.class)
            .withPropertyValues("nebula.ai.rag.enabled=true");

    @Test
    void indexNameEmpty_doesNotRegisterSearchRetriever() {
        runner.run(ctx -> {
            assertThat(ctx).doesNotHaveBean(SearchServiceRetriever.class);
            assertThat(ctx.getBean(HybridRetrievalEngine.class).getRetrievers())
                    .extracting(Retriever::getName)
                    .containsExactly("VectorStoreRetriever");
        });
    }

    @Test
    void indexNameSet_registersSearchRetrieverAfterVectorRetriever() {
        runner.withPropertyValues(
                        "nebula.ai.rag.search.index-name=rag-chunks",
                        "nebula.ai.rag.search.weight=0.4")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(SearchServiceRetriever.class);
                    SearchServiceRetriever retriever = ctx.getBean(SearchServiceRetriever.class);
                    assertThat(retriever.getWeight()).isEqualTo(0.4);
                    // 顺序断言：向量(@Order 10)在前，关键词(order 20)在后
                    assertThat(ctx.getBean(HybridRetrievalEngine.class).getRetrievers())
                            .extracting(Retriever::getName)
                            .containsExactly("VectorStoreRetriever", "SearchServiceRetriever");
                });
    }

    @Test
    void customOrder_isHonoured() {
        runner.withPropertyValues(
                        "nebula.ai.rag.search.index-name=rag-chunks",
                        "nebula.ai.rag.search.order=5")
                .run(ctx -> {
                    // order=5 让关键词检索器排到向量(@Order 10)之前
                    assertThat(ctx.getBean(HybridRetrievalEngine.class).getRetrievers())
                            .extracting(Retriever::getName)
                            .containsExactly("SearchServiceRetriever", "VectorStoreRetriever");
                });
    }

    @Test
    void missingSearchServiceClass_registersNoSearchRetriever() {
        runner.withPropertyValues("nebula.ai.rag.search.index-name=rag-chunks")
                .withClassLoader(new FilteredClassLoader(SearchService.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(HybridRetrievalEngine.class);
                    assertThat(ctx.getBean(HybridRetrievalEngine.class).getRetrievers())
                            .extracting(Retriever::getName)
                            .containsExactly("VectorStoreRetriever");
                });
    }

    @Test
    void missingSearchServiceBean_registersNoSearchRetriever() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RagAutoConfiguration.class))
                .withUserConfiguration(VectorOnlyServices.class)
                .withPropertyValues(
                        "nebula.ai.rag.enabled=true",
                        "nebula.ai.rag.search.index-name=rag-chunks")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(SearchServiceRetriever.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class MockServices {

        @Bean
        VectorStoreService vectorStoreService() {
            VectorStoreService mock = mock(VectorStoreService.class);
            org.mockito.Mockito.lenient().when(mock.search(org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                    .thenReturn(SearchResult.empty());
            return mock;
        }

        @Bean
        SearchService searchService() {
            return mock(SearchService.class);
        }

        @Bean
        ChatService chatService() {
            return mock(ChatService.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class VectorOnlyServices {

        @Bean
        VectorStoreService vectorStoreService() {
            return mock(VectorStoreService.class);
        }

        @Bean
        ChatService chatService() {
            return mock(ChatService.class);
        }
    }
}

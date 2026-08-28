package io.nebula.autoconfigure.ai;

import io.nebula.ai.spring.vectorstore.QdrantIdMappingVectorStore;
import io.qdrant.client.QdrantClient;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 向量后端 provider 分支的四类条件与互斥
 * <p>
 * 同类型的 {@code VectorStore} Bean 只能有一个 {@code @Primary}，
 * 所以 chroma 与 qdrant 必须互斥装配；这里把「默认仍是 chroma（已发布行为不变）」
 * 与「切到 qdrant 后 Chroma Bean 让位」两条同时钉住。
 */
class AIVectorStoreProviderConditionTest {

    /**
     * 只验条件命中与否，不真的连后端：Chroma Bean 在创建时就会去 GET 集合，
     * 因此统一延迟初始化，需要实例的用例再显式 getBean 触发。
     */
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AIAutoConfiguration.class))
            .withUserConfiguration(MockModels.class)
            .withInitializer(ctx -> ctx.addBeanFactoryPostProcessor(
                    new LazyInitializationBeanFactoryPostProcessor()))
            .withPropertyValues("nebula.ai.enabled=true");

    /** 快速失败类用例需要真实例化，不加延迟初始化 */
    private final ApplicationContextRunner eagerRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AIAutoConfiguration.class))
            .withUserConfiguration(MockModels.class)
            .withPropertyValues("nebula.ai.enabled=true");

    @Test
    void defaultProvider_keepsChromaAndSkipsQdrant() {
        runner.run(ctx -> {
            assertThat(registered(ctx, "nebulaChromaApi")).isTrue();
            assertThat(registered(ctx, "nebulaChromaVectorStore")).isTrue();
            assertThat(registered(ctx, "nebulaQdrantClient")).isFalse();
            assertThat(registered(ctx, "nebulaQdrantVectorStore")).isFalse();
        });
    }

    @Test
    void explicitChromaProvider_keepsChromaAndSkipsQdrant() {
        runner.withPropertyValues("nebula.ai.vector-store.default-provider=chroma")
                .run(ctx -> {
                    assertThat(registered(ctx, "nebulaChromaVectorStore")).isTrue();
                    assertThat(registered(ctx, "nebulaQdrantVectorStore")).isFalse();
                });
    }

    @Test
    void qdrantProvider_registersQdrantAndSkipsChroma() {
        runner.withPropertyValues(
                        "nebula.ai.vector-store.default-provider=qdrant",
                        "nebula.ai.vector-store.qdrant.host=qdrant.invalid",
                        "nebula.ai.vector-store.qdrant.collection-name=nebula-test")
                .run(ctx -> {
                    assertThat(registered(ctx, "nebulaQdrantClient")).isTrue();
                    assertThat(registered(ctx, "nebulaQdrantVectorStore")).isTrue();
                    assertThat(ctx.getBean("nebulaQdrantClient")).isInstanceOf(QdrantClient.class);
                    // 未开映射时对外就是裸的 QdrantVectorStore，不该被无谓地包一层
                    assertThat(ctx.getBean("nebulaQdrantVectorStore"))
                            .isInstanceOf(QdrantVectorStore.class);
                    assertThat(registered(ctx, "nebulaChromaApi")).isFalse();
                    assertThat(registered(ctx, "nebulaChromaVectorStore")).isFalse();
                    // VectorStore 只剩一个候选，注入不会因启动顺序变结果
                    assertThat(ctx.getBeanNamesForType(VectorStore.class)).hasSize(1);
                });
    }

    @Test
    void qdrantWithIdMapping_wrapsStoreInMappingDecorator() {
        runner.withPropertyValues(
                        "nebula.ai.vector-store.default-provider=qdrant",
                        "nebula.ai.vector-store.qdrant.id-mapping.enabled=true",
                        "nebula.ai.vector-store.qdrant.id-mapping.namespace-name=vector.sia.vocoor.com",
                        "nebula.ai.vector-store.qdrant.id-mapping.original-doc-id-field=orig_doc_id")
                .run(ctx -> {
                    QdrantIdMappingVectorStore store = ctx.getBean(
                            "nebulaQdrantVectorStore", QdrantIdMappingVectorStore.class);
                    assertThat(store.getDelegate()).isInstanceOf(QdrantVectorStore.class);
                    assertThat(store.getOriginalDocIdField()).isEqualTo("orig_doc_id");
                });
    }

    /**
     * 命名空间漏配必须启动即失败：放行的话会用一个全新命名空间写入，
     * 表现为写得进去、查不出来，比启动不来难查得多
     */
    @Test
    void idMappingWithoutNamespace_failsFastAtStartup() {
        eagerRunner.withPropertyValues(
                        "nebula.ai.vector-store.default-provider=qdrant",
                        "nebula.ai.vector-store.qdrant.id-mapping.enabled=true")
                .run(ctx -> assertThat(ctx).hasFailed()
                        .getFailure()
                        .rootCause()
                        .hasMessageContaining("namespace-name"));
    }

    @Test
    void missingQdrantClass_skipsQdrantBeansEvenWhenProviderIsQdrant() {
        runner.withPropertyValues("nebula.ai.vector-store.default-provider=qdrant")
                .withClassLoader(new FilteredClassLoader(QdrantVectorStore.class))
                .run(ctx -> {
                    assertThat(registered(ctx, "nebulaQdrantClient")).isFalse();
                    assertThat(registered(ctx, "nebulaQdrantVectorStore")).isFalse();
                });
    }

    @Test
    void aiDisabled_registersNeitherBackend() {
        runner.withPropertyValues("nebula.ai.enabled=false",
                        "nebula.ai.vector-store.default-provider=qdrant")
                .run(ctx -> {
                    assertThat(registered(ctx, "nebulaQdrantVectorStore")).isFalse();
                    assertThat(registered(ctx, "nebulaChromaVectorStore")).isFalse();
                });
    }

    @Test
    void missingChromaClass_skipsChromaBeansUnderDefaultProvider() {
        runner.withClassLoader(new FilteredClassLoader(ChromaApi.class))
                .run(ctx -> {
                    assertThat(registered(ctx, "nebulaChromaApi")).isFalse();
                    assertThat(registered(ctx, "nebulaChromaVectorStore")).isFalse();
                });
    }

    /**
     * 只看 Bean 定义在不在，不触发实例化：Chroma Bean 一被创建就会去连 localhost:8000
     */
    private static boolean registered(ConfigurableApplicationContext ctx, String beanName) {
        return ctx.getBeanFactory().containsBeanDefinition(beanName);
    }

    @Configuration(proxyBeanMethods = false)
    static class MockModels {

        @Bean
        EmbeddingModel embeddingModel() {
            EmbeddingModel model = mock(EmbeddingModel.class);
            when(model.dimensions()).thenReturn(1024);
            return model;
        }

        @Bean
        ChatModel chatModel() {
            return mock(ChatModel.class);
        }

        @Bean
        ChatClient.Builder chatClientBuilder() {
            ChatClient.Builder builder = mock(ChatClient.Builder.class);
            when(builder.build()).thenReturn(mock(ChatClient.class));
            return builder;
        }
    }
}

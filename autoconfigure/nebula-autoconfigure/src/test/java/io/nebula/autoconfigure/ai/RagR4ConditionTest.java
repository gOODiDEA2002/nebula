package io.nebula.autoconfigure.ai;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.rag.guard.NoopRetrievedContentSanitizer;
import io.nebula.ai.rag.guard.PatternSanitizer;
import io.nebula.ai.rag.guard.RetrievedContentSanitizer;
import io.nebula.ai.rag.metrics.MicrometerRagMetrics;
import io.nebula.ai.rag.metrics.NoopRagMetrics;
import io.nebula.ai.rag.metrics.RagMetrics;
import io.nebula.ai.rag.pipeline.CitationPostProcessor;
import io.nebula.ai.rag.pipeline.NoopCitationPostProcessor;
import io.nebula.ai.rag.pipeline.StreamingAnswerGenerator;
import io.nebula.ai.rag.rerank.NoopReranker;
import io.nebula.ai.rag.rerank.Reranker;
import io.nebula.ai.rag.rerank.http.HttpCrossEncoderReranker;
import io.nebula.ai.rag.retriever.RetrievalResult;
import io.nebula.ai.rag.retriever.Retriever;

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
 * R4 装配条件矩阵（R4 §3.3、§4、§5.5、§6.3、§9）
 * <p>
 * HTTP 重排（url 缺席/存在、用户 Bean 优先、wire-format 非法、cohere 缺 model）、清洗器三态、
 * 流式（有/无 ChatService）、指标（有/无 MeterRegistry Bean、缺 micrometer 类）。
 */
class RagR4ConditionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RagAutoConfiguration.class))
            .withUserConfiguration(BaseServices.class)
            .withPropertyValues("nebula.ai.rag.enabled=true");

    // ---- HTTP 重排 ----

    @Test
    void rerankHttpUrlAbsent_usesNoopReranker() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(Reranker.class);
            assertThat(ctx.getBean(Reranker.class)).isInstanceOf(NoopReranker.class);
        });
    }

    @Test
    void rerankHttpUrlSet_usesHttpCrossEncoderReranker() {
        runner.withPropertyValues("nebula.ai.rag.rerank.http.url=http://tei:8080/rerank")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(Reranker.class);
                    assertThat(ctx.getBean(Reranker.class)).isInstanceOf(HttpCrossEncoderReranker.class);
                });
    }

    @Test
    void userRerankerBean_winsOverHttp() {
        runner.withUserConfiguration(UserReranker.class)
                .withPropertyValues("nebula.ai.rag.rerank.http.url=http://tei:8080/rerank")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(Reranker.class);
                    assertThat(ctx.getBean(Reranker.class)).isInstanceOf(MarkerReranker.class);
                });
    }

    @Test
    void wireFormatIllegal_failsFast() {
        runner.withPropertyValues(
                        "nebula.ai.rag.rerank.http.url=http://tei:8080/rerank",
                        "nebula.ai.rag.rerank.http.wire-format=bogus")
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure().getMessage()).contains("wire-format");
                });
    }

    @Test
    void cohereMissingModel_failsFast() {
        runner.withPropertyValues(
                        "nebula.ai.rag.rerank.http.url=http://cohere/v2/rerank",
                        "nebula.ai.rag.rerank.http.wire-format=cohere")
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure().getMessage()).contains("model");
                });
    }

    @Test
    void cohereWithModel_usesHttpCrossEncoderReranker() {
        runner.withPropertyValues(
                        "nebula.ai.rag.rerank.http.url=http://cohere/v2/rerank",
                        "nebula.ai.rag.rerank.http.wire-format=cohere",
                        "nebula.ai.rag.rerank.http.model=rerank-v3.5")
                .run(ctx -> assertThat(ctx.getBean(Reranker.class))
                        .isInstanceOf(HttpCrossEncoderReranker.class));
    }

    // ---- 清洗器三态 ----

    @Test
    void sanitizerDisabledByDefault_isNoop() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(RetrievedContentSanitizer.class);
            assertThat(ctx.getBean(RetrievedContentSanitizer.class))
                    .isInstanceOf(NoopRetrievedContentSanitizer.class);
        });
    }

    @Test
    void sanitizerEnabled_isPatternSanitizer() {
        runner.withPropertyValues("nebula.ai.rag.guard.sanitizer.enabled=true")
                .run(ctx -> assertThat(ctx.getBean(RetrievedContentSanitizer.class))
                        .isInstanceOf(PatternSanitizer.class));
    }

    @Test
    void userSanitizerBean_wins() {
        runner.withUserConfiguration(UserSanitizer.class)
                .withPropertyValues("nebula.ai.rag.guard.sanitizer.enabled=true")
                .run(ctx -> assertThat(ctx.getBean(RetrievedContentSanitizer.class))
                        .isInstanceOf(MarkerSanitizer.class));
    }

    @Test
    void citationPostProcessor_defaultIsNoop() {
        runner.run(ctx -> assertThat(ctx.getBean(CitationPostProcessor.class))
                .isInstanceOf(NoopCitationPostProcessor.class));
    }

    // ---- 流式 ----

    @Test
    void streamingDisabledByDefault_noGenerator() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(StreamingAnswerGenerator.class));
    }

    @Test
    void streamingEnabledWithChatService_registersGenerator() {
        runner.withPropertyValues("nebula.ai.rag.streaming.enabled=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(StreamingAnswerGenerator.class));
    }

    @Test
    void streamingEnabledNoChatService_noGenerator() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RagAutoConfiguration.class))
                .withUserConfiguration(RetrieverOnly.class)
                .withPropertyValues("nebula.ai.rag.enabled=true",
                        "nebula.ai.rag.streaming.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).doesNotHaveBean(StreamingAnswerGenerator.class);
                });
    }

    // ---- 指标 ----

    @Test
    void metricsDisabledByDefault_isNoop() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(RagMetrics.class);
            assertThat(ctx.getBean(RagMetrics.class)).isInstanceOf(NoopRagMetrics.class);
        });
    }

    @Test
    void metricsEnabledWithMeterRegistry_isMicrometer() {
        runner.withUserConfiguration(MeterRegistryBean.class)
                .withPropertyValues("nebula.ai.rag.metrics.enabled=true")
                .run(ctx -> assertThat(ctx.getBean(RagMetrics.class))
                        .isInstanceOf(MicrometerRagMetrics.class));
    }

    @Test
    void metricsEnabledNoMeterRegistry_isNoop() {
        runner.withPropertyValues("nebula.ai.rag.metrics.enabled=true")
                .run(ctx -> assertThat(ctx.getBean(RagMetrics.class))
                        .isInstanceOf(NoopRagMetrics.class));
    }

    @Test
    void metricsEnabledMicrometerClassAbsent_isNoop() {
        runner.withClassLoader(new FilteredClassLoader(MeterRegistry.class))
                .withPropertyValues("nebula.ai.rag.metrics.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx.getBean(RagMetrics.class)).isInstanceOf(NoopRagMetrics.class);
                });
    }

    // ------------------------------------------------------------------

    @Configuration(proxyBeanMethods = false)
    static class BaseServices {

        @Bean
        Retriever retriever() {
            return new StubRetriever();
        }

        @Bean
        ChatService chatService() {
            return mock(ChatService.class);
        }
    }

    /** 仅提供 Retriever，无 ChatService（验证流式在缺 ChatService 时不装配） */
    @Configuration(proxyBeanMethods = false)
    static class RetrieverOnly {

        @Bean
        Retriever retriever() {
            return new StubRetriever();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class UserReranker {

        @Bean
        Reranker userReranker() {
            return new MarkerReranker();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class UserSanitizer {

        @Bean
        RetrievedContentSanitizer userSanitizer() {
            return new MarkerSanitizer();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class MeterRegistryBean {

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    static final class StubRetriever implements Retriever {
        @Override
        public List<RetrievalResult> retrieve(String query, int topK, Map<String, Object> filter) {
            return List.of();
        }

        @Override
        public String getName() {
            return "stub";
        }

        @Override
        public double getWeight() {
            return 1.0;
        }
    }

    static final class MarkerReranker implements Reranker {
        @Override
        public List<RetrievalResult> rerank(String query, List<RetrievalResult> results, int topK) {
            return results;
        }

        @Override
        public String getName() {
            return "marker";
        }
    }

    static final class MarkerSanitizer implements RetrievedContentSanitizer {
        @Override
        public RetrievalResult sanitize(RetrievalResult result) {
            return result;
        }
    }
}

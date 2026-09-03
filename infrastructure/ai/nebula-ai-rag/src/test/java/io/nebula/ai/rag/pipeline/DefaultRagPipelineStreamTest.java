package io.nebula.ai.rag.pipeline;

import io.nebula.ai.rag.config.RagProperties;
import io.nebula.ai.rag.fusion.RrfFusionStrategy;
import io.nebula.ai.rag.metrics.NoopRagMetrics;
import io.nebula.ai.rag.rerank.NoopReranker;
import io.nebula.ai.rag.retriever.RetrievalResult;
import io.nebula.ai.rag.retriever.Retriever;
import io.nebula.ai.rag.transform.TrimQueryTransformer;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 流式管线的事件契约（R4 §5.3、§10），用 {@code reactor-test} {@link StepVerifier} 驱动。
 * <p>
 * 覆盖：正常 REFERENCES→DELTA*→COMPLETE（answer 为拼接并经引用后处理的完整文本）；无文档序列；
 * {@code generateAnswer=false}；端口缺席仅 {@code ERROR(streaming-unsupported)}；首个 DELTA 前失败降级为
 * 摘要 DELTA + COMPLETE(degraded)；DELTA 后失败仅 ERROR；总时限超时；取消传播到上游；参数非法 onError。
 */
class DefaultRagPipelineStreamTest {

    @Test
    void normalSequence_referencesThenDeltasThenCompleteWithPostProcessedAnswer() {
        DefaultRagPipeline pipeline = streamingPipeline(properties(),
                prompt -> Flux.just("Hello ", "World"), hit("A"));

        StepVerifier.create(pipeline.queryStream(RagQuery.of("问题")))
                .assertNext(e -> {
                    assertThat(e.getType()).isEqualTo(RagStreamEvent.Type.REFERENCES);
                    assertThat(e.getReferences()).extracting(RetrievalResult::getId).containsExactly("A");
                })
                .assertNext(e -> {
                    assertThat(e.getType()).isEqualTo(RagStreamEvent.Type.DELTA);
                    assertThat(e.getDelta()).isEqualTo("Hello ");
                })
                .assertNext(e -> assertThat(e.getDelta()).isEqualTo("World"))
                .assertNext(e -> {
                    assertThat(e.getType()).isEqualTo(RagStreamEvent.Type.COMPLETE);
                    // 拼接文本经引用后处理（后缀 |cited）
                    assertThat(e.getAnswer().getAnswer()).isEqualTo("Hello World|cited");
                    assertThat(e.getAnswer().isDegraded()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void noDocument_referencesEmptyThenDeltaThenCompleteDegraded() {
        RagProperties properties = properties();
        properties.getDegrade().setNoDocumentAnswer("知识库里没有相关内容");
        DefaultRagPipeline pipeline = streamingPipeline(properties,
                prompt -> Flux.just("不该出现"));

        StepVerifier.create(pipeline.queryStream(RagQuery.of("查不到")))
                .assertNext(e -> {
                    assertThat(e.getType()).isEqualTo(RagStreamEvent.Type.REFERENCES);
                    assertThat(e.getReferences()).isEmpty();
                })
                .assertNext(e -> {
                    assertThat(e.getType()).isEqualTo(RagStreamEvent.Type.DELTA);
                    assertThat(e.getDelta()).isEqualTo("知识库里没有相关内容");
                })
                .assertNext(e -> {
                    assertThat(e.getType()).isEqualTo(RagStreamEvent.Type.COMPLETE);
                    assertThat(e.getAnswer().isDegraded()).isTrue();
                    assertThat(e.getAnswer().getDegradeReason())
                            .isEqualTo(DefaultRagPipeline.REASON_NO_DOCUMENT);
                })
                .verifyComplete();
    }

    @Test
    void generateAnswerFalse_referencesThenCompleteWithNullAnswer() {
        DefaultRagPipeline pipeline = streamingPipeline(properties(),
                prompt -> Flux.just("不该出现"), hit("A"));

        StepVerifier.create(pipeline.queryStream(RagQuery.builder()
                        .query("问题").generateAnswer(false).build()))
                .assertNext(e -> assertThat(e.getType()).isEqualTo(RagStreamEvent.Type.REFERENCES))
                .assertNext(e -> {
                    assertThat(e.getType()).isEqualTo(RagStreamEvent.Type.COMPLETE);
                    assertThat(e.getAnswer().getAnswer()).isNull();
                    assertThat(e.getAnswer().getReferences()).extracting(RetrievalResult::getId)
                            .containsExactly("A");
                })
                .verifyComplete();
    }

    @Test
    void streamingPortAbsent_onlyStreamingUnsupportedError() {
        DefaultRagPipeline pipeline = streamingPipeline(properties(), null, hit("A"));

        StepVerifier.create(pipeline.queryStream(RagQuery.of("问题")))
                .assertNext(e -> {
                    assertThat(e.getType()).isEqualTo(RagStreamEvent.Type.ERROR);
                    assertThat(e.getErrorReason())
                            .isEqualTo(DefaultRagPipeline.REASON_STREAMING_UNSUPPORTED);
                })
                .verifyComplete();
    }

    @Test
    void failureBeforeFirstDelta_degradesToSummaryDeltaAndCompleteDegraded() {
        DefaultRagPipeline pipeline = streamingPipeline(properties(),
                prompt -> Flux.error(new IllegalStateException("模型挂了")), hit("A"));

        StepVerifier.create(pipeline.queryStream(RagQuery.of("问题")))
                .assertNext(e -> assertThat(e.getType()).isEqualTo(RagStreamEvent.Type.REFERENCES))
                .assertNext(e -> {
                    // 首个 DELTA 之前失败：补一条检索摘要 DELTA
                    assertThat(e.getType()).isEqualTo(RagStreamEvent.Type.DELTA);
                    assertThat(e.getDelta()).contains("内容A");
                })
                .assertNext(e -> {
                    assertThat(e.getType()).isEqualTo(RagStreamEvent.Type.COMPLETE);
                    assertThat(e.getAnswer().isDegraded()).isTrue();
                    assertThat(e.getAnswer().getDegradeReason())
                            .isEqualTo(DefaultRagPipeline.REASON_GENERATION_FAILED);
                })
                .verifyComplete();
    }

    @Test
    void failureAfterFirstDelta_emitsErrorWithoutSummary() {
        DefaultRagPipeline pipeline = streamingPipeline(properties(),
                prompt -> Flux.just("已经出了一段").concatWith(Flux.error(new IllegalStateException("中途挂了"))),
                hit("A"));

        StepVerifier.create(pipeline.queryStream(RagQuery.of("问题")))
                .assertNext(e -> assertThat(e.getType()).isEqualTo(RagStreamEvent.Type.REFERENCES))
                .assertNext(e -> {
                    assertThat(e.getType()).isEqualTo(RagStreamEvent.Type.DELTA);
                    assertThat(e.getDelta()).isEqualTo("已经出了一段");
                })
                .assertNext(e -> {
                    // DELTA 之后失败：只发 ERROR，不补摘要（R4-D7）
                    assertThat(e.getType()).isEqualTo(RagStreamEvent.Type.ERROR);
                    assertThat(e.getErrorReason())
                            .isEqualTo(DefaultRagPipeline.REASON_GENERATION_FAILED);
                })
                .verifyComplete();
    }

    @Test
    void totalTimeoutAfterFirstDelta_emitsGenerationTimeoutError() {
        RagProperties properties = properties();
        properties.getGeneration().setTimeoutMs(200); // 总时限 200ms
        // 出一段后永不结束：触发总时限
        DefaultRagPipeline pipeline = streamingPipeline(properties,
                prompt -> Flux.just("片段").concatWith(Flux.never()), hit("A"));

        StepVerifier.create(pipeline.queryStream(RagQuery.of("问题")))
                .assertNext(e -> assertThat(e.getType()).isEqualTo(RagStreamEvent.Type.REFERENCES))
                .assertNext(e -> assertThat(e.getType()).isEqualTo(RagStreamEvent.Type.DELTA))
                .assertNext(e -> {
                    assertThat(e.getType()).isEqualTo(RagStreamEvent.Type.ERROR);
                    assertThat(e.getErrorReason())
                            .isEqualTo(DefaultRagPipeline.REASON_GENERATION_TIMEOUT);
                })
                .expectComplete()
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void cancel_propagatesToUpstream() throws InterruptedException {
        AtomicBoolean upstreamCancelled = new AtomicBoolean(false);
        DefaultRagPipeline pipeline = streamingPipeline(properties(),
                prompt -> Flux.just("a").concatWith(Flux.<String>never())
                        .doOnCancel(() -> upstreamCancelled.set(true)),
                hit("A"));

        StepVerifier.create(pipeline.queryStream(RagQuery.of("问题")))
                .assertNext(e -> assertThat(e.getType()).isEqualTo(RagStreamEvent.Type.REFERENCES))
                .assertNext(e -> assertThat(e.getType()).isEqualTo(RagStreamEvent.Type.DELTA))
                .thenCancel()
                .verify(Duration.ofSeconds(5));

        // 取消异步传播到上游，短轮询等待
        for (int i = 0; i < 50 && !upstreamCancelled.get(); i++) {
            Thread.sleep(20);
        }
        assertThat(upstreamCancelled.get()).isTrue();
    }

    @Test
    void blankOrNullQuery_signalsIllegalArgumentException() {
        DefaultRagPipeline pipeline = streamingPipeline(properties(),
                prompt -> Flux.just("x"), hit("A"));

        StepVerifier.create(pipeline.queryStream(RagQuery.of("   ")))
                .expectError(IllegalArgumentException.class)
                .verify();
        StepVerifier.create(pipeline.queryStream(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    // ---- helpers ----

    private static DefaultRagPipeline streamingPipeline(RagProperties properties,
                                                        StreamingAnswerGenerator streaming,
                                                        RetrievalResult... hits) {
        HybridRetrievalEngine engine = new HybridRetrievalEngine(
                List.of(new FixedRetriever(List.of(hits))), new RrfFusionStrategy(), 2, 15_000);
        return new DefaultRagPipeline(engine, new NoopReranker(),
                new ContextAssembler(properties.getContext().getMaxLength(),
                        properties.getContext().getDocumentTemplate()),
                (query, context) -> "P:" + context,
                (prompt, timeoutMillis) -> "unused",
                properties, new TrimQueryTransformer(),
                result -> result, (answer, assembly) -> answer + "|cited",
                streaming, new NoopRagMetrics());
    }

    private static RagProperties properties() {
        return new RagProperties();
    }

    private static RetrievalResult hit(String id) {
        return RetrievalResult.builder()
                .id(id).content("内容" + id).metadata(Map.of()).score(1.0).source("vector").build();
    }

    /** 返回固定结果的检索器替身 */
    private static final class FixedRetriever implements Retriever {

        private final List<RetrievalResult> results;

        FixedRetriever(List<RetrievalResult> results) {
            this.results = results;
        }

        @Override
        public List<RetrievalResult> retrieve(String query, int topK, Map<String, Object> filter) {
            return results;
        }

        @Override
        public String getName() {
            return "fixed";
        }

        @Override
        public double getWeight() {
            return 1.0;
        }
    }
}

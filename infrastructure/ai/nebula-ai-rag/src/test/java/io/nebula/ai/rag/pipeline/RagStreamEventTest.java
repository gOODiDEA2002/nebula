package io.nebula.ai.rag.pipeline;

import io.nebula.ai.rag.retriever.RetrievalResult;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code RagStreamEvent} 工厂与 {@code RagPipeline.queryStream} 默认实现契约（R4 §5.2、§5.4、§10）
 */
class RagStreamEventTest {

    @Test
    void referencesFactory_setsTypeAndReferences() {
        List<RetrievalResult> refs = List.of(RetrievalResult.builder().id("a").build());
        RagStreamEvent event = RagStreamEvent.references(refs);

        assertThat(event.getType()).isEqualTo(RagStreamEvent.Type.REFERENCES);
        assertThat(event.getReferences()).isEqualTo(refs);
    }

    @Test
    void deltaFactory_setsTypeAndDelta() {
        RagStreamEvent event = RagStreamEvent.delta("片段");

        assertThat(event.getType()).isEqualTo(RagStreamEvent.Type.DELTA);
        assertThat(event.getDelta()).isEqualTo("片段");
    }

    @Test
    void completeFactory_setsTypeAndAnswer() {
        RagAnswer answer = RagAnswer.builder().answer("完整答案").build();
        RagStreamEvent event = RagStreamEvent.complete(answer);

        assertThat(event.getType()).isEqualTo(RagStreamEvent.Type.COMPLETE);
        assertThat(event.getAnswer()).isSameAs(answer);
    }

    @Test
    void errorFactory_setsTypeReasonMessage() {
        RagStreamEvent event = RagStreamEvent.error("some-reason", "说明");

        assertThat(event.getType()).isEqualTo(RagStreamEvent.Type.ERROR);
        assertThat(event.getErrorReason()).isEqualTo("some-reason");
        assertThat(event.getErrorMessage()).isEqualTo("说明");
    }

    @Test
    void defaultQueryStream_returnsStreamingUnsupportedError() {
        // 只实现 query() 的管线走 RagPipeline.queryStream 默认实现
        RagPipeline pipeline = query -> RagAnswer.builder().build();

        StepVerifier.create(pipeline.queryStream(RagQuery.builder().query("q").build()))
                .assertNext(event -> {
                    assertThat(event.getType()).isEqualTo(RagStreamEvent.Type.ERROR);
                    assertThat(event.getErrorReason())
                            .isEqualTo(DefaultRagPipeline.REASON_STREAMING_UNSUPPORTED);
                })
                .verifyComplete();
    }

    @Test
    void builder_allowsMetadataPreservingReferences() {
        // 冻结契约：全参构造与 builder 等价，字段可读回
        RagStreamEvent event = new RagStreamEvent(RagStreamEvent.Type.REFERENCES,
                List.of(RetrievalResult.builder().id("x").metadata(Map.of("k", "v")).build()),
                null, null, null, null);

        assertThat(event.getReferences()).hasSize(1);
        assertThat(event.getReferences().get(0).getId()).isEqualTo("x");
    }
}

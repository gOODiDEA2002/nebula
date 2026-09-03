package io.nebula.ai.rag.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Micrometer 指标实现契约（R4 §6.2、§10）：三个固定 meter 名、有限枚举标签、null 标签兜底为 unknown。
 * 用 {@link SimpleMeterRegistry} 读回 Timer/Counter 计数与标签。
 */
class MicrometerRagMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final MicrometerRagMetrics metrics = new MicrometerRagMetrics(registry);

    @Test
    void constructor_rejectsNullRegistry() {
        assertThatThrownBy(() -> new MicrometerRagMetrics(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recordStage_registersTimerWithStageAndOutcomeTags() {
        metrics.recordStage("rerank", 1_000_000L, "success");

        Timer timer = registry.find(MicrometerRagMetrics.STAGE_TIMER)
                .tag("stage", "rerank").tag("outcome", "success").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(TimeUnit.NANOSECONDS)).isEqualTo(1_000_000L);
    }

    @Test
    void recordQuery_registersTimerWithDegradedAndReasonTags() {
        metrics.recordQuery(2_000_000L, true, "no-document");

        Timer timer = registry.find(MicrometerRagMetrics.QUERY_TIMER)
                .tag("degraded", "true").tag("reason", "no-document").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void recordRerankPassthrough_incrementsCounterWithReasonTag() {
        metrics.recordRerankPassthrough("timeout");
        metrics.recordRerankPassthrough("timeout");

        Counter counter = registry.find(MicrometerRagMetrics.RERANK_PASSTHROUGH_COUNTER)
                .tag("reason", "timeout").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    void nullTagValue_fallsBackToUnknown() {
        metrics.recordStage(null, 1L, null);
        metrics.recordRerankPassthrough(null);

        assertThat(registry.find(MicrometerRagMetrics.STAGE_TIMER)
                .tag("stage", "unknown").tag("outcome", "unknown").timer()).isNotNull();
        assertThat(registry.find(MicrometerRagMetrics.RERANK_PASSTHROUGH_COUNTER)
                .tag("reason", "unknown").counter()).isNotNull();
    }
}

package io.nebula.ai.rag.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Micrometer 的指标实现（R4 §6.2）
 * <p>
 * <b>全模块只有本类 import {@code io.micrometer} 类型</b>：装配层用
 * {@code @ConditionalOnClass(MeterRegistry)} 守护本类加载，Micrometer 缺席时走
 * {@link NoopRagMetrics} 不触碰本类，因此不会有类加载失败（R4 §9 攻击面 18）。
 * <p>
 * 三个 meter 名固定、标签全部有限枚举，绝不带无界维度：
 * <ul>
 *   <li>{@code nebula.rag.stage.duration}（Timer，标签 {@code stage}/{@code outcome}）</li>
 *   <li>{@code nebula.rag.query.duration}（Timer，标签 {@code degraded}/{@code reason}）</li>
 *   <li>{@code nebula.rag.rerank.passthrough}（Counter，标签 {@code reason}）</li>
 * </ul>
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class MicrometerRagMetrics implements RagMetrics {

    /** 阶段耗时 Timer 名 */
    public static final String STAGE_TIMER = "nebula.rag.stage.duration";

    /** 整次查询耗时 Timer 名 */
    public static final String QUERY_TIMER = "nebula.rag.query.duration";

    /** HTTP 重排直通 Counter 名 */
    public static final String RERANK_PASSTHROUGH_COUNTER = "nebula.rag.rerank.passthrough";

    private final MeterRegistry registry;

    public MicrometerRagMetrics(MeterRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("MeterRegistry 不能为空");
        }
        this.registry = registry;
    }

    @Override
    public void recordStage(String stage, long durationNanos, String outcome) {
        Timer.builder(STAGE_TIMER)
                .tag("stage", safe(stage))
                .tag("outcome", safe(outcome))
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordQuery(long durationNanos, boolean degraded, String reason) {
        Timer.builder(QUERY_TIMER)
                .tag("degraded", Boolean.toString(degraded))
                .tag("reason", safe(reason))
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordRerankPassthrough(String reason) {
        Counter.builder(RERANK_PASSTHROUGH_COUNTER)
                .tag("reason", safe(reason))
                .register(registry)
                .increment();
    }

    /** 标签兜底：null 归一化为 {@code unknown}，避免 Micrometer 拒绝 null 标签值 */
    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}

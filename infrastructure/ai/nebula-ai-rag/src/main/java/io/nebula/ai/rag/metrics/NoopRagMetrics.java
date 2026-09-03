package io.nebula.ai.rag.metrics;

/**
 * 不采集任何指标的默认实现（R4 §6.1）
 * <p>
 * 默认装配它而不是 Micrometer 版本：指标依赖 {@code MeterRegistry}，是否采集由应用决定，
 * 框架不替应用默认拉起一套监控。本类不触碰任何 {@code io.micrometer} 类型，因此
 * Micrometer 缺席时也能正常加载（R4 §9 攻击面 18）。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class NoopRagMetrics implements RagMetrics {

    @Override
    public void recordStage(String stage, long durationNanos, String outcome) {
        // 有意为空
    }

    @Override
    public void recordQuery(long durationNanos, boolean degraded, String reason) {
        // 有意为空
    }

    @Override
    public void recordRerankPassthrough(String reason) {
        // 有意为空
    }
}

package io.nebula.ai.rag.metrics;

/**
 * RAG 管线内部指标端口（R4 §6.1）
 * <p>
 * 这是<b>内部</b>端口：不对外承诺稳定，消费方从 {@code MeterRegistry} 读指标而不是实现本接口。
 * 抽成端口只为把 Micrometer 依赖挡在管线之外 —— 缺 Micrometer 时装配 {@link NoopRagMetrics}，
 * 管线代码一行不改。标签取值一律是有限枚举，绝不带查询文本、URL 等无界值（R4 §6.2）。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public interface RagMetrics {

    /**
     * 记录单个阶段耗时
     *
     * @param stage         阶段名：{@code retrieval} | {@code rerank} | {@code assemble} | {@code generation}
     * @param durationNanos 耗时（纳秒）
     * @param outcome       结果：{@code success} | {@code failure} | {@code timeout} | {@code cancelled} | {@code passthrough}
     */
    void recordStage(String stage, long durationNanos, String outcome);

    /**
     * 记录整次查询耗时
     *
     * @param durationNanos 耗时（纳秒）
     * @param degraded      是否降级
     * @param reason        降级原因；未降级时为 {@code none}
     */
    void recordQuery(long durationNanos, boolean degraded, String reason);

    /**
     * 记录一次 HTTP 重排直通（融合原序截断）
     *
     * @param reason 直通原因：{@code timeout} | {@code http-error} | {@code decode-error} | {@code mismatch}
     */
    void recordRerankPassthrough(String reason);
}

package io.nebula.ai.rag.pipeline;

import reactor.core.publisher.Flux;

/**
 * RAG 流程编排接口
 * <p>
 * 完整流程：查询预处理 -> 多路检索 -> 重排序 -> 上下文构建 -> 答案生成。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public interface RagPipeline {

    /**
     * 执行 RAG 查询
     *
     * @param query 查询请求
     * @return 查询结果
     */
    RagAnswer query(RagQuery query);

    /**
     * 流式执行 RAG 查询（R4 §5.4）
     * <p>
     * 默认实现返回单个「暂不支持」{@code ERROR} 事件后正常完成：未覆写本方法或未装配
     * {@code StreamingAnswerGenerator} 的实现走此路径。覆写实现按 R4 §5.3 事件契约产出。
     *
     * @param query 查询请求
     * @return 流式事件序列
     */
    default Flux<RagStreamEvent> queryStream(RagQuery query) {
        return Flux.just(RagStreamEvent.error(DefaultRagPipeline.REASON_STREAMING_UNSUPPORTED,
                "当前 RagPipeline 实现不支持流式"));
    }
}

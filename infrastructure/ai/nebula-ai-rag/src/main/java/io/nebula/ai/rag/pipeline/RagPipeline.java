package io.nebula.ai.rag.pipeline;

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
}

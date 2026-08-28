package io.nebula.ai.rag.rerank;

import io.nebula.ai.rag.retriever.RetrievalResult;

import java.util.List;

/**
 * 重排序器接口
 * <p>
 * 对初始检索结果进行精细排序，提升最终输出的相关性。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public interface Reranker {

    /**
     * 对检索结果重新排序
     *
     * @param query   原始查询
     * @param results 待排序的检索结果
     * @param topK    返回结果数量
     * @return 重排序后的结果列表
     */
    List<RetrievalResult> rerank(String query, List<RetrievalResult> results, int topK);

    /**
     * 重排序器名称
     */
    String getName();
}

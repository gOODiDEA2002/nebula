package io.nebula.ai.rag.eval;

import io.nebula.ai.rag.retriever.RetrievalResult;

import java.util.List;

/**
 * 被评测的检索函数
 * <p>
 * 抽成单方法端口而不是直接吃 {@code Retriever} 或 {@code HybridRetrievalEngine}：
 * 评测对象可能是单个检索器、多路引擎、整条管线，甚至是一段内存里的桩实现，
 * 它们唯一的共同点就是「给查询与深度、还回一批带 ID 的结果」。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
@FunctionalInterface
public interface RetrievalFunction {

    /**
     * 执行一次检索
     *
     * @param query 查询文本
     * @param topK  返回数量上限
     * @return 按相关性降序排列的结果；无结果返回空表而不是 null
     */
    List<RetrievalResult> retrieve(String query, int topK);
}

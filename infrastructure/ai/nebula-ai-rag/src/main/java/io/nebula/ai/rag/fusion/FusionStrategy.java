package io.nebula.ai.rag.fusion;

import io.nebula.ai.rag.retriever.RetrievalResult;

import java.util.List;

/**
 * 多路检索结果融合策略
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public interface FusionStrategy {

    /**
     * 融合多路检索结果
     *
     * @param resultLists 各路检索结果，顺序与 weights 一一对应
     * @param weights     各路权重
     * @param topK        融合后保留数量
     * @return 融合后的结果列表
     */
    List<RetrievalResult> fuse(List<List<RetrievalResult>> resultLists,
                               List<Double> weights, int topK);
}

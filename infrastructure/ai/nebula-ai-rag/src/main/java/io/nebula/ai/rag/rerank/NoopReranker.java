package io.nebula.ai.rag.rerank;

import io.nebula.ai.rag.retriever.RetrievalResult;

import java.util.List;

/**
 * 不做重排、只做截断的默认重排序器
 * <p>
 * 框架默认装配它而不是 LLM 版本：重排要花一次额外的模型调用，
 * 是否值得由应用决定，框架不替应用默认掏这笔钱。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class NoopReranker implements Reranker {

    @Override
    public List<RetrievalResult> rerank(String query, List<RetrievalResult> results, int topK) {
        if (results == null || results.isEmpty() || topK <= 0) {
            return List.of();
        }
        return results.stream().limit(topK).toList();
    }

    @Override
    public String getName() {
        return "NoopReranker";
    }
}

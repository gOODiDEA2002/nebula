package io.nebula.ai.rag.retriever;

import java.util.List;
import java.util.Map;

/**
 * 检索器接口
 * <p>
 * 定义从知识库中检索相关文档的能力，各实现对应不同的检索策略（向量 / 关键词 / 图谱 / ...）。
 * 多个实现同时存在时由 {@code HybridRetrievalEngine} 并行调用并融合。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public interface Retriever {

    /**
     * 带过滤条件的检索
     *
     * @param query  查询文本
     * @param topK   返回数量
     * @param filter 过滤条件，可为 null
     * @return 检索结果列表；无结果返回空表而不是 null
     */
    List<RetrievalResult> retrieve(String query, int topK, Map<String, Object> filter);

    /**
     * 检索器名称，用于日志与 {@code source} 标注
     */
    String getName();

    /**
     * 检索器权重（用于结果融合）
     */
    double getWeight();

    /**
     * 单路检索超时（毫秒）
     * <p>
     * 慢查询必须被单路截断，否则一路卡死会拖垮整条 RAG 链路。
     * 返回非正数表示沿用引擎的默认超时（{@code nebula.ai.rag.retrieval.timeout-seconds}）；
     * 只有需要偏离全局默认的检索器才覆盖本方法。
     */
    default long timeoutMillis() {
        return 0;
    }
}

package io.nebula.ai.rag.guard;

import io.nebula.ai.rag.retriever.RetrievalResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 检索内容进入重排与上下文前的清洗端口（R4 §4.1）
 * <p>
 * 清洗位置固定在<b>融合之后、重排之前</b>（R4-D4）：重排会把候选正文送进 LLM 打分，
 * 若在重排后才清洗，注入文本已经进过一次模型。清洗后的列表同时供重排、上下文与
 * {@code references} 使用，避免前端展示未清洗文本又被复制回提示词。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
@FunctionalInterface
public interface RetrievedContentSanitizer {

    /**
     * 清洗单条检索结果
     *
     * @param result 待清洗结果
     * @return 清洗后的结果；返回 {@code null} 表示将该条从候选中剔除
     */
    RetrievalResult sanitize(RetrievalResult result);

    /**
     * 逐条清洗并过滤被剔除项，保持原顺序
     *
     * @param results 待清洗列表
     * @return 清洗后的列表；入参为 {@code null} 时返回空列表
     */
    default List<RetrievalResult> sanitizeAll(List<RetrievalResult> results) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        List<RetrievalResult> sanitized = new ArrayList<>(results.size());
        for (RetrievalResult result : results) {
            RetrievalResult cleaned = sanitize(result);
            if (cleaned != null) {
                sanitized.add(cleaned);
            }
        }
        return sanitized;
    }
}

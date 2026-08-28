package io.nebula.ai.rag.retriever;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 检索结果
 * <p>
 * {@link #id} 是跨路对齐的唯一依据：多路检索的融合按它去重合并，
 * 各检索器必须返回同一套业务 ID（向量库若做过点 ID 映射，要还原成原始 docId 再返回）。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalResult {

    /** 文档ID */
    private String id;

    /** 文档内容 */
    private String content;

    /** 元数据 */
    private Map<String, Object> metadata;

    /** 相关性分数 */
    private double score;

    /** 数据来源 (vector/keyword/graph/hybrid) */
    private String source;
}

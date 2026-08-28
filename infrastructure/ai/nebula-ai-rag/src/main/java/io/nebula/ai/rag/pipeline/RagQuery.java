package io.nebula.ai.rag.pipeline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * RAG 查询请求
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagQuery {

    /** 查询文本 */
    private String query;

    /** 检索数量；为 null 时用 nebula.ai.rag.retrieval.top-k */
    private Integer topK;

    /** 检索过滤条件 */
    private Map<String, Object> filter;

    /** 是否重排；为 null 时用 nebula.ai.rag.rerank.enabled */
    private Boolean enableRerank;

    /**
     * 是否生成答案
     * <p>
     * 置 false 时只走检索与重排，用于「只要引用不要答案」的场景，省掉一次模型调用。
     */
    @Builder.Default
    private boolean generateAnswer = true;

    /**
     * 创建只带查询文本的请求
     */
    public static RagQuery of(String query) {
        return RagQuery.builder().query(query).build();
    }
}

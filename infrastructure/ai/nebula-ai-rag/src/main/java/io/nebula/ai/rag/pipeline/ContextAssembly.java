package io.nebula.ai.rag.pipeline;

import io.nebula.ai.rag.retriever.RetrievalResult;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 上下文组装结果（R4 §4.2）
 * <p>
 * 在拼接正文之外，额外给出<b>实际入选</b>的引用与序号映射：序号与
 * {@code documentTemplate} 的第一个 {@code %d} 一致，从 1 起。因预算未入选的条数由
 * {@link #getOmittedCount()} 给出，供 {@code references-mode=included} 与
 * {@code CitationPostProcessor} 精确定位引用，无需给 {@code RagAnswer} 加字段（R4-D5）。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public final class ContextAssembly {

    private final String context;
    private final List<RetrievalResult> includedReferences;
    private final Map<Integer, RetrievalResult> citationMap;
    private final int omittedCount;

    public ContextAssembly(String context, List<RetrievalResult> includedReferences,
                           Map<Integer, RetrievalResult> citationMap, int omittedCount) {
        this.context = context == null ? "" : context;
        this.includedReferences = includedReferences == null ? List.of() : List.copyOf(includedReferences);
        this.citationMap = citationMap == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(citationMap));
        this.omittedCount = omittedCount;
    }

    /** 拼接后的上下文正文（与 {@code ContextAssembler.assemble} 逐字相同） */
    public String getContext() {
        return context;
    }

    /** 实际进入上下文的引用，顺序与其在 {@link #getContext()} 中出现的顺序一致 */
    public List<RetrievalResult> getIncludedReferences() {
        return includedReferences;
    }

    /** 序号到引用的映射，1 对应第一篇 */
    public Map<Integer, RetrievalResult> getCitationMap() {
        return citationMap;
    }

    /** 因预算未入选的条数 */
    public int getOmittedCount() {
        return omittedCount;
    }
}

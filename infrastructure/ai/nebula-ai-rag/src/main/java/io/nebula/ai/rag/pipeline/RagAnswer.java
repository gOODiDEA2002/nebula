package io.nebula.ai.rag.pipeline;

import io.nebula.ai.rag.retriever.RetrievalResult;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG 查询结果
 * <p>
 * {@link #degraded} 与 {@link #degradeReason} 是给调用方而不是给日志看的：
 * 降级答案在文本上和正常答案难以区分，不显式标出来，上游就没法决定是否重试、
 * 是否给用户提示、是否计入质量统计。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagAnswer {

    /** 生成的答案；generateAnswer=false 时为 null */
    private String answer;

    /** 引用的检索结果 */
    private List<RetrievalResult> references;

    /** 本次查询耗时（毫秒） */
    private long costMs;

    /** 是否为降级结果 */
    private boolean degraded;

    /** 降级原因 */
    private String degradeReason;
}

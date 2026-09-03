package io.nebula.ai.rag.pipeline;

import io.nebula.ai.rag.retriever.RetrievalResult;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG 流式事件（R4 §5.2）
 * <p>
 * 一次流式查询恰好一个 {@link Type#REFERENCES} 在最前，随后 0..n 个 {@link Type#DELTA}，
 * 最后恰好一个终态（{@link Type#COMPLETE} 或 {@link Type#ERROR}），终态后 Flux 正常完成。
 * 生成失败/超时以 {@code ERROR} 事件或降级 {@code COMPLETE} 表达而不走 {@code Flux.onError}，
 * 保证传输层总能收到格式完整的结尾（R4 §5.3）。
 * <p>
 * 本类是本期首次出现的新类，一旦发布即冻结：后续加字段须走 builder，不得依赖全参构造器顺序。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagStreamEvent {

    /**
     * 事件类型
     */
    public enum Type {
        /** 引用清单，最前恰好一个 */
        REFERENCES,
        /** 文本增量，0..n 个 */
        DELTA,
        /** 成功终态：answer 为拼接后的完整文本（已过 CitationPostProcessor） */
        COMPLETE,
        /** 错误终态：errorReason 与 DefaultRagPipeline.REASON_* 同一套常量 */
        ERROR
    }

    /** 事件类型 */
    private Type type;

    /** REFERENCES 事件的引用清单 */
    private List<RetrievalResult> references;

    /** DELTA 事件的文本增量 */
    private String delta;

    /** COMPLETE 事件的完整答案 */
    private RagAnswer answer;

    /** ERROR 事件的原因码（与 DefaultRagPipeline.REASON_* 一致） */
    private String errorReason;

    /** ERROR 事件的错误说明 */
    private String errorMessage;

    /** 构造 REFERENCES 事件 */
    public static RagStreamEvent references(List<RetrievalResult> references) {
        return RagStreamEvent.builder().type(Type.REFERENCES).references(references).build();
    }

    /** 构造 DELTA 事件 */
    public static RagStreamEvent delta(String delta) {
        return RagStreamEvent.builder().type(Type.DELTA).delta(delta).build();
    }

    /** 构造 COMPLETE 事件 */
    public static RagStreamEvent complete(RagAnswer answer) {
        return RagStreamEvent.builder().type(Type.COMPLETE).answer(answer).build();
    }

    /** 构造 ERROR 事件 */
    public static RagStreamEvent error(String errorReason, String errorMessage) {
        return RagStreamEvent.builder().type(Type.ERROR).errorReason(errorReason).errorMessage(errorMessage).build();
    }
}

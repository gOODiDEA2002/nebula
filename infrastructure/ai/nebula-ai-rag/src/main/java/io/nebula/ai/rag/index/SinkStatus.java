package io.nebula.ai.rag.index;

/**
 * 文档在某个 {@link IndexSink} 上的写入状态
 * <p>
 * 一个文档必须在全部必需 sink 上都是 {@link #DONE} 才算完成（上位设计 §4 决策 4）。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public enum SinkStatus {

    /** 已成功写入该 sink */
    DONE,

    /** 尚未写入或写入未完成 */
    PENDING
}

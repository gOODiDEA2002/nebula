package io.nebula.ai.rag.index;

import java.util.List;

/**
 * 文档源：每次调用 {@link #snapshot()} 返回一份完整一致的快照（J7）
 * <p>
 * 快照语义意味着「快照里没有的文档 = 已删除」，索引管线据此对齐删除，无需独立 tombstone。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public interface DocumentSource {

    /**
     * 源名称，用作状态库的分区键
     */
    String name();

    /**
     * 返回完整一致快照
     */
    List<SourceDocument> snapshot();
}

package io.nebula.ai.rag.index;

import io.nebula.ai.rag.chunking.DocumentChunk;

import java.util.List;

/**
 * 写目标（索引后端）
 * <p>
 * <b>幂等契约（J8，逐实现测试钉住）：</b>
 * <ul>
 *   <li>{@code upsert} 同 ID 重复调用 = 覆盖；</li>
 *   <li>{@code delete} 不存在的 ID = 静默成功；</li>
 *   <li>批内部分失败必须抛出（异常需带上已成功 ID 清单，便于重入对齐）。</li>
 * </ul>
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public interface IndexSink {

    /**
     * sink 名称，用作 {@link DocIndexState#getSinkStatus()} 的键
     */
    String name();

    /**
     * 写入（覆盖）一个文档的全部块
     *
     * @param docId  业务文档 ID
     * @param chunks 该文档的块（已带确定性 ID）
     * @throws IndexSinkException 批内部分失败
     */
    void upsert(String docId, List<DocumentChunk> chunks);

    /**
     * 删除一个文档的指定块
     *
     * @param docId    业务文档 ID
     * @param chunkIds 待删除的块 ID；空表示无需删除
     * @throws IndexSinkException 批内部分失败
     */
    void delete(String docId, List<String> chunkIds);
}

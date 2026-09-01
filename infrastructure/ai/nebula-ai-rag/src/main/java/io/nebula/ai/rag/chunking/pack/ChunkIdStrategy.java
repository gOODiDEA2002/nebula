package io.nebula.ai.rag.chunking.pack;

import io.nebula.ai.rag.chunking.DocumentChunk;

import java.util.UUID;

/**
 * 块 ID 生成策略
 * <p>
 * 默认保持<b>随机</b>而不是确定性，是为了守住「默认行为不变」：现有的
 * {@code DocumentChunk} 构造器就是给随机 UUID，把默认改成确定性会让所有既有调用方
 * 在毫无察觉的情况下换掉一整套块 ID。
 * <p>
 * 确定性策略是评测与增量索引的前置条件：金标按 ID 前缀判中，索引差分按 ID 对齐，
 * 两者都要求「同一份文档重复切分产出同一套 ID」。需要它的场景显式选用。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
@FunctionalInterface
public interface ChunkIdStrategy {

    /**
     * 生成块 ID
     *
     * @param docId 文档标识
     * @param index 块在本文档内的序号，从 0 起算
     * @param chunk 已填好内容的块（ID 尚未写入）
     * @return 块 ID
     */
    String chunkId(String docId, int index, DocumentChunk chunk);

    /**
     * 随机 ID：{@code chunk-<UUID>}，与 {@link DocumentChunk} 构造器的现状语义一致
     */
    static ChunkIdStrategy random() {
        return (docId, index, chunk) -> "chunk-" + UUID.randomUUID();
    }

    /**
     * 确定性 ID：{@code <docId>#<index>}
     */
    static ChunkIdStrategy deterministic() {
        return (docId, index, chunk) -> docId + "#" + index;
    }
}

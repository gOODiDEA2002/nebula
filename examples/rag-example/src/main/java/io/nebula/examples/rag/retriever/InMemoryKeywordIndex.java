package io.nebula.examples.rag.retriever;

import io.nebula.ai.rag.chunking.DocumentChunk;
import io.nebula.ai.rag.index.IndexSink;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存关键词写目标（{@link IndexSink}，name = {@code keyword-memory}）。
 * <p>
 * 实现 {@code IndexSink} 即被框架 {@code indexingPipeline} 收集为第二写目标，演示扩展点
 * 「实现即纳入」。同时是 {@link InMemoryKeywordRetriever} 的数据源：{@code upsert} 存块、
 * {@code delete} 按块 ID 删，与向量库形成双 sink，二者共享同一份确定性块 ID。
 * <p>
 * 幂等契约（J8）：同 docId 的 {@code upsert} 覆盖旧块；{@code delete} 不存在的 ID 静默成功。
 *
 * @author Nebula Framework
 */
public class InMemoryKeywordIndex implements IndexSink {

    /** sink 名称，用作状态库 sinkStatus 的键 */
    public static final String NAME = "keyword-memory";

    /** chunkId -> 存储条目 */
    private final Map<String, StoredChunk> chunks = new ConcurrentHashMap<>();

    /** docId -> 该文档当前的 chunkId 列表 */
    private final Map<String, List<String>> docChunkIds = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void upsert(String docId, List<DocumentChunk> docChunks) {
        // 覆盖语义：先清掉该文档旧块，再写新块
        removeDocChunks(docId);
        List<String> ids = new ArrayList<>();
        for (DocumentChunk chunk : docChunks) {
            String title = chunk.getTitle() == null ? "" : chunk.getTitle();
            chunks.put(chunk.getId(), new StoredChunk(chunk.getId(), docId, title, chunk.getContent()));
            ids.add(chunk.getId());
        }
        docChunkIds.put(docId, ids);
    }

    @Override
    public void delete(String docId, List<String> chunkIds) {
        if (chunkIds != null) {
            for (String id : chunkIds) {
                chunks.remove(id);
            }
        }
        List<String> remaining = docChunkIds.get(docId);
        if (remaining != null && chunkIds != null) {
            remaining.removeAll(chunkIds);
            if (remaining.isEmpty()) {
                docChunkIds.remove(docId);
            }
        }
    }

    /** 返回全部已存块的只读快照，供关键词检索器打分 */
    public List<StoredChunk> allChunks() {
        return new ArrayList<>(chunks.values());
    }

    private void removeDocChunks(String docId) {
        List<String> old = docChunkIds.remove(docId);
        if (old != null) {
            for (String id : old) {
                chunks.remove(id);
            }
        }
    }

    /**
     * 内存块存储条目。
     *
     * @param chunkId 确定性块 ID（{@code <docId>#<序号>}）
     * @param docId   业务文档 ID
     * @param title   块标题（面包屑），可为空串
     * @param content 块正文
     */
    public record StoredChunk(String chunkId, String docId, String title, String content) {
    }
}

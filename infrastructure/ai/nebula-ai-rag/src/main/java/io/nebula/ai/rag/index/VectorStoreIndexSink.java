package io.nebula.ai.rag.index;

import io.nebula.ai.core.model.Document;
import io.nebula.ai.core.vectorstore.VectorStoreService;
import io.nebula.ai.rag.chunking.DocumentChunk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量库写目标（详细设计 §2.3）
 * <p>
 * {@link DocumentChunk} → {@link Document}（content=块内容；metadata 并入 breadcrumb/title/
 * docId/chunkType）；delete 走 {@link VectorStoreService#deleteAll(List)}。
 * <p>
 * <b>J10 醒目声明：</b>本 sink 经 {@link VectorStoreService} 抽象，<b>无法探测后端是否 Qdrant、
 * 是否开启 id-mapping</b>。要保证「同一块 ID 重复 upsert = 覆盖」以及删除对齐，后端必须支持
 * 按业务 ID upsert/删除（Qdrant 需开启 id-mapping 装饰器，把业务 ID 稳定映射为点 ID）。
 * 用错后端（如未开 id-mapping 的 Qdrant）会导致重复写入产生重复点、删除删不掉，
 * 这类风险本框架无法在启动期拦截，只能靠正确配置规避。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class VectorStoreIndexSink implements IndexSink {

    /** sink 名称 */
    public static final String NAME = "vector-store";

    private final VectorStoreService vectorStoreService;

    public VectorStoreIndexSink(VectorStoreService vectorStoreService) {
        if (vectorStoreService == null) {
            throw new IllegalArgumentException("VectorStoreService 不能为空");
        }
        this.vectorStoreService = vectorStoreService;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void upsert(String docId, List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        List<String> succeeded = new ArrayList<>(chunks.size());
        for (DocumentChunk chunk : chunks) {
            try {
                boolean ok = vectorStoreService.add(toDocument(docId, chunk));
                if (!ok) {
                    throw new IndexSinkException(NAME, docId, succeeded,
                            "向量库 add 返回 false, 块 ID=" + chunk.getId(), null);
                }
                succeeded.add(chunk.getId());
            } catch (IndexSinkException e) {
                throw e;
            } catch (Exception e) {
                throw new IndexSinkException(NAME, docId, succeeded,
                        "块 ID=" + chunk.getId() + " 写入异常: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void delete(String docId, List<String> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return;
        }
        vectorStoreService.deleteAll(chunkIds);
    }

    private Document toDocument(String docId, DocumentChunk chunk) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (chunk.getMetadata() != null) {
            metadata.putAll(chunk.getMetadata());
        }
        metadata.put("docId", docId);
        if (chunk.getTitle() != null) {
            metadata.put("title", chunk.getTitle());
        }
        if (chunk.getChunkType() != null) {
            metadata.put("chunkType", chunk.getChunkType().getCode());
        }
        // breadcrumb 已在 chunk.metadata（PackOptions.META_BREADCRUMB 键）里，随 putAll 带入
        return Document.builder()
                .id(chunk.getId())
                .content(chunk.getContent() != null ? chunk.getContent() : "")
                .metadata(metadata)
                .build();
    }
}

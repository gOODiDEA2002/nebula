package io.nebula.ai.rag.index;

import io.nebula.ai.core.model.Document;
import io.nebula.ai.core.vectorstore.VectorStoreService;
import io.nebula.ai.rag.chunking.ChunkType;
import io.nebula.ai.rag.chunking.DocumentChunk;
import io.nebula.ai.rag.chunking.pack.PackOptions;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * VectorStoreIndexSink 幂等契约（P2，详细设计 §2.3、§7）
 * <p>
 * 桩断言收到的 Document（id/content/metadata）与 delete 的 ID 集；部分失败必须抛出并带上已成功 ID。
 */
class VectorStoreIndexSinkTest {

    @Test
    void upsert_mapsChunksToDocumentsWithMergedMetadata() {
        VectorStoreService vectorStore = mock(VectorStoreService.class);
        when(vectorStore.add(any(Document.class))).thenReturn(true);
        VectorStoreIndexSink sink = new VectorStoreIndexSink(vectorStore);

        sink.upsert("doc-1", List.of(chunk("doc-1#0", "第一块", "标题A"),
                chunk("doc-1#1", "第二块", "标题B")));

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(vectorStore, times(2)).add(captor.capture());
        List<Document> docs = captor.getAllValues();
        assertThat(docs).extracting(Document::getId).containsExactly("doc-1#0", "doc-1#1");
        assertThat(docs.get(0).getContent()).isEqualTo("第一块");
        assertThat(docs.get(0).getMetadata())
                .containsEntry("docId", "doc-1")
                .containsEntry("title", "标题A")
                .containsEntry("chunkType", ChunkType.SECTION.getCode())
                .containsKey(PackOptions.META_BREADCRUMB);
    }

    @Test
    void repeatedUpsert_reissuesAddsForSameIds() {
        VectorStoreService vectorStore = mock(VectorStoreService.class);
        when(vectorStore.add(any(Document.class))).thenReturn(true);
        VectorStoreIndexSink sink = new VectorStoreIndexSink(vectorStore);
        List<DocumentChunk> chunks = List.of(chunk("doc-1#0", "块", "标题"));

        sink.upsert("doc-1", chunks);
        sink.upsert("doc-1", chunks);

        // 同 ID 两次 add：覆盖由后端 upsert 语义负责，sink 只需如实按 ID 重发
        verify(vectorStore, times(2)).add(any(Document.class));
    }

    @Test
    void delete_callsDeleteAllWithChunkIds() {
        VectorStoreService vectorStore = mock(VectorStoreService.class);
        VectorStoreIndexSink sink = new VectorStoreIndexSink(vectorStore);

        sink.delete("doc-1", List.of("doc-1#0", "doc-1#1"));

        verify(vectorStore).deleteAll(List.of("doc-1#0", "doc-1#1"));
    }

    @Test
    void deleteEmpty_isSilentNoop() {
        VectorStoreService vectorStore = mock(VectorStoreService.class);
        VectorStoreIndexSink sink = new VectorStoreIndexSink(vectorStore);

        sink.delete("doc-1", List.of());

        verify(vectorStore, never()).deleteAll(anyList());
    }

    @Test
    void partialFailure_throwsWithSucceededIds() {
        VectorStoreService vectorStore = mock(VectorStoreService.class);
        // 第一块成功、第二块失败
        when(vectorStore.add(any(Document.class))).thenReturn(true).thenReturn(false);
        VectorStoreIndexSink sink = new VectorStoreIndexSink(vectorStore);

        assertThatThrownBy(() -> sink.upsert("doc-1", List.of(
                chunk("doc-1#0", "块0", "t0"), chunk("doc-1#1", "块1", "t1"))))
                .isInstanceOf(IndexSinkException.class)
                .satisfies(e -> assertThat(((IndexSinkException) e).getSucceededIds())
                        .containsExactly("doc-1#0"));
    }

    private static DocumentChunk chunk(String id, String content, String title) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(id);
        chunk.setContent(content);
        chunk.setTitle(title);
        chunk.setChunkType(ChunkType.SECTION);
        chunk.addMetadata(PackOptions.META_BREADCRUMB, List.of("根", title));
        return chunk;
    }
}

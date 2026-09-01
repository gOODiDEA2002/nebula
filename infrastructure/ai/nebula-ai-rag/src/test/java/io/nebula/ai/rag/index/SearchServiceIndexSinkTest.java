package io.nebula.ai.rag.index;

import io.nebula.ai.rag.chunking.ChunkType;
import io.nebula.ai.rag.chunking.DocumentChunk;
import io.nebula.ai.rag.chunking.pack.PackOptions;
import io.nebula.ai.rag.retriever.RagSearchDocument;
import io.nebula.search.core.SearchService;
import io.nebula.search.core.model.DocumentResult;
import io.nebula.search.core.model.IndexMapping;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SearchServiceIndexSink 幂等契约（P2，详细设计 §2.3、§7）
 * <p>
 * 桩断言：索引不存在时用默认 mapping 建索引；逐块写 RagSearchDocument（含 id/docId/content/
 * breadcrumb/chunkType）；delete 走 bulkDelete 且删不存在静默；部分失败抛出并带已成功 ID。
 */
class SearchServiceIndexSinkTest {

    private static final String INDEX = "rag-chunks";

    @Test
    void upsert_createsIndexThenWritesEachChunk() {
        SearchService searchService = mock(SearchService.class);
        when(searchService.indexExists(INDEX)).thenReturn(false);
        when(searchService.indexDocument(eq(INDEX), anyString(), any())).thenReturn(ok());
        SearchServiceIndexSink sink = new SearchServiceIndexSink(searchService, INDEX,
                "ik_max_word", "ik_smart");

        sink.upsert("doc-1", List.of(chunk("doc-1#0", "第一块", "标题A"),
                chunk("doc-1#1", "第二块", "标题B")));

        // 索引不存在 → 用默认 mapping 建索引
        ArgumentCaptor<IndexMapping> mappingCaptor = ArgumentCaptor.forClass(IndexMapping.class);
        verify(searchService).createIndex(eq(INDEX), mappingCaptor.capture());
        assertThat(mappingCaptor.getValue().getProperties()).containsKey("properties");

        // 逐块写入
        ArgumentCaptor<RagSearchDocument> docCaptor = ArgumentCaptor.forClass(RagSearchDocument.class);
        verify(searchService, times(2)).indexDocument(eq(INDEX), anyString(), docCaptor.capture());
        List<RagSearchDocument> written = docCaptor.getAllValues();
        assertThat(written).extracting(RagSearchDocument::getId).containsExactly("doc-1#0", "doc-1#1");
        assertThat(written.get(0).getDocId()).isEqualTo("doc-1");
        assertThat(written.get(0).getContent()).isEqualTo("第一块");
        assertThat(written.get(0).getBreadcrumb()).containsExactly("根", "标题A");
        assertThat(written.get(0).getChunkType()).isEqualTo(ChunkType.SECTION.getCode());
    }

    @Test
    void upsert_whenIndexExists_doesNotRecreate() {
        SearchService searchService = mock(SearchService.class);
        when(searchService.indexExists(INDEX)).thenReturn(true);
        when(searchService.indexDocument(eq(INDEX), anyString(), any())).thenReturn(ok());
        SearchServiceIndexSink sink = new SearchServiceIndexSink(searchService, INDEX, "standard", "standard");

        sink.upsert("doc-1", List.of(chunk("doc-1#0", "块", "标题")));

        verify(searchService, never()).createIndex(anyString(), any());
    }

    @Test
    void delete_callsBulkDeleteWhenIndexExists() {
        SearchService searchService = mock(SearchService.class);
        when(searchService.indexExists(INDEX)).thenReturn(true);
        SearchServiceIndexSink sink = new SearchServiceIndexSink(searchService, INDEX, "standard", "standard");

        sink.delete("doc-1", List.of("doc-1#0", "doc-1#1"));

        verify(searchService).bulkDeleteDocuments(INDEX, List.of("doc-1#0", "doc-1#1"));
    }

    @Test
    void delete_whenIndexMissing_isSilentNoop() {
        SearchService searchService = mock(SearchService.class);
        when(searchService.indexExists(INDEX)).thenReturn(false);
        SearchServiceIndexSink sink = new SearchServiceIndexSink(searchService, INDEX, "standard", "standard");

        sink.delete("doc-1", List.of("doc-1#0"));

        verify(searchService, never()).bulkDeleteDocuments(anyString(), anyList());
    }

    @Test
    void partialFailure_throwsWithSucceededIds() {
        SearchService searchService = mock(SearchService.class);
        when(searchService.indexExists(INDEX)).thenReturn(true);
        when(searchService.indexDocument(eq(INDEX), anyString(), any()))
                .thenReturn(ok()).thenReturn(failed());
        SearchServiceIndexSink sink = new SearchServiceIndexSink(searchService, INDEX, "standard", "standard");

        assertThatThrownBy(() -> sink.upsert("doc-1", List.of(
                chunk("doc-1#0", "块0", "t0"), chunk("doc-1#1", "块1", "t1"))))
                .isInstanceOf(IndexSinkException.class)
                .satisfies(e -> assertThat(((IndexSinkException) e).getSucceededIds())
                        .containsExactly("doc-1#0"));
    }

    private static DocumentResult ok() {
        DocumentResult result = new DocumentResult();
        result.setSuccess(true);
        return result;
    }

    private static DocumentResult failed() {
        DocumentResult result = new DocumentResult();
        result.setSuccess(false);
        result.setErrorMessage("mapping 冲突");
        return result;
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

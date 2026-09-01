package io.nebula.ai.rag.index;

import io.nebula.ai.rag.chunking.DocumentChunk;
import io.nebula.ai.rag.chunking.pack.PackOptions;
import io.nebula.ai.rag.retriever.RagSearchDocument;
import io.nebula.search.core.SearchService;
import io.nebula.search.core.model.DocumentResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索引擎写目标（详细设计 §2.3）
 * <p>
 * 写 {@link RagSearchDocument}；索引不存在时用 {@link RagSearchDocument#defaultMapping} 建索引
 * （经 §3.1 的 mapping 下发能力）。delete 走 {@code bulkDeleteDocuments}，删不存在的 ID 静默成功。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class SearchServiceIndexSink implements IndexSink {

    /** sink 名称 */
    public static final String NAME = "search-service";

    private final SearchService searchService;
    private final String indexName;
    private final String analyzer;
    private final String searchAnalyzer;

    public SearchServiceIndexSink(SearchService searchService, String indexName,
                                  String analyzer, String searchAnalyzer) {
        if (searchService == null) {
            throw new IllegalArgumentException("SearchService 不能为空");
        }
        if (indexName == null || indexName.isBlank()) {
            throw new IllegalArgumentException("indexName 不能为空");
        }
        this.searchService = searchService;
        this.indexName = indexName;
        this.analyzer = analyzer != null ? analyzer : "standard";
        this.searchAnalyzer = searchAnalyzer != null ? searchAnalyzer : "standard";
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
        ensureIndex();
        List<String> succeeded = new ArrayList<>(chunks.size());
        for (DocumentChunk chunk : chunks) {
            try {
                // indexDocument 按 ID 写入即 upsert 语义（同 ID 覆盖）
                DocumentResult result =
                        searchService.indexDocument(indexName, chunk.getId(), toSearchDocument(docId, chunk));
                if (result == null || !result.isSuccess()) {
                    String reason = result != null ? result.getErrorMessage() : "结果为空";
                    throw new IndexSinkException(NAME, docId, succeeded,
                            "块 ID=" + chunk.getId() + " 写入失败: " + reason, null);
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
        if (!searchService.indexExists(indexName)) {
            // 索引都不存在，自然没有要删的块，静默成功
            return;
        }
        searchService.bulkDeleteDocuments(indexName, chunkIds);
    }

    private void ensureIndex() {
        if (!searchService.indexExists(indexName)) {
            searchService.createIndex(indexName,
                    RagSearchDocument.defaultMapping(analyzer, searchAnalyzer));
        }
    }

    @SuppressWarnings("unchecked")
    private RagSearchDocument toSearchDocument(String docId, DocumentChunk chunk) {
        RagSearchDocument doc = new RagSearchDocument();
        doc.setId(chunk.getId());
        doc.setDocId(docId);
        doc.setContent(chunk.getContent());
        doc.setTitle(chunk.getTitle());
        if (chunk.getMetadata() != null) {
            Object breadcrumb = chunk.getMetadata().get(PackOptions.META_BREADCRUMB);
            if (breadcrumb instanceof List<?> list) {
                doc.setBreadcrumb((List<String>) list);
            }
        }
        if (chunk.getChunkType() != null) {
            doc.setChunkType(chunk.getChunkType().getCode());
        }
        doc.setMetadata(chunk.getMetadata());
        return doc;
    }
}

package io.nebula.ai.rag.retriever;

import io.nebula.search.core.SearchService;
import io.nebula.search.core.model.SearchDocument;
import io.nebula.search.core.model.SearchResult;
import io.nebula.search.core.query.SearchQuery;
import io.nebula.search.core.query.builder.BoolQueryBuilder;
import io.nebula.search.core.query.builder.MatchQueryBuilder;
import io.nebula.search.core.query.builder.TermQueryBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基于 {@link SearchService} 的 BM25 关键词检索器（P4b，详细设计 §3.3）
 * <p>
 * 查询：{@code content} 走 match 主子句，{@code title} 以 should 子句加权；{@code _score}
 * 直接作 {@link RetrievalResult#getScore()}（RRF 只用名次，量纲无关）。
 * <p>
 * <b>filter 语义（审查发现 9）：</b>只允许 {@code docId} / {@code chunkType} / {@code breadcrumb}
 * 三个键做等值过滤；出现许可集之外的键立即抛 {@link IllegalArgumentException} 快速失败，
 * 不静默忽略。范围过滤本期不开（无消费方）。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class SearchServiceRetriever implements Retriever {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceRetriever.class);

    /** 检索结果的 source 标注，融合时用来识别来源 */
    public static final String SOURCE = "keyword";

    /** filter 许可集：只允许这三个键做等值过滤 */
    private static final Set<String> ALLOWED_FILTER_KEYS = Set.of("docId", "chunkType", "breadcrumb");

    private final SearchService searchService;
    private final String indexName;
    private final String name;
    private final double weight;

    public SearchServiceRetriever(SearchService searchService, String indexName, double weight) {
        this(searchService, indexName, "SearchServiceRetriever", weight);
    }

    public SearchServiceRetriever(SearchService searchService, String indexName, String name,
                                  double weight) {
        if (searchService == null) {
            throw new IllegalArgumentException("SearchService 不能为空");
        }
        if (indexName == null || indexName.isBlank()) {
            throw new IllegalArgumentException("indexName 不能为空");
        }
        this.searchService = searchService;
        this.indexName = indexName;
        this.name = name;
        this.weight = weight;
    }

    @Override
    public List<RetrievalResult> retrieve(String query, int topK, Map<String, Object> filter) {
        BoolQueryBuilder bool = new BoolQueryBuilder()
                .must(new MatchQueryBuilder("content", query))
                .should(new MatchQueryBuilder("title", query));
        applyFilter(bool, filter);

        SearchQuery searchQuery = SearchQuery.builder()
                .index(indexName)
                .query(bool)
                .size(topK)
                .build();

        SearchResult<RagSearchDocument> result =
                searchService.search(searchQuery, RagSearchDocument.class);
        if (result == null || !result.hasHits()) {
            return List.of();
        }

        List<RetrievalResult> results = new ArrayList<>(result.getHitCount());
        for (SearchDocument<RagSearchDocument> hit : result.getDocuments()) {
            RagSearchDocument source = hit.getSource();
            if (source == null) {
                continue;
            }
            results.add(RetrievalResult.builder()
                    .id(source.getId())
                    .content(source.getContent())
                    .metadata(toMetadata(source))
                    .score(hit.getScore() != null ? hit.getScore() : 0.0)
                    .source(SOURCE)
                    .build());
        }
        return results;
    }

    /**
     * 把 filter 落成 bool 的 filter 子句；许可集之外的键立即抛异常
     */
    private void applyFilter(BoolQueryBuilder bool, Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            String key = entry.getKey();
            if (!ALLOWED_FILTER_KEYS.contains(key)) {
                throw new IllegalArgumentException(
                        "SearchServiceRetriever 不支持的过滤键: " + key
                                + "; 许可集仅 " + ALLOWED_FILTER_KEYS + "（等值过滤）");
            }
            Object value = entry.getValue();
            if (value instanceof Collection<?> values) {
                // 多值等值：任一命中即可（Terms 语义）
                BoolQueryBuilder any = new BoolQueryBuilder().minimumShouldMatch("1");
                for (Object v : values) {
                    any.should(new TermQueryBuilder(key, v));
                }
                bool.filter(any);
            } else {
                bool.filter(new TermQueryBuilder(key, value));
            }
        }
    }

    private Map<String, Object> toMetadata(RagSearchDocument source) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (source.getMetadata() != null) {
            metadata.putAll(source.getMetadata());
        }
        metadata.put("docId", source.getDocId());
        metadata.put("breadcrumb", source.getBreadcrumb());
        metadata.put("title", source.getTitle());
        if (source.getChunkType() != null) {
            metadata.put("chunkType", source.getChunkType());
        }
        return metadata;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getWeight() {
        return weight;
    }
}

package io.nebula.ai.rag.index;

import io.nebula.search.core.SearchService;

/**
 * 按物理索引名产出 {@link SearchServiceIndexSink} 的工厂（R3 §3.2、§4.1）
 * <p>
 * BM25 侧实现平凡：R2 的 sink 本就按 indexName 写，换索引名 new 一个即可。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class SearchServiceIndexTargetFactory implements IndexTargetFactory {

    /** 与 {@link SearchServiceIndexSink#NAME} 对齐 */
    public static final String NAME = SearchServiceIndexSink.NAME;

    private final SearchService searchService;
    private final String analyzer;
    private final String searchAnalyzer;

    public SearchServiceIndexTargetFactory(SearchService searchService, String analyzer,
                                           String searchAnalyzer) {
        if (searchService == null) {
            throw new IllegalArgumentException("SearchService 不能为空");
        }
        this.searchService = searchService;
        this.analyzer = analyzer != null ? analyzer : "standard";
        this.searchAnalyzer = searchAnalyzer != null ? searchAnalyzer : "standard";
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public IndexSink sinkFor(String physicalName) {
        return new SearchServiceIndexSink(searchService, physicalName, analyzer, searchAnalyzer);
    }
}

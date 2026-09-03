package io.nebula.ai.rag.index;

import io.nebula.ai.rag.retriever.RagSearchDocument;
import io.nebula.search.core.SearchService;

import java.util.List;

/**
 * 基于 {@link SearchService} 的 BM25 索引蓝绿切换器（R3 §4.1）
 * <p>
 * 只依赖中立的 {@code SearchService} 契约（含 R3 新增的 {@code switchAlias}/{@code resolveAlias}
 * default 方法），不涉及供应商 SDK，故可落 {@code nebula-ai-rag}（不违 Y5）。一个实例绑定一个
 * 逻辑别名（{@code aliasName}）：
 * <ul>
 *   <li>{@code prepare}：索引不存在才建（默认 mapping），幂等；</li>
 *   <li>{@code switchTo}：{@code switchAlias} 原子改指向；</li>
 *   <li>{@code resolveCurrent}：{@code resolveAlias} 取当前指向，空则返回 {@code null}；</li>
 *   <li>{@code drop}：先查本别名当前指向，若正指向待删目标则拒绝删除（不误删活动代际）。</li>
 * </ul>
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class SearchServiceCollectionSwitcher implements CollectionSwitcher {

    /** 与 {@link SearchServiceIndexSink#NAME} 对齐，用于配对写目标 */
    public static final String NAME = SearchServiceIndexSink.NAME;

    private final SearchService searchService;
    private final String aliasName;
    private final String analyzer;
    private final String searchAnalyzer;

    public SearchServiceCollectionSwitcher(SearchService searchService, String aliasName,
                                           String analyzer, String searchAnalyzer) {
        if (searchService == null) {
            throw new IllegalArgumentException("SearchService 不能为空");
        }
        if (aliasName == null || aliasName.isBlank()) {
            throw new IllegalArgumentException("BM25 别名(aliasName)不能为空");
        }
        this.searchService = searchService;
        this.aliasName = aliasName;
        this.analyzer = analyzer != null ? analyzer : "standard";
        this.searchAnalyzer = searchAnalyzer != null ? searchAnalyzer : "standard";
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void prepare(String physicalName) {
        requirePhysicalNotAlias(physicalName);
        if (!searchService.indexExists(physicalName)) {
            searchService.createIndex(physicalName,
                    RagSearchDocument.defaultMapping(analyzer, searchAnalyzer));
        }
    }

    @Override
    public boolean exists(String physicalName) {
        return searchService.indexExists(physicalName);
    }

    @Override
    public void switchTo(String logicalName, String physicalName) {
        requirePhysicalNotAlias(physicalName);
        searchService.switchAlias(logicalName, physicalName);
    }

    @Override
    public String resolveCurrent(String logicalName) {
        List<String> targets = searchService.resolveAlias(logicalName);
        return targets == null || targets.isEmpty() ? null : targets.get(0);
    }

    @Override
    public void drop(String physicalName) {
        // 本别名当前指向待删目标即拒绝(不误删活动代际)
        if (physicalName.equals(resolveCurrent(aliasName))) {
            throw new IllegalStateException(
                    "拒绝删除 BM25 索引 " + physicalName + ": 别名 " + aliasName + " 仍指向它");
        }
        if (searchService.indexExists(physicalName)) {
            searchService.deleteIndex(physicalName);
        }
    }

    /**
     * 别名与物理名相同会让 ES 拒绝（别名不能与索引同名），启用时应先改名迁移
     */
    private void requirePhysicalNotAlias(String physicalName) {
        if (aliasName.equals(physicalName)) {
            throw new IllegalArgumentException(
                    "BM25 物理索引名不能等于别名 " + aliasName + ": 别名与索引不能同名, 启用重灌前需先改名迁移");
        }
    }
}

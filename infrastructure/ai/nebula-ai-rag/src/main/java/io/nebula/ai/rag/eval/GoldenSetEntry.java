package io.nebula.ai.rag.eval;

import java.util.ArrayList;
import java.util.List;

/**
 * 金标条目
 * <p>
 * 一条条目 = 一个查询 + 该查询的命中判据。判据是<b>块 ID 前缀</b>而不是完整 ID：
 * 块索引会随切分策略变化，写死完整 ID 的金标换一次切分就得重写一遍，
 * 前缀（通常是 {@code <语料文件名>#}）在切分策略之间保持稳定。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class GoldenSetEntry {

    /**
     * 查询文本
     */
    private String query;

    /**
     * 命中判据：检索结果 ID 以其中任一前缀开头即算命中
     */
    private List<String> expectedIdPrefixes = new ArrayList<>();

    /**
     * 所属子集，用于分子集聚合；为空时归入 {@link GoldenSet#DEFAULT_SUBSET}
     */
    private String subset;

    public GoldenSetEntry() {
    }

    public GoldenSetEntry(String query, List<String> expectedIdPrefixes, String subset) {
        this.query = query;
        this.expectedIdPrefixes = expectedIdPrefixes != null
                ? new ArrayList<>(expectedIdPrefixes) : new ArrayList<>();
        this.subset = subset;
    }

    /**
     * 判断一个检索结果 ID 是否命中本条目
     *
     * @param resultId 检索结果 ID，可为 null
     * @return ID 以任一期望前缀开头则为 true
     */
    public boolean matches(String resultId) {
        if (resultId == null || expectedIdPrefixes == null) {
            return false;
        }
        for (String prefix : expectedIdPrefixes) {
            if (prefix != null && !prefix.isEmpty() && resultId.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 所属子集；未指定时返回默认子集名
     */
    public String resolvedSubset() {
        return subset == null || subset.isBlank() ? GoldenSet.DEFAULT_SUBSET : subset;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<String> getExpectedIdPrefixes() {
        return expectedIdPrefixes;
    }

    public void setExpectedIdPrefixes(List<String> expectedIdPrefixes) {
        this.expectedIdPrefixes = expectedIdPrefixes != null
                ? new ArrayList<>(expectedIdPrefixes) : new ArrayList<>();
    }

    public String getSubset() {
        return subset;
    }

    public void setSubset(String subset) {
        this.subset = subset;
    }
}

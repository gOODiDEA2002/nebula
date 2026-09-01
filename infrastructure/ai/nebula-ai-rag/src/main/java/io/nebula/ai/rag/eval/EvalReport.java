package io.nebula.ai.rag.eval;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * 评测报告
 * <p>
 * 三项指标的口径（与 {@link RetrievalEvaluator} 的实现一一对应）：
 * <ul>
 *   <li><b>recall@k</b> = 至少命中一条的条目数 / 条目总数；</li>
 *   <li><b>MRR</b> = Σ(1 / 首个命中名次) / 条目总数，未命中的条目计 0；</li>
 *   <li><b>nDCG@k</b> = 二值相关：{@code DCG = Σ 命中位置 1/log2(i+1)}（i 从 1 起算），
 *       {@code IDCG} 按理想排列 —— 把本次命中的这些块全部提到最前面 —— 计算；
 *       一条都没命中时计 0。</li>
 * </ul>
 * {@link #getConfigSnapshot()} 是两份报告可比性的锚：调优前后各跑一份，
 * 快照说明「变的到底是什么」，否则半年后没人说得清某个数字对应哪套参数。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class EvalReport {

    private final double recallAtK;
    private final double mrr;
    private final double ndcgAtK;
    private final int k;
    private final int total;
    private final Map<String, SubsetMetrics> perSubset;
    private final List<QueryOutcome> perQuery;
    private final Map<String, String> configSnapshot;

    public EvalReport(double recallAtK, double mrr, double ndcgAtK, int k, int total,
                      Map<String, SubsetMetrics> perSubset, List<QueryOutcome> perQuery,
                      Map<String, String> configSnapshot) {
        this.recallAtK = recallAtK;
        this.mrr = mrr;
        this.ndcgAtK = ndcgAtK;
        this.k = k;
        this.total = total;
        // 子集按名字排序：两份报告逐行对比时顺序必须稳定
        this.perSubset = perSubset != null
                ? Collections.unmodifiableMap(new TreeMap<>(perSubset)) : Map.of();
        this.perQuery = perQuery != null
                ? List.copyOf(perQuery) : List.of();
        this.configSnapshot = configSnapshot != null
                ? Collections.unmodifiableMap(new TreeMap<>(configSnapshot)) : Map.of();
    }

    public double getRecallAtK() {
        return recallAtK;
    }

    public double getMrr() {
        return mrr;
    }

    public double getNdcgAtK() {
        return ndcgAtK;
    }

    public int getK() {
        return k;
    }

    public int getTotal() {
        return total;
    }

    public Map<String, SubsetMetrics> getPerSubset() {
        return perSubset;
    }

    public List<QueryOutcome> getPerQuery() {
        return perQuery;
    }

    public Map<String, String> getConfigSnapshot() {
        return configSnapshot;
    }

    /**
     * 取某个子集的指标
     *
     * @param subset 子集名
     * @return 该子集指标；不存在时返回 null
     */
    public SubsetMetrics subset(String subset) {
        return perSubset.get(subset);
    }

    /**
     * 单行摘要：三项总体指标 + 各子集 recall + 配置快照
     * <p>
     * 供日志打印与测试断言使用；数值一律四位小数、Locale.ROOT，
     * 保证同一份数据在任何机器上输出一致。
     */
    public String toComparableSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "recall@%d=%.4f mrr=%.4f ndcg@%d=%.4f total=%d",
                k, recallAtK, mrr, k, ndcgAtK, total));
        if (!perSubset.isEmpty()) {
            sb.append(" | subsets:");
            for (Map.Entry<String, SubsetMetrics> entry : perSubset.entrySet()) {
                sb.append(String.format(Locale.ROOT, " %s=%.4f",
                        entry.getKey(), entry.getValue().getRecallAtK()));
            }
        }
        if (!configSnapshot.isEmpty()) {
            sb.append(" | config:");
            for (Map.Entry<String, String> entry : configSnapshot.entrySet()) {
                sb.append(' ').append(entry.getKey()).append('=').append(entry.getValue());
            }
        }
        return sb.toString();
    }

    /**
     * 多行明细：总体一行 + 每个子集一行（三项指标齐全）
     * <p>
     * A/B 对比留档用；{@link #toComparableSummary()} 只带各子集 recall，
     * 要看 MRR 与 nDCG 的分子集变化得用本方法。
     */
    public String toDetailedSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT,
                "总体  recall@%d=%.4f  mrr=%.4f  ndcg@%d=%.4f  total=%d",
                k, recallAtK, mrr, k, ndcgAtK, total));
        for (Map.Entry<String, SubsetMetrics> entry : perSubset.entrySet()) {
            SubsetMetrics metrics = entry.getValue();
            sb.append(String.format(Locale.ROOT,
                    "%n  %-12s recall@%d=%.4f  mrr=%.4f  ndcg@%d=%.4f  n=%d",
                    entry.getKey(), k, metrics.getRecallAtK(), metrics.getMrr(),
                    k, metrics.getNdcgAtK(), metrics.getTotal()));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toComparableSummary();
    }

    /**
     * 单个子集的指标，口径与总体三项完全一致
     */
    public static class SubsetMetrics {

        private final String subset;
        private final double recallAtK;
        private final double mrr;
        private final double ndcgAtK;
        private final int total;

        public SubsetMetrics(String subset, double recallAtK, double mrr, double ndcgAtK, int total) {
            this.subset = subset;
            this.recallAtK = recallAtK;
            this.mrr = mrr;
            this.ndcgAtK = ndcgAtK;
            this.total = total;
        }

        public String getSubset() {
            return subset;
        }

        public double getRecallAtK() {
            return recallAtK;
        }

        public double getMrr() {
            return mrr;
        }

        public double getNdcgAtK() {
            return ndcgAtK;
        }

        public int getTotal() {
            return total;
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "%s{recall=%.4f, mrr=%.4f, ndcg=%.4f, n=%d}",
                    subset, recallAtK, mrr, ndcgAtK, total);
        }
    }

    /**
     * 单条查询的评测明细
     * <p>
     * 保留 {@link #getTopIds()} 是为了让「为什么没命中」可查：
     * 只有聚合数字的报告没法定位是检索没找到、还是切分把答案切碎了。
     */
    public static class QueryOutcome {

        private final String query;
        private final String subset;
        /** 首个命中的名次（1 起算）；未命中为 -1 */
        private final int hitRank;
        private final List<String> topIds;

        public QueryOutcome(String query, String subset, int hitRank, List<String> topIds) {
            this.query = query;
            this.subset = subset;
            this.hitRank = hitRank;
            this.topIds = topIds != null ? List.copyOf(topIds) : List.of();
        }

        public String getQuery() {
            return query;
        }

        public String getSubset() {
            return subset;
        }

        public int getHitRank() {
            return hitRank;
        }

        public List<String> getTopIds() {
            return topIds;
        }

        public boolean isHit() {
            return hitRank > 0;
        }

        @Override
        public String toString() {
            return "[" + subset + "] " + query + " -> rank=" + hitRank + " ids=" + topIds;
        }
    }
}

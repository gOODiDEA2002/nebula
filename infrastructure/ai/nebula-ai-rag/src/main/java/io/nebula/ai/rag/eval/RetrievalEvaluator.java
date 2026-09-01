package io.nebula.ai.rag.eval;

import io.nebula.ai.rag.eval.EvalReport.QueryOutcome;
import io.nebula.ai.rag.eval.EvalReport.SubsetMetrics;
import io.nebula.ai.rag.retriever.RetrievalResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 检索评测器
 * <p>
 * 纯函数式：给一份金标集与一个检索函数，跑出 recall@k / MRR / nDCG@k 三项指标
 * 与逐条明细。不依赖 Spring、不读配置、不落盘 —— 配置快照由调用方传入。
 * <p>
 * <b>指标口径（三处必须一致：本类实现、{@link EvalReport} javadoc、评测报告正文）：</b>
 * <ol>
 *   <li>先把检索结果截断到前 k 条，之后所有计算都在这 k 条内进行；</li>
 *   <li>命中判据是 {@link GoldenSetEntry#matches(String)} 的 ID 前缀匹配；</li>
 *   <li>recall@k 只问「有没有命中」，不问命中几条 —— 一条金标可能对应多个块，
 *       用「命中条数 / 期望条数」会因切分粒度不同而失去可比性；</li>
 *   <li>MRR 只看首个命中的名次，未命中计 0；</li>
 *   <li>nDCG@k 用二值相关：DCG 累加每个命中位置的 {@code 1/log2(i+1)}，
 *       IDCG 按「把本次命中的这些块全部提到最前面」的理想排列算。
 *       这样定义保证 nDCG ∈ [0,1]：金标只声明「答案在哪个文件」，
 *       相关块的真实总数随切分变化，用它做分母会让不同切分的 nDCG 不可比。</li>
 * </ol>
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class RetrievalEvaluator {

    /** 默认评测深度 */
    public static final int DEFAULT_K = 5;

    private final int k;

    /**
     * 使用默认评测深度 {@value #DEFAULT_K}
     */
    public RetrievalEvaluator() {
        this(DEFAULT_K);
    }

    /**
     * @param k 评测深度，必须为正数
     */
    public RetrievalEvaluator(int k) {
        if (k <= 0) {
            throw new IllegalArgumentException("评测深度 k 必须为正数");
        }
        this.k = k;
    }

    public int getK() {
        return k;
    }

    /**
     * 执行评测
     *
     * @param goldenSet      金标集，不能为空
     * @param fn             被评测的检索函数，不能为空
     * @param configSnapshot 本次评测对应的配置快照（切分参数、检索器参数等），可为 null
     * @return 评测报告
     */
    public EvalReport evaluate(GoldenSet goldenSet, RetrievalFunction fn,
                               Map<String, String> configSnapshot) {
        if (goldenSet == null || goldenSet.size() == 0) {
            throw new IllegalArgumentException("金标集不能为空");
        }
        if (fn == null) {
            throw new IllegalArgumentException("检索函数不能为空");
        }

        List<QueryOutcome> outcomes = new ArrayList<>(goldenSet.size());
        Map<String, Accumulator> perSubset = new LinkedHashMap<>();
        Accumulator overall = new Accumulator();

        for (GoldenSetEntry entry : goldenSet.getEntries()) {
            List<RetrievalResult> results = fn.retrieve(entry.getQuery(), k);
            List<String> topIds = topIds(results);

            int hitRank = -1;
            double dcg = 0.0;
            int hits = 0;
            for (int i = 0; i < topIds.size(); i++) {
                if (!entry.matches(topIds.get(i))) {
                    continue;
                }
                hits++;
                dcg += gain(i + 1);
                if (hitRank < 0) {
                    hitRank = i + 1;
                }
            }
            double ndcg = hits == 0 ? 0.0 : dcg / idealDcg(hits);
            double reciprocalRank = hitRank > 0 ? 1.0 / hitRank : 0.0;

            String subset = entry.resolvedSubset();
            outcomes.add(new QueryOutcome(entry.getQuery(), subset, hitRank, topIds));
            overall.add(hitRank > 0, reciprocalRank, ndcg);
            perSubset.computeIfAbsent(subset, name -> new Accumulator())
                    .add(hitRank > 0, reciprocalRank, ndcg);
        }

        Map<String, SubsetMetrics> subsetMetrics = new LinkedHashMap<>();
        for (Map.Entry<String, Accumulator> entry : perSubset.entrySet()) {
            subsetMetrics.put(entry.getKey(), entry.getValue().toSubsetMetrics(entry.getKey()));
        }

        return new EvalReport(overall.recall(), overall.mrr(), overall.ndcg(), k, overall.total,
                subsetMetrics, outcomes, configSnapshot);
    }

    /**
     * 截断到前 k 条并取 ID；null 结果与 null ID 都按「不可能命中」处理
     */
    private List<String> topIds(List<RetrievalResult> results) {
        List<String> ids = new ArrayList<>(k);
        if (results == null) {
            return ids;
        }
        for (RetrievalResult result : results) {
            if (ids.size() >= k) {
                break;
            }
            ids.add(result == null ? null : result.getId());
        }
        return ids;
    }

    /**
     * 二值相关下第 rank 位（1 起算）的增益
     */
    private static double gain(int rank) {
        return 1.0 / (Math.log(rank + 1) / Math.log(2));
    }

    /**
     * 理想排列的 DCG：hits 个命中块全部排在最前面
     */
    private static double idealDcg(int hits) {
        double ideal = 0.0;
        for (int rank = 1; rank <= hits; rank++) {
            ideal += gain(rank);
        }
        return ideal;
    }

    /**
     * 指标累加器：总体与各子集共用同一套算法，避免两处口径漂移
     */
    private static final class Accumulator {

        private int total;
        private int hitCount;
        private double reciprocalRankSum;
        private double ndcgSum;

        void add(boolean hit, double reciprocalRank, double ndcg) {
            total++;
            if (hit) {
                hitCount++;
            }
            reciprocalRankSum += reciprocalRank;
            ndcgSum += ndcg;
        }

        double recall() {
            return total == 0 ? 0.0 : (double) hitCount / total;
        }

        double mrr() {
            return total == 0 ? 0.0 : reciprocalRankSum / total;
        }

        double ndcg() {
            return total == 0 ? 0.0 : ndcgSum / total;
        }

        SubsetMetrics toSubsetMetrics(String subset) {
            return new SubsetMetrics(subset, recall(), mrr(), ndcg(), total);
        }
    }
}

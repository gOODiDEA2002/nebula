package io.nebula.ai.rag.eval;

import io.nebula.ai.rag.eval.EvalReport.QueryOutcome;
import io.nebula.ai.rag.eval.EvalReport.SubsetMetrics;
import io.nebula.ai.rag.retriever.RetrievalResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * 评测指标的手算金标用例
 * <p>
 * 期望值全部是手工展开的十进制常量，<b>不在测试里复算实现的公式</b> ——
 * 用同一个表达式算期望值等于让实现自己判自己及格，公式写错时测试照样绿。
 * 参考值：{@code 1/log2(2)=1.0}、{@code 1/log2(3)=0.6309297535714575}、
 * {@code 1/log2(4)=0.5}。
 */
@DisplayName("RetrievalEvaluator 三指标手算金标")
class RetrievalEvaluatorTest {

    /** 浮点容差：金标是十进制展开值，这里只防最后几位的表示误差 */
    private static final double EPSILON = 1e-9;

    private final RetrievalEvaluator evaluator = new RetrievalEvaluator(5);

    @Test
    @DisplayName("首位命中: recall=1, MRR=1, nDCG=1")
    void hitAtRankOne_scoresPerfect() {
        GoldenSet goldenSet = goldenSet(entry("问题一", "doc-a#", "plain"));

        EvalReport report = evaluator.evaluate(goldenSet,
                fixedResults("doc-a#0", "doc-b#0", "doc-c#0"), null);

        assertThat(report.getRecallAtK()).isEqualTo(1.0);
        assertThat(report.getMrr()).isEqualTo(1.0);
        assertThat(report.getNdcgAtK()).isEqualTo(1.0);
        assertThat(report.getPerQuery()).singleElement()
                .extracting(QueryOutcome::getHitRank).isEqualTo(1);
    }

    @Test
    @DisplayName("第三位命中: MRR=1/3, nDCG=0.5(DCG=1/log2(4), IDCG=1)")
    void hitAtRankThree_matchesHandComputedValues() {
        GoldenSet goldenSet = goldenSet(entry("问题一", "doc-a#", "plain"));

        EvalReport report = evaluator.evaluate(goldenSet,
                fixedResults("doc-x#0", "doc-y#0", "doc-a#2"), null);

        assertThat(report.getRecallAtK()).isEqualTo(1.0);
        assertThat(report.getMrr()).isCloseTo(0.3333333333333333, within(EPSILON));
        assertThat(report.getNdcgAtK()).isCloseTo(0.5, within(EPSILON));
    }

    @Test
    @DisplayName("未命中: 三项全部计 0, hitRank 记 -1")
    void miss_scoresZeroAndRecordsMinusOne() {
        GoldenSet goldenSet = goldenSet(entry("问题一", "doc-a#", "plain"));

        EvalReport report = evaluator.evaluate(goldenSet,
                fixedResults("doc-x#0", "doc-y#0"), null);

        assertThat(report.getRecallAtK()).isEqualTo(0.0);
        assertThat(report.getMrr()).isEqualTo(0.0);
        assertThat(report.getNdcgAtK()).isEqualTo(0.0);
        QueryOutcome outcome = report.getPerQuery().get(0);
        assertThat(outcome.getHitRank()).isEqualTo(-1);
        assertThat(outcome.isHit()).isFalse();
    }

    @Test
    @DisplayName("同一条目多块并列命中: nDCG 按理想排列归一, MRR 仍只看首个名次")
    void multipleHits_normalizeAgainstIdealArrangement() {
        // 命中落在第 2、3 位: DCG = 0.6309297535714575 + 0.5 = 1.1309297535714575
        //                     IDCG = 1.0 + 0.6309297535714575 = 1.6309297535714575
        GoldenSet goldenSet = goldenSet(entry("问题一", "doc-a#", "plain"));

        EvalReport report = evaluator.evaluate(goldenSet,
                fixedResults("doc-x#0", "doc-a#1", "doc-a#5", "doc-y#0"), null);

        assertThat(report.getNdcgAtK()).isCloseTo(0.6934264036172708, within(EPSILON));
        assertThat(report.getMrr()).isCloseTo(0.5, within(EPSILON));
        assertThat(report.getRecallAtK()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("命中块全部排在最前: nDCG=1(理想排列即实际排列)")
    void hitsAtTopPositions_scoreNdcgOne() {
        GoldenSet goldenSet = goldenSet(entry("问题一", "doc-a#", "plain"));

        EvalReport report = evaluator.evaluate(goldenSet,
                fixedResults("doc-a#0", "doc-a#1", "doc-x#0"), null);

        assertThat(report.getNdcgAtK()).isCloseTo(1.0, within(EPSILON));
    }

    @Test
    @DisplayName("k 截断: 第 k+1 位的命中不算命中, topIds 也只留 k 条")
    void beyondK_isNotCounted() {
        RetrievalEvaluator k3 = new RetrievalEvaluator(3);
        GoldenSet goldenSet = goldenSet(entry("问题一", "doc-a#", "plain"));

        EvalReport report = k3.evaluate(goldenSet,
                fixedResults(3, "doc-x#0", "doc-y#0", "doc-z#0", "doc-a#0"), null);

        assertThat(report.getK()).isEqualTo(3);
        assertThat(report.getRecallAtK()).isEqualTo(0.0);
        assertThat(report.getMrr()).isEqualTo(0.0);
        assertThat(report.getNdcgAtK()).isEqualTo(0.0);
        assertThat(report.getPerQuery().get(0).getTopIds())
                .containsExactly("doc-x#0", "doc-y#0", "doc-z#0");
    }

    @Test
    @DisplayName("多条目聚合: 总体与分子集三项指标同口径")
    void aggregation_matchesHandComputedValues() {
        GoldenSet goldenSet = goldenSet(
                entry("表格问题", "t-a#", "table"),
                entry("表格问题二", "t-b#", "table"),
                entry("纯文问题", "p-a#", "plain"));

        Map<String, List<String>> scripted = new LinkedHashMap<>();
        scripted.put("表格问题", List.of("t-a#0", "x#0"));          // 首位命中
        scripted.put("表格问题二", List.of("x#0", "y#0"));           // 未命中
        scripted.put("纯文问题", List.of("x#0", "p-a#3"));          // 第二位命中

        EvalReport report = evaluator.evaluate(goldenSet, scriptedResults(scripted), null);

        // 总体: recall=2/3, MRR=(1+0+0.5)/3=0.5, nDCG=(1+0+0.6309297535714575)/3
        assertThat(report.getRecallAtK()).isCloseTo(0.6666666666666666, within(EPSILON));
        assertThat(report.getMrr()).isCloseTo(0.5, within(EPSILON));
        assertThat(report.getNdcgAtK()).isCloseTo(0.5436432511904858, within(EPSILON));
        assertThat(report.getTotal()).isEqualTo(3);

        SubsetMetrics table = report.subset("table");
        assertThat(table.getTotal()).isEqualTo(2);
        assertThat(table.getRecallAtK()).isCloseTo(0.5, within(EPSILON));
        assertThat(table.getMrr()).isCloseTo(0.5, within(EPSILON));
        assertThat(table.getNdcgAtK()).isCloseTo(0.5, within(EPSILON));

        SubsetMetrics plain = report.subset("plain");
        assertThat(plain.getTotal()).isEqualTo(1);
        assertThat(plain.getRecallAtK()).isCloseTo(1.0, within(EPSILON));
        assertThat(plain.getMrr()).isCloseTo(0.5, within(EPSILON));
        assertThat(plain.getNdcgAtK()).isCloseTo(0.6309297535714575, within(EPSILON));
    }

    @Test
    @DisplayName("单行摘要: 三指标 + 各子集 recall + 配置快照, 子集按名排序")
    void comparableSummary_isStableAndSorted() {
        GoldenSet goldenSet = goldenSet(
                entry("表格问题", "t-a#", "table"),
                entry("纯文问题", "p-a#", "plain"));

        Map<String, List<String>> scripted = new LinkedHashMap<>();
        scripted.put("表格问题", List.of("t-a#0"));
        scripted.put("纯文问题", List.of("x#0"));

        EvalReport report = evaluator.evaluate(goldenSet, scriptedResults(scripted),
                Map.of("chunking", "fixed-500-100", "retriever", "lexical-2gram"));

        assertThat(report.toComparableSummary()).isEqualTo(
                "recall@5=0.5000 mrr=0.5000 ndcg@5=0.5000 total=2"
                        + " | subsets: plain=0.0000 table=1.0000"
                        + " | config: chunking=fixed-500-100 retriever=lexical-2gram");
    }

    @Test
    @DisplayName("配置快照原样带回报告, 作为两份报告的对比锚")
    void configSnapshot_isCarriedIntoReport() {
        GoldenSet goldenSet = goldenSet(entry("问题一", "doc-a#", "plain"));

        EvalReport report = evaluator.evaluate(goldenSet, fixedResults("doc-a#0"),
                Map.of("chunking", "structure-500-100"));

        assertThat(report.getConfigSnapshot()).containsExactlyEntriesOf(
                Map.of("chunking", "structure-500-100"));
    }

    @Test
    @DisplayName("未指定子集的条目归入 default 子集")
    void entryWithoutSubset_fallsBackToDefault() {
        GoldenSet goldenSet = goldenSet(entry("问题一", "doc-a#", null));

        EvalReport report = evaluator.evaluate(goldenSet, fixedResults("doc-a#0"), null);

        assertThat(report.getPerSubset()).containsOnlyKeys(GoldenSet.DEFAULT_SUBSET);
    }

    @Test
    @DisplayName("非法入参快速失败: k<=0 / 空金标 / 空检索函数")
    void invalidArguments_failFast() {
        assertThatThrownBy(() -> new RetrievalEvaluator(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("k");
        assertThatThrownBy(() -> evaluator.evaluate(new GoldenSet(), fixedResults("a"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("金标集");
        assertThatThrownBy(() ->
                evaluator.evaluate(goldenSet(entry("问题一", "doc-a#", "plain")), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("检索函数");
    }

    @Test
    @DisplayName("检索函数返回 null 或空表按未命中处理, 不抛异常")
    void nullResults_areTreatedAsMiss() {
        GoldenSet goldenSet = goldenSet(entry("问题一", "doc-a#", "plain"));

        EvalReport report = evaluator.evaluate(goldenSet, (query, topK) -> null, null);

        assertThat(report.getRecallAtK()).isEqualTo(0.0);
        assertThat(report.getPerQuery().get(0).getTopIds()).isEmpty();
    }

    // ------------------------------------------------------------------
    // 测试桩：桩必须校验收到的参数，桩比真货宽松等于测试白写
    // ------------------------------------------------------------------

    /**
     * 固定结果桩：断言拿到的 topK 与评测器声明的 k 一致，且查询非空
     */
    private RetrievalFunction fixedResults(String... ids) {
        return fixedResults(evaluator.getK(), ids);
    }

    private RetrievalFunction fixedResults(int expectedTopK, String... ids) {
        return (query, topK) -> {
            assertThat(query).as("评测器必须把金标的 query 原样传给检索函数").isNotBlank();
            assertThat(topK).as("评测器必须按自己的 k 调用检索函数").isEqualTo(expectedTopK);
            return toResults(ids);
        };
    }

    /**
     * 按查询分派结果的桩：查询不在脚本里直接判失败，避免「桩悄悄返回空表」掩盖问题
     */
    private RetrievalFunction scriptedResults(Map<String, List<String>> scripted) {
        return (query, topK) -> {
            assertThat(scripted)
                    .as("检索函数收到了脚本之外的查询: " + query)
                    .containsKey(query);
            assertThat(topK).isEqualTo(evaluator.getK());
            return toResults(scripted.get(query).toArray(new String[0]));
        };
    }

    private static List<RetrievalResult> toResults(String... ids) {
        List<RetrievalResult> results = new ArrayList<>(ids.length);
        for (int i = 0; i < ids.length; i++) {
            results.add(RetrievalResult.builder()
                    .id(ids[i])
                    .content("内容-" + ids[i])
                    .score(1.0 - i * 0.01)
                    .source("test")
                    .build());
        }
        return results;
    }

    private static GoldenSet goldenSet(GoldenSetEntry... entries) {
        return new GoldenSet(List.of(entries));
    }

    private static GoldenSetEntry entry(String query, String prefix, String subset) {
        return new GoldenSetEntry(query, List.of(prefix), subset);
    }
}

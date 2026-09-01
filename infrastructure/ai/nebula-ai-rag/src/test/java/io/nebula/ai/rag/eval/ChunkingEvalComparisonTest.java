package io.nebula.ai.rag.eval;

import io.nebula.ai.rag.chunking.DocumentChunk;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 切分改进的 A/B 对比门禁
 * <p>
 * <b>对比设计：</b>同一份语料 × 同一个确定性检索器 × 同一套预算（500/100）× 同一个评测深度，
 * 两侧唯一的差别是切分方式：
 * <ul>
 *   <li><b>A 侧</b>：现状 {@code TextChunker} 定长切分；</li>
 *   <li><b>B 侧</b>：结构解析 + 装箱（原子保护、表头重复、面包屑注入、键路径）。</li>
 * </ul>
 * 其他变量全部锁死，指标差异因此只能归因于切分方式本身。
 * <p>
 * <b>阈值口径（详细设计 §2.4）：</b>四个目标子集要求 recall@5 提升不少于 0.10；
 * plain 是参照子集，允许小幅波动但不允许实质回退（不低于 −0.05）。
 * 用「分子集有改善 + 非目标子集不回退」而不是「整体不低于」，
 * 是因为后者在毫无改进时也能通过。
 * <p>
 * <b>失败时先查语料再谈阈值：</b>本对比的区分度是靠语料构造出来的
 * （见 {@link EvalCorpus} 的构造原则）。如果 A 侧指标突然逼近满分，
 * 说明语料里的埋点被改没了，此时该修语料，放宽阈值只会把门禁变成摆设。
 */
@DisplayName("切分 A/B 对比: 定长 vs 结构化")
class ChunkingEvalComparisonTest {

    /** 评测深度 */
    private static final int K = 5;

    /** 目标子集的 recall@5 最小提升 */
    private static final double MIN_GAIN = 0.10;

    /** 参照子集 plain 允许的最大回退 */
    private static final double MAX_PLAIN_REGRESSION = 0.05;

    private static final List<String> TARGET_SUBSETS = List.of("table", "code", "breadcrumb", "json");

    private static final String PLAIN_SUBSET = "plain";

    private static EvalReport fixedLengthReport;

    private static EvalReport structureReport;

    @BeforeAll
    static void evaluateBothSides() {
        GoldenSet goldenSet = EvalCorpus.goldenSet();
        RetrievalEvaluator evaluator = new RetrievalEvaluator(K);

        List<DocumentChunk> fixedLength = EvalCorpus.fixedLengthChunks();
        fixedLengthReport = evaluator.evaluate(goldenSet,
                new DeterministicLexicalRetriever(fixedLength),
                Map.of("chunking", "fixed-length",
                        "chunkSize", String.valueOf(EvalCorpus.CHUNK_SIZE),
                        "overlap", String.valueOf(EvalCorpus.OVERLAP),
                        "chunks", String.valueOf(fixedLength.size()),
                        "retriever", "deterministic-lexical-2gram"));

        List<DocumentChunk> structure = EvalCorpus.structureChunks();
        structureReport = evaluator.evaluate(goldenSet,
                new DeterministicLexicalRetriever(structure),
                Map.of("chunking", "structure",
                        "chunkSize", String.valueOf(EvalCorpus.CHUNK_SIZE),
                        "overlap", String.valueOf(EvalCorpus.OVERLAP),
                        "breadcrumbToContent", "true",
                        "chunks", String.valueOf(structure.size()),
                        "retriever", "deterministic-lexical-2gram"));

        // 两份报告打进测试日志：验收留档要的就是这两行加两块明细
        System.out.println("[RAG-EVAL] A 定长切分  " + fixedLengthReport.toComparableSummary());
        System.out.println("[RAG-EVAL] B 结构切分  " + structureReport.toComparableSummary());
        System.out.println("[RAG-EVAL] A 侧明细\n" + fixedLengthReport.toDetailedSummary());
        System.out.println("[RAG-EVAL] B 侧明细\n" + structureReport.toDetailedSummary());
        System.out.println("[RAG-EVAL] 分子集 recall@" + K + " 差值");
        for (String subset : fixedLengthReport.getPerSubset().keySet()) {
            System.out.printf("[RAG-EVAL]   %-12s A=%.4f  B=%.4f  delta=%+.4f%n", subset,
                    recall(fixedLengthReport, subset), recall(structureReport, subset),
                    recall(structureReport, subset) - recall(fixedLengthReport, subset));
        }
    }

    @Test
    @DisplayName("语料仍有区分度: A 侧四个目标子集都没到满分(J3 前置检查)")
    void baseline_keepsHeadroom() {
        for (String subset : TARGET_SUBSETS) {
            assertThat(recall(fixedLengthReport, subset))
                    .as("A 侧 %s 子集已接近满分, 说明语料里的埋点被改没了: "
                            + "关键答案必须落在「定长切分必然切坏」的位置", subset)
                    .isLessThan(1.0);
        }
        assertThat(recall(fixedLengthReport, PLAIN_SUBSET))
                .as("plain 是参照子集, A 侧就该拿满分; 拿不到说明语料整体过难, "
                        + "此时目标子集的提升无法归因于结构化切分")
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("四个目标子集 recall@5 提升不少于 0.10")
    void targetSubsets_improveByAtLeastThreshold() {
        for (String subset : TARGET_SUBSETS) {
            double before = recall(fixedLengthReport, subset);
            double after = recall(structureReport, subset);
            assertThat(after - before)
                    .as("%s 子集 recall@%d: A=%.4f B=%.4f", subset, K, before, after)
                    .isGreaterThanOrEqualTo(MIN_GAIN);
        }
    }

    @Test
    @DisplayName("参照子集 plain 不实质回退(不低于 -0.05)")
    void plainSubset_doesNotRegress() {
        double before = recall(fixedLengthReport, PLAIN_SUBSET);
        double after = recall(structureReport, PLAIN_SUBSET);

        assertThat(after - before)
                .as("plain 子集 recall@%d: A=%.4f B=%.4f", K, before, after)
                .isGreaterThanOrEqualTo(-MAX_PLAIN_REGRESSION);
    }

    @Test
    @DisplayName("总体三项指标全部不回退")
    void overallMetrics_doNotRegress() {
        assertThat(structureReport.getRecallAtK())
                .as("总体 recall@%d", K).isGreaterThanOrEqualTo(fixedLengthReport.getRecallAtK());
        assertThat(structureReport.getMrr())
                .as("总体 MRR").isGreaterThanOrEqualTo(fixedLengthReport.getMrr());
        assertThat(structureReport.getNdcgAtK())
                .as("总体 nDCG@%d", K).isGreaterThanOrEqualTo(fixedLengthReport.getNdcgAtK());
    }

    @Test
    @DisplayName("两份报告口径一致: 同一份金标、同一个深度、五个子集齐全")
    void reports_areComparable() {
        assertThat(fixedLengthReport.getTotal()).isEqualTo(structureReport.getTotal());
        assertThat(fixedLengthReport.getK()).isEqualTo(K);
        assertThat(structureReport.getK()).isEqualTo(K);
        assertThat(fixedLengthReport.getPerSubset().keySet())
                .isEqualTo(structureReport.getPerSubset().keySet())
                .containsExactlyInAnyOrder("plain", "table", "code", "breadcrumb", "json");
        // 配置快照是两份报告可比性的锚，必须真的记下了「变的是什么」
        assertThat(fixedLengthReport.getConfigSnapshot()).containsEntry("chunking", "fixed-length");
        assertThat(structureReport.getConfigSnapshot()).containsEntry("chunking", "structure");
    }

    @Test
    @DisplayName("B 侧块 ID 确定性: 重复切分产出同一套 ID(前缀判中的前提)")
    void structureChunkIds_areDeterministic() {
        List<String> first = EvalCorpus.structureChunks().stream().map(DocumentChunk::getId).toList();
        List<String> second = EvalCorpus.structureChunks().stream().map(DocumentChunk::getId).toList();

        assertThat(first).isEqualTo(second);
        assertThat(first).doesNotHaveDuplicates();
        assertThat(first).allSatisfy(id -> assertThat(id).contains("#"));
    }

    private static double recall(EvalReport report, String subset) {
        EvalReport.SubsetMetrics metrics = report.subset(subset);
        assertThat(metrics).as("报告里缺少子集 %s", subset).isNotNull();
        return metrics.getRecallAtK();
    }
}

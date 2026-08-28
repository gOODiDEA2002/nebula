package io.nebula.ai.rag.fusion;

import io.nebula.ai.rag.retriever.RetrievalResult;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * RRF 融合的特征测试
 * <p>
 * 金标是固定输入下的固定输出，逐位对齐生产在用的实现
 * （{@code score(d) = SUM(weight_i / (60 + rank_i + 1))}，rank 从 0 起算）。
 * 融合算法是纯函数，因此可以把期望值直接算死；分数一旦漂移，
 * 检索结果的顺序就会跟着变，靠肉眼看排序是发现不了的。
 */
class RrfFusionStrategyTest {

    /** 浮点比较容差：金标是十进制展开的精确值，这里只防最后一位的表示误差 */
    private static final double EPSILON = 1e-12;

    private final RrfFusionStrategy strategy = new RrfFusionStrategy();

    @Test
    void twoWayFusion_matchesGoldenScoresAndOrder() {
        List<RetrievalResult> vector = List.of(
                result("A", "内容A", "vector"), result("B", "内容B", "vector"), result("C", "内容C", "vector"));
        List<RetrievalResult> keyword = List.of(
                result("C", "内容C", "keyword"), result("A", "内容A", "keyword"), result("D", "内容D", "keyword"));

        List<RetrievalResult> fused = strategy.fuse(
                List.of(vector, keyword), List.of(0.6, 0.4), 3);

        assertThat(fused).extracting(RetrievalResult::getId).containsExactly("A", "C", "B");
        // A = 0.6/61 + 0.4/62, C = 0.6/63 + 0.4/61, B = 0.6/62
        assertThat(fused.get(0).getScore()).isCloseTo(0.016287678476996297, within(EPSILON));
        assertThat(fused.get(1).getScore()).isCloseTo(0.01608118657298985, within(EPSILON));
        assertThat(fused.get(2).getScore()).isCloseTo(0.00967741935483871, within(EPSILON));
        assertThat(fused).extracting(RetrievalResult::getSource)
                .containsOnly(RrfFusionStrategy.SOURCE);
    }

    @Test
    void topK_truncatesAfterFusionNotBefore() {
        List<RetrievalResult> vector = List.of(
                result("A", "内容A", "vector"), result("B", "内容B", "vector"), result("C", "内容C", "vector"));
        List<RetrievalResult> keyword = List.of(
                result("C", "内容C", "keyword"), result("A", "内容A", "keyword"), result("D", "内容D", "keyword"));

        // C 在向量路只排第 3，靠关键词路的第 1 名才升到融合后的第 2
        assertThat(strategy.fuse(List.of(vector, keyword), List.of(0.6, 0.4), 2))
                .extracting(RetrievalResult::getId).containsExactly("A", "C");
    }

    /**
     * 跨路同 ID 时元数据保留谁：默认保留最先命中的那份，
     * sourcePriority 命中的来源可以覆盖。图谱路带回的结构化元数据业务价值更高，
     * 这条一旦失效，命中图谱的查询会静默丢掉关系信息。
     */
    @Test
    void sourcePriority_keepsPreferredSourceMetadataOnIdCollision() {
        List<RetrievalResult> vector = List.of(
                RetrievalResult.builder().id("X").content("向量版内容")
                        .metadata(Map.of("from", "vector")).score(0.9).source("vector").build());
        List<RetrievalResult> graph = List.of(
                RetrievalResult.builder().id("X").content("图谱版内容")
                        .metadata(Map.of("from", "graph", "relation", "supplies")).score(0.5)
                        .source("graph").build());

        RrfFusionStrategy graphPreferred = new RrfFusionStrategy(60, List.of("graph"));
        List<RetrievalResult> fused = graphPreferred.fuse(
                List.of(vector, graph), List.of(0.6, 1.5), 5);

        assertThat(fused).hasSize(1);
        assertThat(fused.get(0).getMetadata()).containsEntry("from", "graph")
                .containsEntry("relation", "supplies");
        assertThat(fused.get(0).getContent()).isEqualTo("图谱版内容");
        // 分数仍是两路之和: 0.6/61 + 1.5/61
        assertThat(fused.get(0).getScore()).isCloseTo(0.03442622950819672, within(EPSILON));
    }

    @Test
    void withoutSourcePriority_keepsFirstSeenMetadataOnIdCollision() {
        List<RetrievalResult> vector = List.of(
                RetrievalResult.builder().id("X").content("向量版内容")
                        .metadata(Map.of("from", "vector")).score(0.9).source("vector").build());
        List<RetrievalResult> graph = List.of(
                RetrievalResult.builder().id("X").content("图谱版内容")
                        .metadata(Map.of("from", "graph")).score(0.5).source("graph").build());

        List<RetrievalResult> fused = strategy.fuse(List.of(vector, graph), List.of(0.6, 1.5), 5);

        assertThat(fused.get(0).getMetadata()).containsEntry("from", "vector");
        assertThat(fused.get(0).getContent()).isEqualTo("向量版内容");
    }

    @Test
    void nullId_isSkippedInsteadOfCrashing() {
        List<RetrievalResult> withNullId = new ArrayList<>();
        withNullId.add(result(null, "无 ID", "vector"));
        withNullId.add(result("A", "内容A", "vector"));

        List<RetrievalResult> fused = strategy.fuse(List.of(withNullId), List.of(1.0), 5);

        assertThat(fused).extracting(RetrievalResult::getId).containsExactly("A");
    }

    @Test
    void emptyListContributesNothing() {
        List<RetrievalResult> vector = List.of(result("A", "内容A", "vector"));

        List<RetrievalResult> withEmpty = strategy.fuse(
                List.of(vector, List.of()), List.of(0.6, 0.4), 5);
        List<RetrievalResult> withoutEmpty = strategy.fuse(List.of(vector), List.of(0.6), 5);

        assertThat(withEmpty).hasSize(1);
        assertThat(withEmpty.get(0).getScore()).isEqualTo(withoutEmpty.get(0).getScore());
    }

    @Test
    void customRrfK_changesScoreButKeepsFormula() {
        List<RetrievalResult> vector = List.of(result("A", "内容A", "vector"));

        List<RetrievalResult> fused = new RrfFusionStrategy(10, List.of())
                .fuse(List.of(vector), List.of(1.0), 5);

        // 1.0 / (10 + 0 + 1)
        assertThat(fused.get(0).getScore()).isCloseTo(1.0 / 11, within(EPSILON));
    }

    @Test
    void mismatchedWeights_failFast() {
        assertThatThrownBy(() -> strategy.fuse(
                List.of(List.of(result("A", "内容A", "vector"))), List.of(0.6, 0.4), 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weights");
    }

    @Test
    void nonPositiveRrfK_rejectedAtConstruction() {
        assertThatThrownBy(() -> new RrfFusionStrategy(0, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static RetrievalResult result(String id, String content, String source) {
        return RetrievalResult.builder()
                .id(id).content(content).metadata(Map.of("from", source)).score(1.0).source(source)
                .build();
    }
}

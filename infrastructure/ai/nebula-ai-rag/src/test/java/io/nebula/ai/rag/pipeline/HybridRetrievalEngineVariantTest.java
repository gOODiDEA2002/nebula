package io.nebula.ai.rag.pipeline;

import io.nebula.ai.rag.fusion.FusionStrategy;
import io.nebula.ai.rag.fusion.RrfFusionStrategy;
import io.nebula.ai.rag.retriever.RetrievalResult;
import io.nebula.ai.rag.retriever.Retriever;
import io.nebula.ai.rag.transform.QueryVariant;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 变体重载的行为契约（P5）
 * <p>
 * 核心是「单变体重载 = 旧字符串路径逐字段一致」，以及「权重 = 检索器权重 × 变体权重」、
 * 「跨变体同 ID 由 RRF 去重合并」。等价性再叠加既有 {@code HybridRetrievalEngineTest}
 * 的零修改全绿，共同证明变体重载没有引入行为漂移。
 */
class HybridRetrievalEngineVariantTest {

    @Test
    void singleVariant_matchesLegacyStringPathFieldByField() {
        List<RetrievalResult> vectorHits = List.of(hit("A", "vector"), hit("B", "vector"));
        List<RetrievalResult> keywordHits = List.of(hit("B", "keyword"), hit("C", "keyword"));

        HybridRetrievalEngine legacyEngine = new HybridRetrievalEngine(
                List.of(new RecordingRetriever("vector", 0.6, vectorHits),
                        new RecordingRetriever("keyword", 0.4, keywordHits)),
                new RrfFusionStrategy(), 2, 15_000);
        HybridRetrievalEngine variantEngine = new HybridRetrievalEngine(
                List.of(new RecordingRetriever("vector", 0.6, vectorHits),
                        new RecordingRetriever("keyword", 0.4, keywordHits)),
                new RrfFusionStrategy(), 2, 15_000);

        List<RetrievalResult> viaString = legacyEngine.retrieve("query", 3, Map.of("k", "v"));
        List<RetrievalResult> viaVariant = variantEngine.retrieve(
                List.of(new QueryVariant("query", 1.0)), 3, Map.of("k", "v"));

        assertThat(viaVariant).extracting(RetrievalResult::getId)
                .isEqualTo(viaString.stream().map(RetrievalResult::getId).toList());
        assertThat(viaVariant).extracting(RetrievalResult::getScore)
                .isEqualTo(viaString.stream().map(RetrievalResult::getScore).toList());
    }

    @Test
    void weightIsProductOfRetrieverAndVariantWeight() {
        CapturingFusion fusion = new CapturingFusion();
        HybridRetrievalEngine engine = new HybridRetrievalEngine(
                List.of(new RecordingRetriever("vector", 0.6, List.of(hit("A", "vector")))),
                fusion, 2, 15_000);

        engine.retrieve(List.of(new QueryVariant("v1", 1.0), new QueryVariant("v2", 0.5)), 5, null);

        // 一个检索器 × 两个变体 = 两个非空任务；权重 = 0.6*1.0 与 0.6*0.5
        assertThat(fusion.lastWeights).containsExactly(0.6, 0.3);
    }

    @Test
    void sameIdAcrossVariants_isDedupedByRrf() {
        // 两个变体各命中同一 ID A，RRF 分数累加而不是产出两条
        HybridRetrievalEngine engine = new HybridRetrievalEngine(
                List.of(new RecordingRetriever("vector", 1.0, List.of(hit("A", "vector")))),
                new RrfFusionStrategy(), 2, 15_000);

        List<RetrievalResult> fused = engine.retrieve(
                List.of(new QueryVariant("v1", 1.0), new QueryVariant("v2", 1.0)), 5, null);

        assertThat(fused).extracting(RetrievalResult::getId).containsExactly("A");
    }

    @Test
    void eachVariantHitsEveryRetriever() {
        RecordingRetriever vector = new RecordingRetriever("vector", 1.0, List.of(hit("A", "vector")));
        HybridRetrievalEngine engine = new HybridRetrievalEngine(
                List.of(vector), new RrfFusionStrategy(), 2, 15_000);

        engine.retrieve(List.of(new QueryVariant("v1", 1.0), new QueryVariant("v2", 1.0)), 10, null);

        // 两个变体 → 检索器被调用两次，每次 topK*multiplier=20
        assertThat(vector.calls.get()).isEqualTo(2);
        assertThat(vector.queries).containsExactlyInAnyOrder("v1", "v2");
        assertThat(vector.lastTopK.get()).isEqualTo(20);
    }

    @Test
    void emptyVariants_failFast() {
        HybridRetrievalEngine engine = new HybridRetrievalEngine(
                List.of(new RecordingRetriever("vector", 1.0, List.of())),
                new RrfFusionStrategy(), 2, 15_000);

        assertThatThrownBy(() -> engine.retrieve(List.<QueryVariant>of(), 5, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("变体");
    }

    private static RetrievalResult hit(String id, String source) {
        return RetrievalResult.builder()
                .id(id).content("内容" + id).metadata(Map.of("from", source)).score(1.0).source(source)
                .build();
    }

    private static final class RecordingRetriever implements Retriever {

        private final String name;
        private final double weight;
        private final List<RetrievalResult> results;
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicInteger lastTopK = new AtomicInteger(-1);
        private final List<String> queries = new CopyOnWriteArrayList<>();

        RecordingRetriever(String name, double weight, List<RetrievalResult> results) {
            this.name = name;
            this.weight = weight;
            this.results = results;
        }

        @Override
        public List<RetrievalResult> retrieve(String query, int topK, Map<String, Object> filter) {
            calls.incrementAndGet();
            lastTopK.set(topK);
            queries.add(query);
            return results;
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

    /** 捕获融合入参的策略替身：只记录权重，用真 RRF 完成融合 */
    private static final class CapturingFusion implements FusionStrategy {

        private final RrfFusionStrategy delegate = new RrfFusionStrategy();
        private volatile List<Double> lastWeights = new ArrayList<>();

        @Override
        public List<RetrievalResult> fuse(List<List<RetrievalResult>> resultLists,
                                          List<Double> weights, int topK) {
            this.lastWeights = new ArrayList<>(weights);
            return delegate.fuse(resultLists, weights, topK);
        }
    }
}

package io.nebula.ai.rag.pipeline;

import io.nebula.ai.rag.fusion.RrfFusionStrategy;
import io.nebula.ai.rag.retriever.RetrievalResult;
import io.nebula.ai.rag.retriever.Retriever;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 多路并行检索引擎的行为契约
 * <p>
 * 检索器替身会记录并断言收到的 query / topK / filter，不做「忽略参数照样返回结果」的放水；
 * 候选放大、单路超时、整体降级三条都靠替身如实反映真实调用才验得住。
 */
class HybridRetrievalEngineTest {

    @Test
    void candidateMultiplier_enlargesPerRetrieverTopK() {
        RecordingRetriever vector = new RecordingRetriever("vector", 1.0, List.of(hit("A", "vector")));
        HybridRetrievalEngine engine = new HybridRetrievalEngine(
                List.of(vector), new RrfFusionStrategy(), 2, 15_000);

        engine.retrieve("空气开关", 10, Map.of("doc_type", "material_code"));

        // 每路取 topK * 2，融合后再截断；不放大的话别路的高位候选永远进不来
        assertThat(vector.lastTopK.get()).isEqualTo(20);
        assertThat(vector.lastQuery.get()).isEqualTo("空气开关");
        assertThat(vector.lastFilter.get()).containsEntry("doc_type", "material_code");
    }

    @Test
    void parallelRetrieval_fusesAllRetrieversInOrder() {
        RecordingRetriever vector = new RecordingRetriever("vector", 0.6,
                List.of(hit("A", "vector"), hit("B", "vector"), hit("C", "vector")));
        RecordingRetriever keyword = new RecordingRetriever("keyword", 0.4,
                List.of(hit("C", "keyword"), hit("A", "keyword"), hit("D", "keyword")));
        HybridRetrievalEngine engine = new HybridRetrievalEngine(
                List.of(vector, keyword), new RrfFusionStrategy(), 2, 15_000);

        List<RetrievalResult> fused = engine.retrieve("query", 3, null);

        assertThat(fused).extracting(RetrievalResult::getId).containsExactly("A", "C", "B");
        assertThat(vector.calls.get()).isEqualTo(1);
        assertThat(keyword.calls.get()).isEqualTo(1);
    }

    /**
     * 单路超时只让那一路返回空表：一路慢查询不该把整条 RAG 链路一起拖死
     */
    @Test
    void slowRetriever_timesOutAloneAndOtherResultsSurvive() {
        Retriever slow = new SleepingRetriever("slow", 1.0, 2_000, 100);
        RecordingRetriever fast = new RecordingRetriever("fast", 1.0, List.of(hit("A", "fast")));
        HybridRetrievalEngine engine = new HybridRetrievalEngine(
                List.of(fast, slow), new RrfFusionStrategy(), 2, 15_000);

        long start = System.currentTimeMillis();
        List<RetrievalResult> fused = engine.retrieve("query", 5, null);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(fused).extracting(RetrievalResult::getId).containsExactly("A");
        // 超时值是 100ms，整体耗时必须远小于慢检索器的 2000ms
        assertThat(elapsed).isLessThan(1_500);
    }

    @Test
    void failingRetriever_yieldsEmptyListWithoutFailingTheQuery() {
        Retriever failing = new ThrowingRetriever("failing", 1.0);
        RecordingRetriever healthy = new RecordingRetriever("healthy", 1.0, List.of(hit("A", "healthy")));
        HybridRetrievalEngine engine = new HybridRetrievalEngine(
                List.of(healthy, failing), new RrfFusionStrategy(), 2, 15_000);

        assertThat(engine.retrieve("query", 5, null))
                .extracting(RetrievalResult::getId).containsExactly("A");
    }

    @Test
    void allRetrieversFail_returnsEmptyInsteadOfThrowing() {
        HybridRetrievalEngine engine = new HybridRetrievalEngine(
                List.of(new ThrowingRetriever("a", 1.0), new ThrowingRetriever("b", 1.0)),
                new RrfFusionStrategy(), 2, 15_000);

        assertThat(engine.retrieve("query", 5, null)).isEmpty();
    }

    /**
     * 融合环节本身出错时退回第一个检索器：宁可只有一路也别把空结果交给上层
     */
    @Test
    void fusionFailure_degradesToFirstRetriever() {
        RecordingRetriever first = new RecordingRetriever("first", 1.0, List.of(hit("A", "first")));
        RecordingRetriever second = new RecordingRetriever("second", 1.0, List.of(hit("B", "second")));
        HybridRetrievalEngine engine = new HybridRetrievalEngine(
                List.of(first, second),
                (resultLists, weights, topK) -> {
                    throw new IllegalStateException("融合炸了");
                },
                2, 15_000);

        List<RetrievalResult> results = engine.retrieve("query", 5, null);

        assertThat(results).extracting(RetrievalResult::getId).containsExactly("A");
        // 降级路径按原始 topK 重取，不再放大
        assertThat(first.lastTopK.get()).isEqualTo(5);
    }

    /**
     * 检索器返回非正超时值时用引擎默认超时，配置项才不会变成死配置
     */
    @Test
    void nonPositiveRetrieverTimeout_fallsBackToEngineDefault() {
        SleepingRetriever slow = new SleepingRetriever("slow", 1.0, 2_000, 0);
        HybridRetrievalEngine engine = new HybridRetrievalEngine(
                List.of(slow), new RrfFusionStrategy(), 2, 100);

        long start = System.currentTimeMillis();
        assertThat(engine.retrieve("query", 5, null)).isEmpty();
        assertThat(System.currentTimeMillis() - start).isLessThan(1_500);
    }

    @Test
    void noRetriever_failsFastAtConstruction() {
        assertThatThrownBy(() -> new HybridRetrievalEngine(
                List.of(), new RrfFusionStrategy(), 2, 15_000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Retriever");
    }

    @Test
    void invalidCandidateMultiplier_failsFastAtConstruction() {
        assertThatThrownBy(() -> new HybridRetrievalEngine(
                List.of(new RecordingRetriever("a", 1.0, List.of())),
                new RrfFusionStrategy(), 0, 15_000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("candidateMultiplier");
    }

    private static RetrievalResult hit(String id, String source) {
        return RetrievalResult.builder()
                .id(id).content("内容" + id).metadata(Map.of("from", source)).score(1.0).source(source)
                .build();
    }

    /**
     * 记录调用参数的检索器替身：断言的是它真实收到的 query/topK/filter，
     * 不提供「参数随便传都返回同样结果」的宽松行为
     */
    private static final class RecordingRetriever implements Retriever {

        private final String name;
        private final double weight;
        private final List<RetrievalResult> results;
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicReference<String> lastQuery = new AtomicReference<>();
        private final AtomicInteger lastTopK = new AtomicInteger(-1);
        private final AtomicReference<Map<String, Object>> lastFilter = new AtomicReference<>();

        RecordingRetriever(String name, double weight, List<RetrievalResult> results) {
            this.name = name;
            this.weight = weight;
            this.results = results;
        }

        @Override
        public List<RetrievalResult> retrieve(String query, int topK, Map<String, Object> filter) {
            calls.incrementAndGet();
            lastQuery.set(query);
            lastTopK.set(topK);
            lastFilter.set(filter);
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

    /** 阻塞指定时长的检索器替身，用来验单路超时 */
    private record SleepingRetriever(String name, double weight, long sleepMillis, long timeout)
            implements Retriever {

        @Override
        public List<RetrievalResult> retrieve(String query, int topK, Map<String, Object> filter) {
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return List.of(hit("SLOW", name));
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public double getWeight() {
            return weight;
        }

        @Override
        public long timeoutMillis() {
            return timeout;
        }
    }

    /** 恒抛异常的检索器替身 */
    private record ThrowingRetriever(String name, double weight) implements Retriever {

        @Override
        public List<RetrievalResult> retrieve(String query, int topK, Map<String, Object> filter) {
            throw new IllegalStateException(name + " 检索失败");
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
}

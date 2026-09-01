package io.nebula.ai.rag.pipeline;

import io.nebula.ai.rag.config.RagProperties;
import io.nebula.ai.rag.fusion.RrfFusionStrategy;
import io.nebula.ai.rag.rerank.NoopReranker;
import io.nebula.ai.rag.rerank.Reranker;
import io.nebula.ai.rag.retriever.RetrievalResult;
import io.nebula.ai.rag.retriever.Retriever;
import io.nebula.ai.rag.transform.QueryTransformer;
import io.nebula.ai.rag.transform.QueryVariant;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 三文本契约与变体截断（P5）
 * <p>
 * 三文本契约：提示词用 {@code RagQuery.query} 原文（含前后空白）；检索用改写器产出的变体；
 * 重排用首个变体文本。带空白查询逐字段钉住三者，防止「一处 trim 顺手改了另一处」。
 */
class RagPipelineThreeTextContractTest {

    @Test
    void threeTexts_areRoutedToDistinctConsumers() {
        // 改写器把原文映射成一个大写变体，用以区分「检索/重排用变体」与「提示词用原文」
        QueryTransformer transformer = raw -> List.of(new QueryVariant("VARIANT-" + raw.trim(), 1.0));
        RecordingRetriever retriever = new RecordingRetriever(List.of(hit("A")));
        RecordingReranker reranker = new RecordingReranker();
        RecordingGenerator generator = new RecordingGenerator("答案");

        DefaultRagPipeline pipeline = pipeline(retriever, reranker, generator,
                new RagProperties(), transformer);

        pipeline.query(RagQuery.of("  空气开关  "));

        // 检索用变体文本
        assertThat(retriever.lastQuery.get()).isEqualTo("VARIANT-空气开关");
        // 重排用首个变体文本
        assertThat(reranker.lastQuery.get()).isEqualTo("VARIANT-空气开关");
        // 提示词用用户原文（含前后空白）
        assertThat(generator.lastPrompt.get()).contains("  空气开关  ");
    }

    @Test
    void blankPaddedQuery_isTrimmedForRetrievalButOriginalForPrompt() {
        RecordingRetriever retriever = new RecordingRetriever(List.of(hit("A")));
        RecordingGenerator generator = new RecordingGenerator("答案");

        // 默认改写器（TrimQueryTransformer）
        DefaultRagPipeline pipeline = new DefaultRagPipeline(
                new HybridRetrievalEngine(List.of(retriever), new RrfFusionStrategy(), 2, 15_000),
                new NoopReranker(),
                new ContextAssembler(4000, "[文档%d] %s\n\n"),
                (query, context) -> "问题:" + query,
                generator, new RagProperties());

        pipeline.query(RagQuery.of("  空气开关供应商  "));

        assertThat(retriever.lastQuery.get()).isEqualTo("空气开关供应商");
        assertThat(generator.lastPrompt.get()).contains("  空气开关供应商  ");
    }

    @Test
    void variantsExceedingMax_areTruncated() {
        // 改写器产出 6 个变体，max-variants=4 应截断到前 4 个
        QueryTransformer transformer = raw -> {
            List<QueryVariant> variants = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                variants.add(new QueryVariant("v" + i, 1.0));
            }
            return variants;
        };
        RecordingRetriever retriever = new RecordingRetriever(List.of(hit("A")));
        RagProperties properties = new RagProperties();
        properties.getTransform().setMaxVariants(4);

        DefaultRagPipeline pipeline = pipeline(retriever, new NoopReranker(),
                new RecordingGenerator("答案"), properties, transformer);

        pipeline.query(RagQuery.of("问题"));

        // 单检索器 × 4 变体 = 4 次调用（第 5、6 个变体被截断）
        assertThat(retriever.calls.get()).isEqualTo(4);
    }

    private static DefaultRagPipeline pipeline(Retriever retriever, Reranker reranker,
                                               AnswerGenerator generator, RagProperties properties,
                                               QueryTransformer transformer) {
        return new DefaultRagPipeline(
                new HybridRetrievalEngine(List.of(retriever), new RrfFusionStrategy(), 2, 15_000),
                reranker,
                new ContextAssembler(properties.getContext().getMaxLength(),
                        properties.getContext().getDocumentTemplate()),
                (query, context) -> "问题: " + query + "\n资料:\n" + context,
                generator, properties, transformer);
    }

    private static RetrievalResult hit(String id) {
        return RetrievalResult.builder()
                .id(id).content("内容" + id).metadata(Map.of()).score(1.0).source("vector").build();
    }

    private static final class RecordingRetriever implements Retriever {

        private final List<RetrievalResult> results;
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicReference<String> lastQuery = new AtomicReference<>();

        RecordingRetriever(List<RetrievalResult> results) {
            this.results = results;
        }

        @Override
        public List<RetrievalResult> retrieve(String query, int topK, Map<String, Object> filter) {
            calls.incrementAndGet();
            lastQuery.set(query);
            return results;
        }

        @Override
        public String getName() {
            return "recording";
        }

        @Override
        public double getWeight() {
            return 1.0;
        }
    }

    private static final class RecordingReranker implements Reranker {

        private final AtomicReference<String> lastQuery = new AtomicReference<>();

        @Override
        public List<RetrievalResult> rerank(String query, List<RetrievalResult> results, int topK) {
            lastQuery.set(query);
            return results.stream().limit(topK).toList();
        }

        @Override
        public String getName() {
            return "recording";
        }
    }

    private static final class RecordingGenerator implements AnswerGenerator {

        private final String answer;
        private final AtomicReference<String> lastPrompt = new AtomicReference<>();

        RecordingGenerator(String answer) {
            this.answer = answer;
        }

        @Override
        public String generate(String prompt, long timeoutMillis) {
            lastPrompt.set(prompt);
            return answer;
        }
    }
}

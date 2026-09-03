package io.nebula.ai.rag.pipeline;

import io.nebula.ai.rag.config.RagProperties;
import io.nebula.ai.rag.fusion.RrfFusionStrategy;
import io.nebula.ai.rag.guard.RetrievedContentSanitizer;
import io.nebula.ai.rag.metrics.NoopRagMetrics;
import io.nebula.ai.rag.metrics.RagMetrics;
import io.nebula.ai.rag.rerank.NoopReranker;
import io.nebula.ai.rag.rerank.Reranker;
import io.nebula.ai.rag.retriever.RetrievalResult;
import io.nebula.ai.rag.retriever.Retriever;
import io.nebula.ai.rag.transform.TrimQueryTransformer;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RAG 管线的行为契约
 * <p>
 * 重点在两条降级路径：无检索结果、生成超时/失败。两者都必须返回结果并显式标注 degraded，
 * 而不是抛异常或者返回一个和正常答案分不出来的字符串。
 */
class DefaultRagPipelineTest {

    @Test
    void happyPath_returnsAnswerWithReferences() {
        RecordingGenerator generator = new RecordingGenerator("这是答案");
        RecordingRetriever retriever = new RecordingRetriever(List.of(hit("A"), hit("B")));
        DefaultRagPipeline pipeline = pipeline(retriever, new NoopReranker(),
                generator, properties());

        RagAnswer answer = pipeline.query(RagQuery.of("  空气开关供应商  "));

        assertThat(answer.getAnswer()).isEqualTo("这是答案");
        assertThat(answer.getReferences()).extracting(RetrievalResult::getId).containsExactly("A", "B");
        assertThat(answer.isDegraded()).isFalse();
        assertThat(answer.getDegradeReason()).isNull();
        assertThat(answer.getCostMs()).isGreaterThanOrEqualTo(0);
        // 检索用 trim 后的查询：前后空白会影响关键词检索的分词与命中
        assertThat(retriever.lastQuery.get()).isEqualTo("空气开关供应商");
        // 提示词用用户原文：模型看到的问题应与用户输入一致，与参照实现保持同一语义
        assertThat(generator.lastPrompt.get()).contains("  空气开关供应商  ");
    }

    @Test
    void contextIsAssembledFromRerankedResultsAndPassedToPrompt() {
        RecordingGenerator generator = new RecordingGenerator("答案");
        DefaultRagPipeline pipeline = pipeline(retriever(hit("A"), hit("B")), new NoopReranker(),
                generator, properties());

        pipeline.query(RagQuery.of("问题"));

        assertThat(generator.lastPrompt.get()).contains("[文档1] 内容A").contains("[文档2] 内容B");
    }

    @Test
    void noDocument_returnsConfiguredAnswerAndMarksDegraded() {
        RagProperties properties = properties();
        properties.getDegrade().setNoDocumentAnswer("知识库里没有相关内容");
        DefaultRagPipeline pipeline = pipeline(retriever(), new NoopReranker(),
                new RecordingGenerator("不该被调用"), properties);

        RagAnswer answer = pipeline.query(RagQuery.of("查不到的东西"));

        assertThat(answer.getAnswer()).isEqualTo("知识库里没有相关内容");
        assertThat(answer.getReferences()).isEmpty();
        assertThat(answer.isDegraded()).isTrue();
        assertThat(answer.getDegradeReason()).isEqualTo(DefaultRagPipeline.REASON_NO_DOCUMENT);
    }

    @Test
    void noDocument_skipsGenerationEntirely() {
        RecordingGenerator generator = new RecordingGenerator("不该被调用");
        DefaultRagPipeline pipeline = pipeline(retriever(), new NoopReranker(), generator, properties());

        pipeline.query(RagQuery.of("查不到的东西"));

        assertThat(generator.calls.get()).isZero();
    }

    /**
     * 生成超时必须降级为检索摘要而不是失败：有资料没结论远好过什么都没有
     */
    @Test
    void generationTimeout_degradesToRetrievalSummary() {
        RagProperties properties = properties();
        properties.getGeneration().setTimeoutMs(100);
        properties.getDegrade().setFallbackHeader("检索到：\n\n");
        properties.getDegrade().setFallbackFooter("（生成暂不可用）");
        DefaultRagPipeline pipeline = pipeline(retriever(hit("A")), new NoopReranker(),
                (prompt, timeoutMillis) -> {
                    try {
                        Thread.sleep(3_000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return "太慢了";
                }, properties);

        long start = System.currentTimeMillis();
        RagAnswer answer = pipeline.query(RagQuery.of("问题"));

        assertThat(System.currentTimeMillis() - start).isLessThan(2_500);
        assertThat(answer.isDegraded()).isTrue();
        assertThat(answer.getDegradeReason()).isEqualTo(DefaultRagPipeline.REASON_GENERATION_TIMEOUT);
        assertThat(answer.getAnswer()).startsWith("检索到：").contains("内容A").endsWith("（生成暂不可用）");
        // 生成挂掉不影响引用返回
        assertThat(answer.getReferences()).extracting(RetrievalResult::getId).containsExactly("A");
    }

    @Test
    void generationFailure_degradesToRetrievalSummary() {
        DefaultRagPipeline pipeline = pipeline(retriever(hit("A")), new NoopReranker(),
                (prompt, timeoutMillis) -> {
                    throw new IllegalStateException("模型挂了");
                }, properties());

        RagAnswer answer = pipeline.query(RagQuery.of("问题"));

        assertThat(answer.isDegraded()).isTrue();
        assertThat(answer.getDegradeReason()).isEqualTo(DefaultRagPipeline.REASON_GENERATION_FAILED);
        assertThat(answer.getReferences()).hasSize(1);
    }

    @Test
    void longContent_isTruncatedInFallbackSummary() {
        RagProperties properties = properties();
        properties.getDegrade().setFallbackExcerptLength(5);
        String longContent = "0123456789";
        DefaultRagPipeline pipeline = pipeline(
                retriever(RetrievalResult.builder().id("A").content(longContent).score(1.0)
                        .source("vector").build()),
                new NoopReranker(),
                (prompt, timeoutMillis) -> {
                    throw new IllegalStateException("模型挂了");
                }, properties);

        assertThat(pipeline.query(RagQuery.of("问题")).getAnswer()).contains("01234…");
    }

    @Test
    void generateAnswerFalse_returnsReferencesOnlyWithoutCallingGenerator() {
        RecordingGenerator generator = new RecordingGenerator("不该被调用");
        DefaultRagPipeline pipeline = pipeline(retriever(hit("A")), new NoopReranker(),
                generator, properties());

        RagAnswer answer = pipeline.query(RagQuery.builder()
                .query("问题").generateAnswer(false).build());

        assertThat(answer.getAnswer()).isNull();
        assertThat(answer.getReferences()).hasSize(1);
        assertThat(answer.isDegraded()).isFalse();
        assertThat(generator.calls.get()).isZero();
    }

    @Test
    void rerankDisabledPerRequest_truncatesWithoutCallingReranker() {
        RecordingReranker reranker = new RecordingReranker();
        RagProperties properties = properties();
        properties.getRerank().setTopK(2);
        DefaultRagPipeline pipeline = pipeline(retriever(hit("A"), hit("B"), hit("C")), reranker,
                new RecordingGenerator("答案"), properties);

        RagAnswer answer = pipeline.query(RagQuery.builder()
                .query("问题").enableRerank(false).build());

        assertThat(reranker.calls.get()).isZero();
        assertThat(answer.getReferences()).extracting(RetrievalResult::getId).containsExactly("A", "B");
    }

    @Test
    void rerankEnabled_passesQueryAndConfiguredTopKToReranker() {
        RecordingReranker reranker = new RecordingReranker();
        RagProperties properties = properties();
        properties.getRerank().setTopK(2);
        DefaultRagPipeline pipeline = pipeline(retriever(hit("A"), hit("B"), hit("C")), reranker,
                new RecordingGenerator("答案"), properties);

        pipeline.query(RagQuery.of("空气开关"));

        assertThat(reranker.calls.get()).isEqualTo(1);
        assertThat(reranker.lastQuery.get()).isEqualTo("空气开关");
        assertThat(reranker.lastTopK.get()).isEqualTo(2);
        assertThat(reranker.lastCandidates.get()).hasSize(3);
    }

    @Test
    void requestTopK_overridesConfiguredTopK() {
        RecordingRetriever retriever = new RecordingRetriever(List.of(hit("A")));
        DefaultRagPipeline pipeline = pipeline(retriever, new NoopReranker(),
                new RecordingGenerator("答案"), properties());

        pipeline.query(RagQuery.builder().query("问题").topK(3).build());

        // 引擎按候选放大倍数 2 请求，因此检索器收到 3 * 2
        assertThat(retriever.lastTopK.get()).isEqualTo(6);
    }

    @Test
    void blankQuery_failsFast() {
        DefaultRagPipeline pipeline = pipeline(retriever(hit("A")), new NoopReranker(),
                new RecordingGenerator("答案"), properties());

        assertThatThrownBy(() -> pipeline.query(RagQuery.of("   ")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pipeline.query(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- R4 追加：清洗、引用模式、引用后处理、构造器等价、指标 ----

    @Test
    void sanitizeRunsBeforeRerank_rerankerSeesCleanedContent() {
        // 清洗器把正文整值改为 CLEANED；重排器应收到已清洗内容（清洗在融合后、重排前，R4-D4）
        RecordingReranker reranker = new RecordingReranker();
        DefaultRagPipeline pipeline = pipeline(retriever(hit("A"), hit("B")), reranker,
                new RecordingGenerator("答案"), properties(),
                replaceContentSanitizer("CLEANED"), new NoopCitationPostProcessor(), new NoopRagMetrics());

        pipeline.query(RagQuery.of("问题"));

        assertThat(reranker.lastCandidates.get()).extracting(RetrievalResult::getContent)
                .containsOnly("CLEANED");
    }

    @Test
    void sanitizeToEmpty_degradesToNoDocument() {
        // 清洗后候选全空：与检索为空同路径走 no-document 降级（R4 §9 攻击面 9）
        RecordingGenerator generator = new RecordingGenerator("不该被调用");
        DefaultRagPipeline pipeline = pipeline(retriever(hit("A"), hit("B")), new NoopReranker(),
                generator, properties(),
                result -> null, new NoopCitationPostProcessor(), new NoopRagMetrics());

        RagAnswer answer = pipeline.query(RagQuery.of("问题"));

        assertThat(answer.isDegraded()).isTrue();
        assertThat(answer.getDegradeReason()).isEqualTo(DefaultRagPipeline.REASON_NO_DOCUMENT);
        assertThat(answer.getReferences()).isEmpty();
        assertThat(generator.calls.get()).isZero();
    }

    @Test
    void referencesModeAll_returnsAllRerankedReferences() {
        RagProperties properties = properties();
        properties.getContext().setMaxLength(15); // 只容得下一篇文档
        properties.getContext().setReferencesMode("all");
        DefaultRagPipeline pipeline = pipeline(retriever(hit("A"), hit("B")), new NoopReranker(),
                new RecordingGenerator("答案"), properties);

        RagAnswer answer = pipeline.query(RagQuery.of("问题"));

        assertThat(answer.getReferences()).extracting(RetrievalResult::getId).containsExactly("A", "B");
    }

    @Test
    void referencesModeIncluded_returnsOnlyReferencesInContext() {
        RagProperties properties = properties();
        properties.getContext().setMaxLength(15); // 只容得下一篇文档
        properties.getContext().setReferencesMode("included");
        DefaultRagPipeline pipeline = pipeline(retriever(hit("A"), hit("B")), new NoopReranker(),
                new RecordingGenerator("答案"), properties);

        RagAnswer answer = pipeline.query(RagQuery.of("问题"));

        assertThat(answer.getReferences()).extracting(RetrievalResult::getId).containsExactly("A");
    }

    @Test
    void citationPostProcessor_runsOnGenerationSuccess() {
        RecordingCitation citation = new RecordingCitation();
        DefaultRagPipeline pipeline = pipeline(retriever(hit("A")), new NoopReranker(),
                new RecordingGenerator("原始答案"), properties(),
                new NoopSanitizer(), citation, new NoopRagMetrics());

        RagAnswer answer = pipeline.query(RagQuery.of("问题"));

        assertThat(citation.calls.get()).isEqualTo(1);
        assertThat(answer.getAnswer()).isEqualTo("CITED:原始答案");
    }

    @Test
    void citationPostProcessor_skippedOnGenerationFailure() {
        RecordingCitation citation = new RecordingCitation();
        DefaultRagPipeline pipeline = pipeline(retriever(hit("A")), new NoopReranker(),
                (prompt, timeoutMillis) -> {
                    throw new IllegalStateException("模型挂了");
                }, properties(),
                new NoopSanitizer(), citation, new NoopRagMetrics());

        RagAnswer answer = pipeline.query(RagQuery.of("问题"));

        assertThat(citation.calls.get()).isZero();
        assertThat(answer.isDegraded()).isTrue();
        assertThat(answer.getAnswer()).doesNotStartWith("CITED:");
    }

    @Test
    void sixArgConstructor_behavesLikeFullConstructorWithDefaults() {
        RagProperties properties = properties();
        DefaultRagPipeline sixArg = pipeline(retriever(hit("A"), hit("B")), new NoopReranker(),
                new RecordingGenerator("答案"), properties);
        DefaultRagPipeline fullArg = pipeline(retriever(hit("A"), hit("B")), new NoopReranker(),
                new RecordingGenerator("答案"), properties,
                new NoopSanitizer(), new NoopCitationPostProcessor(), new NoopRagMetrics());

        RagAnswer a = sixArg.query(RagQuery.of("问题"));
        RagAnswer b = fullArg.query(RagQuery.of("问题"));

        assertThat(a.getAnswer()).isEqualTo(b.getAnswer());
        assertThat(a.getReferences()).extracting(RetrievalResult::getId)
                .isEqualTo(b.getReferences().stream().map(RetrievalResult::getId).toList());
        assertThat(a.isDegraded()).isEqualTo(b.isDegraded());
    }

    @Test
    void metrics_recordsEachStageAndQuery() {
        RecordingMetrics metrics = new RecordingMetrics();
        DefaultRagPipeline pipeline = pipeline(retriever(hit("A")), new NoopReranker(),
                new RecordingGenerator("答案"), properties(),
                new NoopSanitizer(), new NoopCitationPostProcessor(), metrics);

        pipeline.query(RagQuery.of("问题"));

        assertThat(metrics.stages).contains("retrieval", "rerank", "assemble", "generation");
        assertThat(metrics.queryCalls.get()).isEqualTo(1);
    }

    private static DefaultRagPipeline pipeline(Retriever retriever, Reranker reranker,
                                               AnswerGenerator generator, RagProperties properties) {
        HybridRetrievalEngine engine = new HybridRetrievalEngine(
                List.of(retriever), new RrfFusionStrategy(), 2, 15_000);
        return new DefaultRagPipeline(engine, reranker,
                new ContextAssembler(properties.getContext().getMaxLength(),
                        properties.getContext().getDocumentTemplate()),
                (query, context) -> "问题: " + query + "\n资料:\n" + context,
                generator, properties);
    }

    private static DefaultRagPipeline pipeline(Retriever retriever, Reranker reranker,
                                               AnswerGenerator generator, RagProperties properties,
                                               RetrievedContentSanitizer sanitizer,
                                               CitationPostProcessor citation, RagMetrics metrics) {
        HybridRetrievalEngine engine = new HybridRetrievalEngine(
                List.of(retriever), new RrfFusionStrategy(), 2, 15_000);
        return new DefaultRagPipeline(engine, reranker,
                new ContextAssembler(properties.getContext().getMaxLength(),
                        properties.getContext().getDocumentTemplate()),
                (query, context) -> "问题: " + query + "\n资料:\n" + context,
                generator, properties, new TrimQueryTransformer(), sanitizer, citation, null, metrics);
    }

    private static RetrievedContentSanitizer replaceContentSanitizer(String replacement) {
        return result -> RetrievalResult.builder()
                .id(result.getId()).content(replacement).metadata(result.getMetadata())
                .score(result.getScore()).source(result.getSource()).build();
    }

    /** 原样返回的清洗器测试替身 */
    private static final class NoopSanitizer implements RetrievedContentSanitizer {
        @Override
        public RetrievalResult sanitize(RetrievalResult result) {
            return result;
        }
    }

    /** 记录调用次数并给答案加前缀的引用后处理替身 */
    private static final class RecordingCitation implements CitationPostProcessor {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public String process(String answer, ContextAssembly assembly) {
            calls.incrementAndGet();
            return "CITED:" + answer;
        }
    }

    /** 记录各阶段与整次查询的指标替身 */
    private static final class RecordingMetrics implements RagMetrics {
        private final Set<String> stages = ConcurrentHashMap.newKeySet();
        private final AtomicInteger queryCalls = new AtomicInteger();

        @Override
        public void recordStage(String stage, long durationNanos, String outcome) {
            stages.add(stage);
        }

        @Override
        public void recordQuery(long durationNanos, boolean degraded, String reason) {
            queryCalls.incrementAndGet();
        }

        @Override
        public void recordRerankPassthrough(String reason) {
        }
    }

    private static RagProperties properties() {
        return new RagProperties();
    }

    private static RetrievalResult hit(String id) {
        return RetrievalResult.builder()
                .id(id).content("内容" + id).metadata(Map.of()).score(1.0).source("vector").build();
    }

    private static RecordingRetriever retriever(RetrievalResult... results) {
        return new RecordingRetriever(List.of(results));
    }

    /** 记录调用参数的检索器替身 */
    private static final class RecordingRetriever implements Retriever {

        private final List<RetrievalResult> results;
        private final AtomicInteger lastTopK = new AtomicInteger(-1);
        private final AtomicReference<String> lastQuery = new AtomicReference<>();

        RecordingRetriever(List<RetrievalResult> results) {
            this.results = results;
        }

        @Override
        public List<RetrievalResult> retrieve(String query, int topK, Map<String, Object> filter) {
            lastTopK.set(topK);
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

    /** 记录调用参数的重排序器替身：不改顺序但如实记录收到的 query 与 topK */
    private static final class RecordingReranker implements Reranker {

        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicReference<String> lastQuery = new AtomicReference<>();
        private final AtomicInteger lastTopK = new AtomicInteger(-1);
        private final AtomicReference<List<RetrievalResult>> lastCandidates = new AtomicReference<>();

        @Override
        public List<RetrievalResult> rerank(String query, List<RetrievalResult> results, int topK) {
            calls.incrementAndGet();
            lastQuery.set(query);
            lastTopK.set(topK);
            lastCandidates.set(new ArrayList<>(results));
            return results.stream().limit(topK).toList();
        }

        @Override
        public String getName() {
            return "recording";
        }
    }

    /** 记录 prompt 的生成器替身 */
    private static final class RecordingGenerator implements AnswerGenerator {

        private final String answer;
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicReference<String> lastPrompt = new AtomicReference<>();

        RecordingGenerator(String answer) {
            this.answer = answer;
        }

        @Override
        public String generate(String prompt, long timeoutMillis) {
            calls.incrementAndGet();
            lastPrompt.set(prompt);
            return answer;
        }
    }
}

package io.nebula.ai.rag.pipeline;

import io.nebula.ai.rag.config.RagProperties;
import io.nebula.ai.rag.guard.NoopRetrievedContentSanitizer;
import io.nebula.ai.rag.guard.RetrievedContentSanitizer;
import io.nebula.ai.rag.metrics.NoopRagMetrics;
import io.nebula.ai.rag.metrics.RagMetrics;
import io.nebula.ai.rag.rerank.Reranker;
import io.nebula.ai.rag.retriever.RetrievalResult;
import io.nebula.ai.rag.transform.QueryTransformer;
import io.nebula.ai.rag.transform.QueryVariant;
import io.nebula.ai.rag.transform.TrimQueryTransformer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 默认 RAG 管线实现
 * <p>
 * 五步：预处理（trim）-> 多路检索 -> 清洗（R4）-> 重排（可关）-> 上下文拼接 -> 答案生成。
 * <p>
 * 两条降级路径都必须返回结果而不是抛异常：
 * <ul>
 *   <li>一条都没检索到（或清洗后为空）：返回可配的固定答复，引用为空；</li>
 *   <li>生成超时或失败：返回基于检索片段的摘要，引用照常返回。</li>
 * </ul>
 * 检索结果先于生成算好，正是为了让生成挂掉时引用仍然拿得出来 ——
 * 对用户而言「有资料没结论」远好过「什么都没有」。
 * <p>
 * 生成环节在虚拟线程里跑并由本类统一超时：{@link AnswerGenerator} 是可替换端口，
 * 不能指望每个实现都自己守住超时。
 * <p>
 * R4 新增四个可选协作者（清洗、引用后处理、流式生成、指标），全部走 Noop/缺省默认，
 * 默认装配下行为与 R3 完全一致。流式实现只用 {@link StreamingAnswerGenerator} 端口，
 * <b>不</b>从容器直接取 {@code ReactiveChatService}（总纲红线）。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class DefaultRagPipeline implements RagPipeline {

    private static final Logger log = LoggerFactory.getLogger(DefaultRagPipeline.class);

    /** 降级原因：一条都没检索到 */
    public static final String REASON_NO_DOCUMENT = "no-document";

    /** 降级原因：生成超时 */
    public static final String REASON_GENERATION_TIMEOUT = "generation-timeout";

    /** 降级原因：生成失败 */
    public static final String REASON_GENERATION_FAILED = "generation-failed";

    /** 流式不可用原因：未装配 StreamingAnswerGenerator */
    public static final String REASON_STREAMING_UNSUPPORTED = "streaming-unsupported";

    private static final String REFERENCES_MODE_INCLUDED = "included";

    private final HybridRetrievalEngine retrievalEngine;
    private final Reranker reranker;
    private final ContextAssembler contextAssembler;
    private final RagPromptRenderer promptRenderer;
    private final AnswerGenerator answerGenerator;
    private final RagProperties properties;
    private final QueryTransformer queryTransformer;
    private final RetrievedContentSanitizer sanitizer;
    private final CitationPostProcessor citationPostProcessor;
    private final StreamingAnswerGenerator streamingGenerator;
    private final RagMetrics metrics;

    /**
     * 现状六参构造：查询改写器默认 {@link TrimQueryTransformer}，其余可选协作者取 Noop 默认（行为不变）。
     */
    public DefaultRagPipeline(HybridRetrievalEngine retrievalEngine,
                              Reranker reranker,
                              ContextAssembler contextAssembler,
                              RagPromptRenderer promptRenderer,
                              AnswerGenerator answerGenerator,
                              RagProperties properties) {
        this(retrievalEngine, reranker, contextAssembler, promptRenderer, answerGenerator,
                properties, new TrimQueryTransformer());
    }

    /**
     * 七参构造：显式注入查询改写器（P5），其余可选协作者取 Noop 默认（行为不变）。
     */
    public DefaultRagPipeline(HybridRetrievalEngine retrievalEngine,
                              Reranker reranker,
                              ContextAssembler contextAssembler,
                              RagPromptRenderer promptRenderer,
                              AnswerGenerator answerGenerator,
                              RagProperties properties,
                              QueryTransformer queryTransformer) {
        this(retrievalEngine, reranker, contextAssembler, promptRenderer, answerGenerator,
                properties, queryTransformer,
                new NoopRetrievedContentSanitizer(), new NoopCitationPostProcessor(), null, new NoopRagMetrics());
    }

    /**
     * 十一参完整构造（R4）：显式注入清洗器、引用后处理器、流式生成器（可为 {@code null}）与指标。
     */
    public DefaultRagPipeline(HybridRetrievalEngine retrievalEngine,
                              Reranker reranker,
                              ContextAssembler contextAssembler,
                              RagPromptRenderer promptRenderer,
                              AnswerGenerator answerGenerator,
                              RagProperties properties,
                              QueryTransformer queryTransformer,
                              RetrievedContentSanitizer sanitizer,
                              CitationPostProcessor citationPostProcessor,
                              StreamingAnswerGenerator streamingGenerator,
                              RagMetrics metrics) {
        this.retrievalEngine = require(retrievalEngine, "HybridRetrievalEngine");
        this.reranker = require(reranker, "Reranker");
        this.contextAssembler = require(contextAssembler, "ContextAssembler");
        this.promptRenderer = require(promptRenderer, "RagPromptRenderer");
        this.answerGenerator = require(answerGenerator, "AnswerGenerator");
        this.properties = require(properties, "RagProperties");
        this.queryTransformer = require(queryTransformer, "QueryTransformer");
        this.sanitizer = require(sanitizer, "RetrievedContentSanitizer");
        this.citationPostProcessor = require(citationPostProcessor, "CitationPostProcessor");
        // streamingGenerator 允许为 null：未装配时 queryStream 返回「暂不支持」
        this.streamingGenerator = streamingGenerator;
        this.metrics = metrics != null ? metrics : new NoopRagMetrics();
    }

    @Override
    public RagAnswer query(RagQuery query) {
        requireValidQuery(query);

        long start = System.currentTimeMillis();
        long queryStartNanos = System.nanoTime();

        // 三文本契约：提示词用原文；检索用变体；重排用首个变体文本
        List<QueryVariant> variants = resolveVariants(query.getQuery());
        String rerankQuery = variants.get(0).getText();

        List<RetrievalResult> sanitized = retrieveAndSanitize(query, variants, rerankQuery);
        if (sanitized.isEmpty()) {
            return finishQuery(buildNoDocumentAnswer(start), queryStartNanos);
        }

        long rerankNanos = System.nanoTime();
        List<RetrievalResult> references = applyRerank(rerankQuery, sanitized, query);
        metrics.recordStage("rerank", System.nanoTime() - rerankNanos, "success");

        if (!query.isGenerateAnswer()) {
            return finishQuery(RagAnswer.builder()
                    .references(references)
                    .costMs(System.currentTimeMillis() - start)
                    .build(), queryStartNanos);
        }

        long assembleNanos = System.nanoTime();
        ContextAssembly assembly = contextAssembler.assembleDetailed(references);
        metrics.recordStage("assemble", System.nanoTime() - assembleNanos, "success");

        String prompt = promptRenderer.render(query.getQuery(), assembly.getContext());
        List<RetrievalResult> outputReferences = resolveOutputReferences(references, assembly);
        return finishQuery(generate(prompt, references, outputReferences, assembly, start), queryStartNanos);
    }

    @Override
    public Flux<RagStreamEvent> queryStream(RagQuery query) {
        if (query == null || query.getQuery() == null || query.getQuery().isBlank()) {
            return Flux.error(new IllegalArgumentException("RAG 查询文本不能为空"));
        }
        if (streamingGenerator == null) {
            // 端口缺席：不执行检索，直接「暂不支持」，避免白跑一次检索（R4 §5.3、§9 攻击面 17）
            return Flux.just(RagStreamEvent.error(REASON_STREAMING_UNSUPPORTED,
                    "当前 RagPipeline 未装配 StreamingAnswerGenerator"));
        }
        // 检索、清洗、重排、组装是同步阻塞调用，放到 boundedElastic 执行，绝不占用调用方事件循环线程
        return Flux.defer(() -> buildStream(query))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 组装流式事件序列（R4 §5.3 事件契约）。在订阅线程（boundedElastic）上完成检索、清洗、重排、组装，
     * 再拼接生成阶段的 DELTA 与终态。
     */
    private Flux<RagStreamEvent> buildStream(RagQuery query) {
        long start = System.currentTimeMillis();
        long queryStartNanos = System.nanoTime();

        List<QueryVariant> variants = resolveVariants(query.getQuery());
        String rerankQuery = variants.get(0).getText();

        List<RetrievalResult> sanitized = retrieveAndSanitize(query, variants, rerankQuery);
        if (sanitized.isEmpty()) {
            String noDoc = properties.getDegrade().getNoDocumentAnswer();
            recordQuery(queryStartNanos, true, REASON_NO_DOCUMENT);
            return Flux.just(
                    RagStreamEvent.references(List.of()),
                    RagStreamEvent.delta(noDoc),
                    RagStreamEvent.complete(RagAnswer.builder()
                            .answer(noDoc)
                            .references(List.of())
                            .costMs(System.currentTimeMillis() - start)
                            .degraded(true)
                            .degradeReason(REASON_NO_DOCUMENT)
                            .build()));
        }

        long rerankNanos = System.nanoTime();
        List<RetrievalResult> references = applyRerank(rerankQuery, sanitized, query);
        metrics.recordStage("rerank", System.nanoTime() - rerankNanos, "success");

        if (!query.isGenerateAnswer()) {
            recordQuery(queryStartNanos, false, "none");
            return Flux.just(
                    RagStreamEvent.references(references),
                    RagStreamEvent.complete(RagAnswer.builder()
                            .references(references)
                            .costMs(System.currentTimeMillis() - start)
                            .build()));
        }

        long assembleNanos = System.nanoTime();
        ContextAssembly assembly = contextAssembler.assembleDetailed(references);
        metrics.recordStage("assemble", System.nanoTime() - assembleNanos, "success");
        String prompt = promptRenderer.render(query.getQuery(), assembly.getContext());
        List<RetrievalResult> outputReferences = resolveOutputReferences(references, assembly);

        return Flux.concat(
                Flux.just(RagStreamEvent.references(references)),
                streamDeltas(prompt, references, outputReferences, assembly, start, queryStartNanos));
    }

    /**
     * 生成阶段的 DELTA 流与终态（R4 §5.3）。
     * <p>
     * 总时限用 {@code takeUntilOther(Mono.delay)} 实现（从订阅到终态，非字间间隔）：companion 到点抛
     * {@link TimeoutException}，主序列以错误结束再由 {@code onErrorResume} 归一化。首个 DELTA 之前失败/超时
     * 降级为「摘要 DELTA + COMPLETE(degraded)」；之后失败/超时只发 {@code ERROR}，不再补摘要（避免拼接错乱）。
     */
    private Flux<RagStreamEvent> streamDeltas(String prompt,
                                              List<RetrievalResult> summaryRefs,
                                              List<RetrievalResult> outputRefs,
                                              ContextAssembly assembly,
                                              long start,
                                              long queryStartNanos) {
        long timeoutMs = properties.getGeneration().getTimeoutMs();
        StringBuilder accumulator = new StringBuilder();
        AtomicBoolean anyDelta = new AtomicBoolean(false);
        long generationStartNanos = System.nanoTime();

        Flux<RagStreamEvent> deltas = streamingGenerator.generateStream(prompt)
                .takeUntilOther(Mono.delay(Duration.ofMillis(timeoutMs))
                        .flatMap(tick -> Mono.error(new TimeoutException("流式生成总时限超时"))))
                .map(chunk -> {
                    anyDelta.set(true);
                    accumulator.append(chunk);
                    return RagStreamEvent.delta(chunk);
                });

        Mono<RagStreamEvent> completion = Mono.fromSupplier(() -> {
            metrics.recordStage("generation", System.nanoTime() - generationStartNanos, "success");
            String finalAnswer = citationPostProcessor.process(accumulator.toString(), assembly);
            recordQuery(queryStartNanos, false, "none");
            return RagStreamEvent.complete(RagAnswer.builder()
                    .answer(finalAnswer)
                    .references(outputRefs)
                    .costMs(System.currentTimeMillis() - start)
                    .build());
        });

        return deltas.concatWith(completion)
                .onErrorResume(error -> {
                    boolean timeout = error instanceof TimeoutException;
                    String reason = timeout ? REASON_GENERATION_TIMEOUT : REASON_GENERATION_FAILED;
                    String outcome = timeout ? "timeout" : "failure";
                    metrics.recordStage("generation", System.nanoTime() - generationStartNanos, outcome);
                    if (anyDelta.get()) {
                        // 已有部分文本：只发 ERROR，不补摘要（R4-D7）
                        log.warn("RAG 流式生成在首个片段之后失败({})，以 ERROR 终止", reason);
                        recordQuery(queryStartNanos, true, reason);
                        return Flux.just(RagStreamEvent.error(reason, error.getMessage()));
                    }
                    // 首个片段之前失败：降级为检索摘要 DELTA + COMPLETE(degraded)
                    log.warn("RAG 流式生成在首个片段之前失败({})，降级为检索摘要", reason);
                    String fallback = buildFallbackAnswer(summaryRefs);
                    recordQuery(queryStartNanos, true, reason);
                    return Flux.just(
                            RagStreamEvent.delta(fallback),
                            RagStreamEvent.complete(RagAnswer.builder()
                                    .answer(fallback)
                                    .references(outputRefs)
                                    .costMs(System.currentTimeMillis() - start)
                                    .degraded(true)
                                    .degradeReason(reason)
                                    .build()));
                });
    }

    /**
     * 检索并清洗：检索为空或清洗后为空都返回空列表（上层走 no-document 降级）。
     */
    private List<RetrievalResult> retrieveAndSanitize(RagQuery query, List<QueryVariant> variants,
                                                      String rerankQuery) {
        int retrievalTopK = query.getTopK() != null
                ? query.getTopK() : properties.getRetrieval().getTopK();

        long retrievalNanos = System.nanoTime();
        List<RetrievalResult> retrieved;
        try {
            retrieved = retrievalEngine.retrieve(variants, retrievalTopK, query.getFilter());
        } catch (RuntimeException e) {
            metrics.recordStage("retrieval", System.nanoTime() - retrievalNanos, "failure");
            throw e;
        }
        metrics.recordStage("retrieval", System.nanoTime() - retrievalNanos, "success");

        if (retrieved.isEmpty()) {
            log.warn("RAG 未检索到相关文档: query='{}'", truncateLog(rerankQuery));
            return List.of();
        }

        List<RetrievalResult> sanitized = sanitizer.sanitizeAll(retrieved);
        if (sanitized.isEmpty()) {
            log.warn("RAG 检索结果经清洗后为空: query='{}'", truncateLog(rerankQuery));
        }
        return sanitized;
    }

    private RagAnswer buildNoDocumentAnswer(long start) {
        return RagAnswer.builder()
                .answer(properties.getDegrade().getNoDocumentAnswer())
                .references(List.of())
                .costMs(System.currentTimeMillis() - start)
                .degraded(true)
                .degradeReason(REASON_NO_DOCUMENT)
                .build();
    }

    /**
     * 产出查询变体：改写器不得返回空列表；超过 {@code transform.max-variants} 的截断并 warn
     */
    private List<QueryVariant> resolveVariants(String rawQuery) {
        List<QueryVariant> variants = queryTransformer.transform(rawQuery);
        if (variants == null || variants.isEmpty()) {
            throw new IllegalStateException(
                    "QueryTransformer 返回空变体列表: 检索没有可用查询, 属于配置事故");
        }
        int maxVariants = properties.getTransform().getMaxVariants();
        if (maxVariants > 0 && variants.size() > maxVariants) {
            log.warn("查询变体数 {} 超过上限 {}, 截断保留前 {} 个",
                    variants.size(), maxVariants, maxVariants);
            return variants.subList(0, maxVariants);
        }
        return variants;
    }

    private List<RetrievalResult> applyRerank(String query, List<RetrievalResult> retrieved,
                                              RagQuery request) {
        int rerankTopK = properties.getRerank().getTopK();
        boolean enabled = request.getEnableRerank() != null
                ? request.getEnableRerank() : properties.getRerank().isEnabled();
        if (!enabled) {
            return retrieved.stream().limit(rerankTopK).toList();
        }
        return reranker.rerank(query, retrieved, rerankTopK);
    }

    /**
     * 按 {@code context.references-mode} 决定返回的引用：{@code included} 取实际入选引用，
     * 否则（{@code all}）取重排后的全部。
     */
    private List<RetrievalResult> resolveOutputReferences(List<RetrievalResult> references,
                                                          ContextAssembly assembly) {
        String mode = properties.getContext().getReferencesMode();
        if (REFERENCES_MODE_INCLUDED.equalsIgnoreCase(mode)) {
            return assembly.getIncludedReferences();
        }
        return references;
    }

    /**
     * 生成答案；超时或失败时降级为检索摘要。生成成功时对完整文本做引用后处理（降级摘要不做）。
     */
    private RagAnswer generate(String prompt, List<RetrievalResult> summaryRefs,
                               List<RetrievalResult> outputRefs, ContextAssembly assembly, long start) {
        long timeoutMs = properties.getGeneration().getTimeoutMs();
        long generationNanos = System.nanoTime();
        String degradeReason = null;
        String outcome = "success";
        String answer;

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> future = executor.submit(() -> answerGenerator.generate(prompt, timeoutMs));
            try {
                String raw = future.get(timeoutMs, TimeUnit.MILLISECONDS);
                answer = citationPostProcessor.process(raw, assembly);
            } catch (TimeoutException e) {
                future.cancel(true);
                log.warn("RAG 答案生成超时({}ms)，返回检索摘要降级", timeoutMs);
                answer = buildFallbackAnswer(summaryRefs);
                degradeReason = REASON_GENERATION_TIMEOUT;
                outcome = "timeout";
            } catch (InterruptedException e) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                answer = buildFallbackAnswer(summaryRefs);
                degradeReason = REASON_GENERATION_FAILED;
                outcome = "failure";
            } catch (Exception e) {
                log.warn("RAG 答案生成失败，返回检索摘要降级: {}", e.getMessage());
                answer = buildFallbackAnswer(summaryRefs);
                degradeReason = REASON_GENERATION_FAILED;
                outcome = "failure";
            } finally {
                executor.shutdownNow();
            }
        }

        metrics.recordStage("generation", System.nanoTime() - generationNanos, outcome);
        return RagAnswer.builder()
                .answer(answer)
                .references(outputRefs)
                .costMs(System.currentTimeMillis() - start)
                .degraded(degradeReason != null)
                .degradeReason(degradeReason)
                .build();
    }

    /**
     * 基于检索片段拼一份摘要答案
     */
    private String buildFallbackAnswer(List<RetrievalResult> results) {
        RagProperties.Degrade degrade = properties.getDegrade();
        if (results == null || results.isEmpty()) {
            return degrade.getNoDocumentAnswer();
        }
        int excerptLength = degrade.getFallbackExcerptLength();
        StringBuilder sb = new StringBuilder(degrade.getFallbackHeader());
        for (int i = 0; i < results.size(); i++) {
            String content = results.get(i).getContent();
            if (content == null) {
                continue;
            }
            String excerpt = content.length() > excerptLength
                    ? content.substring(0, excerptLength) + "…" : content;
            sb.append(i + 1).append(". ").append(excerpt).append("\n\n");
        }
        sb.append(degrade.getFallbackFooter());
        return sb.toString().trim();
    }

    private RagAnswer finishQuery(RagAnswer answer, long queryStartNanos) {
        recordQuery(queryStartNanos, answer.isDegraded(), answer.getDegradeReason());
        return answer;
    }

    private void recordQuery(long queryStartNanos, boolean degraded, String reason) {
        metrics.recordQuery(System.nanoTime() - queryStartNanos, degraded,
                reason == null ? "none" : reason);
    }

    private static void requireValidQuery(RagQuery query) {
        if (query == null || query.getQuery() == null || query.getQuery().isBlank()) {
            throw new IllegalArgumentException("RAG 查询文本不能为空");
        }
    }

    private static String truncateLog(String text) {
        return text != null && text.length() > 80 ? text.substring(0, 80) + "..." : text;
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value;
    }
}

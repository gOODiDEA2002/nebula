package io.nebula.ai.rag.pipeline;

import io.nebula.ai.rag.config.RagProperties;
import io.nebula.ai.rag.rerank.Reranker;
import io.nebula.ai.rag.retriever.RetrievalResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 默认 RAG 管线实现
 * <p>
 * 五步：预处理（trim）-> 多路检索 -> 重排（可关）-> 上下文拼接 -> 答案生成。
 * <p>
 * 两条降级路径都必须返回结果而不是抛异常：
 * <ul>
 *   <li>一条都没检索到：返回可配的固定答复，引用为空；</li>
 *   <li>生成超时或失败：返回基于检索片段的摘要，引用照常返回。</li>
 * </ul>
 * 检索结果先于生成算好，正是为了让生成挂掉时引用仍然拿得出来 ——
 * 对用户而言「有资料没结论」远好过「什么都没有」。
 * <p>
 * 生成环节在虚拟线程里跑并由本类统一超时：{@link AnswerGenerator} 是可替换端口，
 * 不能指望每个实现都自己守住超时。
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

    private final HybridRetrievalEngine retrievalEngine;
    private final Reranker reranker;
    private final ContextAssembler contextAssembler;
    private final RagPromptRenderer promptRenderer;
    private final AnswerGenerator answerGenerator;
    private final RagProperties properties;

    public DefaultRagPipeline(HybridRetrievalEngine retrievalEngine,
                              Reranker reranker,
                              ContextAssembler contextAssembler,
                              RagPromptRenderer promptRenderer,
                              AnswerGenerator answerGenerator,
                              RagProperties properties) {
        this.retrievalEngine = require(retrievalEngine, "HybridRetrievalEngine");
        this.reranker = require(reranker, "Reranker");
        this.contextAssembler = require(contextAssembler, "ContextAssembler");
        this.promptRenderer = require(promptRenderer, "RagPromptRenderer");
        this.answerGenerator = require(answerGenerator, "AnswerGenerator");
        this.properties = require(properties, "RagProperties");
    }

    @Override
    public RagAnswer query(RagQuery query) {
        if (query == null || query.getQuery() == null || query.getQuery().isBlank()) {
            throw new IllegalArgumentException("RAG 查询文本不能为空");
        }

        long start = System.currentTimeMillis();
        String processedQuery = query.getQuery().trim();

        int retrievalTopK = query.getTopK() != null
                ? query.getTopK() : properties.getRetrieval().getTopK();
        List<RetrievalResult> retrieved =
                retrievalEngine.retrieve(processedQuery, retrievalTopK, query.getFilter());

        if (retrieved.isEmpty()) {
            log.warn("RAG 未检索到相关文档: query='{}'", truncateLog(processedQuery));
            return RagAnswer.builder()
                    .answer(properties.getDegrade().getNoDocumentAnswer())
                    .references(List.of())
                    .costMs(System.currentTimeMillis() - start)
                    .degraded(true)
                    .degradeReason(REASON_NO_DOCUMENT)
                    .build();
        }

        List<RetrievalResult> references = applyRerank(processedQuery, retrieved, query);

        if (!query.isGenerateAnswer()) {
            return RagAnswer.builder()
                    .references(references)
                    .costMs(System.currentTimeMillis() - start)
                    .build();
        }

        String context = contextAssembler.assemble(references);
        String prompt = promptRenderer.render(query.getQuery(), context);
        return generate(prompt, references, start);
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
     * 生成答案；超时或失败时降级为检索摘要
     */
    private RagAnswer generate(String prompt, List<RetrievalResult> references, long start) {
        long timeoutMs = properties.getGeneration().getTimeoutMs();
        String degradeReason = null;
        String answer;

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> future = executor.submit(() -> answerGenerator.generate(prompt, timeoutMs));
            try {
                answer = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                log.warn("RAG 答案生成超时({}ms)，返回检索摘要降级", timeoutMs);
                answer = buildFallbackAnswer(references);
                degradeReason = REASON_GENERATION_TIMEOUT;
            } catch (InterruptedException e) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                answer = buildFallbackAnswer(references);
                degradeReason = REASON_GENERATION_FAILED;
            } catch (Exception e) {
                log.warn("RAG 答案生成失败，返回检索摘要降级: {}", e.getMessage());
                answer = buildFallbackAnswer(references);
                degradeReason = REASON_GENERATION_FAILED;
            } finally {
                executor.shutdownNow();
            }
        }

        return RagAnswer.builder()
                .answer(answer)
                .references(references)
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

package io.nebula.ai.rag.pipeline;

import io.nebula.ai.rag.fusion.FusionStrategy;
import io.nebula.ai.rag.retriever.RetrievalResult;
import io.nebula.ai.rag.retriever.Retriever;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 多路并行检索引擎
 * <p>
 * 并行调用容器内所有 {@link Retriever}，各路独立超时、独立失败，再由
 * {@link FusionStrategy} 融合。设计要点：
 * <ul>
 *   <li><b>单路超时不拖垮整条链路：</b>某一路慢查询只让那一路返回空表，
 *       而不是让整个 RAG 请求一起卡在最慢的那条上；</li>
 *   <li><b>候选放大：</b>各路取 {@code topK * candidateMultiplier} 条，融合后再截断，
 *       否则一路的第 topK+1 名即便在别路排第一也永远进不了融合；</li>
 *   <li><b>整体失败降级：</b>融合环节自身出错时退回列表中第一个检索器的结果，
 *       宁可只有一路也别让上层拿到空。</li>
 * </ul>
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class HybridRetrievalEngine {

    private static final Logger log = LoggerFactory.getLogger(HybridRetrievalEngine.class);

    private final List<Retriever> retrievers;
    private final FusionStrategy fusionStrategy;
    private final int candidateMultiplier;
    private final long defaultTimeoutMillis;

    /**
     * @param retrievers           有序检索器列表，第一个同时充当整体失败时的降级路径
     * @param fusionStrategy       融合策略
     * @param candidateMultiplier  候选放大倍数
     * @param defaultTimeoutMillis 检索器未给出正超时值时使用的默认超时
     */
    public HybridRetrievalEngine(List<Retriever> retrievers, FusionStrategy fusionStrategy,
                                 int candidateMultiplier, long defaultTimeoutMillis) {
        if (retrievers == null || retrievers.isEmpty()) {
            throw new IllegalArgumentException(
                    "至少需要一个 Retriever: 没有检索器的 RAG 管线只会返回空结果, 属于配置事故而不是可用状态");
        }
        if (fusionStrategy == null) {
            throw new IllegalArgumentException("FusionStrategy 不能为空");
        }
        if (candidateMultiplier < 1) {
            throw new IllegalArgumentException("candidateMultiplier 至少为 1");
        }
        if (defaultTimeoutMillis <= 0) {
            throw new IllegalArgumentException("defaultTimeoutMillis 必须为正数");
        }
        this.retrievers = List.copyOf(retrievers);
        this.fusionStrategy = fusionStrategy;
        this.candidateMultiplier = candidateMultiplier;
        this.defaultTimeoutMillis = defaultTimeoutMillis;
    }

    public List<Retriever> getRetrievers() {
        return retrievers;
    }

    /**
     * 多路并行检索并融合
     *
     * @param query  查询文本
     * @param topK   融合后保留数量
     * @param filter 过滤条件，可为 null
     * @return 融合后的结果列表
     */
    public List<RetrievalResult> retrieve(String query, int topK, Map<String, Object> filter) {
        long start = System.currentTimeMillis();
        int candidateTopK = topK * candidateMultiplier;

        try {
            List<CompletableFuture<List<RetrievalResult>>> futures = new ArrayList<>(retrievers.size());
            for (Retriever retriever : retrievers) {
                futures.add(retrieveAsync(retriever, query, candidateTopK, filter));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            List<List<RetrievalResult>> resultLists = new ArrayList<>(retrievers.size());
            List<Double> weights = new ArrayList<>(retrievers.size());
            StringBuilder perRetrieverCounts = new StringBuilder();
            for (int i = 0; i < retrievers.size(); i++) {
                List<RetrievalResult> results = futures.get(i).get();
                perRetrieverCounts.append(retrievers.get(i).getName())
                        .append('=').append(results.size()).append(' ');
                // 空表对 RRF 没有任何贡献，直接跳过以免徒增一路空权重
                if (!results.isEmpty()) {
                    resultLists.add(results);
                    weights.add(retrievers.get(i).getWeight());
                }
            }

            List<RetrievalResult> fused = fusionStrategy.fuse(resultLists, weights, topK);
            log.info("混合检索完成: {}fused={}, 耗时 {}ms",
                    perRetrieverCounts, fused.size(), System.currentTimeMillis() - start);
            return fused;
        } catch (Exception e) {
            Retriever fallback = retrievers.get(0);
            log.error("混合检索失败, 降级为 {} 单路检索: {}", fallback.getName(), e.getMessage());
            return fallback.retrieve(query, topK, filter);
        }
    }

    /**
     * 单路异步检索：超时或失败都收敛成空表，不向上抛
     */
    private CompletableFuture<List<RetrievalResult>> retrieveAsync(
            Retriever retriever, String query, int topK, Map<String, Object> filter) {
        long timeoutMillis = retriever.timeoutMillis() > 0
                ? retriever.timeoutMillis() : defaultTimeoutMillis;
        return CompletableFuture
                .supplyAsync(() -> {
                    List<RetrievalResult> results = retriever.retrieve(query, topK, filter);
                    return results != null ? results : Collections.<RetrievalResult>emptyList();
                })
                .orTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> {
                    log.warn("{} 检索超时或失败: {}", retriever.getName(), ex.getMessage());
                    return Collections.emptyList();
                });
    }
}

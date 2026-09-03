package io.nebula.ai.rag.rerank.http;

import io.nebula.ai.rag.metrics.NoopRagMetrics;
import io.nebula.ai.rag.metrics.RagMetrics;
import io.nebula.ai.rag.rerank.Reranker;
import io.nebula.ai.rag.rerank.http.RerankWireCodec.ScoredIndex;
import io.nebula.ai.rag.retriever.RetrievalResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 通过 HTTP 调用交叉编码服务打分的重排器（R4 §3，Y8：批量、独立超时、失败直通）
 * <p>
 * 与 {@code LlmScoringReranker} 的锦上添花定位一致：重排失败绝不成为检索链路的新故障点。
 * 任何异常（超时、非 2xx、解码失败、条数/下标不符）都在本类内吞掉，回退到<b>融合原序截断</b>，
 * 与 {@code rerank.enabled=false} 走同一条路径，管线看不到差异，并记一次 {@code recordRerankPassthrough}。
 * <p>
 * <b>不做「部分批成功就混排」</b>：未打分候选的相对位置无定义，混排会产生不可解释的排序（R4 §3.2）。
 * <p>
 * 分数语义：以交叉编码分<b>替换</b> {@code score}，不与融合分加权；{@code source} 保留，
 * {@code metadata} 原样透传不注入新键，避免污染消费方 Map。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class HttpCrossEncoderReranker implements Reranker {

    /** 重排器名称 */
    public static final String NAME = "http-cross-encoder";

    private static final Logger log = LoggerFactory.getLogger(HttpCrossEncoderReranker.class);

    private final HttpClient httpClient;
    private final URI url;
    private final RerankWireCodec codec;
    private final String model;
    private final String apiKey;
    private final long timeoutMillis;
    private final int batchSize;
    private final int maxCharsPerDoc;
    private final RagMetrics metrics;

    public HttpCrossEncoderReranker(HttpClient httpClient, URI url, RerankWireCodec codec,
                                    String model, String apiKey, long timeoutMillis,
                                    int batchSize, int maxCharsPerDoc, RagMetrics metrics) {
        if (httpClient == null) {
            throw new IllegalArgumentException("HttpClient 不能为空");
        }
        if (url == null) {
            throw new IllegalArgumentException("重排服务 URL 不能为空");
        }
        if (codec == null) {
            throw new IllegalArgumentException("RerankWireCodec 不能为空");
        }
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis 必须为正数");
        }
        this.httpClient = httpClient;
        this.url = url;
        this.codec = codec;
        this.model = model;
        this.apiKey = apiKey;
        this.timeoutMillis = timeoutMillis;
        this.batchSize = batchSize;
        this.maxCharsPerDoc = maxCharsPerDoc;
        this.metrics = metrics != null ? metrics : new NoopRagMetrics();
    }

    @Override
    public List<RetrievalResult> rerank(String query, List<RetrievalResult> results, int topK) {
        if (results == null || results.isEmpty() || topK <= 0) {
            return List.of();
        }
        // 单条不需要排序：排一个不会淘汰任何东西的序毫无意义，直接截断返回，也不发请求
        if (results.size() <= 1) {
            return results.stream().limit(topK).toList();
        }

        int n = results.size();
        int batch = batchSize <= 0 ? n : batchSize;
        List<String> texts = buildTexts(results);
        List<int[]> ranges = buildRanges(n, batch);

        List<CompletableFuture<HttpResponse<String>>> futures = new ArrayList<>(ranges.size());
        for (int[] range : ranges) {
            String body = codec.encode(query, texts.subList(range[0], range[1]), model);
            futures.add(httpClient.sendAsync(buildRequest(body), HttpResponse.BodyHandlers.ofString()));
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            // 协调预算 get(timeoutMillis) 到点：整体超时
            return passthrough(results, topK, "timeout", futures);
        } catch (ExecutionException e) {
            // 某批先异常完成：请求级超时（与协调预算等值，可能先触发）按 timeout，其余传输错误按 http-error
            String reason = e.getCause() instanceof HttpTimeoutException ? "timeout" : "http-error";
            return passthrough(results, topK, reason, futures);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return passthrough(results, topK, "http-error", futures);
        }

        double[] scores = new double[n];
        boolean[] filled = new boolean[n];
        for (int i = 0; i < ranges.size(); i++) {
            int[] range = ranges.get(i);
            int size = range[1] - range[0];
            HttpResponse<String> response = futures.get(i).join();
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return passthrough(results, topK, "http-error", futures);
            }
            List<ScoredIndex> decoded;
            try {
                decoded = codec.decode(response.body());
            } catch (RuntimeException ex) {
                return passthrough(results, topK, "decode-error", futures);
            }
            if (decoded.size() != size) {
                return passthrough(results, topK, "mismatch", futures);
            }
            for (ScoredIndex scored : decoded) {
                int local = scored.index();
                if (local < 0 || local >= size) {
                    return passthrough(results, topK, "mismatch", futures);
                }
                int global = range[0] + local;
                if (filled[global]) {
                    return passthrough(results, topK, "mismatch", futures);
                }
                scores[global] = scored.score();
                filled[global] = true;
            }
        }

        List<RetrievalResult> reranked = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            RetrievalResult original = results.get(i);
            reranked.add(RetrievalResult.builder()
                    .id(original.getId())
                    .content(original.getContent())
                    .metadata(original.getMetadata())
                    .score(scores[i])
                    .source(original.getSource())
                    .build());
        }
        reranked.sort(Comparator.comparingDouble(RetrievalResult::getScore).reversed());
        return reranked.stream().limit(topK).toList();
    }

    @Override
    public String getName() {
        return NAME;
    }

    private List<String> buildTexts(List<RetrievalResult> results) {
        List<String> texts = new ArrayList<>(results.size());
        for (RetrievalResult result : results) {
            String content = result.getContent() == null ? "" : result.getContent();
            if (maxCharsPerDoc > 0 && content.length() > maxCharsPerDoc) {
                content = content.substring(0, maxCharsPerDoc);
            }
            texts.add(content);
        }
        return texts;
    }

    private List<int[]> buildRanges(int n, int batch) {
        List<int[]> ranges = new ArrayList<>();
        for (int start = 0; start < n; start += batch) {
            ranges.add(new int[]{start, Math.min(start + batch, n)});
        }
        return ranges;
    }

    private HttpRequest buildRequest(String body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(url)
                .timeout(Duration.ofMillis(timeoutMillis))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }
        return builder.build();
    }

    /**
     * 失败直通：取消未决请求、记一次直通指标、warn 一条（只带 URL 与原因，绝不带响应头/体），
     * 回退融合原序截断（与 {@code rerank.enabled=false} 同一路径）。
     */
    private List<RetrievalResult> passthrough(List<RetrievalResult> results, int topK,
                                              String reason, List<CompletableFuture<HttpResponse<String>>> futures) {
        futures.forEach(future -> future.cancel(true));
        metrics.recordRerankPassthrough(reason);
        log.warn("HTTP 交叉编码重排直通(原因={}, url={}, 候选数={}), 回退融合原序", reason, url, results.size());
        return results.stream().limit(topK).toList();
    }
}

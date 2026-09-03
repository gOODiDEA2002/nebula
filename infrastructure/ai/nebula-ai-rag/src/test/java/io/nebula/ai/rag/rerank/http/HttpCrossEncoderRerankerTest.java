package io.nebula.ai.rag.rerank.http;

import io.nebula.ai.rag.metrics.RagMetrics;
import io.nebula.ai.rag.retriever.RetrievalResult;
import io.nebula.core.common.util.JsonUtils;
import tools.jackson.databind.JsonNode;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HTTP 交叉编码重排的端到端契约（R4 §3.2、§10）
 * <p>
 * 用 JDK {@link HttpServer} 起真实端点，覆盖：成功打分改序、分批回填、四类失败直通
 * （timeout/http-error/decode-error/mismatch）、Authorization 头、单条不发请求。
 */
class HttpCrossEncoderRerankerTest {

    private final TeiRerankWireCodec codec = new TeiRerankWireCodec();
    private final RecordingMetrics metrics = new RecordingMetrics();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void success_replacesScoresAndReordersByCrossEncoderScore() {
        URI url = start(scoringHandler(new AtomicInteger(), new AtomicReference<>()));
        HttpCrossEncoderReranker reranker = reranker(url, 10, 2000, null);

        List<RetrievalResult> out = reranker.rerank("q", List.of(hit("d0", "a"), hit("d1", "b"), hit("d2", "c")), 3);

        // 分数按首字符：a=97 < b=98 < c=99，降序应为 c, b, a
        assertThat(out).extracting(RetrievalResult::getId).containsExactly("d2", "d1", "d0");
        assertThat(out.get(0).getScore()).isEqualTo((double) 'c');
        // metadata/source 透传不变
        assertThat(out.get(0).getSource()).isEqualTo("hybrid");
    }

    @Test
    void batching_splitsIntoMultipleRequestsAndBackfillsGlobalIndex() {
        AtomicInteger requestCount = new AtomicInteger();
        URI url = start(scoringHandler(requestCount, new AtomicReference<>()));
        HttpCrossEncoderReranker reranker = reranker(url, 1, 2000, null);

        List<RetrievalResult> out = reranker.rerank("q", List.of(hit("d0", "a"), hit("d1", "b"), hit("d2", "c")), 3);

        assertThat(requestCount.get()).isEqualTo(3);
        assertThat(out).extracting(RetrievalResult::getId).containsExactly("d2", "d1", "d0");
    }

    @Test
    void topK_truncatesAfterRerank() {
        URI url = start(scoringHandler(new AtomicInteger(), new AtomicReference<>()));
        HttpCrossEncoderReranker reranker = reranker(url, 10, 2000, null);

        List<RetrievalResult> out = reranker.rerank("q", List.of(hit("d0", "a"), hit("d1", "b"), hit("d2", "c")), 2);

        assertThat(out).extracting(RetrievalResult::getId).containsExactly("d2", "d1");
    }

    @Test
    void timeout_passesThroughFusedOrder() {
        URI url = start(exchange -> {
            sleep(400);
            respond(exchange, 200, "[]");
        });
        HttpCrossEncoderReranker reranker = reranker(url, 10, 120, null);

        List<RetrievalResult> input = List.of(hit("d0", "a"), hit("d1", "b"));
        List<RetrievalResult> out = reranker.rerank("q", input, 2);

        assertThat(out).extracting(RetrievalResult::getId).containsExactly("d0", "d1");
        assertThat(metrics.lastReason).isEqualTo("timeout");
    }

    @Test
    void httpError_passesThroughFusedOrder() {
        URI url = start(exchange -> respond(exchange, 500, "boom"));
        HttpCrossEncoderReranker reranker = reranker(url, 10, 2000, null);

        List<RetrievalResult> out = reranker.rerank("q", List.of(hit("d0", "a"), hit("d1", "b")), 2);

        assertThat(out).extracting(RetrievalResult::getId).containsExactly("d0", "d1");
        assertThat(metrics.lastReason).isEqualTo("http-error");
    }

    @Test
    void decodeError_passesThroughFusedOrder() {
        URI url = start(exchange -> respond(exchange, 200, "not-json"));
        HttpCrossEncoderReranker reranker = reranker(url, 10, 2000, null);

        List<RetrievalResult> out = reranker.rerank("q", List.of(hit("d0", "a"), hit("d1", "b")), 2);

        assertThat(out).extracting(RetrievalResult::getId).containsExactly("d0", "d1");
        assertThat(metrics.lastReason).isEqualTo("decode-error");
    }

    @Test
    void countMismatch_passesThroughFusedOrder() {
        // 请求 2 条，只返回 1 条
        URI url = start(exchange -> respond(exchange, 200, "[{\"index\":0,\"score\":0.9}]"));
        HttpCrossEncoderReranker reranker = reranker(url, 10, 2000, null);

        List<RetrievalResult> out = reranker.rerank("q", List.of(hit("d0", "a"), hit("d1", "b")), 2);

        assertThat(out).extracting(RetrievalResult::getId).containsExactly("d0", "d1");
        assertThat(metrics.lastReason).isEqualTo("mismatch");
    }

    @Test
    void apiKey_setsAuthorizationHeader() {
        AtomicReference<String> auth = new AtomicReference<>();
        URI url = start(scoringHandler(new AtomicInteger(), auth));
        HttpCrossEncoderReranker reranker = reranker(url, 10, 2000, "secret-token");

        reranker.rerank("q", List.of(hit("d0", "a"), hit("d1", "b")), 2);

        assertThat(auth.get()).isEqualTo("Bearer secret-token");
    }

    @Test
    void batchSizeTwo_fiveCandidates_issuesThreeRequests() {
        AtomicInteger requestCount = new AtomicInteger();
        URI url = start(scoringHandler(requestCount, new AtomicReference<>()));
        HttpCrossEncoderReranker reranker = reranker(url, 2, 2000, null);

        List<RetrievalResult> out = reranker.rerank("q", List.of(
                hit("d0", "a"), hit("d1", "b"), hit("d2", "c"), hit("d3", "d"), hit("d4", "e")), 5);

        // 5 候选、batch-size=2 → 2+2+1 = 3 个请求；按首字符降序 e>d>c>b>a
        assertThat(requestCount.get()).isEqualTo(3);
        assertThat(out).extracting(RetrievalResult::getId).containsExactly("d4", "d3", "d2", "d1", "d0");
    }

    @Test
    void maxCharsPerDoc_truncatesTextSentToServer() {
        // 端点按收到正文的长度打分，验证发送前已按 maxCharsPerDoc 截断
        URI url = start(exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode texts = JsonUtils.parseJson(body).get("texts");
            List<Map<String, Object>> results = new ArrayList<>();
            for (int i = 0; i < texts.size(); i++) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("index", i);
                item.put("score", (double) texts.get(i).asText().length());
                results.add(item);
            }
            respond(exchange, 200, JsonUtils.toJson(results));
        });
        HttpClient client = HttpClient.newHttpClient();
        HttpCrossEncoderReranker reranker =
                new HttpCrossEncoderReranker(client, url, codec, "model", null, 2000, 10, 3, metrics);

        List<RetrievalResult> out = reranker.rerank("q",
                List.of(hit("d0", "abcdefghij"), hit("d1", "xy")), 2);

        // d0 正文 10 字符被截到 3；d1 正文 2 字符不截断 → 分数 3 > 2，d0 在前
        assertThat(out).extracting(RetrievalResult::getId).containsExactly("d0", "d1");
        assertThat(out.get(0).getScore()).isEqualTo(3.0);
        assertThat(out.get(1).getScore()).isEqualTo(2.0);
        // 输出仍保留原始完整正文，仅替换分数
        assertThat(out.get(0).getContent()).isEqualTo("abcdefghij");
    }

    @Test
    void singleResult_returnsWithoutRequest() {
        AtomicInteger requestCount = new AtomicInteger();
        URI url = start(scoringHandler(requestCount, new AtomicReference<>()));
        HttpCrossEncoderReranker reranker = reranker(url, 10, 2000, null);

        List<RetrievalResult> out = reranker.rerank("q", List.of(hit("d0", "a")), 5);

        assertThat(out).extracting(RetrievalResult::getId).containsExactly("d0");
        assertThat(requestCount.get()).isZero();
    }

    // ---- helpers ----

    private HttpCrossEncoderReranker reranker(URI url, int batchSize, long timeoutMillis, String apiKey) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMillis))
                .build();
        return new HttpCrossEncoderReranker(client, url, codec, "model", apiKey,
                timeoutMillis, batchSize, 0, metrics);
    }

    /** 按候选正文首字符打分的 TEI 端点：分数越大越靠前，用于验证改序与分批回填 */
    private HttpHandler scoringHandler(AtomicInteger requestCount, AtomicReference<String> authHolder) {
        return exchange -> {
            requestCount.incrementAndGet();
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader != null) {
                authHolder.set(authHeader);
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode texts = JsonUtils.parseJson(body).get("texts");
            List<Map<String, Object>> results = new ArrayList<>();
            for (int i = 0; i < texts.size(); i++) {
                String text = texts.get(i).asText();
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("index", i);
                item.put("score", text.isEmpty() ? 0.0 : (double) text.charAt(0));
                results.add(item);
            }
            respond(exchange, 200, JsonUtils.toJson(results));
        };
    }

    private URI start(HttpHandler handler) {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/rerank", handler);
            server.start();
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/rerank");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static RetrievalResult hit(String id, String content) {
        return RetrievalResult.builder()
                .id(id)
                .content(content)
                .metadata(Map.of("k", "v"))
                .score(0.0)
                .source("hybrid")
                .build();
    }

    /** 捕获最后一次直通原因的测试替身 */
    private static final class RecordingMetrics implements RagMetrics {
        private volatile String lastReason;

        @Override
        public void recordStage(String stage, long durationNanos, String outcome) {
        }

        @Override
        public void recordQuery(long durationNanos, boolean degraded, String reason) {
        }

        @Override
        public void recordRerankPassthrough(String reason) {
            this.lastReason = reason;
        }
    }
}

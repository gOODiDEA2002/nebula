package io.nebula.examples.rag.controller;

import io.nebula.ai.rag.eval.EvalReport;
import io.nebula.ai.rag.pipeline.RagStreamEvent;
import io.nebula.core.common.result.Result;
import io.nebula.examples.rag.service.RagDemoService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 演示的七个 REST 端点，统一 {@link Result} 外壳。
 * <p>
 * RAG 关闭（{@code AI_ENABLED} 缺省）时全部端点返回禁用提示、HTTP 200、不抛错。
 *
 * @author Nebula Framework
 */
@RestController
@RequestMapping("/rag")
public class RagDemoController {

    private static final String DISABLED_MESSAGE =
            "RAG 未启用：设置 AI_ENABLED=true 并配置向量库与模型端点后重试";

    private final RagDemoService ragDemoService;

    public RagDemoController(RagDemoService ragDemoService) {
        this.ragDemoService = ragDemoService;
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        return Result.success(ragDemoService.status());
    }

    @PostMapping("/index")
    public Result<Map<String, Object>> index() {
        if (!ragDemoService.isEnabled()) {
            return disabled();
        }
        return Result.success(ragDemoService.index());
    }

    @PostMapping("/search")
    public Result<List<Map<String, Object>>> search(@RequestBody RagRequest request) {
        if (!ragDemoService.isEnabled()) {
            return Result.success(List.of(), DISABLED_MESSAGE);
        }
        return Result.success(ragDemoService.search(request.query(), request.topK()));
    }

    @PostMapping("/query")
    public Result<Map<String, Object>> query(@RequestBody RagRequest request) {
        if (!ragDemoService.isEnabled()) {
            return disabled();
        }
        return Result.success(ragDemoService.query(request.query(), request.topK()));
    }

    /**
     * 流式端点。启用态返回 {@code Flux<RagStreamEvent>}（SSE）；
     * 禁用态返回普通 JSON {@link Result} 禁用提示（同为 HTTP 200）。
     */
    @PostMapping(value = "/query/stream",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    public Object queryStream(@RequestBody RagRequest request) {
        if (!ragDemoService.isEnabled()) {
            return disabled();
        }
        return ragDemoService.queryStream(request.query(), request.topK());
    }

    @GetMapping("/eval")
    public Result<EvalReport> eval() {
        if (!ragDemoService.isEnabled()) {
            return Result.success(null, DISABLED_MESSAGE);
        }
        return Result.success(ragDemoService.eval());
    }

    @DeleteMapping("/documents")
    public Result<Map<String, Object>> clearDocuments() {
        if (!ragDemoService.isEnabled()) {
            return disabled();
        }
        return Result.success(ragDemoService.clearDocuments());
    }

    private static Result<Map<String, Object>> disabled() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("enabled", false);
        return Result.success(body, DISABLED_MESSAGE);
    }

    /**
     * 检索 / 问答请求体。
     *
     * @param query 查询文本
     * @param topK  返回数量，缺省走 {@code nebula.ai.rag.retrieval.top-k}
     */
    public record RagRequest(String query, Integer topK) {
    }
}

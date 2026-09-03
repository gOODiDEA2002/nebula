package io.nebula.ai.rag.rerank.http;

import io.nebula.core.common.util.JsonUtils;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HuggingFace Text Embeddings Inference（TEI）/rerank 端点的 wire 格式（R4 §3.1）
 * <p>
 * 请求体：{@code {"query": "...", "texts": ["...", ...], "truncate": true}}；
 * {@code model} 由端点侧决定，此处忽略。
 * <p>
 * 响应体：{@code [{"index": 0, "score": 0.9}, ...]}（顶层为数组）。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class TeiRerankWireCodec implements RerankWireCodec {

    @Override
    public String encode(String query, List<String> texts, String model) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", query);
        body.put("texts", texts);
        body.put("truncate", true);
        String json = JsonUtils.toJson(body);
        if (json == null) {
            throw new IllegalStateException("TEI 重排请求体序列化失败");
        }
        return json;
    }

    @Override
    public List<ScoredIndex> decode(String responseBody) {
        JsonNode root = JsonUtils.parseJson(responseBody);
        if (root == null || !root.isArray()) {
            throw new IllegalArgumentException("TEI 重排响应不是 JSON 数组");
        }
        List<ScoredIndex> result = new ArrayList<>(root.size());
        for (JsonNode item : root) {
            JsonNode indexNode = item.get("index");
            JsonNode scoreNode = item.get("score");
            if (indexNode == null || scoreNode == null || !indexNode.isNumber() || !scoreNode.isNumber()) {
                throw new IllegalArgumentException("TEI 重排响应缺少 index 或 score");
            }
            result.add(new ScoredIndex(indexNode.asInt(), scoreNode.asDouble()));
        }
        return result;
    }
}

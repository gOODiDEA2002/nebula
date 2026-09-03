package io.nebula.ai.rag.rerank.http;

import io.nebula.core.common.util.JsonUtils;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cohere /rerank 端点的 wire 格式（R4 §3.1）
 * <p>
 * 请求体：{@code {"model": "...", "query": "...", "documents": ["...", ...], "top_n": N}}；
 * {@code model} 必填，{@code top_n} 取本批候选数（要求端点返回全批，便于按批内下标回填）。
 * <p>
 * 响应体：{@code {"results": [{"index": 0, "relevance_score": 0.9}, ...]}}。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class CohereRerankWireCodec implements RerankWireCodec {

    @Override
    public String encode(String query, List<String> texts, String model) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("query", query);
        body.put("documents", texts);
        body.put("top_n", texts.size());
        String json = JsonUtils.toJson(body);
        if (json == null) {
            throw new IllegalStateException("Cohere 重排请求体序列化失败");
        }
        return json;
    }

    @Override
    public List<ScoredIndex> decode(String responseBody) {
        JsonNode root = JsonUtils.parseJson(responseBody);
        if (root == null) {
            throw new IllegalArgumentException("Cohere 重排响应不是合法 JSON");
        }
        JsonNode results = root.get("results");
        if (results == null || !results.isArray()) {
            throw new IllegalArgumentException("Cohere 重排响应缺少 results 数组");
        }
        List<ScoredIndex> result = new ArrayList<>(results.size());
        for (JsonNode item : results) {
            JsonNode indexNode = item.get("index");
            JsonNode scoreNode = item.get("relevance_score");
            if (indexNode == null || scoreNode == null || !indexNode.isNumber() || !scoreNode.isNumber()) {
                throw new IllegalArgumentException("Cohere 重排响应缺少 index 或 relevance_score");
            }
            result.add(new ScoredIndex(indexNode.asInt(), scoreNode.asDouble()));
        }
        return result;
    }
}

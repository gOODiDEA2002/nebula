package io.nebula.ai.rag.rerank.http;

import io.nebula.ai.rag.rerank.http.RerankWireCodec.ScoredIndex;
import io.nebula.core.common.util.JsonUtils;
import tools.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cohere wire 格式编解码契约（R4 §3.1、§10）
 */
class CohereRerankWireCodecTest {

    private final CohereRerankWireCodec codec = new CohereRerankWireCodec();

    @Test
    void encode_writesModelQueryDocumentsTopN() {
        String body = codec.encode("查询", List.of("文档A", "文档B", "文档C"), "rerank-v3");

        JsonNode node = JsonUtils.parseJson(body);
        assertThat(node).isNotNull();
        assertThat(node.get("model").asText()).isEqualTo("rerank-v3");
        assertThat(node.get("query").asText()).isEqualTo("查询");
        assertThat(node.get("documents").isArray()).isTrue();
        assertThat(node.get("documents").size()).isEqualTo(3);
        assertThat(node.get("top_n").asInt()).isEqualTo(3);
    }

    @Test
    void decode_parsesResultsWithRelevanceScore() {
        List<ScoredIndex> decoded = codec.decode(
                "{\"results\":[{\"index\":2,\"relevance_score\":0.7},{\"index\":0,\"relevance_score\":0.3}]}");

        assertThat(decoded).hasSize(2);
        assertThat(decoded.get(0).index()).isEqualTo(2);
        assertThat(decoded.get(0).score()).isEqualTo(0.7);
        assertThat(decoded.get(1).index()).isEqualTo(0);
        assertThat(decoded.get(1).score()).isEqualTo(0.3);
    }

    @Test
    void decode_missingResults_throws() {
        assertThatThrownBy(() -> codec.decode("{\"data\":[]}"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decode_invalidJson_throws() {
        assertThatThrownBy(() -> codec.decode("<html>"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decode_missingRelevanceScore_throws() {
        assertThatThrownBy(() -> codec.decode("{\"results\":[{\"index\":0}]}"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

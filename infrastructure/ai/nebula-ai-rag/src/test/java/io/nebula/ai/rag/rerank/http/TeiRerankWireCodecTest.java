package io.nebula.ai.rag.rerank.http;

import io.nebula.ai.rag.rerank.http.RerankWireCodec.ScoredIndex;
import io.nebula.core.common.util.JsonUtils;
import tools.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TEI wire 格式编解码契约（R4 §3.1、§10）
 */
class TeiRerankWireCodecTest {

    private final TeiRerankWireCodec codec = new TeiRerankWireCodec();

    @Test
    void encode_writesQueryTextsTruncate() {
        String body = codec.encode("查询", List.of("文档A", "文档B"), "ignored");

        JsonNode node = JsonUtils.parseJson(body);
        assertThat(node).isNotNull();
        assertThat(node.get("query").asText()).isEqualTo("查询");
        assertThat(node.get("truncate").asBoolean()).isTrue();
        assertThat(node.get("texts").isArray()).isTrue();
        assertThat(node.get("texts").size()).isEqualTo(2);
        assertThat(node.get("texts").get(0).asText()).isEqualTo("文档A");
    }

    @Test
    void decode_parsesIndexAndScoreArray() {
        List<ScoredIndex> decoded = codec.decode("[{\"index\":1,\"score\":0.9},{\"index\":0,\"score\":0.4}]");

        assertThat(decoded).hasSize(2);
        assertThat(decoded.get(0).index()).isEqualTo(1);
        assertThat(decoded.get(0).score()).isEqualTo(0.9);
        assertThat(decoded.get(1).index()).isEqualTo(0);
        assertThat(decoded.get(1).score()).isEqualTo(0.4);
    }

    @Test
    void decode_nonArray_throws() {
        assertThatThrownBy(() -> codec.decode("{\"results\":[]}"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decode_invalidJson_throws() {
        assertThatThrownBy(() -> codec.decode("not-json"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decode_missingScore_throws() {
        assertThatThrownBy(() -> codec.decode("[{\"index\":0}]"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package io.nebula.ai.rag.chunking.pack;

import io.nebula.ai.rag.chunking.DocumentChunk;
import io.nebula.ai.rag.chunking.parse.DocElement;
import io.nebula.ai.rag.chunking.parse.DocElementType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 块 ID 策略
 * <p>
 * 两条不变量：默认仍是随机 ID（守住既有调用方的行为），确定性 ID 必须真的确定 ——
 * 评测按 ID 前缀判中、增量索引按 ID 对齐，两者都靠「同一份文档切两次得到同一套 ID」。
 */
@DisplayName("ChunkIdStrategy")
class ChunkIdStrategyTest {

    private final List<DocElement> elements = List.of(
            new DocElement(DocElementType.PARAGRAPH, "第一段内容。", List.of("章节")),
            new DocElement(DocElementType.PARAGRAPH, "第二段内容。", List.of("另一章节")));

    @Test
    @DisplayName("默认策略是随机 ID, 与 DocumentChunk 构造器的现状语义一致")
    void defaultStrategy_isRandom() {
        List<DocumentChunk> first = new ChunkPacker().pack("doc", elements);
        List<DocumentChunk> second = new ChunkPacker().pack("doc", elements);

        assertThat(first.get(0).getId()).startsWith("chunk-");
        assertThat(first.get(0).getId()).isNotEqualTo(second.get(0).getId());
        assertThat(new PackOptions().getIdStrategy()).isNotNull();
    }

    @Test
    @DisplayName("确定性策略产出 docId#序号, 重复切分完全一致")
    void deterministicStrategy_isReproducible() {
        PackOptions options = new PackOptions();
        options.setIdStrategy(ChunkIdStrategy.deterministic());

        List<DocumentChunk> first = new ChunkPacker(options).pack("guide.md", elements);
        List<DocumentChunk> second = new ChunkPacker(options).pack("guide.md", elements);

        assertThat(first).extracting(DocumentChunk::getId)
                .containsExactly("guide.md#0", "guide.md#1");
        assertThat(second).extracting(DocumentChunk::getId)
                .containsExactly("guide.md#0", "guide.md#1");
    }

    @Test
    @DisplayName("策略拿得到已填好内容的块, 可据内容派生 ID")
    void strategy_receivesPopulatedChunk() {
        PackOptions options = new PackOptions();
        options.setIdStrategy((docId, index, chunk) -> {
            // 桩必须校验拿到的参数：拿不到内容的策略等于没法按内容派生 ID
            assertThat(docId).isEqualTo("guide.md");
            assertThat(chunk.getContent()).isNotBlank();
            assertThat(chunk.getMetadata()).containsKey(PackOptions.META_BREADCRUMB);
            return docId + "@" + index + "@" + chunk.getContent().length();
        });

        List<DocumentChunk> chunks = new ChunkPacker(options).pack("guide.md", elements);

        assertThat(chunks).extracting(DocumentChunk::getId)
                .containsExactly("guide.md@0@6", "guide.md@1@6");
    }

    @Test
    @DisplayName("随机策略每次都不同")
    void randomStrategy_yieldsDistinctIds() {
        ChunkIdStrategy random = ChunkIdStrategy.random();
        DocumentChunk chunk = new DocumentChunk();

        assertThat(random.chunkId("doc", 0, chunk))
                .isNotEqualTo(random.chunkId("doc", 0, chunk));
    }
}

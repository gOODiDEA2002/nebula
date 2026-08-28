package io.nebula.ai.rag.chunking;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 文档切片器迁移回归
 * <p>
 * 迁到 nebula-ai-rag 时把配置对象换成了构造参数，切分结果本身必须一字不变。
 */
class DocumentChunkerTest {

    private final MarkdownDocumentParser parser = new MarkdownDocumentParser();

    @Test
    void markdownSectionsCodeAndConfig_areChunkedSeparately() {
        String markdown = """
                # 标题
                正文内容。

                ```java
                public class Demo {}
                ```

                ```yaml
                nebula:
                  ai:
                    enabled: true
                ```
                """;

        ParsedDocument document = parser.parse(markdown, "demo-module");
        List<DocumentChunk> chunks = new DocumentChunker(3000, 200).chunk(document);

        assertThat(chunks).extracting(DocumentChunk::getChunkType)
                .contains(ChunkType.SECTION, ChunkType.CODE, ChunkType.CONFIG);
        assertThat(chunks).allSatisfy(chunk ->
                assertThat(chunk.getMetadata()).containsEntry("module", "demo-module"));
    }

    @Test
    void longSection_isSplitIntoNumberedParts() {
        String longBody = "内容。".repeat(200);
        ParsedDocument document = parser.parse("# 长章节\n" + longBody, "demo-module");

        List<DocumentChunk> chunks = new DocumentChunker(100, 20).chunk(document);

        assertThat(chunks.size()).isGreaterThan(1);
        assertThat(chunks.get(0).getTitle()).isEqualTo("长章节 (Part 1)");
        assertThat(chunks.get(1).getTitle()).isEqualTo("长章节 (Part 2)");
        assertThat(chunks.get(0).getMetadata()).containsEntry("part", 1);
    }

    @Test
    void emptySection_producesNoChunk() {
        ParsedDocument document = parser.parse("# 空章节\n", "demo-module");

        assertThat(new DocumentChunker(3000, 200).chunk(document)).isEmpty();
    }

    @Test
    void yamlBlock_isTreatedAsConfigNotCode() {
        String markdown = "# 标题\n\n```yaml\nkey: value\n```\n";

        ParsedDocument document = parser.parse(markdown, "demo-module");

        assertThat(document.getConfigExamples()).hasSize(1);
        assertThat(document.getCodeBlocks()).isEmpty();
    }

    @Test
    void invalidConstructorArguments_failFast() {
        assertThatThrownBy(() -> new DocumentChunker(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DocumentChunker(100, 100)).isInstanceOf(IllegalArgumentException.class);
    }
}

package io.nebula.ai.rag.chunking;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 文本分块器迁移回归
 * <p>
 * 分块行为直接决定灌进向量库的内容边界，改块大小或边界判定等于全量重灌，
 * 因此把不变量钉死：不丢内容、不超长、按句子边界切、重叠生效。
 */
class TextChunkerTest {

    private final TextChunker chunker = new TextChunker(500, 100);

    @Test
    void shortText_isReturnedAsSingleTrimmedChunk() {
        assertThat(chunker.chunk("  短文本  ")).containsExactly("短文本");
    }

    @Test
    void emptyOrNullText_yieldsNoChunks() {
        assertThat(chunker.chunk(null)).isEmpty();
        assertThat(chunker.chunk("")).isEmpty();
    }

    @Test
    void longText_isSplitAtSentenceBoundaries() {
        String text = "第一句话结束。" + "第二句话也结束。" + "第三句话同样结束。";

        List<String> chunks = chunker.chunk(text, 10, 2);

        assertThat(chunks).isNotEmpty();
        // 句子边界切分：除最后一块外都应落在句号之后
        assertThat(chunks.get(0)).endsWith("。");
        assertThat(String.join("", chunks)).contains("第一句话结束。");
    }

    @Test
    void chunkSize_isRespectedForBoundarylessText() {
        String text = "a".repeat(1000);

        List<String> chunks = chunker.chunk(text, 100, 20);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(100));
    }

    @Test
    void overlap_keepsAdjacentChunksSharingContent() {
        String text = "abcdefghij".repeat(10);

        List<String> chunks = chunker.chunk(text, 30, 10);

        assertThat(chunks.size()).isGreaterThan(1);
        String first = chunks.get(0);
        String second = chunks.get(1);
        // 重叠区必须真的重复出现，否则边界处的语义会被切断
        assertThat(second).startsWith(first.substring(first.length() - 10));
    }

    @Test
    void chunkByParagraph_keepsParagraphsTogetherWithinBudget() {
        String text = "第一段内容。\n\n第二段内容。\n\n第三段内容。";

        List<String> chunks = chunker.chunkByParagraph(text, 100);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).contains("第一段内容。").contains("第三段内容。");
    }

    @Test
    void chunkByParagraph_splitsWhenBudgetExceeded() {
        String paragraph = "内容".repeat(30);
        String text = paragraph + "\n\n" + paragraph;

        List<String> chunks = chunker.chunkByParagraph(text, 40);

        assertThat(chunks.size()).isGreaterThan(1);
    }

    @Test
    void invalidConstructorArguments_failFast() {
        assertThatThrownBy(() -> new TextChunker(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TextChunker(100, 100)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TextChunker(100, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}

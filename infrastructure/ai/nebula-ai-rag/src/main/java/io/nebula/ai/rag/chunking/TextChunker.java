package io.nebula.ai.rag.chunking;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本分块器
 * <p>
 * 将长文本分割为适合向量化的短文本块，
 * 支持固定长度分块（带重叠）和段落分块两种策略。
 * <p>
 * 与 {@link DocumentChunker} 的分工：本类处理纯文本，按句子边界切；
 * DocumentChunker 处理已解析出章节/代码块/配置块结构的文档。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class TextChunker {

    private final int defaultChunkSize;
    private final int defaultOverlap;

    /** 句子结束标记 */
    private static final char[] SENTENCE_ENDERS = {'。', '！', '？', '.', '!', '?', '\n'};

    /**
     * @param defaultChunkSize 默认块大小（字符数）
     * @param defaultOverlap   默认重叠大小（字符数）
     */
    public TextChunker(int defaultChunkSize, int defaultOverlap) {
        if (defaultChunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize 必须为正数");
        }
        if (defaultOverlap < 0 || defaultOverlap >= defaultChunkSize) {
            throw new IllegalArgumentException("overlap 必须在 [0, chunkSize) 区间内");
        }
        this.defaultChunkSize = defaultChunkSize;
        this.defaultOverlap = defaultOverlap;
    }

    /**
     * 使用默认参数进行固定长度分块
     */
    public List<String> chunk(String text) {
        return chunk(text, defaultChunkSize, defaultOverlap);
    }

    /**
     * 固定长度分块（带重叠，在句子边界处分割）
     *
     * @param text      原始文本
     * @param chunkSize 块大小（字符数）
     * @param overlap   重叠大小（字符数）
     * @return 文本块列表
     */
    public List<String> chunk(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        if (text.length() <= chunkSize) {
            chunks.add(text.trim());
            return chunks;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            // 尝试在句子边界处分割
            if (end < text.length()) {
                int boundary = findSentenceBoundary(text, end, start + overlap);
                if (boundary > start) {
                    end = boundary;
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            start = end - overlap;
            if (start >= text.length() - overlap) {
                break;
            }
        }
        return chunks;
    }

    /**
     * 按段落分块
     *
     * @param text         原始文本
     * @param maxChunkSize 单个块最大字符数
     * @return 文本块列表
     */
    public List<String> chunkByParagraph(String text, int maxChunkSize) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }

        String[] paragraphs = text.split("\n\n");
        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;

            if (currentChunk.length() + paragraph.length() > maxChunkSize) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk.setLength(0);
                }
                // 单个段落超过最大长度，进一步分割
                if (paragraph.length() > maxChunkSize) {
                    chunks.addAll(chunk(paragraph, maxChunkSize, maxChunkSize / 5));
                } else {
                    currentChunk.append(paragraph).append("\n\n");
                }
            } else {
                currentChunk.append(paragraph).append("\n\n");
            }
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        return chunks;
    }

    /**
     * 从 end 位置向前查找最近的句子边界
     */
    private int findSentenceBoundary(String text, int end, int minBoundary) {
        for (int i = end; i > minBoundary; i--) {
            char c = text.charAt(i);
            for (char ender : SENTENCE_ENDERS) {
                if (c == ender) {
                    return i + 1;
                }
            }
        }
        return end;
    }
}

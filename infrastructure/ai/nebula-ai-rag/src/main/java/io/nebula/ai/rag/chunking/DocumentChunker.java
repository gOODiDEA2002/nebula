package io.nebula.ai.rag.chunking;

import io.nebula.ai.rag.chunking.ParsedDocument.Section;
import io.nebula.ai.rag.chunking.ParsedDocument.CodeBlock;
import io.nebula.ai.rag.chunking.ParsedDocument.ConfigExample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档切片器
 *
 * 将解析后的文档切分为适合向量化的小块
 * <p>
 * 2.1.1 起切块参数改为直接的构造参数：原先绑在 {@code nebula-ai-spring} 的
 * {@code RagProperties.ChunkingConfig} 上，其中的 strategy 字段从未被任何代码读取
 * （只有一种混合策略），随该配置类一并删除。
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public class DocumentChunker {

    private static final Logger log = LoggerFactory.getLogger(DocumentChunker.class);

    private final int maxChunkSize;
    private final int overlapSize;

    /**
     * @param maxChunkSize 单块最大字符数
     * @param overlapSize  块间重叠字符数
     */
    public DocumentChunker(int maxChunkSize, int overlapSize) {
        if (maxChunkSize <= 0) {
            throw new IllegalArgumentException("maxChunkSize 必须为正数");
        }
        if (overlapSize < 0 || overlapSize >= maxChunkSize) {
            throw new IllegalArgumentException("overlapSize 必须在 [0, maxChunkSize) 区间内");
        }
        this.maxChunkSize = maxChunkSize;
        this.overlapSize = overlapSize;
    }

    /**
     * 切片文档（使用混合策略）
     *
     * @param document 解析后的文档
     * @return 文档块列表
     */
    public List<DocumentChunk> chunk(ParsedDocument document) {
        List<DocumentChunk> chunks = new ArrayList<>();

        log.debug("开始切片文档: {}", document.getModuleName());

        // 1. 章节文本块
        chunks.addAll(chunkSections(document));

        // 2. 独立的代码块
        chunks.addAll(chunkCodeBlocks(document));

        // 3. 独立的配置块
        chunks.addAll(chunkConfigBlocks(document));

        log.debug("文档切片完成，共 {} 个块", chunks.size());

        return chunks;
    }

    /**
     * 切分章节
     */
    private List<DocumentChunk> chunkSections(ParsedDocument document) {
        List<DocumentChunk> chunks = new ArrayList<>();

        for (Section section : document.getSections()) {
            // 如果章节太长，需要进一步切分
            if (section.getContent() != null && section.getContent().length() > maxChunkSize) {
                chunks.addAll(splitLongSection(section, document));
            } else {
                DocumentChunk chunk = buildSectionChunk(section, document);
                if (chunk != null) {
                    chunks.add(chunk);
                }
            }
        }

        return chunks;
    }

    /**
     * 构建章节块
     */
    private DocumentChunk buildSectionChunk(Section section, ParsedDocument document) {
        if (section.getContent() == null || section.getContent().trim().isEmpty()) {
            return null;
        }

        DocumentChunk chunk = new DocumentChunk();
        chunk.setModuleName(document.getModuleName());
        chunk.setChunkType(ChunkType.SECTION);
        chunk.setTitle(section.getTitle());
        chunk.setContent(section.getContent());

        // 添加元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("module", document.getModuleName());
        metadata.put("section", section.getTitle());
        metadata.put("level", section.getLevel());
        metadata.put("type", "documentation");
        metadata.put("doc_type", document.getDocType() != null ? document.getDocType() : "GENERAL");
        metadata.put("category", document.getCategory() != null ? document.getCategory() : "other");

        // 检查是否包含代码
        if (containsCode(section.getContent())) {
            metadata.put("has_code", true);
        }

        chunk.setMetadata(metadata);

        return chunk;
    }

    /**
     * 切分过长的章节
     */
    private List<DocumentChunk> splitLongSection(Section section, ParsedDocument document) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String content = section.getContent();

        int start = 0;
        int partNumber = 1;

        while (start < content.length()) {
            int end = Math.min(start + maxChunkSize, content.length());

            // 尝试在句子边界分割
            if (end < content.length()) {
                int lastPeriod = content.lastIndexOf('.', end);
                int lastNewline = content.lastIndexOf('\n', end);
                int splitPoint = Math.max(lastPeriod, lastNewline);

                if (splitPoint > start) {
                    end = splitPoint + 1;
                }
            }

            String chunkContent = content.substring(start, end).trim();

            if (!chunkContent.isEmpty()) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setModuleName(document.getModuleName());
                chunk.setChunkType(ChunkType.SECTION);
                chunk.setTitle(section.getTitle() + " (Part " + partNumber + ")");
                chunk.setContent(chunkContent);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("module", document.getModuleName());
                metadata.put("section", section.getTitle());
                metadata.put("part", partNumber);
                metadata.put("level", section.getLevel());
                metadata.put("type", "documentation");
                metadata.put("doc_type", document.getDocType() != null ? document.getDocType() : "GENERAL");
                metadata.put("category", document.getCategory() != null ? document.getCategory() : "other");

                chunk.setMetadata(metadata);
                chunks.add(chunk);

                partNumber++;
            }

            // 移动到下一个块，保留重叠
            // 确保 start 至少前进 1，防止无限循环
            int nextStart = end - overlapSize;
            if (nextStart <= start) {
                // 如果计算出的下一个起点没有前进，强制前进
                start = Math.min(start + maxChunkSize / 2, content.length());
            } else {
                start = Math.max(nextStart, 0);
            }
        }

        return chunks;
    }

    /**
     * 切分代码块
     */
    private List<DocumentChunk> chunkCodeBlocks(ParsedDocument document) {
        List<DocumentChunk> chunks = new ArrayList<>();

        for (CodeBlock codeBlock : document.getCodeBlocks()) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setModuleName(document.getModuleName());
            chunk.setChunkType(ChunkType.CODE);
            chunk.setTitle("Code Example: " + codeBlock.getLanguage());
            chunk.setContent(codeBlock.getCode());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("module", document.getModuleName());
            metadata.put("type", "code");
            metadata.put("language", codeBlock.getLanguage());
            metadata.put("doc_type", document.getDocType() != null ? document.getDocType() : "GENERAL");
            metadata.put("category", document.getCategory() != null ? document.getCategory() : "other");

            chunk.setMetadata(metadata);
            chunks.add(chunk);
        }

        return chunks;
    }

    /**
     * 切分配置块
     */
    private List<DocumentChunk> chunkConfigBlocks(ParsedDocument document) {
        List<DocumentChunk> chunks = new ArrayList<>();

        for (ConfigExample config : document.getConfigExamples()) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setModuleName(document.getModuleName());
            chunk.setChunkType(ChunkType.CONFIG);
            chunk.setTitle("Configuration Example");
            chunk.setContent(config.getContent());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("module", document.getModuleName());
            metadata.put("type", "configuration");
            metadata.put("format", config.getFormat());
            metadata.put("doc_type", document.getDocType() != null ? document.getDocType() : "GENERAL");
            metadata.put("category", document.getCategory() != null ? document.getCategory() : "other");

            chunk.setMetadata(metadata);
            chunks.add(chunk);
        }

        return chunks;
    }

    /**
     * 检查内容是否包含代码
     */
    private boolean containsCode(String content) {
        return content.contains("```") ||
                content.contains("@") || // Java annotations
                content.contains("public class") ||
                content.contains("public interface");
    }
}












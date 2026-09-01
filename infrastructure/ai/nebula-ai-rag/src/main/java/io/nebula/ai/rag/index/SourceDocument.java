package io.nebula.ai.rag.index;

import java.util.Map;

/**
 * 源文档：完整快照语义（J7）
 * <p>
 * 快照中不存在即视为已删除，无独立 tombstone。{@link #contentHash} 由 {@link DocumentSource}
 * 提供；未提供时索引管线用 {@link Sha256ContentHash} 现算。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class SourceDocument {

    /** 业务 docId，块 ID 前缀 */
    private String id;

    /** 文档原文 */
    private String content;

    /** 格式标识，对应 {@code StructureParser.format()} */
    private String format;

    /** 内容哈希，由 DocumentSource 提供；为空时管线现算 */
    private String contentHash;

    /** 附加元数据 */
    private Map<String, Object> metadata;

    public SourceDocument() {
    }

    public SourceDocument(String id, String content, String format) {
        this.id = id;
        this.content = content;
        this.format = format;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}

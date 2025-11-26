package io.nebula.ai.spring.rag.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文档块
 * 
 * 表示经过切分后的文档片段，用于向量化和检索
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public class DocumentChunk {
    /**
     * 唯一ID
     */
    private String id;
    
    /**
     * 模块名称
     */
    private String moduleName;
    
    /**
     * 块类型
     */
    private ChunkType chunkType;
    
    /**
     * 标题
     */
    private String title;
    
    /**
     * 内容
     */
    private String content;
    
    /**
     * 元数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 创建时间
     */
    private Instant createdAt;
    
    public DocumentChunk() {
        this.id = "chunk-" + UUID.randomUUID().toString();
        this.metadata = new HashMap<>();
        this.createdAt = Instant.now();
    }
    
    /**
     * 添加元数据
     */
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getModuleName() {
        return moduleName;
    }
    
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }
    
    public ChunkType getChunkType() {
        return chunkType;
    }
    
    public void setChunkType(ChunkType chunkType) {
        this.chunkType = chunkType;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}


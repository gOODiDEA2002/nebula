package io.nebula.ai.spring.rag.model;

/**
 * 文档块类型枚举
 * 
 * 用于标识 RAG 系统中不同类型的文档块
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public enum ChunkType {
    /**
     * 章节文本
     */
    SECTION("section", "章节文本"),
    
    /**
     * 代码示例
     */
    CODE("code", "代码示例"),
    
    /**
     * 配置示例
     */
    CONFIG("config", "配置示例"),
    
    /**
     * 表格
     */
    TABLE("table", "表格"),
    
    /**
     * 图表
     */
    DIAGRAM("diagram", "图表");
    
    private final String code;
    private final String description;
    
    ChunkType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static ChunkType fromCode(String code) {
        for (ChunkType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown chunk type: " + code);
    }
}












package io.nebula.data.persistence.readwrite;

/**
 * 数据源类型枚举
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public enum DataSourceType {
    
    /**
     * 读数据源（从库）
     */
    READ("read", "从库数据源，用于查询操作"),
    
    /**
     * 写数据源（主库）
     */
    WRITE("write", "主库数据源，用于写入操作");
    
    private final String code;
    private final String description;
    
    DataSourceType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * 获取代码
     */
    public String getCode() {
        return code;
    }
    
    /**
     * 获取描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据代码获取枚举
     */
    public static DataSourceType fromCode(String code) {
        if (code == null) {
            return null;
        }
        
        for (DataSourceType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        
        throw new IllegalArgumentException("Unknown data source type code: " + code);
    }
    
    /**
     * 检查是否为读类型
     */
    public boolean isRead() {
        return this == READ;
    }
    
    /**
     * 检查是否为写类型
     */
    public boolean isWrite() {
        return this == WRITE;
    }
    
    @Override
    public String toString() {
        return String.format("%s(%s)", name(), description);
    }
}

package io.nebula.example.rpc.async.api.dto;

/**
 * 数据处理类型枚举
 * 
 * @author Nebula Framework
 */
public enum ProcessType {
    
    /**
     * 数据导入
     */
    DATA_IMPORT("数据导入"),
    
    /**
     * 数据导出
     */
    DATA_EXPORT("数据导出"),
    
    /**
     * 数据转换
     */
    DATA_TRANSFORM("数据转换"),
    
    /**
     * 批量计算
     */
    BATCH_CALCULATION("批量计算"),
    
    /**
     * 数据清洗
     */
    DATA_CLEANING("数据清洗"),
    
    /**
     * 报表生成
     */
    REPORT_GENERATION("报表生成");
    
    private final String description;
    
    ProcessType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

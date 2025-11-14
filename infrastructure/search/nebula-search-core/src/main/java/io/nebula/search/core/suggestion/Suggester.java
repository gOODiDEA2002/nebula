package io.nebula.search.core.suggestion;

/**
 * 建议器接口基类
 * 所有建议类型都实现此接口
 * 
 * @author nebula
 */
public interface Suggester {
    
    /**
     * 获取建议器名称
     * 
     * @return 建议器名称
     */
    String getName();
    
    /**
     * 获取建议器类型
     * 
     * @return 建议器类型
     */
    SuggesterType getType();
    
    /**
     * 获取输入文本
     * 
     * @return 输入文本
     */
    String getText();
    
    /**
     * 获取目标字段
     * 
     * @return 字段名
     */
    String getField();
}


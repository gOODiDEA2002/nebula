package io.nebula.search.core.aggregation;

/**
 * 聚合接口基类
 * 所有聚合类型都实现此接口
 * 
 * @author nebula
 */
public interface Aggregation {
    
    /**
     * 获取聚合名称
     * 
     * @return 聚合名称
     */
    String getName();
    
    /**
     * 获取聚合类型
     * 
     * @return 聚合类型
     */
    AggregationType getType();
    
    /**
     * 获取目标字段
     * 
     * @return 字段名
     */
    String getField();
}


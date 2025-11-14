package io.nebula.search.core.aggregation;

import java.util.ArrayList;
import java.util.List;

/**
 * 聚合抽象基类
 * 提供通用的聚合属性和方法
 * 
 * @author nebula
 */
public abstract class AbstractAggregation implements Aggregation {
    
    /**
     * 聚合名称
     */
    protected String name;
    
    /**
     * 目标字段
     */
    protected String field;
    
    /**
     * 子聚合列表
     */
    protected List<Aggregation> subAggregations;
    
    protected AbstractAggregation(String name, String field) {
        this.name = name;
        this.field = field;
        this.subAggregations = new ArrayList<>();
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getField() {
        return field;
    }
    
    /**
     * 添加子聚合
     * 
     * @param aggregation 子聚合
     * @return 当前聚合对象（支持链式调用）
     */
    public AbstractAggregation addSubAggregation(Aggregation aggregation) {
        this.subAggregations.add(aggregation);
        return this;
    }
    
    /**
     * 获取所有子聚合
     * 
     * @return 子聚合列表
     */
    public List<Aggregation> getSubAggregations() {
        return subAggregations;
    }
    
    /**
     * 是否有子聚合
     * 
     * @return 是否有子聚合
     */
    public boolean hasSubAggregations() {
        return subAggregations != null && !subAggregations.isEmpty();
    }
}


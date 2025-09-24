package io.nebula.data.access.query.impl;

import io.nebula.data.access.query.QueryBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Supplier;

/**
 * QueryBuilder的默认实现
 * 使用内存查询构建器作为通用实现
 * 
 * @param <T> 实体类型
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class DefaultQueryBuilder<T> implements QueryBuilder<T> {
    
    private final List<QueryCondition> conditions = new ArrayList<>();
    private final List<SortCondition> sortConditions = new ArrayList<>();
    private final Supplier<List<T>> dataSupplier;
    
    private Integer limitCount;
    private Integer offsetCount;
    
    public DefaultQueryBuilder(Supplier<List<T>> dataSupplier) {
        this.dataSupplier = dataSupplier;
    }
    
    @Override
    public QueryBuilder<T> eq(String field, Object value) {
        conditions.add(new QueryCondition(field, Operator.EQ, value, LogicalOperator.AND));
        return this;
    }
    
    @Override
    public QueryBuilder<T> ne(String field, Object value) {
        conditions.add(new QueryCondition(field, Operator.NE, value, LogicalOperator.AND));
        return this;
    }
    
    @Override
    public QueryBuilder<T> gt(String field, Object value) {
        conditions.add(new QueryCondition(field, Operator.GT, value, LogicalOperator.AND));
        return this;
    }
    
    @Override
    public QueryBuilder<T> gte(String field, Object value) {
        conditions.add(new QueryCondition(field, Operator.GTE, value, LogicalOperator.AND));
        return this;
    }
    
    @Override
    public QueryBuilder<T> lt(String field, Object value) {
        conditions.add(new QueryCondition(field, Operator.LT, value, LogicalOperator.AND));
        return this;
    }
    
    @Override
    public QueryBuilder<T> lte(String field, Object value) {
        conditions.add(new QueryCondition(field, Operator.LTE, value, LogicalOperator.AND));
        return this;
    }
    
    @Override
    public QueryBuilder<T> like(String field, Object value) {
        conditions.add(new QueryCondition(field, Operator.LIKE, value, LogicalOperator.AND));
        return this;
    }
    
    @Override
    public QueryBuilder<T> likeLeft(String field, Object value) {
        conditions.add(new QueryCondition(field, Operator.LIKE_LEFT, value, LogicalOperator.AND));
        return this;
    }
    
    @Override
    public QueryBuilder<T> likeRight(String field, Object value) {
        conditions.add(new QueryCondition(field, Operator.LIKE_RIGHT, value, LogicalOperator.AND));
        return this;
    }
    
    @Override
    public QueryBuilder<T> in(String field, Object... values) {
        conditions.add(new QueryCondition(field, Operator.IN, Arrays.asList(values), LogicalOperator.AND));
        return this;
    }
    
    @Override
    public QueryBuilder<T> in(String field, Iterable<?> values) {
        List<Object> valueList = new ArrayList<>();
        values.forEach(valueList::add);
        conditions.add(new QueryCondition(field, Operator.IN, valueList, LogicalOperator.AND));
        return this;
    }
    
    @Override
    public QueryBuilder<T> notIn(String field, Object... values) {
        conditions.add(new QueryCondition(field, Operator.NOT_IN, Arrays.asList(values), LogicalOperator.AND));
        return this;
    }
    
    @Override
    public QueryBuilder<T> notIn(String field, Iterable<?> values) {
        List<Object> valueList = new ArrayList<>();
        values.forEach(valueList::add);
        conditions.add(new QueryCondition(field, Operator.NOT_IN, valueList, LogicalOperator.AND));
        return this;
    }
    
    @Override
    public QueryBuilder<T> between(String field, Object start, Object end) {
        conditions.add(new QueryCondition(field, Operator.BETWEEN, Arrays.asList(start, end), LogicalOperator.AND));
        return this;
    }
    
    @Override
    public QueryBuilder<T> notBetween(String field, Object start, Object end) {
        conditions.add(new QueryCondition(field, Operator.NOT_BETWEEN, Arrays.asList(start, end), LogicalOperator.AND));
        return this;
    }
    
    @Override
    public QueryBuilder<T> isNull(String field) {
        conditions.add(new QueryCondition(field, Operator.IS_NULL, null, LogicalOperator.AND));
        return this;
    }
    
    @Override
    public QueryBuilder<T> isNotNull(String field) {
        conditions.add(new QueryCondition(field, Operator.IS_NOT_NULL, null, LogicalOperator.AND));
        return this;
    }
    
    @Override
    public QueryBuilder<T> and() {
        // 标记下一个条件使用AND连接
        return this;
    }
    
    @Override
    public QueryBuilder<T> or() {
        // 标记下一个条件使用OR连接
        if (!conditions.isEmpty()) {
            conditions.get(conditions.size() - 1).setLogicalOperator(LogicalOperator.OR);
        }
        return this;
    }
    
    @Override
    public QueryBuilder<T> groupStart() {
        conditions.add(new QueryCondition("(", Operator.GROUP_START, null, LogicalOperator.AND));
        return this;
    }
    
    @Override
    public QueryBuilder<T> groupEnd() {
        conditions.add(new QueryCondition(")", Operator.GROUP_END, null, LogicalOperator.AND));
        return this;
    }
    
    @Override
    public QueryBuilder<T> orderByAsc(String field) {
        sortConditions.add(new SortCondition(field, Sort.Direction.ASC));
        return this;
    }
    
    @Override
    public QueryBuilder<T> orderByDesc(String field) {
        sortConditions.add(new SortCondition(field, Sort.Direction.DESC));
        return this;
    }
    
    @Override
    public QueryBuilder<T> orderBy(Sort sort) {
        if (sort != null) {
            sort.forEach(order -> 
                sortConditions.add(new SortCondition(order.getProperty(), order.getDirection()))
            );
        }
        return this;
    }
    
    @Override
    public QueryBuilder<T> limit(int limit) {
        this.limitCount = limit;
        return this;
    }
    
    @Override
    public QueryBuilder<T> offset(int offset) {
        this.offsetCount = offset;
        return this;
    }
    
    @Override
    public Page<T> page(Pageable pageable) {
        List<T> filteredData = executeQuery();
        
        // 应用分页
        int start = (int) (pageable.getPageNumber() * pageable.getPageSize());
        int end = Math.min(start + pageable.getPageSize(), filteredData.size());
        
        List<T> pageData = filteredData.subList(start, end);
        
        return new PageImpl<>(pageData, pageable, filteredData.size());
    }
    
    @Override
    public List<T> list() {
        return executeQuery();
    }
    
    @Override
    public Optional<T> first() {
        List<T> results = limit(1).list();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    @Override
    public T one() {
        List<T> results = limit(2).list();
        if (results.isEmpty()) {
            throw new RuntimeException("No result found");
        }
        if (results.size() > 1) {
            throw new RuntimeException("More than one result found");
        }
        return results.get(0);
    }
    
    @Override
    public Optional<T> oneOrEmpty() {
        List<T> results = limit(2).list();
        if (results.isEmpty()) {
            return Optional.empty();
        }
        if (results.size() > 1) {
            throw new RuntimeException("More than one result found");
        }
        return Optional.of(results.get(0));
    }
    
    @Override
    public long count() {
        return executeQuery().size();
    }
    
    @Override
    public boolean exists() {
        return !executeQuery().isEmpty();
    }
    
    @Override
    public QueryBuilder<T> clear() {
        conditions.clear();
        sortConditions.clear();
        limitCount = null;
        offsetCount = null;
        return this;
    }
    
    @Override
    public QueryBuilder<T> clone() {
        DefaultQueryBuilder<T> cloned = new DefaultQueryBuilder<>(dataSupplier);
        cloned.conditions.addAll(this.conditions);
        cloned.sortConditions.addAll(this.sortConditions);
        cloned.limitCount = this.limitCount;
        cloned.offsetCount = this.offsetCount;
        return cloned;
    }
    
    /**
     * 执行查询
     * 注意：这是一个简化的内存实现，仅用于演示
     * 实际使用中应该根据具体的数据访问技术实现
     */
    private List<T> executeQuery() {
        try {
            List<T> data = dataSupplier.get();
            if (data == null) {
                return new ArrayList<>();
            }
            
            // 在实际实现中，这里应该将条件转换为具体的查询语句
            // 这里只是一个简化的演示实现
            log.debug("Executing query with {} conditions, {} sort conditions", 
                     conditions.size(), sortConditions.size());
            
            // 应用偏移量和限制
            int start = offsetCount != null ? offsetCount : 0;
            int end = limitCount != null ? Math.min(start + limitCount, data.size()) : data.size();
            
            if (start >= data.size()) {
                return new ArrayList<>();
            }
            
            return data.subList(start, end);
            
        } catch (Exception e) {
            log.error("Error executing query", e);
            throw new RuntimeException("Query execution failed", e);
        }
    }
    
    /**
     * 查询条件
     */
    private static class QueryCondition {
        private final String field;
        private final Operator operator;
        private final Object value;
        private LogicalOperator logicalOperator;
        
        public QueryCondition(String field, Operator operator, Object value, LogicalOperator logicalOperator) {
            this.field = field;
            this.operator = operator;
            this.value = value;
            this.logicalOperator = logicalOperator;
        }
        
        public void setLogicalOperator(LogicalOperator logicalOperator) {
            this.logicalOperator = logicalOperator;
        }
        
        // Getters
        public String getField() { return field; }
        public Operator getOperator() { return operator; }
        public Object getValue() { return value; }
        public LogicalOperator getLogicalOperator() { return logicalOperator; }
    }
    
    /**
     * 排序条件
     */
    private static class SortCondition {
        private final String field;
        private final Sort.Direction direction;
        
        public SortCondition(String field, Sort.Direction direction) {
            this.field = field;
            this.direction = direction;
        }
        
        // Getters
        public String getField() { return field; }
        public Sort.Direction getDirection() { return direction; }
    }
    
    /**
     * 操作符枚举
     */
    private enum Operator {
        EQ, NE, GT, GTE, LT, LTE,
        LIKE, LIKE_LEFT, LIKE_RIGHT,
        IN, NOT_IN,
        BETWEEN, NOT_BETWEEN,
        IS_NULL, IS_NOT_NULL,
        GROUP_START, GROUP_END
    }
    
    /**
     * 逻辑操作符枚举
     */
    private enum LogicalOperator {
        AND, OR
    }
}

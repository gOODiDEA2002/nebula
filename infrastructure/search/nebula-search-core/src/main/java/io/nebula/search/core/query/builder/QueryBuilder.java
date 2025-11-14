package io.nebula.search.core.query.builder;

/**
 * 查询构建器接口
 * 所有查询类型都实现此接口
 * 
 * @author nebula
 */
public interface QueryBuilder {
    
    /**
     * 获取查询类型
     * 
     * @return 查询类型
     */
    QueryType getQueryType();
}


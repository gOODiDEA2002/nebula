package io.nebula.search.core.query.builder;

/**
 * 查询类型枚举
 * 
 * @author nebula
 */
public enum QueryType {
    
    /**
     * 匹配所有文档
     */
    MATCH_ALL,
    
    /**
     * 全文匹配查询
     */
    MATCH,
    
    /**
     * 多字段匹配查询
     */
    MULTI_MATCH,
    
    /**
     * 精确匹配查询
     */
    TERM,
    
    /**
     * 多词项匹配查询
     */
    TERMS,
    
    /**
     * 范围查询
     */
    RANGE,
    
    /**
     * 前缀查询
     */
    PREFIX,
    
    /**
     * 通配符查询
     */
    WILDCARD,
    
    /**
     * 正则表达式查询
     */
    REGEXP,
    
    /**
     * 模糊查询
     */
    FUZZY,
    
    /**
     * 布尔组合查询
     */
    BOOL,
    
    /**
     * 存在性查询
     */
    EXISTS,
    
    /**
     * ID查询
     */
    IDS,
    
    /**
     * 嵌套查询
     */
    NESTED
}


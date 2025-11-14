package io.nebula.search.core.query.builder;

import java.util.Collections;
import java.util.Map;

/**
 * 匹配所有文档查询构建器
 * 
 * 使用场景：
 * - 获取索引中的所有文档
 * - 作为基础查询，配合过滤器使用
 * - 测试和调试
 * 
 * @author nebula
 */
public class MatchAllQueryBuilder implements QueryBuilder {
    
    @Override
    public QueryType getQueryType() {
        return QueryType.MATCH_ALL;
    }
}


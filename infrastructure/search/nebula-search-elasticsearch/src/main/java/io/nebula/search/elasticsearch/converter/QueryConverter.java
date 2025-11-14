package io.nebula.search.elasticsearch.converter;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import io.nebula.search.core.query.builder.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 查询转换器
 * 将 nebula 的 QueryBuilder 转换为 Elasticsearch Query
 * 
 * @author nebula
 */
public class QueryConverter {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryConverter.class);
    
    /**
     * 转换查询
     * 
     * @param nebulaQueryBuilder nebula 查询构建器
     * @return Elasticsearch Query
     */
    public static Query convert(io.nebula.search.core.query.builder.QueryBuilder nebulaQueryBuilder) {
        if (nebulaQueryBuilder == null) {
            return MatchAllQuery.of(m -> m)._toQuery();
        }

        switch (nebulaQueryBuilder.getQueryType()) {
            case MATCH:
                return convertMatchQuery((MatchQueryBuilder) nebulaQueryBuilder);
            case TERM:
                return convertTermQuery((TermQueryBuilder) nebulaQueryBuilder);
            case RANGE:
                return convertRangeQuery((RangeQueryBuilder) nebulaQueryBuilder);
            case BOOL:
                return convertBoolQuery((BoolQueryBuilder) nebulaQueryBuilder);
            case MATCH_ALL:
                return MatchAllQuery.of(m -> m)._toQuery();
            default:
                logger.warn("Unsupported query type: {}, using match_all", nebulaQueryBuilder.getQueryType());
                return MatchAllQuery.of(m -> m)._toQuery();
        }
    }
    
    /**
     * 转换 Match 查询
     */
    private static Query convertMatchQuery(MatchQueryBuilder matchBuilder) {
        return MatchQuery.of(m -> {
            m.field(matchBuilder.getField())
             .query(String.valueOf(matchBuilder.getQuery()));
            
            if (matchBuilder.getOperator() != null) {
                String operator = matchBuilder.getOperator();
                m.operator(Operator.valueOf(operator.substring(0, 1).toUpperCase() + operator.substring(1).toLowerCase()));
            }
            
            if (matchBuilder.getMinimumShouldMatch() != null) {
                m.minimumShouldMatch(matchBuilder.getMinimumShouldMatch());
            }
            
            if (matchBuilder.getFuzziness() != null) {
                m.fuzziness(matchBuilder.getFuzziness());
            }
            
            if (matchBuilder.getPrefixLength() != null) {
                m.prefixLength(matchBuilder.getPrefixLength());
            }
            
            if (matchBuilder.getMaxExpansions() != null) {
                m.maxExpansions(matchBuilder.getMaxExpansions());
            }
            
            return m;
        })._toQuery();
    }
    
    /**
     * 转换 Term 查询
     */
    private static Query convertTermQuery(TermQueryBuilder termBuilder) {
        return TermQuery.of(t -> {
            t.field(termBuilder.getField())
             .value(FieldValue.of(String.valueOf(termBuilder.getValue())));
            
            if (termBuilder.getBoost() != null) {
                t.boost(termBuilder.getBoost());
            }
            
            return t;
        })._toQuery();
    }
    
    /**
     * 转换 Range 查询
     */
    private static Query convertRangeQuery(RangeQueryBuilder rangeBuilder) {
        return RangeQuery.of(r -> {
            r.field(rangeBuilder.getField());
            
            if (rangeBuilder.getGt() != null) {
                r.gt(co.elastic.clients.json.JsonData.of(rangeBuilder.getGt()));
            }
            if (rangeBuilder.getGte() != null) {
                r.gte(co.elastic.clients.json.JsonData.of(rangeBuilder.getGte()));
            }
            if (rangeBuilder.getLt() != null) {
                r.lt(co.elastic.clients.json.JsonData.of(rangeBuilder.getLt()));
            }
            if (rangeBuilder.getLte() != null) {
                r.lte(co.elastic.clients.json.JsonData.of(rangeBuilder.getLte()));
            }
            if (rangeBuilder.getFormat() != null) {
                r.format(rangeBuilder.getFormat());
            }
            if (rangeBuilder.getTimeZone() != null) {
                r.timeZone(rangeBuilder.getTimeZone());
            }
            if (rangeBuilder.getBoost() != null) {
                r.boost(rangeBuilder.getBoost());
            }
            
            return r;
        })._toQuery();
    }
    
    /**
     * 转换 Bool 查询
     */
    private static Query convertBoolQuery(BoolQueryBuilder boolBuilder) {
        return BoolQuery.of(b -> {
            // 处理 must 子句
            for (io.nebula.search.core.query.builder.QueryBuilder mustQuery : boolBuilder.getMust()) {
                b.must(convert(mustQuery));
            }
            
            // 处理 should 子句
            for (io.nebula.search.core.query.builder.QueryBuilder shouldQuery : boolBuilder.getShould()) {
                b.should(convert(shouldQuery));
            }
            
            // 处理 mustNot 子句
            for (io.nebula.search.core.query.builder.QueryBuilder mustNotQuery : boolBuilder.getMustNot()) {
                b.mustNot(convert(mustNotQuery));
            }
            
            // 处理 filter 子句
            for (io.nebula.search.core.query.builder.QueryBuilder filterQuery : boolBuilder.getFilter()) {
                b.filter(convert(filterQuery));
            }
            
            // 处理 minimumShouldMatch
            if (boolBuilder.getMinimumShouldMatch() != null) {
                b.minimumShouldMatch(boolBuilder.getMinimumShouldMatch());
            }
            
            // 处理 boost
            if (boolBuilder.getBoost() != null) {
                b.boost(boolBuilder.getBoost());
            }
            
            return b;
        })._toQuery();
    }
}

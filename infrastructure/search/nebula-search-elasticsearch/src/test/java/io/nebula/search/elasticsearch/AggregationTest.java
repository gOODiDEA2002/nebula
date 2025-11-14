package io.nebula.search.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import io.nebula.search.core.model.AggregationResult;
import io.nebula.search.core.query.AggregationQuery;
import io.nebula.search.core.query.builder.RangeQueryBuilder;
import io.nebula.search.elasticsearch.config.ElasticsearchProperties;
import io.nebula.search.elasticsearch.service.ElasticsearchSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Elasticsearch 聚合查询测试（强类型 API）
 * 
 * 使用新的强类型 Aggregation API 进行测试
 */
@ExtendWith(MockitoExtension.class)
class AggregationTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private ElasticsearchProperties properties;

    private ElasticsearchSearchService searchService;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getIndexPrefix()).thenReturn("nebula-");
        lenient().when(properties.getBulkTimeout()).thenReturn(Duration.ofSeconds(30));
        lenient().when(properties.getDefaultShards()).thenReturn(1);
        lenient().when(properties.getDefaultReplicas()).thenReturn(1);
        
        searchService = new ElasticsearchSearchService(
            elasticsearchClient,
            elasticsearchOperations,
            properties
        );
    }

    @Test
    void testTermsAggregation() throws Exception {
        // 使用强类型 API 创建 Terms 聚合
        io.nebula.search.core.aggregation.Aggregation termsAgg = 
            new io.nebula.search.core.aggregation.TermsAggregation("category-stats", "category")
                .size(10);
        
        AggregationQuery query = AggregationQuery.builder()
            .index("test-index")
            .addAggregation(termsAgg)
            .build();
        
        // Mock Elasticsearch 响应
        SearchResponse<Void> mockResponse = mock(SearchResponse.class);
        StringTermsAggregate mockTermsAgg = mock(StringTermsAggregate.class);
        StringTermsBucket mockBucket = mock(StringTermsBucket.class);
        
        lenient().when(mockBucket.key()).thenReturn(FieldValue.of("electronics"));
        lenient().when(mockBucket.docCount()).thenReturn(100L);
        lenient().when(mockTermsAgg.buckets()).thenReturn(Buckets.of(b -> b.array(List.of(mockBucket))));
        
        Aggregate mockAggregate = mock(Aggregate.class);
        lenient().when(mockAggregate.isSterms()).thenReturn(true);
        lenient().when(mockAggregate.sterms()).thenReturn(mockTermsAgg);
        
        lenient().when(mockResponse.aggregations()).thenReturn(Map.of("category-stats", mockAggregate));
        
        when(elasticsearchClient.search(any(Function.class), any(Class.class)))
            .thenReturn(mockResponse);
        
        // 执行聚合查询
        AggregationResult result = searchService.aggregate(query);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAggregations()).containsKey("category-stats");
    }

    @Test
    void testAvgAggregation() throws Exception {
        // 使用强类型 API 创建平均值聚合
        io.nebula.search.core.aggregation.Aggregation avgAgg = 
            io.nebula.search.core.aggregation.MetricAggregation.avg("avg-price", "price");
        
        AggregationQuery query = AggregationQuery.builder()
            .index("test-index")
            .addAggregation(avgAgg)
            .build();
        
        // Mock Elasticsearch 响应
        SearchResponse<Void> mockResponse = mock(SearchResponse.class);
        AvgAggregate mockAvgAgg = mock(AvgAggregate.class);
        when(mockAvgAgg.value()).thenReturn(299.99);
        
        Aggregate mockAggregate = mock(Aggregate.class);
        when(mockAggregate.isAvg()).thenReturn(true);
        when(mockAggregate.avg()).thenReturn(mockAvgAgg);
        
        when(mockResponse.aggregations()).thenReturn(Map.of("avg-price", mockAggregate));
        
        when(elasticsearchClient.search(any(Function.class), any(Class.class)))
            .thenReturn(mockResponse);
        
        // 执行聚合查询
        AggregationResult result = searchService.aggregate(query);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAggregations()).containsKey("avg-price");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> avgResult = (Map<String, Object>) result.getAggregations().get("avg-price");
        assertThat(avgResult).containsKey("value");
        assertThat(avgResult.get("value")).isEqualTo(299.99);
    }

    @Test
    void testSumAggregation() throws Exception {
        // 使用强类型 API 创建求和聚合
        io.nebula.search.core.aggregation.Aggregation sumAgg = 
            io.nebula.search.core.aggregation.MetricAggregation.sum("total-sales", "sales");
        
        AggregationQuery query = AggregationQuery.builder()
            .index("test-index")
            .addAggregation(sumAgg)
            .build();
        
        // Mock Elasticsearch 响应
        SearchResponse<Void> mockResponse = mock(SearchResponse.class);
        SumAggregate mockSumAgg = mock(SumAggregate.class);
        when(mockSumAgg.value()).thenReturn(15000.0);
        
        Aggregate mockAggregate = mock(Aggregate.class);
        when(mockAggregate.isSum()).thenReturn(true);
        when(mockAggregate.sum()).thenReturn(mockSumAgg);
        
        when(mockResponse.aggregations()).thenReturn(Map.of("total-sales", mockAggregate));
        
        when(elasticsearchClient.search(any(Function.class), any(Class.class)))
            .thenReturn(mockResponse);
        
        // 执行聚合查询
        AggregationResult result = searchService.aggregate(query);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAggregations()).containsKey("total-sales");
    }

    @Test
    void testDateHistogramAggregation() throws Exception {
        // 使用强类型 API 创建日期直方图聚合
        io.nebula.search.core.aggregation.Aggregation dateHistoAgg = 
            new io.nebula.search.core.aggregation.DateHistogramAggregation("daily-stats", "timestamp")
                .calendarInterval("1d")
                .format("yyyy-MM-dd");
        
        AggregationQuery query = AggregationQuery.builder()
            .index("test-index")
            .addAggregation(dateHistoAgg)
            .build();
        
        // Mock Elasticsearch 响应
        SearchResponse<Void> mockResponse = mock(SearchResponse.class);
        DateHistogramAggregate mockDateHistoAgg = mock(DateHistogramAggregate.class);
        DateHistogramBucket mockBucket = mock(DateHistogramBucket.class);
        
        when(mockBucket.keyAsString()).thenReturn("2024-01-01");
        when(mockBucket.docCount()).thenReturn(50L);
        when(mockBucket.aggregations()).thenReturn(Map.of());
        when(mockDateHistoAgg.buckets()).thenReturn(Buckets.of(b -> b.array(List.of(mockBucket))));
        
        Aggregate mockAggregate = mock(Aggregate.class);
        when(mockAggregate.isDateHistogram()).thenReturn(true);
        when(mockAggregate.dateHistogram()).thenReturn(mockDateHistoAgg);
        
        when(mockResponse.aggregations()).thenReturn(Map.of("daily-stats", mockAggregate));
        
        when(elasticsearchClient.search(any(Function.class), any(Class.class)))
            .thenReturn(mockResponse);
        
        // 执行聚合查询
        AggregationResult result = searchService.aggregate(query);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAggregations()).containsKey("daily-stats");
    }

    @Test
    void testNestedAggregation() throws Exception {
        // 使用强类型 API 创建嵌套聚合（分类统计，每个分类内计算平均价格）
        io.nebula.search.core.aggregation.TermsAggregation categoryAgg = 
            new io.nebula.search.core.aggregation.TermsAggregation("categories", "category")
                .size(10);
        categoryAgg.addSubAggregation(
            io.nebula.search.core.aggregation.MetricAggregation.avg("avg-price", "price")
        );
        
        AggregationQuery query = AggregationQuery.builder()
            .index("test-index")
            .addAggregation(categoryAgg)
            .build();
        
        // Mock Elasticsearch 响应（带子聚合）
        SearchResponse<Void> mockResponse = mock(SearchResponse.class);
        
        // Mock 子聚合
        AvgAggregate mockAvgSubAgg = mock(AvgAggregate.class);
        when(mockAvgSubAgg.value()).thenReturn(199.99);
        Aggregate mockSubAggregate = mock(Aggregate.class);
        when(mockSubAggregate.isAvg()).thenReturn(true);
        when(mockSubAggregate.avg()).thenReturn(mockAvgSubAgg);
        
        // Mock 主聚合桶
        StringTermsBucket mockBucket = mock(StringTermsBucket.class);
        lenient().when(mockBucket.key()).thenReturn(FieldValue.of("books"));
        lenient().when(mockBucket.docCount()).thenReturn(100L);
        lenient().when(mockBucket.aggregations()).thenReturn(Map.of("avg-price", mockSubAggregate));
        
        StringTermsAggregate mockTermsAgg = mock(StringTermsAggregate.class);
        when(mockTermsAgg.buckets()).thenReturn(Buckets.of(b -> b.array(List.of(mockBucket))));
        
        Aggregate mockAggregate = mock(Aggregate.class);
        when(mockAggregate.isSterms()).thenReturn(true);
        when(mockAggregate.sterms()).thenReturn(mockTermsAgg);
        
        when(mockResponse.aggregations()).thenReturn(Map.of("categories", mockAggregate));
        
        when(elasticsearchClient.search(any(Function.class), any(Class.class)))
            .thenReturn(mockResponse);
        
        // 执行聚合查询
        AggregationResult result = searchService.aggregate(query);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAggregations()).containsKey("categories");
    }

    @Test
    void testAggregationWithFilter() throws Exception {
        // 使用强类型 API 创建带过滤条件的聚合（只统计价格 > 100 的商品）
        io.nebula.search.core.aggregation.Aggregation avgAgg = 
            io.nebula.search.core.aggregation.MetricAggregation.avg("avg-price", "price");
        
        AggregationQuery query = AggregationQuery.builder()
            .index("test-index")
            .query(new RangeQueryBuilder("price").gte(100))  // 过滤条件
            .addAggregation(avgAgg)
            .build();
        
        // Mock Elasticsearch 响应
        SearchResponse<Void> mockResponse = mock(SearchResponse.class);
        AvgAggregate mockAvgAgg = mock(AvgAggregate.class);
        when(mockAvgAgg.value()).thenReturn(350.00);
        
        Aggregate mockAggregate = mock(Aggregate.class);
        when(mockAggregate.isAvg()).thenReturn(true);
        when(mockAggregate.avg()).thenReturn(mockAvgAgg);
        
        when(mockResponse.aggregations()).thenReturn(Map.of("avg-price", mockAggregate));
        
        when(elasticsearchClient.search(any(Function.class), any(Class.class)))
            .thenReturn(mockResponse);
        
        // 执行聚合查询
        AggregationResult result = searchService.aggregate(query);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAggregations()).containsKey("avg-price");
    }
}

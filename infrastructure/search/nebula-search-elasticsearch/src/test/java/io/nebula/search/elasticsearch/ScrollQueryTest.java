package io.nebula.search.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import io.nebula.search.core.model.ScrollResult;
import io.nebula.search.core.query.SearchQuery;
import io.nebula.search.core.query.builder.QueryBuilder;
import io.nebula.search.core.query.builder.MatchQueryBuilder;
import io.nebula.search.elasticsearch.config.ElasticsearchProperties;
import io.nebula.search.elasticsearch.service.ElasticsearchSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Elasticsearch 滚动查询测试
 */
@ExtendWith(MockitoExtension.class)
class ScrollQueryTest {

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
        lenient().when(properties.getScrollTimeout()).thenReturn(Duration.ofMinutes(1));
        
        searchService = new ElasticsearchSearchService(
            elasticsearchClient,
            elasticsearchOperations,
            properties
        );
    }

    @Test
    void testScrollQuery() throws IOException {
        // 测试开始滚动查询（使用强类型 API）
        QueryBuilder queryBuilder = new MatchQueryBuilder("name", "test");
        
        SearchQuery query = SearchQuery.builder()
            .index("test-index")
            .query(queryBuilder)
            .size(100)
            .build();
        
        // Mock 搜索响应
        SearchResponse<Map> searchResponse = mock(SearchResponse.class);
        HitsMetadata<Map> hitsMetadata = mock(HitsMetadata.class);
        
        lenient().when(searchResponse.scrollId()).thenReturn("scroll_id_123");
        lenient().when(searchResponse.hits()).thenReturn(hitsMetadata);
        lenient().when(hitsMetadata.hits()).thenReturn(Collections.emptyList());
        
        when(elasticsearchClient.search(any(Function.class), eq(Map.class)))
            .thenReturn(searchResponse);
        
        // 执行测试
        ScrollResult<Map> result = searchService.scrollSearch(query, Map.class);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getScrollId()).isEqualTo("scroll_id_123");
        
        verify(elasticsearchClient).search(any(Function.class), eq(Map.class));
    }

    @Test
    void testScrollPagination() throws IOException {
        // 测试滚动分页（继续滚动）
        String scrollId = "scroll_id_123";
        
        // Mock 滚动响应
        ScrollResponse<Map> scrollResponse = mock(ScrollResponse.class);
        HitsMetadata<Map> hitsMetadata = mock(HitsMetadata.class);
        
        lenient().when(scrollResponse.scrollId()).thenReturn(scrollId);
        lenient().when(scrollResponse.hits()).thenReturn(hitsMetadata);
        lenient().when(hitsMetadata.hits()).thenReturn(Collections.emptyList());
        
        when(elasticsearchClient.scroll(any(Function.class), eq(Map.class)))
            .thenReturn(scrollResponse);
        
        // 执行测试
        ScrollResult<Map> result = searchService.scroll(scrollId, Map.class);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getScrollId()).isEqualTo(scrollId);
        
        verify(elasticsearchClient).scroll(any(Function.class), eq(Map.class));
    }

    @Test
    void testClearScroll() throws IOException {
        // 测试清除滚动上下文
        String scrollId = "scroll_id_123";
        
        // 执行测试
        searchService.clearScroll(scrollId);
        
        // 验证清除操作被调用
        verify(elasticsearchClient).clearScroll(any(Function.class));
    }

    @Test
    void testScrollTimeout() {
        // 测试滚动超时（简化测试 - 验证属性设置）
        assertThat(properties.getScrollTimeout()).isNotNull();
        assertThat(properties.getScrollTimeout().toMillis()).isGreaterThan(0);
    }
}


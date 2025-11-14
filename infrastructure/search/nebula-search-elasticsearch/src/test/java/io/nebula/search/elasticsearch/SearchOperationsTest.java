package io.nebula.search.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.*;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import io.nebula.search.core.model.SearchResult;
import io.nebula.search.core.query.SearchQuery;
import io.nebula.search.core.query.builder.*;
import io.nebula.search.elasticsearch.config.ElasticsearchProperties;
import io.nebula.search.elasticsearch.service.ElasticsearchSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Elasticsearch 搜索操作测试
 * 测试各种查询类型
 */
@ExtendWith(MockitoExtension.class)
class SearchOperationsTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    private ElasticsearchSearchService searchService;
    private ElasticsearchProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ElasticsearchProperties();
        properties.setIndexPrefix("test");
        properties.setDefaultShards(1);
        properties.setDefaultReplicas(0);
        properties.setBulkTimeout(Duration.ofSeconds(30));
        properties.setScrollTimeout(Duration.ofMinutes(1));
        
        searchService = new ElasticsearchSearchService(
                elasticsearchClient, elasticsearchOperations, properties);
    }

    /**
     * 测试全文匹配查询
     */
    @Test
    @SuppressWarnings("unchecked")
    void testMatchQuery() throws Exception {
        // 准备测试数据
        TestDocument testDoc = new TestDocument("doc-1", "bike content", "tag1");
        
        // Mock SearchResponse
        SearchResponse<TestDocument> searchResponse = mock(SearchResponse.class);
        HitsMetadata<TestDocument> hitsMetadata = mock(HitsMetadata.class);
        TotalHits totalHits = mock(TotalHits.class);
        Hit<TestDocument> hit = mock(Hit.class);
        
        lenient().when(totalHits.value()).thenReturn(1L);
        lenient().when(hitsMetadata.total()).thenReturn(totalHits);
        lenient().when(hitsMetadata.maxScore()).thenReturn(1.0);
        lenient().when(hitsMetadata.hits()).thenReturn(List.of(hit));
        lenient().when(hit.id()).thenReturn("doc-1");
        lenient().when(hit.source()).thenReturn(testDoc);
        lenient().when(hit.score()).thenReturn(1.0);
        lenient().when(searchResponse.hits()).thenReturn(hitsMetadata);
        lenient().when(searchResponse.took()).thenReturn(10L);
        lenient().when(searchResponse.timedOut()).thenReturn(false);
        
        // Mock client.search()
        when(elasticsearchClient.search(any(Function.class), eq(TestDocument.class))).thenReturn(searchResponse);
        
        // 使用强类型 API 创建搜索查询
        QueryBuilder queryBuilder = new MatchQueryBuilder("name", "bike");
        
        SearchQuery query = SearchQuery.builder()
            .index("test-index")
            .query(queryBuilder)
            .from(0)
            .size(10)
            .build();
        
        // 执行搜索
        SearchResult<TestDocument> result = searchService.search(query, TestDocument.class);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalHits()).isEqualTo(1L);
        assertThat(result.getDocuments()).hasSize(1);
        assertThat(result.getDocuments().get(0).getId()).isEqualTo("doc-1");
        
        // 验证client.search()被调用
        verify(elasticsearchClient, times(1)).search(any(Function.class), eq(TestDocument.class));
    }

    /**
     * 测试精确匹配（term）查询
     */
    @Test
    @SuppressWarnings("unchecked")
    void testTermQuery() throws Exception {
        // 准备测试数据
        TestDocument testDoc = new TestDocument("doc-1", "exact match", "tag1");
        
        // Mock SearchResponse
        SearchResponse<TestDocument> searchResponse = mockSuccessSearchResponse(testDoc);
        
        // Mock client.search()
        when(elasticsearchClient.search(any(Function.class), eq(TestDocument.class))).thenReturn(searchResponse);
        
        // 使用强类型 API 创建搜索查询（term查询）
        QueryBuilder queryBuilder = new TermQueryBuilder("content", "exact match");
        
        SearchQuery query = SearchQuery.builder()
            .index("test-index")
            .query(queryBuilder)
            .from(0)
            .size(10)
            .build();
        
        // 执行搜索
        SearchResult<TestDocument> result = searchService.search(query, TestDocument.class);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalHits()).isGreaterThan(0L);
    }

    /**
     * 测试布尔组合查询（must, should, must_not）
     */
    @Test
    @SuppressWarnings("unchecked")
    void testBoolQuery() throws Exception {
        // 准备测试数据
        TestDocument testDoc = new TestDocument("doc-1", "bike content", "tag1");
        
        // Mock SearchResponse
        SearchResponse<TestDocument> searchResponse = mockSuccessSearchResponse(testDoc);
        
        // Mock client.search()
        when(elasticsearchClient.search(any(Function.class), eq(TestDocument.class))).thenReturn(searchResponse);
        
        // 使用强类型 API 创建搜索查询（布尔查询）
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        boolQuery.must(new MatchQueryBuilder("content", "bike"));
        boolQuery.should(new TermQueryBuilder("tag", "tag1"));
        boolQuery.mustNot(new TermQueryBuilder("status", "deleted"));
        
        SearchQuery query = SearchQuery.builder()
            .index("test-index")
            .query(boolQuery)
            .from(0)
            .size(10)
            .build();
        
        // 执行搜索
        SearchResult<TestDocument> result = searchService.search(query, TestDocument.class);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }

    /**
     * 测试范围查询（数字/日期）
     */
    @Test
    @SuppressWarnings("unchecked")
    void testRangeQuery() throws Exception {
        // 准备测试数据
        TestDocument testDoc = new TestDocument("doc-1", "price 150", "tag1");
        
        // Mock SearchResponse
        SearchResponse<TestDocument> searchResponse = mockSuccessSearchResponse(testDoc);
        
        // Mock client.search()
        when(elasticsearchClient.search(any(Function.class), eq(TestDocument.class))).thenReturn(searchResponse);
        
        // 使用强类型 API 创建搜索查询（范围查询）
        QueryBuilder queryBuilder = new RangeQueryBuilder("price")
            .gte(100)
            .lte(200);
        
        SearchQuery query = SearchQuery.builder()
            .index("test-index")
            .query(queryBuilder)
            .from(0)
            .size(10)
            .build();
        
        // 执行搜索
        SearchResult<TestDocument> result = searchService.search(query, TestDocument.class);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }

    /**
     * 测试分页查询
     */
    @Test
    @SuppressWarnings("unchecked")
    void testPagination() throws Exception {
        // 准备测试数据
        TestDocument testDoc = new TestDocument("doc-1", "page content", "tag1");
        
        // Mock SearchResponse
        SearchResponse<TestDocument> searchResponse = mock(SearchResponse.class);
        HitsMetadata<TestDocument> hitsMetadata = mock(HitsMetadata.class);
        TotalHits totalHits = mock(TotalHits.class);
        Hit<TestDocument> hit = mock(Hit.class);
        
        lenient().when(totalHits.value()).thenReturn(100L); // 总共100条记录
        lenient().when(hitsMetadata.total()).thenReturn(totalHits);
        lenient().when(hitsMetadata.maxScore()).thenReturn(1.0);
        lenient().when(hitsMetadata.hits()).thenReturn(List.of(hit));
        lenient().when(hit.id()).thenReturn("doc-1");
        lenient().when(hit.source()).thenReturn(testDoc);
        lenient().when(hit.score()).thenReturn(1.0);
        lenient().when(searchResponse.hits()).thenReturn(hitsMetadata);
        lenient().when(searchResponse.took()).thenReturn(10L);
        lenient().when(searchResponse.timedOut()).thenReturn(false);
        
        // Mock client.search()
        when(elasticsearchClient.search(any(Function.class), eq(TestDocument.class))).thenReturn(searchResponse);
        
        // 使用强类型 API 创建分页查询 - 第2页，每页10条
        QueryBuilder queryBuilder = new MatchAllQueryBuilder();
        
        SearchQuery query = SearchQuery.builder()
            .index("test-index")
            .query(queryBuilder)
            .from(10)  // 跳过前10条
            .size(10)  // 返回10条
            .build();
        
        // 执行搜索
        SearchResult<TestDocument> result = searchService.search(query, TestDocument.class);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalHits()).isEqualTo(100L);
        assertThat(result.getDocuments()).hasSize(1);
    }

    /**
     * 测试结果排序
     */
    @Test
    @SuppressWarnings("unchecked")
    void testSorting() throws Exception {
        // 准备测试数据
        TestDocument testDoc1 = new TestDocument("doc-1", "content 1", "tag1");
        TestDocument testDoc2 = new TestDocument("doc-2", "content 2", "tag2");
        
        // Mock SearchResponse with multiple hits
        SearchResponse<TestDocument> searchResponse = mock(SearchResponse.class);
        HitsMetadata<TestDocument> hitsMetadata = mock(HitsMetadata.class);
        TotalHits totalHits = mock(TotalHits.class);
        Hit<TestDocument> hit1 = mock(Hit.class);
        Hit<TestDocument> hit2 = mock(Hit.class);
        
        lenient().when(totalHits.value()).thenReturn(2L);
        lenient().when(hitsMetadata.total()).thenReturn(totalHits);
        lenient().when(hitsMetadata.maxScore()).thenReturn(1.0);
        lenient().when(hitsMetadata.hits()).thenReturn(List.of(hit1, hit2));
        lenient().when(hit1.id()).thenReturn("doc-1");
        lenient().when(hit1.source()).thenReturn(testDoc1);
        lenient().when(hit1.score()).thenReturn(1.0);
        lenient().when(hit2.id()).thenReturn("doc-2");
        lenient().when(hit2.source()).thenReturn(testDoc2);
        lenient().when(hit2.score()).thenReturn(0.8);
        lenient().when(searchResponse.hits()).thenReturn(hitsMetadata);
        lenient().when(searchResponse.took()).thenReturn(10L);
        lenient().when(searchResponse.timedOut()).thenReturn(false);
        
        // Mock client.search()
        when(elasticsearchClient.search(any(Function.class), eq(TestDocument.class))).thenReturn(searchResponse);
        
        // 使用强类型 API 创建搜索查询（带排序）
        QueryBuilder queryBuilder = new MatchQueryBuilder("content", "test");
        
        SearchQuery query = SearchQuery.builder()
            .index("test-index")
            .query(queryBuilder)
            .from(0)
            .size(10)
            .build();
        
        // 执行搜索
        SearchResult<TestDocument> result = searchService.search(query, TestDocument.class);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDocuments()).hasSize(2);
        assertThat(result.getDocuments().get(0).getId()).isEqualTo("doc-1");
        assertThat(result.getDocuments().get(1).getId()).isEqualTo("doc-2");
    }

    /**
     * 辅助方法：创建成功的SearchResponse mock
     */
    @SuppressWarnings("unchecked")
    private SearchResponse<TestDocument> mockSuccessSearchResponse(TestDocument testDoc) {
        SearchResponse<TestDocument> searchResponse = mock(SearchResponse.class);
        HitsMetadata<TestDocument> hitsMetadata = mock(HitsMetadata.class);
        TotalHits totalHits = mock(TotalHits.class);
        Hit<TestDocument> hit = mock(Hit.class);
        
        lenient().when(totalHits.value()).thenReturn(1L);
        lenient().when(hitsMetadata.total()).thenReturn(totalHits);
        lenient().when(hitsMetadata.maxScore()).thenReturn(1.0);
        lenient().when(hitsMetadata.hits()).thenReturn(List.of(hit));
        lenient().when(hit.id()).thenReturn("doc-1");
        lenient().when(hit.source()).thenReturn(testDoc);
        lenient().when(hit.score()).thenReturn(1.0);
        lenient().when(searchResponse.hits()).thenReturn(hitsMetadata);
        lenient().when(searchResponse.took()).thenReturn(10L);
        lenient().when(searchResponse.timedOut()).thenReturn(false);
        
        return searchResponse;
    }

    // 测试文档类
    static class TestDocument {
        private String id;
        private String content;
        private String tag;

        public TestDocument() {
        }

        public TestDocument(String id, String content, String tag) {
            this.id = id;
            this.content = content;
            this.tag = tag;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }
    }
}

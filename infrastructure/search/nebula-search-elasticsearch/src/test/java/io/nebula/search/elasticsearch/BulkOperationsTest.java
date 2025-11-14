package io.nebula.search.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ErrorCause;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import io.nebula.search.core.model.BulkResult;
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
import java.util.*;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Elasticsearch 批量操作测试
 */
@ExtendWith(MockitoExtension.class)
class BulkOperationsTest {

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
    void testBulkIndex() throws IOException {
        // 准备测试数据
        Map<String, TestDocument> documents = new HashMap<>();
        documents.put("1", new TestDocument("1", "Document 1", "Content 1"));
        documents.put("2", new TestDocument("2", "Document 2", "Content 2"));
        documents.put("3", new TestDocument("3", "Document 3", "Content 3"));

        // Mock 响应
        BulkResponse bulkResponse = mock(BulkResponse.class);
        List<BulkResponseItem> items = Arrays.asList(
            createSuccessItem("1"),
            createSuccessItem("2"),
            createSuccessItem("3")
        );
        when(bulkResponse.items()).thenReturn(items);

        when(elasticsearchClient.bulk(any(Function.class))).thenReturn(bulkResponse);

        // 执行测试
        BulkResult result = searchService.bulkIndexDocuments("test-index", documents);

        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalCount()).isEqualTo(3);
        assertThat(result.getSuccessCount()).isEqualTo(3);
        assertThat(result.getFailureCount()).isEqualTo(0);
        assertThat(result.getErrors()).isEmpty();

        verify(elasticsearchClient).bulk(any(Function.class));
    }

    @Test
    void testBulkUpdate() throws IOException {
        // 注意：ElasticsearchSearchService 没有批量更新方法
        // 但可以通过批量索引来模拟批量更新（使用相同ID覆盖）
        
        Map<String, TestDocument> documents = new HashMap<>();
        documents.put("1", new TestDocument("1", "Updated 1", "New Content 1"));
        documents.put("2", new TestDocument("2", "Updated 2", "New Content 2"));

        BulkResponse bulkResponse = mock(BulkResponse.class);
        List<BulkResponseItem> items = Arrays.asList(
            createSuccessItem("1"),
            createSuccessItem("2")
        );
        when(bulkResponse.items()).thenReturn(items);

        when(elasticsearchClient.bulk(any(Function.class))).thenReturn(bulkResponse);

        // 使用 bulkIndex 模拟更新（覆盖现有文档）
        BulkResult result = searchService.bulkIndexDocuments("test-index", documents);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getFailureCount()).isEqualTo(0);
    }

    @Test
    void testBulkDelete() throws IOException {
        // 准备测试数据
        List<String> ids = Arrays.asList("1", "2", "3");

        // Mock 响应
        BulkResponse bulkResponse = mock(BulkResponse.class);
        List<BulkResponseItem> items = Arrays.asList(
            createSuccessItem("1"),
            createSuccessItem("2"),
            createSuccessItem("3")
        );
        when(bulkResponse.items()).thenReturn(items);

        when(elasticsearchClient.bulk(any(Function.class))).thenReturn(bulkResponse);

        // 执行测试
        BulkResult result = searchService.bulkDeleteDocuments("test-index", ids);

        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalCount()).isEqualTo(3);
        assertThat(result.getSuccessCount()).isEqualTo(3);
        assertThat(result.getFailureCount()).isEqualTo(0);
        assertThat(result.getErrors()).isEmpty();

        verify(elasticsearchClient).bulk(any(Function.class));
    }

    @Test
    void testBulkMixedOperations() throws IOException {
        // 由于 ElasticsearchSearchService 没有统一的批量操作接口
        // 这里测试先批量索引，再批量删除的场景
        
        // 1. 批量索引
        Map<String, TestDocument> documents = new HashMap<>();
        documents.put("1", new TestDocument("1", "Doc 1", "Content 1"));
        documents.put("2", new TestDocument("2", "Doc 2", "Content 2"));

        // 先创建所有的 item，避免在 when() 调用中创建
        BulkResponseItem item1 = createSuccessItem("1");
        BulkResponseItem item2 = createSuccessItem("2");
        BulkResponseItem item3 = createSuccessItem("1");

        BulkResponse indexResponse = mock(BulkResponse.class);
        when(indexResponse.items()).thenReturn(Arrays.asList(item1, item2));

        BulkResponse deleteResponse = mock(BulkResponse.class);
        when(deleteResponse.items()).thenReturn(Arrays.asList(item3));

        when(elasticsearchClient.bulk(any(Function.class)))
            .thenReturn(indexResponse)
            .thenReturn(deleteResponse);

        // 执行批量索引
        BulkResult indexResult = searchService.bulkIndexDocuments("test-index", documents);
        assertThat(indexResult.getSuccessCount()).isEqualTo(2);

        // 执行批量删除
        BulkResult deleteResult = searchService.bulkDeleteDocuments("test-index", Arrays.asList("1"));
        assertThat(deleteResult.getSuccessCount()).isEqualTo(1);

        verify(elasticsearchClient, times(2)).bulk(any(Function.class));
    }

    @Test
    void testBulkErrorHandling() throws IOException {
        // 准备测试数据（3个文档，其中2个失败）
        Map<String, TestDocument> documents = new HashMap<>();
        documents.put("1", new TestDocument("1", "Document 1", "Content 1"));
        documents.put("2", new TestDocument("2", "Document 2", "Content 2"));
        documents.put("3", new TestDocument("3", "Document 3", "Content 3"));

        // Mock 响应（1个成功，2个失败）
        BulkResponse bulkResponse = mock(BulkResponse.class);
        List<BulkResponseItem> items = Arrays.asList(
            createSuccessItem("1"),
            createFailureItem("2", "Version conflict"),
            createFailureItem("3", "Document already exists")
        );
        when(bulkResponse.items()).thenReturn(items);

        when(elasticsearchClient.bulk(any(Function.class))).thenReturn(bulkResponse);

        // 执行测试
        BulkResult result = searchService.bulkIndexDocuments("test-index", documents);

        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue(); // service标记为success，因为请求成功（即使部分失败）
        assertThat(result.getTotalCount()).isEqualTo(3);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(2);
        assertThat(result.getErrors()).hasSize(2);
        assertThat(result.getErrors()).contains("Version conflict", "Document already exists");

        verify(elasticsearchClient).bulk(any(Function.class));
    }

    // ========== Helper Methods ==========

    private BulkResponseItem createSuccessItem(String id) {
        BulkResponseItem item = mock(BulkResponseItem.class);
        lenient().when(item.id()).thenReturn(id);
        lenient().when(item.error()).thenReturn(null);
        return item;
    }

    private BulkResponseItem createFailureItem(String id, String errorMessage) {
        BulkResponseItem item = mock(BulkResponseItem.class);
        lenient().when(item.id()).thenReturn(id);
        
        ErrorCause error = mock(ErrorCause.class);
        lenient().when(error.reason()).thenReturn(errorMessage);
        
        lenient().when(item.error()).thenReturn(error);
        return item;
    }

    // ========== Test Data Classes ==========

    static class TestDocument {
        private String id;
        private String title;
        private String content;

        public TestDocument() {}

        public TestDocument(String id, String title, String content) {
            this.id = id;
            this.title = title;
            this.content = content;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}


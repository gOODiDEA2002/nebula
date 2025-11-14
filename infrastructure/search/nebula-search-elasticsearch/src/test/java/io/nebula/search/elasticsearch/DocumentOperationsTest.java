package io.nebula.search.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch._types.Result;
import io.nebula.search.core.model.DocumentResult;
import io.nebula.search.elasticsearch.config.ElasticsearchProperties;
import io.nebula.search.elasticsearch.service.ElasticsearchSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.time.Duration;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Elasticsearch 文档操作测试
 * 测试文档的CRUD操作
 */
@ExtendWith(MockitoExtension.class)
class DocumentOperationsTest {

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
     * 测试文档索引操作
     */
    @Test
    @SuppressWarnings("unchecked")
    void testIndexDocument() throws Exception {
        // 准备测试文档
        TestDocument testDoc = new TestDocument("doc-1", "Test Content", "tag1");
        
        // Mock IndexResponse
        IndexResponse indexResponse = mock(IndexResponse.class);
        lenient().when(indexResponse.id()).thenReturn("doc-1");
        lenient().when(indexResponse.result()).thenReturn(Result.Created);
        
        // Mock client.index() - 使用Function.class明确指定参数类型
        when(elasticsearchClient.index(any(Function.class))).thenReturn(indexResponse);
        
        // 执行索引操作
        DocumentResult result = searchService.indexDocument("test-index", "doc-1", testDoc);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getId()).isEqualTo("doc-1");
        assertThat(result.getIndex()).isEqualTo("test_test-index");
        
        // 验证client.index()被调用
        verify(elasticsearchClient, times(1)).index(any(Function.class));
    }

    /**
     * 测试获取文档操作
     */
    @Test
    @SuppressWarnings("unchecked")
    void testGetDocument() throws Exception {
        // 准备测试数据
        TestDocument expectedDoc = new TestDocument("doc-1", "Test Content", "tag1");
        
        // Mock GetResponse
        GetResponse<TestDocument> getResponse = mock(GetResponse.class);
        when(getResponse.found()).thenReturn(true);
        when(getResponse.source()).thenReturn(expectedDoc);
        
        // Mock client.get() - 使用Function.class和Class.class明确指定参数类型
        when(elasticsearchClient.get(any(Function.class), any(Class.class))).thenReturn((GetResponse)getResponse);
        
        // 执行获取操作
        TestDocument result = searchService.getDocument("test-index", "doc-1", TestDocument.class);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("doc-1");
        assertThat(result.getContent()).isEqualTo("Test Content");
        
        // 验证client.get()被调用
        verify(elasticsearchClient, times(1)).get(any(Function.class), any(Class.class));
    }

    /**
     * 测试获取不存在的文档
     */
    @Test
    @SuppressWarnings("unchecked")
    void testGetDocumentNotFound() throws Exception {
        // Mock GetResponse (文档不存在)
        GetResponse<TestDocument> getResponse = mock(GetResponse.class);
        when(getResponse.found()).thenReturn(false);
        
        // Mock client.get()
        when(elasticsearchClient.get(any(Function.class), any(Class.class))).thenReturn((GetResponse)getResponse);
        
        // 执行获取操作
        TestDocument result = searchService.getDocument("test-index", "non-existent", TestDocument.class);
        
        // 验证结果为null
        assertThat(result).isNull();
    }

    /**
     * 测试更新文档操作
     */
    @Test
    @SuppressWarnings("unchecked")
    void testUpdateDocument() throws Exception {
        // 准备测试文档
        TestDocument updatedDoc = new TestDocument("doc-1", "Updated Content", "tag2");
        
        // Mock UpdateResponse
        UpdateResponse<TestDocument> updateResponse = mock(UpdateResponse.class);
        lenient().when(updateResponse.id()).thenReturn("doc-1");
        lenient().when(updateResponse.result()).thenReturn(Result.Updated);
        
        // Mock client.update() - 使用Function.class和Class.class明确指定参数类型
        when(elasticsearchClient.update(any(Function.class), any(Class.class))).thenReturn((UpdateResponse)updateResponse);
        
        // 执行更新操作
        DocumentResult result = searchService.updateDocument("test-index", "doc-1", updatedDoc);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getId()).isEqualTo("doc-1");
        assertThat(result.getIndex()).isEqualTo("test_test-index");
        
        // 验证client.update()被调用
        verify(elasticsearchClient, times(1)).update(any(Function.class), any(Class.class));
    }

    /**
     * 测试删除文档操作
     */
    @Test
    @SuppressWarnings("unchecked")
    void testDeleteDocument() throws Exception {
        // Mock DeleteResponse
        DeleteResponse deleteResponse = mock(DeleteResponse.class);
        lenient().when(deleteResponse.id()).thenReturn("doc-1");
        lenient().when(deleteResponse.result()).thenReturn(Result.Deleted);
        
        // Mock client.delete() - 使用Function.class明确指定参数类型
        when(elasticsearchClient.delete(any(Function.class))).thenReturn(deleteResponse);
        
        // 执行删除操作
        DocumentResult result = searchService.deleteDocument("test-index", "doc-1");
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getId()).isEqualTo("doc-1");
        assertThat(result.getIndex()).isEqualTo("test_test-index");
        
        // 验证client.delete()被调用
        verify(elasticsearchClient, times(1)).delete(any(Function.class));
    }

    /**
     * 测试文档操作异常处理
     */
    @Test
    @SuppressWarnings("unchecked")
    void testIndexDocumentWithException() throws Exception {
        // 准备测试文档
        TestDocument testDoc = new TestDocument("doc-1", "Test Content", "tag1");
        
        // Mock client.index() 抛出异常 - 使用Function.class明确指定参数类型
        when(elasticsearchClient.index(any(Function.class))).thenThrow(new RuntimeException("Indexing failed"));
        
        // 执行索引操作
        DocumentResult result = searchService.indexDocument("test-index", "doc-1", testDoc);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Indexing failed");
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


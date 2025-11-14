package io.nebula.search.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import io.nebula.search.core.model.IndexInfo;
import io.nebula.search.core.model.IndexMapping;
import io.nebula.search.core.model.IndexResult;
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
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Elasticsearch 索引管理测试
 */
@ExtendWith(MockitoExtension.class)
class IndexManagementTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private ElasticsearchProperties properties;

    @Mock
    private ElasticsearchIndicesClient indicesClient;

    private ElasticsearchSearchService searchService;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getIndexPrefix()).thenReturn("nebula-");
        lenient().when(properties.getBulkTimeout()).thenReturn(Duration.ofSeconds(30));
        lenient().when(properties.getDefaultShards()).thenReturn(1);
        lenient().when(properties.getDefaultReplicas()).thenReturn(1);
        
        lenient().when(elasticsearchClient.indices()).thenReturn(indicesClient);
        
        searchService = new ElasticsearchSearchService(
            elasticsearchClient,
            elasticsearchOperations,
            properties
        );
    }

    @Test
    void testCreateIndex() throws IOException {
        // 准备测试数据
        String indexName = "test-index";
        IndexMapping mapping = new IndexMapping();
        
        // Mock 响应
        CreateIndexResponse response = mock(CreateIndexResponse.class);
        lenient().when(response.acknowledged()).thenReturn(true);
        lenient().when(response.index()).thenReturn("nebula-test-index");
        
        when(indicesClient.create(any(Function.class))).thenReturn(response);
        
        // 执行测试
        IndexResult result = searchService.createIndex(indexName, mapping);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getIndexName()).isEqualTo("nebula-_test-index"); // prefix + "_" + indexName
        assertThat(result.isAcknowledged()).isTrue();
        
        verify(indicesClient).create(any(Function.class));
    }

    @Test
    void testDeleteIndex() throws IOException {
        // 准备测试数据
        String indexName = "test-index";
        
        // Mock 响应
        DeleteIndexResponse response = mock(DeleteIndexResponse.class);
        lenient().when(response.acknowledged()).thenReturn(true);
        
        when(indicesClient.delete(any(Function.class))).thenReturn(response);
        
        // 执行测试
        IndexResult result = searchService.deleteIndex(indexName);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getIndexName()).isEqualTo("nebula-_test-index"); // prefix + "_" + indexName
        
        verify(indicesClient).delete(any(Function.class));
    }

    @Test
    void testExistsIndex() throws IOException {
        // 准备测试数据
        String indexName = "test-index";
        
        // Mock 响应 - 索引存在
        BooleanResponse existsResponse = mock(BooleanResponse.class);
        when(existsResponse.value()).thenReturn(true);
        
        when(indicesClient.exists(any(Function.class))).thenReturn(existsResponse);
        
        // 执行测试
        boolean exists = searchService.indexExists(indexName);
        
        // 验证结果
        assertThat(exists).isTrue();
        
        verify(indicesClient).exists(any(Function.class));
    }

    @Test
    void testExistsIndexNotFound() throws IOException {
        // 准备测试数据
        String indexName = "non-existent-index";
        
        // Mock 响应 - 索引不存在
        BooleanResponse existsResponse = mock(BooleanResponse.class);
        when(existsResponse.value()).thenReturn(false);
        
        when(indicesClient.exists(any(Function.class))).thenReturn(existsResponse);
        
        // 执行测试
        boolean exists = searchService.indexExists(indexName);
        
        // 验证结果
        assertThat(exists).isFalse();
        
        verify(indicesClient).exists(any(Function.class));
    }

    @Test
    void testGetIndexInfo() throws IOException {
        // 准备测试数据
        String indexName = "test-index";
        
        // Mock 响应
        GetIndexResponse response = mock(GetIndexResponse.class);
        
        when(indicesClient.get(any(Function.class))).thenReturn(response);
        
        // 执行测试
        IndexInfo info = searchService.getIndexInfo(indexName);
        
        // 验证结果
        assertThat(info).isNotNull();
        assertThat(info.getIndexName()).isEqualTo("nebula-_test-index"); // prefix + "_" + indexName
        
        verify(indicesClient).get(any(Function.class));
    }

    @Test
    void testCreateIndexWithException() throws IOException {
        // 准备测试数据
        String indexName = "test-index";
        IndexMapping mapping = new IndexMapping();
        
        // Mock 异常
        when(indicesClient.create(any(Function.class)))
            .thenThrow(new IOException("Failed to create index"));
        
        // 执行测试
        IndexResult result = searchService.createIndex(indexName, mapping);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Failed to create index");
        
        verify(indicesClient).create(any(Function.class));
    }

    // 注意：以下方法在当前 ElasticsearchSearchService 中尚未实现
    // 当实现后，需要添加相应的测试
    
    // TODO: testUpdateMapping() - 测试更新映射
    // 需要实现 ElasticsearchSearchService.updateMapping() 方法
    
    // TODO: testUpdateIndexSettings() - 测试更新索引设置
    // 需要实现 ElasticsearchSearchService.updateIndexSettings() 方法
}


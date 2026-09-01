package io.nebula.search.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.util.ObjectBuilder;
import io.nebula.search.core.model.IndexMapping;
import io.nebula.search.core.model.IndexResult;
import io.nebula.search.elasticsearch.config.ElasticsearchProperties;
import io.nebula.search.elasticsearch.service.ElasticsearchSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * createIndex 的 mapping/settings 下发（P4a，详细设计 §3.1）
 * <p>
 * 通过捕获传给客户端的请求构造函数、应用到一个真实的 {@link CreateIndexRequest.Builder} 上，
 * 检视构建出的请求对象是否携带了 mappings / settings。IK analyzer 样例只做「构造正确性」断言，
 * 真实 IK 环境验证归 SIA 侧后续采用时。
 */
@ExtendWith(MockitoExtension.class)
class CreateIndexMappingTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private ElasticsearchProperties properties;

    @Mock
    private ElasticsearchIndicesClient indicesClient;

    private ElasticsearchSearchService searchService;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getIndexPrefix()).thenReturn("");
        lenient().when(properties.getDefaultShards()).thenReturn(1);
        lenient().when(properties.getDefaultReplicas()).thenReturn(1);
        lenient().when(elasticsearchClient.indices()).thenReturn(indicesClient);
        lenient().when(elasticsearchClient._jsonpMapper()).thenReturn(new JacksonJsonpMapper());
        searchService = new ElasticsearchSearchService(elasticsearchClient, properties);
    }

    @Test
    void nonEmptyProperties_areAppliedToRequestMappings() throws Exception {
        // 与既有调用方一致：IndexMapping.properties 承载完整 mappings 主体（含 properties 键）
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("content", Map.of("type", "text", "analyzer", "standard"));
        fields.put("docId", Map.of("type", "keyword"));
        IndexMapping mapping = new IndexMapping(Map.of("properties", fields));

        CreateIndexRequest request = captureRequest(mapping);

        assertThat(request.mappings()).isNotNull();
        assertThat(request.mappings().properties()).containsKeys("content", "docId");
        assertThat(request.mappings().properties().get("content").isText()).isTrue();
    }

    @Test
    void ikSettingsSample_isAppliedAndUserKeysOverrideDefaults() throws Exception {
        Map<String, Object> analyzer = Map.of("ik_analyzer",
                Map.of("type", "custom", "tokenizer", "ik_max_word"));
        Map<String, Object> analysis = Map.of("analyzer", analyzer);
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("number_of_shards", "3");   // 用户键覆盖默认 1
        settings.put("analysis", analysis);
        IndexMapping mapping = new IndexMapping();
        mapping.setSettings(settings);

        CreateIndexRequest request = captureRequest(mapping);

        assertThat(request.settings()).isNotNull();
        assertThat(request.settings().numberOfShards()).isEqualTo("3");
        // 默认副本仍在（未被用户覆盖）
        assertThat(request.settings().numberOfReplicas()).isEqualTo("1");
        // IK analyzer 进入 analysis
        assertThat(request.settings().analysis()).isNotNull();
        assertThat(request.settings().analysis().analyzer()).containsKey("ik_analyzer");
    }

    @Test
    void emptyMapping_takesLegacyDefaultPath() throws Exception {
        CreateIndexRequest request = captureRequest(new IndexMapping());

        assertThat(request.mappings()).isNull();
        assertThat(request.settings()).isNotNull();
        assertThat(request.settings().numberOfShards()).isEqualTo("1");
        assertThat(request.settings().numberOfReplicas()).isEqualTo("1");
    }

    @Test
    void malformedProperties_yieldErrorResultMentioningIndexName() {
        // properties 值不是合法字段定义（字符串而非对象），反序列化必失败
        IndexMapping mapping = new IndexMapping(
                Map.of("properties", Map.of("content", "not-an-object")));

        IndexResult result = searchService.createIndex("broken-index", mapping);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("broken-index");
    }

    @SuppressWarnings("unchecked")
    private CreateIndexRequest captureRequest(IndexMapping mapping) throws Exception {
        CreateIndexResponse response = mock(CreateIndexResponse.class);
        when(indicesClient.create(any(Function.class))).thenReturn(response);

        searchService.createIndex("test-index", mapping);

        ArgumentCaptor<Function<CreateIndexRequest.Builder, ObjectBuilder<CreateIndexRequest>>> captor =
                ArgumentCaptor.forClass(Function.class);
        verify(indicesClient).create(captor.capture());
        return captor.getValue().apply(new CreateIndexRequest.Builder()).build();
    }
}

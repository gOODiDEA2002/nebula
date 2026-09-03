package io.nebula.search.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesRequest;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesResponse;
import co.elastic.clients.elasticsearch.indices.update_aliases.Action;
import co.elastic.clients.util.ObjectBuilder;
import io.nebula.search.core.SearchService;
import io.nebula.search.core.model.IndexResult;
import io.nebula.search.elasticsearch.config.ElasticsearchProperties;
import io.nebula.search.elasticsearch.service.ElasticsearchSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 索引别名切换与解析（R3 §4.1）
 * <p>
 * switchAlias 通过捕获传给客户端的请求构造函数、应用到真实的 {@link UpdateAliasesRequest.Builder}，
 * 检视操作列表（别名存在 → remove 旧 + add 新一次提交；不存在 → 仅 add）。resolveAlias 对 404
 * 返回空列表。默认 default 方法在未覆盖的实现上抛 {@link UnsupportedOperationException} / 返回空列表。
 */
class AliasOperationsTest {

    private ElasticsearchClient client;
    private ElasticsearchIndicesClient indicesClient;
    private ElasticsearchSearchService service;

    @BeforeEach
    void setUp() {
        client = mock(ElasticsearchClient.class);
        indicesClient = mock(ElasticsearchIndicesClient.class);
        ElasticsearchProperties properties = mock(ElasticsearchProperties.class);
        lenient().when(properties.getIndexPrefix()).thenReturn("");
        lenient().when(client.indices()).thenReturn(indicesClient);
        service = new ElasticsearchSearchService(client, properties);
    }

    @Test
    @SuppressWarnings("unchecked")
    void switchAlias_existingAlias_removesOldAndAddsNewInOneRequest() throws Exception {
        ElasticsearchSearchService spy = spy(service);
        doReturn(List.of("chunks-g1")).when(spy).resolveAlias("chunks");
        when(indicesClient.updateAliases(any(Function.class))).thenReturn(mock(UpdateAliasesResponse.class));

        IndexResult result = spy.switchAlias("chunks", "chunks-g2");

        assertThat(result.isSuccess()).isTrue();
        UpdateAliasesRequest request = captureUpdateAliases();
        List<Action> actions = request.actions();
        // 一次请求内: remove 旧(chunks-g1) + add 新(chunks-g2)
        assertThat(actions).hasSize(2);
        assertThat(actions.get(0).isRemove()).isTrue();
        assertThat(actions.get(0).remove().index()).isEqualTo("chunks-g1");
        assertThat(actions.get(0).remove().alias()).isEqualTo("chunks");
        assertThat(actions.get(1).isAdd()).isTrue();
        assertThat(actions.get(1).add().index()).isEqualTo("chunks-g2");
        assertThat(actions.get(1).add().alias()).isEqualTo("chunks");
    }

    @Test
    @SuppressWarnings("unchecked")
    void switchAlias_missingAlias_onlyAdds() throws Exception {
        ElasticsearchSearchService spy = spy(service);
        doReturn(List.of()).when(spy).resolveAlias("chunks");
        when(indicesClient.updateAliases(any(Function.class))).thenReturn(mock(UpdateAliasesResponse.class));

        spy.switchAlias("chunks", "chunks-g1");

        UpdateAliasesRequest request = captureUpdateAliases();
        List<Action> actions = request.actions();
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).isAdd()).isTrue();
        assertThat(actions.get(0).add().index()).isEqualTo("chunks-g1");
        assertThat(actions.get(0).add().alias()).isEqualTo("chunks");
    }

    @Test
    @SuppressWarnings("unchecked")
    void resolveAlias_notFound_returnsEmptyList() throws Exception {
        ErrorResponse error = ErrorResponse.of(b -> b
                .status(404)
                .error(e -> e.type("alias_not_found_exception").reason("alias [chunks] missing")));
        ElasticsearchException notFound = new ElasticsearchException("indices.get_alias", error);
        when(indicesClient.getAlias(any(Function.class))).thenThrow(notFound);

        assertThat(service.resolveAlias("chunks")).isEmpty();
    }

    @Test
    void defaultMethods_onUnsupportedImplementation_throwOrReturnEmpty() {
        // 未覆盖别名能力的 SearchService 实现继承为「不支持」
        SearchService bare = mock(SearchService.class, CALLS_REAL_METHODS);

        assertThatThrownBy(() -> bare.switchAlias("a", "b"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(bare.resolveAlias("a")).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private UpdateAliasesRequest captureUpdateAliases() throws Exception {
        var captor = forClass(Function.class);
        verify(indicesClient).updateAliases((Function<UpdateAliasesRequest.Builder,
                ObjectBuilder<UpdateAliasesRequest>>) captor.capture());
        return ((Function<UpdateAliasesRequest.Builder, ObjectBuilder<UpdateAliasesRequest>>)
                captor.getValue()).apply(new UpdateAliasesRequest.Builder()).build();
    }
}

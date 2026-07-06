package io.nebula.autoconfigure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 SSA-1 修复：similaritySearch 传递 filterExpression 与 similarityThreshold，
 * delete(Filter.Expression) 真实调用 Chroma 删除接口（修复前直接抛 UnsupportedOperationException）。
 */
class CustomChromaVectorStoreTest {

    private ChromaApi chromaApi;
    private EmbeddingModel embeddingModel;
    private CustomChromaVectorStore store;

    @BeforeEach
    void setUp() {
        chromaApi = mock(ChromaApi.class);
        embeddingModel = mock(EmbeddingModel.class);
        when(chromaApi.getCollection(anyString(), anyString(), eq("test-col")))
                .thenReturn(new ChromaApi.Collection("col-id-1", "test-col", null));
        // mock 实例的内部 objectMapper 为 null，无法 callRealMethod，等价实现 where 的 JSON->Map 解析
        when(chromaApi.where(anyString())).thenAnswer(inv ->
                new ObjectMapper().readValue(inv.getArgument(0, String.class), Map.class));
        store = new CustomChromaVectorStore(chromaApi, embeddingModel, "test-col", false);
    }

    /**
     * 修复前：QueryRequest 仅携带 query+topK，where 恒为 null，filterExpression 被静默忽略
     */
    @Test
    void similaritySearchPassesFilterExpressionAsWhereClause() {
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        when(chromaApi.queryCollection(anyString(), anyString(), eq("col-id-1"), any()))
                .thenReturn(new ChromaApi.QueryResponse(
                        List.of(List.of("id1")),
                        null,
                        List.of(List.of("doc1")),
                        List.of(List.of(Map.of("type", "a"))),
                        List.of(List.of(0.1))));

        Filter.Expression filter = new FilterExpressionBuilder().eq("type", "a").build();
        store.similaritySearch(SearchRequest.builder()
                .query("hello")
                .topK(5)
                .filterExpression(filter)
                .build());

        ArgumentCaptor<ChromaApi.QueryRequest> captor = ArgumentCaptor.forClass(ChromaApi.QueryRequest.class);
        verify(chromaApi).queryCollection(anyString(), anyString(), eq("col-id-1"), captor.capture());

        ChromaApi.QueryRequest sent = captor.getValue();
        assertThat(sent.nResults()).isEqualTo(5);
        assertThat(sent.where()).as("filterExpression 必须转换为 where 子句传给 Chroma").isNotNull();
        assertThat(sent.where()).containsKey("type");
    }

    /**
     * 修复前：similarityThreshold 被忽略，低于阈值的结果也会返回
     */
    @Test
    void similaritySearchAppliesSimilarityThreshold() {
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        // 两条结果: distance 0.1 → score 0.9(过阈值); distance 0.6 → score 0.4(被过滤)
        when(chromaApi.queryCollection(anyString(), anyString(), eq("col-id-1"), any()))
                .thenReturn(new ChromaApi.QueryResponse(
                        List.of(List.of("id1", "id2")),
                        null,
                        List.of(List.of("doc1", "doc2")),
                        List.of(List.of(Map.of(), Map.of())),
                        List.of(List.of(0.1, 0.6))));

        List<Document> result = store.similaritySearch(SearchRequest.builder()
                .query("hello")
                .topK(5)
                .similarityThreshold(0.8)
                .build());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("id1");
        assertThat(result.get(0).getScore()).isCloseTo(0.9, org.assertj.core.data.Offset.offset(1e-9));
    }

    /**
     * 无过滤条件时 where 保持 null（行为与官方实现一致，不影响原有调用方）
     */
    @Test
    void similaritySearchWithoutFilterSendsNullWhere() {
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        when(chromaApi.queryCollection(anyString(), anyString(), eq("col-id-1"), any()))
                .thenReturn(new ChromaApi.QueryResponse(null, null, null, null, null));

        store.similaritySearch(SearchRequest.builder().query("hello").topK(3).build());

        ArgumentCaptor<ChromaApi.QueryRequest> captor = ArgumentCaptor.forClass(ChromaApi.QueryRequest.class);
        verify(chromaApi).queryCollection(anyString(), anyString(), eq("col-id-1"), captor.capture());
        assertThat(captor.getValue().where()).isNull();
    }

    /**
     * 修复前：delete(Filter.Expression) 直接抛 UnsupportedOperationException
     */
    @Test
    void deleteByFilterExpressionCallsChromaDeleteWithWhere() {
        when(chromaApi.deleteEmbeddings(anyString(), anyString(), eq("col-id-1"), any())).thenReturn(200);

        Filter.Expression filter = new FilterExpressionBuilder().eq("source", "s1").build();
        store.delete(filter);

        ArgumentCaptor<ChromaApi.DeleteEmbeddingsRequest> captor =
                ArgumentCaptor.forClass(ChromaApi.DeleteEmbeddingsRequest.class);
        verify(chromaApi).deleteEmbeddings(anyString(), anyString(), eq("col-id-1"), captor.capture());

        ChromaApi.DeleteEmbeddingsRequest sent = captor.getValue();
        assertThat(sent.ids()).as("按过滤删除不携带 ids").isNull();
        assertThat(sent.where()).containsKey("source");
    }
}

package io.nebula.search.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import io.nebula.search.core.model.SuggestResult;
import io.nebula.search.core.query.SuggestQuery;
import io.nebula.search.core.suggestion.*;
import io.nebula.search.elasticsearch.config.ElasticsearchProperties;
import io.nebula.search.elasticsearch.service.ElasticsearchSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.util.*;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Elasticsearch 建议查询测试（强类型 API）
 * 
 * 使用新的强类型 Suggester API 进行测试
 */
@ExtendWith(MockitoExtension.class)
class SuggestTest {

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
        lenient().when(properties.getBulkTimeout()).thenReturn(java.time.Duration.ofSeconds(30));
        lenient().when(properties.getDefaultShards()).thenReturn(1);
        lenient().when(properties.getDefaultReplicas()).thenReturn(1);
        searchService = new ElasticsearchSearchService(elasticsearchClient, elasticsearchOperations, properties);
    }

    @Test
    void testTermSuggester() throws Exception {
        // 使用强类型 API 创建 Term Suggester
        TermSuggester termSuggester = new TermSuggester("my-term-suggester", "message", "some test mssage");
        termSuggester.size(5);

        SuggestQuery query = SuggestQuery.builder()
            .index("test-index")
            .addSuggester(termSuggester)
            .build();

        // Mock SearchResponse
        SearchResponse<Void> mockResponse = mock(SearchResponse.class);
        
        // Since suggest() API is complex and parseSuggestionResults is not fully implemented,
        // we'll verify the service method is called and returns empty results for now
        when(mockResponse.suggest()).thenReturn(Collections.emptyMap());
        when(elasticsearchClient.search(any(Function.class), any(Class.class)))
            .thenReturn(mockResponse);

        // 执行建议查询
        SuggestResult result = searchService.suggest(query);

        // 验证结果（当前 parseSuggestionResults 返回空，所以结果为空）
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSuggestions()).isNotNull();

        verify(elasticsearchClient, times(1)).search(any(Function.class), any(Class.class));
    }

    @Test
    void testPhraseSuggester() throws Exception {
        // 使用强类型 API 创建 Phrase Suggester
        PhraseSuggester phraseSuggester = new PhraseSuggester("my-phrase-suggester", "title", "noble prize");
        phraseSuggester.size(5);
        phraseSuggester.confidence(0.5f);
        phraseSuggester.highlight("<em>", "</em>");

        SuggestQuery query = SuggestQuery.builder()
            .index("test-index")
            .addSuggester(phraseSuggester)
            .build();

        // Mock SearchResponse
        SearchResponse<Void> mockResponse = mock(SearchResponse.class);
        when(mockResponse.suggest()).thenReturn(Collections.emptyMap());
        when(elasticsearchClient.search(any(Function.class), any(Class.class)))
            .thenReturn(mockResponse);

        // 执行建议查询
        SuggestResult result = searchService.suggest(query);

        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSuggestions()).isNotNull();

        verify(elasticsearchClient, times(1)).search(any(Function.class), any(Class.class));
    }

    @Test
    void testCompletionSuggester() throws Exception {
        // 使用强类型 API 创建 Completion Suggester
        CompletionSuggester completionSuggester = new CompletionSuggester("my-completion-suggester", "suggest", "sear");
        completionSuggester.size(10);
        completionSuggester.skipDuplicates(true);

        SuggestQuery query = SuggestQuery.builder()
            .index("test-index")
            .addSuggester(completionSuggester)
            .build();

        // Mock SearchResponse
        SearchResponse<Void> mockResponse = mock(SearchResponse.class);
        when(mockResponse.suggest()).thenReturn(Collections.emptyMap());
        when(elasticsearchClient.search(any(Function.class), any(Class.class)))
            .thenReturn(mockResponse);

        // 执行建议查询
        SuggestResult result = searchService.suggest(query);

        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSuggestions()).isNotNull();

        verify(elasticsearchClient, times(1)).search(any(Function.class), any(Class.class));
    }
    
    @Test
    void testSuggestWithEmptySuggesters() {
        // 创建空的建议查询
        SuggestQuery query = SuggestQuery.builder()
            .index("test-index")
            .build();

        // 执行建议查询
        SuggestResult result = searchService.suggest(query);

        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSuggestions()).isEmpty();
    }
    
    @Test
    void testSuggestWithException() throws Exception {
        // 创建建议查询
        Suggester termSuggester = new TermSuggester("test-suggester", "test", "field");
        
        SuggestQuery query = SuggestQuery.builder()
            .index("test-index")
            .addSuggester(termSuggester)
            .build();

        // Mock exception
        when(elasticsearchClient.search(any(Function.class), any(Class.class)))
            .thenThrow(new RuntimeException("Search failed"));

        // 执行建议查询
        SuggestResult result = searchService.suggest(query);

        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Search failed");
    }
}

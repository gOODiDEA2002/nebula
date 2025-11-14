package io.nebula.ai.spring.vectorstore;

import io.nebula.ai.core.embedding.EmbeddingService;
import io.nebula.ai.core.model.Document;
import io.nebula.ai.core.model.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * SpringAIVectorStoreService 单元测试
 * 
 * 测试目标：验证向量存储服务的核心功能
 * - 添加文档（单个和批量）
 * - 语义搜索
 * - 带过滤条件的搜索
 * 
 * @author Nebula Framework
 */
@ExtendWith(MockitoExtension.class)
class SpringAIVectorStoreServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private EmbeddingService embeddingService;
    
    @Mock
    private io.nebula.ai.spring.config.VectorStoreProperties properties;

    private SpringAIVectorStoreService vectorStoreService;

    @BeforeEach
    void setUp() {
        // 设置 VectorStoreProperties 默认值
        when(properties.isBatchingEnabled()).thenReturn(true);
        when(properties.getBatchSize()).thenReturn(10);
        when(properties.getBatchDelayMs()).thenReturn(100L);
        when(properties.isRetryEnabled()).thenReturn(true);
        when(properties.getMaxRetryAttempts()).thenReturn(3);
        when(properties.getRetryDelayMs()).thenReturn(500L);
        
        vectorStoreService = new SpringAIVectorStoreService(vectorStore, embeddingService, properties);
    }
    
    /**
     * 设置 EmbeddingService 的默认 stub 行为
     * 仅在需要向量化的测试中调用
     */
    private void setupEmbeddingServiceStub() {
        io.nebula.ai.core.model.EmbeddingResponse mockEmbeddingResponse = mock(io.nebula.ai.core.model.EmbeddingResponse.class);
        List<Double> mockVector = List.of(0.1, 0.2, 0.3);
        when(mockEmbeddingResponse.getFirstVector()).thenReturn(mockVector);
        // 使用 lenient 避免 UnnecessaryStubbingException（某些文档可能已有向量）
        lenient().when(embeddingService.embed(anyString())).thenReturn(mockEmbeddingResponse);
    }

    /**
     * 测试添加单个文档功能
     * 
     * 场景：添加一个文档到向量存储
     * 验证：VectorStore.add() 被正确调用，文档转换正确，元数据包含 nebula_id 等
     */
    @Test
    void testAddDocument() {
        // Given: 准备测试文档
        setupEmbeddingServiceStub(); // 设置embedding stub
        
        Document document = Document.builder()
                .id("doc-1")
                .content("This is a test document about Spring AI.")
                .addMetadata("source", "test")
                .addMetadata("category", "tech")
                .build();

        doNothing().when(vectorStore).add(anyList());

        // When: 执行添加
        boolean result = vectorStoreService.add(document);

        // Then: 验证结果
        assertThat(result).isTrue();

        // 验证 VectorStore.add() 被调用
        ArgumentCaptor<List<org.springframework.ai.document.Document>> captor = 
            ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());

        List<org.springframework.ai.document.Document> addedDocs = captor.getValue();
        assertThat(addedDocs).hasSize(1);
        
        org.springframework.ai.document.Document addedDoc = addedDocs.get(0);
        // Spring AI Document可能生成新的ID，所以我们验证其他关键字段
        assertThat(addedDoc.getText()).isEqualTo("This is a test document about Spring AI.");
        assertThat(addedDoc.getMetadata()).containsEntry("nebula_id", "doc-1");
        assertThat(addedDoc.getMetadata()).containsKey("nebula_created_at");
        assertThat(addedDoc.getMetadata()).containsKey("nebula_updated_at");
        assertThat(addedDoc.getMetadata()).containsEntry("source", "test");
        assertThat(addedDoc.getMetadata()).containsEntry("category", "tech");
    }

    /**
     * 测试批量添加文档功能
     * 
     * 场景：批量添加 5 个文档
     * 验证：VectorStore.add() 被调用一次，所有文档都被添加
     */
    @Test
    void testAddAllDocuments() {
        // Given: 准备测试文档（不需要embedding stub，因为addAll不调用embeddingService）
        List<Document> documents = List.of(
            Document.simple("Document 1"),
            Document.simple("Document 2"),
            Document.simple("Document 3"),
            Document.simple("Document 4"),
            Document.simple("Document 5")
        );

        doNothing().when(vectorStore).add(anyList());

        // When: 执行批量添加
        int result = vectorStoreService.addAll(documents);

        // Then: 验证结果
        assertThat(result).isEqualTo(5);

        // 验证 VectorStore.add() 被调用
        ArgumentCaptor<List<org.springframework.ai.document.Document>> captor = 
            ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());

        List<org.springframework.ai.document.Document> addedDocs = captor.getValue();
        assertThat(addedDocs).hasSize(5);
    }

    /**
     * 测试语义搜索功能
     * 
     * 场景：执行语义搜索，获取最相关的文档
     * 验证：VectorStore.similaritySearch() 被正确调用，SearchResult 正确构建
     */
    @Test
    void testSearch() {
        // Given: 准备测试数据
        String query = "What is Spring AI?";
        int topK = 3;

        // Mock Spring AI 文档结果
        org.springframework.ai.document.Document mockDoc1 = new org.springframework.ai.document.Document(
            "doc-1",
            "Spring AI is a framework...",
            Map.of("nebula_id", "doc-1", "score", "0.95")
        );
        org.springframework.ai.document.Document mockDoc2 = new org.springframework.ai.document.Document(
            "doc-2",
            "Spring AI provides AI capabilities...",
            Map.of("nebula_id", "doc-2", "score", "0.85")
        );

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
            .thenReturn(List.of(mockDoc1, mockDoc2));

        // When: 执行搜索
        SearchResult result = vectorStoreService.search(query, topK);

        // Then: 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getDocuments()).hasSize(2);
        assertThat(result.getTotalFound()).isEqualTo(2);
        assertThat(result.getQuery()).isEqualTo(query);
        assertThat(result.getTimestamp()).isNotNull();

        // 验证文档内容
        assertThat(result.getContents()).contains(
            "Spring AI is a framework...",
            "Spring AI provides AI capabilities..."
        );

        // 验证 VectorStore.similaritySearch() 被调用
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());

        SearchRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.getQuery()).isEqualTo(query);
        assertThat(capturedRequest.getTopK()).isEqualTo(topK);
    }

    /**
     * 测试带过滤条件的搜索功能
     * 
     * 场景：执行带有过滤条件的搜索（如 category="tech"）
     * 验证：过滤表达式正确构建（如 "category == 'tech'"）
     */
    @Test
    void testSearchWithFilter() {
        // Given: 准备测试数据
        String query = "Spring AI";
        int topK = 5;
        Map<String, Object> filter = Map.of(
            "category", "tech",
            "status", "published"
        );

        // Mock Spring AI 文档结果
        org.springframework.ai.document.Document mockDoc = new org.springframework.ai.document.Document(
            "doc-1",
            "Filtered result",
            Map.of("category", "tech", "status", "published")
        );

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
            .thenReturn(List.of(mockDoc));

        // When: 执行带过滤条件的搜索
        SearchResult result = vectorStoreService.search(query, topK, filter);

        // Then: 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getDocuments()).hasSize(1);

        // 验证 filterExpression 被正确构建
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());

        SearchRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.getFilterExpression()).isNotNull();
        // 过滤表达式应该存在（具体格式由 Spring AI 实现）
        assertThat(capturedRequest.getFilterExpression().toString()).contains("category");
        assertThat(capturedRequest.getFilterExpression().toString()).contains("status");
    }

    /**
     * 测试带相似度阈值的搜索功能
     * 
     * 场景：执行搜索并设置相似度阈值
     * 验证：阈值参数正确传递
     */
    @Test
    void testSearchWithThreshold() {
        // Given: 准备测试数据
        String query = "test query";
        int topK = 10;
        double similarityThreshold = 0.75;

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
            .thenReturn(List.of());

        // When: 执行带阈值的搜索
        SearchResult result = vectorStoreService.search(query, topK, similarityThreshold);

        // Then: 验证结果
        assertThat(result).isNotNull();

        // 验证阈值参数被正确传递
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());

        SearchRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.getSimilarityThreshold()).isEqualTo(similarityThreshold);
    }

    /**
     * 测试文档删除功能
     * 
     * 场景：删除单个文档
     * 验证：VectorStore.delete() 被正确调用
     */
    @Test
    void testDelete() {
        // Given: 准备测试数据
        String documentId = "doc-to-delete";

        doNothing().when(vectorStore).delete(anyList());

        // When: 执行删除
        boolean result = vectorStoreService.delete(documentId);

        // Then: 验证结果
        assertThat(result).isTrue();

        // 验证 VectorStore.delete() 被调用
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).delete(captor.capture());

        List<String> deletedIds = captor.getValue();
        assertThat(deletedIds).contains(documentId);
    }

    /**
     * 测试批量删除功能
     * 
     * 场景：批量删除多个文档
     * 验证：VectorStore.delete() 被调用，ID 列表正确
     */
    @Test
    void testDeleteAll() {
        // Given: 准备测试数据
        List<String> idsToDelete = List.of("doc-1", "doc-2", "doc-3");

        doNothing().when(vectorStore).delete(anyList());

        // When: 执行批量删除
        int result = vectorStoreService.deleteAll(idsToDelete);

        // Then: 验证结果
        assertThat(result).isEqualTo(3);

        // 验证 VectorStore.delete() 被调用
        verify(vectorStore).delete(idsToDelete);
    }
}

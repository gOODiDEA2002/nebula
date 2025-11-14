package io.nebula.ai.spring.embedding;

import io.nebula.ai.core.model.EmbeddingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SpringAIEmbeddingService 单元测试
 * 
 * 测试目标：验证文本嵌入服务的核心功能
 * - 单文本向量化
 * - 批量文本向量化
 * - 相似度计算
 * 
 * @author Nebula Framework
 */
@ExtendWith(MockitoExtension.class)
class SpringAIEmbeddingServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    private SpringAIEmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new SpringAIEmbeddingService(embeddingModel);
    }

    /**
     * 测试单文本向量化功能
     * 
     * 场景：输入单条文本，获取向量化结果
     * 验证：返回 EmbeddingResponse，向量维度正确（1536）
     */
    @Test
    void testEmbedSingleText() {
        // Given: 准备测试数据
        String text = "Hello, this is a test text.";
        float[] mockVector = new float[1536];
        for (int i = 0; i < mockVector.length; i++) {
            mockVector[i] = (float) (Math.random() * 2 - 1); // 随机值 [-1, 1]
        }

        // Mock EmbeddingModel 响应
        org.springframework.ai.embedding.EmbeddingResponse mockSpringResponse = mock(org.springframework.ai.embedding.EmbeddingResponse.class);
        org.springframework.ai.embedding.Embedding mockEmbedding = mock(org.springframework.ai.embedding.Embedding.class);
        
        when(embeddingModel.embedForResponse(any(List.class))).thenReturn(mockSpringResponse);
        when(mockSpringResponse.getResults()).thenReturn(List.of(mockEmbedding));
        when(mockEmbedding.getOutput()).thenReturn(mockVector);

        // When: 执行向量化
        EmbeddingResponse response = embeddingService.embed(text);

        // Then: 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getEmbeddings()).hasSize(1);
        assertThat(response.getFirstVector()).hasSize(1536);
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getModel()).isNotNull();

        // 验证 EmbeddingModel 被调用
        verify(embeddingModel).embedForResponse(argThat(list -> list.size() == 1));
    }

    /**
     * 测试批量文本向量化功能
     * 
     * 场景：输入多条文本（如 10 条），获取批量向量化结果
     * 验证：每条文本都有对应的向量，索引和向量对应关系正确
     */
    @Test
    void testEmbedBatch() {
        // Given: 准备测试数据
        List<String> texts = List.of(
            "Text 1", "Text 2", "Text 3", "Text 4", "Text 5",
            "Text 6", "Text 7", "Text 8", "Text 9", "Text 10"
        );
        
        // 创建 10 个向量
        List<org.springframework.ai.embedding.Embedding> mockEmbeddings = texts.stream()
            .map(t -> {
                org.springframework.ai.embedding.Embedding mockEmbedding = mock(org.springframework.ai.embedding.Embedding.class);
                float[] mockVector = new float[1536];
                for (int i = 0; i < mockVector.length; i++) {
                    mockVector[i] = (float) Math.random();
                }
                when(mockEmbedding.getOutput()).thenReturn(mockVector);
                return mockEmbedding;
            })
            .toList();

        // Mock EmbeddingModel 响应
        org.springframework.ai.embedding.EmbeddingResponse mockSpringResponse = mock(org.springframework.ai.embedding.EmbeddingResponse.class);
        when(embeddingModel.embedForResponse(any(List.class))).thenReturn(mockSpringResponse);
        when(mockSpringResponse.getResults()).thenReturn(mockEmbeddings);

        // When: 执行批量向量化
        EmbeddingResponse response = embeddingService.embed(texts);

        // Then: 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getEmbeddings()).hasSize(10);
        assertThat(response.getAllVectors()).hasSize(10);
        
        // 验证每个向量的维度
        response.getAllVectors().forEach(vector -> {
            assertThat(vector).hasSize(1536);
        });

        // 验证 EmbeddingModel 被调用
        verify(embeddingModel).embedForResponse(argThat(list -> list.size() == 10));
    }

    /**
     * 测试余弦相似度计算功能
     * 
     * 场景：计算两个向量的相似度
     * 验证：
     * - 相同向量相似度 = 1.0
     * - 正交向量相似度 = 0.0
     */
    @Test
    void testCosineSimilarity() {
        // Given: 准备测试向量
        // 测试1: 相同向量，相似度应该为 1.0
        List<Double> vector1 = List.of(1.0, 2.0, 3.0, 4.0);
        List<Double> vector2 = List.of(1.0, 2.0, 3.0, 4.0);

        // When & Then: 测试相同向量
        double similarity1 = embeddingService.similarity(vector1, vector2);
        assertThat(similarity1).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.0001));

        // 测试2: 正交向量，相似度应该为 0.0
        List<Double> vectorA = List.of(1.0, 0.0, 0.0);
        List<Double> vectorB = List.of(0.0, 1.0, 0.0);

        double similarity2 = embeddingService.similarity(vectorA, vectorB);
        assertThat(similarity2).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.0001));

        // 测试3: 相反向量，相似度应该为 -1.0
        List<Double> vectorX = List.of(1.0, 2.0, 3.0);
        List<Double> vectorY = List.of(-1.0, -2.0, -3.0);

        double similarity3 = embeddingService.similarity(vectorX, vectorY);
        assertThat(similarity3).isCloseTo(-1.0, org.assertj.core.data.Offset.offset(0.0001));
    }

    /**
     * 测试相似度计算 - 维度不匹配异常
     * 
     * 场景：输入不同维度的向量
     * 验证：抛出异常
     */
    @Test
    void testSimilarityWithMismatchedDimensions() {
        // Given: 不同维度的向量
        List<Double> vector1 = List.of(1.0, 2.0, 3.0);
        List<Double> vector2 = List.of(1.0, 2.0);

        // When & Then: 验证抛出异常
        assertThatThrownBy(() -> embeddingService.similarity(vector1, vector2))
            .hasMessageContaining("向量维度不匹配");
    }

    /**
     * 测试服务元数据
     * 
     * 场景：获取服务的元数据信息
     * 验证：维度、模型、批量大小等信息正确
     */
    @Test
    void testServiceMetadata() {
        // When & Then: 验证服务元数据
        assertThat(embeddingService.getDimension()).isEqualTo(1536);
        assertThat(embeddingService.getCurrentModel()).isEqualTo("text-embedding-ada-002");
        assertThat(embeddingService.getMaxBatchSize()).isEqualTo(2048);
        assertThat(embeddingService.getSupportedModels()).contains(
            "text-embedding-ada-002",
            "text-embedding-3-small",
            "text-embedding-3-large"
        );
    }
}

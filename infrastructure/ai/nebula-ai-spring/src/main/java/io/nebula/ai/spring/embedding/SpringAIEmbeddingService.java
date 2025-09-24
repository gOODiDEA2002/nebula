package io.nebula.ai.spring.embedding;

import io.nebula.ai.core.embedding.EmbeddingService;
import io.nebula.ai.core.exception.EmbeddingException;
import io.nebula.ai.core.model.EmbeddingRequest;
import io.nebula.ai.core.model.EmbeddingResponse;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

/**
 * 基于Spring AI的嵌入服务实现
 */
@Service
public class SpringAIEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(SpringAIEmbeddingService.class);

    private final EmbeddingModel embeddingModel;

    @Autowired
    public SpringAIEmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public EmbeddingResponse embed(String text) {
        try {
            log.debug("对文本进行向量化: {}", text.substring(0, Math.min(text.length(), 50)) + "...");

            org.springframework.ai.embedding.EmbeddingResponse springResponse = 
                    embeddingModel.embedForResponse(List.of(text));

            return convertToNebulaResponse(springResponse);

        } catch (Exception e) {
            log.error("文本向量化失败: {}", e.getMessage(), e);
            throw new EmbeddingException("文本向量化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public EmbeddingResponse embed(List<String> texts) {
        try {
            log.debug("批量向量化文本: {} 条", texts.size());

            if (texts.size() > getMaxBatchSize()) {
                throw new EmbeddingException("批量大小超过限制: " + texts.size() + " > " + getMaxBatchSize());
            }

            org.springframework.ai.embedding.EmbeddingResponse springResponse = 
                    embeddingModel.embedForResponse(texts);

            return convertToNebulaResponse(springResponse);

        } catch (Exception e) {
            log.error("批量文本向量化失败: {}", e.getMessage(), e);
            throw new EmbeddingException("批量文本向量化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        try {
            log.debug("处理嵌入请求: {} 条文本", request.getTexts().size());

            // 构建Spring AI的嵌入选项
            EmbeddingOptions options = null;
            if (request.getOptions() != null && !request.getOptions().isEmpty()) {
                // 这里可以根据需要转换选项
                // Spring AI的EmbeddingOptions可能有不同的实现
            }

            org.springframework.ai.embedding.EmbeddingResponse springResponse = 
                    embeddingModel.embedForResponse(request.getTexts());

            return convertToNebulaResponse(springResponse);

        } catch (Exception e) {
            log.error("嵌入请求处理失败: {}", e.getMessage(), e);
            throw new EmbeddingException("嵌入请求处理失败: " + e.getMessage(), e);
        }
    }

    @Override
    public CompletableFuture<EmbeddingResponse> embedAsync(String text) {
        return CompletableFuture.supplyAsync(() -> embed(text));
    }

    @Override
    public CompletableFuture<EmbeddingResponse> embedAsync(List<String> texts) {
        return CompletableFuture.supplyAsync(() -> embed(texts));
    }

    @Override
    public CompletableFuture<EmbeddingResponse> embedAsync(EmbeddingRequest request) {
        return CompletableFuture.supplyAsync(() -> embed(request));
    }

    @Override
    public double similarity(List<Double> vector1, List<Double> vector2) {
        if (vector1.size() != vector2.size()) {
            throw new EmbeddingException("向量维度不匹配: " + vector1.size() + " != " + vector2.size());
        }

        // 计算余弦相似度
        return cosineSimilarity(vector1, vector2);
    }

    @Override
    public int getDimension() {
        // OpenAI text-embedding-ada-002 的默认维度
        // 实际应用中应该从模型配置中获取
        return 1536;
    }

    @Override
    public boolean isAvailable() {
        try {
            // 发送简单的测试文本进行向量化
            embed("test");
            return true;
        } catch (Exception e) {
            log.warn("嵌入服务不可用: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> getSupportedModels() {
        // Spring AI 目前没有标准的方法获取支持的模型列表
        // 这里返回常见的OpenAI嵌入模型
        return List.of(
                "text-embedding-ada-002",
                "text-embedding-3-small",
                "text-embedding-3-large"
        );
    }

    @Override
    public String getCurrentModel() {
        // 从Spring AI EmbeddingModel获取当前模型
        // 这是一个简化的实现
        return "text-embedding-ada-002"; // 默认值
    }

    @Override
    public int getMaxBatchSize() {
        // OpenAI 嵌入API的批量限制
        return 2048;
    }

    /**
     * 将Spring AI的EmbeddingResponse转换为Nebula的EmbeddingResponse
     */
    private EmbeddingResponse convertToNebulaResponse(org.springframework.ai.embedding.EmbeddingResponse springResponse) {
        List<EmbeddingResponse.Embedding> embeddings = IntStream.range(0, springResponse.getResults().size())
                .mapToObj(i -> {
                    org.springframework.ai.embedding.Embedding springEmbedding = springResponse.getResults().get(i);
                    float[] floatVector = springEmbedding.getOutput();
                    List<Double> vector = new java.util.ArrayList<>(floatVector.length);
                    for (float f : floatVector) {
                        vector.add((double) f);
                    }
                    
                    return new EmbeddingResponse.Embedding(i, vector, null);
                })
                .toList();

        EmbeddingResponse.Usage usage = null;
        if (springResponse.getMetadata() != null && springResponse.getMetadata().getUsage() != null) {
            usage = new EmbeddingResponse.Usage(
                    springResponse.getMetadata().getUsage().getPromptTokens(),
                    springResponse.getMetadata().getUsage().getTotalTokens()
            );
        }

        return EmbeddingResponse.builder()
                .embeddings(embeddings)
                .timestamp(LocalDateTime.now())
                .model(getCurrentModel())
                .usage(usage)
                .build();
    }

    /**
     * 计算余弦相似度
     */
    private double cosineSimilarity(List<Double> vectorA, List<Double> vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += Math.pow(vectorA.get(i), 2);
            normB += Math.pow(vectorB.get(i), 2);
        }
        
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}

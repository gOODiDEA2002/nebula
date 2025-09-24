package io.nebula.ai.core.embedding;

import io.nebula.ai.core.model.EmbeddingRequest;
import io.nebula.ai.core.model.EmbeddingResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 嵌入服务接口
 * 提供文本向量化的统一抽象
 */
public interface EmbeddingService {

    /**
     * 对单个文本进行向量化
     *
     * @param text 待向量化的文本
     * @return 向量化结果
     */
    EmbeddingResponse embed(String text);

    /**
     * 对多个文本进行批量向量化
     *
     * @param texts 待向量化的文本列表
     * @return 向量化结果
     */
    EmbeddingResponse embed(List<String> texts);

    /**
     * 处理嵌入请求
     *
     * @param request 嵌入请求
     * @return 向量化结果
     */
    EmbeddingResponse embed(EmbeddingRequest request);

    /**
     * 异步对单个文本进行向量化
     *
     * @param text 待向量化的文本
     * @return 异步向量化结果
     */
    CompletableFuture<EmbeddingResponse> embedAsync(String text);

    /**
     * 异步对多个文本进行批量向量化
     *
     * @param texts 待向量化的文本列表
     * @return 异步向量化结果
     */
    CompletableFuture<EmbeddingResponse> embedAsync(List<String> texts);

    /**
     * 异步处理嵌入请求
     *
     * @param request 嵌入请求
     * @return 异步向量化结果
     */
    CompletableFuture<EmbeddingResponse> embedAsync(EmbeddingRequest request);

    /**
     * 计算两个向量的相似度
     *
     * @param vector1 向量1
     * @param vector2 向量2
     * @return 相似度值 (0-1之间，1表示完全相似)
     */
    double similarity(List<Double> vector1, List<Double> vector2);

    /**
     * 获取嵌入向量的维度
     *
     * @return 向量维度
     */
    int getDimension();

    /**
     * 检查服务是否可用
     *
     * @return 是否可用
     */
    boolean isAvailable();

    /**
     * 获取支持的模型列表
     *
     * @return 模型列表
     */
    List<String> getSupportedModels();

    /**
     * 获取当前使用的模型
     *
     * @return 当前模型
     */
    String getCurrentModel();

    /**
     * 获取批处理的最大大小
     *
     * @return 最大批处理大小
     */
    int getMaxBatchSize();
}

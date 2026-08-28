package io.nebula.ai.rag.retriever;

import io.nebula.ai.core.model.SearchRequest;
import io.nebula.ai.core.model.SearchResult;
import io.nebula.ai.core.vectorstore.VectorStoreService;

import java.util.List;
import java.util.Map;

/**
 * 基于 {@link VectorStoreService} 的向量检索器
 * <p>
 * 依赖的是 Nebula 的向量存储抽象而不是任何具体后端，换 Chroma / Qdrant 对本类零影响。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class VectorStoreRetriever implements Retriever {

    /** 检索结果的 source 标注，融合时用来识别来源 */
    public static final String SOURCE = "vector";

    private final VectorStoreService vectorStoreService;
    private final String name;
    private final double weight;
    private final double similarityThreshold;

    public VectorStoreRetriever(VectorStoreService vectorStoreService, double weight,
                                double similarityThreshold) {
        this(vectorStoreService, "VectorStoreRetriever", weight, similarityThreshold);
    }

    public VectorStoreRetriever(VectorStoreService vectorStoreService, String name, double weight,
                                double similarityThreshold) {
        if (vectorStoreService == null) {
            throw new IllegalArgumentException("VectorStoreService 不能为空");
        }
        this.vectorStoreService = vectorStoreService;
        this.name = name;
        this.weight = weight;
        this.similarityThreshold = similarityThreshold;
    }

    @Override
    public List<RetrievalResult> retrieve(String query, int topK, Map<String, Object> filter) {
        SearchResult searchResult = vectorStoreService.search(SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .filter(filter)
                .build());

        if (searchResult == null || searchResult.isEmpty()) {
            return List.of();
        }
        return searchResult.getDocuments().stream()
                .map(document -> RetrievalResult.builder()
                        .id(document.getId())
                        .content(document.getContent())
                        .metadata(document.getMetadata())
                        .score(document.getScore())
                        .source(SOURCE)
                        .build())
                .toList();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getWeight() {
        return weight;
    }
}

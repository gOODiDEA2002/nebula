package io.nebula.ai.spring.vectorstore;

import io.nebula.ai.core.embedding.EmbeddingService;
import io.nebula.ai.core.vectorstore.VectorStoreService;
import io.nebula.ai.rag.index.IndexSink;
import io.nebula.ai.rag.index.IndexTargetFactory;
import io.nebula.ai.rag.index.VectorStoreIndexSink;
import io.nebula.ai.spring.config.VectorStoreProperties;
import io.qdrant.client.QdrantClient;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * 按物理集合名产出向量写目标（R3 §4.2，落 nebula-ai-spring 守 Y5）
 * <p>
 * {@code sinkFor(physical)}：用 {@link QdrantVectorStoreFactory} 按物理集合名构建 VectorStore
 * （id-mapping 装饰规则与 {@code AIAutoConfiguration} 同一套，避免新旧代际 ID 形态不一致），
 * 再包 {@code SpringAIVectorStoreService} 与 R2 的 {@link VectorStoreIndexSink}。
 * <p>
 * 集合的建由 {@code QdrantCollectionSwitcher.prepare} 负责，故此处 {@code initializeSchema=false}，
 * 避免以嵌入模型默认维度重复建集合。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class QdrantIndexTargetFactory implements IndexTargetFactory {

    /** 与 {@link VectorStoreIndexSink#NAME} 对齐 */
    public static final String NAME = VectorStoreIndexSink.NAME;

    private final QdrantClient client;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingService embeddingService;
    private final VectorStoreProperties vectorStoreProperties;
    private final boolean idMappingEnabled;
    private final String namespaceName;
    private final String originalDocIdField;

    public QdrantIndexTargetFactory(QdrantClient client, EmbeddingModel embeddingModel,
                                    EmbeddingService embeddingService,
                                    VectorStoreProperties vectorStoreProperties,
                                    boolean idMappingEnabled, String namespaceName,
                                    String originalDocIdField) {
        if (client == null) {
            throw new IllegalArgumentException("QdrantClient 不能为空");
        }
        if (embeddingModel == null) {
            throw new IllegalArgumentException("EmbeddingModel 不能为空");
        }
        if (embeddingService == null) {
            throw new IllegalArgumentException("EmbeddingService 不能为空");
        }
        this.client = client;
        this.embeddingModel = embeddingModel;
        this.embeddingService = embeddingService;
        this.vectorStoreProperties = vectorStoreProperties;
        this.idMappingEnabled = idMappingEnabled;
        this.namespaceName = namespaceName;
        this.originalDocIdField = originalDocIdField;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public IndexSink sinkFor(String physicalName) {
        VectorStore vectorStore = QdrantVectorStoreFactory.create(client, embeddingModel,
                physicalName, false, idMappingEnabled, namespaceName, originalDocIdField);
        VectorStoreService vectorStoreService =
                new SpringAIVectorStoreService(vectorStore, embeddingService, vectorStoreProperties);
        return new VectorStoreIndexSink(vectorStoreService);
    }
}

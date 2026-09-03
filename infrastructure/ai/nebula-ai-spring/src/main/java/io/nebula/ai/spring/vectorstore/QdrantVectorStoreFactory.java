package io.nebula.ai.spring.vectorstore;

import io.qdrant.client.QdrantClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;

/**
 * Qdrant {@link VectorStore} 构建与 id-mapping 装饰的<b>单一出处</b>（R3 §4.2）
 * <p>
 * 装饰规则（enabled 判定 + 命名空间校验 + 包 {@link QdrantIdMappingVectorStore}）此前散落在
 * {@code AIAutoConfiguration} 的装配方法里，R3 把它抽成本静态工厂，供 {@code AIAutoConfiguration}
 * 与 {@code QdrantIndexTargetFactory} 共同调用，消除重复分支 —— 避免新旧代际 ID 形态不一致。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public final class QdrantVectorStoreFactory {

    private static final Logger log = LoggerFactory.getLogger(QdrantVectorStoreFactory.class);

    private QdrantVectorStoreFactory() {
    }

    /**
     * 按集合名构建 {@link QdrantVectorStore}，并按 id-mapping 规则决定是否装饰
     *
     * @param client            Qdrant 客户端
     * @param embeddingModel    嵌入模型
     * @param collectionName    物理集合名
     * @param initializeSchema  是否让 VectorStore 自建集合（重灌路径由 CollectionSwitcher.prepare
     *                          负责建集合，此处应传 false，避免以嵌入模型默认维度重复建集合）
     * @param idMappingEnabled  是否启用点 ID 映射
     * @param namespaceName     映射命名空间名；{@code idMappingEnabled=true} 时必填
     * @param originalDocIdField 原始 docId 的 payload 字段名；空时用默认
     * @return 已按规则装饰（或未装饰）的 VectorStore
     */
    public static VectorStore create(QdrantClient client, EmbeddingModel embeddingModel,
                                     String collectionName, boolean initializeSchema,
                                     boolean idMappingEnabled, String namespaceName,
                                     String originalDocIdField) {
        VectorStore raw = QdrantVectorStore.builder(client, embeddingModel)
                .collectionName(collectionName)
                .initializeSchema(initializeSchema)
                .build();
        return decorateWithIdMapping(raw, idMappingEnabled, namespaceName, originalDocIdField);
    }

    /**
     * id-mapping 装饰规则的单一实现：{@code AIAutoConfiguration} 与重灌工厂共用
     * <p>
     * {@code enabled=false} 直接返回裸 VectorStore；启用时命名空间缺失即抛（错配会产生一个全新
     * 命名空间，表现为写入成功、全库检索永远为空，比启动不来难查得多）。
     *
     * @throws IllegalStateException 启用 id-mapping 却未配命名空间
     */
    public static VectorStore decorateWithIdMapping(VectorStore raw, boolean idMappingEnabled,
                                                    String namespaceName, String originalDocIdField) {
        if (!idMappingEnabled) {
            return raw;
        }
        // 命名空间缺失时直接失败：错配会产生一个全新的命名空间，
        // 表现为写入成功、全库检索永远为空，比启动不来难查得多
        if (namespaceName == null || namespaceName.isBlank()) {
            throw new IllegalStateException(
                    "nebula.ai.vector-store.qdrant.id-mapping.enabled=true 时必须配置 "
                            + "nebula.ai.vector-store.qdrant.id-mapping.namespace-name; "
                            + "命名空间决定全库点 ID, 缺失或改动会让已灌入的数据全部检索不到");
        }
        QdrantPointIdMapper pointIdMapper = new QdrantPointIdMapper(namespaceName);
        String field = (originalDocIdField == null || originalDocIdField.isBlank())
                ? QdrantPointIdMapper.DEFAULT_ORIGINAL_DOC_ID_FIELD : originalDocIdField;
        log.info("Qdrant 点 ID 映射已启用, namespaceName={}, namespace={}, payload 字段={}",
                pointIdMapper.getNamespaceName(), pointIdMapper.getNamespace(), field);
        return new QdrantIdMappingVectorStore(raw, pointIdMapper, field);
    }
}

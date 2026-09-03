package io.nebula.ai.rag.index;

/**
 * 按物理名产出写目标（R3 §3.2）
 * <p>
 * 重灌要写的是<b>尚未接管读流量的新物理目标</b>，而 R2 的两个 sink 分别绑定「容器里的
 * {@code VectorStoreService}」与「配置里的索引名」，都指向活动目标。故新增本中立工厂端口，
 * {@code ReindexPipeline} 用它拿到「指向新代际」的 sink。
 * <p>
 * <b>Y5：</b>向量侧实现落 {@code nebula-ai-spring}（按集合名构造 {@code VectorStore} 需要 Qdrant
 * 供应商知识与 id-mapping 装饰）；BM25 侧实现平凡，落本模块的 {@code SearchServiceIndexTargetFactory}。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public interface IndexTargetFactory {

    /**
     * 与 {@link CollectionSwitcher#name()} 对齐
     */
    String name();

    /**
     * 按物理名产出写目标
     */
    IndexSink sinkFor(String physicalName);
}

package io.nebula.ai.rag.index;

/**
 * 后端中立的「别名 → 物理目标」切换端口（R3 §3.1）
 * <p>
 * 蓝绿切换：逻辑名（别名）承载读流量，物理名承载写入。同一后端内的切换必须原子完成，
 * 跨后端切换由 {@code ReindexPipeline} 按顺序执行并在失败时回滚（R3 §4.3）。
 * <p>
 * <b>Y5/Y6：</b>{@code nebula-ai-rag} 只持本中立端口，供应商实现（Qdrant alias、ES alias）
 * 分别落 {@code nebula-ai-spring} 与本模块的 {@code SearchServiceCollectionSwitcher}
 * （后者只依赖中立的 {@code SearchService} 契约，不涉及供应商 SDK）。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public interface CollectionSwitcher {

    /**
     * 与 {@link IndexSink#name()} 对齐，用于把切换器与写目标配对
     */
    String name();

    /**
     * 物理名推导；默认 {@code <逻辑名>-g<代际>}，实现可覆盖以适配既有命名
     */
    default String physicalName(String logicalName, long generation) {
        return logicalName + "-g" + generation;
    }

    /**
     * 幂等准备物理目标（已存在即返回，不重建、不清空）
     */
    void prepare(String physicalName);

    /**
     * 物理目标是否存在
     */
    boolean exists(String physicalName);

    /**
     * 把逻辑名（别名）指向物理名；同一后端内必须原子完成，别名不存在时等同于首次建立
     */
    void switchTo(String logicalName, String physicalName);

    /**
     * 当前逻辑名指向的物理名；不存在返回 {@code null}（回滚与清理的安全校验依据）
     */
    String resolveCurrent(String logicalName);

    /**
     * 永久删除物理目标；实现必须拒绝删除仍被任何别名指向的目标
     */
    void drop(String physicalName);
}

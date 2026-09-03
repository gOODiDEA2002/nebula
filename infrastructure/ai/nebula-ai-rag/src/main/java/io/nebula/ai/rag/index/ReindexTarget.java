package io.nebula.ai.rag.index;

/**
 * 重灌的一个后端目标（R3 §3.3）：把切换器、写目标工厂与逻辑别名捆成一份
 * <p>
 * {@link CollectionSwitcher} 与 {@link IndexTargetFactory} 按 {@code name()} 配对，
 * {@code logicalName} 是该后端的别名（{@code reindex.vector-alias} / {@code reindex.search-alias}）。
 * {@code ReindexPipeline} 持有按 {@code switch-order} 排好序的目标列表，逐个切换、失败逆序回滚。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class ReindexTarget {

    private final CollectionSwitcher switcher;
    private final IndexTargetFactory factory;
    private final String logicalName;

    public ReindexTarget(CollectionSwitcher switcher, IndexTargetFactory factory, String logicalName) {
        if (switcher == null) {
            throw new IllegalArgumentException("CollectionSwitcher 不能为空");
        }
        if (factory == null) {
            throw new IllegalArgumentException("IndexTargetFactory 不能为空");
        }
        if (logicalName == null || logicalName.isBlank()) {
            throw new IllegalArgumentException("逻辑别名(logicalName)不能为空");
        }
        if (!switcher.name().equals(factory.name())) {
            throw new IllegalArgumentException("切换器与写目标工厂的 name 必须一致: switcher="
                    + switcher.name() + ", factory=" + factory.name());
        }
        this.switcher = switcher;
        this.factory = factory;
        this.logicalName = logicalName;
    }

    public CollectionSwitcher getSwitcher() {
        return switcher;
    }

    public IndexTargetFactory getFactory() {
        return factory;
    }

    public String getLogicalName() {
        return logicalName;
    }

    public String name() {
        return switcher.name();
    }
}

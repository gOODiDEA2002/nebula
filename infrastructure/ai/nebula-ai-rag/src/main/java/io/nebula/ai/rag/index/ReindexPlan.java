package io.nebula.ai.rag.index;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 重灌计划（R3 §3.3）：由 {@code ReindexPipeline} 内部产出，也可供业务预览
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class ReindexPlan {

    private final long fromGeneration;
    private final long toGeneration;
    /** switcher name → 新物理名 */
    private final Map<String, String> targets;

    public ReindexPlan(long fromGeneration, long toGeneration, Map<String, String> targets) {
        this.fromGeneration = fromGeneration;
        this.toGeneration = toGeneration;
        this.targets = new LinkedHashMap<>(targets);
    }

    public long getFromGeneration() {
        return fromGeneration;
    }

    public long getToGeneration() {
        return toGeneration;
    }

    public Map<String, String> getTargets() {
        return targets;
    }
}

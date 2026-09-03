package io.nebula.ai.rag.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 一次重灌运行的结果报告（R3 §3.3）
 * <p>
 * 风格对齐 {@link IndexRunReport}：{@link #toComparableSummary()} 便于留档比对。
 * {@code failureStage} 为 {@code null} 表示全程成功；否则取
 * {@code PREPARE / INDEX / SWITCH / ROLLBACK}。回滚亦失败时 {@code manualInterventionHint}
 * 给出人工干预指引。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class ReindexReport {

    /** 阶段常量 */
    public static final String STAGE_PREPARE = "PREPARE";
    public static final String STAGE_INDEX = "INDEX";
    public static final String STAGE_SWITCH = "SWITCH";
    public static final String STAGE_ROLLBACK = "ROLLBACK";

    private final long generation;
    private IndexRunReport indexRun;
    private final List<String> switched = new ArrayList<>();
    private final List<String> rolledBack = new ArrayList<>();
    private String failureStage;
    private String manualInterventionHint;

    public ReindexReport(long generation) {
        this.generation = generation;
    }

    public long getGeneration() {
        return generation;
    }

    public IndexRunReport getIndexRun() {
        return indexRun;
    }

    public void setIndexRun(IndexRunReport indexRun) {
        this.indexRun = indexRun;
    }

    public List<String> getSwitched() {
        return List.copyOf(switched);
    }

    public void addSwitched(String switcherName) {
        switched.add(switcherName);
    }

    public List<String> getRolledBack() {
        return List.copyOf(rolledBack);
    }

    public void addRolledBack(String switcherName) {
        rolledBack.add(switcherName);
    }

    public String getFailureStage() {
        return failureStage;
    }

    public void setFailureStage(String failureStage) {
        this.failureStage = failureStage;
    }

    public String getManualInterventionHint() {
        return manualInterventionHint;
    }

    /**
     * 追加一条人工干预指引（回滚失败时逐后端累加）
     */
    public void appendManualInterventionHint(String hint) {
        if (manualInterventionHint == null || manualInterventionHint.isBlank()) {
            manualInterventionHint = hint;
        } else {
            manualInterventionHint = manualInterventionHint + " | " + hint;
        }
    }

    /**
     * 是否全程成功（无失败阶段）
     */
    public boolean isSuccessful() {
        return failureStage == null;
    }

    public String toComparableSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT,
                "generation=%d stage=%s switched=%s rolledBack=%s",
                generation, failureStage == null ? "OK" : failureStage, switched, rolledBack));
        if (indexRun != null) {
            sb.append(" | indexRun[").append(indexRun.toComparableSummary()).append("]");
        }
        // 固定提示：重灌是停写窗口操作，活动代际的增量不会自动补进新代际（R3 §6、R3-D2）
        sb.append(" | note=重灌为停写窗口操作; 窗口内活动代际的增量不会自动补进新代际, 切换后需再跑一次增量对齐");
        if (manualInterventionHint != null && !manualInterventionHint.isBlank()) {
            sb.append(" | manualIntervention=").append(manualInterventionHint);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toComparableSummary();
    }
}

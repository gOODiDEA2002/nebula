package io.nebula.ai.rag.index;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 索引差分计划器
 * <p>
 * 对齐规则（详细设计 §2.2）：
 * <ul>
 *   <li>状态库没有 → toAdd；</li>
 *   <li>hash 变了，或任一必需 sink 未 DONE → toUpdate；</li>
 *   <li>hash 未变且全 sink DONE → 跳过；</li>
 *   <li>状态库有、快照没有 → toDelete（快照完整语义）。</li>
 * </ul>
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class IndexPlanner {

    /**
     * 计算差分计划
     *
     * @param snapshot          当前快照
     * @param states            状态库中该源的现有状态（docId → 状态）
     * @param requiredSinkNames 必需 sink 名集合；判断「全 sink DONE」用
     */
    public IndexPlan plan(List<SourceDocument> snapshot, Map<String, DocIndexState> states,
                          Set<String> requiredSinkNames) {
        List<SourceDocument> toAdd = new ArrayList<>();
        List<SourceDocument> toUpdate = new ArrayList<>();
        List<DocIndexState> toDelete = new ArrayList<>();

        Set<String> snapshotIds = new HashSet<>();
        for (SourceDocument doc : snapshot) {
            snapshotIds.add(doc.getId());
            DocIndexState state = states.get(doc.getId());
            if (state == null) {
                toAdd.add(doc);
            } else if (!Objects.equals(doc.getContentHash(), state.getContentHash())
                    || !allSinksDone(state, requiredSinkNames)) {
                toUpdate.add(doc);
            }
        }

        for (Map.Entry<String, DocIndexState> entry : states.entrySet()) {
            if (!snapshotIds.contains(entry.getKey())) {
                toDelete.add(entry.getValue());
            }
        }

        return new IndexPlan(toAdd, toUpdate, toDelete);
    }

    private boolean allSinksDone(DocIndexState state, Set<String> requiredSinkNames) {
        for (String sinkName : requiredSinkNames) {
            if (state.getSinkStatus().get(sinkName) != SinkStatus.DONE) {
                return false;
            }
        }
        return true;
    }
}

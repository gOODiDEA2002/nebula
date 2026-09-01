package io.nebula.ai.rag.index;

import java.util.List;

/**
 * 索引差分计划
 * <ul>
 *   <li>{@code toAdd}：状态库里没有的新文档；</li>
 *   <li>{@code toUpdate}：hash 变了或任一必需 sink 仍 PENDING 的文档（后者即半途中断的重入）；</li>
 *   <li>{@code toDelete}：状态库有、快照没有的文档（按快照的完整语义视为已删除）。</li>
 * </ul>
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class IndexPlan {

    private final List<SourceDocument> toAdd;
    private final List<SourceDocument> toUpdate;
    private final List<DocIndexState> toDelete;

    public IndexPlan(List<SourceDocument> toAdd, List<SourceDocument> toUpdate,
                     List<DocIndexState> toDelete) {
        this.toAdd = List.copyOf(toAdd);
        this.toUpdate = List.copyOf(toUpdate);
        this.toDelete = List.copyOf(toDelete);
    }

    public List<SourceDocument> getToAdd() {
        return toAdd;
    }

    public List<SourceDocument> getToUpdate() {
        return toUpdate;
    }

    public List<DocIndexState> getToDelete() {
        return toDelete;
    }
}

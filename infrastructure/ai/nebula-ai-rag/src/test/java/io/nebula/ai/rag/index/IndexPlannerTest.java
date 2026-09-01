package io.nebula.ai.rag.index;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 索引差分矩阵（P2，详细设计 §2.2）
 * <p>
 * 覆盖：新增 / hash 变更更新 / 单 sink PENDING 重入 / hash 未变全 DONE 跳过 / 删除对齐。
 */
class IndexPlannerTest {

    private static final Set<String> REQUIRED = Set.of("vector-store", "search-service");

    private final IndexPlanner planner = new IndexPlanner();

    @Test
    void newDocument_goesToAdd() {
        SourceDocument doc = doc("d1", "hash-1");
        IndexPlan plan = planner.plan(List.of(doc), new LinkedHashMap<>(), REQUIRED);

        assertThat(plan.getToAdd()).extracting(SourceDocument::getId).containsExactly("d1");
        assertThat(plan.getToUpdate()).isEmpty();
        assertThat(plan.getToDelete()).isEmpty();
    }

    @Test
    void changedHash_goesToUpdate() {
        Map<String, DocIndexState> states = states(state("d1", "old-hash", REQUIRED));
        IndexPlan plan = planner.plan(List.of(doc("d1", "new-hash")), states, REQUIRED);

        assertThat(plan.getToUpdate()).extracting(SourceDocument::getId).containsExactly("d1");
        assertThat(plan.getToAdd()).isEmpty();
        assertThat(plan.getToDelete()).isEmpty();
    }

    @Test
    void sameHashOneSinkPending_goesToUpdateForReentry() {
        DocIndexState state = new DocIndexState("d1");
        state.setContentHash("hash-1");
        Map<String, SinkStatus> sinkStatus = new LinkedHashMap<>();
        sinkStatus.put("vector-store", SinkStatus.DONE);
        sinkStatus.put("search-service", SinkStatus.PENDING);   // 半途中断
        state.setSinkStatus(sinkStatus);

        IndexPlan plan = planner.plan(List.of(doc("d1", "hash-1")), states(state), REQUIRED);

        assertThat(plan.getToUpdate()).extracting(SourceDocument::getId).containsExactly("d1");
    }

    @Test
    void missingRequiredSink_goesToUpdate() {
        // 状态里只记了一个 sink，另一个必需 sink 从未写过（等价 PENDING）
        DocIndexState state = new DocIndexState("d1");
        state.setContentHash("hash-1");
        Map<String, SinkStatus> sinkStatus = new LinkedHashMap<>();
        sinkStatus.put("vector-store", SinkStatus.DONE);
        state.setSinkStatus(sinkStatus);

        IndexPlan plan = planner.plan(List.of(doc("d1", "hash-1")), states(state), REQUIRED);

        assertThat(plan.getToUpdate()).extracting(SourceDocument::getId).containsExactly("d1");
    }

    @Test
    void sameHashAllSinksDone_isSkipped() {
        IndexPlan plan = planner.plan(List.of(doc("d1", "hash-1")),
                states(state("d1", "hash-1", REQUIRED)), REQUIRED);

        assertThat(plan.getToAdd()).isEmpty();
        assertThat(plan.getToUpdate()).isEmpty();
        assertThat(plan.getToDelete()).isEmpty();
    }

    @Test
    void inStateButNotInSnapshot_goesToDelete() {
        Map<String, DocIndexState> states = states(
                state("d1", "hash-1", REQUIRED), state("gone", "hash-x", REQUIRED));

        IndexPlan plan = planner.plan(List.of(doc("d1", "hash-1")), states, REQUIRED);

        assertThat(plan.getToDelete()).extracting(DocIndexState::getDocId).containsExactly("gone");
        assertThat(plan.getToAdd()).isEmpty();
        assertThat(plan.getToUpdate()).isEmpty();
    }

    private static SourceDocument doc(String id, String hash) {
        SourceDocument doc = new SourceDocument(id, "content of " + id, "markdown");
        doc.setContentHash(hash);
        return doc;
    }

    private static DocIndexState state(String id, String hash, Set<String> doneSinks) {
        DocIndexState state = new DocIndexState(id);
        state.setContentHash(hash);
        Map<String, SinkStatus> sinkStatus = new LinkedHashMap<>();
        for (String sink : doneSinks) {
            sinkStatus.put(sink, SinkStatus.DONE);
        }
        state.setSinkStatus(sinkStatus);
        return state;
    }

    private static Map<String, DocIndexState> states(DocIndexState... states) {
        Map<String, DocIndexState> map = new LinkedHashMap<>();
        for (DocIndexState state : states) {
            map.put(state.getDocId(), state);
        }
        return map;
    }
}

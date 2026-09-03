package io.nebula.ai.rag.index;

import io.nebula.ai.rag.chunking.DocumentChunk;
import io.nebula.ai.rag.chunking.parse.MarkdownStructureParser;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 重灌管线全路径（R3 §3.3、§4.3）
 * <p>
 * 成功路径（prepare→灌→切→清理）；灌入有失败则不切换；切换中途失败触发回滚且 active 不推进；
 * 回滚亦失败给出人工干预提示；building != 0 时续跑同代际不新开。
 */
class ReindexPipelineTest {

    private static final MarkdownStructureParser MARKDOWN = new MarkdownStructureParser();

    @Test
    void successPath_advancesActiveAndCleansOldGenerations() {
        InMemoryIndexStateRepository repo = new InMemoryIndexStateRepository();
        // 种子: active=2, 两代旧物理目标存在, 别名指向 g2
        seedGeneration(repo, "src", 2, 0);
        FakeSwitcher search = new FakeSwitcher(SearchServiceIndexSink.NAME);
        FakeSwitcher vector = new FakeSwitcher("vector-store");
        for (FakeSwitcher s : List.of(search, vector)) {
            s.existing.add(s.physical(1));
            s.existing.add(s.physical(2));
            s.current = s.physical(2);
        }

        ReindexReport report = pipeline(repo, keep(0), search, vector).reindex();

        assertThat(report.isSuccessful()).isTrue();
        assertThat(report.getGeneration()).isEqualTo(3);
        assertThat(report.getSwitched())
                .containsExactly(SearchServiceIndexSink.NAME, "vector-store");
        // 别名指向新代际
        assertThat(search.current).isEqualTo("search-service-alias-g3");
        assertThat(vector.current).isEqualTo("vector-store-alias-g3");
        // keep=0 → 清理 g1、g2(均非活动)
        assertThat(search.existing).containsExactly("search-service-alias-g3");
        // 代际状态推进
        IndexGenerationState state = pipeline(repo, keep(0), search, vector)
                .loadGenerationState("src");
        assertThat(state.getActiveGeneration()).isEqualTo(3);
        assertThat(state.getBuildingGeneration()).isZero();
    }

    @Test
    void ingestFailure_doesNotSwitch_staysBuilding() {
        InMemoryIndexStateRepository repo = new InMemoryIndexStateRepository();
        FakeSwitcher search = new FakeSwitcher(SearchServiceIndexSink.NAME);
        FakeSwitcher vector = new FakeSwitcher("vector-store");
        // vector 工厂产出的 sink 永远 upsert 失败 → 灌入 failed>0
        ReindexPipeline pipeline = new ReindexPipeline(source("src"),
                List.of(target(search, always -> new RecordingSink(SearchServiceIndexSink.NAME)),
                        target(vector, p -> new AlwaysFailingSink("vector-store"))),
                repo, new IndexPlanner(), List.of(MARKDOWN), null, 2);

        ReindexReport report = pipeline.reindex();

        assertThat(report.isSuccessful()).isFalse();
        assertThat(report.getFailureStage()).isEqualTo(ReindexReport.STAGE_INDEX);
        assertThat(report.getSwitched()).isEmpty();
        // 从未切换
        assertThat(search.switchCalls).isZero();
        assertThat(vector.switchCalls).isZero();
        // building 已持久化(可续跑), active 未推进
        IndexGenerationState state = pipeline.loadGenerationState("src");
        assertThat(state.getActiveGeneration()).isZero();
        assertThat(state.getBuildingGeneration()).isEqualTo(1);
    }

    @Test
    void switchFailure_rollsBack_activeNotAdvanced() {
        InMemoryIndexStateRepository repo = new InMemoryIndexStateRepository();
        seedGeneration(repo, "src", 1, 0);
        FakeSwitcher search = new FakeSwitcher(SearchServiceIndexSink.NAME);
        FakeSwitcher vector = new FakeSwitcher("vector-store");
        search.current = search.physical(1);
        vector.current = vector.physical(1);
        vector.failSwitchOnCall = 1;   // 第二个后端前向切换即失败

        ReindexReport report = pipeline(repo, keep(2), search, vector).reindex();

        assertThat(report.getFailureStage()).isEqualTo(ReindexReport.STAGE_SWITCH);
        // search 已前向切换, 触发回滚指回 g1
        assertThat(report.getRolledBack()).containsExactly(SearchServiceIndexSink.NAME);
        assertThat(search.current).isEqualTo("search-service-alias-g1");
        assertThat(report.getManualInterventionHint()).isNull();
        // active 不推进(仍 1), building 仍为 2(未清)
        IndexGenerationState state = pipeline(repo, keep(2), search, vector)
                .loadGenerationState("src");
        assertThat(state.getActiveGeneration()).isEqualTo(1);
        assertThat(state.getBuildingGeneration()).isEqualTo(2);
    }

    @Test
    void rollbackFailure_yieldsManualInterventionHint() {
        InMemoryIndexStateRepository repo = new InMemoryIndexStateRepository();
        seedGeneration(repo, "src", 1, 0);
        FakeSwitcher search = new FakeSwitcher(SearchServiceIndexSink.NAME);
        FakeSwitcher vector = new FakeSwitcher("vector-store");
        search.current = search.physical(1);
        vector.current = vector.physical(1);
        search.failSwitchOnCall = 2;   // 前向切换成功, 回滚(第二次 switchTo)失败
        vector.failSwitchOnCall = 1;   // 第二个后端前向失败, 触发回滚

        ReindexReport report = pipeline(repo, keep(2), search, vector).reindex();

        assertThat(report.getFailureStage()).isEqualTo(ReindexReport.STAGE_ROLLBACK);
        assertThat(report.getManualInterventionHint())
                .contains("search-service-alias-g1")
                .contains("指回");
        // active 不推进
        assertThat(pipeline(repo, keep(2), search, vector).loadGenerationState("src")
                .getActiveGeneration()).isEqualTo(1);
    }

    @Test
    void buildingNonZero_resumesSameGeneration() {
        InMemoryIndexStateRepository repo = new InMemoryIndexStateRepository();
        // active=1, building=3(上次重灌到 g3 未完成)
        seedGeneration(repo, "src", 1, 3);
        FakeSwitcher search = new FakeSwitcher(SearchServiceIndexSink.NAME);
        FakeSwitcher vector = new FakeSwitcher("vector-store");
        search.current = search.physical(1);
        vector.current = vector.physical(1);

        ReindexReport report = pipeline(repo, keep(2), search, vector).reindex();

        // 续跑 g3, 不新开 g4
        assertThat(report.getGeneration()).isEqualTo(3);
        assertThat(report.isSuccessful()).isTrue();
        IndexGenerationState state = pipeline(repo, keep(2), search, vector)
                .loadGenerationState("src");
        assertThat(state.getActiveGeneration()).isEqualTo(3);
        assertThat(state.getBuildingGeneration()).isZero();
    }

    // ------------------------------------------------------------------

    private static void seedGeneration(InMemoryIndexStateRepository repo, String source,
                                       long active, long building) {
        DocIndexState encoded = new DocIndexState(source);
        encoded.setGeneration(active);
        encoded.setContentHash(Long.toString(building));
        repo.save(source + ReindexPipeline.GENERATION_PARTITION_SUFFIX, encoded);
    }

    private static int keep(int n) {
        return n;
    }

    private ReindexPipeline pipeline(InMemoryIndexStateRepository repo, int keepGenerations,
                                     FakeSwitcher... switchers) {
        List<ReindexTarget> targets = new ArrayList<>();
        for (FakeSwitcher s : switchers) {
            targets.add(target(s, p -> new RecordingSink(s.name)));
        }
        return new ReindexPipeline(source("src"), targets, repo, new IndexPlanner(),
                List.of(MARKDOWN), null, keepGenerations);
    }

    private static ReindexTarget target(FakeSwitcher switcher, SinkFactory sinkFactory) {
        IndexTargetFactory factory = new IndexTargetFactory() {
            @Override
            public String name() {
                return switcher.name;
            }

            @Override
            public IndexSink sinkFor(String physicalName) {
                return sinkFactory.create(physicalName);
            }
        };
        return new ReindexTarget(switcher, factory, switcher.name + "-alias");
    }

    private static DocumentSource source(String name) {
        return new DocumentSource() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public List<SourceDocument> snapshot() {
                return List.of(new SourceDocument("d1", "# 标题\n\n正文", "markdown"));
            }
        };
    }

    @FunctionalInterface
    private interface SinkFactory {
        IndexSink create(String physicalName);
    }

    /** 内存切换器：单逻辑别名, 记录 prepare/switch/drop, 可配置在第 N 次 switchTo 抛出 */
    private static final class FakeSwitcher implements CollectionSwitcher {

        private final String name;
        private final Set<String> existing = new LinkedHashSet<>();
        private String current;             // 别名当前指向的物理名
        private int switchCalls;
        private int failSwitchOnCall;        // 0 = 从不失败

        FakeSwitcher(String name) {
            this.name = name;
        }

        String physical(long generation) {
            return name + "-alias-g" + generation;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void prepare(String physicalName) {
            existing.add(physicalName);
        }

        @Override
        public boolean exists(String physicalName) {
            return existing.contains(physicalName);
        }

        @Override
        public void switchTo(String logicalName, String physicalName) {
            switchCalls++;
            if (failSwitchOnCall != 0 && switchCalls == failSwitchOnCall) {
                throw new IllegalStateException("模拟切换失败: " + name + " -> " + physicalName);
            }
            current = physicalName;
        }

        @Override
        public String resolveCurrent(String logicalName) {
            return current;
        }

        @Override
        public void drop(String physicalName) {
            if (physicalName.equals(current)) {
                throw new IllegalStateException("拒绝删除活动目标: " + physicalName);
            }
            existing.remove(physicalName);
        }
    }

    /** 记录 upsert 的 sink 桩 */
    private static class RecordingSink implements IndexSink {

        private final String name;
        private final List<String> upserted = new CopyOnWriteArrayList<>();

        RecordingSink(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void upsert(String docId, List<DocumentChunk> chunks) {
            upserted.add(docId);
        }

        @Override
        public void delete(String docId, List<String> chunkIds) {
        }
    }

    /** upsert 永远抛出的 sink 桩 */
    private static final class AlwaysFailingSink extends RecordingSink {

        AlwaysFailingSink(String name) {
            super(name);
        }

        @Override
        public void upsert(String docId, List<DocumentChunk> chunks) {
            throw new IndexSinkException(name(), docId, List.of(), "模拟灌入失败", null);
        }
    }
}

package io.nebula.ai.rag.index;

import io.nebula.ai.rag.chunking.pack.PackOptions;
import io.nebula.ai.rag.chunking.parse.StructureParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 版本化重灌与蓝绿切换管线（R3 §3.3、§4.3）
 * <p>
 * 六步执行（每步失败即停并进入回滚判定）：
 * <ol>
 *   <li>读代际状态，{@code toGeneration = max(active, building) + 1}；{@code building != 0}
 *       说明上次重灌未完成，<b>沿用该代际续跑</b>（可重入）；</li>
 *   <li>逐 switcher {@code prepare(physicalName)}，写 {@code buildingGeneration}；</li>
 *   <li>用 {@link IndexTargetFactory} 产出的 sink 组一条临时 {@link IndexingPipeline}，以分区键
 *       {@code <source>@g<代际>} 跑全量（复用 R2 全部差分/续跑/幂等逻辑，不复制实现）；</li>
 *   <li>灌入零失败才切换；有失败则停在 building 态，下次重跑续跑；</li>
 *   <li>按 {@code switch-order} 顺序逐 switcher {@code switchTo}；任一失败即逆序回滚已切换的后端，
 *       {@code activeGeneration} 不推进（§4.3）；</li>
 *   <li>全切成功 → 推进 {@code activeGeneration}、清零 {@code buildingGeneration}；按
 *       {@code keep-generations} 清理更旧代际（{@code drop} 前用 {@code resolveCurrent} 校验未被指向）。</li>
 * </ol>
 * <b>跨后端不可原子（J9 的诚实结论）：</b>向量集合与 BM25 索引分属两个独立后端，只能各自原子 +
 * 顺序执行 + 失败回滚。两次切换之间存在毫秒级「跨代际混读」窗口，无法消除，只能缩短（§4.3）。
 * <p>
 * <b>非目标：</b>不做重灌期间的读写并发协调（停写窗口操作，R3-D2）。窗口内活动代际的增量不会
 * 自动补进新代际，切换后需再跑一次增量对齐；该约束固定写入 {@link ReindexReport} 摘要。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class ReindexPipeline {

    private static final Logger log = LoggerFactory.getLogger(ReindexPipeline.class);

    /** 代际状态的分区键后缀 */
    static final String GENERATION_PARTITION_SUFFIX = "#generation";
    /** 重灌灌入的分区键中缀 */
    static final String INGEST_PARTITION_INFIX = "@g";

    private final DocumentSource source;
    private final List<ReindexTarget> targets;
    private final IndexStateRepository stateRepository;
    private final IndexPlanner planner;
    private final List<StructureParser> parsers;
    private final PackOptions packOptions;
    private final int keepGenerations;

    /**
     * @param source          文档源（全量快照）
     * @param targets         按 {@code switch-order} 排好序的后端目标（切换器 + 写目标工厂 + 别名）
     * @param stateRepository 状态库（承载代际状态与灌入状态）
     * @param planner         差分计划器
     * @param parsers         结构解析器
     * @param packOptions     装箱选项（管线内部强制确定性块 ID）
     * @param keepGenerations 保留的历史代际数；0 = 切换后立即清理
     */
    public ReindexPipeline(DocumentSource source, List<ReindexTarget> targets,
                           IndexStateRepository stateRepository, IndexPlanner planner,
                           List<StructureParser> parsers, PackOptions packOptions,
                           int keepGenerations) {
        if (source == null) {
            throw new IllegalArgumentException("DocumentSource 不能为空");
        }
        if (targets == null || targets.isEmpty()) {
            throw new IllegalArgumentException("至少需要一个 ReindexTarget");
        }
        if (stateRepository == null) {
            throw new IllegalArgumentException("IndexStateRepository 不能为空");
        }
        if (planner == null) {
            throw new IllegalArgumentException("IndexPlanner 不能为空");
        }
        if (parsers == null || parsers.isEmpty()) {
            throw new IllegalArgumentException("至少需要一个 StructureParser");
        }
        this.source = source;
        this.targets = List.copyOf(targets);
        this.stateRepository = stateRepository;
        this.planner = planner;
        this.parsers = List.copyOf(parsers);
        this.packOptions = packOptions;
        this.keepGenerations = Math.max(0, keepGenerations);
    }

    /**
     * 跑一次版本化重灌
     */
    public ReindexReport reindex() {
        IndexGenerationState state = loadGenerationState(source.name());
        long from = state.getActiveGeneration();
        long to = state.getBuildingGeneration() != 0
                ? state.getBuildingGeneration()               // 续跑上次未完成的代际
                : Math.max(state.getActiveGeneration(), state.getBuildingGeneration()) + 1;

        ReindexPlan plan = buildPlan(from, to);
        ReindexReport report = new ReindexReport(to);

        // 尽早持久化 building：即便后续崩溃, 重入也沿用同一代际(prepare 幂等), 不新开代际
        state.setBuildingGeneration(to);
        saveGenerationState(state);

        // Step 2: 逐 switcher 幂等准备物理目标
        try {
            for (ReindexTarget target : targets) {
                target.getSwitcher().prepare(plan.getTargets().get(target.name()));
            }
        } catch (Exception e) {
            report.setFailureStage(ReindexReport.STAGE_PREPARE);
            log.error("重灌 prepare 阶段失败(代际 g{}): {}", to, e.getMessage(), e);
            return report;
        }

        // Step 3: 以 <source>@g<代际> 分区跑全量灌入(复用 R2 的 IndexingPipeline)
        IndexRunReport indexRun = runIngest(plan);
        report.setIndexRun(indexRun);

        // Step 4: 灌入零失败才进入切换
        if (indexRun.getFailed() != 0) {
            report.setFailureStage(ReindexReport.STAGE_INDEX);
            log.warn("重灌灌入阶段有失败(代际 g{}, failed={}), 停在 building 态, 下次重跑续跑",
                    to, indexRun.getFailed());
            return report;
        }

        // Step 5: 按 switch-order 逐后端切换; 记录旧物理名以备回滚
        List<ReindexTarget> switched = new ArrayList<>();
        Map<ReindexTarget, String> oldTargets = new LinkedHashMap<>();
        try {
            for (ReindexTarget target : targets) {
                String physical = plan.getTargets().get(target.name());
                oldTargets.put(target, target.getSwitcher().resolveCurrent(target.getLogicalName()));
                target.getSwitcher().switchTo(target.getLogicalName(), physical);
                switched.add(target);
                report.addSwitched(target.name());
            }
        } catch (Exception e) {
            report.setFailureStage(ReindexReport.STAGE_SWITCH);
            log.error("重灌切换阶段失败(代际 g{}): {}; 开始回滚已切换的 {} 个后端",
                    to, e.getMessage(), switched.stream().map(ReindexTarget::name).toList(), e);
            rollback(switched, oldTargets, to, report);
            return report;   // activeGeneration 不推进
        }

        // Step 6: 全切成功 → 推进活动代际、清零 building、按保留策略清理
        state.setActiveGeneration(to);
        state.setBuildingGeneration(0);
        saveGenerationState(state);
        cleanupOldGenerations(to, report);

        log.info("重灌完成: {}", report.toComparableSummary());
        return report;
    }

    // ------------------------------------------------------------------
    // 计划与灌入
    // ------------------------------------------------------------------

    private ReindexPlan buildPlan(long from, long to) {
        Map<String, String> physicalTargets = new LinkedHashMap<>();
        for (ReindexTarget target : targets) {
            physicalTargets.put(target.name(),
                    target.getSwitcher().physicalName(target.getLogicalName(), to));
        }
        return new ReindexPlan(from, to, physicalTargets);
    }

    /**
     * 组一条临时 {@link IndexingPipeline}，写目标全部指向新代际的物理名，分区键
     * {@code <source>@g<代际>} —— 与活动代际的差分基线天然隔离，重灌失败不污染活动代际。
     */
    private IndexRunReport runIngest(ReindexPlan plan) {
        List<IndexSink> sinks = new ArrayList<>(targets.size());
        for (ReindexTarget target : targets) {
            sinks.add(target.getFactory().sinkFor(plan.getTargets().get(target.name())));
        }
        DocumentSource generationSource = new GenerationScopedSource(source, plan.getToGeneration());
        IndexingPipeline pipeline = new IndexingPipeline(parsers, packOptions, sinks,
                stateRepository, planner);
        return pipeline.run(generationSource);
    }

    // ------------------------------------------------------------------
    // 回滚与清理
    // ------------------------------------------------------------------

    /**
     * 逆序回滚已切换的后端；回滚亦失败时给出人工干预指引并 error 级日志（不再自动重试）
     */
    private void rollback(List<ReindexTarget> switched, Map<ReindexTarget, String> oldTargets,
                          long to, ReindexReport report) {
        for (int i = switched.size() - 1; i >= 0; i--) {
            ReindexTarget target = switched.get(i);
            String old = oldTargets.get(target);
            String logical = target.getLogicalName();
            if (old == null) {
                // 首次重灌无旧物理名可回退: 别名现指向新代际, 无法还原到「先前无别名」的状态
                report.appendManualInterventionHint("后端 " + target.name() + " 别名 " + logical
                        + " 现指向 g" + to + ", 但首次重灌无旧物理名可回退, 需人工确认是否保留或删除");
                log.error("回滚: 后端 {} 别名 {} 无旧物理名可回退(首次重灌)", target.name(), logical);
                continue;
            }
            try {
                target.getSwitcher().switchTo(logical, old);
                report.addRolledBack(target.name());
            } catch (Exception e) {
                report.setFailureStage(ReindexReport.STAGE_ROLLBACK);
                report.appendManualInterventionHint("后端 " + target.name() + " 别名 " + logical
                        + " 现指向 g" + to + ", 应指回 " + old + "; 回滚失败: " + e.getMessage());
                log.error("回滚失败: 后端 {} 别名 {} 应指回 {}, 但回滚抛出: {}",
                        target.name(), logical, old, e.getMessage(), e);
            }
        }
    }

    /**
     * 按 {@code keep-generations} 清理更旧代际：保留 {@code [to-keep, to]}，清理 {@code [1, to-keep-1]}。
     * {@code drop} 前由切换器自身用 {@code resolveCurrent}/别名反查拒绝删除活动目标；此处清理失败
     * 不使整次重灌失败（切换已成功），仅记入人工干预提示。
     */
    private void cleanupOldGenerations(long to, ReindexReport report) {
        long dropUpTo = to - keepGenerations - 1;
        for (long gen = 1; gen <= dropUpTo; gen++) {
            for (ReindexTarget target : targets) {
                String physical = target.getSwitcher().physicalName(target.getLogicalName(), gen);
                try {
                    if (target.getSwitcher().exists(physical)) {
                        target.getSwitcher().drop(physical);
                    }
                } catch (Exception e) {
                    report.appendManualInterventionHint("清理旧代际 g" + gen + " 的后端 "
                            + target.name() + " 物理目标 " + physical + " 失败: " + e.getMessage());
                    log.warn("清理旧代际失败: 后端 {} 物理目标 {}: {}",
                            target.name(), physical, e.getMessage());
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // 代际状态编解码（复用 IndexStateRepository，不改接口，守 Y1）
    // ------------------------------------------------------------------

    IndexGenerationState loadGenerationState(String sourceName) {
        Map<String, DocIndexState> partition =
                stateRepository.load(sourceName + GENERATION_PARTITION_SUFFIX);
        DocIndexState encoded = partition.get(sourceName);
        if (encoded == null) {
            return new IndexGenerationState(sourceName);
        }
        IndexGenerationState state = new IndexGenerationState(sourceName);
        state.setActiveGeneration(encoded.getGeneration());
        // buildingGeneration 编码在 contentHash（避免占用 DocIndexState.generation 的既有语义）
        state.setBuildingGeneration(parseLong(encoded.getContentHash()));
        state.setSchemaVersion(encoded.getSchemaVersion());
        return state;
    }

    void saveGenerationState(IndexGenerationState state) {
        DocIndexState encoded = new DocIndexState(state.getSourceName());
        encoded.setGeneration(state.getActiveGeneration());
        encoded.setContentHash(Long.toString(state.getBuildingGeneration()));
        encoded.setSchemaVersion(state.getSchemaVersion());
        stateRepository.save(state.getSourceName() + GENERATION_PARTITION_SUFFIX, encoded);
    }

    private static long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 把源包成「代际作用域」：{@code name()} 变为 {@code <source>@g<代际>}，{@code snapshot()} 透传。
     * 让 R2 的 {@link IndexingPipeline} 把灌入状态写进隔离分区，不污染活动代际的差分基线。
     */
    private static final class GenerationScopedSource implements DocumentSource {

        private final DocumentSource delegate;
        private final String scopedName;

        GenerationScopedSource(DocumentSource delegate, long generation) {
            this.delegate = delegate;
            this.scopedName = delegate.name() + INGEST_PARTITION_INFIX + generation;
        }

        @Override
        public String name() {
            return scopedName;
        }

        @Override
        public List<SourceDocument> snapshot() {
            return delegate.snapshot();
        }
    }
}

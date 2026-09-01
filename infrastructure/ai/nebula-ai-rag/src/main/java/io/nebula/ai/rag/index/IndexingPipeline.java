package io.nebula.ai.rag.index;

import io.nebula.ai.rag.chunking.DocumentChunk;
import io.nebula.ai.rag.chunking.pack.ChunkIdStrategy;
import io.nebula.ai.rag.chunking.pack.ChunkPacker;
import io.nebula.ai.rag.chunking.pack.PackOptions;
import io.nebula.ai.rag.chunking.parse.DocElement;
import io.nebula.ai.rag.chunking.parse.StructureParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 增量索引管线（P2-min，详细设计 §2.2）
 * <p>
 * {@link #run(DocumentSource)} 每文档五步：解析（按 format 选 {@link StructureParser}）→
 * 装箱（<b>强制确定性块 ID</b>，忽略传入的随机策略并 warn）→ 逐 sink「先 delete 旧块再 upsert
 * 新块」，成功即置该 sink DONE 并<b>逐 sink 保存状态</b> → 全 sink DONE 后更新 hash 与 chunkIds →
 * 失败文档记入 {@link IndexRunReport} 继续下一文档。
 * <p>
 * <b>可重入：</b>半途中断后重跑，PENDING 的文档自然落入 {@code toUpdate}；确定性 ID + 逐 sink
 * 先删后写保证「同一份文档重复索引 = 覆盖」，不产生孤儿块。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class IndexingPipeline {

    private static final Logger log = LoggerFactory.getLogger(IndexingPipeline.class);

    private final Map<String, StructureParser> parsersByFormat = new LinkedHashMap<>();
    private final PackOptions effectiveOptions;
    private final List<IndexSink> sinks;
    private final IndexStateRepository stateRepository;
    private final IndexPlanner planner;
    private final Set<String> sinkNames;

    public IndexingPipeline(List<StructureParser> parsers, PackOptions packOptions,
                            List<IndexSink> sinks, IndexStateRepository stateRepository,
                            IndexPlanner planner) {
        if (parsers == null || parsers.isEmpty()) {
            throw new IllegalArgumentException("至少需要一个 StructureParser");
        }
        if (sinks == null || sinks.isEmpty()) {
            throw new IllegalArgumentException("至少需要一个 IndexSink");
        }
        if (stateRepository == null) {
            throw new IllegalArgumentException("IndexStateRepository 不能为空");
        }
        if (planner == null) {
            throw new IllegalArgumentException("IndexPlanner 不能为空");
        }
        for (StructureParser parser : parsers) {
            parsersByFormat.putIfAbsent(parser.format(), parser);
        }
        this.effectiveOptions = forceDeterministicId(
                packOptions != null ? packOptions : PackOptions.defaults());
        this.sinks = List.copyOf(sinks);
        this.stateRepository = stateRepository;
        this.planner = planner;
        Map<String, Boolean> names = new LinkedHashMap<>();
        for (IndexSink sink : this.sinks) {
            names.put(sink.name(), Boolean.TRUE);
        }
        this.sinkNames = names.keySet();
    }

    /**
     * 跑一次增量索引
     */
    public IndexRunReport run(DocumentSource source) {
        IndexRunReport report = new IndexRunReport(source.name());
        List<SourceDocument> snapshot = source.snapshot();
        // hash 优先取源提供的；缺失时现算
        for (SourceDocument doc : snapshot) {
            if (doc.getContentHash() == null || doc.getContentHash().isBlank()) {
                doc.setContentHash(Sha256ContentHash.of(doc.getContent()));
            }
        }

        Map<String, DocIndexState> states = stateRepository.load(source.name());
        IndexPlan plan = planner.plan(snapshot, states, sinkNames);

        for (DocIndexState toDelete : plan.getToDelete()) {
            deleteDocument(source.name(), toDelete, report);
        }
        for (SourceDocument doc : plan.getToAdd()) {
            indexDocument(source.name(), doc, states.get(doc.getId()), true, report);
        }
        for (SourceDocument doc : plan.getToUpdate()) {
            indexDocument(source.name(), doc, states.get(doc.getId()), false, report);
        }

        log.info("索引运行完成: {}", report.toComparableSummary());
        return report;
    }

    // ------------------------------------------------------------------
    // 删除对齐
    // ------------------------------------------------------------------

    private void deleteDocument(String sourceName, DocIndexState state, IndexRunReport report) {
        List<String> chunkIds = state.getChunkIds() != null ? state.getChunkIds() : List.of();
        String currentSink = null;
        try {
            for (IndexSink sink : sinks) {
                currentSink = sink.name();
                sink.delete(state.getDocId(), chunkIds);
            }
            stateRepository.remove(sourceName, state.getDocId());
            report.recordDeleted();
        } catch (Exception e) {
            log.warn("删除文档 {} 失败(sink={}): {}", state.getDocId(), currentSink, e.getMessage());
            report.recordFailure(state.getDocId(), currentSink, e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 新增 / 更新
    // ------------------------------------------------------------------

    private void indexDocument(String sourceName, SourceDocument doc, DocIndexState existing,
                               boolean isNew, IndexRunReport report) {
        String currentSink = "-";
        try {
            List<DocumentChunk> chunks = parseAndPack(doc);
            List<String> newChunkIds = chunks.stream().map(DocumentChunk::getId).toList();
            List<String> oldChunkIds = existing != null && existing.getChunkIds() != null
                    ? existing.getChunkIds() : List.of();

            // 工作状态：hash 与 chunkIds 保持旧值直到全 sink 完成（§2.2）
            DocIndexState working = new DocIndexState(doc.getId());
            working.setContentHash(existing != null ? existing.getContentHash() : null);
            working.setChunkIds(oldChunkIds);
            working.setSinkStatus(existing != null
                    ? new LinkedHashMap<>(existing.getSinkStatus()) : new LinkedHashMap<>());
            working.setGeneration(existing != null ? existing.getGeneration() : 0);
            working.setSchemaVersion(1);

            for (IndexSink sink : sinks) {
                currentSink = sink.name();
                sink.delete(doc.getId(), oldChunkIds);
                sink.upsert(doc.getId(), chunks);
                working.getSinkStatus().put(sink.name(), SinkStatus.DONE);
                stateRepository.save(sourceName, working);   // 逐 sink 保存进度
            }

            // 全 sink DONE：更新 hash 与 chunkIds，代际 +1
            working.setContentHash(doc.getContentHash());
            working.setChunkIds(newChunkIds);
            working.setGeneration(working.getGeneration() + 1);
            stateRepository.save(sourceName, working);

            if (isNew) {
                report.recordAdded();
            } else {
                report.recordUpdated();
            }
        } catch (Exception e) {
            log.warn("索引文档 {} 失败(sink={}): {}", doc.getId(), currentSink, e.getMessage());
            report.recordFailure(doc.getId(), currentSink, e.getMessage());
        }
    }

    private List<DocumentChunk> parseAndPack(SourceDocument doc) {
        StructureParser parser = parsersByFormat.get(doc.getFormat());
        if (parser == null) {
            throw new IllegalStateException("没有匹配格式 " + doc.getFormat()
                    + " 的 StructureParser; 已注册: " + parsersByFormat.keySet());
        }
        List<DocElement> elements = parser.parse(doc.getContent(), null);
        return new ChunkPacker(effectiveOptions).pack(doc.getId(), elements);
    }

    /**
     * 强制确定性块 ID：复制传入选项、把 idStrategy 换成 {@link ChunkIdStrategy#deterministic()}；
     * 若传入的是非确定性策略则 warn（评测与增量对齐都要求「同文档重复切分产出同一套 ID」）。
     */
    private PackOptions forceDeterministicId(PackOptions source) {
        PackOptions options = new PackOptions();
        options.setMaxChunkSize(source.getMaxChunkSize());
        options.setOverlap(source.getOverlap());
        options.setPreserveTypes(source.getPreserveTypes());
        options.setSeparators(source.getSeparators());
        options.setLengthMeasure(source.getLengthMeasure());
        options.setBreadcrumbToContent(source.isBreadcrumbToContent());
        options.setCodeSummaryToContent(source.isCodeSummaryToContent());
        options.setIdStrategy(ChunkIdStrategy.deterministic());
        if (!isDeterministic(source.getIdStrategy())) {
            log.warn("索引管线强制使用确定性块 ID: 传入的 idStrategy 非确定性, 已忽略。"
                    + "增量差分靠 ID 对齐, 随机 ID 会让每次重跑都全量重写");
        }
        return options;
    }

    /**
     * 双探针判定策略是否确定性：同一输入两次产出相同即视为确定性
     */
    private boolean isDeterministic(ChunkIdStrategy strategy) {
        if (strategy == null) {
            return false;
        }
        DocumentChunk probe = new DocumentChunk();
        probe.setContent("__probe__");
        String first = strategy.chunkId("__probe_doc__", 0, probe);
        String second = strategy.chunkId("__probe_doc__", 0, probe);
        return first != null && first.equals(second);
    }
}

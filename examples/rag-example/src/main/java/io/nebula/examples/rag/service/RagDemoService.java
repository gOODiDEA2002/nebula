package io.nebula.examples.rag.service;

import io.nebula.ai.rag.config.RagProperties;
import io.nebula.ai.rag.eval.EvalReport;
import io.nebula.ai.rag.eval.GoldenSet;
import io.nebula.ai.rag.eval.RetrievalEvaluator;
import io.nebula.ai.rag.index.DocIndexState;
import io.nebula.ai.rag.index.IndexRunReport;
import io.nebula.ai.rag.index.IndexStateRepository;
import io.nebula.ai.rag.index.IndexingPipeline;
import io.nebula.ai.rag.pipeline.HybridRetrievalEngine;
import io.nebula.ai.rag.pipeline.RagAnswer;
import io.nebula.ai.rag.pipeline.RagPipeline;
import io.nebula.ai.rag.pipeline.RagQuery;
import io.nebula.ai.rag.pipeline.RagStreamEvent;
import io.nebula.ai.rag.retriever.RetrievalResult;
import io.nebula.ai.rag.retriever.Retriever;
import io.nebula.examples.rag.config.RagDemoProperties;
import io.nebula.examples.rag.index.ClasspathDocumentSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 编排 index / clear / eval / status / search / query 六件事。
 * <p>
 * 全部框架依赖经 {@link ObjectProvider} 惰性获取：RAG 关闭（{@code AI_ENABLED} 缺省）时
 * 这些 Bean 均不存在，端点走禁用分支返回提示、HTTP 200、不抛错。
 *
 * @author Nebula Framework
 */
@Service
public class RagDemoService {

    private static final String GOLDEN_SET_PATH = "eval/golden-set.json";

    private final ObjectProvider<RagPipeline> ragPipelineProvider;
    private final ObjectProvider<HybridRetrievalEngine> engineProvider;
    private final ObjectProvider<IndexingPipeline> indexingPipelineProvider;
    private final ObjectProvider<ClasspathDocumentSource> documentSourceProvider;
    private final ObjectProvider<IndexStateRepository> stateRepositoryProvider;
    private final ObjectProvider<RagProperties> ragPropertiesProvider;
    private final RagDemoProperties demoProperties;
    private final String embeddingModel;

    public RagDemoService(ObjectProvider<RagPipeline> ragPipelineProvider,
                          ObjectProvider<HybridRetrievalEngine> engineProvider,
                          ObjectProvider<IndexingPipeline> indexingPipelineProvider,
                          ObjectProvider<ClasspathDocumentSource> documentSourceProvider,
                          ObjectProvider<IndexStateRepository> stateRepositoryProvider,
                          ObjectProvider<RagProperties> ragPropertiesProvider,
                          RagDemoProperties demoProperties,
                          @Value("${nebula.ai.openai.embedding.options.model:}") String embeddingModel) {
        this.ragPipelineProvider = ragPipelineProvider;
        this.engineProvider = engineProvider;
        this.indexingPipelineProvider = indexingPipelineProvider;
        this.documentSourceProvider = documentSourceProvider;
        this.stateRepositoryProvider = stateRepositoryProvider;
        this.ragPropertiesProvider = ragPropertiesProvider;
        this.demoProperties = demoProperties;
        this.embeddingModel = embeddingModel;
    }

    /** RAG 是否启用：以管线 Bean 是否装配为准 */
    public boolean isEnabled() {
        return ragPipelineProvider.getIfAvailable() != null;
    }

    /** GET /rag/status */
    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", isEnabled());
        if (!isEnabled()) {
            return status;
        }
        RagProperties props = ragPropertiesProvider.getObject();
        HybridRetrievalEngine engine = engineProvider.getObject();

        List<Map<String, Object>> retrievers = new ArrayList<>();
        List<Retriever> ordered = engine.getRetrievers();
        for (int i = 0; i < ordered.size(); i++) {
            Retriever r = ordered.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", r.getName());
            item.put("weight", r.getWeight());
            // order 取融合顺序位次（由 Spring @Order 决定：向量 10 在前、关键词 20 在后）
            item.put("order", i);
            retrievers.add(item);
        }
        status.put("retrievers", retrievers);
        status.put("rrfK", props.getFusion().getRrfK());
        status.put("chunkSize", props.getChunking().getSize());
        status.put("chunkOverlap", props.getChunking().getOverlap());
        status.put("topK", props.getRetrieval().getTopK());

        Map<String, DocIndexState> state = loadState();
        int chunks = 0;
        for (DocIndexState s : state.values()) {
            chunks += s.getChunkIds() == null ? 0 : s.getChunkIds().size();
        }
        status.put("indexedDocuments", state.size());
        status.put("indexedChunks", chunks);

        status.put("sanitizerEnabled", props.getGuard().getSanitizer().isEnabled());
        status.put("streamingEnabled", props.getStreaming().isEnabled());
        status.put("metricsEnabled", props.getMetrics().isEnabled());
        return status;
    }

    /** POST /rag/index：恢复正常快照后跑索引管线 */
    public Map<String, Object> index() {
        ClasspathDocumentSource source = documentSourceProvider.getObject();
        source.restore();
        IndexRunReport report = indexingPipelineProvider.getObject().run(source);
        return toReportMap(report);
    }

    /** DELETE /rag/documents：令快照返回空表，再跑一次让规划器判全删 */
    public Map<String, Object> clearDocuments() {
        ClasspathDocumentSource source = documentSourceProvider.getObject();
        source.clear();
        IndexRunReport report = indexingPipelineProvider.getObject().run(source);
        return toReportMap(report);
    }

    /** POST /rag/search：只检索不生成 */
    public List<Map<String, Object>> search(String query, Integer topK) {
        RagQuery ragQuery = RagQuery.builder()
                .query(query)
                .topK(topK)
                .generateAnswer(false)
                .build();
        RagAnswer answer = ragPipelineProvider.getObject().query(ragQuery);
        List<Map<String, Object>> results = new ArrayList<>();
        if (answer.getReferences() != null) {
            for (RetrievalResult r : answer.getReferences()) {
                results.add(toReferenceMap(r));
            }
        }
        return results;
    }

    /** POST /rag/query：完整管线 */
    public Map<String, Object> query(String query, Integer topK) {
        RagQuery ragQuery = RagQuery.builder()
                .query(query)
                .topK(topK)
                .build();
        RagAnswer answer = ragPipelineProvider.getObject().query(ragQuery);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("answer", answer.getAnswer());
        List<Map<String, Object>> refs = new ArrayList<>();
        if (answer.getReferences() != null) {
            for (RetrievalResult r : answer.getReferences()) {
                refs.add(toReferenceMap(r));
            }
        }
        result.put("references", refs);
        result.put("degraded", answer.isDegraded());
        result.put("degradeReason", answer.getDegradeReason());
        result.put("costMs", answer.getCostMs());
        return result;
    }

    /** POST /rag/query/stream：流式事件序列 */
    public Flux<RagStreamEvent> queryStream(String query, Integer topK) {
        RagQuery ragQuery = RagQuery.builder()
                .query(query)
                .topK(topK)
                .build();
        return ragPipelineProvider.getObject().queryStream(ragQuery);
    }

    /** GET /rag/eval：金标集逐条走多路引擎，框架计算 recall@k / MRR / nDCG@k */
    public EvalReport eval() {
        RagProperties props = ragPropertiesProvider.getObject();
        HybridRetrievalEngine engine = engineProvider.getObject();
        int k = props.getRetrieval().getTopK();
        GoldenSet goldenSet = loadGoldenSet();
        RetrievalEvaluator evaluator = new RetrievalEvaluator(k);
        return evaluator.evaluate(goldenSet,
                (q, topK) -> engine.retrieve(q, topK, null),
                configSnapshot(props));
    }

    private Map<String, String> configSnapshot(RagProperties props) {
        Map<String, String> snapshot = new LinkedHashMap<>();
        snapshot.put("chunkSize", String.valueOf(props.getChunking().getSize()));
        snapshot.put("chunkOverlap", String.valueOf(props.getChunking().getOverlap()));
        snapshot.put("rrfK", String.valueOf(props.getFusion().getRrfK()));
        snapshot.put("vectorWeight", String.valueOf(demoProperties.getVectorWeight()));
        snapshot.put("keywordWeight", String.valueOf(demoProperties.getKeywordWeight()));
        snapshot.put("topK", String.valueOf(props.getRetrieval().getTopK()));
        snapshot.put("embeddingModel", embeddingModel);
        return snapshot;
    }

    private Map<String, DocIndexState> loadState() {
        IndexStateRepository repo = stateRepositoryProvider.getIfAvailable();
        if (repo == null) {
            return Map.of();
        }
        return repo.load(ClasspathDocumentSource.SOURCE_NAME);
    }

    private GoldenSet loadGoldenSet() {
        try (InputStream in = new ClassPathResource(GOLDEN_SET_PATH).getInputStream()) {
            return GoldenSet.fromJson(in);
        } catch (Exception e) {
            throw new IllegalStateException("读取金标集失败: " + GOLDEN_SET_PATH + ", " + e.getMessage(), e);
        }
    }

    private Map<String, Object> toReportMap(IndexRunReport report) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("added", report.getAdded());
        map.put("updated", report.getUpdated());
        map.put("deleted", report.getDeleted());
        map.put("failed", report.getFailed());
        List<Map<String, Object>> failures = new ArrayList<>();
        for (IndexRunReport.Failure f : report.getFailures()) {
            Map<String, Object> fm = new LinkedHashMap<>();
            fm.put("docId", f.docId());
            fm.put("sinkName", f.sinkName());
            fm.put("reason", f.reason());
            failures.add(fm);
        }
        map.put("failures", failures);
        return map;
    }

    private Map<String, Object> toReferenceMap(RetrievalResult r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId());
        map.put("content", r.getContent());
        map.put("score", r.getScore());
        map.put("source", r.getSource());
        Map<String, Object> meta = r.getMetadata();
        map.put("docId", meta == null ? null : meta.get("docId"));
        map.put("title", meta == null ? null : meta.get("title"));
        return map;
    }
}

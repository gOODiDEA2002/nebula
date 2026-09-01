package io.nebula.ai.rag.index;

import io.nebula.ai.rag.chunking.DocumentChunk;
import io.nebula.ai.rag.chunking.pack.PackOptions;
import io.nebula.ai.rag.chunking.parse.MarkdownStructureParser;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 增量索引管线：逐文档失败继续、可重入、删除对齐、确定性块 ID（P2，详细设计 §2.2、§7）
 */
class IndexingPipelineTest {

    private static final MarkdownStructureParser MARKDOWN = new MarkdownStructureParser();

    @Test
    void failingDocument_isRecordedAndOthersContinue() {
        RecordingSink sink = new RecordingSink("vector-store");
        InMemoryIndexStateRepository repo = new InMemoryIndexStateRepository();
        IndexingPipeline pipeline = pipeline(List.of(sink), repo);

        // d2 的格式没有对应解析器 → 解析阶段失败，但 d1/d3 照常
        MutableSource source = new MutableSource("src", List.of(
                doc("d1", "# 标题1\n\n正文一", "markdown"),
                doc("d2", "正文二", "unknown-format"),
                doc("d3", "# 标题3\n\n正文三", "markdown")));

        IndexRunReport report = pipeline.run(source);

        assertThat(report.getAdded()).isEqualTo(2);
        assertThat(report.getFailed()).isEqualTo(1);
        assertThat(report.getFailures()).extracting(IndexRunReport.Failure::docId).containsExactly("d2");
        assertThat(sink.upsertedDocIds()).containsExactlyInAnyOrder("d1", "d3");
    }

    @Test
    void deterministicChunkIds_areEnforcedRegardlessOfInput() {
        RecordingSink sink = new RecordingSink("vector-store");
        IndexingPipeline pipeline = pipeline(List.of(sink), new InMemoryIndexStateRepository());

        pipeline.run(new MutableSource("src", List.of(
                doc("d1", "# 标题\n\n正文", "markdown"))));

        // 强制确定性 ID：<docId>#<index>
        assertThat(sink.chunksFor("d1")).extracting(DocumentChunk::getId)
                .allSatisfy(id -> assertThat(id).startsWith("d1#"));
    }

    @Test
    void interruptedRun_isResumedOnReentry() {
        RecordingSink sink1 = new RecordingSink("vector-store");
        FailingSink sink2 = new FailingSink("search-service", 1);   // 第一次 upsert 抛出
        InMemoryIndexStateRepository repo = new InMemoryIndexStateRepository();
        IndexingPipeline pipeline = pipeline(List.of(sink1, sink2), repo);
        MutableSource source = new MutableSource("src", List.of(
                doc("d1", "# 标题\n\n正文", "markdown")));

        // 第一次：sink1 成功、sink2 崩 → 失败记录，状态里 sink2 仍 PENDING
        IndexRunReport first = pipeline.run(source);
        assertThat(first.getFailed()).isEqualTo(1);
        assertThat(first.getAdded()).isZero();
        Map<String, DocIndexState> afterFirst = repo.load("src");
        assertThat(afterFirst.get("d1").getSinkStatus())
                .containsEntry("vector-store", SinkStatus.DONE)
                .doesNotContainEntry("search-service", SinkStatus.DONE);

        // 第二次：d1 因 sink2 PENDING 落入 toUpdate，sink2 这次成功
        IndexRunReport second = pipeline.run(source);
        assertThat(second.getUpdated()).isEqualTo(1);
        assertThat(second.getFailed()).isZero();
        Map<String, DocIndexState> afterSecond = repo.load("src");
        assertThat(afterSecond.get("d1").getSinkStatus())
                .containsEntry("vector-store", SinkStatus.DONE)
                .containsEntry("search-service", SinkStatus.DONE);
        assertThat(afterSecond.get("d1").getChunkIds()).isNotEmpty();
        assertThat(sink2.upsertedDocIds()).containsExactly("d1");
    }

    @Test
    void documentRemovedFromSnapshot_isDeletedFromSinksAndState() {
        RecordingSink sink = new RecordingSink("vector-store");
        InMemoryIndexStateRepository repo = new InMemoryIndexStateRepository();
        IndexingPipeline pipeline = pipeline(List.of(sink), repo);

        MutableSource source = new MutableSource("src", List.of(
                doc("d1", "# 标题\n\n正文", "markdown"),
                doc("d2", "# 标题\n\n正文二", "markdown")));
        pipeline.run(source);
        List<String> d2ChunkIds = new ArrayList<>(repo.load("src").get("d2").getChunkIds());

        // 下一次快照少了 d2 → 视为删除
        source.setSnapshot(List.of(doc("d1", "# 标题\n\n正文", "markdown")));
        IndexRunReport report = pipeline.run(source);

        assertThat(report.getDeleted()).isEqualTo(1);
        assertThat(sink.deletedIds()).containsAll(d2ChunkIds);
        assertThat(repo.load("src")).doesNotContainKey("d2");
    }

    @Test
    void codeSummaryOption_survivesForcedIdStrategyCopy() {
        RecordingSink sink = new RecordingSink("vector-store");
        PackOptions options = PackOptions.defaults();
        options.setCodeSummaryToContent(true);
        IndexingPipeline pipeline = new IndexingPipeline(List.of(MARKDOWN), options,
                List.of(sink), new InMemoryIndexStateRepository(), new IndexPlanner());

        // 管线内部会复制选项以强制确定性 ID，codeSummaryToContent 不得在复制中丢失
        pipeline.run(new MutableSource("src", List.of(
                doc("d1", "# 标题\n\n```java\npublic class Foo {\n}\n```", "markdown"))));

        assertThat(sink.chunksFor("d1"))
                .anySatisfy(chunk -> assertThat(chunk.getContent()).contains("// public class Foo {"));
    }

    // ------------------------------------------------------------------

    private static IndexingPipeline pipeline(List<IndexSink> sinks, InMemoryIndexStateRepository repo) {
        return new IndexingPipeline(List.of(MARKDOWN), null, sinks, repo, new IndexPlanner());
    }

    private static SourceDocument doc(String id, String content, String format) {
        return new SourceDocument(id, content, format);
    }

    /** 可变快照的文档源 */
    private static final class MutableSource implements DocumentSource {

        private final String name;
        private volatile List<SourceDocument> snapshot;

        MutableSource(String name, List<SourceDocument> snapshot) {
            this.name = name;
            this.snapshot = snapshot;
        }

        void setSnapshot(List<SourceDocument> snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public List<SourceDocument> snapshot() {
            // 返回新副本，模拟每次读到一份独立快照
            return new ArrayList<>(snapshot);
        }
    }

    /** 记录收到的 upsert/delete 参数的 sink 桩 */
    private static class RecordingSink implements IndexSink {

        private final String name;
        private final List<String> upserted = new CopyOnWriteArrayList<>();
        private final Map<String, List<DocumentChunk>> chunksByDoc = new java.util.concurrent.ConcurrentHashMap<>();
        private final List<String> deleted = new CopyOnWriteArrayList<>();

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
            chunksByDoc.put(docId, new ArrayList<>(chunks));
        }

        @Override
        public void delete(String docId, List<String> chunkIds) {
            deleted.addAll(chunkIds);
        }

        List<String> upsertedDocIds() {
            return new ArrayList<>(upserted);
        }

        List<DocumentChunk> chunksFor(String docId) {
            return chunksByDoc.getOrDefault(docId, List.of());
        }

        List<String> deletedIds() {
            return new ArrayList<>(deleted);
        }
    }

    /** 前 N 次 upsert 抛出的 sink 桩，用于模拟中断 */
    private static final class FailingSink extends RecordingSink {

        private final AtomicInteger remainingFailures;

        FailingSink(String name, int failuresBeforeSuccess) {
            super(name);
            this.remainingFailures = new AtomicInteger(failuresBeforeSuccess);
        }

        @Override
        public void upsert(String docId, List<DocumentChunk> chunks) {
            if (remainingFailures.getAndDecrement() > 0) {
                throw new IndexSinkException(name(), docId, List.of(), "模拟中断", null);
            }
            super.upsert(docId, chunks);
        }
    }
}

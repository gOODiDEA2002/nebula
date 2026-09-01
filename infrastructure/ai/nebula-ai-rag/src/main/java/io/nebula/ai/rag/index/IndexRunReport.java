package io.nebula.ai.rag.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 一次索引运行的结果报告
 * <p>
 * 计数 added/updated/deleted/failed，并逐失败文档记下 sink 与异常摘要。
 * {@link #toComparableSummary()} 与 {@code EvalReport} 同风格，便于留档比对。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class IndexRunReport {

    private final String sourceName;
    private int added;
    private int updated;
    private int deleted;
    private final List<Failure> failures = new ArrayList<>();

    public IndexRunReport(String sourceName) {
        this.sourceName = sourceName;
    }

    public void recordAdded() {
        added++;
    }

    public void recordUpdated() {
        updated++;
    }

    public void recordDeleted() {
        deleted++;
    }

    public void recordFailure(String docId, String sinkName, String reason) {
        failures.add(new Failure(docId, sinkName, reason));
    }

    public String getSourceName() {
        return sourceName;
    }

    public int getAdded() {
        return added;
    }

    public int getUpdated() {
        return updated;
    }

    public int getDeleted() {
        return deleted;
    }

    public int getFailed() {
        return failures.size();
    }

    public List<Failure> getFailures() {
        return List.copyOf(failures);
    }

    public String toComparableSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "source=%s added=%d updated=%d deleted=%d failed=%d",
                sourceName, added, updated, deleted, failures.size()));
        if (!failures.isEmpty()) {
            sb.append(" | failures:");
            for (Failure failure : failures) {
                sb.append(String.format(Locale.ROOT, " %s@%s(%s)",
                        failure.docId(), failure.sinkName(), failure.reason()));
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toComparableSummary();
    }

    /**
     * 单条失败记录：文档、出错的 sink（解析/装箱阶段用 {@code -}）、异常摘要
     */
    public record Failure(String docId, String sinkName, String reason) {
    }
}

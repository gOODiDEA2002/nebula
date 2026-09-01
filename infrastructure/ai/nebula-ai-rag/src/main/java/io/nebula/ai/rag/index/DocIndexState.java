package io.nebula.ai.rag.index;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档 × sink 的索引状态
 * <p>
 * 全部必需 sink 为 {@link SinkStatus#DONE} 才算该文档完成（上位 §4 决策 4）。
 * {@code schemaVersion} 首版为 1。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class DocIndexState {

    private String docId;

    private String contentHash;

    private List<String> chunkIds;

    /** sinkName → DONE / PENDING */
    private Map<String, SinkStatus> sinkStatus = new LinkedHashMap<>();

    private long generation;

    private int schemaVersion = 1;

    public DocIndexState() {
    }

    public DocIndexState(String docId) {
        this.docId = docId;
    }

    /**
     * 深拷贝：状态库每次 save 应存快照，避免调用方后续 mutate 回写已存的进度
     */
    public DocIndexState copy() {
        DocIndexState copy = new DocIndexState(docId);
        copy.contentHash = contentHash;
        copy.chunkIds = chunkIds != null ? List.copyOf(chunkIds) : null;
        copy.sinkStatus = new LinkedHashMap<>(sinkStatus);
        copy.generation = generation;
        copy.schemaVersion = schemaVersion;
        return copy;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public List<String> getChunkIds() {
        return chunkIds;
    }

    public void setChunkIds(List<String> chunkIds) {
        this.chunkIds = chunkIds;
    }

    public Map<String, SinkStatus> getSinkStatus() {
        return sinkStatus;
    }

    public void setSinkStatus(Map<String, SinkStatus> sinkStatus) {
        this.sinkStatus = sinkStatus != null ? sinkStatus : new LinkedHashMap<>();
    }

    public long getGeneration() {
        return generation;
    }

    public void setGeneration(long generation) {
        this.generation = generation;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }
}

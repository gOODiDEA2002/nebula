package io.nebula.ai.rag.index;

/**
 * 索引代际状态（R3 §3.3、R3-D1）
 * <p>
 * <b>不复用 {@link DocIndexState#getGeneration()}：</b>后者在 R2 的语义是「该文档写入完成次数」
 * （每次全 sink 完成 +1），与「索引代际」是两个概念，混用会让 R2 既有语义漂移（违 Y2）。
 * 故 R3 另立源级别的代际状态。
 * <p>
 * 持久化复用 {@link IndexStateRepository}，以分区键 {@code <sourceName>#generation} 承载，
 * 编解码见 {@code ReindexPipeline}（无需改状态库接口，守 Y1）。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class IndexGenerationState {

    /** 源名（业务 docId 的分区） */
    private String sourceName;

    /** 别名当前指向的代际；0 = 尚无活动代际 */
    private long activeGeneration;

    /** 正在灌但未切换的代际；0 = 无 */
    private long buildingGeneration;

    /** schema 版本，首版 = 1 */
    private int schemaVersion = 1;

    public IndexGenerationState() {
    }

    public IndexGenerationState(String sourceName) {
        this.sourceName = sourceName;
    }

    public IndexGenerationState copy() {
        IndexGenerationState copy = new IndexGenerationState(sourceName);
        copy.activeGeneration = activeGeneration;
        copy.buildingGeneration = buildingGeneration;
        copy.schemaVersion = schemaVersion;
        return copy;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public long getActiveGeneration() {
        return activeGeneration;
    }

    public void setActiveGeneration(long activeGeneration) {
        this.activeGeneration = activeGeneration;
    }

    public long getBuildingGeneration() {
        return buildingGeneration;
    }

    public void setBuildingGeneration(long buildingGeneration) {
        this.buildingGeneration = buildingGeneration;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }
}

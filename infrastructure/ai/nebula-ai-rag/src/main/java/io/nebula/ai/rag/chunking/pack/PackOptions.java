package io.nebula.ai.rag.chunking.pack;

import io.nebula.ai.rag.chunking.parse.DocElementType;

import java.util.EnumSet;
import java.util.Set;

/**
 * 装箱参数
 * <p>
 * 全部有默认值，且默认值与现状切分参数（500/100）一致，
 * 让「换成结构化切分」这件事不必同时换一套预算。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class PackOptions {

    /** 面包屑写入 metadata 的固定键名 */
    public static final String META_BREADCRUMB = "breadcrumb";

    /** 表格按行切分后的分片编号写入 metadata 的键名 */
    public static final String META_TABLE_PART = "tablePart";

    /** 面包屑注入正文时的层级分隔符 */
    public static final String BREADCRUMB_SEPARATOR = " > ";

    private int maxChunkSize = 500;

    private int overlap = 100;

    /**
     * 原子单元类型：这些元素宁可超限也不切开
     * <p>
     * 代码块切一半就不再是可执行片段，表格切掉表头就不知道每列是什么，
     * 两者被切坏之后的检索价值接近于零，因此宁可让块超一点预算。
     */
    private Set<DocElementType> preserveTypes =
            EnumSet.of(DocElementType.TABLE, DocElementType.CODE);

    private SeparatorHierarchy separators = SeparatorHierarchy.chineseDefault();

    private LengthMeasure lengthMeasure = LengthMeasure.chars();

    private ChunkIdStrategy idStrategy = ChunkIdStrategy.random();

    /**
     * 是否把面包屑注入块正文首行
     * <p>
     * 默认关：正文是喂给检索与模型的内容，往里加东西会改变既有块的文本，
     * 属于行为变化而不是新增能力。需要靠标题路径提升召回的场景显式打开。
     */
    private boolean breadcrumbToContent = false;

    public static PackOptions defaults() {
        return new PackOptions();
    }

    public int getMaxChunkSize() {
        return maxChunkSize;
    }

    public void setMaxChunkSize(int maxChunkSize) {
        if (maxChunkSize <= 0) {
            throw new IllegalArgumentException("maxChunkSize 必须为正数");
        }
        this.maxChunkSize = maxChunkSize;
    }

    public int getOverlap() {
        return overlap;
    }

    public void setOverlap(int overlap) {
        if (overlap < 0) {
            throw new IllegalArgumentException("overlap 不能为负数");
        }
        this.overlap = overlap;
    }

    public Set<DocElementType> getPreserveTypes() {
        return preserveTypes;
    }

    public void setPreserveTypes(Set<DocElementType> preserveTypes) {
        this.preserveTypes = preserveTypes != null
                ? EnumSet.copyOf(preserveTypes) : EnumSet.noneOf(DocElementType.class);
    }

    public SeparatorHierarchy getSeparators() {
        return separators;
    }

    public void setSeparators(SeparatorHierarchy separators) {
        if (separators == null) {
            throw new IllegalArgumentException("separators 不能为空");
        }
        this.separators = separators;
    }

    public LengthMeasure getLengthMeasure() {
        return lengthMeasure;
    }

    public void setLengthMeasure(LengthMeasure lengthMeasure) {
        if (lengthMeasure == null) {
            throw new IllegalArgumentException("lengthMeasure 不能为空");
        }
        this.lengthMeasure = lengthMeasure;
    }

    public ChunkIdStrategy getIdStrategy() {
        return idStrategy;
    }

    public void setIdStrategy(ChunkIdStrategy idStrategy) {
        if (idStrategy == null) {
            throw new IllegalArgumentException("idStrategy 不能为空");
        }
        this.idStrategy = idStrategy;
    }

    public boolean isBreadcrumbToContent() {
        return breadcrumbToContent;
    }

    public void setBreadcrumbToContent(boolean breadcrumbToContent) {
        this.breadcrumbToContent = breadcrumbToContent;
    }

    /**
     * 校验参数之间的相容性；装箱前调用一次，把配置错误挡在产出块之前
     */
    public void validate() {
        if (overlap >= maxChunkSize) {
            throw new IllegalArgumentException(
                    "overlap 必须小于 maxChunkSize, 否则切分无法前进");
        }
    }
}

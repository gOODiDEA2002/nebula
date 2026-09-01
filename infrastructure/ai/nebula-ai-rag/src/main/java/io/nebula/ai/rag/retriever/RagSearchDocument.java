package io.nebula.ai.rag.retriever;

import io.nebula.search.core.model.IndexMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BM25 关键词检索路的索引文档模型（P4b，详细设计 §3.2）
 * <p>
 * 字段与默认 mapping 一一对应：{@code content} 走可配分析器的全文检索，{@code title}
 * 带 keyword 子字段，其余标识字段走 keyword 精确匹配，{@code metadata} 只存不检索。
 * <p>
 * 本类是普通 POJO，由 {@code SearchService} 负责序列化/反序列化，因此保留无参构造与全套访问器。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class RagSearchDocument {

    /** = 块 ID，keyword */
    private String id;

    /** 业务文档 ID，keyword */
    private String docId;

    /** 正文，text，analyzer 可配（默认 standard） */
    private String content;

    /** 标题，text + keyword 子字段 */
    private String title;

    /** 面包屑（标题/键路径），keyword 数组 */
    private List<String> breadcrumb;

    /** 块类型，keyword */
    private String chunkType;

    /** 附加元数据，object，enabled=false（只存不检索） */
    private Map<String, Object> metadata;

    public RagSearchDocument() {
    }

    /**
     * 默认 mapping：analyzer 与 search_analyzer 经配置注入（默认 {@code standard}）。
     * <p>
     * 返回的 {@link IndexMapping} 的 {@code properties} 承载完整 mappings 主体
     * （含顶层 {@code properties} 键），与框架其余调用方一致。IK 环境下把两个参数换成
     * {@code ik_max_word}/{@code ik_smart} 即可，无需改本类。
     *
     * @param analyzer       建索引与查询的分析器
     * @param searchAnalyzer 查询侧的 search_analyzer
     */
    public static IndexMapping defaultMapping(String analyzer, String searchAnalyzer) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("id", Map.of("type", "keyword"));
        props.put("docId", Map.of("type", "keyword"));
        props.put("content", textField(analyzer, searchAnalyzer));
        Map<String, Object> title = new LinkedHashMap<>(textField(analyzer, searchAnalyzer));
        title.put("fields", Map.of("keyword", Map.of("type", "keyword")));
        props.put("title", title);
        props.put("breadcrumb", Map.of("type", "keyword"));
        props.put("chunkType", Map.of("type", "keyword"));
        props.put("metadata", Map.of("type", "object", "enabled", false));
        return new IndexMapping(Map.of("properties", props));
    }

    private static Map<String, Object> textField(String analyzer, String searchAnalyzer) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("type", "text");
        field.put("analyzer", analyzer);
        field.put("search_analyzer", searchAnalyzer);
        return field;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getBreadcrumb() {
        return breadcrumb;
    }

    public void setBreadcrumb(List<String> breadcrumb) {
        this.breadcrumb = breadcrumb;
    }

    public String getChunkType() {
        return chunkType;
    }

    public void setChunkType(String chunkType) {
        this.chunkType = chunkType;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}

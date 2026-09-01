package io.nebula.ai.rag.chunking.parse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档元素
 * <p>
 * 解析层与装箱层之间唯一的数据契约。元素只描述「这是一段什么、它在文档里的位置路径是什么」，
 * 不含任何长度或切分决策 —— 那些属于装箱层。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class DocElement {

    /** 表格属性：表头行数（含 Markdown 的分隔行），装箱层按行切表时据此重复表头 */
    public static final String ATTR_HEADER_ROW_COUNT = "headerRowCount";

    /** 代码属性：语言标记，无标记时不写入 */
    public static final String ATTR_LANGUAGE = "language";

    private DocElementType type;

    /**
     * 元素原文；表格含表头行，代码含围栏内的原始缩进
     */
    private String text;

    /**
     * 位置路径：Markdown/HTML 的标题路径、XML 的元素路径、JSON 的键路径
     */
    private List<String> breadcrumb = new ArrayList<>();

    /**
     * 类型相关属性，键见本类常量
     */
    private Map<String, Object> attrs = new LinkedHashMap<>();

    public DocElement() {
    }

    public DocElement(DocElementType type, String text, List<String> breadcrumb) {
        this.type = type;
        this.text = text;
        setBreadcrumb(breadcrumb);
    }

    /**
     * 写入一个属性
     */
    public DocElement attr(String key, Object value) {
        this.attrs.put(key, value);
        return this;
    }

    /**
     * 读取整数属性
     *
     * @param key          属性键
     * @param defaultValue 缺失或类型不符时的取值
     */
    public int intAttr(String key, int defaultValue) {
        Object value = attrs.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }

    public DocElementType getType() {
        return type;
    }

    public void setType(DocElementType type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getBreadcrumb() {
        return breadcrumb;
    }

    public void setBreadcrumb(List<String> breadcrumb) {
        this.breadcrumb = breadcrumb != null ? new ArrayList<>(breadcrumb) : new ArrayList<>();
    }

    public Map<String, Object> getAttrs() {
        return attrs;
    }

    public void setAttrs(Map<String, Object> attrs) {
        this.attrs = attrs != null ? new LinkedHashMap<>(attrs) : new LinkedHashMap<>();
    }

    @Override
    public String toString() {
        return type + breadcrumb.toString() + " len=" + (text == null ? 0 : text.length());
    }
}

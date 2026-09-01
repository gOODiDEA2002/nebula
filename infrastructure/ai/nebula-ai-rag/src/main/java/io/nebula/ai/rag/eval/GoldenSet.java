package io.nebula.ai.rag.eval;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.CollectionType;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 评测金标集
 * <p>
 * <b>口径声明（照抄进任何引用本类的报告，不要另行发明）：</b>
 * <ul>
 *   <li><b>命中判据是前缀匹配：</b>检索结果 ID 以条目的任一 {@code expectedIdPrefixes} 开头即算命中。
 *       前提是块 ID 具有确定性（见 {@code ChunkIdStrategy.deterministic()}），
 *       随机 UUID 的块 ID 无法用本类评测。</li>
 *   <li><b>相关性是二值的：</b>命中即相关，不区分「更相关 / 略相关」，
 *       因此 nDCG 用二值相关公式，不做分级增益。</li>
 *   <li><b>适用范围：</b>用于同一语料、同一检索器下比较不同<b>切分 / 检索配置</b>的相对优劣。
 *       它不是「RAG 质量的绝对分数」，跨语料、跨检索器的数值不可比。</li>
 * </ul>
 * 本类是纯库：不依赖 Spring，不反读任何配置，配置快照由调用方显式传给
 * {@link RetrievalEvaluator#evaluate}。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class GoldenSet {

    /** 条目未指定子集时归入的默认子集名 */
    public static final String DEFAULT_SUBSET = "default";

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private List<GoldenSetEntry> entries = new ArrayList<>();

    public GoldenSet() {
    }

    public GoldenSet(List<GoldenSetEntry> entries) {
        setEntries(entries);
    }

    /**
     * 从 JSON 流读取金标集
     * <p>
     * 接受两种顶层形态：条目数组 {@code [ {...}, {...} ]}，或对象
     * {@code {"entries": [ ... ]}}。解析失败、结构非法、条目缺字段一律抛
     * {@link IllegalArgumentException} 并带上具体原因 —— 金标是评测的地基，
     * 静默跳过坏条目会让报告在无人察觉的情况下失真。
     *
     * @param in JSON 输入流，不能为 null；由调用方负责关闭
     * @return 金标集
     * @throws IllegalArgumentException 流为空、JSON 非法或条目字段缺失
     */
    public static GoldenSet fromJson(InputStream in) {
        if (in == null) {
            throw new IllegalArgumentException("金标集输入流不能为空");
        }
        List<GoldenSetEntry> parsed;
        try {
            CollectionType listType = MAPPER.getTypeFactory()
                    .constructCollectionType(List.class, GoldenSetEntry.class);
            byte[] content = in.readAllBytes();
            if (content.length == 0) {
                throw new IllegalArgumentException("金标集内容为空");
            }
            String text = new String(content, java.nio.charset.StandardCharsets.UTF_8).trim();
            if (text.startsWith("[")) {
                parsed = MAPPER.readValue(text, listType);
            } else {
                GoldenSetWrapper wrapper = MAPPER.readValue(text, GoldenSetWrapper.class);
                parsed = wrapper.getEntries();
            }
        } catch (JacksonException e) {
            throw new IllegalArgumentException("金标集 JSON 解析失败: " + e.getMessage(), e);
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("金标集读取失败: " + e.getMessage(), e);
        }
        if (parsed == null || parsed.isEmpty()) {
            throw new IllegalArgumentException("金标集不能为空: 至少需要一条条目");
        }
        return new GoldenSet(parsed);
    }

    /**
     * 全部条目（只读视图）
     */
    public List<GoldenSetEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<GoldenSetEntry> entries) {
        List<GoldenSetEntry> copy = new ArrayList<>();
        if (entries != null) {
            for (int i = 0; i < entries.size(); i++) {
                copy.add(validate(entries.get(i), i));
            }
        }
        this.entries = copy;
    }

    /**
     * 条目总数
     */
    public int size() {
        return entries.size();
    }

    /**
     * 出现过的全部子集名，保持条目出现顺序
     */
    public Set<String> subsets() {
        Set<String> names = new LinkedHashSet<>();
        for (GoldenSetEntry entry : entries) {
            names.add(entry.resolvedSubset());
        }
        return names;
    }

    private static GoldenSetEntry validate(GoldenSetEntry entry, int index) {
        if (entry == null) {
            throw new IllegalArgumentException("金标条目[" + index + "]为 null");
        }
        if (entry.getQuery() == null || entry.getQuery().isBlank()) {
            throw new IllegalArgumentException("金标条目[" + index + "]缺少 query");
        }
        if (entry.getExpectedIdPrefixes() == null || entry.getExpectedIdPrefixes().isEmpty()) {
            throw new IllegalArgumentException(
                    "金标条目[" + index + "] (query=" + entry.getQuery() + ") 缺少 expectedIdPrefixes");
        }
        for (String prefix : entry.getExpectedIdPrefixes()) {
            if (prefix == null || prefix.isBlank()) {
                throw new IllegalArgumentException(
                        "金标条目[" + index + "] (query=" + entry.getQuery() + ") 含空的 expectedIdPrefix");
            }
        }
        return entry;
    }

    /**
     * {@code {"entries": [...]}} 形态的载体
     */
    static class GoldenSetWrapper {

        private List<GoldenSetEntry> entries;

        public List<GoldenSetEntry> getEntries() {
            return entries;
        }

        public void setEntries(List<GoldenSetEntry> entries) {
            this.entries = entries;
        }
    }
}

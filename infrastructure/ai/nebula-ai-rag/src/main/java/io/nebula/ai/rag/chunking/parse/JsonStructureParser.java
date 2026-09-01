package io.nebula.ai.rag.chunking.parse;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * JSON 结构解析器（按键路径递归下潜）
 * <p>
 * <b>切分口径：</b>从根开始递归，只要某棵子树的序列化长度不超过记录预算，就把整棵子树封成
 * 一个 RECORD 元素，面包屑写它的键路径；超过预算才继续往下潜。这样切出来的每个元素
 * 都是<b>合法的 JSON 子树</b>而不是半截文本，配置项与它所属的分组也就不会被切散。
 * <p>
 * <b>为什么要带键路径：</b>{@code {"timeout-seconds": 15}} 这样的叶子单独拿出来毫无检索价值 ——
 * 有价值的是「它是 nebula.ai.rag.retrieval 下的 timeout-seconds」。键路径经面包屑传给装箱层，
 * 由装箱层决定写进 metadata 还是同时写进正文。
 * <p>
 * <b>坏 JSON 直接抛异常</b>，与 Markdown 的容错处理不同：Markdown 畸形还能按段落降级理解，
 * 解析不了的 JSON 没有任何合理的部分解释，静默产出半份内容比失败更糟。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class JsonStructureParser implements StructureParser {

    /** 格式标识 */
    public static final String FORMAT = "json";

    /** 默认记录预算：与装箱层默认块大小对齐 */
    public static final int DEFAULT_MAX_RECORD_CHARS = 500;

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private final int maxRecordChars;

    /**
     * 使用默认记录预算 {@value #DEFAULT_MAX_RECORD_CHARS}
     */
    public JsonStructureParser() {
        this(DEFAULT_MAX_RECORD_CHARS);
    }

    /**
     * @param maxRecordChars 单个 RECORD 元素的序列化长度预算；应与装箱层的块大小一致，
     *                       否则要么切得比装箱预算还碎，要么每个元素都要装箱层再切一次
     */
    public JsonStructureParser(int maxRecordChars) {
        if (maxRecordChars <= 0) {
            throw new IllegalArgumentException("maxRecordChars 必须为正数");
        }
        this.maxRecordChars = maxRecordChars;
    }

    public int getMaxRecordChars() {
        return maxRecordChars;
    }

    @Override
    public String format() {
        return FORMAT;
    }

    @Override
    public List<DocElement> parse(String content, ParseOptions options) {
        ParseOptions limits = options != null ? options : ParseOptions.defaults();
        limits.checkInput(content);
        List<DocElement> elements = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return elements;
        }

        JsonNode root;
        try {
            root = MAPPER.readTree(content);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("JSON 解析失败: " + e.getMessage(), e);
        }
        walk(root, new ArrayList<>(), 1, elements, limits);
        return elements;
    }

    private void walk(JsonNode node, List<String> path, int depth,
                      List<DocElement> elements, ParseOptions limits) {
        limits.checkDepth(depth);
        String serialized = MAPPER.writeValueAsString(node);

        // 叶子（含超长叶子）无处再拆，容器只要装得下就整棵封存
        if (!node.isContainer() || serialized.length() <= maxRecordChars) {
            emit(node, serialized, path, elements, limits);
            return;
        }

        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.properties().iterator();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                path.add(field.getKey());
                walk(field.getValue(), path, depth + 1, elements, limits);
                path.remove(path.size() - 1);
            }
            return;
        }

        for (int i = 0; i < node.size(); i++) {
            path.add("[" + i + "]");
            walk(node.get(i), path, depth + 1, elements, limits);
            path.remove(path.size() - 1);
        }
    }

    private void emit(JsonNode node, String serialized, List<String> path,
                      List<DocElement> elements, ParseOptions limits) {
        // 空对象与空数组不产出元素：它们在检索上没有任何信息量
        if (node.isContainer() && node.isEmpty()) {
            return;
        }
        elements.add(new DocElement(DocElementType.RECORD, serialized, path));
        limits.checkElementCount(elements.size());
    }
}

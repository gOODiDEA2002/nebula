package io.nebula.ai.rag.chunking.parse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * HTML 结构解析器（jsoup）
 * <p>
 * <b>依赖边界：</b>本类是整个 {@code nebula-ai-rag} 模块<b>唯一</b> import jsoup 的类，
 * 而 jsoup 是 optional 依赖。因此：消费方在装配本类之前必须做类存在检查，
 * 缺少 jsoup 时加载本类会得到 {@code NoClassDefFoundError}。把 jsoup 的接触面收在
 * 一个类里，就是为了让这个检查只需要判断一个类名。
 * <p>
 * <b>剔除导航噪声：</b>{@code script/style/nav/footer/header/aside} 一律先删掉。
 * 它们在每个页面上都一样，留着只会让每个块都带上同一堆页脚文字，
 * 检索时互相稀释，还会让不同页面的块看起来彼此相似。
 * <p>
 * <b>表格统一成 Markdown 管道表示：</b>与 Markdown 解析器产出同一种表格文本，
 * 装箱层的「超限按行切并重复表头」因此只需要写一份。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class HtmlStructureParser implements StructureParser {

    /** 格式标识 */
    public static final String FORMAT = "html";

    /** 本类唯一依赖的 jsoup 类名，供消费方做类存在检查 */
    public static final String JSOUP_CLASS_NAME = "org.jsoup.Jsoup";

    private static final String NOISE_SELECTOR = "script, style, nav, footer, header, aside, noscript";

    private static final Set<String> HEADING_TAGS = Set.of("h1", "h2", "h3", "h4", "h5", "h6");

    private static final int MAX_HEADING_LEVEL = 6;

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

        Document document = Jsoup.parse(content);
        document.select(NOISE_SELECTOR).remove();

        String[] headingStack = new String[MAX_HEADING_LEVEL + 1];
        Element body = document.body() != null ? document.body() : document;
        walk(body, headingStack, elements, limits, 1);
        return elements;
    }

    private void walk(Element element, String[] headingStack, List<DocElement> elements,
                      ParseOptions limits, int depth) {
        limits.checkDepth(depth);
        for (Element child : element.children()) {
            String tag = child.tagName().toLowerCase(Locale.ROOT);

            if (HEADING_TAGS.contains(tag)) {
                int level = Integer.parseInt(tag.substring(1));
                String title = normalize(child.text());
                if (!title.isEmpty()) {
                    headingStack[level] = title;
                    for (int deeper = level + 1; deeper <= MAX_HEADING_LEVEL; deeper++) {
                        headingStack[deeper] = null;
                    }
                    add(elements, new DocElement(DocElementType.HEADING, title,
                            breadcrumb(headingStack)), limits);
                }
                continue;
            }

            switch (tag) {
                case "table" -> {
                    DocElement table = toTableElement(child, headingStack);
                    if (table != null) {
                        add(elements, table, limits);
                    }
                }
                case "pre" -> {
                    String code = child.wholeText();
                    if (!code.isBlank()) {
                        add(elements, new DocElement(DocElementType.CODE, code.stripTrailing(),
                                breadcrumb(headingStack)), limits);
                    }
                }
                case "li" -> {
                    String text = normalize(child.text());
                    if (!text.isEmpty()) {
                        add(elements, new DocElement(DocElementType.LIST_ITEM, text,
                                breadcrumb(headingStack)), limits);
                    }
                }
                case "p", "blockquote" -> {
                    String text = normalize(child.text());
                    if (!text.isEmpty()) {
                        add(elements, new DocElement(DocElementType.PARAGRAPH, text,
                                breadcrumb(headingStack)), limits);
                    }
                }
                default -> {
                    // 容器标签本身可能挂着直接文本（div 里裸写的一段字），先把它收掉再下潜，
                    // 否则这段文字会随着递归被丢失
                    String direct = directText(child);
                    if (!direct.isEmpty()) {
                        add(elements, new DocElement(DocElementType.PARAGRAPH, direct,
                                breadcrumb(headingStack)), limits);
                    }
                    walk(child, headingStack, elements, limits, depth + 1);
                }
            }
        }
    }

    /**
     * 只取元素自己的直接文本子节点，不含后代元素的文本
     */
    private static String directText(Element element) {
        StringBuilder sb = new StringBuilder();
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode textNode) {
                sb.append(textNode.text());
            }
        }
        return normalize(sb.toString());
    }

    /**
     * 表格转 Markdown 管道表示；首行当表头，紧跟一行分隔行
     */
    private DocElement toTableElement(Element table, String[] headingStack) {
        Elements rows = table.select("tr");
        if (rows.isEmpty()) {
            return null;
        }
        List<List<String>> matrix = new ArrayList<>();
        for (Element row : rows) {
            Elements cells = row.select("th, td");
            if (cells.isEmpty()) {
                continue;
            }
            List<String> values = new ArrayList<>(cells.size());
            for (Element cell : cells) {
                values.add(normalize(cell.text()).replace("|", "\\|"));
            }
            matrix.add(values);
        }
        if (matrix.isEmpty()) {
            return null;
        }

        int columns = matrix.get(0).size();
        StringBuilder sb = new StringBuilder();
        sb.append(toRow(matrix.get(0), columns)).append('\n');
        sb.append("|").append(" --- |".repeat(Math.max(1, columns)));
        for (int i = 1; i < matrix.size(); i++) {
            sb.append('\n').append(toRow(matrix.get(i), columns));
        }
        return new DocElement(DocElementType.TABLE, sb.toString(), breadcrumb(headingStack))
                .attr(DocElement.ATTR_HEADER_ROW_COUNT, 2);
    }

    private static String toRow(List<String> values, int columns) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < columns; i++) {
            sb.append(' ').append(i < values.size() ? values.get(i) : "").append(" |");
        }
        return sb.toString();
    }

    private static List<String> breadcrumb(String[] headingStack) {
        List<String> path = new ArrayList<>();
        for (int level = 1; level <= MAX_HEADING_LEVEL; level++) {
            if (headingStack[level] != null) {
                path.add(headingStack[level]);
            }
        }
        return path;
    }

    private static String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private static void add(List<DocElement> elements, DocElement element, ParseOptions limits) {
        elements.add(element);
        limits.checkElementCount(elements.size());
    }
}

package io.nebula.ai.rag.chunking.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 结构解析器（正则行扫描）
 * <p>
 * <b>为什么是行扫描而不是 AST 解析器：</b>本设计需要的全部是行级结构 ——
 * 标题层级、围栏代码块、管道表格、列表项、段落。这些用行扫描就能准确识别，
 * 引入 flexmark 一类的 AST 解析器要多背一个第三方依赖（违反依赖边界铁律），
 * 换来的是本设计用不上的内联语法树。等真的出现嵌套语法需求时再议。
 * <p>
 * <b>识别顺序是有意的：</b>围栏代码块优先于一切 —— 栏内内容不参与表格、标题、
 * 列表的识别。否则代码里的管道行会被误判成表格，注释里的井号会被误判成标题。
 * <p>
 * <b>畸形输入不抛异常：</b>未闭合的围栏按「一直到文末都是代码」处理；
 * 缺分隔行的管道行按普通段落处理。文档源里的畸形 Markdown 是常态，
 * 为它中断整批索引不划算；真正该中断的是触发安全上限（{@link ParseOptions}）的输入。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class MarkdownStructureParser implements StructureParser {

    /** 格式标识 */
    public static final String FORMAT = "markdown";

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.*\\S)\\s*$");

    private static final Pattern FENCE = Pattern.compile("^\\s*(`{3,}|~{3,})\\s*([\\w+-]*)\\s*$");

    private static final Pattern LIST_ITEM = Pattern.compile("^\\s*([-*+]|\\d+[.)])\\s+\\S.*$");

    /** 表格分隔行：至少一个中划线，只由竖线、中划线、冒号与空白组成 */
    private static final Pattern TABLE_SEPARATOR = Pattern.compile("^\\s*\\|?[\\s:|-]*-[\\s:|-]*\\|?\\s*$");

    /** 这些语言标记的围栏块归为配置而不是代码 */
    private static final Set<String> CONFIG_LANGUAGES =
            Set.of("yaml", "yml", "properties", "ini", "toml", "conf");

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
        if (content == null || content.isEmpty()) {
            return elements;
        }

        String[] lines = content.split("\n", -1);
        String[] headingStack = new String[MAX_HEADING_LEVEL + 1];
        List<String> paragraph = new ArrayList<>();

        int index = 0;
        while (index < lines.length) {
            String line = stripCarriageReturn(lines[index]);

            Matcher fence = FENCE.matcher(line);
            if (fence.matches()) {
                flushParagraph(paragraph, headingStack, elements, limits);
                index = consumeFence(lines, index, fence.group(1), fence.group(2),
                        headingStack, elements, limits);
                continue;
            }

            Matcher heading = HEADING.matcher(line);
            if (heading.matches()) {
                flushParagraph(paragraph, headingStack, elements, limits);
                int level = heading.group(1).length();
                String title = heading.group(2).trim();
                headingStack[level] = title;
                for (int deeper = level + 1; deeper <= MAX_HEADING_LEVEL; deeper++) {
                    headingStack[deeper] = null;
                }
                limits.checkDepth(level);
                add(elements, new DocElement(DocElementType.HEADING, title,
                        breadcrumb(headingStack)), limits);
                index++;
                continue;
            }

            if (isTableStart(lines, index)) {
                flushParagraph(paragraph, headingStack, elements, limits);
                index = consumeTable(lines, index, headingStack, elements, limits);
                continue;
            }

            if (LIST_ITEM.matcher(line).matches()) {
                flushParagraph(paragraph, headingStack, elements, limits);
                add(elements, new DocElement(DocElementType.LIST_ITEM, line.trim(),
                        breadcrumb(headingStack)), limits);
                index++;
                continue;
            }

            if (line.isBlank()) {
                flushParagraph(paragraph, headingStack, elements, limits);
            } else {
                paragraph.add(line.trim());
            }
            index++;
        }

        flushParagraph(paragraph, headingStack, elements, limits);
        return elements;
    }

    /**
     * 消费一个围栏代码块；未闭合时一直吃到文末
     *
     * @return 下一个待处理的行号
     */
    private int consumeFence(String[] lines, int start, String marker, String language,
                             String[] headingStack, List<DocElement> elements,
                             ParseOptions limits) {
        List<String> body = new ArrayList<>();
        int index = start + 1;
        boolean closed = false;
        char markerChar = marker.charAt(0);
        while (index < lines.length) {
            String line = stripCarriageReturn(lines[index]);
            Matcher closing = FENCE.matcher(line);
            // 闭合围栏必须是同种符号且不带语言标记
            if (closing.matches() && closing.group(1).charAt(0) == markerChar
                    && closing.group(2).isEmpty()) {
                closed = true;
                index++;
                break;
            }
            body.add(line);
            index++;
        }

        String text = String.join("\n", body);
        boolean config = language != null
                && CONFIG_LANGUAGES.contains(language.toLowerCase(Locale.ROOT));
        DocElement element = new DocElement(
                config ? DocElementType.CONFIG : DocElementType.CODE,
                text, breadcrumb(headingStack));
        if (language != null && !language.isEmpty()) {
            element.attr(DocElement.ATTR_LANGUAGE, language);
        }
        if (!closed) {
            // 记下来而不是悄悄修好：未闭合围栏往往意味着上游文档本身被截断了
            element.attr("unterminated", true);
        }
        if (!text.isBlank()) {
            add(elements, element, limits);
        }
        return index;
    }

    /**
     * 当前行是否是一张管道表格的起始行：本行以竖线开头，且下一行是分隔行
     */
    private boolean isTableStart(String[] lines, int index) {
        String line = stripCarriageReturn(lines[index]).trim();
        if (!line.startsWith("|")) {
            return false;
        }
        if (index + 1 >= lines.length) {
            return false;
        }
        String next = stripCarriageReturn(lines[index + 1]).trim();
        return next.startsWith("|") && TABLE_SEPARATOR.matcher(next).matches();
    }

    /**
     * 消费一整张表格
     *
     * @return 下一个待处理的行号
     */
    private int consumeTable(String[] lines, int start, String[] headingStack,
                             List<DocElement> elements, ParseOptions limits) {
        List<String> rows = new ArrayList<>();
        int index = start;
        while (index < lines.length) {
            String line = stripCarriageReturn(lines[index]).trim();
            if (!line.startsWith("|")) {
                break;
            }
            rows.add(line);
            index++;
        }
        DocElement element = new DocElement(DocElementType.TABLE,
                String.join("\n", rows), breadcrumb(headingStack));
        // 表头 = 首行 + 分隔行，按行切表时整体重复这两行，切出来的每片仍是合法表格
        element.attr(DocElement.ATTR_HEADER_ROW_COUNT, 2);
        add(elements, element, limits);
        return index;
    }

    private void flushParagraph(List<String> paragraph, String[] headingStack,
                                List<DocElement> elements, ParseOptions limits) {
        if (paragraph.isEmpty()) {
            return;
        }
        String text = String.join("\n", paragraph).trim();
        paragraph.clear();
        if (!text.isEmpty()) {
            add(elements, new DocElement(DocElementType.PARAGRAPH, text,
                    breadcrumb(headingStack)), limits);
        }
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

    private static void add(List<DocElement> elements, DocElement element, ParseOptions limits) {
        elements.add(element);
        limits.checkElementCount(elements.size());
    }

    private static String stripCarriageReturn(String line) {
        return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
    }
}

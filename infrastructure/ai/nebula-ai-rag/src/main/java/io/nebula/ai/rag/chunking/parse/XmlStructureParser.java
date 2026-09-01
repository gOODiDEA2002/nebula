package io.nebula.ai.rag.chunking.parse;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * XML 结构解析器（StAX，禁 DTD 与外部实体）
 * <p>
 * <b>安全是第一位的：</b>工厂上显式关闭 DTD 支持与外部实体解析。这两个开关关掉之后，
 * 实体展开炸弹（十亿笑声）与外部实体注入（读本地文件、发内网请求）都失去着力点。
 * 默认配置<b>不</b>安全，所以这里必须显式设，而不是依赖运行时默认值。
 * <p>
 * <b>切分口径：</b>只有「带文本的叶子元素」产出 RECORD，容器元素只推进面包屑。
 * 面包屑是元素路径，并把 {@code id} / {@code name} 这类识别性属性拼进当前层，
 * 否则一串重复的兄弟元素切出来的块彼此无法区分。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class XmlStructureParser implements StructureParser {

    /** 格式标识 */
    public static final String FORMAT = "xml";

    /** 拼进面包屑的识别性属性，按此顺序取第一个存在的 */
    private static final String[] IDENTITY_ATTRIBUTES = {"id", "name", "key", "code"};

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

        XMLStreamReader reader = null;
        try {
            reader = secureFactory().createXMLStreamReader(new StringReader(content));
            readAll(reader, elements, limits);
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException("XML 解析失败: " + e.getMessage(), e);
        } finally {
            closeQuietly(reader);
        }
        return elements;
    }

    /**
     * 关闭 DTD 与外部实体的 StAX 工厂
     */
    private static XMLInputFactory secureFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        return factory;
    }

    private void readAll(XMLStreamReader reader, List<DocElement> elements, ParseOptions limits)
            throws XMLStreamException {
        Deque<String> path = new ArrayDeque<>();
        // 每层一个栈帧：是否见过子元素 + 本层累积的文本
        Deque<Frame> frames = new ArrayDeque<>();

        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT -> {
                    Frame parent = frames.peek();
                    if (parent != null) {
                        parent.hasChildElement = true;
                    }
                    frames.push(new Frame());
                    path.addLast(segment(reader));
                    limits.checkDepth(path.size());
                }
                case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA -> {
                    Frame current = frames.peek();
                    if (current != null) {
                        current.text.append(reader.getText());
                    }
                }
                case XMLStreamConstants.END_ELEMENT -> {
                    Frame current = frames.isEmpty() ? null : frames.pop();
                    if (current != null) {
                        String value = current.text.toString().replaceAll("\\s+", " ").trim();
                        // 只有叶子（没有子元素）且有文本的元素才产出记录：
                        // 容器元素的文本要么是空白缩进, 要么已经被子元素各自产出过
                        if (!current.hasChildElement && !value.isEmpty()) {
                            elements.add(new DocElement(DocElementType.RECORD, value,
                                    new ArrayList<>(path)));
                            limits.checkElementCount(elements.size());
                        }
                    }
                    if (!path.isEmpty()) {
                        path.removeLast();
                    }
                }
                default -> {
                    // 注释、处理指令、文档声明等一律忽略
                }
            }
        }
    }

    /**
     * 解析栈帧
     */
    private static final class Frame {

        private boolean hasChildElement;

        private final StringBuilder text = new StringBuilder();
    }

    /**
     * 面包屑当前层：元素名，带上第一个存在的识别性属性
     */
    private static String segment(XMLStreamReader reader) {
        String name = reader.getLocalName();
        for (String attribute : IDENTITY_ATTRIBUTES) {
            String value = reader.getAttributeValue(null, attribute);
            if (value != null && !value.isBlank()) {
                return name + "[" + attribute + "=" + value.trim() + "]";
            }
        }
        return name;
    }

    private static void closeQuietly(XMLStreamReader reader) {
        if (reader == null) {
            return;
        }
        try {
            reader.close();
        } catch (XMLStreamException ignored) {
            // 关闭失败不影响已解析出的元素
        }
    }
}

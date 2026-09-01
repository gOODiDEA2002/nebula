package io.nebula.ai.rag.chunking.parse;

import java.util.ArrayList;
import java.util.List;

/**
 * JSONL 结构解析器（按行切记录）
 * <p>
 * 每个非空行是一条独立记录，各自成一个 RECORD 元素，面包屑写行号作为溯源。
 * 记录型数据的天然边界就是行，把若干条记录合并进一个块只会让每条记录的信号
 * 被邻居稀释 —— 检索「某一条记录」时，块里另外四条记录全是噪声。
 * <p>
 * <b>单行超限不在本层处理：</b>交给装箱层按分隔符递归降级，与其他超限元素同一条路径。
 * 本层不做「超长行再按 JSON 下潜」的特判 —— 那会让解析层反过来依赖装箱层的预算，
 * 两层的职责就糊在一起了。
 * <p>
 * <b>坏行不中断整批：</b>本层不校验每行是否合法 JSON。JSONL 常见于日志与导出数据，
 * 一行坏了就丢掉整个文件是不可接受的；真正的格式校验由消费方按需要做。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class JsonlStructureParser implements StructureParser {

    /** 格式标识 */
    public static final String FORMAT = "jsonl";

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
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
            }
            if (line.isBlank()) {
                continue;
            }
            elements.add(new DocElement(DocElementType.RECORD, line.trim(),
                    List.of("line " + (i + 1))));
            limits.checkElementCount(elements.size());
        }
        return elements;
    }
}

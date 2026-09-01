package io.nebula.ai.rag.chunking.parse;

import java.util.List;

/**
 * 结构解析器
 * <p>
 * 两段式切分的第一段：把某种格式的文档拆成带类型与位置路径的元素流，
 * 但<b>不做任何长度决策</b>。长度预算、原子保护、重叠回退全部属于第二段
 * （{@code chunking.pack}），这样新增一种文档格式只需新增一个解析器，
 * 装箱逻辑一行都不用改。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public interface StructureParser {

    /**
     * 支持的格式标识，如 {@code markdown} / {@code html} / {@code xml} / {@code json} / {@code jsonl}
     */
    String format();

    /**
     * 解析文档
     *
     * @param content 文档原文；null 或空串返回空表
     * @param options 安全上限，为 null 时用 {@link ParseOptions#defaults()}
     * @return 顺序元素流；无内容返回空表而不是 null
     * @throws ParseLimitExceededException 触发任一安全上限
     */
    List<DocElement> parse(String content, ParseOptions options);
}

package io.nebula.ai.rag.chunking.parse;

/**
 * 文档元素类型
 * <p>
 * 解析层的统一词汇表：各格式的解析器都把自己的语法结构映射到这几种类型上，
 * 装箱层只认这几种类型，因此装箱逻辑与文档格式完全解耦。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public enum DocElementType {

    /**
     * 标题：只用于推进面包屑，不单独成块
     */
    HEADING,

    /**
     * 普通段落
     */
    PARAGRAPH,

    /**
     * 代码块：默认原子单元，宁超限不切
     */
    CODE,

    /**
     * 表格：默认原子单元，超限时按数据行切并重复表头
     */
    TABLE,

    /**
     * 列表项：每项一个元素，由装箱层决定合并粒度
     */
    LIST_ITEM,

    /**
     * 配置片段（YAML / properties 等）
     */
    CONFIG,

    /**
     * 记录：JSON 子树、JSONL 行、XML 叶子元素等结构化记录
     */
    RECORD
}

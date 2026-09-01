package io.nebula.ai.rag.transform;

/**
 * 查询变体
 * <p>
 * 一个原始查询经改写/扩展后可能产生多个变体，每个变体自带一个权重，
 * 参与多路检索融合时最终权重 = 检索器权重 × 变体权重。
 * <p>
 * 单变体（权重 1.0）即现状语义：一个变体 × N 检索器 = 现状的 N 列表 N 权重。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class QueryVariant {

    /** 变体文本 */
    private final String text;

    /** 变体权重，默认 1.0 */
    private final double weight;

    public QueryVariant(String text) {
        this(text, 1.0);
    }

    public QueryVariant(String text, double weight) {
        this.text = text;
        this.weight = weight;
    }

    public String getText() {
        return text;
    }

    public double getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return "QueryVariant{text='" + text + "', weight=" + weight + '}';
    }
}

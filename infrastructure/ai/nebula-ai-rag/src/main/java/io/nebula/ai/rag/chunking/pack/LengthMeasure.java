package io.nebula.ai.rag.chunking.pack;

/**
 * 长度度量端口
 * <p>
 * 装箱预算按什么计量是可替换的：默认按字符数，因为它零依赖、零成本、行为可预测。
 * 真正想按 token 计量时（模型上下文预算是按 token 算的），实现一个走分词器的度量注进来即可，
 * 装箱算法本身一行都不用改。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
@FunctionalInterface
public interface LengthMeasure {

    /**
     * 度量文本长度
     *
     * @param text 文本，可为 null
     * @return 长度；null 返回 0
     */
    int length(String text);

    /**
     * 按字符数度量（默认）
     */
    static LengthMeasure chars() {
        return text -> text == null ? 0 : text.length();
    }
}

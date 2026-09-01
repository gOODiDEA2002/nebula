package io.nebula.ai.rag.chunking.parse;

/**
 * 解析安全上限
 * <p>
 * 三个上限都有默认值，调用方不配也不会失去保护。上限存在的理由是解析输入往往来自
 * 外部文档源：深度炸弹式的嵌套 JSON、几十万个元素的 XML、几百兆的单文件，
 * 都会在没有上限时把解析进程的内存吃干净。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class ParseOptions {

    /**
     * 单次解析的输入字符数上限
     */
    private int maxInputChars = 2_000_000;

    /**
     * 结构嵌套深度上限（JSON 键层级、XML 元素层级、标题层级）
     */
    private int maxDepth = 64;

    /**
     * 产出元素数量上限
     */
    private int maxElements = 100_000;

    /**
     * 默认上限
     */
    public static ParseOptions defaults() {
        return new ParseOptions();
    }

    public int getMaxInputChars() {
        return maxInputChars;
    }

    public void setMaxInputChars(int maxInputChars) {
        if (maxInputChars <= 0) {
            throw new IllegalArgumentException("maxInputChars 必须为正数");
        }
        this.maxInputChars = maxInputChars;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        if (maxDepth <= 0) {
            throw new IllegalArgumentException("maxDepth 必须为正数");
        }
        this.maxDepth = maxDepth;
    }

    public int getMaxElements() {
        return maxElements;
    }

    public void setMaxElements(int maxElements) {
        if (maxElements <= 0) {
            throw new IllegalArgumentException("maxElements 必须为正数");
        }
        this.maxElements = maxElements;
    }

    /**
     * 校验输入长度
     *
     * @throws ParseLimitExceededException 超过 {@link #getMaxInputChars()}
     */
    public void checkInput(String content) {
        if (content != null && content.length() > maxInputChars) {
            throw new ParseLimitExceededException(
                    "输入长度 " + content.length() + " 超过上限 " + maxInputChars);
        }
    }

    /**
     * 校验元素数量
     *
     * @throws ParseLimitExceededException 超过 {@link #getMaxElements()}
     */
    public void checkElementCount(int count) {
        if (count > maxElements) {
            throw new ParseLimitExceededException(
                    "元素数量 " + count + " 超过上限 " + maxElements);
        }
    }

    /**
     * 校验嵌套深度
     *
     * @throws ParseLimitExceededException 超过 {@link #getMaxDepth()}
     */
    public void checkDepth(int depth) {
        if (depth > maxDepth) {
            throw new ParseLimitExceededException(
                    "嵌套深度 " + depth + " 超过上限 " + maxDepth);
        }
    }
}

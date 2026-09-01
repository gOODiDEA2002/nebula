package io.nebula.ai.rag.chunking.parse;

/**
 * 解析超出安全上限
 * <p>
 * 触发上限时抛异常而不是静默截断：静默截断出来的块集看起来一切正常，
 * 但会缺失一部分文档内容，事后没有任何迹象可查。宁可让调用方看到一次失败，
 * 也不要让知识库里悄悄少掉半篇文档。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class ParseLimitExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ParseLimitExceededException(String message) {
        super(message);
    }
}

package io.nebula.ai.rag.pipeline;

/**
 * RAG 提示词渲染端口
 * <p>
 * 提示词是业务资产，通常有自己的模板管理与版本控制，所以做成可注入端口，
 * 框架只提供一个够用的默认实现。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
@FunctionalInterface
public interface RagPromptRenderer {

    /**
     * 渲染提示词
     *
     * @param query   用户原始查询
     * @param context 已拼接的检索上下文
     * @return 提示词全文
     */
    String render(String query, String context);
}

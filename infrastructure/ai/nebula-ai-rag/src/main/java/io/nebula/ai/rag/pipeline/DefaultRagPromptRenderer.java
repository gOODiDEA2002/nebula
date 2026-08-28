package io.nebula.ai.rag.pipeline;

/**
 * 默认提示词渲染实现
 * <p>
 * 模板可配但刻意保持简单：明确要求「只依据给定资料作答、资料不足时说明」，
 * 这两条是 RAG 生成环节最基本的护栏，缺了就会得到一本正经的编造。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class DefaultRagPromptRenderer implements RagPromptRenderer {

    private static final String DEFAULT_TEMPLATE = """
            请仅依据下列资料回答问题。资料不足以回答时，如实说明无法从资料中得到答案，不要编造。

            资料：
            %s
            问题：%s

            回答：""";

    private final String template;

    public DefaultRagPromptRenderer() {
        this(DEFAULT_TEMPLATE);
    }

    /**
     * @param template 模板，两个占位符依次为上下文与问题
     */
    public DefaultRagPromptRenderer(String template) {
        if (template == null || template.isBlank()) {
            throw new IllegalArgumentException("提示词模板不能为空");
        }
        this.template = template;
    }

    @Override
    public String render(String query, String context) {
        return String.format(template, context != null ? context : "", query);
    }
}

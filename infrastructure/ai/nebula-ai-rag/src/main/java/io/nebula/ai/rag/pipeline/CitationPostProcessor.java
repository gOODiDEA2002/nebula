package io.nebula.ai.rag.pipeline;

/**
 * 答案引用改写端口（R4 §4.3）
 * <p>
 * 对<b>完整</b>答案文本做角标、脚注、去重等引用改写，只消费 {@link ContextAssembly}。
 * 管线仅在<b>生成成功</b>（非降级摘要）时调用；流式路径只在 {@code COMPLETE} 前对拼接后的
 * 完整文本调用，{@code DELTA} 不改写。无配置键：默认 Noop Bean 零效果，用户提供 Bean 即生效（Y2）。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
@FunctionalInterface
public interface CitationPostProcessor {

    /**
     * 改写答案中的引用
     *
     * @param answer   完整答案文本
     * @param assembly 上下文组装结果（正文、入选引用、序号映射）
     * @return 改写后的答案文本
     */
    String process(String answer, ContextAssembly assembly);
}

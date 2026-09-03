package io.nebula.ai.rag.pipeline;

/**
 * 不改写答案的默认引用处理器（R4 §4.3）
 * <p>
 * 默认装配它：原样返回答案，保证既有行为零变化（Y2）。用户提供自定义 Bean 即可接管改写。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class NoopCitationPostProcessor implements CitationPostProcessor {

    @Override
    public String process(String answer, ContextAssembly assembly) {
        return answer;
    }
}

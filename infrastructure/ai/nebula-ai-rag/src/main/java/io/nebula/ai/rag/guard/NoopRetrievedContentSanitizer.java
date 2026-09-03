package io.nebula.ai.rag.guard;

import io.nebula.ai.rag.retriever.RetrievalResult;

/**
 * 不做任何清洗的默认实现（R4 §4.1）
 * <p>
 * 默认装配它：清洗默认关闭（{@code guard.sanitizer.enabled=false}），保证既有行为零变化（Y2）。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class NoopRetrievedContentSanitizer implements RetrievedContentSanitizer {

    @Override
    public RetrievalResult sanitize(RetrievalResult result) {
        return result;
    }
}

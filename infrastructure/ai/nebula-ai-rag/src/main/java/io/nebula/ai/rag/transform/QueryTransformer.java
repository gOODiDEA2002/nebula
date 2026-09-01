package io.nebula.ai.rag.transform;

import java.util.List;

/**
 * 查询改写器
 * <p>
 * 把原始查询转成一个或多个 {@link QueryVariant}。默认实现
 * {@link TrimQueryTransformer} 只做 trim 返回单变体，保持现状语义；
 * LLM 改写/多查询扩展由 nebula-ai-spring 的适配器提供。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
@FunctionalInterface
public interface QueryTransformer {

    /**
     * 改写查询
     *
     * @param rawQuery 原始查询文本（未 trim）
     * @return 变体列表；<b>不能为空</b>，返回空列表视为配置错误应抛异常
     */
    List<QueryVariant> transform(String rawQuery);
}

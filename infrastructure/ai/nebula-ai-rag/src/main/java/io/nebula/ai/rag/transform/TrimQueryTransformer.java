package io.nebula.ai.rag.transform;

import java.util.List;

/**
 * 默认查询改写器：只做 trim 返回单变体
 * <p>
 * 这是现状语义 —— 查询前后空白会影响关键词检索的分词与命中，因此 trim；
 * 权重固定 1.0，单变体 × N 检索器等价于原来的 N 路检索 N 权重。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class TrimQueryTransformer implements QueryTransformer {

    @Override
    public List<QueryVariant> transform(String rawQuery) {
        String text = rawQuery == null ? "" : rawQuery.trim();
        return List.of(new QueryVariant(text, 1.0));
    }
}

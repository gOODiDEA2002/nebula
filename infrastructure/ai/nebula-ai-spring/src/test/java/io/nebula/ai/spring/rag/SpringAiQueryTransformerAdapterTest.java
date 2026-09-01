package io.nebula.ai.spring.rag;

import io.nebula.ai.rag.transform.QueryVariant;

import org.junit.jupiter.api.Test;
import org.springframework.ai.rag.Query;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SpringAiQueryTransformerAdapter 的映射与失败直通（P5，详细设计 §4.2、§7）
 * <p>
 * 用包可见构造注入 Spring AI 侧的桩，绕过真实 LLM。
 */
class SpringAiQueryTransformerAdapterTest {

    @Test
    void rewriteResult_isMappedToSingleVariant() {
        SpringAiQueryTransformerAdapter adapter =
                new SpringAiQueryTransformerAdapter(q -> List.of(new Query("改写后的查询")));

        List<QueryVariant> variants = adapter.transform("  原始查询  ");

        assertThat(variants).hasSize(1);
        assertThat(variants.get(0).getText()).isEqualTo("改写后的查询");
        assertThat(variants.get(0).getWeight()).isEqualTo(1.0);
    }

    @Test
    void multiQueryResult_isMappedToMultipleVariants() {
        SpringAiQueryTransformerAdapter adapter = new SpringAiQueryTransformerAdapter(
                q -> List.of(new Query(q.text()), new Query("变体一"), new Query("变体二")));

        List<QueryVariant> variants = adapter.transform("原问题");

        assertThat(variants).extracting(QueryVariant::getText)
                .containsExactly("原问题", "变体一", "变体二");
    }

    @Test
    void llmFailure_passesThroughOriginalQueryWithoutThrowing() {
        SpringAiQueryTransformerAdapter adapter = new SpringAiQueryTransformerAdapter(q -> {
            throw new RuntimeException("LLM 不可用");
        });

        List<QueryVariant> variants = adapter.transform("  空气开关  ");

        // 直通原查询（trim 后），不抛异常
        assertThat(variants).hasSize(1);
        assertThat(variants.get(0).getText()).isEqualTo("空气开关");
    }

    @Test
    void repeatedFailure_stillPassesThrough() {
        SpringAiQueryTransformerAdapter adapter = new SpringAiQueryTransformerAdapter(q -> {
            throw new RuntimeException("LLM 不可用");
        });

        assertThat(adapter.transform("q1")).extracting(QueryVariant::getText).containsExactly("q1");
        assertThat(adapter.transform("q2")).extracting(QueryVariant::getText).containsExactly("q2");
    }

    @Test
    void emptyResult_fallsBackToOriginalQuery() {
        SpringAiQueryTransformerAdapter adapter =
                new SpringAiQueryTransformerAdapter(q -> List.of());

        assertThat(adapter.transform("  原始  ")).extracting(QueryVariant::getText)
                .containsExactly("原始");
    }

    @Test
    void blankOnlyResults_fallBackToOriginalQuery() {
        SpringAiQueryTransformerAdapter adapter =
                new SpringAiQueryTransformerAdapter(q -> List.of(new Query("   ")));

        assertThat(adapter.transform("原始")).extracting(QueryVariant::getText)
                .containsExactly("原始");
    }
}

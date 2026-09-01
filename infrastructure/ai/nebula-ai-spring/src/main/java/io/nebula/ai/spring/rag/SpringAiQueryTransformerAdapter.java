package io.nebula.ai.spring.rag;

import io.nebula.ai.rag.transform.QueryTransformer;
import io.nebula.ai.rag.transform.QueryVariant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Spring AI 查询改写适配器（P5，详细设计 §4.2）
 * <p>
 * 把 Spring AI 的 {@link RewriteQueryTransformer}（mode=rewrite）或 {@link MultiQueryExpander}
 * （mode=multi-query，{@code includeOriginal=true} 保底）包装成本框架的 {@link QueryTransformer}。
 * <p>
 * <b>LLM 失败或超时直通原查询（warn 一次）：</b>改写依赖大模型，模型不可用时不能让整条 RAG 挂掉，
 * 退回「原查询单变体」即等价于未改写。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class SpringAiQueryTransformerAdapter implements QueryTransformer {

    private static final Logger log = LoggerFactory.getLogger(SpringAiQueryTransformerAdapter.class);

    /** 统一成「原查询 → 若干查询」的函数，屏蔽 rewrite 与 multi-query 的差异 */
    private final Function<Query, List<Query>> delegate;

    private final AtomicBoolean warned = new AtomicBoolean(false);

    /** 包可见构造：便于测试注入桩，绕过真实 LLM */
    SpringAiQueryTransformerAdapter(Function<Query, List<Query>> delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate 不能为空");
        }
        this.delegate = delegate;
    }

    /**
     * mode=rewrite：包装 {@link RewriteQueryTransformer}（单查询改写）
     */
    public static SpringAiQueryTransformerAdapter rewrite(ChatClient.Builder chatClientBuilder) {
        RewriteQueryTransformer transformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();
        return new SpringAiQueryTransformerAdapter(query -> List.of(transformer.transform(query)));
    }

    /**
     * mode=multi-query：包装 {@link MultiQueryExpander}，强制 {@code includeOriginal=true} 保底
     */
    public static SpringAiQueryTransformerAdapter multiQuery(ChatClient.Builder chatClientBuilder) {
        MultiQueryExpander expander = MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder)
                .includeOriginal(true)
                .build();
        return new SpringAiQueryTransformerAdapter(expander::expand);
    }

    @Override
    public List<QueryVariant> transform(String rawQuery) {
        String trimmed = rawQuery == null ? "" : rawQuery.trim();
        try {
            List<Query> queries = delegate.apply(new Query(trimmed));
            if (queries == null || queries.isEmpty()) {
                return List.of(new QueryVariant(trimmed, 1.0));
            }
            List<QueryVariant> variants = queries.stream()
                    .filter(q -> q != null && q.text() != null && !q.text().isBlank())
                    .map(q -> new QueryVariant(q.text(), 1.0))
                    .toList();
            // 全部被过滤空 → 退回原查询，保证不返回空列表（契约要求）
            return variants.isEmpty() ? List.of(new QueryVariant(trimmed, 1.0)) : variants;
        } catch (Exception e) {
            if (warned.compareAndSet(false, true)) {
                log.warn("查询改写失败, 直通原查询(后续同类失败不再重复告警): {}", e.getMessage());
            }
            return List.of(new QueryVariant(trimmed, 1.0));
        }
    }
}

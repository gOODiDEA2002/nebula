package io.nebula.ai.spring.rag;

import io.nebula.ai.rag.transform.QueryTransformer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.context.annotation.Bean;

/**
 * Spring AI 查询改写器装配（P5，详细设计 §4.2）
 * <p>
 * 仅当 classpath 同时具备本框架 {@link QueryTransformer}、Spring AI 的 {@link RewriteQueryTransformer}
 * 与 {@link ChatClient} 时才生效。按 {@code nebula.ai.rag.transform.mode} 装配对应改写器：
 * <ul>
 *   <li>{@code rewrite} → 包装 {@code RewriteQueryTransformer}；</li>
 *   <li>{@code multi-query} → 包装 {@code MultiQueryExpander}（includeOriginal=true）。</li>
 * </ul>
 * <b>快速失败：</b>显式配了 {@code rewrite}/{@code multi-query} 但缺 {@link ChatClient.Builder} Bean，
 * 直接抛异常，不静默降级为 trim；若缺 spring-ai-rag 类，则本配置整体不加载，由
 * {@code RagAutoConfiguration} 的默认 {@code QueryTransformer} 兜底抛出明确失败。
 * <p>
 * {@code @AutoConfigureBefore} 用类名引用 {@code RagAutoConfiguration}（避免对 autoconfigure 模块的硬依赖），
 * 保证本适配器先注册，从而覆盖其默认 {@code TrimQueryTransformer}。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
@AutoConfiguration
@AutoConfigureBefore(name = "io.nebula.autoconfigure.ai.RagAutoConfiguration")
@ConditionalOnClass({ QueryTransformer.class, RewriteQueryTransformer.class, ChatClient.class })
public class SpringAiQueryTransformerAutoConfiguration {

    private static final Logger log =
            LoggerFactory.getLogger(SpringAiQueryTransformerAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "nebula.ai.rag.transform", name = "mode", havingValue = "rewrite")
    @ConditionalOnMissingBean(QueryTransformer.class)
    public QueryTransformer rewriteQueryTransformer(
            ObjectProvider<ChatClient.Builder> chatClientBuilder) {
        ChatClient.Builder builder = requireChatClientBuilder(chatClientBuilder, "rewrite");
        log.info("配置 Nebula SpringAiQueryTransformerAdapter (mode=rewrite)");
        return SpringAiQueryTransformerAdapter.rewrite(builder);
    }

    @Bean
    @ConditionalOnProperty(prefix = "nebula.ai.rag.transform", name = "mode", havingValue = "multi-query")
    @ConditionalOnMissingBean(QueryTransformer.class)
    public QueryTransformer multiQueryTransformer(
            ObjectProvider<ChatClient.Builder> chatClientBuilder) {
        ChatClient.Builder builder = requireChatClientBuilder(chatClientBuilder, "multi-query");
        log.info("配置 Nebula SpringAiQueryTransformerAdapter (mode=multi-query)");
        return SpringAiQueryTransformerAdapter.multiQuery(builder);
    }

    private ChatClient.Builder requireChatClientBuilder(
            ObjectProvider<ChatClient.Builder> provider, String mode) {
        ChatClient.Builder builder = provider.getIfAvailable();
        if (builder == null) {
            throw new IllegalStateException(
                    "nebula.ai.rag.transform.mode=" + mode + " 需要 ChatClient.Builder Bean，"
                            + "但容器内没有; 请配置 Spring AI 的 ChatModel（如 spring.ai.openai.*），"
                            + "或改回 nebula.ai.rag.transform.mode=none");
        }
        return builder;
    }
}

package io.nebula.autoconfigure.ai;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.core.embedding.EmbeddingService;
import io.nebula.ai.core.vectorstore.VectorStoreService;
import io.nebula.ai.rag.chunking.TextChunker;
import io.nebula.ai.rag.config.RagProperties;
import io.nebula.ai.rag.fusion.FusionStrategy;
import io.nebula.ai.rag.fusion.RrfFusionStrategy;
import io.nebula.ai.rag.pipeline.AnswerGenerator;
import io.nebula.ai.rag.pipeline.ChatServiceAnswerGenerator;
import io.nebula.ai.rag.pipeline.ContextAssembler;
import io.nebula.ai.rag.pipeline.DefaultRagPipeline;
import io.nebula.ai.rag.pipeline.DefaultRagPromptRenderer;
import io.nebula.ai.rag.pipeline.HybridRetrievalEngine;
import io.nebula.ai.rag.pipeline.RagPipeline;
import io.nebula.ai.rag.pipeline.RagPromptRenderer;
import io.nebula.ai.rag.chunking.pack.PackOptions;
import io.nebula.ai.rag.chunking.parse.JsonStructureParser;
import io.nebula.ai.rag.chunking.parse.JsonlStructureParser;
import io.nebula.ai.rag.chunking.parse.MarkdownStructureParser;
import io.nebula.ai.rag.chunking.parse.StructureParser;
import io.nebula.ai.rag.chunking.parse.XmlStructureParser;
import io.nebula.ai.rag.index.CollectionSwitcher;
import io.nebula.ai.rag.index.DocumentSource;
import io.nebula.ai.rag.index.IndexPlanner;
import io.nebula.ai.rag.index.IndexSink;
import io.nebula.ai.rag.index.IndexStateRepository;
import io.nebula.ai.rag.index.IndexTargetFactory;
import io.nebula.ai.rag.index.IndexingPipeline;
import io.nebula.ai.rag.index.InMemoryIndexStateRepository;
import io.nebula.ai.rag.index.ReindexPipeline;
import io.nebula.ai.rag.index.ReindexTarget;
import io.nebula.ai.rag.index.SearchServiceCollectionSwitcher;
import io.nebula.ai.rag.index.SearchServiceIndexSink;
import io.nebula.ai.rag.index.SearchServiceIndexTargetFactory;
import io.nebula.ai.rag.index.VectorStoreIndexSink;
import io.nebula.ai.spring.config.AIProperties;
import io.nebula.ai.spring.config.VectorStoreProperties;
import io.nebula.ai.spring.vectorstore.QdrantCollectionSwitcher;
import io.nebula.ai.spring.vectorstore.QdrantIndexTargetFactory;
import io.qdrant.client.QdrantClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import io.nebula.ai.rag.rerank.NoopReranker;
import io.nebula.ai.rag.rerank.Reranker;
import io.nebula.ai.rag.retriever.Retriever;
import io.nebula.ai.rag.retriever.SearchServiceRetriever;
import io.nebula.ai.rag.retriever.VectorStoreRetriever;
import io.nebula.ai.rag.transform.QueryTransformer;
import io.nebula.ai.rag.transform.TrimQueryTransformer;
import io.nebula.core.common.diagnostic.NebulaComponentSummary;
import io.nebula.core.common.diagnostic.SimpleComponentSummary;
import io.nebula.search.core.SearchService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * Nebula RAG 自动配置类
 * <p>
 * Level 3 默认关闭（{@code nebula.ai.rag.enabled=true} 才启动）：RAG 依赖向量库等
 * 外部服务，引入 JAR 不该造成任何远程连接。starter-ai 也不预置开启。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
@AutoConfiguration(after = AIAutoConfiguration.class)
@ConditionalOnClass({ RagPipeline.class, Retriever.class })
@ConditionalOnProperty(prefix = "nebula.ai.rag", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(RagProperties.class)
public class RagAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RagAutoConfiguration.class);

    public RagAutoConfiguration() {
        log.info("Nebula AI RAG 模块自动配置已启用");
    }

    /**
     * 基于向量存储的默认检索器
     * <p>
     * 权重固定为 1.0：权重是检索器自身的特性，多路检索场景下由应用提供
     * 各自带权重的 Retriever Bean 覆盖或补充，框架不为此额外开一组配置项。
     */
    @Bean
    @Order(10)
    @ConditionalOnBean(VectorStoreService.class)
    @ConditionalOnMissingBean(VectorStoreRetriever.class)
    public VectorStoreRetriever vectorStoreRetriever(VectorStoreService vectorStoreService) {
        log.info("配置 Nebula VectorStoreRetriever");
        return new VectorStoreRetriever(vectorStoreService, 1.0, 0.0);
    }

    /**
     * RRF 融合策略
     */
    @Bean
    @ConditionalOnMissingBean(FusionStrategy.class)
    public FusionStrategy rrfFusionStrategy(RagProperties properties) {
        RagProperties.Fusion fusion = properties.getFusion();
        log.info("配置 Nebula RrfFusionStrategy - rrfK: {}, sourcePriority: {}",
                fusion.getRrfK(), fusion.getSourcePriority());
        return new RrfFusionStrategy(fusion.getRrfK(), fusion.getSourcePriority());
    }

    /**
     * 多路并行检索引擎
     * <p>
     * 容器里一个 {@link Retriever} 都没有时直接启动失败：这种情况下管线只会永远返回
     * 「没找到相关内容」，是配置事故而不是可用状态，藏到运行期才暴露代价更大。
     */
    @Bean
    @ConditionalOnMissingBean(HybridRetrievalEngine.class)
    public HybridRetrievalEngine hybridRetrievalEngine(ObjectProvider<Retriever> retrievers,
                                                       FusionStrategy fusionStrategy,
                                                       RagProperties properties) {
        List<Retriever> ordered = retrievers.orderedStream().toList();
        if (ordered.isEmpty()) {
            throw new IllegalStateException(
                    "nebula.ai.rag.enabled=true 但容器内没有任何 Retriever Bean; "
                            + "请提供至少一个 io.nebula.ai.rag.retriever.Retriever 实现, "
                            + "或启用 VectorStoreService 以装配默认的 VectorStoreRetriever");
        }
        RagProperties.Retrieval retrieval = properties.getRetrieval();
        log.info("配置 Nebula HybridRetrievalEngine - 检索器: {}, 候选放大: {}x, 默认超时: {}s",
                ordered.stream().map(Retriever::getName).toList(),
                retrieval.getCandidateMultiplier(), retrieval.getTimeoutSeconds());
        return new HybridRetrievalEngine(ordered, fusionStrategy,
                retrieval.getCandidateMultiplier(), retrieval.getTimeoutSeconds() * 1000L);
    }

    /**
     * 默认重排序器：只截断不排序
     */
    @Bean
    @ConditionalOnMissingBean(Reranker.class)
    public Reranker noopReranker() {
        log.info("配置 Nebula NoopReranker (未提供 Reranker 实现, 仅按融合顺序截断)");
        return new NoopReranker();
    }

    /**
     * 上下文拼接器
     */
    @Bean
    @ConditionalOnMissingBean(ContextAssembler.class)
    public ContextAssembler contextAssembler(RagProperties properties) {
        RagProperties.Context context = properties.getContext();
        log.info("配置 Nebula ContextAssembler - 上下文预算: {} 字符", context.getMaxLength());
        return new ContextAssembler(context.getMaxLength(), context.getDocumentTemplate());
    }

    /**
     * 默认提示词渲染器
     */
    @Bean
    @ConditionalOnMissingBean(RagPromptRenderer.class)
    public RagPromptRenderer ragPromptRenderer() {
        return new DefaultRagPromptRenderer();
    }

    /**
     * 默认答案生成器：走 ChatService
     */
    @Bean
    @ConditionalOnBean(ChatService.class)
    @ConditionalOnMissingBean(AnswerGenerator.class)
    public AnswerGenerator defaultAnswerGenerator(ChatService chatService) {
        log.info("配置 Nebula ChatServiceAnswerGenerator");
        return new ChatServiceAnswerGenerator(chatService);
    }

    /**
     * 默认查询改写器：{@link TrimQueryTransformer}（现状语义）
     * <p>
     * 仅当没有其它 {@link QueryTransformer} Bean 时装配。{@code transform.mode} 若被显式配成
     * {@code rewrite}/{@code multi-query}，必须由 nebula-ai-spring 的适配器提供对应改写器；
     * 此时本默认 Bean 直接抛异常快速失败，绝不静默退回 trim（详细设计 §4.2）。
     */
    @Bean
    @ConditionalOnMissingBean(QueryTransformer.class)
    public QueryTransformer queryTransformer(RagProperties properties) {
        String mode = properties.getTransform().getMode();
        if (mode != null && !"none".equalsIgnoreCase(mode.trim())) {
            throw new IllegalStateException(
                    "nebula.ai.rag.transform.mode=" + mode + " 需要 nebula-ai-spring 的查询改写器适配"
                            + "（spring-ai-rag + ChatClient.Builder），但容器内没有对应 QueryTransformer Bean; "
                            + "请确认已引入 nebula-ai-spring 且 spring-ai-rag 在 classpath 且存在 ChatClient.Builder, "
                            + "或改回 nebula.ai.rag.transform.mode=none");
        }
        log.info("配置 Nebula TrimQueryTransformer (transform.mode=none)");
        return new TrimQueryTransformer();
    }

    /**
     * RAG 管线
     */
    @Bean
    @ConditionalOnBean({ HybridRetrievalEngine.class, AnswerGenerator.class })
    @ConditionalOnMissingBean(RagPipeline.class)
    public RagPipeline ragPipeline(HybridRetrievalEngine hybridRetrievalEngine,
                                   Reranker reranker,
                                   ContextAssembler contextAssembler,
                                   RagPromptRenderer ragPromptRenderer,
                                   AnswerGenerator answerGenerator,
                                   RagProperties properties,
                                   QueryTransformer queryTransformer) {
        log.info("配置 Nebula RagPipeline - 重排: {}, 生成超时: {}ms, 改写器: {}",
                properties.getRerank().isEnabled(), properties.getGeneration().getTimeoutMs(),
                queryTransformer.getClass().getSimpleName());
        return new DefaultRagPipeline(hybridRetrievalEngine, reranker, contextAssembler,
                ragPromptRenderer, answerGenerator, properties, queryTransformer);
    }

    /**
     * 文本分块器
     */
    @Bean
    @ConditionalOnMissingBean(TextChunker.class)
    public TextChunker textChunker(RagProperties properties) {
        RagProperties.Chunking chunking = properties.getChunking();
        return new TextChunker(chunking.getSize(), chunking.getOverlap());
    }

    /**
     * 组件摘要: RAG
     */
    @Bean
    NebulaComponentSummary ragSummary(RagProperties properties,
                                      ObjectProvider<Retriever> retrievers) {
        var details = new java.util.LinkedHashMap<String, String>();
        details.put("Retrievers", retrievers.orderedStream().map(Retriever::getName).toList().toString());
        details.put("Top K", String.valueOf(properties.getRetrieval().getTopK()));
        details.put("Candidate Multiplier", String.valueOf(properties.getRetrieval().getCandidateMultiplier()));
        details.put("RRF K", String.valueOf(properties.getFusion().getRrfK()));
        details.put("Source Priority", properties.getFusion().getSourcePriority().toString());
        details.put("Rerank", properties.getRerank().isEnabled()
                + " (topK=" + properties.getRerank().getTopK() + ")");
        details.put("Context Max Length", String.valueOf(properties.getContext().getMaxLength()));
        details.put("Generation Timeout", properties.getGeneration().getTimeoutMs() + "ms");
        return new SimpleComponentSummary("AI", "RAG", true, 901, details);
    }

    /**
     * BM25 关键词检索路装配（P4b，详细设计 §3.3）
     * <p>
     * 需同时满足：classpath 有 {@link SearchService} 类、容器内有 {@link SearchService} Bean、
     * {@code nebula.ai.rag.search.index-name} 非空。三者缺一即不装配（默认行为不变）。
     * <p>
     * 检索器实现 {@link Ordered}，顺序取 {@code nebula.ai.rag.search.order}（默认 20），
     * 排在框架默认向量检索器（{@code @Order(10)}）之后。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(SearchService.class)
    @Conditional(SearchIndexNamePresentCondition.class)
    static class SearchRetrieverConfiguration {

        @Bean
        @ConditionalOnBean(SearchService.class)
        @ConditionalOnMissingBean(SearchServiceRetriever.class)
        SearchServiceRetriever searchServiceRetriever(SearchService searchService,
                                                      RagProperties properties) {
            RagProperties.Search search = properties.getSearch();
            log.info("配置 Nebula SearchServiceRetriever - 索引: {}, 权重: {}, 顺序: {}",
                    search.getIndexName(), search.getWeight(), search.getOrder());
            return new OrderedSearchServiceRetriever(searchService, search.getIndexName(),
                    search.getWeight(), search.getOrder());
        }
    }

    /**
     * 带顺序的 {@link SearchServiceRetriever}：让 {@code search.order} 在 {@code orderedStream}
     * 中生效，同时把「Spring 顺序」这个关注点挡在框架无关的检索器类之外。
     */
    static final class OrderedSearchServiceRetriever extends SearchServiceRetriever implements Ordered {

        private final int order;

        OrderedSearchServiceRetriever(SearchService searchService, String indexName, double weight,
                                      int order) {
            super(searchService, indexName, weight);
            this.order = order;
        }

        @Override
        public int getOrder() {
            return order;
        }
    }

    /**
     * {@code nebula.ai.rag.search.index-name} 非空才成立
     * <p>
     * {@code @ConditionalOnProperty} 无法区分空串与真实值（空串会被判为「已设置」），
     * 因此用自定义 Condition 显式要求非空。
     */
    static class SearchIndexNamePresentCondition implements org.springframework.context.annotation.Condition {

        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                               org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            String indexName = context.getEnvironment()
                    .getProperty("nebula.ai.rag.search.index-name");
            return indexName != null && !indexName.isBlank();
        }
    }

    /**
     * 索引治理装配（P2-min，详细设计 §2.4）
     * <p>
     * 需 {@code nebula.ai.rag.indexing.enabled=true}。{@link IndexingPipeline} 还需容器内已有
     * {@link DocumentSource} 与 {@link IndexStateRepository}（{@code @ConditionalOnBean}，缺任一即不装配）——
     * 持续增量场景缺持久化状态库即无 Bean，业务侧注入点在编译期暴露，等价「启动快速失败」。
     * <p>
     * {@code InMemoryIndexStateRepository} 有意不进自动装配（上位 DS6）：它重启即失忆，
     * 无法支撑删除对齐，只能由应用显式提供或用于测试。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "nebula.ai.rag.indexing", name = "enabled", havingValue = "true")
    static class IndexingConfiguration {

        @Bean
        @ConditionalOnMissingBean(IndexPlanner.class)
        IndexPlanner indexPlanner() {
            return new IndexPlanner();
        }

        // ---- 内置结构解析器（应用可覆盖或补充）----

        @Bean
        @ConditionalOnMissingBean(MarkdownStructureParser.class)
        MarkdownStructureParser markdownStructureParser() {
            return new MarkdownStructureParser();
        }

        @Bean
        @ConditionalOnMissingBean(JsonStructureParser.class)
        JsonStructureParser jsonStructureParser() {
            return new JsonStructureParser();
        }

        @Bean
        @ConditionalOnMissingBean(JsonlStructureParser.class)
        JsonlStructureParser jsonlStructureParser() {
            return new JsonlStructureParser();
        }

        @Bean
        @ConditionalOnMissingBean(XmlStructureParser.class)
        XmlStructureParser xmlStructureParser() {
            return new XmlStructureParser();
        }

        // ---- 内置写目标 ----

        @Bean
        @ConditionalOnBean(VectorStoreService.class)
        @ConditionalOnMissingBean(VectorStoreIndexSink.class)
        VectorStoreIndexSink vectorStoreIndexSink(VectorStoreService vectorStoreService) {
            log.info("配置 Nebula VectorStoreIndexSink");
            return new VectorStoreIndexSink(vectorStoreService);
        }

        @Bean
        @ConditionalOnClass(SearchService.class)
        @ConditionalOnBean(SearchService.class)
        @ConditionalOnMissingBean(SearchServiceIndexSink.class)
        @Conditional(IndexingSearchIndexNamePresentCondition.class)
        SearchServiceIndexSink searchServiceIndexSink(SearchService searchService,
                                                      RagProperties properties) {
            RagProperties.Indexing indexing = properties.getIndexing();
            RagProperties.Search search = properties.getSearch();
            log.info("配置 Nebula SearchServiceIndexSink - 索引: {}", indexing.getSearchIndexName());
            return new SearchServiceIndexSink(searchService, indexing.getSearchIndexName(),
                    search.getAnalyzer(), search.getSearchAnalyzer());
        }

        @Bean
        @ConditionalOnBean({ DocumentSource.class, IndexStateRepository.class })
        @ConditionalOnMissingBean(IndexingPipeline.class)
        IndexingPipeline indexingPipeline(ObjectProvider<StructureParser> parsers,
                                          ObjectProvider<IndexSink> sinks,
                                          IndexStateRepository stateRepository,
                                          IndexPlanner indexPlanner,
                                          RagProperties properties) {
            List<StructureParser> parserList = parsers.orderedStream().toList();
            List<IndexSink> sinkList = sinks.orderedStream().toList();
            RagProperties.Chunking chunking = properties.getChunking();
            PackOptions packOptions = new PackOptions();
            packOptions.setMaxChunkSize(chunking.getSize());
            packOptions.setOverlap(chunking.getOverlap());
            packOptions.setCodeSummaryToContent(chunking.isCodeSummary());
            log.info("配置 Nebula IndexingPipeline - 解析器: {}, 写目标: {}",
                    parserList.stream().map(StructureParser::format).toList(),
                    sinkList.stream().map(IndexSink::name).toList());
            return new IndexingPipeline(parserList, packOptions, sinkList, stateRepository,
                    indexPlanner);
        }
    }

    /**
     * 状态库缺席快速失败守卫（R3 §7，补强 R2 的「静默不装配」）
     * <p>
     * 启用 {@code nebula.ai.rag.indexing} 并提供了 {@link DocumentSource}，却没有持久化
     * {@link IndexStateRepository} 时启动快速失败，指向真实原因而非藏到运行期的「找不到 Bean」。
     * <p>
     * 四态：
     * <ul>
     *   <li>有持久化状态库 → 正常放行；</li>
     *   <li>缺状态库 + {@code fail-fast-without-state-repository=true}（默认）→ 构造即抛，启动失败；</li>
     *   <li>缺状态库 + 检查关闭 → warn 一次放行（仅一次性任务）；</li>
     *   <li>状态库是 {@link InMemoryIndexStateRepository}（用户明示选择）→ 不失败，但 warn 其重启即失忆的局限。</li>
     * </ul>
     * 无 {@link DocumentSource} 时不构造本守卫（没有源就没有增量任务，无需状态库）。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "nebula.ai.rag.indexing", name = "enabled", havingValue = "true")
    static class IndexingStateGuardConfiguration {

        @Bean
        @ConditionalOnBean(DocumentSource.class)
        IndexStateRepositoryGuard indexStateRepositoryGuard(
                ObjectProvider<IndexStateRepository> stateRepositories, RagProperties properties) {
            IndexStateRepository repository = stateRepositories.getIfAvailable();
            boolean failFast = properties.getIndexing().isFailFastWithoutStateRepository();
            if (repository == null) {
                if (failFast) {
                    throw new IllegalStateException(
                            "已启用 nebula.ai.rag.indexing 并提供了 DocumentSource, 但容器内没有 "
                                    + "IndexStateRepository。持续增量与删除对齐依赖持久化状态; 请提供实现, "
                                    + "或仅用于一次性任务时显式声明 InMemoryIndexStateRepository Bean(重启即失忆), "
                                    + "或置 nebula.ai.rag.indexing.fail-fast-without-state-repository=false 关闭本检查");
                }
                log.warn("nebula.ai.rag.indexing 已启用且有 DocumentSource, 但缺 IndexStateRepository; "
                        + "fail-fast-without-state-repository=false 已关闭启动检查, 增量与删除对齐将不可用");
            } else if (repository instanceof InMemoryIndexStateRepository) {
                log.warn("索引状态库是 InMemoryIndexStateRepository: 重启即失忆, 无法支撑删除对齐; "
                        + "仅适用于测试与一次性任务, 持续增量请换持久化实现");
            }
            return new IndexStateRepositoryGuard();
        }
    }

    /**
     * 状态库守卫标记 Bean：其存在与否本身没有语义，语义在
     * {@link IndexingStateGuardConfiguration#indexStateRepositoryGuard} 的构造校验里。
     */
    static final class IndexStateRepositoryGuard {
    }

    /**
     * {@code nebula.ai.rag.indexing.search-index-name} 非空才成立（同 §3.3 的空串区分问题）
     */
    static class IndexingSearchIndexNamePresentCondition
            implements org.springframework.context.annotation.Condition {

        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                               org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            String indexName = context.getEnvironment()
                    .getProperty("nebula.ai.rag.indexing.search-index-name");
            return indexName != null && !indexName.isBlank();
        }
    }

    // ==================================================================
    // R3：版本化重灌与蓝绿切换装配（详细设计 §3.3、§4、§7）
    // ==================================================================

    /**
     * 重灌装配（R3）：{@code nebula.ai.rag.indexing.reindex.enabled=true} 且至少一个别名键非空才激活。
     * <p>
     * 向量侧目标需 classpath 有 Qdrant 类 + {@code vector-alias} 非空 + 容器有 {@link QdrantClient} 等 Bean；
     * BM25 侧目标需 classpath 有 {@link SearchService} 类 + {@code search-alias} 非空 + 容器有
     * {@link SearchService} Bean。{@link ReindexPipeline} 需容器有 {@link DocumentSource} 与
     * {@link IndexStateRepository}，并按 {@code switch-order} 排序目标。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "nebula.ai.rag.indexing.reindex", name = "enabled",
            havingValue = "true")
    @Conditional(ReindexAliasPresentCondition.class)
    static class ReindexConfiguration {

        /**
         * 向量侧重灌目标：Qdrant 集合切换器 + 写目标工厂
         */
        @Configuration(proxyBeanMethods = false)
        @ConditionalOnClass(QdrantVectorStore.class)
        @Conditional(VectorAliasPresentCondition.class)
        static class VectorReindexConfiguration {

            @Bean
            @ConditionalOnBean({ QdrantClient.class, EmbeddingModel.class, EmbeddingService.class })
            @ConditionalOnMissingBean(name = "reindexVectorTarget")
            ReindexTarget reindexVectorTarget(QdrantClient nebulaQdrantClient,
                                              EmbeddingModel embeddingModel,
                                              EmbeddingService embeddingService,
                                              ObjectProvider<VectorStoreProperties> vectorStoreProperties,
                                              ObjectProvider<AIProperties> aiProperties,
                                              RagProperties ragProperties) {
                RagProperties.Indexing.Reindex reindex = ragProperties.getIndexing().getReindex();
                boolean idMappingEnabled = false;
                String namespaceName = null;
                String originalDocIdField = null;
                long timeoutSeconds = 30;
                AIProperties ai = aiProperties.getIfAvailable();
                if (ai != null) {
                    AIProperties.QdrantProperties qdrant = ai.getVectorStore().getQdrant();
                    AIProperties.QdrantProperties.IdMapping idMapping = qdrant.getIdMapping();
                    idMappingEnabled = idMapping.isEnabled();
                    namespaceName = idMapping.getNamespaceName();
                    originalDocIdField = idMapping.getOriginalDocIdField();
                    timeoutSeconds = qdrant.getTimeoutSeconds();
                }
                CollectionSwitcher switcher = new QdrantCollectionSwitcher(nebulaQdrantClient,
                        reindex.getVectorAlias(), reindex.getVectorDimension(),
                        reindex.getVectorDistance(), timeoutSeconds);
                IndexTargetFactory factory = new QdrantIndexTargetFactory(nebulaQdrantClient,
                        embeddingModel, embeddingService, vectorStoreProperties.getIfAvailable(),
                        idMappingEnabled, namespaceName, originalDocIdField);
                log.info("配置 Nebula 重灌向量目标 - 别名: {}, 维度: {}, 距离: {}",
                        reindex.getVectorAlias(), reindex.getVectorDimension(),
                        reindex.getVectorDistance());
                return new ReindexTarget(switcher, factory, reindex.getVectorAlias());
            }
        }

        /**
         * BM25 侧重灌目标：SearchService 索引切换器 + 写目标工厂
         */
        @Configuration(proxyBeanMethods = false)
        @ConditionalOnClass(SearchService.class)
        @Conditional(SearchAliasPresentCondition.class)
        static class SearchReindexConfiguration {

            @Bean
            @ConditionalOnBean(SearchService.class)
            @ConditionalOnMissingBean(name = "reindexSearchTarget")
            ReindexTarget reindexSearchTarget(SearchService searchService,
                                              RagProperties ragProperties) {
                RagProperties.Indexing.Reindex reindex = ragProperties.getIndexing().getReindex();
                RagProperties.Search search = ragProperties.getSearch();
                CollectionSwitcher switcher = new SearchServiceCollectionSwitcher(searchService,
                        reindex.getSearchAlias(), search.getAnalyzer(), search.getSearchAnalyzer());
                IndexTargetFactory factory = new SearchServiceIndexTargetFactory(searchService,
                        search.getAnalyzer(), search.getSearchAnalyzer());
                log.info("配置 Nebula 重灌 BM25 目标 - 别名: {}", reindex.getSearchAlias());
                return new ReindexTarget(switcher, factory, reindex.getSearchAlias());
            }
        }

        /**
         * 重灌管线：按 {@code switch-order} 排序目标后组装。
         * <p>
         * {@code reindex.enabled=true} 且配了别名却没有任何可用切换目标（缺 Qdrant/SearchService
         * 的 Bean 或类）时启动快速失败 —— 显式配置却不可满足不能静默跳过。
         */
        @Bean
        @ConditionalOnBean({ DocumentSource.class, IndexStateRepository.class })
        @ConditionalOnMissingBean(ReindexPipeline.class)
        ReindexPipeline reindexPipeline(DocumentSource source,
                                        ObjectProvider<ReindexTarget> targetProvider,
                                        IndexStateRepository stateRepository,
                                        ObjectProvider<IndexPlanner> plannerProvider,
                                        ObjectProvider<StructureParser> parserProvider,
                                        RagProperties properties) {
            RagProperties.Indexing.Reindex reindex = properties.getIndexing().getReindex();
            List<ReindexTarget> targets = orderBySwitchOrder(
                    targetProvider.orderedStream().toList(), reindex.getSwitchOrder());
            if (targets.isEmpty()) {
                throw new IllegalStateException(
                        "nebula.ai.rag.indexing.reindex.enabled=true 且配置了别名, 但容器内没有可用的重灌切换目标; "
                                + "请确认: vector-alias 对应的 Qdrant(客户端 Bean 与 QdrantVectorStore 类)可用, "
                                + "或 search-alias 对应的 SearchService(Bean 与类)可用");
            }
            List<StructureParser> parsers = parserProvider.orderedStream().toList();
            if (parsers.isEmpty()) {
                parsers = List.of(new MarkdownStructureParser(), new JsonStructureParser(),
                        new JsonlStructureParser(), new XmlStructureParser());
            }
            IndexPlanner planner = plannerProvider.getIfAvailable(IndexPlanner::new);
            RagProperties.Chunking chunking = properties.getChunking();
            PackOptions packOptions = new PackOptions();
            packOptions.setMaxChunkSize(chunking.getSize());
            packOptions.setOverlap(chunking.getOverlap());
            packOptions.setCodeSummaryToContent(chunking.isCodeSummary());
            log.info("配置 Nebula ReindexPipeline - 切换目标: {}, switch-order: {}, keep-generations: {}",
                    targets.stream().map(ReindexTarget::name).toList(), reindex.getSwitchOrder(),
                    reindex.getKeepGenerations());
            return new ReindexPipeline(source, targets, stateRepository, planner, parsers, packOptions,
                    reindex.getKeepGenerations());
        }

        /**
         * 按 {@code switch-order} 排序目标：{@code search-first} 时 BM25（search-service）在前，
         * 向量（vector-store）在后；{@code vector-first} 反之。R3 §4.3 默认 search-first。
         */
        private static List<ReindexTarget> orderBySwitchOrder(List<ReindexTarget> targets,
                                                              String switchOrder) {
            boolean vectorFirst = "vector-first".equalsIgnoreCase(
                    switchOrder == null ? "" : switchOrder.trim());
            List<ReindexTarget> ordered = new java.util.ArrayList<>(targets);
            ordered.sort(java.util.Comparator.comparingInt(t -> weight(t.name(), vectorFirst)));
            return ordered;
        }

        private static int weight(String name, boolean vectorFirst) {
            boolean isVector = VectorStoreIndexSink.NAME.equals(name);
            if (vectorFirst) {
                return isVector ? 0 : 1;
            }
            return isVector ? 1 : 0;
        }
    }

    /**
     * {@code reindex.vector-alias} 或 {@code reindex.search-alias} 至少一个非空才成立
     */
    static class ReindexAliasPresentCondition
            implements org.springframework.context.annotation.Condition {

        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                               org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            return isPresent(context, "nebula.ai.rag.indexing.reindex.vector-alias")
                    || isPresent(context, "nebula.ai.rag.indexing.reindex.search-alias");
        }

        static boolean isPresent(org.springframework.context.annotation.ConditionContext context,
                                 String key) {
            String value = context.getEnvironment().getProperty(key);
            return value != null && !value.isBlank();
        }
    }

    /**
     * {@code reindex.vector-alias} 非空才成立
     */
    static class VectorAliasPresentCondition
            implements org.springframework.context.annotation.Condition {

        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                               org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            return ReindexAliasPresentCondition.isPresent(context,
                    "nebula.ai.rag.indexing.reindex.vector-alias");
        }
    }

    /**
     * {@code reindex.search-alias} 非空才成立
     */
    static class SearchAliasPresentCondition
            implements org.springframework.context.annotation.Condition {

        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                               org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            return ReindexAliasPresentCondition.isPresent(context,
                    "nebula.ai.rag.indexing.reindex.search-alias");
        }
    }
}

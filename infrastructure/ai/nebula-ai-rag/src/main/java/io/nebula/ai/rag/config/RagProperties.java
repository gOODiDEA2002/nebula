package io.nebula.ai.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 配置属性
 * <p>
 * {@code enabled} 默认 false：RAG 依赖向量库等外部服务，遵循框架
 * 「依赖决定能力、配置决定启动」的三级启用策略（Level 3）。
 * <p>
 * 检索器权重不在这里配置 —— 权重属于各 {@code Retriever} 实现自身的特性，
 * 由各 Bean 自带，避免框架属性类和业务配置两处各说各话。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
@ConfigurationProperties(prefix = "nebula.ai.rag")
public class RagProperties {

    /**
     * 是否启用 RAG 能力
     */
    private boolean enabled = false;

    private Retrieval retrieval = new Retrieval();

    private Fusion fusion = new Fusion();

    private Rerank rerank = new Rerank();

    private Context context = new Context();

    private Generation generation = new Generation();

    private Chunking chunking = new Chunking();

    private Degrade degrade = new Degrade();

    private Indexing indexing = new Indexing();

    private Search search = new Search();

    private Transform transform = new Transform();

    private Guard guard = new Guard();

    private Streaming streaming = new Streaming();

    private Metrics metrics = new Metrics();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Retrieval getRetrieval() {
        return retrieval;
    }

    public void setRetrieval(Retrieval retrieval) {
        this.retrieval = retrieval;
    }

    public Fusion getFusion() {
        return fusion;
    }

    public void setFusion(Fusion fusion) {
        this.fusion = fusion;
    }

    public Rerank getRerank() {
        return rerank;
    }

    public void setRerank(Rerank rerank) {
        this.rerank = rerank;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Generation getGeneration() {
        return generation;
    }

    public void setGeneration(Generation generation) {
        this.generation = generation;
    }

    public Chunking getChunking() {
        return chunking;
    }

    public void setChunking(Chunking chunking) {
        this.chunking = chunking;
    }

    public Degrade getDegrade() {
        return degrade;
    }

    public void setDegrade(Degrade degrade) {
        this.degrade = degrade;
    }

    public Indexing getIndexing() {
        return indexing;
    }

    public void setIndexing(Indexing indexing) {
        this.indexing = indexing;
    }

    public Search getSearch() {
        return search;
    }

    public void setSearch(Search search) {
        this.search = search;
    }

    public Transform getTransform() {
        return transform;
    }

    public void setTransform(Transform transform) {
        this.transform = transform;
    }

    public Guard getGuard() {
        return guard;
    }

    public void setGuard(Guard guard) {
        this.guard = guard;
    }

    public Streaming getStreaming() {
        return streaming;
    }

    public void setStreaming(Streaming streaming) {
        this.streaming = streaming;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    /**
     * 检索配置
     */
    public static class Retrieval {

        /**
         * 最终返回的检索结果数量
         */
        private int topK = 10;

        /**
         * 候选放大倍数：各路取 topK * multiplier 条候选，融合后再截断
         * <p>
         * 不放大的话，一路的第 topK+1 名即便在其他路排第一也永远进不了融合。
         */
        private int candidateMultiplier = 2;

        /**
         * 单路检索默认超时（秒）
         * <p>
         * 仅对 {@code Retriever.timeoutMillis()} 返回非正数的检索器生效；
         * 检索器自己给出正的超时值时以它为准。
         */
        private int timeoutSeconds = 15;

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public int getCandidateMultiplier() {
            return candidateMultiplier;
        }

        public void setCandidateMultiplier(int candidateMultiplier) {
            this.candidateMultiplier = candidateMultiplier;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    /**
     * 融合配置
     */
    public static class Fusion {

        /**
         * RRF 常数
         */
        private int rrfK = 60;

        /**
         * 元数据保留优先的来源列表（如 graph）；为空表示保留最先命中的那一份
         */
        private List<String> sourcePriority = new ArrayList<>();

        public int getRrfK() {
            return rrfK;
        }

        public void setRrfK(int rrfK) {
            this.rrfK = rrfK;
        }

        public List<String> getSourcePriority() {
            return sourcePriority;
        }

        public void setSourcePriority(List<String> sourcePriority) {
            this.sourcePriority = sourcePriority;
        }
    }

    /**
     * 重排序配置
     */
    public static class Rerank {

        /**
         * 是否启用重排序；关闭时管线直接按融合顺序截断
         */
        private boolean enabled = true;

        /**
         * 重排序后保留数量
         */
        private int topK = 5;

        /**
         * 重排序超时（毫秒）
         * <p>
         * R4 起同时作为 HTTP 交叉编码重排的独立超时（含连接与读取）；LLM 重排（SIA）不消费本项。
         */
        private int timeoutMs = 3000;

        /**
         * HTTP 交叉编码重排配置（P6）；{@code url} 为空时不装配 {@code HttpCrossEncoderReranker}
         */
        private Http http = new Http();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public Http getHttp() {
            return http;
        }

        public void setHttp(Http http) {
            this.http = http;
        }

        /**
         * HTTP 交叉编码重排配置（P6，R4 §3、§8）
         * <p>
         * 显式配置 {@code url} 才装配 {@code HttpCrossEncoderReranker}；缺 {@code url} 时既有
         * {@code noopReranker} 路径零变化。{@code wireFormat=cohere} 时 {@code model} 必填。
         */
        public static class Http {

            /**
             * 交叉编码重排服务地址；空表示不装配 HTTP 重排，如 {@code http://tei:8080/rerank}
             */
            private String url = "";

            /**
             * 报文格式：{@code tei} | {@code cohere}
             */
            private String wireFormat = "tei";

            /**
             * 模型名；{@code cohere} 格式必填（随请求发送），{@code tei} 忽略
             */
            private String model = "";

            /**
             * 可选 API Key；非空时发送 {@code Authorization: Bearer}，建议经环境变量注入
             */
            private String apiKey = "";

            /**
             * 每个 HTTP 请求的候选数上限
             */
            private int batchSize = 32;

            /**
             * 单条候选正文的客户端截断字符数；{@code 0} 表示不在客户端截断（TEI 走服务端 {@code truncate}）
             */
            private int maxCharsPerDoc = 0;

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public String getWireFormat() {
                return wireFormat;
            }

            public void setWireFormat(String wireFormat) {
                this.wireFormat = wireFormat;
            }

            public String getModel() {
                return model;
            }

            public void setModel(String model) {
                this.model = model;
            }

            public String getApiKey() {
                return apiKey;
            }

            public void setApiKey(String apiKey) {
                this.apiKey = apiKey;
            }

            public int getBatchSize() {
                return batchSize;
            }

            public void setBatchSize(int batchSize) {
                this.batchSize = batchSize;
            }

            public int getMaxCharsPerDoc() {
                return maxCharsPerDoc;
            }

            public void setMaxCharsPerDoc(int maxCharsPerDoc) {
                this.maxCharsPerDoc = maxCharsPerDoc;
            }
        }
    }

    /**
     * 上下文拼接配置
     */
    public static class Context {

        /**
         * 上下文最大字符长度
         */
        private int maxLength = 4000;

        /**
         * 单篇文档的拼接模板，两个占位符依次为序号与内容
         */
        private String documentTemplate = "[文档%d] %s\n\n";

        /**
         * {@code RagAnswer.references} 语义（R4 §4.4）：{@code all}（现状，返回重排后全部 topK）
         * | {@code included}（只返回实际进入上下文的引用，序号即 {@code [文档n]}）
         */
        private String referencesMode = "all";

        public int getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }

        public String getDocumentTemplate() {
            return documentTemplate;
        }

        public void setDocumentTemplate(String documentTemplate) {
            this.documentTemplate = documentTemplate;
        }

        public String getReferencesMode() {
            return referencesMode;
        }

        public void setReferencesMode(String referencesMode) {
            this.referencesMode = referencesMode;
        }
    }

    /**
     * 答案生成配置
     */
    public static class Generation {

        /**
         * 生成超时（毫秒），超时后返回检索摘要降级答案
         */
        private long timeoutMs = 90_000;

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    /**
     * 文本切块配置
     */
    public static class Chunking {

        /**
         * 分块大小（字符）
         */
        private int size = 500;

        /**
         * 分块重叠（字符）
         */
        private int overlap = 100;

        /**
         * 是否在超限代码块首行附加签名摘要（R1 遗留小项，默认关，Y2）
         * <p>
         * 开启时 CODE 原子块在块首附加一行注释形式的签名摘要，提升代码子集召回；
         * 默认关闭以保持既有块正文不变。
         */
        private boolean codeSummary = false;

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int getOverlap() {
            return overlap;
        }

        public void setOverlap(int overlap) {
            this.overlap = overlap;
        }

        public boolean isCodeSummary() {
            return codeSummary;
        }

        public void setCodeSummary(boolean codeSummary) {
            this.codeSummary = codeSummary;
        }
    }

    /**
     * 降级文案配置
     * <p>
     * 用户可见文案必须可配：默认值取自生产在用的中文表述，
     * 应用换措辞不该被迫改框架代码。
     */
    public static class Degrade {

        /**
         * 一条都没检索到时的答复
         */
        private String noDocumentAnswer =
                "根据现有知识库资料，暂时无法找到与您的问题直接相关的内容。请尝试换个方式提问，或联系管理员补充相关知识。";

        /**
         * 生成失败时检索摘要的开头
         */
        private String fallbackHeader = "根据知识库检索，找到以下相关内容：\n\n";

        /**
         * 生成失败时检索摘要的结尾
         */
        private String fallbackFooter = "（大模型生成答案暂不可用，以上来自检索结果。）";

        /**
         * 检索摘要中单条内容的截断长度
         */
        private int fallbackExcerptLength = 280;

        /**
         * 生成器返回空正文时是否判降级
         * <p>
         * 推理模型（思考 token 也计入 max-tokens）可能在思考耗尽预算后返回空正文
         * （finish_reason=length），此时生成器拿到的正文为 null 或纯空白。开启后此类空正文
         * 按检索摘要降级，降级原因为 empty-answer；默认关闭以保持既有行为不变。
         */
        private boolean onEmptyAnswer = false;

        public String getNoDocumentAnswer() {
            return noDocumentAnswer;
        }

        public void setNoDocumentAnswer(String noDocumentAnswer) {
            this.noDocumentAnswer = noDocumentAnswer;
        }

        public String getFallbackHeader() {
            return fallbackHeader;
        }

        public void setFallbackHeader(String fallbackHeader) {
            this.fallbackHeader = fallbackHeader;
        }

        public String getFallbackFooter() {
            return fallbackFooter;
        }

        public void setFallbackFooter(String fallbackFooter) {
            this.fallbackFooter = fallbackFooter;
        }

        public int getFallbackExcerptLength() {
            return fallbackExcerptLength;
        }

        public void setFallbackExcerptLength(int fallbackExcerptLength) {
            this.fallbackExcerptLength = fallbackExcerptLength;
        }

        public boolean isOnEmptyAnswer() {
            return onEmptyAnswer;
        }

        public void setOnEmptyAnswer(boolean onEmptyAnswer) {
            this.onEmptyAnswer = onEmptyAnswer;
        }
    }

    /**
     * 索引治理配置（P2-min）
     * <p>
     * 默认关闭：增量索引任务需要应用侧提供 {@code DocumentSource} 与持久化
     * {@code IndexStateRepository}，引入 JAR 不该自动装配任何写目标。
     */
    public static class Indexing {

        /**
         * 是否启用索引治理装配；默认 false
         */
        private boolean enabled = false;

        /**
         * {@code SearchServiceIndexSink} 写入的索引名；空表示不装配该 Sink
         */
        private String searchIndexName = "";

        /**
         * 启用索引治理并提供了 {@code DocumentSource}，却没有持久化 {@code IndexStateRepository}
         * 时是否启动快速失败（R3 §7，默认 true）
         * <p>
         * 持续增量与删除对齐依赖持久化状态；缺状态库时静默不装配会把配置事故藏到运行期。
         * 显式声明 {@code InMemoryIndexStateRepository} 的用户不触发本检查（那是明示选择），
         * 但装配时 warn 一次说明其重启即失忆的局限。仅一次性任务可置 false 关闭本检查。
         */
        private boolean failFastWithoutStateRepository = true;

        /**
         * 版本化重灌与蓝绿切换配置（R3）
         */
        private Reindex reindex = new Reindex();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSearchIndexName() {
            return searchIndexName;
        }

        public void setSearchIndexName(String searchIndexName) {
            this.searchIndexName = searchIndexName;
        }

        public boolean isFailFastWithoutStateRepository() {
            return failFastWithoutStateRepository;
        }

        public void setFailFastWithoutStateRepository(boolean failFastWithoutStateRepository) {
            this.failFastWithoutStateRepository = failFastWithoutStateRepository;
        }

        public Reindex getReindex() {
            return reindex;
        }

        public void setReindex(Reindex reindex) {
            this.reindex = reindex;
        }

        /**
         * 版本化重灌与蓝绿切换配置（R3 §5，全部默认关）
         * <p>
         * {@code enabled=false} 时不装配任何 {@code CollectionSwitcher} 与重灌管线，默认行为不变。
         * 别名键为空的一侧不参与切换；两侧别名均空时重灌无目标可切。
         */
        public static class Reindex {

            /**
             * 是否启用版本化重灌装配；默认 false（不装配任何切换器与重灌管线）
             */
            private boolean enabled = false;

            /**
             * 向量集合别名；空表示向量侧不参与切换
             */
            private String vectorAlias = "";

            /**
             * 新建向量集合的维度；必须 {@code > 0} 才允许 {@code prepare} 建集合
             * <p>
             * 不从旧集合推断：重灌的典型动因就是换模型换维度，抄旧维度正好抄错。
             */
            private int vectorDimension = 0;

            /**
             * 新建向量集合的距离度量：{@code cosine} | {@code dot} | {@code euclid}
             */
            private String vectorDistance = "cosine";

            /**
             * BM25 索引别名；空表示 BM25 侧不参与切换
             */
            private String searchAlias = "";

            /**
             * 跨后端切换顺序：{@code search-first} | {@code vector-first}
             */
            private String switchOrder = "search-first";

            /**
             * 保留的历史代际数；{@code 0} = 切换后立即清理更旧代际
             */
            private int keepGenerations = 2;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getVectorAlias() {
                return vectorAlias;
            }

            public void setVectorAlias(String vectorAlias) {
                this.vectorAlias = vectorAlias;
            }

            public int getVectorDimension() {
                return vectorDimension;
            }

            public void setVectorDimension(int vectorDimension) {
                this.vectorDimension = vectorDimension;
            }

            public String getVectorDistance() {
                return vectorDistance;
            }

            public void setVectorDistance(String vectorDistance) {
                this.vectorDistance = vectorDistance;
            }

            public String getSearchAlias() {
                return searchAlias;
            }

            public void setSearchAlias(String searchAlias) {
                this.searchAlias = searchAlias;
            }

            public String getSwitchOrder() {
                return switchOrder;
            }

            public void setSwitchOrder(String switchOrder) {
                this.switchOrder = switchOrder;
            }

            public int getKeepGenerations() {
                return keepGenerations;
            }

            public void setKeepGenerations(int keepGenerations) {
                this.keepGenerations = keepGenerations;
            }
        }
    }

    /**
     * BM25 关键词检索路配置（P4b）
     * <p>
     * {@code indexName} 为空时不装配 {@code SearchServiceRetriever}，
     * 因此默认行为不变。权重与顺序均可配。
     */
    public static class Search {

        /**
         * 关键词检索的索引名；空表示不装配 {@code SearchServiceRetriever}
         */
        private String indexName = "";

        /**
         * 检索器融合权重
         */
        private double weight = 0.4;

        /**
         * 检索器在有序列表中的顺序值（越小越靠前）
         */
        private int order = 20;

        /**
         * 正文字段的查询分析器（建索引不由本项决定，仅查询侧）
         */
        private String analyzer = "standard";

        /**
         * 默认 mapping 的 search_analyzer
         */
        private String searchAnalyzer = "standard";

        public String getIndexName() {
            return indexName;
        }

        public void setIndexName(String indexName) {
            this.indexName = indexName;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }

        public String getAnalyzer() {
            return analyzer;
        }

        public void setAnalyzer(String analyzer) {
            this.analyzer = analyzer;
        }

        public String getSearchAnalyzer() {
            return searchAnalyzer;
        }

        public void setSearchAnalyzer(String searchAnalyzer) {
            this.searchAnalyzer = searchAnalyzer;
        }
    }

    /**
     * 查询改写与变体检索配置（P5）
     * <p>
     * {@code mode=none} 默认装 {@code TrimQueryTransformer}（现状语义）；
     * {@code rewrite}/{@code multi-query} 需 nebula-ai-spring 的 spring-ai 改写器适配，
     * 缺依赖时启动快速失败，不静默降级。
     */
    public static class Transform {

        /**
         * 改写模式：{@code none} | {@code rewrite} | {@code multi-query}
         */
        private String mode = "none";

        /**
         * 变体数上限，超出截断并 warn
         */
        private int maxVariants = 4;

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public int getMaxVariants() {
            return maxVariants;
        }

        public void setMaxVariants(int maxVariants) {
            this.maxVariants = maxVariants;
        }
    }

    /**
     * 可信上下文清洗配置（P7-a，R4 §4）
     * <p>
     * {@code sanitizer.enabled=false}（默认）装 {@code NoopRetrievedContentSanitizer}，
     * 装配结果与 R3 完全相同。
     */
    public static class Guard {

        private Sanitizer sanitizer = new Sanitizer();

        public Sanitizer getSanitizer() {
            return sanitizer;
        }

        public void setSanitizer(Sanitizer sanitizer) {
            this.sanitizer = sanitizer;
        }

        /**
         * 检索内容清洗器配置
         */
        public static class Sanitizer {

            /**
             * 是否启用清洗；关闭时装 {@code NoopRetrievedContentSanitizer}
             */
            private boolean enabled = false;

            /**
             * 命中处置：{@code replace}（整值替换为 {@code replacement}）| {@code drop}（剔除该条）
             */
            private String mode = "replace";

            /**
             * {@code replace} 模式的替换文案
             */
            private String replacement = "[内容因安全策略未进入上下文]";

            /**
             * 自定义正则列表；空表示使用内置 {@code PatternSanitizer.DEFAULT_PATTERN}，
             * 非空则完全覆盖内置正则
             */
            private List<String> patterns = new ArrayList<>();

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getMode() {
                return mode;
            }

            public void setMode(String mode) {
                this.mode = mode;
            }

            public String getReplacement() {
                return replacement;
            }

            public void setReplacement(String replacement) {
                this.replacement = replacement;
            }

            public List<String> getPatterns() {
                return patterns;
            }

            public void setPatterns(List<String> patterns) {
                this.patterns = patterns;
            }
        }
    }

    /**
     * 流式生成配置（P7-b，R4 §5）
     * <p>
     * {@code enabled=false}（默认）不装配 {@code StreamingAnswerGenerator}，
     * {@code queryStream} 返回「暂不支持」事件。
     */
    public static class Streaming {

        /**
         * 是否启用流式生成；开启后在存在 {@code ChatService} 时装配默认流式适配器
         */
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 指标采集配置（P7-c，R4 §6）
     * <p>
     * {@code enabled=false}（默认）装 {@code NoopRagMetrics}，不触碰 Micrometer。
     */
    public static class Metrics {

        /**
         * 是否启用指标；开启后在存在 {@code MeterRegistry} 时装配 Micrometer 适配
         */
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}

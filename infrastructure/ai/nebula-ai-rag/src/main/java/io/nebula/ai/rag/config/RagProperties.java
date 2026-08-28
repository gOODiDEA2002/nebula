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
         */
        private int timeoutMs = 3000;

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
    }
}

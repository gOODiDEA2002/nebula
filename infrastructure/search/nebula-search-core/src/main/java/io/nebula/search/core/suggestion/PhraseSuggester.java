package io.nebula.search.core.suggestion;

/**
 * 短语建议器
 * 基于 n-gram 模型的短语级别纠错
 * 
 * 使用场景：
 * - 搜索框短语纠错
 * - 考虑上下文的错误修正
 * 
 * 示例：
 * - "noble prize" -> "nobel prize"
 * - "quick fox jumps" -> "quick brown fox jumps"
 * 
 * @author nebula
 */
public class PhraseSuggester extends AbstractSuggester {
    
    /**
     * n-gram 大小
     */
    private Integer gramSize;
    
    /**
     * 实际词错误可能性（0-1）
     */
    private Float realWordErrorLikelihood;
    
    /**
     * 置信度阈值（0-1）
     */
    private Float confidence;
    
    /**
     * 最大错误数
     */
    private Float maxErrors;
    
    /**
     * 分隔符
     */
    private String separator;
    
    /**
     * 高亮前缀标签
     */
    private String highlightPreTag;
    
    /**
     * 高亮后缀标签
     */
    private String highlightPostTag;
    
    /**
     * 是否启用校对（验证建议是否匹配文档）
     */
    private Boolean collate;
    
    public PhraseSuggester(String name, String text, String field) {
        super(name, text, field);
        this.gramSize = 3; // 默认 trigram
        this.realWordErrorLikelihood = 0.95f;
        this.confidence = 0.0f;
        this.separator = " ";
    }
    
    @Override
    public SuggesterType getType() {
        return SuggesterType.PHRASE;
    }
    
    // Builder 模式
    
    public PhraseSuggester gramSize(int gramSize) {
        this.gramSize = gramSize;
        return this;
    }
    
    public PhraseSuggester realWordErrorLikelihood(float realWordErrorLikelihood) {
        this.realWordErrorLikelihood = realWordErrorLikelihood;
        return this;
    }
    
    public PhraseSuggester confidence(float confidence) {
        this.confidence = confidence;
        return this;
    }
    
    public PhraseSuggester maxErrors(float maxErrors) {
        this.maxErrors = maxErrors;
        return this;
    }
    
    public PhraseSuggester separator(String separator) {
        this.separator = separator;
        return this;
    }
    
    public PhraseSuggester highlight(String preTag, String postTag) {
        this.highlightPreTag = preTag;
        this.highlightPostTag = postTag;
        return this;
    }
    
    public PhraseSuggester collate(boolean collate) {
        this.collate = collate;
        return this;
    }
    
    // Getters
    
    public Integer getGramSize() {
        return gramSize;
    }
    
    public Float getRealWordErrorLikelihood() {
        return realWordErrorLikelihood;
    }
    
    public Float getConfidence() {
        return confidence;
    }
    
    public Float getMaxErrors() {
        return maxErrors;
    }
    
    public String getSeparator() {
        return separator;
    }
    
    public String getHighlightPreTag() {
        return highlightPreTag;
    }
    
    public String getHighlightPostTag() {
        return highlightPostTag;
    }
    
    public Boolean getCollate() {
        return collate;
    }
}


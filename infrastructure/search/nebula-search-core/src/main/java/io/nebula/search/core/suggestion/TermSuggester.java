package io.nebula.search.core.suggestion;

/**
 * 词项建议器
 * 基于编辑距离的单词级别纠错
 * 
 * 使用场景：
 * - 搜索框拼写纠错
 * - 单词级别的错误修正
 * 
 * 示例：
 * - "tset" -> "test"
 * - "javs" -> "java"
 * 
 * @author nebula
 */
public class TermSuggester extends AbstractSuggester {
    
    /**
     * 建议模式
     * missing: 仅为缺失的词提供建议
     * popular: 仅建议出现频率更高的词
     * always: 总是提供建议
     */
    private String suggestMode;
    
    /**
     * 最大编辑距离（1 或 2）
     */
    private Integer maxEdits;
    
    /**
     * 前缀长度（不参与模糊匹配的前缀字符数）
     */
    private Integer prefixLength;
    
    /**
     * 最小词长
     */
    private Integer minWordLength;
    
    /**
     * 最小文档频率
     */
    private Float minDocFreq;
    
    public TermSuggester(String name, String text, String field) {
        super(name, text, field);
        this.suggestMode = "missing";
        this.maxEdits = 2;
        this.prefixLength = 1;
        this.minWordLength = 4;
    }
    
    @Override
    public SuggesterType getType() {
        return SuggesterType.TERM;
    }
    
    // Builder 模式
    
    public TermSuggester suggestMode(String suggestMode) {
        this.suggestMode = suggestMode;
        return this;
    }
    
    public TermSuggester maxEdits(int maxEdits) {
        this.maxEdits = maxEdits;
        return this;
    }
    
    public TermSuggester prefixLength(int prefixLength) {
        this.prefixLength = prefixLength;
        return this;
    }
    
    public TermSuggester minWordLength(int minWordLength) {
        this.minWordLength = minWordLength;
        return this;
    }
    
    public TermSuggester minDocFreq(float minDocFreq) {
        this.minDocFreq = minDocFreq;
        return this;
    }
    
    // Getters
    
    public String getSuggestMode() {
        return suggestMode;
    }
    
    public Integer getMaxEdits() {
        return maxEdits;
    }
    
    public Integer getPrefixLength() {
        return prefixLength;
    }
    
    public Integer getMinWordLength() {
        return minWordLength;
    }
    
    public Float getMinDocFreq() {
        return minDocFreq;
    }
}


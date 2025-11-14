package io.nebula.search.core.suggestion;

/**
 * 补全建议器
 * 基于前缀匹配的自动补全
 * 
 * 使用场景：
 * - 搜索框自动补全
 * - 快速输入提示
 * 
 * 示例：
 * - "java" -> ["javascript", "java programming", "java tutorial"]
 * - "spring" -> ["spring boot", "spring framework", "spring cloud"]
 * 
 * 注意：需要字段类型为 completion
 * 
 * @author nebula
 */
public class CompletionSuggester extends AbstractSuggester {
    
    /**
     * 前缀（用于补全的输入前缀）
     */
    private String prefix;
    
    /**
     * 正则表达式（用于补全的正则模式）
     */
    private String regex;
    
    /**
     * 是否跳过重复
     */
    private Boolean skipDuplicates;
    
    /**
     * 模糊匹配选项
     */
    private FuzzyOptions fuzzy;
    
    public CompletionSuggester(String name, String text, String field) {
        super(name, text, field);
        this.prefix = text; // 默认使用输入文本作为前缀
        this.skipDuplicates = false;
    }
    
    @Override
    public SuggesterType getType() {
        return SuggesterType.COMPLETION;
    }
    
    // Builder 模式
    
    public CompletionSuggester prefix(String prefix) {
        this.prefix = prefix;
        return this;
    }
    
    public CompletionSuggester regex(String regex) {
        this.regex = regex;
        return this;
    }
    
    public CompletionSuggester skipDuplicates(boolean skipDuplicates) {
        this.skipDuplicates = skipDuplicates;
        return this;
    }
    
    public CompletionSuggester fuzzy(FuzzyOptions fuzzy) {
        this.fuzzy = fuzzy;
        return this;
    }
    
    // Getters
    
    public String getPrefix() {
        return prefix;
    }
    
    public String getRegex() {
        return regex;
    }
    
    public Boolean getSkipDuplicates() {
        return skipDuplicates;
    }
    
    public FuzzyOptions getFuzzy() {
        return fuzzy;
    }
    
    /**
     * 模糊匹配选项
     */
    public static class FuzzyOptions {
        /**
         * 编辑距离（0, 1, 2, AUTO）
         */
        private String fuzziness;
        
        /**
         * 是否转置
         */
        private Boolean transpositions;
        
        /**
         * 最小长度
         */
        private Integer minLength;
        
        /**
         * 前缀长度
         */
        private Integer prefixLength;
        
        /**
         * Unicode 感知
         */
        private Boolean unicodeAware;
        
        public FuzzyOptions() {
            this.fuzziness = "AUTO";
            this.transpositions = true;
            this.minLength = 3;
            this.prefixLength = 1;
            this.unicodeAware = false;
        }
        
        public FuzzyOptions fuzziness(String fuzziness) {
            this.fuzziness = fuzziness;
            return this;
        }
        
        public FuzzyOptions transpositions(boolean transpositions) {
            this.transpositions = transpositions;
            return this;
        }
        
        public FuzzyOptions minLength(int minLength) {
            this.minLength = minLength;
            return this;
        }
        
        public FuzzyOptions prefixLength(int prefixLength) {
            this.prefixLength = prefixLength;
            return this;
        }
        
        public FuzzyOptions unicodeAware(boolean unicodeAware) {
            this.unicodeAware = unicodeAware;
            return this;
        }
        
        // Getters
        
        public String getFuzziness() {
            return fuzziness;
        }
        
        public Boolean getTranspositions() {
            return transpositions;
        }
        
        public Integer getMinLength() {
            return minLength;
        }
        
        public Integer getPrefixLength() {
            return prefixLength;
        }
        
        public Boolean getUnicodeAware() {
            return unicodeAware;
        }
    }
}


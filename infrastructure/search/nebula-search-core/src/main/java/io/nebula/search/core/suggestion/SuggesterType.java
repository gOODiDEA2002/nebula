package io.nebula.search.core.suggestion;

/**
 * 建议器类型枚举
 * 
 * @author nebula
 */
public enum SuggesterType {
    
    /**
     * 词项建议 - 基于编辑距离的单词纠错
     * 用于纠正拼写错误的单词
     */
    TERM,
    
    /**
     * 短语建议 - 基于 n-gram 模型的短语纠错
     * 用于纠正整个短语，考虑上下文
     */
    PHRASE,
    
    /**
     * 补全建议 - 基于前缀匹配的自动补全
     * 用于搜索建议和自动补全场景
     */
    COMPLETION
}


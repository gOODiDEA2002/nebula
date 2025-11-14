package io.nebula.search.elasticsearch.converter;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import co.elastic.clients.elasticsearch.core.search.TermSuggester;
import co.elastic.clients.elasticsearch.core.search.PhraseSuggester;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggester;
import co.elastic.clients.elasticsearch.core.search.SuggestFuzziness;
import io.nebula.search.core.suggestion.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 建议器转换器
 * 将 nebula 的 Suggester 转换为 Elasticsearch Java Client 的 Suggester
 * 
 * @author nebula
 */
public class SuggesterConverter {
    
    private static final Logger logger = LoggerFactory.getLogger(SuggesterConverter.class);
    
    /**
     * 转换建议器列表
     * 
     * @param suggesters nebula 建议器列表
     * @return Elasticsearch Suggester
     */
    public static Suggester convert(List<io.nebula.search.core.suggestion.Suggester> suggesters) {
        if (suggesters == null || suggesters.isEmpty()) {
            return null;
        }
        
        Suggester.Builder suggesterBuilder = new Suggester.Builder();
        
        // 处理每个建议器
        for (io.nebula.search.core.suggestion.Suggester suggester : suggesters) {
            String name = suggester.getName();
            
            switch (suggester.getType()) {
                case TERM:
                    suggesterBuilder.suggesters(name, s -> s
                        .text(suggester.getText())
                        .term(convertTermSuggester((io.nebula.search.core.suggestion.TermSuggester) suggester))
                    );
                    break;
                    
                case PHRASE:
                    suggesterBuilder.suggesters(name, s -> s
                        .text(suggester.getText())
                        .phrase(convertPhraseSuggester((io.nebula.search.core.suggestion.PhraseSuggester) suggester))
                    );
                    break;
                    
                case COMPLETION:
                    suggesterBuilder.suggesters(name, s -> s
                        .prefix(suggester.getText())
                        .completion(convertCompletionSuggester((io.nebula.search.core.suggestion.CompletionSuggester) suggester))
                    );
                    break;
                    
                default:
                    logger.warn("Unsupported suggester type: {}", suggester.getType());
                    break;
            }
        }
        
        return suggesterBuilder.build();
    }
    
    /**
     * 转换 Term Suggester
     */
    private static TermSuggester convertTermSuggester(io.nebula.search.core.suggestion.TermSuggester termSuggester) {
        return TermSuggester.of(t -> {
            t.field(termSuggester.getField());
            
            if (termSuggester.getSize() != null) {
                t.size(termSuggester.getSize());
            }
            
            if (termSuggester.getMinDocFreq() != null) {
                t.minDocFreq(termSuggester.getMinDocFreq());
            }
            
            if (termSuggester.getMaxEdits() != null) {
                t.maxEdits(termSuggester.getMaxEdits());
            }
            
            if (termSuggester.getPrefixLength() != null) {
                t.prefixLength(termSuggester.getPrefixLength());
            }
            
            if (termSuggester.getMinWordLength() != null) {
                t.minWordLength(termSuggester.getMinWordLength());
            }
            
            if (termSuggester.getSuggestMode() != null) {
                t.suggestMode(co.elastic.clients.elasticsearch._types.SuggestMode.valueOf(
                    termSuggester.getSuggestMode().substring(0, 1).toUpperCase() + 
                    termSuggester.getSuggestMode().substring(1).toLowerCase()
                ));
            }
            
            return t;
        });
    }
    
    /**
     * 转换 Phrase Suggester
     */
    private static PhraseSuggester convertPhraseSuggester(io.nebula.search.core.suggestion.PhraseSuggester phraseSuggester) {
        return PhraseSuggester.of(p -> {
            p.field(phraseSuggester.getField());
            
            if (phraseSuggester.getSize() != null) {
                p.size(phraseSuggester.getSize());
            }
            
            if (phraseSuggester.getConfidence() != null) {
                p.confidence(phraseSuggester.getConfidence().doubleValue());
            }
            
            if (phraseSuggester.getHighlightPreTag() != null && phraseSuggester.getHighlightPostTag() != null) {
                p.highlight(h -> h
                    .preTag(phraseSuggester.getHighlightPreTag())
                    .postTag(phraseSuggester.getHighlightPostTag())
                );
            }
            
            return p;
        });
    }
    
    /**
     * 转换 Completion Suggester
     */
    private static CompletionSuggester convertCompletionSuggester(io.nebula.search.core.suggestion.CompletionSuggester completionSuggester) {
        return CompletionSuggester.of(c -> {
            c.field(completionSuggester.getField());
            
            if (completionSuggester.getSize() != null) {
                c.size(completionSuggester.getSize());
            }
            
            if (completionSuggester.getSkipDuplicates() != null) {
                c.skipDuplicates(completionSuggester.getSkipDuplicates());
            }
            
            return c;
        });
    }
    
    /**
     * 解析建议结果
     * 
     * @param suggestResponse Elasticsearch 建议结果
     * @return Map 格式的建议结果
     */
    public static Map<String, List<String>> parseSuggestionResults(Object suggestResponse) {
        Map<String, List<String>> result = new HashMap<>();
        
        // TODO: 根据实际的 Elasticsearch Java Client API 完善此方法
        // 当前返回空结果，待确认 SearchResponse.suggest() 的返回类型后再完善
        logger.warn("parseSuggestionResults is not fully implemented yet");
        
        return result;
    }
}

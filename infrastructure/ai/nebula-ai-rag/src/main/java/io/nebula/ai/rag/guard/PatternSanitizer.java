package io.nebula.ai.rag.guard;

import io.nebula.ai.rag.retriever.RetrievalResult;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 正则命中即整值替换或剔除的清洗器（R4 §4.1，移植 SIA CloudFieldSanitizer 手法）
 * <p>
 * 只做<b>整值</b>处理而非局部抹除：注入片段常与正常内容交织，局部替换后残留的上下文
 * 仍可能改变模型行为；整值替换或整条剔除语义清晰、不可被绕过。{@link Mode#REPLACE} 保留其余字段
 * （引用仍能定位到原文档），{@link Mode#DROP} 直接剔除该条。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class PatternSanitizer implements RetrievedContentSanitizer {

    /** 默认注入特征正则（移植 SIA {@code AgentSafetyPatterns.PROMPT_INJECTION}，大小写不敏感） */
    public static final String DEFAULT_PATTERN =
            "忽略(以上|之前|前面|上述)(的)?(指令|提示|要求|设定)|(你现在是|你是一个|假装你|扮演)|"
                    + "(system|assistant|user)\\s*[:：]|</?(system|prompt|instruction)>|"
                    + "ignore\\s+(all\\s+)?(previous|above|prior)\\s+(instructions|prompts)|jailbreak|dan\\s+mode";

    /** 命中后替换正文用的占位文本 */
    public static final String DEFAULT_REPLACEMENT = "[内容因安全策略未进入上下文]";

    /**
     * 命中后的处理方式
     */
    public enum Mode {
        /** 正文整值替换为占位文本，其余字段保留 */
        REPLACE,
        /** 整条剔除 */
        DROP
    }

    private final List<Pattern> patterns;
    private final Mode mode;
    private final String replacement;

    /**
     * @param patterns    命中特征的正则列表；为空则不命中任何内容
     * @param mode        命中处理方式，不能为空
     * @param replacement REPLACE 模式的占位文本；为空时用 {@link #DEFAULT_REPLACEMENT}
     */
    public PatternSanitizer(List<Pattern> patterns, Mode mode, String replacement) {
        if (mode == null) {
            throw new IllegalArgumentException("Mode 不能为空");
        }
        this.patterns = patterns == null ? List.of() : List.copyOf(patterns);
        this.mode = mode;
        this.replacement = replacement == null || replacement.isBlank() ? DEFAULT_REPLACEMENT : replacement;
    }

    @Override
    public RetrievalResult sanitize(RetrievalResult result) {
        if (result == null) {
            return null;
        }
        String content = result.getContent();
        if (content == null || content.isEmpty() || !matches(content)) {
            return result;
        }
        if (mode == Mode.DROP) {
            return null;
        }
        return RetrievalResult.builder()
                .id(result.getId())
                .content(replacement)
                .metadata(result.getMetadata())
                .score(result.getScore())
                .source(result.getSource())
                .build();
    }

    private boolean matches(String content) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(content).find()) {
                return true;
            }
        }
        return false;
    }
}

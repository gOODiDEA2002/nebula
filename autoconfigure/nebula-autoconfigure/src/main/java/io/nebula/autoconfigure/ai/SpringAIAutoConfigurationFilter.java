package io.nebula.autoconfigure.ai;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;

import java.util.Set;

/**
 * Spring AI 自动配置过滤器
 * 在框架层面排除 Spring AI 的默认自动配置，使用 Nebula 的自定义配置
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class SpringAIAutoConfigurationFilter implements AutoConfigurationImportFilter {

    /**
     * 需要排除的 Spring AI 自动配置类
     */
    private static final Set<String> EXCLUDED_AUTO_CONFIGURATIONS = Set.of(
            // OpenAI 相关自动配置
            "org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration",
            // Chroma 向量存储自动配置
            "org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreAutoConfiguration"
    );

    @Override
    public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
        boolean[] matches = new boolean[autoConfigurationClasses.length];
        
        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            // 如果在排除列表中,则返回 false(不匹配,即排除)
            // 否则返回 true(匹配,即保留)
            // 注意:autoConfigurationClasses[i] 可能为 null,需要先检查
            String autoConfigurationClass = autoConfigurationClasses[i];
            matches[i] = autoConfigurationClass == null || !EXCLUDED_AUTO_CONFIGURATIONS.contains(autoConfigurationClass);
        }
        
        return matches;
    }
}


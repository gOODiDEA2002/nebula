package io.nebula.autoconfigure.ai;

import io.nebula.ai.spring.config.AIProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;

import static org.assertj.core.api.Assertions.assertThat;

class AIAutoConfigurationOptionsTest {

    private static final String API_KEY = "test-compatible-api-key";

    private final AIAutoConfiguration autoConfiguration = new AIAutoConfiguration();

    @Test
    void chatModelUsesConfiguredCredentials() {
        OpenAiChatModel model = (OpenAiChatModel) autoConfiguration.nebulaOpenAiChatModel(properties());

        assertThat(model.getOptions().getApiKey()).isEqualTo(API_KEY);
        assertThat(model.getOptions().getBaseUrl()).isEqualTo("https://example.invalid/v1");
    }

    @Test
    void embeddingModelUsesFloatEncoding() {
        OpenAiEmbeddingModel model = (OpenAiEmbeddingModel) autoConfiguration
                .nebulaOpenAiEmbeddingModel(properties());

        assertThat(model.getOptions().getApiKey()).isEqualTo(API_KEY);
        assertThat(model.getOptions().getEncodingFormat())
                .isEqualTo(OpenAiEmbeddingOptions.EncodingFormat.FLOAT);
    }

    private AIProperties properties() {
        AIProperties properties = new AIProperties();
        properties.getOpenai().setApiKey(API_KEY);
        properties.getOpenai().setBaseUrl("https://example.invalid/v1");
        return properties;
    }
}

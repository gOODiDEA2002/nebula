package io.nebula.ai.spring.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AIPropertiesTest {

    @Test
    void openAiBaseUrlIncludesApiVersionByDefault() {
        AIProperties properties = new AIProperties();

        assertThat(properties.getOpenai().getBaseUrl())
                .isEqualTo("https://api.openai.com/v1");
        assertThat(properties.getOpenai().getChat().getOptions().getMaxRetries()).isEqualTo(2);
        assertThat(properties.getOpenai().getEmbedding().getOptions().getMaxRetries()).isEqualTo(2);
    }
}

package io.nebula.web.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class WebMvcConfigurerCoexistenceTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WebAutoConfiguration.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withPropertyValues(
                    "nebula.web.rate-limit.enabled=true",
                    "nebula.web.cache.enabled=true");

    @Test
    void enabledWebFeaturesRegisterTheirConfigurersTogether() {
        runner.run(context -> assertThat(context)
                .hasBean("nebulaWebMvcConfigurer")
                .hasBean("rateLimitWebMvcConfigurer")
                .hasBean("responseCacheWebMvcConfigurer"));
    }
}

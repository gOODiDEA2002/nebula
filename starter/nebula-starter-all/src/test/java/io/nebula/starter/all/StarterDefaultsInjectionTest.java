package io.nebula.starter.all;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 nebula-starter-all 的 nebula-defaults.properties 正确注入默认配置。
 * Jackson 3 迁移完成后 preferred-json-mapper 不再强制为 jackson2。
 */
@SpringBootTest(classes = StarterDefaultsInjectionTest.MinimalApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StarterDefaultsInjectionTest {

    @Autowired
    private Environment environment;

    @Test
    void persistenceEnabledByDefault() {
        assertThat(environment.getProperty("nebula.data.persistence.enabled"))
                .isEqualTo("true");
    }

    @Test
    void cacheEnabledByDefault() {
        assertThat(environment.getProperty("nebula.data.cache.enabled"))
                .isEqualTo("true");
    }

    @Test
    void preferredJsonMapperNotForcedToJackson2() {
        assertThat(environment.getProperty("spring.http.converters.preferred-json-mapper"))
                .isNull();
    }

    @SpringBootConfiguration
    static class MinimalApp {
    }
}

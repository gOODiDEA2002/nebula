package io.nebula.starter.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 nebula-starter-mcp 的 nebula-defaults.properties 正确注入默认配置。
 * Jackson 3 迁移完成后 preferred-json-mapper 不再强制为 jackson2。
 */
@SpringBootTest(classes = StarterDefaultsInjectionTest.MinimalApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StarterDefaultsInjectionTest {

    @Autowired
    private Environment environment;

    @Test
    void aiEnabledByDefault() {
        assertThat(environment.getProperty("nebula.ai.enabled"))
                .isEqualTo("true");
    }

    @Test
    void mcpServerEnabledByDefault() {
        assertThat(environment.getProperty("nebula.ai.mcp.server.enabled"))
                .isEqualTo("true");
    }

    @Test
    void legacyIneffectiveMcpKeyNotInjected() {
        // nebula.ai.mcp.enabled 无对应的绑定字段与条件读取方, 不应再出现在 defaults 中
        assertThat(environment.getProperty("nebula.ai.mcp.enabled"))
                .isNull();
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

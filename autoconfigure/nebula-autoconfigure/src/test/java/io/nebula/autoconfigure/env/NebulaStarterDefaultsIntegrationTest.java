package io.nebula.autoconfigure.env;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 {@link NebulaStarterDefaultsPostProcessor} 确实被 Spring Boot 加载并生效。
 * <p>
 * 该 EPP 曾错误地用 {@code .imports} 文件注册, 而 Spring Boot 只从 {@code spring.factories}
 * 加载 EnvironmentPostProcessor, 导致 Starter 的 nebula-defaults.properties 从未被注入,
 * "引入 Starter 即开箱即用"整套机制静默失效。改注册到 spring.factories 后本测试应通过。
 * <p>
 * EPP 在环境准备阶段运行, 与外部中间件无关, 用最小上下文即可验证(webEnvironment=NONE, 无 @EnableAutoConfiguration)。
 */
@SpringBootTest(classes = NebulaStarterDefaultsIntegrationTest.MinimalApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class NebulaStarterDefaultsIntegrationTest {

    @Autowired
    private Environment environment;

    @Test
    void starterDefaultsAreInjected() {
        // 该属性仅存在于 test 的 META-INF/nebula-defaults.properties;
        // 若 EPP 未被加载(旧 .imports 注册的 bug), 此处会是 null
        assertThat(environment.getProperty("nebula.test.defaults-sentinel")).isEqualTo("applied");
    }

    @Test
    void userConfigOverridesStarterDefaults() {
        // defaults 以最低优先级(addLast)注入, 用户 application.properties 应能覆盖同名项:
        // nebula.test.override-check 在 defaults 中为 from-defaults, 在 application.properties 中为 from-app
        assertThat(environment.getProperty("nebula.test.override-check")).isEqualTo("from-app");
    }

    @SpringBootConfiguration
    static class MinimalApp {
    }
}

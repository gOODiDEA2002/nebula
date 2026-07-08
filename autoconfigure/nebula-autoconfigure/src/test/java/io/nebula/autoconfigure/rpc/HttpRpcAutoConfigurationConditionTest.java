package io.nebula.autoconfigure.rpc;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import io.nebula.rpc.http.client.HttpRpcClient;
import io.nebula.rpc.http.config.HttpRpcProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HttpRpcAutoConfiguration 三态条件测试：
 * (1) enabled=true + HttpRpcClient 类在 → 有 HttpRpcClient Bean
 * (2) enabled=false → 无 Bean
 * (3) HttpRpcClient 类缺失 → 配置不加载
 */
class HttpRpcAutoConfigurationConditionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpRpcAutoConfiguration.class))
            .withUserConfiguration(MockDeps.class);

    @Test
    void enabled_hasHttpRpcClient() {
        runner.withPropertyValues("nebula.rpc.http.enabled=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(HttpRpcClient.class));
    }

    @Test
    void disabled_noHttpRpcClient() {
        runner.withPropertyValues("nebula.rpc.http.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(HttpRpcClient.class));
    }

    @Test
    void defaultDisabled_noHttpRpcClient() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(HttpRpcClient.class));
    }

    @Test
    void missingClass_noHttpRpcClient() {
        runner.withPropertyValues("nebula.rpc.http.enabled=true")
                .withClassLoader(new FilteredClassLoader(HttpRpcProperties.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(HttpRpcClient.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class MockDeps {
        @Bean
        ObjectMapper objectMapper() {
            return JsonMapper.builder().build();
        }
    }
}

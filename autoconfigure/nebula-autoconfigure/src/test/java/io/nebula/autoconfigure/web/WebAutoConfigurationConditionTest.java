package io.nebula.autoconfigure.web;

import io.nebula.web.autoconfigure.WebAutoConfiguration;
import io.nebula.web.autoconfigure.WebProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebAutoConfiguration 三态条件测试：
 * (1) Web 环境 + DispatcherServlet → 配置加载
 * (2) 非 Web 环境 → 配置不加载
 * (3) DispatcherServlet 类缺失 → 配置不加载
 */
class WebAutoConfigurationConditionTest {

    @Test
    void webEnvironment_configurationLoads() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(WebAutoConfiguration.class))
                .run(ctx -> assertThat(ctx).hasSingleBean(WebProperties.class));
    }

    @Test
    void nonWebEnvironment_configurationNotLoaded() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(WebAutoConfiguration.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(WebProperties.class));
    }

    @Test
    void missingDispatcherServlet_configurationNotLoaded() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(WebAutoConfiguration.class))
                .withClassLoader(new FilteredClassLoader(DispatcherServlet.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(WebProperties.class));
    }
}

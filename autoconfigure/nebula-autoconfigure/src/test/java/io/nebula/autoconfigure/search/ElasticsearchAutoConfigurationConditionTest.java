package io.nebula.autoconfigure.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.search.elasticsearch.service.ElasticsearchSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ElasticsearchAutoConfiguration 三态条件测试：
 * (1) enabled=true + ElasticsearchClient 类在 → 有 SearchService Bean
 * (2) enabled=false → 无 Bean
 * (3) ElasticsearchClient 类缺失 → 配置不加载
 */
class ElasticsearchAutoConfigurationConditionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ElasticsearchAutoConfiguration.class))
            .withUserConfiguration(MockDeps.class);

    @Test
    void enabled_hasSearchService() {
        runner.withPropertyValues(
                        "nebula.search.elasticsearch.enabled=true",
                        "nebula.search.elasticsearch.hosts=localhost:9200")
                .run(ctx -> assertThat(ctx).hasSingleBean(ElasticsearchSearchService.class));
    }

    @Test
    void disabled_noSearchService() {
        runner.withPropertyValues("nebula.search.elasticsearch.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(ElasticsearchSearchService.class));
    }

    @Test
    void defaultDisabled_noSearchService() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(ElasticsearchSearchService.class));
    }

    @Test
    void missingClass_noSearchService() {
        runner.withPropertyValues(
                        "nebula.search.elasticsearch.enabled=true",
                        "nebula.search.elasticsearch.hosts=localhost:9200")
                .withClassLoader(new FilteredClassLoader(ElasticsearchClient.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(ElasticsearchSearchService.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class MockDeps {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}

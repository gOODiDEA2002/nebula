package io.nebula.autoconfigure.data;

import io.nebula.data.cache.config.CacheProperties;
import io.nebula.data.cache.manager.CacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CacheAutoConfiguration 条件测试：
 * (1) enabled=true → 有 CacheManager（默认 local 模式）
 * (2) enabled=false → 无 CacheManager
 * (3) 默认未设置 → 无 CacheManager（matchIfMissing=false）
 *
 * <p>注：CacheAutoConfiguration 无 @ConditionalOnClass，故不测"缺类"态。
 * 需提供 Spring CacheManager 以满足 @EnableCaching。</p>
 */
class CacheAutoConfigurationConditionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class))
            .withUserConfiguration(SpringCacheDeps.class);

    @Test
    void enabled_hasCacheManager() {
        runner.withPropertyValues("nebula.data.cache.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasSingleBean(CacheManager.class);
                    assertThat(ctx).hasSingleBean(CacheProperties.class);
                });
    }

    @Test
    void disabled_noCacheManager() {
        runner.withPropertyValues("nebula.data.cache.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(CacheManager.class));
    }

    @Test
    void defaultDisabled_noCacheManager() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(CacheManager.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class SpringCacheDeps {
        @Bean
        org.springframework.cache.CacheManager springCacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }
}

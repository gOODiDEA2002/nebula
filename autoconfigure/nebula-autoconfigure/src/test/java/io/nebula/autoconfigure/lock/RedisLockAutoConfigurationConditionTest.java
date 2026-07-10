package io.nebula.autoconfigure.lock;

import io.nebula.lock.LockManager;
import io.nebula.lock.redis.LockedAspect;
import io.nebula.lock.redis.RedisLockAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisLockAutoConfigurationConditionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisLockAutoConfiguration.class))
            .withUserConfiguration(MockRedissonConfiguration.class);

    @Test
    void defaultDisabled_doesNotCreateLockBeans() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(LockManager.class);
            assertThat(context).doesNotHaveBean(LockedAspect.class);
        });
    }

    @Test
    void enabled_createsLockBeans() {
        runner.withPropertyValues("nebula.lock.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(LockManager.class);
                    assertThat(context).hasSingleBean(LockedAspect.class);
                });
    }

    @Test
    void explicitlyDisabled_doesNotCreateLockBeans() {
        runner.withPropertyValues("nebula.lock.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(LockManager.class);
                    assertThat(context).doesNotHaveBean(LockedAspect.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class MockRedissonConfiguration {

        @Bean
        RedissonClient redissonClient() {
            return mock(RedissonClient.class);
        }
    }
}

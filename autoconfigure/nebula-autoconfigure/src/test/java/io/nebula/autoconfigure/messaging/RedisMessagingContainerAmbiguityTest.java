package io.nebula.autoconfigure.messaging;

import io.nebula.messaging.redis.config.RedisMessagingAutoConfiguration;
import io.nebula.messaging.redis.consumer.RedisMessageConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 回归测试: cache 模块的失效广播容器(cacheInvalidationListenerContainer)与
 * messaging-redis 模块自建容器(redisMessageListenerContainer)同时存在时,
 * redisMessageConsumer 必须通过 @Qualifier 精确注入自己的容器, 不得因
 * 同类型双 Bean 产生歧义导致启动失败(proud-day 接入时暴露, 2026-07-08)
 */
class RedisMessagingContainerAmbiguityTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisMessagingAutoConfiguration.class))
            .withUserConfiguration(CoexistingContainerDeps.class);

    @Test
    void consumerBindsOwnContainer_whenCacheInvalidationContainerCoexists() {
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx.getBeansOfType(RedisMessageListenerContainer.class)).hasSize(2);
            assertThat(ctx).hasSingleBean(RedisMessageConsumer.class);

            // 消费者内部持有的必须是 messaging 模块自建容器, 而非缓存失效容器
            Object bound = ReflectionTestUtils.getField(
                    ctx.getBean(RedisMessageConsumer.class), "listenerContainer");
            assertThat(bound).isSameAs(ctx.getBean("redisMessageListenerContainer"));
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CoexistingContainerDeps {

        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            return mock(RedisConnectionFactory.class);
        }

        @Bean
        StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
            StringRedisTemplate template = new StringRedisTemplate();
            template.setConnectionFactory(factory);
            return template;
        }

        /**
         * 模拟 CacheAutoConfiguration 在 multi-level 模式下注册的失效广播容器
         */
        @Bean
        RedisMessageListenerContainer cacheInvalidationListenerContainer() {
            return mock(RedisMessageListenerContainer.class);
        }
    }
}

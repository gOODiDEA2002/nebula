package io.nebula.autoconfigure.messaging;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.nebula.messaging.rabbitmq.producer.RabbitMQMessageProducer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * RabbitMQAutoConfiguration 三态条件测试：
 * (1) enabled=true + ConnectionFactory 类在 → 有 RabbitMQMessageProducer
 * (2) enabled=false → 无 Bean
 * (3) ConnectionFactory 类缺失 → 配置不加载
 */
class RabbitMQAutoConfigurationConditionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RabbitMQAutoConfiguration.class));

    @Test
    void enabled_hasProducerBean() {
        runner.withPropertyValues("nebula.messaging.rabbitmq.enabled=true")
                .withUserConfiguration(MockRabbitDeps.class)
                .run(ctx -> assertThat(ctx).hasSingleBean(RabbitMQMessageProducer.class));
    }

    @Test
    void disabled_noProducerBean() {
        runner.withPropertyValues("nebula.messaging.rabbitmq.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(RabbitMQMessageProducer.class));
    }

    @Test
    void defaultDisabled_noProducerBean() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(RabbitMQMessageProducer.class));
    }

    @Test
    void missingClass_noProducerBean() {
        runner.withPropertyValues("nebula.messaging.rabbitmq.enabled=true")
                .withClassLoader(new FilteredClassLoader(ConnectionFactory.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(RabbitMQMessageProducer.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class MockRabbitDeps {
        @Bean
        Connection rabbitMQConnection() {
            return mock(Connection.class);
        }
    }
}

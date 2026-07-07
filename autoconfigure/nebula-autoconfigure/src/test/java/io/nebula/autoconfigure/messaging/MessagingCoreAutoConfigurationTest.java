package io.nebula.autoconfigure.messaging;

import io.nebula.messaging.core.annotation.MessageHandlerProcessor;
import io.nebula.messaging.core.manager.MessageManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 双 MQ 并存冲突修复验证（MW-20）
 *
 * <p>此前 RabbitMQ/RocketMQ 自动配置各自 @Primary 自己的 MessageManager,
 * 双 MQ 同时启用时按类型注入直接崩溃。现由 MessagingCoreAutoConfiguration
 * 按 nebula.messaging.primary（默认 rabbitmq）统一选主。</p>
 */
class MessagingCoreAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MessagingCoreAutoConfiguration.class));

    @Test
    @DisplayName("双 MQ 并存: 默认 rabbitmq 为 primary, 上下文不再崩溃")
    void dualManagersDefaultPrimaryIsRabbit(){
        runner.withUserConfiguration(DualManagerConfig.class).run(context -> {
            assertThat(context).hasNotFailed();
            MessageManager primary = context.getBean(MessageManager.class);
            assertThat(primary).isSameAs(context.getBean("rabbitMQMessageManager"));
            assertThat(context).hasSingleBean(MessageHandlerProcessor.class);
        });
    }

    @Test
    @DisplayName("nebula.messaging.primary=rocketmq: rocketmq 为 primary")
    void dualManagersExplicitRocketPrimary() {
        runner.withUserConfiguration(DualManagerConfig.class)
                .withPropertyValues("nebula.messaging.primary=rocketmq")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    MessageManager primary = context.getBean(MessageManager.class);
                    assertThat(primary).isSameAs(context.getBean("rocketMQMessageManager"));
                });
    }

    @Test
    @DisplayName("primary 配置值无匹配: 启动失败并列出候选(而非静默任选)")
    void unmatchedPrimaryFailsFast() {
        runner.withUserConfiguration(DualManagerConfig.class)
                .withPropertyValues("nebula.messaging.primary=kafka")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("kafka")
                            .hasStackTraceContaining("rabbitMQMessageManager");
                });
    }

    @Test
    @DisplayName("单 MQ: 不做选主干预, 注解处理器直接绑定唯一 Manager")
    void singleManagerNeedsNoPrimary() {
        runner.withUserConfiguration(SingleManagerConfig.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(MessageManager.class))
                    .isSameAs(context.getBean("rabbitMQMessageManager"));
            assertThat(context).hasSingleBean(MessageHandlerProcessor.class);
        });
    }

    @Test
    @DisplayName("无任何 MQ: 不注册注解处理器")
    void noManagerNoProcessor() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(MessageHandlerProcessor.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class DualManagerConfig {
        @Bean
        MessageManager rabbitMQMessageManager() {
            return mock(MessageManager.class);
        }

        @Bean
        MessageManager rocketMQMessageManager() {
            return mock(MessageManager.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class SingleManagerConfig {
        @Bean
        MessageManager rabbitMQMessageManager() {
            return mock(MessageManager.class);
        }
    }
}

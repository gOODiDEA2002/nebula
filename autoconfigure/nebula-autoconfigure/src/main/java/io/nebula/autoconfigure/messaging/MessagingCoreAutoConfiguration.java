package io.nebula.autoconfigure.messaging;

import io.nebula.messaging.core.annotation.MessageHandlerProcessor;
import io.nebula.messaging.core.manager.MessageManager;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * 消息核心共享自动配置（MW-20）
 *
 * <p>此前 RabbitMQ 与 RocketMQ 的自动配置各自将自己的 MessageManager 标 {@code @Primary},
 * 且各自注册一个 {@link MessageHandlerProcessor}: 双 MQ 同时启用时按类型注入
 * {@link MessageManager} 直接抛 NoUniqueBeanDefinitionException(两个 primary),
 * {@code @MessageListener} 注册到哪个 MQ 取决于自动配置处理顺序(隐式且脆弱)。</p>
 *
 * <p>现统一为: 各实现配置不再标 @Primary, 由本配置的 BeanFactoryPostProcessor 按
 * {@code nebula.messaging.primary}(默认 rabbitmq)在多 Manager 并存时动态选定唯一 primary;
 * {@link MessageHandlerProcessor} 只在此注册一份, 注入选定的 primary Manager,
 * 即 @MessageListener 始终注册到 primary MQ。</p>
 *
 * @author Nebula Framework
 * @since 2.0.1
 */
@AutoConfiguration(after = {RabbitMQAutoConfiguration.class, RocketMQAutoConfiguration.class})
@ConditionalOnClass(MessageManager.class)
public class MessagingCoreAutoConfiguration {

    /**
     * 多 MessageManager 并存时按 nebula.messaging.primary 选定唯一 primary。
     * 匹配规则: bean 名称(小写)包含配置值(小写), 如 rabbitmq → rabbitMQMessageManager。
     */
    @Bean
    public static BeanFactoryPostProcessor messageManagerPrimarySelector(Environment environment) {
        return beanFactory -> {
            String[] names = beanFactory.getBeanNamesForType(MessageManager.class, true, false);
            if (names.length <= 1) {
                return;
            }
            String primary = environment.getProperty("nebula.messaging.primary", "rabbitmq");
            String keyword = primary.toLowerCase();
            String matched = null;
            for (String name : names) {
                if (name.toLowerCase().contains(keyword)) {
                    matched = name;
                    break;
                }
            }
            if (matched == null) {
                throw new IllegalStateException(
                        "nebula.messaging.primary=" + primary + " 未匹配任何 MessageManager Bean, 候选: "
                                + Arrays.toString(names));
            }
            for (String name : names) {
                BeanDefinition definition = beanFactory.getBeanDefinition(name);
                definition.setPrimary(name.equals(matched));
            }
        };
    }

    /**
     * 统一的 @MessageListener/@MessageHandler 注解处理器, 注册到 primary MessageManager
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(MessageManager.class)
    public static MessageHandlerProcessor messageHandlerProcessor(@Lazy MessageManager messageManager) {
        return new MessageHandlerProcessor(messageManager);
    }
}

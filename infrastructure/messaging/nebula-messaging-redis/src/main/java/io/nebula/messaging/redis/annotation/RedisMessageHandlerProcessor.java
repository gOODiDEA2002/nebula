package io.nebula.messaging.redis.annotation;

import io.nebula.messaging.core.consumer.MessageHandler;
import io.nebula.messaging.core.message.Message;
import io.nebula.messaging.redis.consumer.RedisMessageConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * RedisMessageHandler 注解处理器
 * <p>
 * 扫描 Bean 中标注了 @RedisMessageHandler 的方法，并自动注册为消息监听器。
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class RedisMessageHandlerProcessor implements BeanPostProcessor {

    private final RedisMessageConsumer<?> messageConsumer;
    private final Executor asyncExecutor;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();

        ReflectionUtils.doWithMethods(targetClass, method -> {
            RedisMessageHandler annotation = AnnotationUtils.findAnnotation(method, RedisMessageHandler.class);
            if (annotation != null) {
                processHandler(bean, method, annotation);
            }
        });

        return bean;
    }

    /**
     * 处理单个消息处理器方法
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void processHandler(Object bean, Method method, RedisMessageHandler annotation) {
        String channel = annotation.channel();
        String pattern = annotation.pattern();

        if (!StringUtils.hasText(channel) && !StringUtils.hasText(pattern)) {
            log.warn("@RedisMessageHandler 注解必须指定 channel 或 pattern: {}.{}", 
                    bean.getClass().getSimpleName(), method.getName());
            return;
        }

        if (StringUtils.hasText(channel) && StringUtils.hasText(pattern)) {
            log.warn("@RedisMessageHandler 注解不能同时指定 channel 和 pattern: {}.{}", 
                    bean.getClass().getSimpleName(), method.getName());
            return;
        }

        // 创建消息处理器
        MessageHandler<?> handler = createHandler(bean, method, annotation);

        // 注册订阅
        if (StringUtils.hasText(channel)) {
            log.info("注册 Redis 消息处理器: channel={}, method={}.{}", 
                    channel, bean.getClass().getSimpleName(), method.getName());
            ((RedisMessageConsumer) messageConsumer).subscribe(channel, handler);
        } else {
            log.info("注册 Redis 消息处理器: pattern={}, method={}.{}", 
                    pattern, bean.getClass().getSimpleName(), method.getName());
            ((RedisMessageConsumer) messageConsumer).subscribePattern(pattern, handler);
        }
    }

    /**
     * 创建消息处理器
     */
    @SuppressWarnings("unchecked")
    private <T> MessageHandler<T> createHandler(Object bean, Method method, RedisMessageHandler annotation) {
        boolean async = annotation.async();
        boolean throwOnError = annotation.throwOnError();
        Class<?> payloadType = annotation.payloadType();

        // 如果没有指定载荷类型，从方法参数推断
        if (payloadType == Object.class && method.getParameterCount() > 0) {
            Class<?> paramType = method.getParameterTypes()[0];
            if (Message.class.isAssignableFrom(paramType)) {
                // 如果参数是 Message 类型，尝试从泛型获取载荷类型
                // 这里简化处理，使用 Object
                payloadType = Object.class;
            } else {
                payloadType = paramType;
            }
        }

        final Class<?> finalPayloadType = payloadType;

        return new MessageHandler<T>() {
            @Override
            public void handle(Message<T> message) {
                Runnable task = () -> {
                    try {
                        method.setAccessible(true);
                        
                        // 根据方法参数类型决定传递什么
                        if (method.getParameterCount() == 0) {
                            method.invoke(bean);
                        } else if (method.getParameterCount() == 1) {
                            Class<?> paramType = method.getParameterTypes()[0];
                            if (Message.class.isAssignableFrom(paramType)) {
                                method.invoke(bean, message);
                            } else {
                                // 直接传递载荷
                                method.invoke(bean, message.getPayload());
                            }
                        } else {
                            log.warn("消息处理方法参数数量不正确: {}.{}", 
                                    bean.getClass().getSimpleName(), method.getName());
                        }
                    } catch (Exception e) {
                        log.error("消息处理失败: method={}.{}, error={}", 
                                bean.getClass().getSimpleName(), method.getName(), e.getMessage(), e);
                        if (throwOnError) {
                            throw new RuntimeException("消息处理失败", e);
                        }
                    }
                };

                if (async && asyncExecutor != null) {
                    asyncExecutor.execute(task);
                } else {
                    task.run();
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<T> getMessageType() {
                return (Class<T>) finalPayloadType;
            }
        };
    }
}


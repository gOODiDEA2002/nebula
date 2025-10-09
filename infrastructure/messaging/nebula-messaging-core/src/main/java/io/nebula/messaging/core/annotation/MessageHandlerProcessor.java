package io.nebula.messaging.core.annotation;

import io.nebula.messaging.core.consumer.MessageConsumer;
import io.nebula.messaging.core.manager.MessageManager;
import io.nebula.messaging.core.message.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * 消息处理器注解处理器
 * 扫描并注册带有 @MessageHandler 注解的方法
 * 
 * @author nebula
 */
@Slf4j
@Component
public class MessageHandlerProcessor implements BeanPostProcessor {
    
    private final MessageManager messageManager;
    
    public MessageHandlerProcessor(MessageManager messageManager) {
        this.messageManager = messageManager;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        
        ReflectionUtils.doWithMethods(targetClass, method -> {
            MessageHandler annotation = AnnotationUtils.findAnnotation(method, MessageHandler.class);
            if (annotation != null) {
                registerMessageHandler(bean, method, annotation);
            }
        }, this::isHandlerMethod);
        
        return bean;
    }
    
    /**
     * 判断是否是处理器方法
     */
    private boolean isHandlerMethod(Method method) {
        return AnnotationUtils.findAnnotation(method, MessageHandler.class) != null;
    }
    
    /**
     * 注册消息处理器
     */
    private void registerMessageHandler(Object bean, Method method, MessageHandler annotation) {
        // 获取主题名称
        String topic = determineTopic(annotation, method);
        
        // 获取队列名称
        String queue = determineQueue(annotation, topic);
        
        // 获取标签
        String tag = annotation.tag();
        
        log.info("注册消息处理器: bean={}, method={}, topic={}, queue={}, tag={}", 
                bean.getClass().getSimpleName(), method.getName(), topic, queue, tag);
        
        // 创建消息处理器
        io.nebula.messaging.core.consumer.MessageHandler<Object> handler = createHandler(bean, method);
        
        // 配置消费者
        configureConsumer(annotation);
        
        // 注册处理器
        @SuppressWarnings("unchecked")
        MessageConsumer<Object> consumer = (MessageConsumer<Object>) messageManager.getConsumer();
        if (StringUtils.hasText(tag)) {
            consumer.subscribeWithTag(topic, tag, handler);
        } else if (StringUtils.hasText(queue)) {
            consumer.subscribe(topic, queue, handler);
        } else {
            consumer.subscribe(topic, handler);
        }
        
        log.info("消息处理器注册成功: topic={}, queue={}, method={}", 
                topic, queue, method.getName());
    }
    
    /**
     * 确定主题名称
     */
    private String determineTopic(MessageHandler annotation, Method method) {
        // 优先使用 value
        if (StringUtils.hasText(annotation.value())) {
            return annotation.value();
        }
        
        // 其次使用 topic
        if (StringUtils.hasText(annotation.topic())) {
            return annotation.topic();
        }
        
        // 默认使用方法所在类的简单名称
        return method.getDeclaringClass().getSimpleName().toLowerCase();
    }
    
    /**
     * 确定队列名称
     */
    private String determineQueue(MessageHandler annotation, String topic) {
        if (StringUtils.hasText(annotation.queue())) {
            return annotation.queue();
        }
        
        // 默认与主题名称相同
        return topic;
    }
    
    /**
     * 创建消息处理器
     */
    private io.nebula.messaging.core.consumer.MessageHandler<Object> createHandler(Object bean, Method method) {
        // 提取方法参数的泛型类型
        Class<?> messageType = extractMessageType(method);
        
        return new io.nebula.messaging.core.consumer.MessageHandler<Object>() {
            @Override
            public void handle(Message<Object> message) {
                try {
                    // 使方法可访问
                    ReflectionUtils.makeAccessible(method);
                    
                    // 调用处理方法
                    if (method.getParameterCount() == 1) {
                        // 如果方法只有一个参数，传递整个 Message 对象
                        method.invoke(bean, message);
                    } else if (method.getParameterCount() == 0) {
                        // 如果没有参数，直接调用
                        method.invoke(bean);
                    } else {
                        log.warn("消息处理方法参数数量不正确: method={}, paramCount={}", 
                                method.getName(), method.getParameterCount());
                    }
                } catch (Exception e) {
                    log.error("消息处理失败: method={}, message={}, error={}", 
                            method.getName(), message.getId(), e.getMessage(), e);
                    throw new RuntimeException("消息处理失败", e);
                }
            }
            
            @Override
            @SuppressWarnings("unchecked")
            public Class<Object> getMessageType() {
                return (Class<Object>) messageType;
            }
        };
    }
    
    /**
     * 从方法参数中提取消息载荷类型
     * 
     * @param method 处理方法
     * @return 消息载荷类型
     */
    private Class<?> extractMessageType(Method method) {
        if (method.getParameterCount() == 0) {
            return Object.class;
        }
        
        // 获取第一个参数类型
        java.lang.reflect.Type paramType = method.getGenericParameterTypes()[0];
        
        // 检查是否是 Message<T> 类型
        if (paramType instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType pType = (java.lang.reflect.ParameterizedType) paramType;
            
            // 确认原始类型是 Message
            if (pType.getRawType() == Message.class) {
                // 获取泛型参数 T
                java.lang.reflect.Type[] actualTypeArguments = pType.getActualTypeArguments();
                if (actualTypeArguments.length > 0) {
                    java.lang.reflect.Type actualType = actualTypeArguments[0];
                    if (actualType instanceof Class) {
                        log.debug("提取到消息载荷类型: method={}, type={}", 
                                method.getName(), ((Class<?>) actualType).getName());
                        return (Class<?>) actualType;
                    }
                }
            }
        }
        
        log.warn("无法提取消息载荷类型，使用 Object.class: method={}", method.getName());
        return Object.class;
    }
    
    /**
     * 配置消费者
     */
    private void configureConsumer(MessageHandler annotation) {
        MessageConsumer<?> consumer = messageManager.getConsumer();
        
        if (consumer.getConfig() == null) {
            return;
        }
        
        // 设置消费者组
        if (StringUtils.hasText(annotation.consumerGroup())) {
            consumer.getConfig().setConsumerGroup(annotation.consumerGroup());
        }
        
        // 设置并发数
        if (annotation.concurrency() > 0) {
            consumer.getConfig().setConcurrency(annotation.concurrency());
        }
        
        // 设置自动确认
        consumer.getConfig().setAutoAck(annotation.autoAck());
        
        // 设置最大重试次数
        if (annotation.maxRetries() > 0) {
            consumer.getConfig().setMaxRetries(annotation.maxRetries());
        }
    }
}


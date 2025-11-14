package io.nebula.messaging.rabbitmq.delay;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Set;

/**
 * 延时消息监听器注解处理器
 * 
 * 扫描所有标记了 @DelayMessageListener 的方法，
 * 自动注册到 DelayMessageConsumer
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@Component
public class DelayMessageListenerProcessor implements BeanPostProcessor, ApplicationContextAware {
    
    private ApplicationContext applicationContext;
    private DelayMessageConsumer delayMessageConsumer;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 延迟获取 DelayMessageConsumer，避免循环依赖
        if (delayMessageConsumer == null) {
            try {
                delayMessageConsumer = applicationContext.getBean(DelayMessageConsumer.class);
            } catch (Exception e) {
                log.debug("DelayMessageConsumer not available yet, skipping delay message listener registration for bean: {}", beanName);
                return bean;
            }
        }
        
        // 扫描bean中标记了 @DelayMessageListener 的方法
        Class<?> targetClass = bean.getClass();
        Map<Method, DelayMessageListener> annotatedMethods = MethodIntrospector.selectMethods(
            targetClass,
            (MethodIntrospector.MetadataLookup<DelayMessageListener>) method ->
                AnnotatedElementUtils.findMergedAnnotation(method, DelayMessageListener.class)
        );
        
        // 注册每个监听器方法
        annotatedMethods.forEach((method, annotation) -> {
            try {
                registerDelayMessageListener(bean, method, annotation);
            } catch (Exception e) {
                throw new IllegalStateException(
                    "Failed to register delay message listener: " + method.getName(), e);
            }
        });
        
        return bean;
    }
    
    /**
     * 注册延时消息监听器
     */
    @SuppressWarnings("unchecked")
    private void registerDelayMessageListener(Object bean, Method method, DelayMessageListener annotation) throws IOException {
        // 验证方法签名
        validateMethodSignature(method);
        
        // 提取消息载荷类型
        Class<?> messageType = extractMessageType(method);
        
        // 获取配置
        String queue = annotation.queue();
        String topic = annotation.topic().isEmpty() ? queue : annotation.topic();
        
        // 创建处理器适配器
        DelayMessageConsumer.DelayMessageHandler handler = (payload, context) -> {
            try {
                method.setAccessible(true);
                method.invoke(bean, payload, context);
            } catch (Exception e) {
                log.error("Error invoking delay message listener method: bean={}, method={}", 
                    bean.getClass().getSimpleName(), method.getName(), e);
                throw new RuntimeException("Failed to invoke delay message listener", e);
            }
        };
        
        // 注册到 DelayMessageConsumer
        delayMessageConsumer.subscribe(queue, (Class)messageType, handler);
        
        log.info("注册延时消息监听器: bean={}, method={}, queue={}, topic={}, messageType={}", 
            bean.getClass().getSimpleName(), method.getName(), queue, topic, messageType.getSimpleName());
    }
    
    /**
     * 验证方法签名
     * 方法必须有两个参数：消息载荷和延时消息上下文
     */
    private void validateMethodSignature(Method method) {
        Parameter[] parameters = method.getParameters();
        
        if (parameters.length != 2) {
            throw new IllegalArgumentException(
                String.format("@DelayMessageListener method must have exactly 2 parameters, but found %d: %s.%s",
                    parameters.length, method.getDeclaringClass().getSimpleName(), method.getName()));
        }
        
        // 第二个参数必须是 DelayMessageContext
        Class<?> secondParamType = parameters[1].getType();
        if (!DelayMessageContext.class.isAssignableFrom(secondParamType)) {
            throw new IllegalArgumentException(
                String.format("@DelayMessageListener method's second parameter must be DelayMessageContext: %s.%s",
                    method.getDeclaringClass().getSimpleName(), method.getName()));
        }
    }
    
    /**
     * 提取消息载荷类型（第一个参数）
     */
    private Class<?> extractMessageType(Method method) {
        Parameter[] parameters = method.getParameters();
        return parameters[0].getType();
    }
}


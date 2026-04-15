package io.nebula.messaging.core.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 消息处理器注解
 * 
 * @deprecated 请使用 {@link MessageListener}，避免与 {@link io.nebula.messaging.core.consumer.MessageHandler} 接口命名冲突。
 *             将在 3.0.0 版本移除。
 *
 * @author nebula
 */
@Deprecated(since = "2.0.1", forRemoval = true)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface MessageHandler {
    
    /**
     * 消息主题
     * 默认为方法所在类的简单名称
     * 
     * @return 主题名称
     */
    String value() default "";
    
    /**
     * 消息主题（与 value 作用相同）
     * 
     * @return 主题名称
     */
    String topic() default "";
    
    /**
     * 消息队列
     * 如果不指定，默认与主题名称相同
     * 
     * @return 队列名称
     */
    String queue() default "";
    
    /**
     * 消息标签（用于过滤）
     * 
     * @return 标签
     */
    String tag() default "";
    
    /**
     * 消费者组
     * 
     * @return 消费者组名称
     */
    String consumerGroup() default "default";
    
    /**
     * 并发消费线程数
     * 
     * @return 线程数
     */
    int concurrency() default 1;
    
    /**
     * 是否自动确认
     * 
     * @return 是否自动确认
     */
    boolean autoAck() default false;
    
    /**
     * 最大重试次数
     * 
     * @return 最大重试次数
     */
    int maxRetries() default 3;
    
    /**
     * 描述信息
     * 
     * @return 描述
     */
    String description() default "";
}


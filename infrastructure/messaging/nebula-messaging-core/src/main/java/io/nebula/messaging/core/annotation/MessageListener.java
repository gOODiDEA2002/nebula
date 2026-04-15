package io.nebula.messaging.core.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 消息监听方法注解
 * 用于标注消息处理方法，自动注册到消息管理器。
 * <p>
 * 替代 {@link MessageHandler}，避免与 {@link io.nebula.messaging.core.consumer.MessageHandler} 接口命名冲突。
 * </p>
 *
 * @author Nebula Framework
 * @since 2.0.1
 * @see MessageHandler
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface MessageListener {

    String value() default "";

    String topic() default "";

    String queue() default "";

    String tag() default "";

    String consumerGroup() default "default";

    int concurrency() default 1;

    boolean autoAck() default false;

    int maxRetries() default 3;

    String description() default "";
}

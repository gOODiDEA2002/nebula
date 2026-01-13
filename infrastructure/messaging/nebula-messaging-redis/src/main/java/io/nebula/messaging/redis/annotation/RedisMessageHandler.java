package io.nebula.messaging.redis.annotation;

import java.lang.annotation.*;

/**
 * Redis 消息处理器注解
 * <p>
 * 标注在方法上，用于声明该方法为 Redis 消息处理器。
 * 支持频道订阅和模式订阅。
 * </p>
 *
 * <pre>
 * 使用示例:
 * 
 * // 频道订阅
 * &#64;RedisMessageHandler(channel = "user:notification")
 * public void handleNotification(Message&lt;Notification&gt; message) {
 *     // 处理消息
 * }
 * 
 * // 模式订阅
 * &#64;RedisMessageHandler(pattern = "user:*")
 * public void handleUserEvents(Message&lt;UserEvent&gt; message) {
 *     // 处理消息
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisMessageHandler {

    /**
     * 订阅的频道名称（与 pattern 互斥）
     *
     * @return 频道名称
     */
    String channel() default "";

    /**
     * 订阅的模式（支持 * 和 ? 通配符，与 channel 互斥）
     *
     * @return 模式
     */
    String pattern() default "";

    /**
     * 消息载荷类型
     * <p>
     * 如果不指定，则从方法参数推断
     * </p>
     *
     * @return 载荷类型
     */
    Class<?> payloadType() default Object.class;

    /**
     * 是否异步处理
     *
     * @return 是否异步
     */
    boolean async() default false;

    /**
     * 处理失败时是否抛出异常
     *
     * @return 是否抛出异常
     */
    boolean throwOnError() default false;
}


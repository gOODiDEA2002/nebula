package io.nebula.messaging.rabbitmq.delay;

import java.lang.annotation.*;

/**
 * 延时消息监听器注解
 * 
 * 标记在方法上，用于自动注册延时消息处理器
 * 
 * 使用示例：
 * <pre>
 * {@code
 * @Component
 * public class OrderHandler {
 *     
 *     @DelayMessageListener(queue = "order.timeout.queue")
 *     public void handleOrderTimeout(OrderTimeoutEvent event, DelayMessageContext context) {
 *         log.info("订单超时处理: orderId={}, delay={}ms", 
 *             event.getOrderId(), context.getTotalDelay());
 *         // 处理订单超时逻辑
 *     }
 * }
 * }
 * </pre>
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DelayMessageListener {
    
    /**
     * 目标队列名称（必填）
     */
    String queue();
    
    /**
     * 目标主题（可选）
     * 如果不指定，将使用队列名称作为主题
     */
    String topic() default "";
    
    /**
     * 并发消费者数量
     * 默认1个消费者
     */
    int concurrency() default 1;
    
    /**
     * 最大重试次数
     * 默认3次
     */
    int maxRetries() default 3;
    
    /**
     * 是否自动启动
     * 默认true
     */
    boolean autoStartup() default true;
}


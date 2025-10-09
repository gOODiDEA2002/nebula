package io.nebula.rpc.core.annotation;

import java.lang.annotation.*;

/**
 * RPC客户端注解
 * 用于标记RPC客户端接口，支持声明式服务调用
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RpcClient {
    
    /**
     * 服务名称（用于服务发现）
     * 如果为空，则使用接口的全限定名
     */
    String value() default "";
    
    /**
     * 服务名称（等同于 value）
     */
    String name() default "";
    
    /**
     * 服务URL（直接指定服务地址）
     * 如果指定了URL，则不使用服务发现
     */
    String url() default "";
    
    /**
     * 上下文ID（用于区分同一服务的不同客户端）
     */
    String contextId() default "";
    
    /**
     * 配置类（用于自定义客户端配置）
     */
    Class<?>[] configuration() default {};
    
    /**
     * 降级处理类（当服务不可用时的降级逻辑）
     */
    Class<?> fallback() default void.class;
    
    /**
     * 降级工厂类（用于创建降级处理实例）
     */
    Class<?> fallbackFactory() default void.class;
    
    /**
     * 连接超时时间（毫秒）
     */
    int connectTimeout() default 30000;
    
    /**
     * 读取超时时间（毫秒）
     */
    int readTimeout() default 60000;
}


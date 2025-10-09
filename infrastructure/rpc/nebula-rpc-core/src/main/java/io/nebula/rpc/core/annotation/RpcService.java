package io.nebula.rpc.core.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * RPC服务端实现注解
 * 标记一个类作为RPC服务的服务端实现
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RpcService {
    
    /**
     * 实现的RPC服务接口类
     */
    Class<?> value();
    
    /**
     * 服务名称,默认使用接口的全限定名
     */
    String serviceName() default "";
}


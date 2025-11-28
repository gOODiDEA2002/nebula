package io.nebula.rpc.core.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.*;

/**
 * RPC调用注解
 * 用于标记RPC方法，指定调用的路径和HTTP方法
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RpcCall {
    
    /**
     * 请求路径
     */
    @AliasFor("path")
    String value() default "";
    
    /**
     * 请求路径（等同于 value）
     */
    @AliasFor("value")
    String path() default "";
    
    /**
     * HTTP方法（GET, POST, PUT, DELETE等）
     * 使用 "*" 或 "ANY" 表示接受所有HTTP方法
     * 默认为 "*"（接受所有方法）
     */
    String method() default "*";
    
    /**
     * 请求头
     */
    String[] headers() default {};
    
    /**
     * 请求参数
     */
    String[] params() default {};
    
    /**
     * 内容类型
     */
    String contentType() default "application/json";
    
    /**
     * 接受类型
     */
    String[] produces() default {};
    
    /**
     * 消费类型
     */
    String[] consumes() default {};
}


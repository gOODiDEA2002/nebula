package io.nebula.rpc.core.annotation;

import io.nebula.rpc.core.scan.RpcClientScannerRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用RPC客户端扫描注解
 * 在Spring Boot应用中使用此注解来自动扫描和注册RPC客户端
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(RpcClientScannerRegistrar.class)
public @interface EnableRpcClients {
    
    /**
     * 扫描的基础包路径
     * 如果为空，则扫描标注类所在的包及其子包
     */
    String[] value() default {};
    
    /**
     * 扫描的基础包路径（等同于 value）
     */
    String[] basePackages() default {};
    
    /**
     * 扫描的基础类
     * 将扫描这些类所在的包
     */
    Class<?>[] basePackageClasses() default {};
    
    /**
     * 默认配置类
     */
    Class<?>[] defaultConfiguration() default {};
    
    /**
     * 指定要扫描的RPC客户端接口
     */
    Class<?>[] clients() default {};
}


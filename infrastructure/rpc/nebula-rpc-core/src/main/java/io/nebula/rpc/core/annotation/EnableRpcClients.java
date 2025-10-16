package io.nebula.rpc.core.annotation;

import io.nebula.rpc.core.scan.RpcClientScannerRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用RPC客户端扫描注解
 * 在Spring Boot应用中使用此注解来自动扫描和注册RPC客户端
 * 
 * 使用方式：
 * 1. 指定默认服务名：@EnableRpcClients("nebula-example-user-service")
 * 2. 指定扫描包：@EnableRpcClients(basePackages = "com.example.api.rpc")
 * 3. 同时指定：@EnableRpcClients(value = "service-name", basePackages = "com.example.api.rpc")
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
     * 默认服务名（用于服务发现）
     * 所有扫描到的 @RpcClient 接口如果没有指定服务名，则使用此默认值
     * 
     * 简写方式：@EnableRpcClients("service-name")
     * 完整方式：@EnableRpcClients(value = "service-name", basePackages = "...")
     */
    String[] value() default {};
    
    /**
     * 扫描的基础包路径
     * 如果为空，则扫描标注类所在的包及其子包
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


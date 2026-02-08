package io.nebula.example.microservice.api;

import io.nebula.rpc.core.annotation.EnableRpcClients;
import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * Microservice API 自动配置类
 * 
 * 自动扫描并注册当前包及子包下所有的 @RpcClient 接口
 * 当其他服务依赖此 API 模块时，会自动注册 RPC 客户端代理
 * 
 * 使用方式：
 * 1. 在服务的 pom.xml 中添加 microservice-api 依赖
 * 2. Spring Boot 会自动发现并加载此配置类
 * 3. 无需在启动类添加 @EnableRpcClients 注解
 * 
 * @author Nebula Framework Team
 * @since 1.0.0
 */
@AutoConfiguration
@EnableRpcClients(basePackages = "io.nebula.example.microservice.api")
public class MicroserviceApiAutoConfiguration {
}

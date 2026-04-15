package io.nebula.example.order.api;

import io.nebula.rpc.core.annotation.EnableRpcClients;
import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * Order API 自动配置类
 * 自动扫描并注册当前包及子包下所有的 @RemoteService 接口
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@AutoConfiguration
@EnableRpcClients("nebula-example-order-service")
public class OrderApiAutoConfiguration {

}


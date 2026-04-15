package io.nebula.example.user.api;

import io.nebula.rpc.core.annotation.EnableRpcClients;
import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * User API 自动配置类
 * 自动扫描并注册当前包及子包下所有的 @RemoteService 接口
 * 默认服务名：nebula-example-user-service
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@AutoConfiguration
@EnableRpcClients("nebula-example-user-service")
public class UserApiAutoConfiguration {
}


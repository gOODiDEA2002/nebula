package io.nebula.autoconfigure.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Gateway gRPC Server 排除配置
 * <p>
 * 通过 EnvironmentPostProcessor 在 Spring Boot 启动早期阶段，
 * 自动添加 grpc-spring-boot-starter 服务器端自动配置的排除项。
 * <p>
 * 网关不需要启动 gRPC 服务器，只需要 gRPC 客户端功能。
 * <p>
 * 排除的配置类：
 * <ul>
 *   <li>GrpcServerAutoConfiguration - gRPC 服务器核心配置</li>
 *   <li>GrpcServerFactoryAutoConfiguration - gRPC 服务器工厂配置</li>
 *   <li>GrpcServerSecurityAutoConfiguration - gRPC 服务器安全配置</li>
 * </ul>
 */
public class GatewayGrpcServerExcludeConfiguration implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "nebulaGatewayExcludes";
    
    private static final String[] GRPC_SERVER_AUTO_CONFIGURATIONS = {
        "net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration",
        "net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration",
        "net.devh.boot.grpc.server.autoconfigure.GrpcServerSecurityAutoConfiguration"
    };

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // 检查是否为 Gateway 项目（通过检查类路径）
        if (!isGatewayProject()) {
            return;
        }

        // 获取现有的排除配置
        String existingExcludes = environment.getProperty("spring.autoconfigure.exclude", "");
        
        // 构建新的排除列表
        StringBuilder excludes = new StringBuilder(existingExcludes);
        for (String config : GRPC_SERVER_AUTO_CONFIGURATIONS) {
            if (!existingExcludes.contains(config)) {
                if (excludes.length() > 0) {
                    excludes.append(",");
                }
                excludes.append(config);
            }
        }

        // 添加到环境属性
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.autoconfigure.exclude", excludes.toString());
        
        environment.getPropertySources().addFirst(
            new MapPropertySource(PROPERTY_SOURCE_NAME, properties)
        );
    }
    
    /**
     * 检查是否为 Gateway 项目
     */
    private boolean isGatewayProject() {
        try {
            Class.forName("org.springframework.cloud.gateway.filter.GatewayFilter");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}


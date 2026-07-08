package io.nebula.autoconfigure.env;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * 将旧配置 {@code nebula.rpc.grpc.server.port} 桥接到 {@code spring.grpc.server.port}。
 * <p>
 * 仅当用户显式配置了旧键且未配置新键时才桥接, 以最低优先级注入, 新键始终优先。
 * {@code nebula.rpc.grpc.server.port} 已标记 {@code @Deprecated},
 * 后续版本建议直接使用 {@code spring.grpc.server.port}。
 */
public class NebulaGrpcServerPortBridgePostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String legacy = environment.getProperty("nebula.rpc.grpc.server.port");
        if (legacy == null || environment.getProperty("spring.grpc.server.port") != null) {
            return;
        }
        Map<String, Object> props = Map.of("spring.grpc.server.port", legacy);
        environment.getPropertySources().addLast(
                new MapPropertySource("nebula-grpc-port-bridge", props));
    }
}

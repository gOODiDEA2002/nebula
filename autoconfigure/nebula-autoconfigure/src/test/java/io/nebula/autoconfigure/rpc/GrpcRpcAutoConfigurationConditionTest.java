package io.nebula.autoconfigure.rpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.rpc.grpc.client.GrpcRpcClient;
import io.nebula.rpc.grpc.config.GrpcRpcProperties;
import io.nebula.rpc.grpc.server.GrpcRpcServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GrpcRpcAutoConfiguration 三态条件测试：
 * (1) enabled=true + GrpcRpcClient 类在 → 有 GrpcRpcClient/Server Bean
 * (2) enabled=false → 无 Bean
 * (3) GrpcRpcClient 类缺失 → 配置不加载
 */
class GrpcRpcAutoConfigurationConditionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GrpcRpcAutoConfiguration.class))
            .withUserConfiguration(MockDeps.class);

    @Test
    void enabled_hasGrpcBeans() {
        runner.withPropertyValues("nebula.rpc.grpc.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(GrpcRpcClient.class);
                    assertThat(ctx).hasSingleBean(GrpcRpcServer.class);
                });
    }

    @Test
    void disabled_noGrpcBeans() {
        runner.withPropertyValues("nebula.rpc.grpc.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(GrpcRpcClient.class);
                    assertThat(ctx).doesNotHaveBean(GrpcRpcServer.class);
                });
    }

    @Test
    void defaultDisabled_noGrpcBeans() {
        runner.run(ctx -> {
            assertThat(ctx).doesNotHaveBean(GrpcRpcClient.class);
            assertThat(ctx).doesNotHaveBean(GrpcRpcServer.class);
        });
    }

    @Test
    void missingClass_noGrpcBeans() {
        runner.withPropertyValues("nebula.rpc.grpc.enabled=true")
                .withClassLoader(new FilteredClassLoader(GrpcRpcProperties.class))
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(GrpcRpcClient.class);
                    assertThat(ctx).doesNotHaveBean(GrpcRpcServer.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class MockDeps {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}

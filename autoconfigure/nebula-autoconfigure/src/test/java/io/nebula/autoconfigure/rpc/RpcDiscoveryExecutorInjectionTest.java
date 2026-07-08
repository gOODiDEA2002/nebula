package io.nebula.autoconfigure.rpc;

import io.nebula.discovery.core.LoadBalancer;
import io.nebula.discovery.core.ServiceDiscovery;
import io.nebula.rpc.core.client.RpcClient;
import io.nebula.rpc.core.discovery.ServiceDiscoveryRpcClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证 RpcDiscoveryAutoConfiguration 对 rpcExecutor Bean 的可选注入:
 * - 有 rpcExecutor Bean 时注入它(HTTP RPC 场景)
 * - 无 rpcExecutor Bean 时回落到 ForkJoinPool.commonPool()(gRPC-only 场景)
 */
class RpcDiscoveryExecutorInjectionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RpcDiscoveryAutoConfiguration.class))
            .withUserConfiguration(MockBeansConfig.class)
            .withPropertyValues("nebula.rpc.discovery.enabled=true");

    @Test
    void withRpcExecutorBean_injectsIt() {
        Executor customExecutor = Runnable::run;
        contextRunner
                .withBean("rpcExecutor", Executor.class, () -> customExecutor)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ServiceDiscoveryRpcClient.class);
                    ServiceDiscoveryRpcClient client = ctx.getBean(ServiceDiscoveryRpcClient.class);
                    Executor actual = getAsyncExecutor(client);
                    assertThat(actual).isSameAs(customExecutor);
                });
    }

    @Test
    void withoutRpcExecutorBean_fallsBackToCommonPool() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(ServiceDiscoveryRpcClient.class);
            ServiceDiscoveryRpcClient client = ctx.getBean(ServiceDiscoveryRpcClient.class);
            Executor actual = getAsyncExecutor(client);
            assertThat(actual).isSameAs(ForkJoinPool.commonPool());
        });
    }

    private static Executor getAsyncExecutor(ServiceDiscoveryRpcClient client) throws Exception {
        Field field = ServiceDiscoveryRpcClient.class.getDeclaredField("asyncExecutor");
        field.setAccessible(true);
        return (Executor) field.get(client);
    }

    @Configuration(proxyBeanMethods = false)
    static class MockBeansConfig {
        @Bean
        ServiceDiscovery serviceDiscovery() {
            return mock(ServiceDiscovery.class);
        }

        @Bean
        LoadBalancer loadBalancer() {
            return mock(LoadBalancer.class);
        }

        @Bean
        RpcClient delegateRpcClient() {
            return mock(RpcClient.class);
        }
    }
}

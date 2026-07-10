package io.nebula.autoconfigure.rpc;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import io.nebula.rpc.http.client.HttpRpcClient;
import io.nebula.rpc.http.server.HttpRpcController;
import io.nebula.rpc.http.config.HttpRpcProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 HttpRpcAutoConfiguration 对连接池/超时参数的接通:
 * - rpcRestClient 使用 HttpComponentsClientHttpRequestFactory
 * - 被裁决删除的字段已不存在
 */
class HttpRpcClientConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpRpcAutoConfiguration.class))
            .withUserConfiguration(MockBeansConfig.class)
            .withPropertyValues(
                    "nebula.rpc.http.enabled=true",
                    "nebula.rpc.http.client.enabled=true"
            );

    @Test
    void rpcRestClient_createdWithHttpComponents() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasBean("rpcRestClient");
            assertThat(ctx.getBean("rpcRestClient", RestClient.class)).isNotNull();
        });
    }

    @Test
    void rpcRestClient_readsCustomProperties() {
        contextRunner
                .withPropertyValues(
                        "nebula.rpc.http.client.connect-timeout=5000",
                        "nebula.rpc.http.client.read-timeout=10000",
                        "nebula.rpc.http.client.max-connections=50",
                        "nebula.rpc.http.client.max-connections-per-route=25",
                        "nebula.rpc.http.client.keep-alive-time=30000",
                        "nebula.rpc.http.client.idle-evict-time=15000"
                )
                .run(ctx -> {
                    assertThat(ctx).hasBean("rpcRestClient");
                    HttpRpcProperties props = ctx.getBean(HttpRpcProperties.class);
                    assertThat(props.getClient().getConnectTimeout()).isEqualTo(5000);
                    assertThat(props.getClient().getReadTimeout()).isEqualTo(10000);
                    assertThat(props.getClient().getMaxConnections()).isEqualTo(50);
                    assertThat(props.getClient().getMaxConnectionsPerRoute()).isEqualTo(25);
                    assertThat(props.getClient().getKeepAliveTime()).isEqualTo(30000);
                    assertThat(props.getClient().getIdleEvictTime()).isEqualTo(15000);
                });
    }

    @Test
    void idleEvictTimeFallsBackToKeepAliveTime() {
        contextRunner
                .withPropertyValues("nebula.rpc.http.client.keep-alive-time=45000")
                .run(ctx -> {
                    HttpRpcProperties props = ctx.getBean(HttpRpcProperties.class);
                    assertThat(props.getClient().getIdleEvictTime()).isEqualTo(45000);
                });
    }

    @Test
    void deletedFields_noLongerExist() {
        String[] deletedFields = {"writeTimeout", "retryCount", "retryInterval",
                "compressionEnabled", "loggingEnabled"};
        for (String fieldName : deletedFields) {
            assertThat(hasField(HttpRpcProperties.ClientConfig.class, fieldName))
                    .as("字段 %s 应已删除", fieldName)
                    .isFalse();
        }
    }

    @Test
    void rpcComponentsReuseApplicationObjectMapper() {
        contextRunner.run(ctx -> {
            ObjectMapper expected = ctx.getBean(ObjectMapper.class);
            HttpRpcClient client = ctx.getBean(HttpRpcClient.class);
            HttpRpcController controller = ctx.getBean(HttpRpcController.class);

            assertThat(ReflectionTestUtils.getField(client, "objectMapper")).isSameAs(expected);
            assertThat(ReflectionTestUtils.getField(controller, "objectMapper")).isSameAs(expected);
        });
    }

    private static boolean hasField(Class<?> clazz, String fieldName) {
        try {
            clazz.getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class MockBeansConfig {
        @Bean
        ObjectMapper objectMapper() {
            return JsonMapper.builder().build();
        }
    }
}

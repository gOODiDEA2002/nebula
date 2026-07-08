package io.nebula.autoconfigure.env;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 gRPC 端口桥接 EPP 的三态行为:
 * 1) 只配旧键 -> 新键出现且值一致
 * 2) 两者都配 -> 新键保持用户值
 * 3) 都不配 -> 不注入
 */
class NebulaGrpcServerPortBridgeTest {

    private final NebulaGrpcServerPortBridgePostProcessor processor = new NebulaGrpcServerPortBridgePostProcessor();

    @Test
    void onlyLegacyKey_bridgesToNewKey() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("nebula.rpc.grpc.server.port", "9527");
        processor.postProcessEnvironment(env, null);
        assertThat(env.getProperty("spring.grpc.server.port")).isEqualTo("9527");
    }

    @Test
    void bothKeys_newKeyTakesPrecedence() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("nebula.rpc.grpc.server.port", "9527")
                .withProperty("spring.grpc.server.port", "9999");
        processor.postProcessEnvironment(env, null);
        assertThat(env.getProperty("spring.grpc.server.port")).isEqualTo("9999");
    }

    @Test
    void noKeys_nothingInjected() {
        MockEnvironment env = new MockEnvironment();
        processor.postProcessEnvironment(env, null);
        assertThat(env.getProperty("spring.grpc.server.port")).isNull();
    }
}

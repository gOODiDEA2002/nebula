package io.nebula.rpc.grpc;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.nebula.rpc.grpc.proto.GenericRpcServiceGrpc;
import io.nebula.rpc.grpc.proto.RpcRequest;
import io.nebula.rpc.grpc.proto.RpcResponse;
import io.nebula.rpc.grpc.client.GrpcRpcClient;
import io.nebula.rpc.grpc.config.GrpcRpcProperties;
import io.nebula.rpc.grpc.server.GrpcAuthTokenInterceptor;
import io.nebula.rpc.grpc.server.GrpcRpcServer;
import io.nebula.rpc.grpc.test.TestRpcService;
import io.nebula.rpc.grpc.test.TestRpcServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.grpc.test.autoconfigure.LocalGrpcServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GlobalServerInterceptor;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * gRPC 服务端回环集成测试。
 * <p>
 * 验证 Boot 官方 gRPC Server 自动配置下:
 * <ul>
 *   <li>GrpcRpcServer(BindableService) 被自动挂载并可接收 RPC 调用</li>
 *   <li>{@code @RpcService} 标注的服务实现被扫描注册, 方法路由正确</li>
 *   <li>GrpcAuthTokenInterceptor({@code @GlobalServerInterceptor}) 生效:
 *       无 token 返回 UNAUTHENTICATED, 正确 token 放行</li>
 * </ul>
 */
@SpringBootTest(
        classes = GrpcServerLoopbackIntegrationTest.TestApp.class,
        properties = {
                "spring.grpc.server.port=0",
                "nebula.rpc.grpc.server.auth-token=test-token"
        }
)
class GrpcServerLoopbackIntegrationTest {

    @LocalGrpcServerPort
    private int grpcPort;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 用例一: 带正确 token 的 RPC 回环调用, 验证服务端启动 + 服务注册 + 调用路由
     */
    @Test
    void rpcRoundTripWithValidToken() throws Exception {
        ManagedChannel channel = createChannel(true);
        try {
            GenericRpcServiceGrpc.GenericRpcServiceBlockingStub stub =
                    GenericRpcServiceGrpc.newBlockingStub(channel)
                            .withDeadlineAfter(5, TimeUnit.SECONDS);

            RpcRequest request = RpcRequest.newBuilder()
                    .setRequestId("roundtrip-1")
                    .setServiceName(TestRpcService.class.getName())
                    .setMethodName("sayHello")
                    .addParameterTypes("java.lang.String")
                    .addParameters("\"World\"")
                    .setTimestamp(System.currentTimeMillis())
                    .build();

            RpcResponse response = stub.call(request);

            assertThat(response.getSuccess()).isTrue();
            assertThat(response.getResult()).isEqualTo("\"Hello, World\"");
        } finally {
            channel.shutdownNow().awaitTermination(3, TimeUnit.SECONDS);
        }
    }

    /**
     * 用例二(a): 无 token 调用, 断言 UNAUTHENTICATED
     */
    @Test
    void noTokenRejectedAsUnauthenticated() throws Exception {
        ManagedChannel channel = createChannel(false);
        try {
            GenericRpcServiceGrpc.GenericRpcServiceBlockingStub stub =
                    GenericRpcServiceGrpc.newBlockingStub(channel)
                            .withDeadlineAfter(5, TimeUnit.SECONDS);

            RpcRequest request = RpcRequest.newBuilder()
                    .setRequestId("no-token-1")
                    .setServiceName(TestRpcService.class.getName())
                    .setMethodName("sayHello")
                    .addParameterTypes("java.lang.String")
                    .addParameters("\"World\"")
                    .setTimestamp(System.currentTimeMillis())
                    .build();

            assertThatThrownBy(() -> stub.call(request))
                    .isInstanceOf(StatusRuntimeException.class)
                    .satisfies(ex -> {
                        Status status = ((StatusRuntimeException) ex).getStatus();
                        assertThat(status.getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
                    });
        } finally {
            channel.shutdownNow().awaitTermination(3, TimeUnit.SECONDS);
        }
    }

    /**
     * 用例二(b): 带正确 token 调用成功(不同方法验证路由)
     */
    @Test
    void correctTokenWithDifferentMethod() throws Exception {
        ManagedChannel channel = createChannel(true);
        try {
            GenericRpcServiceGrpc.GenericRpcServiceBlockingStub stub =
                    GenericRpcServiceGrpc.newBlockingStub(channel)
                            .withDeadlineAfter(5, TimeUnit.SECONDS);

            RpcRequest request = RpcRequest.newBuilder()
                    .setRequestId("with-token-1")
                    .setServiceName(TestRpcService.class.getName())
                    .setMethodName("add")
                    .addParameterTypes("java.lang.Integer")
                    .addParameterTypes("java.lang.Integer")
                    .addParameters("10")
                    .addParameters("20")
                    .setTimestamp(System.currentTimeMillis())
                    .build();

            RpcResponse response = stub.call(request);

            assertThat(response.getSuccess()).isTrue();
            assertThat(response.getResult()).isEqualTo("30");
        } finally {
            channel.shutdownNow().awaitTermination(3, TimeUnit.SECONDS);
        }
    }

    @Test
    void frameworkClientAddsConfiguredAuthToken() {
        GrpcRpcProperties.ClientConfig config = new GrpcRpcProperties.ClientConfig();
        config.setTarget("localhost:" + grpcPort);
        config.setAuthToken("test-token");
        config.setRequestTimeout(5_000);

        GrpcRpcClient client = new GrpcRpcClient(objectMapper, config);
        try {
            String result = client.call(TestRpcService.class, "sayHello", "World");
            assertThat(result).isEqualTo("Hello, World");
        } finally {
            client.close();
        }
    }

    private ManagedChannel createChannel(boolean includeToken) {
        NettyChannelBuilder builder = NettyChannelBuilder
                .forAddress("localhost", grpcPort)
                .usePlaintext();

        if (includeToken) {
            builder.intercept(new TokenClientInterceptor("test-token"));
        }

        return builder.build();
    }

    /**
     * 客户端拦截器: 向每次调用的 metadata 注入 auth token
     */
    private static class TokenClientInterceptor implements ClientInterceptor {
        private final String token;

        TokenClientInterceptor(String token) {
            this.token = token;
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
            return new ForwardingClientCall.SimpleForwardingClientCall<>(
                    next.newCall(method, callOptions)) {
                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    headers.put(GrpcAuthTokenInterceptor.AUTH_TOKEN_KEY, token);
                    super.start(responseListener, headers);
                }
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class TestApp {

        @Bean
        ObjectMapper objectMapper() {
            return JsonMapper.builder().build();
        }

        @Bean
        GrpcRpcServer grpcRpcServer(ObjectMapper objectMapper) {
            return new GrpcRpcServer(objectMapper);
        }

        @Bean
        @GlobalServerInterceptor
        GrpcAuthTokenInterceptor grpcAuthTokenInterceptor(
                @Value("${nebula.rpc.grpc.server.auth-token:}") String authToken) {
            return new GrpcAuthTokenInterceptor(authToken);
        }

        @Bean
        TestRpcServiceImpl testRpcService() {
            return new TestRpcServiceImpl();
        }
    }
}

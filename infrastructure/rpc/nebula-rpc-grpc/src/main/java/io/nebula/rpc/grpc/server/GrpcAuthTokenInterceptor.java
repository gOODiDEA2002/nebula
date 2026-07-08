package io.nebula.rpc.grpc.server;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;

/**
 * gRPC RPC 端点 token 鉴权拦截器。
 * <p>
 * 与 HTTP 侧 {@code HttpRpcController} 的 {@code X-Nebula-Rpc-Token} 逻辑对齐：
 * 配置了 token 时校验，未配置时放行（默认关闭，不影响纯内网 gRPC 调用）。
 * gRPC metadata key 为 {@code x-nebula-rpc-token}（小写，gRPC 惯例）。
 *
 * @author Nebula Framework
 * @since 2.0.1
 */
@Slf4j
public class GrpcAuthTokenInterceptor implements ServerInterceptor {

    public static final Metadata.Key<String> AUTH_TOKEN_KEY =
            Metadata.Key.of("x-nebula-rpc-token", Metadata.ASCII_STRING_MARSHALLER);

    private final String expectedToken;

    public GrpcAuthTokenInterceptor(String expectedToken) {
        this.expectedToken = expectedToken == null ? "" : expectedToken;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        if (expectedToken.isEmpty()) {
            return next.startCall(call, headers);
        }

        String provided = headers.get(AUTH_TOKEN_KEY);
        boolean pass = provided != null && java.security.MessageDigest.isEqual(
                expectedToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                provided.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (!pass) {
            log.warn("gRPC RPC token 校验失败: method={}", call.getMethodDescriptor().getFullMethodName());
            call.close(Status.UNAUTHENTICATED.withDescription("gRPC RPC 鉴权失败"), new Metadata());
            return new ServerCall.Listener<>() {};
        }

        return next.startCall(call, headers);
    }
}

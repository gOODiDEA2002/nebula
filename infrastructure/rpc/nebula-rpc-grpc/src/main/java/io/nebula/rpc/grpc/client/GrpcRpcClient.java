package io.nebula.rpc.grpc.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.nebula.rpc.core.client.RpcClient;
import io.nebula.rpc.grpc.config.GrpcRpcProperties;
import io.nebula.rpc.grpc.proto.GenericRpcServiceGrpc;
import io.nebula.rpc.grpc.proto.RpcRequest;
import io.nebula.rpc.grpc.proto.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * gRPC RPC 客户端
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class GrpcRpcClient implements RpcClient {

    private final ObjectMapper objectMapper;
    private final GrpcRpcProperties.ClientConfig clientConfig;
    private ManagedChannel channel;
    private GenericRpcServiceGrpc.GenericRpcServiceBlockingStub blockingStub;
    private String target;

    public GrpcRpcClient(ObjectMapper objectMapper, GrpcRpcProperties.ClientConfig clientConfig) {
        this.objectMapper = objectMapper;
        this.clientConfig = clientConfig;
        this.target = clientConfig.getTarget();
        initChannel();
    }

    /**
     * 初始化 gRPC Channel
     */
    private void initChannel() {
        if (channel != null && !channel.isShutdown()) {
            return;
        }

        log.info("初始化 gRPC Channel: target={}", target);

        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder
                .forTarget(target)
                .maxInboundMessageSize(clientConfig.getMaxInboundMessageSize());

        // 配置协商类型
        if ("plaintext".equals(clientConfig.getNegotiationType())) {
            channelBuilder.usePlaintext();
        }

        // 配置负载均衡
        if (clientConfig.getLoadBalancingPolicy() != null) {
            channelBuilder.defaultLoadBalancingPolicy(clientConfig.getLoadBalancingPolicy());
        }

        channel = channelBuilder.build();
        blockingStub = GenericRpcServiceGrpc.newBlockingStub(channel)
                .withDeadlineAfter(clientConfig.getRequestTimeout(), TimeUnit.MILLISECONDS);
    }

    @Override
    public <T> T call(Class<T> serviceClass, String methodName, Object... args) {
        String requestId = UUID.randomUUID().toString();
        
        log.debug("执行 gRPC RPC 调用: requestId={}, service={}, method={}", 
                requestId, serviceClass.getName(), methodName);

        try {
            // 构建请求
            RpcRequest.Builder requestBuilder = RpcRequest.newBuilder()
                    .setRequestId(requestId)
                    .setServiceName(serviceClass.getName())
                    .setMethodName(methodName)
                    .setTimestamp(System.currentTimeMillis());

            // 添加参数类型和参数值
            if (args != null && args.length > 0) {
                for (Object arg : args) {
                    if (arg != null) {
                        requestBuilder.addParameterTypes(arg.getClass().getName());
                        requestBuilder.addParameters(objectMapper.writeValueAsString(arg));
                    } else {
                        requestBuilder.addParameterTypes("java.lang.Object");
                        requestBuilder.addParameters("null");
                    }
                }
            }

            // 执行调用
            RpcResponse response = executeWithRetry(requestBuilder.build());

            // 处理响应
            if (!response.getSuccess()) {
                throw new RuntimeException(
                        String.format("gRPC RPC调用失败: %s - %s", 
                                response.getErrorCode(), response.getErrorMessage()));
            }

            // 反序列化结果
            String resultJson = response.getResult();
            if (resultJson == null || resultJson.isEmpty() || "null".equals(resultJson)) {
                return null;
            }

            return objectMapper.readValue(resultJson, serviceClass);

        } catch (Exception e) {
            log.error("gRPC RPC 调用异常: requestId={}, service={}, method={}", 
                    requestId, serviceClass.getName(), methodName, e);
            throw new RuntimeException("gRPC RPC调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> CompletableFuture<T> callAsync(Class<T> serviceClass, String methodName, Object... args) {
        return CompletableFuture.supplyAsync(() -> call(serviceClass, methodName, args));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T createProxy(Class<T> serviceClass) {
        return (T) Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class<?>[]{serviceClass},
                (proxy, method, methodArgs) -> {
                    // 对于Object类的方法，直接调用
                    if (method.getDeclaringClass() == Object.class) {
                        return method.invoke(this, methodArgs);
                    }

                    // 执行 gRPC RPC 调用
                    Class<?> returnType = method.getReturnType();
                    Object result = callInternal(serviceClass, method, methodArgs);
                    
                    if (result == null) {
                        return null;
                    }

                    // 转换结果类型
                    return objectMapper.convertValue(result, returnType);
                }
        );
    }

    /**
     * 内部调用方法(支持返回类型推断)
     */
    private Object callInternal(Class<?> serviceClass, Method method, Object[] args) throws Exception {
        String requestId = UUID.randomUUID().toString();
        
        log.debug("执行 gRPC RPC 调用: requestId={}, service={}, method={}", 
                requestId, serviceClass.getName(), method.getName());

        // 构建请求
        RpcRequest.Builder requestBuilder = RpcRequest.newBuilder()
                .setRequestId(requestId)
                .setServiceName(serviceClass.getName())
                .setMethodName(method.getName())
                .setTimestamp(System.currentTimeMillis());

        // 添加参数类型和参数值
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                requestBuilder.addParameterTypes(parameterTypes[i].getName());
                if (args[i] != null) {
                    requestBuilder.addParameters(objectMapper.writeValueAsString(args[i]));
                } else {
                    requestBuilder.addParameters("null");
                }
            }
        }

        // 执行调用
        RpcResponse response = executeWithRetry(requestBuilder.build());

        // 处理响应
        if (!response.getSuccess()) {
            throw new RuntimeException(
                    String.format("gRPC RPC调用失败: %s - %s", 
                            response.getErrorCode(), response.getErrorMessage()));
        }

        // 反序列化结果
        String resultJson = response.getResult();
        if (resultJson == null || resultJson.isEmpty() || "null".equals(resultJson)) {
            return null;
        }

        return objectMapper.readValue(resultJson, method.getReturnType());
    }

    /**
     * 带重试的执行
     */
    private RpcResponse executeWithRetry(RpcRequest request) {
        int retryCount = clientConfig.getRetryCount();
        long retryInterval = clientConfig.getRetryInterval();
        
        Exception lastException = null;
        for (int i = 0; i <= retryCount; i++) {
            try {
                return blockingStub.call(request);
            } catch (Exception e) {
                lastException = e;
                if (i < retryCount) {
                    log.warn("gRPC RPC 调用失败，第 {} 次重试: {}", i + 1, e.getMessage());
                    try {
                        Thread.sleep(retryInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        throw new RuntimeException("gRPC RPC调用失败，已重试 " + retryCount + " 次", lastException);
    }

    @Override
    public String getServiceAddress(String serviceName) {
        return target;
    }

    /**
     * 设置目标地址
     */
    public void setTarget(String target) {
        if (!this.target.equals(target)) {
            this.target = target;
            close();
            initChannel();
        }
    }

    @Override
    public void close() {
        if (channel != null && !channel.isShutdown()) {
            log.info("关闭 gRPC Channel: target={}", target);
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error("关闭 gRPC Channel 失败", e);
                Thread.currentThread().interrupt();
            }
        }
    }
}


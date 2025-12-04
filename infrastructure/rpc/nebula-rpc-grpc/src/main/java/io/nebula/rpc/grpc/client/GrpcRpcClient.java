package io.nebula.rpc.grpc.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.nebula.rpc.core.client.RpcClient;
import io.nebula.rpc.core.discovery.ServiceDiscoveryRpcClient;
import io.nebula.rpc.grpc.config.GrpcRpcProperties;
import io.nebula.rpc.core.context.RpcContext;
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
 * 支持服务发现集成，实现 ConfigurableRpcClient 接口以支持动态地址变更
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class GrpcRpcClient implements ServiceDiscoveryRpcClient.ConfigurableRpcClient {

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
        // 注意：不在这里设置deadline，而是在每次调用时设置，避免deadline过期问题
        blockingStub = GenericRpcServiceGrpc.newBlockingStub(channel);
    }
    
    /**
     * 获取带有新deadline的stub（每次调用时使用）
     */
    private GenericRpcServiceGrpc.GenericRpcServiceBlockingStub getStubWithDeadline() {
        return blockingStub.withDeadlineAfter(clientConfig.getRequestTimeout(), TimeUnit.MILLISECONDS);
    }

    @Override
    public <T> T call(Class<T> serviceClass, String methodName, Object... args) {
        String requestId = UUID.randomUUID().toString();
        
        log.debug("执行 gRPC RPC 调用: requestId={}, service={}, method={}", 
                requestId, serviceClass.getName(), methodName);

        try {
            // 通过反射找到方法，获取实际返回类型
            Method method = findMethod(serviceClass, methodName, args);
            if (method == null) {
                throw new NoSuchMethodException(
                        String.format("方法未找到: %s.%s", serviceClass.getName(), methodName));
            }
            
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

            // 使用方法的实际返回类型进行反序列化（支持泛型）
            @SuppressWarnings("unchecked")
            T result = (T) objectMapper.readValue(resultJson, 
                    objectMapper.constructType(method.getGenericReturnType()));
            
            return result;

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
                    // callInternal 已经使用 method.getGenericReturnType() 进行了正确的反序列化
                    // 直接返回结果，不需要再次转换（避免泛型信息丢失）
                    return callInternal(serviceClass, method, methodArgs);
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
        
        // 添加 RpcContext 中的 metadata（如用户信息）
        requestBuilder.putAllMetadata(RpcContext.getAll());

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

        // 使用泛型返回类型以支持 List<T>、Map<K,V> 等泛型类型
        return objectMapper.readValue(resultJson, 
                objectMapper.constructType(method.getGenericReturnType()));
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
                // 每次调用时获取带有新deadline的stub，避免deadline过期问题
                return getStubWithDeadline().call(request);
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
    public synchronized void setTarget(String target) {
        if (target == null) {
            return;
        }
        // 处理 this.target 可能为 null 的情况
        if (target.equals(this.target)) {
            return;
        }
        
        String oldTarget = this.target;
        this.target = target;
        
        // 保存旧的 channel 用于后续关闭
        ManagedChannel oldChannel = this.channel;
        
        // 创建新的 channel 和 stub
        log.info("切换 gRPC 目标地址: {} -> {}", oldTarget, target);
        this.channel = null;
        initChannel();
        
        // 异步关闭旧的 channel（避免阻塞）
        if (oldChannel != null && !oldChannel.isShutdown()) {
            final ManagedChannel channelToClose = oldChannel;
            Thread.ofVirtual().start(() -> {
                try {
                    channelToClose.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    log.warn("关闭旧 gRPC Channel 时被中断");
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.warn("关闭旧 gRPC Channel 失败", e);
                }
            });
        }
    }
    
    /**
     * 设置目标地址（实现 ConfigurableRpcClient 接口）
     * 用于服务发现集成，支持动态地址变更
     * 
     * @param address gRPC 地址（如：192.168.2.200:9081）
     *                ServiceDiscoveryRpcClient 已经处理好了端口映射
     */
    @Override
    public void setTargetAddress(String address) {
        // ✅ 直接使用传入的地址（已经由 ServiceDiscoveryRpcClient 处理好了）
        String newTarget = address
                .replace("http://", "")
                .replace("https://", "");
        
        log.debug("设置 gRPC 目标地址: {}", newTarget);
        setTarget(newTarget);
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
    
    /**
     * 查找方法（根据方法名和参数类型）
     */
    private Method findMethod(Class<?> serviceClass, String methodName, Object[] args) {
        // 获取参数类型
        Class<?>[] parameterTypes = new Class<?>[args == null ? 0 : args.length];
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                parameterTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
            }
        }
        
        // 尝试精确匹配
        try {
            return serviceClass.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            // 尝试模糊匹配（处理包装类型 vs 基本类型）
            for (Method method : serviceClass.getMethods()) {
                if (method.getName().equals(methodName) && 
                    method.getParameterCount() == parameterTypes.length) {
                    
                    // 检查参数类型是否兼容
                    Class<?>[] methodParams = method.getParameterTypes();
                    boolean compatible = true;
                    for (int i = 0; i < parameterTypes.length; i++) {
                        if (!isCompatible(parameterTypes[i], methodParams[i])) {
                            compatible = false;
                            break;
                        }
                    }
                    
                    if (compatible) {
                        return method;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * 检查类型是否兼容（处理包装类型和基本类型）
     */
    private boolean isCompatible(Class<?> argType, Class<?> paramType) {
        if (paramType.isAssignableFrom(argType)) {
            return true;
        }
        
        // 当参数为 null 时，argType 为 Object.class，此时与任何非基本类型参数兼容
        if (argType == Object.class && !paramType.isPrimitive()) {
            return true;
        }
        
        // 处理基本类型和包装类型
        if (paramType.isPrimitive()) {
            if (paramType == int.class && argType == Integer.class) return true;
            if (paramType == long.class && argType == Long.class) return true;
            if (paramType == double.class && argType == Double.class) return true;
            if (paramType == float.class && argType == Float.class) return true;
            if (paramType == boolean.class && argType == Boolean.class) return true;
            if (paramType == byte.class && argType == Byte.class) return true;
            if (paramType == short.class && argType == Short.class) return true;
            if (paramType == char.class && argType == Character.class) return true;
        }
        
        return false;
    }
}


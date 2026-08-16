package io.nebula.rpc.grpc.client;

import tools.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import io.nebula.rpc.core.client.RpcClient;
import io.nebula.rpc.core.discovery.ServiceDiscoveryRpcClient;
import io.nebula.rpc.grpc.config.GrpcRpcProperties;
import io.nebula.rpc.core.context.RpcContext;
import io.nebula.rpc.grpc.proto.GenericRpcServiceGrpc;
import io.nebula.rpc.grpc.proto.RpcRequest;
import io.nebula.rpc.grpc.proto.RpcResponse;
import io.nebula.rpc.grpc.server.GrpcAuthTokenInterceptor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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

    /**
     * 按目标地址缓存的常驻 Channel。
     * <p>
     * {@link #callWithTarget} 走这里, 每个目标地址各持一条 {@link ManagedChannel},
     * 从而无需在整个 RPC 往返期间持锁改写共享的 {@link #target}/{@link #channel}, 消除串行化。
     * <p>
     * 内存边界: 缓存无界。假设单服务下游实例数为个位数量级(Nacos 注册实例数),
     * 键为「host:grpcPort」字符串, 常驻通道数与实例数同阶, 可接受; 仅在 {@link #close()} 统一释放。
     */
    private final ConcurrentHashMap<String, ManagedChannel> targetChannels = new ConcurrentHashMap<>();

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

        channel = buildChannel(target);
        // 注意：不在这里设置deadline，而是在每次调用时设置，避免deadline过期问题
        blockingStub = newAuthStub(channel);
    }

    /**
     * 按目标地址构建一条 {@link ManagedChannel}（不设置 deadline，deadline 每次调用时再加）。
     */
    private ManagedChannel buildChannel(String targetAddress) {
        NettyChannelBuilder channelBuilder = NettyChannelBuilder
                .forTarget(targetAddress)
                .maxInboundMessageSize(clientConfig.getMaxInboundMessageSize())
                .proxyDetector(addr -> null);

        if ("plaintext".equals(clientConfig.getNegotiationType())) {
            channelBuilder.usePlaintext();
        }

        if (clientConfig.getLoadBalancingPolicy() != null) {
            channelBuilder.defaultLoadBalancingPolicy(clientConfig.getLoadBalancingPolicy());
        }

        return channelBuilder.build();
    }

    /**
     * 基于给定 channel 构建 blocking stub，并按配置附加认证头拦截器。
     */
    private GenericRpcServiceGrpc.GenericRpcServiceBlockingStub newAuthStub(ManagedChannel targetChannel) {
        GenericRpcServiceGrpc.GenericRpcServiceBlockingStub stub =
                GenericRpcServiceGrpc.newBlockingStub(targetChannel);
        if (clientConfig.getAuthToken() != null && !clientConfig.getAuthToken().isEmpty()) {
            Metadata headers = new Metadata();
            headers.put(GrpcAuthTokenInterceptor.AUTH_TOKEN_KEY, clientConfig.getAuthToken());
            stub = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));
        }
        return stub;
    }

    @Override
    public <T> T call(Class<?> serviceClass, String methodName, Object... args) {
        // 走共享单通道 blockingStub（兼容既有直连/代理路径）
        return doCall(blockingStub, serviceClass, methodName, args);
    }

    /**
     * 面向指定目标地址发起一次<b>无锁</b>调用，重写 {@link ServiceDiscoveryRpcClient.ConfigurableRpcClient}
     * 默认的 {@code synchronized(this){ setTargetAddress + call }} 版本。
     * <p>
     * 默认实现在整个 RPC 往返期间持有 this 监视器，会把同一代理 bean 的全部调用串行化
     * （生产实测有效并发被钉死为 1）。本实现按目标地址维护独立的常驻 Channel
     * （见 {@link #targetChannels}），每次调用用对应地址的 channel 现构 stub 发起，
     * 不触碰共享可变的 {@link #target}/{@link #channel}，因此并发调用不同或相同地址互不阻塞，
     * 且不会因 target 被并发覆盖而串台。
     * <p>
     * 地址失联时的重试语义与 {@link #call} 完全一致（由 {@link #executeWithRetry} 承担）；
     * 缓存的 channel 长驻复用，gRPC 自身负责断线重连，仅在 {@link #close()} 时统一关闭。
     */
    @Override
    public <T> T callWithTarget(String targetAddress, Class<?> serviceClass, String methodName, Object... args) {
        String normalized = normalizeTarget(targetAddress);
        ManagedChannel targetChannel = targetChannels.computeIfAbsent(normalized, addr -> {
            log.info("为目标地址创建常驻 gRPC Channel: {}", addr);
            return buildChannel(addr);
        });
        return doCall(newAuthStub(targetChannel), serviceClass, methodName, args);
    }

    /**
     * 使用给定的 blocking stub 执行一次调用（构建请求 + 重试 + 反序列化）。
     * 与共享单通道解耦，供 {@link #call} 与 {@link #callWithTarget} 共用。
     */
    private <T> T doCall(GenericRpcServiceGrpc.GenericRpcServiceBlockingStub baseStub,
                         Class<?> serviceClass, String methodName, Object... args) {
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
            RpcResponse response = executeWithRetry(baseStub, requestBuilder.build());

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

    /**
     * 归一化目标地址：剥离 http(s):// 前缀，得到 gRPC 需要的 host:port。
     */
    private String normalizeTarget(String address) {
        return address
                .replace("http://", "")
                .replace("https://", "");
    }

    @Override
    public <T> CompletableFuture<T> callAsync(Class<?> serviceClass, String methodName, Object... args) {
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
     * 带重试的执行（走共享单通道 blockingStub）
     */
    private RpcResponse executeWithRetry(RpcRequest request) {
        return executeWithRetry(blockingStub, request);
    }

    /**
     * 带重试的执行（面向指定 base stub，deadline 每次尝试时新加，避免 deadline 过期）
     */
    private RpcResponse executeWithRetry(GenericRpcServiceGrpc.GenericRpcServiceBlockingStub baseStub,
                                         RpcRequest request) {
        int retryCount = clientConfig.getRetryCount();
        long retryInterval = clientConfig.getRetryInterval();

        Exception lastException = null;
        for (int i = 0; i <= retryCount; i++) {
            try {
                // 每次调用时附加新deadline的stub，避免deadline过期问题
                return baseStub.withDeadlineAfter(clientConfig.getRequestTimeout(), TimeUnit.MILLISECONDS)
                        .call(request);
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
        // 直接使用传入的地址（已经由 ServiceDiscoveryRpcClient 处理好了）
        String newTarget = normalizeTarget(address);

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
        // 关闭按目标地址缓存的常驻 channel
        for (Map.Entry<String, ManagedChannel> entry : targetChannels.entrySet()) {
            ManagedChannel targetChannel = entry.getValue();
            if (targetChannel != null && !targetChannel.isShutdown()) {
                log.info("关闭缓存的 gRPC Channel: target={}", entry.getKey());
                try {
                    targetChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    log.error("关闭缓存的 gRPC Channel 失败: target={}", entry.getKey(), e);
                    Thread.currentThread().interrupt();
                }
            }
        }
        targetChannels.clear();
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
